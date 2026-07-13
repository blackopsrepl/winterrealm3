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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.sf.l2j.gameserver.bot.tactics.BotProfileBrain;
import net.sf.l2j.gameserver.model.base.ClassId;

class BotProductionProfileCatalogTest
{
	@Test
	void hierophantOwnsDifferentBuffSetsForEveryExactRecipient() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.hierophant, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		Map<ClassId, Set<Integer>> skillsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), skillsByRecipient.keySet());

		Set<Integer> duelist = skillsByRecipient.get(ClassId.duelist);
		Set<Integer> mysticMuse = skillsByRecipient.get(ClassId.mysticMuse);
		Set<Integer> phoenixKnight = skillsByRecipient.get(ClassId.phoenixKnight);
		assertTrue(duelist.containsAll(Set.of(1068, 1077, 1086, 1240, 1242, 1356, 1388)));
		assertFalse(duelist.contains(1085));
		assertTrue(mysticMuse.containsAll(Set.of(1048, 1078, 1085, 1389)));
		assertFalse(mysticMuse.contains(1068));
		assertFalse(mysticMuse.contains(1356));
		assertTrue(phoenixKnight.containsAll(Set.of(1044, 1068, 1243, 1356, 1389)));
		assertFalse(phoenixKnight.contains(1388));
		assertNotEquals(duelist, mysticMuse);
		assertNotEquals(duelist, phoenixKnight);
		assertNotEquals(mysticMuse, phoenixKnight);
	}

	@Test
	void prophetRemainsAnIndependentExactCasterWithoutHierophantSkills() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.prophet, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		Map<ClassId, Set<Integer>> skillsByRecipient = skillsByRecipient(profile);

		assertEquals(playableClasses(), skillsByRecipient.keySet());
		assertTrue(skillsByRecipient.get(ClassId.duelist).containsAll(Set.of(1068, 1077, 1086, 1240, 1242, 1388)));
		assertTrue(skillsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1048, 1078, 1085, 1389)));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> (policy.skillId() == 1352) || (policy.skillId() == 1356)));
	}

	@Test
	void elderTargetsExactRecipientsWithItsOwnSupportKit() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.elder, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		Map<ClassId, Set<Integer>> skillsByRecipient = skillsByRecipient(profile);

		assertEquals(playableClasses(), skillsByRecipient.keySet());
		Set<Integer> duelist = skillsByRecipient.get(ClassId.duelist);
		Set<Integer> mysticMuse = skillsByRecipient.get(ClassId.mysticMuse);
		Set<Integer> phoenixKnight = skillsByRecipient.get(ClassId.phoenixKnight);
		assertTrue(duelist.containsAll(Set.of(1068, 1259, 1397)));
		assertFalse(duelist.contains(1078));
		assertFalse(duelist.contains(1303));
		assertTrue(mysticMuse.containsAll(Set.of(1078, 1259, 1303, 1397)));
		assertFalse(mysticMuse.contains(1068));
		assertTrue(phoenixKnight.containsAll(Set.of(1044, 1068, 1243, 1259, 1304, 1397)));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> (policy.skillId() == 1353) || (policy.skillId() == 1354) || (policy.skillId() == 1355)));
	}

	private static Map<ClassId, Set<Integer>> skillsByRecipient(BotClassProfile profile)
	{
		Map<ClassId, Set<Integer>> skillsByRecipient = new EnumMap<>(ClassId.class);
		for (BotSkillPolicy policy : profile.getSkillPolicies())
		{
			if (policy.intent() != BotSkillPolicy.Intent.BUFF)
			{
				continue;
			}
			ClassId recipient = null;
			for (BotRuleCondition condition : policy.conditions())
			{
				if ((condition instanceof BotRuleCondition.ExactClass exactClass) && (exactClass.subject() == BotRuleCondition.Subject.TARGET) && exactClass.expected())
				{
					assertNull(recipient, () -> policy.key() + " has more than one positive exact recipient");
					recipient = exactClass.classId();
				}
			}
			assertTrue(recipient != null, () -> policy.key() + " has no positive exact recipient");
			skillsByRecipient.computeIfAbsent(recipient, ignored -> new HashSet<>()).add(policy.skillId());
		}
		return skillsByRecipient;
	}

	private static Set<ClassId> playableClasses()
	{
		EnumSet<ClassId> playable = EnumSet.noneOf(ClassId.class);
		for (ClassId classId : ClassId.values())
		{
			if (classId.getRace() != null)
			{
				playable.add(classId);
			}
		}
		return playable;
	}

	private static Path productionDirectory()
	{
		Path moduleSibling = Path.of("..", "l2j-server-datapack", "src", "main", "resources").resolve(BotClassProfileLoader.DATAPACK_DIRECTORY).normalize();
		if (Files.isDirectory(moduleSibling))
		{
			return moduleSibling;
		}
		Path repositoryRoot = Path.of("official", "l2j-server-datapack", "src", "main", "resources").resolve(BotClassProfileLoader.DATAPACK_DIRECTORY).normalize();
		if (Files.isDirectory(repositoryRoot))
		{
			return repositoryRoot;
		}
		throw new IllegalStateException("Could not locate the production exact-class profile catalog");
	}
}
