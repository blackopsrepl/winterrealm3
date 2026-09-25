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
package net.sf.l2j.gameserver.bot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe index of loaded bot runtimes. The world remains authoritative
 * for player entities; this registry contains controller state only.
 */
public final class BotRegistry
{
	private final ConcurrentMap<Integer, BotRuntime> _runtimes = new ConcurrentHashMap<>();

	/**
	 * Registers one runtime, rejecting duplicate character ownership.
	 * @param runtime the runtime to register
	 */
	public void register(BotRuntime runtime)
	{
		if (runtime == null)
		{
			throw new IllegalArgumentException("Bot runtime cannot be null");
		}
		BotRuntime existing = _runtimes.putIfAbsent(runtime.getCharacterId(), runtime);
		if (existing != null)
		{
			throw new IllegalStateException("Bot runtime is already registered for character " + runtime.getCharacterId());
		}
	}

	/**
	 * Gets a runtime by authoritative character id.
	 * @param characterId the character object id
	 * @return the runtime, or {@code null} when not loaded
	 */
	public BotRuntime get(int characterId)
	{
		return _runtimes.get(characterId);
	}

	/**
	 * Removes a runtime only when the same instance still owns the entry.
	 * @param runtime the runtime to remove
	 * @return {@code true} when the runtime was removed
	 */
	public boolean unregister(BotRuntime runtime)
	{
		return (runtime != null) && _runtimes.remove(runtime.getCharacterId(), runtime);
	}

	/**
	 * Returns a stable character-id ordered view for scheduling and shutdown.
	 * @return an immutable runtime snapshot
	 */
	public List<BotRuntime> snapshot()
	{
		List<BotRuntime> snapshot = new ArrayList<>(_runtimes.values());
		snapshot.sort(Comparator.comparingInt(BotRuntime::getCharacterId));
		return List.copyOf(snapshot);
	}

	public int size()
	{
		return _runtimes.size();
	}

	public boolean isEmpty()
	{
		return _runtimes.isEmpty();
	}
}
