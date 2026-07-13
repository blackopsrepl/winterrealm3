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
package net.sf.l2j.gameserver.bot.tactics;

import java.util.Set;
import java.util.function.ToDoubleFunction;

import net.sf.l2j.gameserver.bot.strategy.BotAction;
import net.sf.l2j.gameserver.bot.strategy.BotNextAction;
import net.sf.l2j.gameserver.model.base.ClassId;

/** A context-scored profile action indexed by classes its conditions require to exist. */
record BotContextualNextAction(BotAction<BotDecisionContext> action, String actionName, ToDoubleFunction<BotDecisionContext> relevance, Set<ClassId> requiredObservedClasses)
{
	BotContextualNextAction
	{
		if ((action == null) || (actionName == null) || actionName.isBlank() || (relevance == null) || (requiredObservedClasses == null) || !action.name().equals(actionName))
		{
			throw new IllegalArgumentException("Contextual profile action requires one action name, relevance function, and exact class index");
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

	BotContextualNextAction(BotAction<BotDecisionContext> action, BotNextAction next, Set<ClassId> requiredObservedClasses)
	{
		this(action, next.actionName(), ignored -> next.relevance(), requiredObservedClasses);
	}

	BotNextAction next(BotDecisionContext context)
	{
		double score = relevance.applyAsDouble(context);
		if (!Double.isFinite(score) || (score <= 0.0))
		{
			throw new IllegalStateException("Contextual relevance for " + actionName + " must be positive and finite");
		}
		return new BotNextAction(actionName, score);
	}
}
