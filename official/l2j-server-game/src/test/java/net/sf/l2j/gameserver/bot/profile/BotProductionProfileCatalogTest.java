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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

	@Test
	void evaSaintAddsThirdClassSupportWithoutWideningProphecyTargets() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.evaSaint, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		Map<ClassId, Set<Integer>> skillsByRecipient = skillsByRecipient(profile);

		assertEquals(playableClasses(), skillsByRecipient.keySet());
		Set<Integer> duelist = skillsByRecipient.get(ClassId.duelist);
		Set<Integer> mysticMuse = skillsByRecipient.get(ClassId.mysticMuse);
		Set<Integer> doomcryer = skillsByRecipient.get(ClassId.doomcryer);
		assertTrue(duelist.containsAll(Set.of(1068, 1353, 1354, 1397)));
		assertFalse(duelist.contains(1355));
		assertTrue(mysticMuse.containsAll(Set.of(1078, 1303, 1353, 1354, 1355, 1397)));
		assertTrue(doomcryer.containsAll(Set.of(1078, 1353, 1354, 1397)));
		assertFalse(doomcryer.contains(1355));
	}

	@Test
	void shillienElderOwnsMagicAndMeleeSupportWithoutAHealerFallback() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.shillenElder, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		Map<ClassId, Set<Integer>> skillsByRecipient = skillsByRecipient(profile);

		assertEquals(playableClasses(), skillsByRecipient.keySet());
		Set<Integer> duelist = skillsByRecipient.get(ClassId.duelist);
		Set<Integer> mysticMuse = skillsByRecipient.get(ClassId.mysticMuse);
		Set<Integer> hawkeye = skillsByRecipient.get(ClassId.hawkeye);
		assertTrue(duelist.containsAll(Set.of(1035, 1040, 1068, 1077, 1189, 1204, 1240, 1242, 1268, 1392)));
		assertFalse(duelist.contains(1059));
		assertTrue(mysticMuse.containsAll(Set.of(1035, 1040, 1059, 1078, 1189, 1204, 1303, 1392)));
		assertFalse(mysticMuse.contains(1068));
		assertFalse(mysticMuse.contains(1268));
		assertTrue(hawkeye.containsAll(Set.of(1068, 1077, 1240, 1242)));
		assertFalse(hawkeye.contains(1268));

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(Set.of(1015, 1217), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> policy.skillId() == 1401));
	}

	@Test
	void shillienSaintKeepsProphecyOfWindOnExactDaggerAndArcherTargets() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.shillienSaint, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		Map<ClassId, Set<Integer>> skillsByRecipient = skillsByRecipient(profile);

		assertEquals(playableClasses(), skillsByRecipient.keySet());
		assertTrue(skillsByRecipient.get(ClassId.abyssWalker).containsAll(Set.of(1268, 1354, 1357)));
		assertTrue(skillsByRecipient.get(ClassId.ghostSentinel).containsAll(Set.of(1354, 1357)));
		assertFalse(skillsByRecipient.get(ClassId.duelist).contains(1357));
		assertFalse(skillsByRecipient.get(ClassId.phoenixKnight).contains(1357));
		assertTrue(skillsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1059, 1078, 1303, 1354)));

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(Set.of(1015, 1217), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> (policy.skillId() == 1401) || (policy.skillId() == 1428)));
	}

	@Test
	void rechargersUseExactEligibilityAndClassSpecificMpThresholds() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		EnumSet<ClassId> forbidden = EnumSet.of(ClassId.oracle, ClassId.elder, ClassId.evaSaint, ClassId.shillienOracle, ClassId.shillenElder, ClassId.shillienSaint);
		EnumSet<ClassId> eligible = EnumSet.copyOf(playableClasses());
		eligible.removeAll(forbidden);

		for (ClassId caster : List.of(ClassId.elder, ClassId.evaSaint, ClassId.shillenElder, ClassId.shillienSaint))
		{
			BotClassProfile profile = registry.require(caster, "world");
			Map<ClassId, List<BotSkillPolicy>> rechargeByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RECHARGE);
			assertEquals(eligible, rechargeByRecipient.keySet());
			for (ClassId classId : forbidden)
			{
				assertFalse(rechargeByRecipient.containsKey(classId));
			}
			assertRechargePolicy(rechargeByRecipient, ClassId.mysticMuse, 60);
			assertRechargePolicy(rechargeByRecipient, ClassId.duelist, 35);
			assertRechargePolicy(rechargeByRecipient, ClassId.fighter, 20);
		}
	}

	@Test
	void elvenHealersPreserveExactClassDutyInsideSeparateRecoveryBands() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		for (ClassId caster : List.of(ClassId.elder, ClassId.evaSaint))
		{
			BotClassProfile profile = registry.require(caster, "world");
			Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
			assertEquals(playableClasses(), healByRecipient.keySet());
			for (List<BotSkillPolicy> policies : healByRecipient.values())
			{
				assertEquals(Set.of(1015, 1217, 1401), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
			}

			BotSkillPolicy phoenixBattle = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34300);
			BotSkillPolicy phoenixMajor = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1401, 77, 15, 24300);
			assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1217, 91, 25, 14300);
			BotSkillPolicy abyssBattle = assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1015, 25, 5, 31550);
			assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1401, 48, 15, 21550);
			assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1217, 62, 25, 11550);

			assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
			assertTrue(abyssBattle.priority() > (phoenixMajor.priority() + 235));
		}
	}

	private static Map<ClassId, Set<Integer>> skillsByRecipient(BotClassProfile profile)
	{
		Map<ClassId, Set<Integer>> skillsByRecipient = new EnumMap<>(ClassId.class);
		policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF).forEach((recipient, policies) ->
		{
			for (BotSkillPolicy policy : policies)
			{
				skillsByRecipient.computeIfAbsent(recipient, ignored -> new HashSet<>()).add(policy.skillId());
			}
		});
		return skillsByRecipient;
	}

	private static Map<ClassId, List<BotSkillPolicy>> policiesByRecipient(BotClassProfile profile, BotSkillPolicy.Intent intent)
	{
		Map<ClassId, List<BotSkillPolicy>> policiesByRecipient = new EnumMap<>(ClassId.class);
		for (BotSkillPolicy policy : profile.getSkillPolicies())
		{
			if (policy.intent() != intent)
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
			policiesByRecipient.computeIfAbsent(recipient, ignored -> new ArrayList<>()).add(policy);
		}
		return policiesByRecipient;
	}

	private static void assertRechargePolicy(Map<ClassId, List<BotSkillPolicy>> policiesByRecipient, ClassId recipient, int targetThreshold)
	{
		List<BotSkillPolicy> policies = policiesByRecipient.get(recipient);
		assertEquals(1, policies.size());
		BotSkillPolicy policy = policies.get(0);
		assertEquals(1013, policy.skillId());
		assertEquals(BotSkillPolicy.Target.PARTY_LOWEST_MP, policy.target());
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.BELOW) && (resource.value() == targetThreshold)));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.ABOVE) && (resource.value() == 45)));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Distance distance) && (distance.subject() == BotRuleCondition.Subject.TARGET) && (distance.comparison() == BotRuleCondition.Comparison.AT_MOST) && (distance.value() == 1200)));
	}

	private static BotSkillPolicy assertHealPolicy(Map<ClassId, List<BotSkillPolicy>> policiesByRecipient, ClassId recipient, int skillId, int targetThreshold, int selfReserve, int priority)
	{
		BotSkillPolicy policy = policiesByRecipient.get(recipient).stream().filter(candidate -> candidate.skillId() == skillId).findFirst().orElseThrow();
		assertEquals(BotSkillPolicy.Target.PARTY_LOWEST_HP, policy.target());
		assertEquals(priority, policy.priority());
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.TARGET) && (status.status() == BotRuleCondition.StatusType.ALIVE) && status.expected()));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.HP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.BELOW) && (resource.value() == targetThreshold)));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.ABOVE) && (resource.value() == selfReserve)));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Distance distance) && (distance.subject() == BotRuleCondition.Subject.TARGET) && (distance.comparison() == BotRuleCondition.Comparison.AT_MOST) && (distance.value() == 1200)));
		return policy;
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
