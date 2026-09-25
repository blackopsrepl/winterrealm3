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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.l2j.gameserver.model.base.ClassId;

/**
 * Immutable exact-key profile registry. It deliberately exposes no parent,
 * role, race, or archetype lookup.
 */
public final class BotClassProfileRegistry
{
	private final Map<BotClassProfileKey, BotClassProfile> _profiles;
	private final Set<ClassId> _coveredClasses;

	public BotClassProfileRegistry(Collection<BotClassProfile> profiles)
	{
		if (profiles == null)
		{
			throw new IllegalArgumentException("Bot profile collection cannot be null");
		}

		Map<BotClassProfileKey, BotClassProfile> indexedProfiles = new LinkedHashMap<>();
		EnumSet<ClassId> coveredClasses = EnumSet.noneOf(ClassId.class);
		for (BotClassProfile profile : profiles)
		{
			if (profile == null)
			{
				throw new IllegalArgumentException("Bot profile collection cannot contain null");
			}
			BotClassProfile existing = indexedProfiles.putIfAbsent(profile.getKey(), profile);
			if (existing != null)
			{
				throw new IllegalArgumentException("Duplicate bot profile: " + profile.getKey());
			}
			coveredClasses.add(profile.getClassId());
		}

		_profiles = Collections.unmodifiableMap(indexedProfiles);
		_coveredClasses = Collections.unmodifiableSet(coveredClasses);
	}

	/**
	 * Resolves only an exact class and normalized profile-name match.
	 * @param classId the player's active class
	 * @param profileName the persisted profile variant
	 * @return the exact profile
	 * @throws IllegalArgumentException when no exact profile exists
	 */
	public BotClassProfile require(ClassId classId, String profileName)
	{
		BotClassProfileKey key = new BotClassProfileKey(classId, profileName);
		BotClassProfile profile = _profiles.get(key);
		if (profile == null)
		{
			throw new IllegalArgumentException("No exact bot profile for class " + classId + " and name " + key.profileName());
		}
		return profile;
	}

	/**
	 * Lists playable classes that have no explicitly registered profile.
	 * @return immutable class-id ordered missing coverage
	 */
	public List<ClassId> missingPlayableClasses()
	{
		List<ClassId> missing = new ArrayList<>();
		for (ClassId classId : ClassId.values())
		{
			if ((classId.getRace() != null) && !_coveredClasses.contains(classId))
			{
				missing.add(classId);
			}
		}
		return List.copyOf(missing);
	}

	/**
	 * Requires explicit coverage for every playable Interlude class.
	 */
	public void requireCompletePlayableCoverage()
	{
		List<ClassId> missing = missingPlayableClasses();
		if (!missing.isEmpty())
		{
			throw new IllegalStateException("Missing exact bot profiles for playable classes: " + missing);
		}
	}

	public int size()
	{
		return _profiles.size();
	}

	/** @return every profile in deterministic load order */
	public List<BotClassProfile> getProfiles()
	{
		return List.copyOf(_profiles.values());
	}
}
