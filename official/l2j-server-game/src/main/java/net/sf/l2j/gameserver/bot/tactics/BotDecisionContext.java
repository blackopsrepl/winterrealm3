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

import net.sf.l2j.gameserver.bot.perception.BotPerceptionSnapshot;

/** One immutable perception plus its narrow, serialized execution port. */
public record BotDecisionContext(BotPerceptionSnapshot perception, BotActionExecutor executor)
{
	public BotDecisionContext
	{
		if ((perception == null) || (executor == null))
		{
			throw new IllegalArgumentException("Bot decision context requires perception and execution");
		}
	}
}
