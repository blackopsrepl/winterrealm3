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
 *
 * Adapted from CMaNGOS playerbots Queue and ActionBasket. Original C++
 * copyright (C) 2005-2018 MaNGOS project. Modified 2026-07-13.
 */
package net.sf.l2j.gameserver.bot.strategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Persistent, de-duplicating, deterministically ordered per-bot queue. */
final class BotDecisionQueue
{
	private static final Comparator<QueuedAction> ORDER = Comparator.comparingDouble(QueuedAction::relevance).reversed().thenComparing(QueuedAction::actionName).thenComparingLong(QueuedAction::sequence);

	private final Map<String, QueuedAction> _actions = new HashMap<>();
	private long _nextSequence;

	void offer(String actionName, double relevance, boolean skipPrerequisites, BotEvent event, long tick)
	{
		if (!Double.isFinite(relevance) || (relevance <= 0.0))
		{
			return;
		}
		QueuedAction next = new QueuedAction(actionName, relevance, skipPrerequisites, event, tick, _nextSequence++);
		_actions.compute(actionName, (key, existing) -> ((existing == null) || (next.relevance() > existing.relevance())) ? next : existing);
	}

	QueuedAction poll()
	{
		QueuedAction selected = peek();
		if (selected != null)
		{
			_actions.remove(selected.actionName(), selected);
		}
		return selected;
	}

	QueuedAction peek()
	{
		return _actions.values().stream().min(ORDER).orElse(null);
	}

	List<QueuedAction> removeExpired(long currentTick, long maximumAgeTicks)
	{
		List<QueuedAction> expired = new ArrayList<>();
		for (QueuedAction action : _actions.values())
		{
			if ((currentTick - action.createdTick()) > maximumAgeTicks)
			{
				expired.add(action);
			}
		}
		expired.sort(ORDER);
		for (QueuedAction action : expired)
		{
			_actions.remove(action.actionName(), action);
		}
		return expired;
	}

	int size()
	{
		return _actions.size();
	}

	record QueuedAction(String actionName, double relevance, boolean skipPrerequisites, BotEvent event, long createdTick, long sequence)
	{
	}
}
