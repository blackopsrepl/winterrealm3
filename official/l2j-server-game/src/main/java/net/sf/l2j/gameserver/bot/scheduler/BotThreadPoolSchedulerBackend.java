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

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.ThreadPoolManager;

/** Production bridge to the game server's shared AI executors. */
public final class BotThreadPoolSchedulerBackend implements BotSchedulerBackend
{
	@Override
	public BotScheduledPulse schedulePulse(Runnable pulse, long periodMillis)
	{
		ScheduledFuture<?> future = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(pulse, 0, periodMillis);
		if (future == null)
		{
			throw new IllegalStateException("The AI scheduler rejected the native bot pulse");
		}
		return () -> future.cancel(false);
	}

	@Override
	public void executeDecision(Runnable decision)
	{
		ThreadPoolManager.getInstance().executeAi(decision);
	}
}
