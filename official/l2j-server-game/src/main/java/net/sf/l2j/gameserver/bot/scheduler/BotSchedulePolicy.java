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

import java.util.concurrent.TimeUnit;

/** Immutable global cadence and admission limits for the central scheduler. */
public record BotSchedulePolicy(long pulseMillis, long foregroundCadenceMillis, long backgroundCadenceMillis, int maxDispatchPerPulse, int maxInFlight)
{
	public static final long MAXIMUM_CADENCE_MILLIS = 60000;
	public static final int MAXIMUM_DISPATCH_LIMIT = 10000;

	public BotSchedulePolicy
	{
		if ((pulseMillis <= 0) || (pulseMillis > MAXIMUM_CADENCE_MILLIS))
		{
			throw new IllegalArgumentException("Bot scheduler pulse must be between 1 and " + MAXIMUM_CADENCE_MILLIS + " milliseconds");
		}
		if ((foregroundCadenceMillis < pulseMillis) || (foregroundCadenceMillis > MAXIMUM_CADENCE_MILLIS))
		{
			throw new IllegalArgumentException("Foreground cadence must be between the pulse and " + MAXIMUM_CADENCE_MILLIS + " milliseconds");
		}
		if ((backgroundCadenceMillis < foregroundCadenceMillis) || (backgroundCadenceMillis > MAXIMUM_CADENCE_MILLIS))
		{
			throw new IllegalArgumentException("Background cadence must be at least the foreground cadence and at most " + MAXIMUM_CADENCE_MILLIS + " milliseconds");
		}
		if ((maxDispatchPerPulse <= 0) || (maxDispatchPerPulse > MAXIMUM_DISPATCH_LIMIT))
		{
			throw new IllegalArgumentException("Bot dispatch limit must be between 1 and " + MAXIMUM_DISPATCH_LIMIT);
		}
		if ((maxInFlight <= 0) || (maxInFlight > MAXIMUM_DISPATCH_LIMIT))
		{
			throw new IllegalArgumentException("Bot in-flight limit must be between 1 and " + MAXIMUM_DISPATCH_LIMIT);
		}
	}

	public long cadenceNanos(BotActivityTier tier)
	{
		if (tier == null)
		{
			throw new IllegalArgumentException("Bot activity tier cannot be null");
		}
		long millis = tier == BotActivityTier.FOREGROUND ? foregroundCadenceMillis : backgroundCadenceMillis;
		return TimeUnit.MILLISECONDS.toNanos(millis);
	}
}
