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
	void clericUsesItsOwnExactHealAndBuffCoverage() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.cleric, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(Set.of(1011, 1015), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		BotSkillPolicy phoenixBattle = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34300);
		BotSkillPolicy phoenixEfficient = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1011, 91, 25, 14300);
		BotSkillPolicy abyssBattle = assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1015, 25, 5, 31550);
		assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1011, 62, 25, 11550);
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
		assertTrue(abyssBattle.priority() > (phoenixEfficient.priority() + 235));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027), groupHeals.keySet());
		assertTrue(groupHeals.get(1027).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 45) && (count.radius() == 1000)));

		Map<ClassId, Set<Integer>> buffsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), buffsByRecipient.keySet());
		assertTrue(buffsByRecipient.get(ClassId.duelist).containsAll(Set.of(1068, 1077)));
		assertFalse(buffsByRecipient.get(ClassId.duelist).contains(1085));
		assertTrue(buffsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1078, 1085)));
		assertFalse(buffsByRecipient.get(ClassId.mysticMuse).contains(1068));
		assertTrue(buffsByRecipient.get(ClassId.phoenixKnight).containsAll(Set.of(1044, 1068)));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertEquals(178, resurrectionByRecipient.values().stream().mapToInt(List::size).sum());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 35, 23100);
		BotSkillPolicy phoenixBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, true, 35, 22400);
		BotSkillPolicy phoenixSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, false, 25, 20400);
		assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, true, 35, 19250);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 25, 17250);
		assertTrue(cardinalBattlefield.priority() > (phoenixBattlefield.priority() + 80));
		assertTrue(phoenixSafe.priority() > (abyssSafe.priority() + 80));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1012, 1013, 1043).contains(policy.skillId())));
	}

	@Test
	void oracleAddsExactRechargeAgilityAndWeightSupport() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.oracle, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(Set.of(1011, 1015), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34300);
		assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1011, 62, 25, 11550);

		Map<ClassId, Set<Integer>> buffsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), buffsByRecipient.keySet());
		assertTrue(buffsByRecipient.get(ClassId.abyssWalker).containsAll(Set.of(1033, 1068, 1087)));
		assertTrue(buffsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1033, 1078)));
		assertFalse(buffsByRecipient.get(ClassId.mysticMuse).contains(1068));
		assertFalse(buffsByRecipient.get(ClassId.mysticMuse).contains(1087));
		assertTrue(buffsByRecipient.get(ClassId.fortuneSeeker).containsAll(Set.of(1068, 1257)));
		assertFalse(buffsByRecipient.get(ClassId.duelist).contains(1257));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertEquals(178, resurrectionByRecipient.values().stream().mapToInt(List::size).sum());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 45, 23300);
		BotSkillPolicy evaSaintBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.evaSaint, true, 45, 23200);
		assertResurrectionPolicy(resurrectionByRecipient, ClassId.dreadnought, true, 45, 21200);
		BotSkillPolicy phoenixSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, false, 35, 20500);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 35, 17150);
		assertTrue(cardinalBattlefield.priority() > (evaSaintBattlefield.priority() + 80));
		assertTrue(phoenixSafe.priority() > (abyssSafe.priority() + 80));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1012, 1043, 1259, 1303, 1397).contains(policy.skillId())));
	}

	@Test
	void shillienOracleOwnsDifferentExactMagicAndMeleeSupport() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.shillienOracle, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(Set.of(1011, 1015), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}

		Map<ClassId, Set<Integer>> buffsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), buffsByRecipient.keySet());
		assertTrue(buffsByRecipient.get(ClassId.duelist).containsAll(Set.of(1068, 1077, 1189, 1268)));
		assertFalse(buffsByRecipient.get(ClassId.duelist).contains(1059));
		assertTrue(buffsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1059, 1078, 1189)));
		assertFalse(buffsByRecipient.get(ClassId.mysticMuse).contains(1068));
		assertFalse(buffsByRecipient.get(ClassId.mysticMuse).contains(1268));
		assertTrue(buffsByRecipient.get(ClassId.hawkeye).containsAll(Set.of(1068, 1077, 1189)));
		assertFalse(buffsByRecipient.get(ClassId.hawkeye).contains(1268));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1012, 1016, 1043, 1087, 1240, 1242, 1257, 1303).contains(policy.skillId())));
	}

	@Test
	void bishopUsesItsOwnEmergencyAndGroupHealingLanes() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.bishop, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(Set.of(1217, 1218, 1401), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		BotSkillPolicy phoenixBattle = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1218, 46, 5, 34300);
		BotSkillPolicy abyssBattle = assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1218, 25, 5, 31550);
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> buffByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		BotSkillPolicy phoenixCelestial = buffByRecipient.get(ClassId.phoenixKnight).stream().filter(policy -> policy.skillId() == 1418).findFirst().orElseThrow();
		BotSkillPolicy abyssCelestial = buffByRecipient.get(ClassId.abyssWalker).stream().filter(policy -> policy.skillId() == 1418).findFirst().orElseThrow();
		assertEquals(44300, phoenixCelestial.priority());
		assertEquals(41550, abyssCelestial.priority());
		assertTrue(phoenixCelestial.priority() > (abyssCelestial.priority() + 235));
		assertTrue(phoenixCelestial.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.HP) && (resource.value() == 56)));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027, 1219, 1271, 1402), groupHeals.keySet());
		assertTrue(groupHeals.get(1271).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.comparison() == BotRuleCondition.Comparison.AT_LEAST) && (count.count() == 4) && (count.threshold() == 35) && (count.radius() == 1000)));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> policy.skillId() == 1015));
	}

	@Test
	void cardinalAddsExactCleanseAndBalanceLifeDecisions() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.cardinal, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(Set.of(1217, 1218, 1401), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}

		Map<ClassId, List<BotSkillPolicy>> cureByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), cureByRecipient.keySet());
		for (List<BotSkillPolicy> policies : cureByRecipient.values())
		{
			assertEquals(4, policies.size());
			assertTrue(policies.stream().allMatch(policy -> policy.skillId() == 1409));
			Set<BotRuleCondition.StatusType> statuses = policies.stream().flatMap(policy -> policy.conditions().stream()).filter(BotRuleCondition.Status.class::isInstance).map(BotRuleCondition.Status.class::cast).filter(status -> status.subject() == BotRuleCondition.Subject.TARGET).map(BotRuleCondition.Status::status).filter(status -> status != BotRuleCondition.StatusType.ALIVE).collect(Collectors.toSet());
			assertEquals(Set.of(BotRuleCondition.StatusType.STUNNED, BotRuleCondition.StatusType.SILENCED, BotRuleCondition.StatusType.MUTED, BotRuleCondition.StatusType.ROOTED), statuses);
		}
		BotSkillPolicy phoenixStun = cureByRecipient.get(ClassId.phoenixKnight).stream().filter(policy -> policy.key().endsWith("cleanse-stun")).findFirst().orElseThrow();
		BotSkillPolicy abyssStun = cureByRecipient.get(ClassId.abyssWalker).stream().filter(policy -> policy.key().endsWith("cleanse-stun")).findFirst().orElseThrow();
		assertEquals(40300, phoenixStun.priority());
		assertEquals(37550, abyssStun.priority());
		assertTrue(phoenixStun.priority() > (abyssStun.priority() + 235));

		BotSkillPolicy balanceLife = profile.getSkillPolicies().stream().filter(policy -> policy.skillId() == 1335).findFirst().orElseThrow();
		assertEquals(BotSkillPolicy.Intent.GROUP_HEAL, balanceLife.intent());
		assertTrue(balanceLife.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.filter() == BotRuleCondition.GroupFilter.ALIVE) && (count.count() == 3)));
		assertTrue(balanceLife.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 1) && (count.threshold() == 35)));
		assertTrue(skillsByRecipient(profile).get(ClassId.abyssWalker).contains(1353));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> (policy.skillId() == 1013) || (policy.skillId() == 1426)));
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
	void swordSingerMaintainsSongsForExactPartyBeneficiaries() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.swordSinger, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, Set<Integer>> songsByRecipient = groupSkillsByRecipient(profile, BotSkillPolicy.Intent.SONG);
		assertEquals(playableClasses(), songsByRecipient.keySet());
		for (Set<Integer> songs : songsByRecipient.values())
		{
			assertTrue(songs.containsAll(Set.of(264, 265, 267, 268, 304)));
		}

		Set<Integer> phoenixKnight = songsByRecipient.get(ClassId.phoenixKnight);
		Set<Integer> abyssWalker = songsByRecipient.get(ClassId.abyssWalker);
		Set<Integer> mysticMuse = songsByRecipient.get(ClassId.mysticMuse);
		assertTrue(phoenixKnight.containsAll(Set.of(269, 305)));
		assertFalse(phoenixKnight.contains(266));
		assertTrue(abyssWalker.containsAll(Set.of(266, 269)));
		assertFalse(abyssWalker.contains(305));
		assertFalse(mysticMuse.contains(266));
		assertFalse(mysticMuse.contains(269));
		assertFalse(mysticMuse.contains(305));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(270, 306, 308).contains(policy.skillId())));
	}

	@Test
	void swordMuseOwnsThirdClassSongsForExactSkillUsers() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.swordMuse, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, Set<Integer>> songsByRecipient = groupSkillsByRecipient(profile, BotSkillPolicy.Intent.SONG);
		assertEquals(playableClasses(), songsByRecipient.keySet());
		assertTrue(songsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(349, 363)));
		assertFalse(songsByRecipient.get(ClassId.mysticMuse).contains(364));
		assertTrue(songsByRecipient.get(ClassId.cardinal).containsAll(Set.of(349, 363)));
		assertFalse(songsByRecipient.get(ClassId.cardinal).contains(364));
		assertTrue(songsByRecipient.get(ClassId.adventurer).contains(364));
		assertFalse(songsByRecipient.get(ClassId.adventurer).contains(349));
		assertFalse(songsByRecipient.get(ClassId.adventurer).contains(363));
		assertTrue(songsByRecipient.get(ClassId.phoenixKnight).containsAll(Set.of(305, 364)));
		assertFalse(songsByRecipient.get(ClassId.hierophant).contains(364));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> policy.skillId() == 437));
	}

	@Test
	void bladedancerMaintainsDifferentDanceSetsForExactBeneficiaries() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.bladedancer, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, Set<Integer>> dancesByRecipient = groupSkillsByRecipient(profile, BotSkillPolicy.Intent.DANCE);
		assertEquals(playableClasses(), dancesByRecipient.keySet());
		assertTrue(dancesByRecipient.get(ClassId.duelist).containsAll(Set.of(271, 272, 274, 275, 310)));
		assertFalse(dancesByRecipient.get(ClassId.duelist).contains(273));
		assertFalse(dancesByRecipient.get(ClassId.duelist).contains(276));
		assertTrue(dancesByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(273, 276)));
		assertFalse(dancesByRecipient.get(ClassId.mysticMuse).contains(271));
		assertFalse(dancesByRecipient.get(ClassId.mysticMuse).contains(310));
		assertEquals(Set.of(276), dancesByRecipient.get(ClassId.hierophant));
		assertTrue(dancesByRecipient.get(ClassId.hawkeye).containsAll(Set.of(271, 272, 274, 275)));
		assertFalse(dancesByRecipient.get(ClassId.hawkeye).contains(310));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(277, 307, 309, 311).contains(policy.skillId())));
	}

	@Test
	void spectralDancerLimitsSirensDanceToExactMagicDamageClasses() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.spectralDancer, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, Set<Integer>> dancesByRecipient = groupSkillsByRecipient(profile, BotSkillPolicy.Intent.DANCE);
		assertEquals(playableClasses(), dancesByRecipient.keySet());
		assertTrue(dancesByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(273, 276, 365)));
		assertTrue(dancesByRecipient.get(ClassId.stormScreamer).contains(365));
		assertTrue(dancesByRecipient.get(ClassId.cardinal).containsAll(Set.of(273, 276)));
		assertFalse(dancesByRecipient.get(ClassId.cardinal).contains(365));
		assertTrue(dancesByRecipient.get(ClassId.elementalMaster).containsAll(Set.of(273, 276)));
		assertFalse(dancesByRecipient.get(ClassId.elementalMaster).contains(365));
		assertFalse(dancesByRecipient.get(ClassId.duelist).contains(365));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(366, 367).contains(policy.skillId())));
	}

	@Test
	void warcryerChantsForExactBeneficiariesWithoutOverwritingBuffFamilies() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.warcryer, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, Set<Integer>> chantsByRecipient = groupSkillsByRecipient(profile, BotSkillPolicy.Intent.CHANT);
		assertEquals(playableClasses(), chantsByRecipient.keySet());
		assertTrue(chantsByRecipient.get(ClassId.duelist).containsAll(Set.of(1006, 1007, 1009, 1251, 1253, 1308, 1309, 1310, 1390)));
		assertFalse(chantsByRecipient.get(ClassId.duelist).contains(1002));
		assertFalse(chantsByRecipient.get(ClassId.duelist).contains(1252));
		assertFalse(chantsByRecipient.get(ClassId.duelist).contains(1391));
		assertTrue(chantsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1002, 1006, 1009, 1391)));
		assertFalse(chantsByRecipient.get(ClassId.mysticMuse).contains(1007));
		assertFalse(chantsByRecipient.get(ClassId.mysticMuse).contains(1390));
		assertTrue(chantsByRecipient.get(ClassId.abyssWalker).containsAll(Set.of(1007, 1251, 1252, 1253, 1308, 1309, 1310, 1390)));
		assertFalse(chantsByRecipient.get(ClassId.abyssWalker).contains(1284));
		assertTrue(chantsByRecipient.get(ClassId.phoenixKnight).containsAll(Set.of(1007, 1251, 1253, 1284, 1308, 1309, 1310, 1391)));
		assertFalse(chantsByRecipient.get(ClassId.phoenixKnight).contains(1390));

		BotSkillPolicy duelistBattle = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("duelist-chant-of-battle")).findFirst().orElseThrow();
		BotRuleCondition.GroupClassCount battleFamily = duelistBattle.conditions().stream().filter(BotRuleCondition.GroupClassCount.class::isInstance).map(BotRuleCondition.GroupClassCount.class::cast).findFirst().orElseThrow();
		assertEquals(List.of(1003, 1007, 1068), battleFamily.effectSkillIds());
		BotSkillPolicy duelistWar = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("duelist-war-chant")).findFirst().orElseThrow();
		BotRuleCondition.GroupClassCount greaterFamily = duelistWar.conditions().stream().filter(BotRuleCondition.GroupClassCount.class::isInstance).map(BotRuleCondition.GroupClassCount.class::cast).findFirst().orElseThrow();
		assertEquals(List.of(1388, 1389, 1390, 1391), greaterFamily.effectSkillIds());
		assertTrue(profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.CHANT).allMatch(policy -> policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 30))));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> policy.skillId() == 1229));
	}

	@Test
	void doomcryerPartitionsThirdClassChantsByExactRecipient() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.doomcryer, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, Set<Integer>> chantsByRecipient = groupSkillsByRecipient(profile, BotSkillPolicy.Intent.CHANT);
		assertEquals(playableClasses(), chantsByRecipient.keySet());
		for (Set<Integer> chants : chantsByRecipient.values())
		{
			assertTrue(chants.contains(1362));
			assertEquals(1, chants.stream().filter(skillId -> (skillId == 1363) || (skillId == 1413)).count());
		}
		assertTrue(chantsByRecipient.get(ClassId.duelist).containsAll(Set.of(1007, 1363)));
		assertFalse(chantsByRecipient.get(ClassId.duelist).contains(1413));
		assertTrue(chantsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1002, 1413)));
		assertFalse(chantsByRecipient.get(ClassId.mysticMuse).contains(1363));
		assertTrue(chantsByRecipient.get(ClassId.cardinal).contains(1413));
		assertTrue(chantsByRecipient.get(ClassId.doomcryer).contains(1363));

		BotSkillPolicy mysticMagnus = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("mystic-muse-magnus-chant")).findFirst().orElseThrow();
		BotRuleCondition.GroupClassCount prophecyFamily = mysticMagnus.conditions().stream().filter(BotRuleCondition.GroupClassCount.class::isInstance).map(BotRuleCondition.GroupClassCount.class::cast).findFirst().orElseThrow();
		assertEquals(List.of(1355, 1356, 1357, 1363, 1413, 1414), prophecyFamily.effectSkillIds());
		assertTrue(mysticMagnus.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 45)));
		BotSkillPolicy cardinalSpirit = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("cardinal-chant-of-spirit")).findFirst().orElseThrow();
		BotRuleCondition.GroupClassCount spiritCount = cardinalSpirit.conditions().stream().filter(BotRuleCondition.GroupClassCount.class::isInstance).map(BotRuleCondition.GroupClassCount.class::cast).findFirst().orElseThrow();
		assertEquals(900, spiritCount.radius());
		assertTrue(cardinalSpirit.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 40)));
	}

	@Test
	void rechargersUseExactEligibilityAndClassSpecificMpThresholds() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		EnumSet<ClassId> forbidden = EnumSet.of(ClassId.oracle, ClassId.elder, ClassId.evaSaint, ClassId.shillienOracle, ClassId.shillenElder, ClassId.shillienSaint);
		EnumSet<ClassId> eligible = EnumSet.copyOf(playableClasses());
		eligible.removeAll(forbidden);

		for (ClassId caster : List.of(ClassId.oracle, ClassId.shillienOracle, ClassId.elder, ClassId.evaSaint, ClassId.shillenElder, ClassId.shillienSaint))
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

	private static Map<ClassId, Set<Integer>> groupSkillsByRecipient(BotClassProfile profile, BotSkillPolicy.Intent intent)
	{
		Map<ClassId, Set<Integer>> skillsByRecipient = new EnumMap<>(ClassId.class);
		for (BotSkillPolicy policy : profile.getSkillPolicies())
		{
			if (policy.intent() != intent)
			{
				continue;
			}
			assertEquals(BotSkillPolicy.Target.SELF, policy.target());
			List<BotRuleCondition.GroupClassCount> counts = policy.conditions().stream().filter(BotRuleCondition.GroupClassCount.class::isInstance).map(BotRuleCondition.GroupClassCount.class::cast).toList();
			assertEquals(1, counts.size(), () -> policy.key() + " must name one exact group beneficiary");
			BotRuleCondition.GroupClassCount count = counts.get(0);
			assertEquals(BotRuleCondition.Group.PARTY, count.group());
			assertTrue(count.effectSkillIds().contains(policy.skillId()));
			assertEquals(BotRuleCondition.EffectState.ABSENT, count.effectState());
			assertEquals(BotRuleCondition.Comparison.AT_LEAST, count.comparison());
			assertEquals(1, count.count());
			assertTrue((count.radius() > 0) && (count.radius() <= BotRuleCondition.MAXIMUM_RANGE));
			List<BotRuleCondition.Resource> reserves = policy.conditions().stream().filter(BotRuleCondition.Resource.class::isInstance).map(BotRuleCondition.Resource.class::cast).filter(resource -> (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.ABOVE)).toList();
			assertEquals(1, reserves.size(), () -> policy.key() + " must preserve one explicit MP reserve");
			assertTrue((reserves.get(0).value() >= 0) && (reserves.get(0).value() < 100));
			skillsByRecipient.computeIfAbsent(count.classId(), ignored -> new HashSet<>()).add(policy.skillId());
		}
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

	private static BotSkillPolicy assertResurrectionPolicy(Map<ClassId, List<BotSkillPolicy>> policiesByRecipient, ClassId recipient, boolean battlefield, int selfReserve, int priority)
	{
		BotSkillPolicy policy = policiesByRecipient.get(recipient).stream().filter(candidate -> candidate.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.SELF) && (status.status() == BotRuleCondition.StatusType.IN_COMBAT) && (status.expected() == battlefield))).findFirst().orElseThrow();
		assertEquals(1016, policy.skillId());
		assertEquals(BotSkillPolicy.Target.PARTY_DEAD_HIGHEST_LEVEL, policy.target());
		assertEquals(priority, policy.priority());
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.TARGET) && (status.status() == BotRuleCondition.StatusType.ALIVE) && !status.expected()));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.SELF) && (status.status() == BotRuleCondition.StatusType.ALIVE) && status.expected()));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.ABOVE) && (resource.value() == selfReserve)));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Distance distance) && (distance.subject() == BotRuleCondition.Subject.TARGET) && (distance.comparison() == BotRuleCondition.Comparison.AT_MOST) && (distance.value() == 900)));
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
