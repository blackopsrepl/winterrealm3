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
package net.sf.l2j.gameserver.bot.profile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.l2j.gameserver.model.base.ClassId;

/**
 * Immutable root of one exact-class behavior policy. Tactical policy data is
 * added to this class without weakening its one-class identity.
 */
public final class BotClassProfile
{
	public static final int SCHEMA_VERSION = 1;

	private final BotClassProfileKey _key;
	private final BotCombatPolicy _combatPolicy;
	private final List<BotSkillPolicy> _skillPolicies;

	public BotClassProfile(int schemaVersion, ClassId classId, String profileName, BotCombatPolicy combatPolicy, List<BotSkillPolicy> skillPolicies)
	{
		if (schemaVersion != SCHEMA_VERSION)
		{
			throw new IllegalArgumentException("Unsupported bot class profile schema version: " + schemaVersion);
		}
		if (combatPolicy == null)
		{
			throw new IllegalArgumentException("Bot class profile combat policy cannot be null");
		}
		if (skillPolicies == null)
		{
			throw new IllegalArgumentException("Bot class profile skill policies cannot be null");
		}

		_key = new BotClassProfileKey(classId, profileName);
		_combatPolicy = combatPolicy;
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

	public List<BotSkillPolicy> getSkillPolicies()
	{
		return _skillPolicies;
	}
}
