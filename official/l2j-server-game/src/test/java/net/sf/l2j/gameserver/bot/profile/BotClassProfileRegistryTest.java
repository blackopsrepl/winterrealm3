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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.sf.l2j.gameserver.model.base.ClassId;

class BotClassProfileRegistryTest
{
	@Test
	void resolvesOnlyTheExactClassAndNamedVariant()
	{
		BotClassProfile bishop = profile(ClassId.bishop, "bishop.party");
		BotClassProfile cardinal = profile(ClassId.cardinal, "cardinal.party");
		BotClassProfileRegistry registry = new BotClassProfileRegistry(List.of(bishop, cardinal));

		assertSame(bishop, registry.require(ClassId.bishop, " BISHOP.PARTY "));
		assertSame(cardinal, registry.require(ClassId.cardinal, "cardinal.party"));
		assertThrows(IllegalArgumentException.class, () -> registry.require(ClassId.cardinal, "bishop.party"));
		assertThrows(IllegalArgumentException.class, () -> registry.require(ClassId.bishop, "cardinal.party"));
	}

	@Test
	void rejectsDuplicateExactKeys()
	{
		BotClassProfile first = profile(ClassId.elder, "elder.party");
		BotClassProfile duplicate = profile(ClassId.elder, " ELDER.PARTY ");

		assertThrows(IllegalArgumentException.class, () -> new BotClassProfileRegistry(List.of(first, duplicate)));
	}

	@Test
	void reportsEveryUncoveredPlayableClassAndExcludesDummyEntries()
	{
		BotClassProfileRegistry registry = new BotClassProfileRegistry(List.of(profile(ClassId.fighter, "fighter.default")));
		List<ClassId> missing = registry.missingPlayableClasses();

		assertFalse(missing.contains(ClassId.fighter));
		assertTrue(missing.contains(ClassId.bishop));
		assertFalse(missing.contains(ClassId.dummyEntry1));
		assertThrows(IllegalStateException.class, registry::requireCompletePlayableCoverage);
	}

	@Test
	void acceptsExplicitProfilesForTheEntirePlayableClassSet()
	{
		List<BotClassProfile> profiles = Arrays.stream(ClassId.values()).filter(classId -> classId.getRace() != null).map(classId -> profile(classId, classId.name() + ".coverage-test")).toList();
		BotClassProfileRegistry registry = new BotClassProfileRegistry(profiles);

		registry.requireCompletePlayableCoverage();
		assertEquals(profiles.size(), registry.size());
		assertTrue(registry.missingPlayableClasses().isEmpty());
	}

	private static BotClassProfile profile(ClassId classId, String name)
	{
		return new BotClassProfile(BotClassProfile.SCHEMA_VERSION, classId, name, new BotCombatPolicy(BotCombatPolicy.Targeting.NONE, 0, 0, 0, 0), completeRelationships(), List.of());
	}

	private static List<BotClassRelationship> completeRelationships()
	{
		return Arrays.stream(ClassId.values()).filter(classId -> classId.getRace() != null).map(classId -> new BotClassRelationship(classId, 100, 100, 100, 100, 100, 100)).toList();
	}
}
