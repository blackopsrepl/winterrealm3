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
package net.sf.l2j.gameserver.bot.tactics;

import net.sf.l2j.gameserver.bot.strategy.BotAction;
import net.sf.l2j.gameserver.bot.strategy.BotNextAction;

/** A scored profile action that can cheaply reject the current snapshot. */
record BotContextualNextAction(BotAction<BotDecisionContext> action, BotNextAction next)
{
	BotContextualNextAction
	{
		if ((action == null) || (next == null) || !action.name().equals(next.actionName()))
		{
			throw new IllegalArgumentException("Contextual profile action and score must share one action name");
		}
	}
}
