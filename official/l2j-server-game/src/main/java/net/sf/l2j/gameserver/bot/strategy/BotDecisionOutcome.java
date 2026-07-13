/*
 * Copyright © 2026 l2j-interlude contributors
 *
 * This file is part of l2j-interlude.
 *
 * l2j-interlude is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * l2j-interlude is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.bot.strategy;

/**
 * Complete observable outcome of one decision tick.
 * @param status terminal status
 * @param actionName successfully executed action, or empty
 * @param trace bounded evaluation trace
 * @param pendingActions actions retained for later ticks
 * @param failure surfaced engine/action failure, or {@code null}
 */
public record BotDecisionOutcome(BotDecisionStatus status, String actionName, BotDecisionTrace trace, int pendingActions, RuntimeException failure)
{
	public BotDecisionOutcome
	{
		if ((status == null) || (trace == null) || (pendingActions < 0))
		{
			throw new IllegalArgumentException("Invalid bot decision outcome");
		}
		actionName = (actionName == null) ? "" : actionName;
		if ((status == BotDecisionStatus.ERROR) != (failure != null))
		{
			throw new IllegalArgumentException("Only error outcomes carry a failure");
		}
	}
}
