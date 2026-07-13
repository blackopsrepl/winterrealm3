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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BotDecisionSchedulerTest
{
	private static final long MILLISECOND = 1000000;
	private final Logger _schedulerLogger = Logger.getLogger(BotDecisionScheduler.class.getName());
	private Level _previousLogLevel;

	@BeforeEach
	void silenceExpectedFailureLogs()
	{
		_previousLogLevel = _schedulerLogger.getLevel();
		_schedulerLogger.setLevel(Level.OFF);
	}

	@AfterEach
	void restoreSchedulerLogs()
	{
		_schedulerLogger.setLevel(_previousLogLevel);
	}

	@Test
	void usesOnePulseAndRotatesAcrossABoundedAdmissionWindow()
	{
		AtomicLong clock = new AtomicLong();
		ManualBackend backend = new ManualBackend();
		FakeRuntime first = new FakeRuntime(10, BotActivityTier.FOREGROUND);
		FakeRuntime second = new FakeRuntime(20, BotActivityTier.FOREGROUND);
		FakeRuntime third = new FakeRuntime(30, BotActivityTier.FOREGROUND);
		BotDecisionScheduler scheduler = scheduler(List.of(third, first, second), new BotSchedulePolicy(100, 100, 1000, 2, 2), backend, clock);

		scheduler.start();
		assertEquals(1, backend.scheduledPulses);
		backend.firePulse();
		assertEquals(2, backend.pendingDecisions());
		assertEquals(2, scheduler.snapshot().inFlight());
		backend.firePulse();
		assertEquals(2, backend.pendingDecisions());

		backend.runAllDecisions();
		clock.set(100 * MILLISECOND);
		backend.firePulse();
		backend.runAllDecisions();

		assertEquals(List.of(1L, 4L), first.ticks);
		assertEquals(List.of(2L), second.ticks);
		assertEquals(List.of(3L), third.ticks);
		assertEquals(2, scheduler.snapshot().maximumObservedInFlight());
		scheduler.stopAndAwait();
	}

	@Test
	void appliesBackgroundCadenceAndNeverOverlapsOneRuntime()
	{
		AtomicLong clock = new AtomicLong();
		ManualBackend backend = new ManualBackend();
		FakeRuntime runtime = new FakeRuntime(10, BotActivityTier.BACKGROUND);
		BotDecisionScheduler scheduler = scheduler(List.of(runtime), new BotSchedulePolicy(100, 100, 1000, 10, 10), backend, clock);
		scheduler.start();

		backend.firePulse();
		clock.set(500 * MILLISECOND);
		backend.firePulse();
		assertEquals(1, backend.pendingDecisions());
		backend.runAllDecisions();

		clock.set(999 * MILLISECOND);
		backend.firePulse();
		assertEquals(0, backend.pendingDecisions());
		clock.set(1000 * MILLISECOND);
		backend.firePulse();
		assertEquals(1, backend.pendingDecisions());
		backend.runAllDecisions();
		assertEquals(2, runtime.ticks.size());
		scheduler.stopAndAwait();
	}

	@Test
	void quiescesAdmittedWorkBeforeStopReturns() throws InterruptedException
	{
		AtomicLong clock = new AtomicLong();
		ManualBackend backend = new ManualBackend();
		FakeRuntime runtime = new FakeRuntime(10, BotActivityTier.FOREGROUND);
		BotDecisionScheduler scheduler = scheduler(List.of(runtime), new BotSchedulePolicy(100, 100, 1000, 1, 1), backend, clock);
		scheduler.start();
		backend.firePulse();

		Thread stopper = Thread.ofPlatform().start(scheduler::stopAndAwait);
		assertTrue(backend.cancelled.await(1, TimeUnit.SECONDS));
		assertTrue(stopper.isAlive());
		backend.runAllDecisions();
		stopper.join(1000);

		assertFalse(stopper.isAlive());
		assertFalse(scheduler.snapshot().running());
		assertEquals(0, scheduler.snapshot().inFlight());
	}

	@Test
	void releasesAdmissionsAfterDecisionFailureOrExecutorRejection()
	{
		AtomicLong clock = new AtomicLong();
		ManualBackend backend = new ManualBackend();
		FakeRuntime runtime = new FakeRuntime(10, BotActivityTier.FOREGROUND);
		runtime.fail = true;
		BotDecisionScheduler scheduler = scheduler(List.of(runtime), new BotSchedulePolicy(100, 100, 1000, 1, 1), backend, clock);
		scheduler.start();
		backend.firePulse();
		backend.runAllDecisions();
		assertEquals(0, scheduler.snapshot().inFlight());
		assertEquals(1, scheduler.snapshot().failed());

		runtime.fail = false;
		backend.reject = true;
		clock.set(100 * MILLISECOND);
		backend.firePulse();
		assertEquals(0, scheduler.snapshot().inFlight());
		assertEquals(1, scheduler.snapshot().rejected());
		assertFalse(runtime.gate.isInFlight());
		scheduler.stopAndAwait();
	}

	@Test
	void stillQuiescesWhenPulseCancellationReportsFailure() throws InterruptedException
	{
		AtomicLong clock = new AtomicLong();
		ManualBackend backend = new ManualBackend();
		backend.failCancellation = true;
		FakeRuntime runtime = new FakeRuntime(10, BotActivityTier.FOREGROUND);
		BotDecisionScheduler scheduler = scheduler(List.of(runtime), new BotSchedulePolicy(100, 100, 1000, 1, 1), backend, clock);
		scheduler.start();
		backend.firePulse();
		AtomicReference<RuntimeException> failure = new AtomicReference<>();

		Thread stopper = Thread.ofPlatform().start(() ->
		{
			try
			{
				scheduler.stopAndAwait();
			}
			catch (RuntimeException e)
			{
				failure.set(e);
			}
		});
		assertTrue(backend.cancelled.await(1, TimeUnit.SECONDS));
		assertTrue(stopper.isAlive());
		backend.runAllDecisions();
		stopper.join(1000);

		assertFalse(stopper.isAlive());
		assertEquals("test cancellation failure", failure.get().getMessage());
		assertEquals(0, scheduler.snapshot().inFlight());
	}

	@Test
	void rejectsDuplicateStart()
	{
		ManualBackend backend = new ManualBackend();
		BotDecisionScheduler scheduler = scheduler(List.of(), new BotSchedulePolicy(100, 100, 1000, 1, 1), backend, new AtomicLong());
		scheduler.start();

		assertThrows(IllegalStateException.class, scheduler::start);
		scheduler.stopAndAwait();
	}

	private static BotDecisionScheduler scheduler(List<? extends BotScheduledRuntime> runtimes, BotSchedulePolicy policy, ManualBackend backend, AtomicLong clock)
	{
		return new BotDecisionScheduler(() -> runtimes, policy, backend, clock::get);
	}

	private static final class FakeRuntime implements BotScheduledRuntime
	{
		private final int characterId;
		private final BotActivityTier tier;
		private final BotDecisionGate gate = new BotDecisionGate();
		private final List<Long> ticks = new ArrayList<>();
		private boolean eligible = true;
		private boolean fail;

		private FakeRuntime(int characterId, BotActivityTier tier)
		{
			this.characterId = characterId;
			this.tier = tier;
		}

		@Override
		public int getCharacterId()
		{
			return characterId;
		}

		@Override
		public boolean isDecisionEligible()
		{
			return eligible;
		}

		@Override
		public BotActivityTier getActivityTier()
		{
			return tier;
		}

		@Override
		public boolean tryBeginDecision(long nowNanos, long cadenceNanos)
		{
			return gate.tryAcquire(nowNanos, cadenceNanos);
		}

		@Override
		public void runDecision(long tick)
		{
			ticks.add(tick);
			if (fail)
			{
				throw new IllegalStateException("test decision failure");
			}
		}

		@Override
		public void finishDecision()
		{
			gate.release();
		}
	}

	private static final class ManualBackend implements BotSchedulerBackend
	{
		private final Deque<Runnable> decisions = new ArrayDeque<>();
		private final CountDownLatch cancelled = new CountDownLatch(1);
		private Runnable pulse;
		private int scheduledPulses;
		private boolean reject;
		private boolean failCancellation;

		@Override
		public BotScheduledPulse schedulePulse(Runnable scheduledPulse, long periodMillis)
		{
			scheduledPulses++;
			pulse = scheduledPulse;
			return () ->
			{
				cancelled.countDown();
				if (failCancellation)
				{
					throw new IllegalStateException("test cancellation failure");
				}
			};
		}

		@Override
		public void executeDecision(Runnable decision)
		{
			if (reject)
			{
				throw new IllegalStateException("test executor rejection");
			}
			decisions.addLast(decision);
		}

		private void firePulse()
		{
			pulse.run();
		}

		private int pendingDecisions()
		{
			return decisions.size();
		}

		private void runAllDecisions()
		{
			while (!decisions.isEmpty())
			{
				decisions.removeFirst().run();
			}
		}
	}
}
