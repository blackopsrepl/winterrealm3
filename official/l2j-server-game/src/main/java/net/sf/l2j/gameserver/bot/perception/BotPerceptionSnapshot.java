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
package net.sf.l2j.gameserver.bot.perception;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.Group;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.Subject;

/** Complete immutable input to one serialized bot decision tick. */
public final class BotPerceptionSnapshot
{
	private final long _tick;
	private final int _selfObjectId;
	private final int _currentTargetObjectId;
	private final int _ownerObjectId;
	private final int _summonObjectId;
	private final int _partyLeaderObjectId;
	private final Map<Integer, BotObservedCharacter> _characters;
	private final List<Integer> _partyObjectIds;
	private final List<Integer> _clanObjectIds;
	private final List<Integer> _hostileObjectIds;

	public BotPerceptionSnapshot(long tick, int selfObjectId, int currentTargetObjectId, int ownerObjectId, int summonObjectId, int partyLeaderObjectId, Collection<BotObservedCharacter> characters, Collection<Integer> partyObjectIds, Collection<Integer> clanObjectIds, Collection<Integer> hostileObjectIds)
	{
		if (tick <= 0)
		{
			throw new IllegalArgumentException("Perception tick must be positive");
		}
		if (selfObjectId <= 0)
		{
			throw new IllegalArgumentException("Perception self id must be positive");
		}
		requireOptionalId(currentTargetObjectId, "current target");
		requireOptionalId(ownerObjectId, "owner");
		requireOptionalId(summonObjectId, "summon");
		requireOptionalId(partyLeaderObjectId, "party leader");
		if (characters == null)
		{
			throw new IllegalArgumentException("Perception characters cannot be null");
		}

		Map<Integer, BotObservedCharacter> indexed = new LinkedHashMap<>();
		for (BotObservedCharacter character : characters.stream().sorted((left, right) -> Integer.compare(left.objectId(), right.objectId())).toList())
		{
			if (indexed.putIfAbsent(character.objectId(), character) != null)
			{
				throw new IllegalArgumentException("Duplicate perceived character " + character.objectId());
			}
		}
		if (!indexed.containsKey(selfObjectId))
		{
			throw new IllegalArgumentException("Perception does not contain its self character " + selfObjectId);
		}

		_tick = tick;
		_selfObjectId = selfObjectId;
		_currentTargetObjectId = requireObservedOptional(indexed, currentTargetObjectId, "current target");
		_ownerObjectId = requireObservedOptional(indexed, ownerObjectId, "owner");
		_summonObjectId = requireObservedOptional(indexed, summonObjectId, "summon");
		_partyLeaderObjectId = requireObservedOptional(indexed, partyLeaderObjectId, "party leader");
		_characters = Collections.unmodifiableMap(indexed);
		_partyObjectIds = normalizedIds(indexed, partyObjectIds, "party");
		_clanObjectIds = normalizedIds(indexed, clanObjectIds, "clan");
		_hostileObjectIds = normalizedIds(indexed, hostileObjectIds, "hostile");
	}

	public long tick()
	{
		return _tick;
	}

	public int selfObjectId()
	{
		return _selfObjectId;
	}

	public int currentTargetObjectId()
	{
		return _currentTargetObjectId;
	}

	public int ownerObjectId()
	{
		return _ownerObjectId;
	}

	public int summonObjectId()
	{
		return _summonObjectId;
	}

	public int partyLeaderObjectId()
	{
		return _partyLeaderObjectId;
	}

	public BotObservedCharacter self()
	{
		return _characters.get(_selfObjectId);
	}

	public Optional<BotObservedCharacter> find(int objectId)
	{
		return Optional.ofNullable(_characters.get(objectId));
	}

	public Optional<BotObservedCharacter> subject(Subject subject, int resolvedTargetObjectId)
	{
		if (subject == null)
		{
			throw new IllegalArgumentException("Perception subject cannot be null");
		}
		return find(switch (subject)
		{
			case SELF -> _selfObjectId;
			case TARGET -> resolvedTargetObjectId;
			case OWNER -> _ownerObjectId;
			case SUMMON -> _summonObjectId;
		});
	}

	public List<Integer> group(Group group)
	{
		if (group == null)
		{
			throw new IllegalArgumentException("Perception group cannot be null");
		}
		return switch (group)
		{
			case PARTY -> _partyObjectIds;
			case CLAN -> _clanObjectIds;
			case NEARBY_HOSTILES -> _hostileObjectIds;
		};
	}

	public Collection<BotObservedCharacter> characters()
	{
		return _characters.values();
	}

	private static List<Integer> normalizedIds(Map<Integer, BotObservedCharacter> characters, Collection<Integer> ids, String label)
	{
		if (ids == null)
		{
			throw new IllegalArgumentException("Perception " + label + " ids cannot be null");
		}
		List<Integer> normalized = ids.stream().distinct().sorted().toList();
		for (Integer id : normalized)
		{
			if ((id == null) || !characters.containsKey(id))
			{
				throw new IllegalArgumentException("Perception " + label + " character is not observed: " + id);
			}
		}
		return normalized;
	}

	private static int requireObservedOptional(Map<Integer, BotObservedCharacter> characters, int objectId, String label)
	{
		if ((objectId != 0) && !characters.containsKey(objectId))
		{
			throw new IllegalArgumentException("Perception " + label + " character is not observed: " + objectId);
		}
		return objectId;
	}

	private static void requireOptionalId(int objectId, String label)
	{
		if (objectId < 0)
		{
			throw new IllegalArgumentException("Perception " + label + " id cannot be negative");
		}
	}
}
