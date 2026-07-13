/*
 * Copyright © 2004-2020 L2J Server
 *
 * This file is part of L2J Server.
 *
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.bot.scheduler;

import java.util.Comparator;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** One fair, bounded pulse scheduler for every loaded native player bot. */
public final class BotDecisionScheduler
{
	private static final Logger _log = Logger.getLogger(BotDecisionScheduler.class.getName());

	private final Supplier<? extends List<? extends BotScheduledRuntime>> _runtimeSource;
	private final BotSchedulePolicy _policy;
	private final BotSchedulerBackend _backend;
	private final LongSupplier _clock;
	private BotScheduledPulse _scheduledPulse;
	private boolean _running;
	private int _cursor;
	private int _inFlight;
	private int _maximumObservedInFlight;
	private long _ticks;
	private long _pulses;
	private long _dispatched;
	private long _completed;
	private long _failed;
	private long _rejected;
	private long _maximumQueueLagNanos;
	private long _maximumDecisionDurationNanos;

	public BotDecisionScheduler(Supplier<? extends List<? extends BotScheduledRuntime>> runtimeSource, BotSchedulePolicy policy, BotSchedulerBackend backend)
	{
		this(runtimeSource, policy, backend, System::nanoTime);
	}

	BotDecisionScheduler(Supplier<? extends List<? extends BotScheduledRuntime>> runtimeSource, BotSchedulePolicy policy, BotSchedulerBackend backend, LongSupplier clock)
	{
		if ((runtimeSource == null) || (policy == null) || (backend == null) || (clock == null))
		{
			throw new IllegalArgumentException("Bot scheduler requires runtimes, policy, backend, and clock");
		}
		_runtimeSource = runtimeSource;
		_policy = policy;
		_backend = backend;
		_clock = clock;
	}

	/** Starts exactly one periodic scheduling pulse. */
	public synchronized void start()
	{
		if (_running || (_scheduledPulse != null))
		{
			throw new IllegalStateException("Bot decision scheduler has already been started");
		}
		_running = true;
		try
		{
			_scheduledPulse = _backend.schedulePulse(this::safePulse, _policy.pulseMillis());
			if (_scheduledPulse == null)
			{
				throw new IllegalStateException("Bot scheduler backend returned no pulse handle");
			}
		}
		catch (RuntimeException e)
		{
			_running = false;
			throw e;
		}
	}

	/** Cancels admission and waits without unloading players under live work. */
	public void stopAndAwait()
	{
		BotScheduledPulse pulse;
		synchronized (this)
		{
			_running = false;
			pulse = _scheduledPulse;
			_scheduledPulse = null;
		}
		RuntimeException cancellationFailure = null;
		if (pulse != null)
		{
			try
			{
				pulse.cancel();
			}
			catch (RuntimeException e)
			{
				cancellationFailure = e;
			}
		}

		boolean interrupted = false;
		synchronized (this)
		{
			while (_inFlight > 0)
			{
				try
				{
					wait();
				}
				catch (InterruptedException e)
				{
					interrupted = true;
				}
			}
		}
		if (interrupted)
		{
			Thread.currentThread().interrupt();
		}
		if (cancellationFailure != null)
		{
			throw cancellationFailure;
		}
	}

	public synchronized BotSchedulerSnapshot snapshot()
	{
		return new BotSchedulerSnapshot(_running, _pulses, _dispatched, _completed, _failed, _rejected, _inFlight, _maximumObservedInFlight, _maximumQueueLagNanos, _maximumDecisionDurationNanos);
	}

	private void safePulse()
	{
		try
		{
			pulse();
		}
		catch (RuntimeException e)
		{
			recordPulseFailure();
			_log.log(Level.SEVERE, "Native bot scheduling pulse failed", e);
		}
	}

	private synchronized void pulse()
	{
		if (!_running)
		{
			return;
		}
		_pulses++;
		List<? extends BotScheduledRuntime> runtimes;
		try
		{
			runtimes = _runtimeSource.get().stream().sorted(Comparator.comparingInt(BotScheduledRuntime::getCharacterId)).toList();
		}
		catch (RuntimeException e)
		{
			_failed++;
			_log.log(Level.SEVERE, "Could not snapshot native bot runtimes", e);
			return;
		}
		if (runtimes.isEmpty())
		{
			_cursor = 0;
			return;
		}

		int start = Math.floorMod(_cursor, runtimes.size());
		int examined = 0;
		int admitted = 0;
		long nowNanos = _clock.getAsLong();
		while ((examined < runtimes.size()) && (admitted < _policy.maxDispatchPerPulse()) && (_inFlight < _policy.maxInFlight()))
		{
			BotScheduledRuntime runtime = runtimes.get((start + examined) % runtimes.size());
			examined++;
			try
			{
				if (!runtime.isDecisionEligible())
				{
					continue;
				}

				long cadenceNanos = _policy.cadenceNanos(runtime.getActivityTier());
				if (!runtime.tryBeginDecision(nowNanos, cadenceNanos))
				{
					continue;
				}
			}
			catch (RuntimeException e)
			{
				_failed++;
				_log.log(Level.WARNING, "Could not admit bot character " + runtime.getCharacterId(), e);
				continue;
			}
			admitted++;
			_inFlight++;
			_maximumObservedInFlight = Math.max(_maximumObservedInFlight, _inFlight);
			_dispatched++;
			long tick = ++_ticks;
			try
			{
				_backend.executeDecision(() -> execute(runtime, tick, nowNanos));
			}
			catch (RuntimeException e)
			{
				_rejected++;
				try
				{
					runtime.finishDecision();
				}
				finally
				{
					complete(false, 0, 0);
				}
				_log.log(Level.WARNING, "AI executor rejected bot character " + runtime.getCharacterId(), e);
			}
		}
		_cursor = (start + examined) % runtimes.size();
	}

	private void execute(BotScheduledRuntime runtime, long tick, long admittedAtNanos)
	{
		long startedAtNanos = _clock.getAsLong();
		boolean succeeded = false;
		try
		{
			runtime.runDecision(tick);
			succeeded = true;
		}
		catch (RuntimeException e)
		{
			_log.log(Level.WARNING, "Decision failed for bot character " + runtime.getCharacterId(), e);
		}
		finally
		{
			long finishedAtNanos = _clock.getAsLong();
			try
			{
				runtime.finishDecision();
			}
			finally
			{
				complete(succeeded, elapsed(admittedAtNanos, startedAtNanos), elapsed(startedAtNanos, finishedAtNanos));
			}
		}
	}

	private synchronized void complete(boolean succeeded, long queueLagNanos, long durationNanos)
	{
		if (_inFlight <= 0)
		{
			throw new IllegalStateException("Bot scheduler completed work without an in-flight admission");
		}
		_inFlight--;
		_completed++;
		if (!succeeded)
		{
			_failed++;
		}
		_maximumQueueLagNanos = Math.max(_maximumQueueLagNanos, queueLagNanos);
		_maximumDecisionDurationNanos = Math.max(_maximumDecisionDurationNanos, durationNanos);
		notifyAll();
	}

	private synchronized void recordPulseFailure()
	{
		_failed++;
	}

	private static long elapsed(long start, long end)
	{
		return end >= start ? end - start : 0;
	}
}
