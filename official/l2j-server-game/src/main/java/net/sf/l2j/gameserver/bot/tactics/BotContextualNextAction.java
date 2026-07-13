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

import java.util.Set;

import net.sf.l2j.gameserver.bot.strategy.BotAction;
import net.sf.l2j.gameserver.bot.strategy.BotNextAction;
import net.sf.l2j.gameserver.model.base.ClassId;

/** A scored profile action indexed by classes its conditions require to exist. */
record BotContextualNextAction(BotAction<BotDecisionContext> action, BotNextAction next, Set<ClassId> requiredObservedClasses)
{
	BotContextualNextAction
	{
		if ((action == null) || (next == null) || (requiredObservedClasses == null) || !action.name().equals(next.actionName()))
		{
			throw new IllegalArgumentException("Contextual profile action requires one action name and its exact class index");
		}
		if (requiredObservedClasses.stream().anyMatch(classId -> (classId == null) || (classId.getRace() == null)))
		{
			throw new IllegalArgumentException("Contextual profile class index must contain only exact playable classes");
		}
		requiredObservedClasses = Set.copyOf(requiredObservedClasses);
	}

	BotContextualNextAction(BotAction<BotDecisionContext> action, BotNextAction next)
	{
		this(action, next, Set.of());
	}
}
