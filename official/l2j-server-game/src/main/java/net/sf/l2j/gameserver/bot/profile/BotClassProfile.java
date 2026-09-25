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
package net.sf.l2j.gameserver.bot.profile;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.l2j.gameserver.model.base.ClassId;

/**
 * Immutable root of one exact-class behavior policy. Tactical policy data is
 * added to this class without weakening its one-class identity.
 */
public final class BotClassProfile
{
	public static final int SCHEMA_VERSION = 2;
	private static final List<ClassId> PLAYABLE_CLASSES = Arrays.stream(ClassId.values()).filter(classId -> classId.getRace() != null).toList();

	private final BotClassProfileKey _key;
	private final BotCombatPolicy _combatPolicy;
	private final List<BotClassRelationship> _classRelationships;
	private final Map<ClassId, BotClassRelationship> _classRelationshipsByClass;
	private final List<BotSkillPolicy> _skillPolicies;

	public BotClassProfile(int schemaVersion, ClassId classId, String profileName, BotCombatPolicy combatPolicy, Collection<BotClassRelationship> classRelationships, List<BotSkillPolicy> skillPolicies)
	{
		if (schemaVersion != SCHEMA_VERSION)
		{
			throw new IllegalArgumentException("Unsupported bot class profile schema version: " + schemaVersion);
		}
		if (combatPolicy == null)
		{
			throw new IllegalArgumentException("Bot class profile combat policy cannot be null");
		}
		if (classRelationships == null)
		{
			throw new IllegalArgumentException("Bot class profile relationships cannot be null");
		}
		if (skillPolicies == null)
		{
			throw new IllegalArgumentException("Bot class profile skill policies cannot be null");
		}

		_key = new BotClassProfileKey(classId, profileName);
		_combatPolicy = combatPolicy;
		EnumMap<ClassId, BotClassRelationship> relationshipsByClass = new EnumMap<>(ClassId.class);
		for (BotClassRelationship relationship : classRelationships)
		{
			if (relationship == null)
			{
				throw new IllegalArgumentException("Bot class profile relationships cannot contain null");
			}
			if (relationshipsByClass.putIfAbsent(relationship.classId(), relationship) != null)
			{
				throw new IllegalArgumentException("Duplicate exact class relationship " + relationship.classId() + " in " + _key);
			}
		}
		List<ClassId> missingRelationships = PLAYABLE_CLASSES.stream().filter(relationshipClass -> !relationshipsByClass.containsKey(relationshipClass)).toList();
		if (!missingRelationships.isEmpty())
		{
			throw new IllegalArgumentException("Missing exact class relationships in " + _key + ": " + missingRelationships);
		}
		_classRelationships = PLAYABLE_CLASSES.stream().map(relationshipsByClass::get).toList();
		_classRelationshipsByClass = Map.copyOf(relationshipsByClass);
		Set<String> keys = new HashSet<>();
		for (BotSkillPolicy skillPolicy : skillPolicies)
		{
			if (skillPolicy == null)
			{
				throw new IllegalArgumentException("Bot class profile skill policies cannot contain null");
			}
			if (!keys.add(skillPolicy.key()))
			{
				throw new IllegalArgumentException("Duplicate skill policy key " + skillPolicy.key() + " in " + _key);
			}
		}
		_skillPolicies = List.copyOf(skillPolicies);
	}

	public BotClassProfileKey getKey()
	{
		return _key;
	}

	public ClassId getClassId()
	{
		return _key.classId();
	}

	public String getProfileName()
	{
		return _key.profileName();
	}

	public BotCombatPolicy getCombatPolicy()
	{
		return _combatPolicy;
	}

	/** @return all exact relationships in ClassId declaration order */
	public List<BotClassRelationship> getClassRelationships()
	{
		return _classRelationships;
	}

	/**
	 * Requires this profile's authored knowledge for an exact observed class.
	 * No parent, race, role, or archetype fallback exists.
	 */
	public BotClassRelationship requireClassRelationship(ClassId observedClass)
	{
		BotClassRelationship relationship = _classRelationshipsByClass.get(observedClass);
		if (relationship == null)
		{
			throw new IllegalArgumentException("No exact class relationship for " + observedClass + " in " + _key);
		}
		return relationship;
	}

	public List<BotSkillPolicy> getSkillPolicies()
	{
		return _skillPolicies;
	}

}
