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

/** Per-runtime admission gate preventing overlapping decisions. */
public final class BotDecisionGate
{
	private boolean _inFlight;
	private long _nextDueNanos = Long.MIN_VALUE;

	/**
	 * Admits one due decision and advances its next due time atomically.
	 * @param nowNanos monotonic scheduler time
	 * @param cadenceNanos positive tier cadence
	 * @return {@code true} only when this caller owns the decision slot
	 */
	public synchronized boolean tryAcquire(long nowNanos, long cadenceNanos)
	{
		if (cadenceNanos <= 0)
		{
			throw new IllegalArgumentException("Bot decision cadence must be positive");
		}
		if (_inFlight || (nowNanos < _nextDueNanos))
		{
			return false;
		}
		_inFlight = true;
		_nextDueNanos = saturatedAdd(nowNanos, cadenceNanos);
		return true;
	}

	/** Releases a decision slot exactly once. */
	public synchronized void release()
	{
		if (!_inFlight)
		{
			throw new IllegalStateException("Bot decision gate is not acquired");
		}
		_inFlight = false;
		notifyAll();
	}

	public synchronized boolean isInFlight()
	{
		return _inFlight;
	}

	public synchronized long getNextDueNanos()
	{
		return _nextDueNanos;
	}

	/** Waits until an admitted decision has returned. */
	public void awaitIdle()
	{
		boolean interrupted = false;
		synchronized (this)
		{
			while (_inFlight)
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
	}

	private static long saturatedAdd(long left, long right)
	{
		return left > (Long.MAX_VALUE - right) ? Long.MAX_VALUE : left + right;
	}
}
