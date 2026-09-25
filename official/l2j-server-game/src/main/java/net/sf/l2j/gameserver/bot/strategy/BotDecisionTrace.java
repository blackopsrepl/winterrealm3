/*
 * Copyright © 2026 winterrealm3 contributors
 *
 * This file is part of winterrealm3.
 *
 * winterrealm3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * winterrealm3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.bot.strategy;

import java.util.ArrayList;
import java.util.List;

/** Bounded per-tick decision explanation. */
public final class BotDecisionTrace
{
	private final int _capacity;
	private final List<BotDecisionTraceEntry> _entries = new ArrayList<>();
	private boolean _truncated;

	BotDecisionTrace(int capacity)
	{
		if (capacity < 1)
		{
			throw new IllegalArgumentException("Bot decision trace capacity must be positive");
		}
		_capacity = capacity;
	}

	void add(long tick, String actionName, double relevance, BotDecisionStep step, String source, String detail)
	{
		if (_entries.size() >= _capacity)
		{
			_truncated = true;
			return;
		}
		_entries.add(new BotDecisionTraceEntry(_entries.size(), tick, actionName, relevance, step, source, (detail == null) ? "" : detail));
	}

	public List<BotDecisionTraceEntry> entries()
	{
		return List.copyOf(_entries);
	}

	public boolean isTruncated()
	{
		return _truncated;
	}
}
