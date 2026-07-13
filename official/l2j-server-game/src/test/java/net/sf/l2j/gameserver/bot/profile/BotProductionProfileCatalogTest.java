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
	void everyCurrentProfileOwnsOneCompleteDistinctAllClassMatrix() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		Set<List<BotClassRelationship>> distinctMatrices = new HashSet<>();
		for (BotClassProfile profile : registry.getProfiles())
		{
			List<BotClassRelationship> relationships = profile.getClassRelationships();
			assertEquals(playableClasses(), relationships.stream().map(BotClassRelationship::classId).collect(Collectors.toCollection(() -> EnumSet.noneOf(ClassId.class))), () -> profile.getKey() + " must know every playable exact class");
			assertEquals(89, relationships.size());
			assertTrue(relationships.stream().flatMapToInt(relationship -> java.util.stream.IntStream.of(relationship.allySupportPriority(), relationship.allyProtectionPriority(), relationship.hostileAttackPriority(), relationship.hostileThreatPriority(), relationship.hostileControlPriority(), relationship.hostileInterruptPriority())).allMatch(priority -> (priority >= BotClassRelationship.MINIMUM_PRIORITY) && (priority <= BotClassRelationship.MAXIMUM_PRIORITY)));
			assertTrue(distinctMatrices.add(relationships), () -> profile.getKey() + " shares another exact class's matrix");
		}
		assertEquals(registry.size(), distinctMatrices.size());
	}

	@Test
	void hierophantOwnsCompleteExactPartySupportContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.hierophant, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		Map<ClassId, List<BotSkillPolicy>> buffsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		assertEquals(playableClasses(), buffsByRecipient.keySet());
		Set<Integer> universalBuffs = Set.of(1032, 1033, 1035, 1036, 1040, 1045, 1204, 1352);
		assertTrue(buffsByRecipient.values().stream().allMatch(policies -> policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()).containsAll(universalBuffs)));

		Set<Integer> duelist = buffsByRecipient.get(ClassId.duelist).stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet());
		Set<Integer> mysticMuse = buffsByRecipient.get(ClassId.mysticMuse).stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet());
		Set<Integer> phoenixKnight = buffsByRecipient.get(ClassId.phoenixKnight).stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet());
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
		BotSkillPolicy phoenixShield = assertBuffPolicy(buffsByRecipient, ClassId.phoenixKnight, 1040, 30, 9963);
		BotSkillPolicy abyssShield = assertBuffPolicy(buffsByRecipient, ClassId.abyssWalker, 1040, 30, 9658);
		assertBuffPolicy(buffsByRecipient, ClassId.phoenixKnight, 1352, 35, 9663);
		assertBuffPolicy(buffsByRecipient, ClassId.phoenixKnight, 1356, 45, 10463);
		assertTrue(phoenixShield.priority() > abyssShield.priority());

		Map<ClassId, List<BotSkillPolicy>> healsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healsByRecipient.keySet());
		assertTrue(healsByRecipient.values().stream().allMatch(policies -> (policies.size() == 2) && policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()).equals(Set.of(1011, 1015))));
		BotSkillPolicy phoenixBattle = assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34820);
		BotSkillPolicy phoenixEfficient = assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1011, 91, 25, 14820);
		BotSkillPolicy abyssBattle = assertHealPolicy(healsByRecipient, ClassId.abyssWalker, 1015, 25, 5, 32100);
		assertHealPolicy(healsByRecipient, ClassId.abyssWalker, 1011, 62, 25, 12100);
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
		assertTrue(abyssBattle.priority() > (phoenixEfficient.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> curesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), curesByRecipient.keySet());
		assertTrue(curesByRecipient.values().stream().allMatch(policies -> (policies.size() == 1) && (policies.get(0).skillId() == 1012)));
		BotSkillPolicy cardinalCure = assertCurePolicy(curesByRecipient, ClassId.cardinal, 20, 31500);
		BotSkillPolicy phoenixCure = assertCurePolicy(curesByRecipient, ClassId.phoenixKnight, 20, 30320);
		BotSkillPolicy abyssCure = assertCurePolicy(curesByRecipient, ClassId.abyssWalker, 20, 27600);
		assertTrue(phoenixCure.priority() > (abyssCure.priority() + 235));
		assertTrue(abyssBattle.priority() > cardinalCure.priority());

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027), groupHeals.keySet());
		assertTrue(groupHeals.get(1027).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 45) && (count.radius() == 1000)));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertEquals(178, resurrectionByRecipient.values().stream().mapToInt(List::size).sum());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 50, 25000);
		BotSkillPolicy phoenixBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, true, 50, 23820);
		BotSkillPolicy phoenixSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, false, 40, 21820);
		assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, true, 50, 21100);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 40, 19100);
		assertTrue(cardinalBattlefield.priority() > (phoenixBattlefield.priority() + 80));
		assertTrue(phoenixSafe.priority() > (abyssSafe.priority() + 80));
		assertTrue(abyssBattle.priority() > (cardinalBattlefield.priority() + 235));

		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1043, 1073, 1182, 1189, 1191, 1392, 1393, 1411, 1412).contains(policy.skillId())));
	}

	@Test
	void prophetOwnsCompleteExactPartySupportContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.prophet, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());

		Map<ClassId, List<BotSkillPolicy>> buffsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		assertEquals(playableClasses(), buffsByRecipient.keySet());
		Set<Integer> universalBuffs = Set.of(1032, 1033, 1035, 1036, 1040, 1045, 1204);
		assertTrue(buffsByRecipient.values().stream().allMatch(policies -> policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()).containsAll(universalBuffs)));
		Set<Integer> duelist = buffsByRecipient.get(ClassId.duelist).stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet());
		Set<Integer> mysticMuse = buffsByRecipient.get(ClassId.mysticMuse).stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet());
		assertTrue(duelist.containsAll(Set.of(1068, 1077, 1086, 1240, 1242, 1388)));
		assertFalse(duelist.contains(1085));
		assertTrue(mysticMuse.containsAll(Set.of(1048, 1078, 1085, 1389)));
		assertFalse(mysticMuse.contains(1068));
		BotSkillPolicy phoenixShield = assertBuffPolicy(buffsByRecipient, ClassId.phoenixKnight, 1040, 30, 9930);
		BotSkillPolicy abyssShield = assertBuffPolicy(buffsByRecipient, ClassId.abyssWalker, 1040, 30, 9655);
		assertBuffPolicy(buffsByRecipient, ClassId.phoenixKnight, 1032, 30, 8530);
		assertTrue(phoenixShield.priority() > abyssShield.priority());

		Map<ClassId, List<BotSkillPolicy>> healsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healsByRecipient.keySet());
		assertTrue(healsByRecipient.values().stream().allMatch(policies -> (policies.size() == 2) && policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()).equals(Set.of(1011, 1015))));
		BotSkillPolicy phoenixBattle = assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34488);
		BotSkillPolicy phoenixEfficient = assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1011, 91, 25, 14488);
		BotSkillPolicy abyssBattle = assertHealPolicy(healsByRecipient, ClassId.abyssWalker, 1015, 25, 5, 32040);
		assertHealPolicy(healsByRecipient, ClassId.abyssWalker, 1011, 62, 25, 12040);
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
		assertTrue(abyssBattle.priority() > (phoenixEfficient.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> curesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), curesByRecipient.keySet());
		assertTrue(curesByRecipient.values().stream().allMatch(policies -> (policies.size() == 1) && (policies.get(0).skillId() == 1012)));
		BotSkillPolicy cardinalCure = assertCurePolicy(curesByRecipient, ClassId.cardinal, 20, 31508);
		BotSkillPolicy phoenixCure = assertCurePolicy(curesByRecipient, ClassId.phoenixKnight, 20, 30488);
		BotSkillPolicy abyssCure = assertCurePolicy(curesByRecipient, ClassId.abyssWalker, 20, 28040);
		assertTrue(phoenixCure.priority() > (abyssCure.priority() + 235));
		assertTrue(abyssBattle.priority() > cardinalCure.priority());

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027), groupHeals.keySet());
		assertTrue(groupHeals.get(1027).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 45) && (count.radius() == 1000)));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertEquals(178, resurrectionByRecipient.values().stream().mapToInt(List::size).sum());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 50, 24508);
		BotSkillPolicy phoenixBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, true, 50, 23488);
		BotSkillPolicy phoenixSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, false, 40, 21488);
		assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, true, 50, 21040);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 40, 19040);
		assertTrue(cardinalBattlefield.priority() > (phoenixBattlefield.priority() + 80));
		assertTrue(phoenixSafe.priority() > (abyssSafe.priority() + 80));
		assertTrue(abyssBattle.priority() > (cardinalBattlefield.priority() + 235));

		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1043, 1073, 1182, 1189, 1191, 1352, 1356, 1392, 1393).contains(policy.skillId())));
	}

	@Test
	void clericOwnsCompleteExactPartySupportContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.cleric, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(975, profile.getSkillPolicies().size());

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(Set.of(1011, 1015), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		BotSkillPolicy cardinalBattle = assertHealPolicy(healByRecipient, ClassId.cardinal, 1015, 48, 5, 36000);
		BotSkillPolicy phoenixBattle = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34914);
		BotSkillPolicy phoenixEfficient = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1011, 91, 25, 14914);
		BotSkillPolicy abyssBattle = assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1015, 25, 5, 32120);
		assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1011, 62, 25, 12120);
		assertTrue(cardinalBattle.priority() > (phoenixBattle.priority() + 80));
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
		assertTrue(abyssBattle.priority() > (phoenixEfficient.priority() + 235));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027), groupHeals.keySet());
		assertTrue(groupHeals.get(1027).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 45) && (count.radius() == 1000)));

		Map<ClassId, List<BotSkillPolicy>> buffPoliciesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		Map<ClassId, Set<Integer>> buffsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), buffsByRecipient.keySet());
		assertTrue(buffsByRecipient.get(ClassId.duelist).containsAll(Set.of(1068, 1077)));
		assertFalse(buffsByRecipient.get(ClassId.duelist).contains(1085));
		assertTrue(buffsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1078, 1085)));
		assertFalse(buffsByRecipient.get(ClassId.mysticMuse).contains(1068));
		assertTrue(buffsByRecipient.get(ClassId.phoenixKnight).containsAll(Set.of(1044, 1068)));
		BotSkillPolicy phoenixShield = assertBuffPolicy(buffPoliciesByRecipient, ClassId.phoenixKnight, 1040, 30, 9990);
		BotSkillPolicy abyssShield = assertBuffPolicy(buffPoliciesByRecipient, ClassId.abyssWalker, 1040, 30, 9664);
		assertTrue(phoenixShield.priority() > (abyssShield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> curesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), curesByRecipient.keySet());
		assertTrue(curesByRecipient.values().stream().allMatch(policies -> policies.size() == 1));
		BotSkillPolicy cardinalCure = assertCurePolicy(curesByRecipient, ClassId.cardinal, 15, 31500);
		BotSkillPolicy phoenixCure = assertCurePolicy(curesByRecipient, ClassId.phoenixKnight, 15, 30414);
		BotSkillPolicy abyssCure = assertCurePolicy(curesByRecipient, ClassId.abyssWalker, 15, 27620);
		assertTrue(cardinalCure.priority() > (phoenixCure.priority() + 80));
		assertTrue(phoenixCure.priority() > (abyssCure.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertEquals(178, resurrectionByRecipient.values().stream().mapToInt(List::size).sum());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 35, 24000);
		BotSkillPolicy phoenixBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, true, 35, 22914);
		BotSkillPolicy phoenixSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, false, 25, 20914);
		assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, true, 35, 20120);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 25, 18120);
		assertTrue(cardinalBattlefield.priority() > (phoenixBattlefield.priority() + 80));
		assertTrue(phoenixSafe.priority() > (abyssSafe.priority() + 80));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1013, 1043, 1073, 1191).contains(policy.skillId())));
	}

	@Test
	void oracleOwnsCompleteExactPartyAndRechargeContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.oracle, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(1020, profile.getSkillPolicies().size());

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(Set.of(1011, 1015), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		BotSkillPolicy cardinalBattle = assertHealPolicy(healByRecipient, ClassId.cardinal, 1015, 48, 5, 35832);
		BotSkillPolicy phoenixBattle = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34752);
		BotSkillPolicy abyssBattle = assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1015, 25, 5, 32160);
		assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1011, 62, 25, 12160);
		assertTrue(cardinalBattle.priority() > (phoenixBattle.priority() + 80));
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> buffPoliciesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		Map<ClassId, Set<Integer>> buffsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), buffsByRecipient.keySet());
		assertTrue(buffsByRecipient.get(ClassId.abyssWalker).containsAll(Set.of(1033, 1068, 1087)));
		assertTrue(buffsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1033, 1078)));
		assertFalse(buffsByRecipient.get(ClassId.mysticMuse).contains(1068));
		assertFalse(buffsByRecipient.get(ClassId.mysticMuse).contains(1087));
		assertTrue(buffsByRecipient.get(ClassId.fortuneSeeker).containsAll(Set.of(1068, 1257)));
		assertFalse(buffsByRecipient.get(ClassId.duelist).contains(1257));
		BotSkillPolicy phoenixShield = assertBuffPolicy(buffPoliciesByRecipient, ClassId.phoenixKnight, 1040, 30, 9951);
		BotSkillPolicy abyssShield = assertBuffPolicy(buffPoliciesByRecipient, ClassId.abyssWalker, 1040, 30, 9662);
		assertTrue(phoenixShield.priority() > (abyssShield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> rechargeByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RECHARGE);
		assertEquals(83, rechargeByRecipient.size());
		for (ClassId forbidden : List.of(ClassId.oracle, ClassId.elder, ClassId.evaSaint, ClassId.shillienOracle, ClassId.shillenElder, ClassId.shillienSaint))
		{
			assertFalse(rechargeByRecipient.containsKey(forbidden));
		}
		BotSkillPolicy cardinalRecharge = assertRechargePolicy(rechargeByRecipient, ClassId.cardinal, 60);
		BotSkillPolicy mysticRecharge = assertRechargePolicy(rechargeByRecipient, ClassId.mysticMuse, 60);
		BotSkillPolicy phoenixRecharge = assertRechargePolicy(rechargeByRecipient, ClassId.phoenixKnight, 35);
		BotSkillPolicy abyssRecharge = assertRechargePolicy(rechargeByRecipient, ClassId.abyssWalker, 35);
		BotSkillPolicy fighterRecharge = assertRechargePolicy(rechargeByRecipient, ClassId.fighter, 20);
		assertEquals(12909, cardinalRecharge.priority());
		assertEquals(12745, mysticRecharge.priority());
		assertEquals(12451, phoenixRecharge.priority());
		assertEquals(12162, abyssRecharge.priority());
		assertEquals(11710, fighterRecharge.priority());
		assertTrue(phoenixRecharge.priority() > (abyssRecharge.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> curesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), curesByRecipient.keySet());
		assertTrue(curesByRecipient.values().stream().allMatch(policies -> policies.size() == 1));
		BotSkillPolicy evaSaintCure = assertCurePolicy(curesByRecipient, ClassId.evaSaint, 20, 31500);
		BotSkillPolicy cardinalCure = assertCurePolicy(curesByRecipient, ClassId.cardinal, 20, 31332);
		BotSkillPolicy phoenixCure = assertCurePolicy(curesByRecipient, ClassId.phoenixKnight, 20, 30252);
		BotSkillPolicy abyssCure = assertCurePolicy(curesByRecipient, ClassId.abyssWalker, 20, 27660);
		assertTrue(evaSaintCure.priority() > (cardinalCure.priority() + 80));
		assertTrue(phoenixCure.priority() > (abyssCure.priority() + 235));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027), groupHeals.keySet());

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertEquals(178, resurrectionByRecipient.values().stream().mapToInt(List::size).sum());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy evaSaintBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.evaSaint, true, 45, 24000);
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 45, 23832);
		assertResurrectionPolicy(resurrectionByRecipient, ClassId.dreadnought, true, 45, 21780);
		BotSkillPolicy phoenixSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, false, 35, 20752);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 35, 18160);
		assertTrue(evaSaintBattlefield.priority() > (cardinalBattlefield.priority() + 80));
		assertTrue(phoenixSafe.priority() > (abyssSafe.priority() + 80));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1043, 1073, 1259, 1303, 1397).contains(policy.skillId())));
	}

	@Test
	void shillienOracleOwnsCompleteExactPartyAndRechargeContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.shillienOracle, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(1040, profile.getSkillPolicies().size());

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(Set.of(1011, 1015), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		BotSkillPolicy shillienSaintBattle = assertHealPolicy(healByRecipient, ClassId.shillienSaint, 1015, 47, 5, 35912);
		BotSkillPolicy cardinalBattle = assertHealPolicy(healByRecipient, ClassId.cardinal, 1015, 48, 5, 35724);
		BotSkillPolicy phoenixBattle = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34664);
		BotSkillPolicy abyssBattle = assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1015, 25, 5, 32120);
		assertTrue(shillienSaintBattle.priority() > (cardinalBattle.priority() + 80));
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> buffPoliciesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		Map<ClassId, Set<Integer>> buffsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), buffsByRecipient.keySet());
		assertTrue(buffsByRecipient.get(ClassId.duelist).containsAll(Set.of(1068, 1077, 1268)));
		assertFalse(buffsByRecipient.get(ClassId.duelist).contains(1059));
		assertTrue(buffsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1059, 1078)));
		assertFalse(buffsByRecipient.get(ClassId.mysticMuse).contains(1068));
		assertFalse(buffsByRecipient.get(ClassId.mysticMuse).contains(1268));
		assertTrue(buffsByRecipient.get(ClassId.hawkeye).containsAll(Set.of(1068, 1077)));
		assertFalse(buffsByRecipient.get(ClassId.hawkeye).contains(1268));
		BotSkillPolicy phoenixShield = assertBuffPolicy(buffPoliciesByRecipient, ClassId.phoenixKnight, 1040, 30, 9947);
		BotSkillPolicy abyssShield = assertBuffPolicy(buffPoliciesByRecipient, ClassId.abyssWalker, 1040, 30, 9661);
		assertTrue(phoenixShield.priority() > (abyssShield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> rechargeByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RECHARGE);
		assertEquals(83, rechargeByRecipient.size());
		for (ClassId forbidden : List.of(ClassId.oracle, ClassId.elder, ClassId.evaSaint, ClassId.shillienOracle, ClassId.shillenElder, ClassId.shillienSaint))
		{
			assertFalse(rechargeByRecipient.containsKey(forbidden));
		}
		BotSkillPolicy stormRecharge = assertRechargePolicy(rechargeByRecipient, ClassId.stormScreamer, 60);
		BotSkillPolicy mysticRecharge = assertRechargePolicy(rechargeByRecipient, ClassId.mysticMuse, 60);
		BotSkillPolicy phoenixRecharge = assertRechargePolicy(rechargeByRecipient, ClassId.phoenixKnight, 35);
		BotSkillPolicy abyssRecharge = assertRechargePolicy(rechargeByRecipient, ClassId.abyssWalker, 35);
		assertEquals(12748, stormRecharge.priority());
		assertEquals(12713, mysticRecharge.priority());
		assertEquals(12447, phoenixRecharge.priority());
		assertEquals(12161, abyssRecharge.priority());
		assertTrue(stormRecharge.priority() > mysticRecharge.priority());
		assertTrue(phoenixRecharge.priority() > (abyssRecharge.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> curesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), curesByRecipient.keySet());
		assertTrue(curesByRecipient.values().stream().allMatch(policies -> policies.size() == 1));
		BotSkillPolicy shillienSaintCure = assertCurePolicy(curesByRecipient, ClassId.shillienSaint, 20, 31412);
		BotSkillPolicy cardinalCure = assertCurePolicy(curesByRecipient, ClassId.cardinal, 20, 31224);
		BotSkillPolicy phoenixCure = assertCurePolicy(curesByRecipient, ClassId.phoenixKnight, 20, 30164);
		BotSkillPolicy abyssCure = assertCurePolicy(curesByRecipient, ClassId.abyssWalker, 20, 27620);
		assertTrue(shillienSaintCure.priority() > (cardinalCure.priority() + 80));
		assertTrue(phoenixCure.priority() > (abyssCure.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy shillienSaintBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.shillienSaint, true, 45, 23912);
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 45, 23724);
		BotSkillPolicy phoenixSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, false, 35, 20664);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 35, 18120);
		assertTrue(shillienSaintBattlefield.priority() > (cardinalBattlefield.priority() + 80));
		assertTrue(phoenixSafe.priority() > (abyssSafe.priority() + 80));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027), groupHeals.keySet());
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1043, 1073, 1087, 1189, 1240, 1242, 1257, 1303).contains(policy.skillId())));
	}

	@Test
	void bishopOwnsCompleteExactSpecialistSupportContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.bishop, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(1604, profile.getSkillPolicies().size());

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(6, policies.size());
			assertEquals(Set.of(1020, 1217, 1218, 1258, 1401), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		BotSkillPolicy phoenixBattle = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1218, 46, 5, 35228);
		BotSkillPolicy abyssBattle = assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1218, 25, 5, 32240);
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
		BotSkillPolicy phoenixRestore = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1258, 77, 35, 32228);
		assertTrue(phoenixRestore.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.SELF) && (status.status() == BotRuleCondition.StatusType.IN_COMBAT) && !status.expected()));
		assertTrue(phoenixRestore.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.TARGET) && (status.status() == BotRuleCondition.StatusType.IN_COMBAT) && !status.expected()));
		List<BotSkillPolicy> phoenixVitalize = healByRecipient.get(ClassId.phoenixKnight).stream().filter(policy -> policy.skillId() == 1020).toList();
		assertEquals(2, phoenixVitalize.size());
		assertTrue(phoenixVitalize.stream().allMatch(policy -> policy.priority() == 33728));
		assertEquals(Set.of(BotRuleCondition.StatusType.POISONED, BotRuleCondition.StatusType.BLEEDING), phoenixVitalize.stream().flatMap(policy -> policy.conditions().stream()).filter(BotRuleCondition.Status.class::isInstance).map(BotRuleCondition.Status.class::cast).filter(status -> status.subject() == BotRuleCondition.Subject.TARGET).map(BotRuleCondition.Status::status).filter(status -> status != BotRuleCondition.StatusType.ALIVE).collect(Collectors.toSet()));

		Map<ClassId, List<BotSkillPolicy>> buffByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		assertEquals(playableClasses(), buffByRecipient.keySet());
		assertTrue(buffByRecipient.values().stream().allMatch(policies -> policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()).containsAll(Set.of(1035, 1040, 1204, 1418))));
		assertBuffPolicy(buffByRecipient, ClassId.phoenixKnight, 1040, 30, 10026);
		assertBuffPolicy(buffByRecipient, ClassId.abyssWalker, 1040, 30, 9673);
		BotSkillPolicy phoenixCelestial = buffByRecipient.get(ClassId.phoenixKnight).stream().filter(policy -> policy.skillId() == 1418).findFirst().orElseThrow();
		BotSkillPolicy abyssCelestial = buffByRecipient.get(ClassId.abyssWalker).stream().filter(policy -> policy.skillId() == 1418).findFirst().orElseThrow();
		assertEquals(45228, phoenixCelestial.priority());
		assertEquals(42240, abyssCelestial.priority());
		assertTrue(phoenixCelestial.priority() > (abyssCelestial.priority() + 235));
		assertTrue(phoenixCelestial.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.HP) && (resource.value() == 56)));

		Map<ClassId, List<BotSkillPolicy>> curesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), curesByRecipient.keySet());
		for (List<BotSkillPolicy> policies : curesByRecipient.values())
		{
			assertEquals(3, policies.size());
			assertEquals(Set.of(1012, 1018), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
			assertEquals(Set.of(BotRuleCondition.StatusType.POISONED, BotRuleCondition.StatusType.BLEEDING, BotRuleCondition.StatusType.PARALYZED), policies.stream().flatMap(policy -> policy.conditions().stream()).filter(BotRuleCondition.Status.class::isInstance).map(BotRuleCondition.Status.class::cast).filter(status -> status.subject() == BotRuleCondition.Subject.TARGET).map(BotRuleCondition.Status::status).filter(status -> status != BotRuleCondition.StatusType.ALIVE).collect(Collectors.toSet()));
		}
		List<BotSkillPolicy> phoenixCures = curesByRecipient.get(ClassId.phoenixKnight);
		assertEquals(30228, phoenixCures.stream().filter(policy -> policy.skillId() == 1012).findFirst().orElseThrow().priority());
		assertEquals(31128, phoenixCures.stream().filter(policy -> policy.key().endsWith("purify-paralysis")).findFirst().orElseThrow().priority());
		assertEquals(28140, curesByRecipient.get(ClassId.abyssWalker).stream().filter(policy -> policy.key().endsWith("purify-paralysis")).findFirst().orElseThrow().priority());

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 45, 25000);
		BotSkillPolicy phoenixBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, true, 45, 24228);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 35, 19240);
		assertTrue(cardinalBattlefield.priority() > (phoenixBattlefield.priority() + 80));
		assertTrue(phoenixBattlefield.priority() > (abyssSafe.priority() + 80));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027, 1219, 1271, 1311, 1402), groupHeals.keySet());
		assertTrue(groupHeals.get(1271).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.comparison() == BotRuleCondition.Comparison.AT_LEAST) && (count.count() == 4) && (count.threshold() == 35) && (count.radius() == 1000)));
		assertTrue(groupHeals.get(1311).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.group() == BotRuleCondition.Group.PARTY) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 55)));
		Map<Integer, BotSkillPolicy> groupBuffs = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_BUFF).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1307, 1311), groupBuffs.keySet());
		assertTrue(groupBuffs.get(1307).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupEffectCount count) && (count.group() == BotRuleCondition.Group.PARTY) && count.effectSkillIds().equals(List.of(1307)) && (count.effectState() == BotRuleCondition.EffectState.ABSENT)));
		assertTrue(groupBuffs.get(1311).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupEffectCount count) && (count.group() == BotRuleCondition.Group.PARTY) && count.effectSkillIds().equals(List.of(1045, 1311)) && (count.effectState() == BotRuleCondition.EffectState.ABSENT)));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1015, 1043, 1073, 1191, 1254).contains(policy.skillId())));
	}

	@Test
	void cardinalOwnsCompleteExactSpecialistSupportContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.cardinal, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(2495, profile.getSkillPolicies().size());

		Map<ClassId, List<BotSkillPolicy>> healByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healByRecipient.values())
		{
			assertEquals(6, policies.size());
			assertEquals(Set.of(1020, 1217, 1218, 1258, 1401), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		BotSkillPolicy phoenixBattle = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1218, 46, 5, 35454);
		BotSkillPolicy abyssBattle = assertHealPolicy(healByRecipient, ClassId.abyssWalker, 1218, 25, 5, 32320);
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
		BotSkillPolicy phoenixRestore = assertHealPolicy(healByRecipient, ClassId.phoenixKnight, 1258, 77, 35, 32454);
		assertTrue(phoenixRestore.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.SELF) && (status.status() == BotRuleCondition.StatusType.IN_COMBAT) && !status.expected()));
		assertTrue(phoenixRestore.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.TARGET) && (status.status() == BotRuleCondition.StatusType.IN_COMBAT) && !status.expected()));
		List<BotSkillPolicy> phoenixVitalize = healByRecipient.get(ClassId.phoenixKnight).stream().filter(policy -> policy.skillId() == 1020).toList();
		assertEquals(2, phoenixVitalize.size());
		assertTrue(phoenixVitalize.stream().allMatch(policy -> policy.priority() == 33954));
		assertEquals(Set.of(BotRuleCondition.StatusType.POISONED, BotRuleCondition.StatusType.BLEEDING), phoenixVitalize.stream().map(BotProductionProfileCatalogTest::targetAbnormality).collect(Collectors.toSet()));

		Map<ClassId, List<BotSkillPolicy>> buffByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		assertEquals(playableClasses(), buffByRecipient.keySet());
		assertTrue(buffByRecipient.values().stream().allMatch(policies -> policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()).containsAll(Set.of(1035, 1040, 1204, 1353, 1418))));
		assertBuffPolicy(buffByRecipient, ClassId.phoenixKnight, 1353, 30, 10348);
		assertBuffPolicy(buffByRecipient, ClassId.abyssWalker, 1353, 30, 9979);
		assertBuffPolicy(buffByRecipient, ClassId.phoenixKnight, 1040, 30, 10048);
		assertBuffPolicy(buffByRecipient, ClassId.abyssWalker, 1040, 30, 9679);
		BotSkillPolicy phoenixCelestial = buffByRecipient.get(ClassId.phoenixKnight).stream().filter(policy -> policy.skillId() == 1418).findFirst().orElseThrow();
		BotSkillPolicy abyssCelestial = buffByRecipient.get(ClassId.abyssWalker).stream().filter(policy -> policy.skillId() == 1418).findFirst().orElseThrow();
		assertEquals(45454, phoenixCelestial.priority());
		assertEquals(42320, abyssCelestial.priority());
		assertTrue(phoenixCelestial.priority() > (abyssCelestial.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> cureByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), cureByRecipient.keySet());
		Set<BotRuleCondition.StatusType> completeCureSet = Set.of(BotRuleCondition.StatusType.POISONED, BotRuleCondition.StatusType.BLEEDING, BotRuleCondition.StatusType.PARALYZED, BotRuleCondition.StatusType.CONFUSED, BotRuleCondition.StatusType.DEBUFFED, BotRuleCondition.StatusType.FEARED, BotRuleCondition.StatusType.MUTED, BotRuleCondition.StatusType.ROOTED, BotRuleCondition.StatusType.SILENCED, BotRuleCondition.StatusType.SLEEPING, BotRuleCondition.StatusType.STUNNED, BotRuleCondition.StatusType.WEAKENED);
		for (List<BotSkillPolicy> policies : cureByRecipient.values())
		{
			assertEquals(12, policies.size());
			assertEquals(Set.of(1012, 1018, 1409), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
			assertEquals(completeCureSet, policies.stream().map(BotProductionProfileCatalogTest::targetAbnormality).collect(Collectors.toSet()));
		}
		Map<BotRuleCondition.StatusType, BotSkillPolicy> phoenixCures = cureByRecipient.get(ClassId.phoenixKnight).stream().collect(Collectors.toMap(BotProductionProfileCatalogTest::targetAbnormality, policy -> policy));
		assertEquals(1012, phoenixCures.get(BotRuleCondition.StatusType.POISONED).skillId());
		assertEquals(30454, phoenixCures.get(BotRuleCondition.StatusType.POISONED).priority());
		assertEquals(1018, phoenixCures.get(BotRuleCondition.StatusType.PARALYZED).skillId());
		assertEquals(31354, phoenixCures.get(BotRuleCondition.StatusType.PARALYZED).priority());
		BotSkillPolicy phoenixStun = phoenixCures.get(BotRuleCondition.StatusType.STUNNED);
		BotSkillPolicy abyssStun = cureByRecipient.get(ClassId.abyssWalker).stream().filter(policy -> targetAbnormality(policy) == BotRuleCondition.StatusType.STUNNED).findFirst().orElseThrow();
		assertEquals(1409, phoenixStun.skillId());
		assertEquals(31354, phoenixStun.priority());
		assertEquals(28220, abyssStun.priority());
		assertTrue(phoenixStun.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 25)));
		assertTrue(phoenixStun.priority() > (abyssStun.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 45, 25000);
		BotSkillPolicy phoenixBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, true, 45, 24454);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 35, 19320);
		assertTrue(cardinalBattlefield.priority() > (phoenixBattlefield.priority() + 80));
		assertTrue(phoenixBattlefield.priority() > (abyssSafe.priority() + 80));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027, 1219, 1271, 1311, 1335, 1402), groupHeals.keySet());
		BotSkillPolicy balanceLife = profile.getSkillPolicies().stream().filter(policy -> policy.skillId() == 1335).findFirst().orElseThrow();
		assertEquals(BotSkillPolicy.Intent.GROUP_HEAL, balanceLife.intent());
		assertTrue(balanceLife.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.filter() == BotRuleCondition.GroupFilter.ALIVE) && (count.count() == 3)));
		assertTrue(balanceLife.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 1) && (count.threshold() == 35)));
		Map<Integer, BotSkillPolicy> groupBuffs = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_BUFF).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1307, 1311), groupBuffs.keySet());
		assertTrue(groupBuffs.get(1307).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupEffectCount count) && (count.group() == BotRuleCondition.Group.PARTY) && count.effectSkillIds().equals(List.of(1307)) && (count.effectState() == BotRuleCondition.EffectState.ABSENT)));
		assertTrue(groupBuffs.get(1311).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupEffectCount count) && (count.group() == BotRuleCondition.Group.PARTY) && count.effectSkillIds().equals(List.of(1045, 1311)) && (count.effectState() == BotRuleCondition.EffectState.ABSENT)));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1013, 1015, 1043, 1073, 1191, 1254, 1426).contains(policy.skillId())));
	}

	@Test
	void elderOwnsCompleteExactRecoveryRechargeAndSupportContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.elder, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(1591, profile.getSkillPolicies().size());

		Map<ClassId, List<BotSkillPolicy>> buffsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		Map<ClassId, Set<Integer>> skillsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), skillsByRecipient.keySet());
		Set<Integer> universalBuffs = Set.of(1033, 1035, 1040, 1204, 1259, 1397);
		assertTrue(skillsByRecipient.values().stream().allMatch(skills -> skills.containsAll(universalBuffs)));
		Set<Integer> duelist = skillsByRecipient.get(ClassId.duelist);
		Set<Integer> mysticMuse = skillsByRecipient.get(ClassId.mysticMuse);
		Set<Integer> phoenixKnight = skillsByRecipient.get(ClassId.phoenixKnight);
		Set<Integer> fortuneSeeker = skillsByRecipient.get(ClassId.fortuneSeeker);
		assertTrue(duelist.containsAll(Set.of(1033, 1068, 1259, 1397)));
		assertFalse(duelist.contains(1078));
		assertFalse(duelist.contains(1303));
		assertTrue(mysticMuse.containsAll(Set.of(1033, 1078, 1259, 1303, 1397)));
		assertFalse(mysticMuse.contains(1068));
		assertTrue(phoenixKnight.containsAll(Set.of(1033, 1044, 1068, 1243, 1259, 1304, 1397)));
		assertTrue(fortuneSeeker.contains(1257));
		assertFalse(duelist.contains(1257));
		BotSkillPolicy phoenixShield = assertBuffPolicy(buffsByRecipient, ClassId.phoenixKnight, 1040, 30, 9973);
		BotSkillPolicy abyssShield = assertBuffPolicy(buffsByRecipient, ClassId.abyssWalker, 1040, 30, 9670);
		assertBuffPolicy(buffsByRecipient, ClassId.fortuneSeeker, 1257, 30, 8591);
		assertTrue(phoenixShield.priority() > (abyssShield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> healsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healsByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healsByRecipient.values())
		{
			assertEquals(5, policies.size());
			assertEquals(Set.of(1015, 1020, 1217, 1401), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
			assertEquals(2, policies.stream().filter(policy -> policy.skillId() == 1020).count());
		}
		BotSkillPolicy phoenixBattle = assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34840);
		assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1401, 77, 15, 24840);
		assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1217, 91, 25, 14840);
		BotSkillPolicy abyssBattle = assertHealPolicy(healsByRecipient, ClassId.abyssWalker, 1015, 25, 5, 32200);
		Map<BotRuleCondition.StatusType, BotSkillPolicy> phoenixVitalize = healsByRecipient.get(ClassId.phoenixKnight).stream().filter(policy -> policy.skillId() == 1020).collect(Collectors.toMap(BotProductionProfileCatalogTest::targetAbnormality, policy -> policy));
		assertEquals(Set.of(BotRuleCondition.StatusType.POISONED, BotRuleCondition.StatusType.BLEEDING), phoenixVitalize.keySet());
		for (BotSkillPolicy vitalize : phoenixVitalize.values())
		{
			assertEquals(33340, vitalize.priority());
			assertTrue(vitalize.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.HP) && (resource.value() == 77)));
			assertTrue(vitalize.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 30)));
		}
		BotSkillPolicy abyssVitalize = healsByRecipient.get(ClassId.abyssWalker).stream().filter(policy -> policy.skillId() == 1020).findFirst().orElseThrow();
		assertEquals(30700, abyssVitalize.priority());
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
		assertTrue(phoenixBattle.priority() > (abyssVitalize.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> curesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), curesByRecipient.keySet());
		assertTrue(curesByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		Map<BotRuleCondition.StatusType, BotSkillPolicy> phoenixCures = curesByRecipient.get(ClassId.phoenixKnight).stream().collect(Collectors.toMap(BotProductionProfileCatalogTest::targetAbnormality, policy -> policy));
		assertEquals(Set.of(BotRuleCondition.StatusType.POISONED, BotRuleCondition.StatusType.BLEEDING), phoenixCures.keySet());
		assertEquals(1012, phoenixCures.get(BotRuleCondition.StatusType.POISONED).skillId());
		assertEquals(29940, phoenixCures.get(BotRuleCondition.StatusType.POISONED).priority());
		assertEquals(1020, phoenixCures.get(BotRuleCondition.StatusType.BLEEDING).skillId());
		assertEquals(30540, phoenixCures.get(BotRuleCondition.StatusType.BLEEDING).priority());
		assertTrue(phoenixCures.values().stream().noneMatch(policy -> policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.HP))));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027), groupHeals.keySet());
		assertTrue(groupHeals.get(1027).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.group() == BotRuleCondition.Group.PARTY) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 45) && (count.radius() == 1000)));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 55, 24940);
		BotSkillPolicy phoenixBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, true, 55, 23840);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 45, 19200);
		assertTrue(cardinalBattlefield.priority() > (phoenixBattlefield.priority() + 80));
		assertTrue(phoenixBattlefield.priority() > (abyssSafe.priority() + 80));
		assertTrue(abyssBattle.priority() > (cardinalBattlefield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> rechargeByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RECHARGE);
		assertEquals(83, rechargeByRecipient.size());
		assertEquals(12760, assertRechargePolicy(rechargeByRecipient, ClassId.mysticMuse, 60).priority());
		assertEquals(12297, assertRechargePolicy(rechargeByRecipient, ClassId.duelist, 35).priority());
		assertEquals(11720, assertRechargePolicy(rechargeByRecipient, ClassId.fighter, 20).priority());
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1043, 1073, 1353, 1354, 1355, 1393).contains(policy.skillId())));
	}

	@Test
	void evaSaintOwnsCompleteExactRecoveryRechargeAndSupportContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.evaSaint, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(1800, profile.getSkillPolicies().size());

		Map<ClassId, List<BotSkillPolicy>> buffsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		Map<ClassId, Set<Integer>> skillsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), skillsByRecipient.keySet());
		Set<Integer> universalBuffs = Set.of(1033, 1035, 1040, 1204, 1259, 1353, 1354, 1397);
		assertTrue(skillsByRecipient.values().stream().allMatch(skills -> skills.containsAll(universalBuffs)));
		Set<Integer> duelist = skillsByRecipient.get(ClassId.duelist);
		Set<Integer> mysticMuse = skillsByRecipient.get(ClassId.mysticMuse);
		Set<Integer> phoenixKnight = skillsByRecipient.get(ClassId.phoenixKnight);
		Set<Integer> fortuneSeeker = skillsByRecipient.get(ClassId.fortuneSeeker);
		Set<Integer> doomcryer = skillsByRecipient.get(ClassId.doomcryer);
		assertTrue(duelist.containsAll(Set.of(1033, 1068, 1259, 1353, 1354, 1397)));
		assertFalse(duelist.contains(1078));
		assertFalse(duelist.contains(1303));
		assertFalse(duelist.contains(1355));
		assertTrue(mysticMuse.containsAll(Set.of(1033, 1078, 1259, 1303, 1353, 1354, 1355, 1397)));
		assertFalse(mysticMuse.contains(1068));
		assertTrue(phoenixKnight.containsAll(Set.of(1033, 1044, 1068, 1243, 1259, 1304, 1353, 1354, 1397)));
		assertTrue(fortuneSeeker.contains(1257));
		assertFalse(duelist.contains(1257));
		assertTrue(doomcryer.containsAll(Set.of(1078, 1353, 1354, 1397)));
		assertFalse(doomcryer.contains(1355));
		BotSkillPolicy phoenixShield = assertBuffPolicy(buffsByRecipient, ClassId.phoenixKnight, 1040, 30, 9990);
		BotSkillPolicy abyssShield = assertBuffPolicy(buffsByRecipient, ClassId.abyssWalker, 1040, 30, 9676);
		assertBuffPolicy(buffsByRecipient, ClassId.mysticMuse, 1355, 45, 10273);
		assertBuffPolicy(buffsByRecipient, ClassId.fortuneSeeker, 1257, 30, 8602);
		assertTrue(phoenixShield.priority() > (abyssShield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> healsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healsByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healsByRecipient.values())
		{
			assertEquals(5, policies.size());
			assertEquals(Set.of(1015, 1020, 1217, 1401), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
			assertEquals(2, policies.stream().filter(policy -> policy.skillId() == 1020).count());
		}
		BotSkillPolicy cardinalBattle = assertHealPolicy(healsByRecipient, ClassId.cardinal, 1015, 48, 5, 36000);
		BotSkillPolicy phoenixBattle = assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 35016);
		assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1401, 77, 15, 25016);
		assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1217, 91, 25, 15016);
		BotSkillPolicy abyssBattle = assertHealPolicy(healsByRecipient, ClassId.abyssWalker, 1015, 25, 5, 32280);
		Map<BotRuleCondition.StatusType, BotSkillPolicy> phoenixVitalize = healsByRecipient.get(ClassId.phoenixKnight).stream().filter(policy -> policy.skillId() == 1020).collect(Collectors.toMap(BotProductionProfileCatalogTest::targetAbnormality, policy -> policy));
		assertEquals(Set.of(BotRuleCondition.StatusType.POISONED, BotRuleCondition.StatusType.BLEEDING), phoenixVitalize.keySet());
		for (BotSkillPolicy vitalize : phoenixVitalize.values())
		{
			assertEquals(33516, vitalize.priority());
			assertTrue(vitalize.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.HP) && (resource.value() == 77)));
			assertTrue(vitalize.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 30)));
		}
		BotSkillPolicy abyssVitalize = healsByRecipient.get(ClassId.abyssWalker).stream().filter(policy -> policy.skillId() == 1020).findFirst().orElseThrow();
		assertEquals(30780, abyssVitalize.priority());
		assertTrue(cardinalBattle.priority() > (phoenixBattle.priority() + 80));
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
		assertTrue(phoenixBattle.priority() > (abyssVitalize.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> curesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), curesByRecipient.keySet());
		assertTrue(curesByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		Map<BotRuleCondition.StatusType, BotSkillPolicy> phoenixCures = curesByRecipient.get(ClassId.phoenixKnight).stream().collect(Collectors.toMap(BotProductionProfileCatalogTest::targetAbnormality, policy -> policy));
		assertEquals(Set.of(BotRuleCondition.StatusType.POISONED, BotRuleCondition.StatusType.BLEEDING), phoenixCures.keySet());
		assertEquals(1012, phoenixCures.get(BotRuleCondition.StatusType.POISONED).skillId());
		assertEquals(30016, phoenixCures.get(BotRuleCondition.StatusType.POISONED).priority());
		assertEquals(1020, phoenixCures.get(BotRuleCondition.StatusType.BLEEDING).skillId());
		assertEquals(30716, phoenixCures.get(BotRuleCondition.StatusType.BLEEDING).priority());
		assertTrue(phoenixCures.values().stream().noneMatch(policy -> policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.HP))));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027), groupHeals.keySet());
		assertTrue(groupHeals.get(1027).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.group() == BotRuleCondition.Group.PARTY) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 45) && (count.radius() == 1000)));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 55, 25000);
		BotSkillPolicy phoenixBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, true, 55, 24016);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 45, 19280);
		assertTrue(cardinalBattlefield.priority() > (phoenixBattlefield.priority() + 80));
		assertTrue(phoenixBattlefield.priority() > (abyssSafe.priority() + 80));
		assertTrue(abyssBattle.priority() > (cardinalBattlefield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> rechargeByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RECHARGE);
		assertEquals(83, rechargeByRecipient.size());
		assertEquals(12773, assertRechargePolicy(rechargeByRecipient, ClassId.mysticMuse, 60).priority());
		assertEquals(12307, assertRechargePolicy(rechargeByRecipient, ClassId.duelist, 35).priority());
		assertEquals(11728, assertRechargePolicy(rechargeByRecipient, ClassId.fighter, 20).priority());
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1043, 1073, 1351, 1359, 1393, 1428).contains(policy.skillId())));
	}

	@Test
	void shillienElderOwnsCompleteExactRecoveryRechargeAndSupportContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.shillenElder, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(1350, profile.getSkillPolicies().size());

		Map<ClassId, List<BotSkillPolicy>> buffsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		Map<ClassId, Set<Integer>> skillsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), skillsByRecipient.keySet());
		Set<Integer> universalBuffs = Set.of(1035, 1040, 1204);
		assertTrue(skillsByRecipient.values().stream().allMatch(skills -> skills.containsAll(universalBuffs)));
		Set<Integer> duelist = skillsByRecipient.get(ClassId.duelist);
		Set<Integer> mysticMuse = skillsByRecipient.get(ClassId.mysticMuse);
		Set<Integer> phoenixKnight = skillsByRecipient.get(ClassId.phoenixKnight);
		Set<Integer> hawkeye = skillsByRecipient.get(ClassId.hawkeye);
		assertTrue(duelist.containsAll(Set.of(1035, 1040, 1068, 1077, 1204, 1240, 1242, 1268)));
		assertFalse(duelist.contains(1059));
		assertTrue(mysticMuse.containsAll(Set.of(1035, 1040, 1059, 1078, 1204, 1303)));
		assertFalse(mysticMuse.contains(1068));
		assertFalse(mysticMuse.contains(1268));
		assertTrue(phoenixKnight.containsAll(Set.of(1068, 1077, 1240, 1242, 1268)));
		assertTrue(hawkeye.containsAll(Set.of(1068, 1077, 1240, 1242)));
		assertFalse(hawkeye.contains(1268));
		BotSkillPolicy phoenixShield = assertBuffPolicy(buffsByRecipient, ClassId.phoenixKnight, 1040, 30, 9964);
		BotSkillPolicy abyssShield = assertBuffPolicy(buffsByRecipient, ClassId.abyssWalker, 1040, 30, 9667);
		assertBuffPolicy(buffsByRecipient, ClassId.duelist, 1068, 30, 9291);
		assertBuffPolicy(buffsByRecipient, ClassId.mysticMuse, 1059, 30, 9221);
		assertTrue(phoenixShield.priority() > (abyssShield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> healsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healsByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healsByRecipient.values())
		{
			assertEquals(Set.of(1015, 1217), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		BotSkillPolicy shillienSaintBattle = assertHealPolicy(healsByRecipient, ClassId.shillienSaint, 1015, 47, 5, 36000);
		BotSkillPolicy cardinalBattle = assertHealPolicy(healsByRecipient, ClassId.cardinal, 1015, 48, 5, 35832);
		BotSkillPolicy phoenixBattle = assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34752);
		BotSkillPolicy phoenixGreater = assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1217, 91, 25, 14752);
		BotSkillPolicy abyssBattle = assertHealPolicy(healsByRecipient, ClassId.abyssWalker, 1015, 25, 5, 32160);
		assertHealPolicy(healsByRecipient, ClassId.abyssWalker, 1217, 62, 25, 12160);
		assertTrue(shillienSaintBattle.priority() > (cardinalBattle.priority() + 80));
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
		assertTrue(abyssBattle.priority() > (phoenixGreater.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> curesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), curesByRecipient.keySet());
		assertTrue(curesByRecipient.values().stream().allMatch(policies -> policies.size() == 3));
		Map<BotRuleCondition.StatusType, BotSkillPolicy> shillienSaintCures = curesByRecipient.get(ClassId.shillienSaint).stream().collect(Collectors.toMap(BotProductionProfileCatalogTest::targetAbnormality, policy -> policy));
		assertEquals(Set.of(BotRuleCondition.StatusType.POISONED, BotRuleCondition.StatusType.BLEEDING, BotRuleCondition.StatusType.PARALYZED), shillienSaintCures.keySet());
		assertEquals(1012, shillienSaintCures.get(BotRuleCondition.StatusType.POISONED).skillId());
		assertEquals(31100, shillienSaintCures.get(BotRuleCondition.StatusType.POISONED).priority());
		assertEquals(1018, shillienSaintCures.get(BotRuleCondition.StatusType.BLEEDING).skillId());
		assertEquals(31300, shillienSaintCures.get(BotRuleCondition.StatusType.BLEEDING).priority());
		assertEquals(1018, shillienSaintCures.get(BotRuleCondition.StatusType.PARALYZED).skillId());
		assertEquals(31600, shillienSaintCures.get(BotRuleCondition.StatusType.PARALYZED).priority());
		assertTrue(shillienSaintCures.values().stream().noneMatch(policy -> policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.HP))));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027, 1219), groupHeals.keySet());
		assertTrue(groupHeals.get(1027).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.group() == BotRuleCondition.Group.PARTY) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 45) && (count.radius() == 1000)));
		assertTrue(groupHeals.get(1219).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.group() == BotRuleCondition.Group.PARTY) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 55) && (count.radius() == 1000)));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy shillienSaintBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.shillienSaint, true, 55, 25000);
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 55, 24832);
		BotSkillPolicy phoenixBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, true, 55, 23752);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 45, 19160);
		assertTrue(shillienSaintBattlefield.priority() > (cardinalBattlefield.priority() + 80));
		assertTrue(phoenixBattlefield.priority() > (abyssSafe.priority() + 80));
		assertTrue(abyssBattle.priority() > (shillienSaintBattlefield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> rechargeByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RECHARGE);
		assertEquals(83, rechargeByRecipient.size());
		assertEquals(12761, assertRechargePolicy(rechargeByRecipient, ClassId.stormScreamer, 60).priority());
		assertEquals(12721, assertRechargePolicy(rechargeByRecipient, ClassId.mysticMuse, 60).priority());
		assertEquals(12464, assertRechargePolicy(rechargeByRecipient, ClassId.phoenixKnight, 35).priority());
		assertEquals(12167, assertRechargePolicy(rechargeByRecipient, ClassId.abyssWalker, 35).priority());
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1020, 1043, 1073, 1189, 1354, 1357, 1392, 1401, 1428).contains(policy.skillId())));
	}

	@Test
	void shillienSaintOwnsCompleteExactRecoveryRechargeAndSupportContract() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.shillienSaint, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(1454, profile.getSkillPolicies().size());

		Map<ClassId, List<BotSkillPolicy>> buffsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.BUFF);
		Map<ClassId, Set<Integer>> skillsByRecipient = skillsByRecipient(profile);
		assertEquals(playableClasses(), skillsByRecipient.keySet());
		Set<Integer> universalBuffs = Set.of(1035, 1040, 1204, 1354);
		assertTrue(skillsByRecipient.values().stream().allMatch(skills -> skills.containsAll(universalBuffs)));
		assertTrue(skillsByRecipient.get(ClassId.abyssWalker).containsAll(Set.of(1268, 1354, 1357)));
		assertTrue(skillsByRecipient.get(ClassId.ghostSentinel).containsAll(Set.of(1354, 1357)));
		assertFalse(skillsByRecipient.get(ClassId.ghostSentinel).contains(1268));
		assertFalse(skillsByRecipient.get(ClassId.duelist).contains(1357));
		assertFalse(skillsByRecipient.get(ClassId.phoenixKnight).contains(1357));
		assertTrue(skillsByRecipient.get(ClassId.mysticMuse).containsAll(Set.of(1059, 1078, 1303, 1354)));
		assertFalse(skillsByRecipient.get(ClassId.mysticMuse).contains(1357));
		BotSkillPolicy phoenixShield = assertBuffPolicy(buffsByRecipient, ClassId.phoenixKnight, 1040, 30, 9981);
		BotSkillPolicy abyssShield = assertBuffPolicy(buffsByRecipient, ClassId.abyssWalker, 1040, 30, 9673);
		assertBuffPolicy(buffsByRecipient, ClassId.ghostSentinel, 1357, 45, 10218);
		assertBuffPolicy(buffsByRecipient, ClassId.duelist, 1068, 30, 9302);
		assertBuffPolicy(buffsByRecipient, ClassId.mysticMuse, 1059, 30, 9229);
		assertTrue(phoenixShield.priority() > (abyssShield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> healsByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.HEAL);
		assertEquals(playableClasses(), healsByRecipient.keySet());
		for (List<BotSkillPolicy> policies : healsByRecipient.values())
		{
			assertEquals(Set.of(1015, 1217), policies.stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));
		}
		BotSkillPolicy cardinalBattle = assertHealPolicy(healsByRecipient, ClassId.cardinal, 1015, 48, 5, 36000);
		BotSkillPolicy phoenixBattle = assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1015, 46, 5, 34928);
		BotSkillPolicy phoenixGreater = assertHealPolicy(healsByRecipient, ClassId.phoenixKnight, 1217, 91, 25, 14928);
		BotSkillPolicy abyssBattle = assertHealPolicy(healsByRecipient, ClassId.abyssWalker, 1015, 25, 5, 32240);
		assertHealPolicy(healsByRecipient, ClassId.abyssWalker, 1217, 62, 25, 12240);
		assertTrue(cardinalBattle.priority() > (phoenixBattle.priority() + 80));
		assertTrue(phoenixBattle.priority() > (abyssBattle.priority() + 235));
		assertTrue(abyssBattle.priority() > (phoenixGreater.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> curesByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.CURE);
		assertEquals(playableClasses(), curesByRecipient.keySet());
		assertTrue(curesByRecipient.values().stream().allMatch(policies -> policies.size() == 3));
		Map<BotRuleCondition.StatusType, BotSkillPolicy> cardinalCures = curesByRecipient.get(ClassId.cardinal).stream().collect(Collectors.toMap(BotProductionProfileCatalogTest::targetAbnormality, policy -> policy));
		assertEquals(Set.of(BotRuleCondition.StatusType.POISONED, BotRuleCondition.StatusType.BLEEDING, BotRuleCondition.StatusType.PARALYZED), cardinalCures.keySet());
		assertEquals(1012, cardinalCures.get(BotRuleCondition.StatusType.POISONED).skillId());
		assertEquals(31100, cardinalCures.get(BotRuleCondition.StatusType.POISONED).priority());
		assertEquals(1018, cardinalCures.get(BotRuleCondition.StatusType.BLEEDING).skillId());
		assertEquals(31300, cardinalCures.get(BotRuleCondition.StatusType.BLEEDING).priority());
		assertEquals(1018, cardinalCures.get(BotRuleCondition.StatusType.PARALYZED).skillId());
		assertEquals(31600, cardinalCures.get(BotRuleCondition.StatusType.PARALYZED).priority());
		assertTrue(cardinalCures.values().stream().noneMatch(policy -> policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.HP))));

		Map<Integer, BotSkillPolicy> groupHeals = profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.GROUP_HEAL).collect(Collectors.toMap(BotSkillPolicy::skillId, policy -> policy));
		assertEquals(Set.of(1027, 1219), groupHeals.keySet());
		assertTrue(groupHeals.get(1027).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.group() == BotRuleCondition.Group.PARTY) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 45) && (count.radius() == 1000)));
		assertTrue(groupHeals.get(1219).conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.group() == BotRuleCondition.Group.PARTY) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.count() == 3) && (count.threshold() == 55) && (count.radius() == 1000)));

		Map<ClassId, List<BotSkillPolicy>> resurrectionByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RESURRECT);
		assertEquals(playableClasses(), resurrectionByRecipient.keySet());
		assertTrue(resurrectionByRecipient.values().stream().allMatch(policies -> policies.size() == 2));
		BotSkillPolicy cardinalBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.cardinal, true, 55, 25000);
		BotSkillPolicy phoenixBattlefield = assertResurrectionPolicy(resurrectionByRecipient, ClassId.phoenixKnight, true, 55, 23928);
		BotSkillPolicy abyssSafe = assertResurrectionPolicy(resurrectionByRecipient, ClassId.abyssWalker, false, 45, 19240);
		assertTrue(cardinalBattlefield.priority() > (phoenixBattlefield.priority() + 80));
		assertTrue(phoenixBattlefield.priority() > (abyssSafe.priority() + 80));
		assertTrue(abyssBattle.priority() > (cardinalBattlefield.priority() + 235));

		Map<ClassId, List<BotSkillPolicy>> rechargeByRecipient = policiesByRecipient(profile, BotSkillPolicy.Intent.RECHARGE);
		assertEquals(83, rechargeByRecipient.size());
		assertEquals(12775, assertRechargePolicy(rechargeByRecipient, ClassId.stormScreamer, 60).priority());
		assertEquals(12729, assertRechargePolicy(rechargeByRecipient, ClassId.mysticMuse, 60).priority());
		assertEquals(12481, assertRechargePolicy(rechargeByRecipient, ClassId.phoenixKnight, 35).priority());
		assertEquals(12173, assertRechargePolicy(rechargeByRecipient, ClassId.abyssWalker, 35).priority());
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> Set.of(1020, 1043, 1073, 1189, 1358, 1392, 1401, 1428).contains(policy.skillId())));
	}

	@Test
	void swordSingerMaintainsSongsForExactPartyBeneficiaries() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.swordSinger, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(8, profile.getSkillPolicies().size());
		assertEquals(Set.of(264, 265, 266, 267, 268, 269, 304, 305), profile.getSkillPolicies().stream().map(BotSkillPolicy::skillId).collect(Collectors.toSet()));

		Map<ClassId, Set<Integer>> songsByRecipient = repertoireSkillsByRecipient(profile, BotSkillPolicy.Intent.SONG);
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
	void warcryerChantsAndRecoversItsExactPartyBeneficiaries() throws IOException
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
		assertEquals(9880, duelistBattle.priority());
		BotRuleCondition.GroupClassCount battleFamily = duelistBattle.conditions().stream().filter(BotRuleCondition.GroupClassCount.class::isInstance).map(BotRuleCondition.GroupClassCount.class::cast).findFirst().orElseThrow();
		assertEquals(List.of(1003, 1007, 1068), battleFamily.effectSkillIds());
		BotSkillPolicy duelistWar = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("duelist-war-chant")).findFirst().orElseThrow();
		assertEquals(10280, duelistWar.priority());
		BotRuleCondition.GroupClassCount greaterFamily = duelistWar.conditions().stream().filter(BotRuleCondition.GroupClassCount.class::isInstance).map(BotRuleCondition.GroupClassCount.class::cast).findFirst().orElseThrow();
		assertEquals(List.of(1388, 1389, 1390, 1391), greaterFamily.effectSkillIds());
		assertTrue(profile.getSkillPolicies().stream().filter(policy -> policy.intent() == BotSkillPolicy.Intent.CHANT).allMatch(policy -> policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 30))));

		Map<ClassId, Set<Integer>> recoveryByRecipient = groupSkillsByRecipient(profile, BotSkillPolicy.Intent.GROUP_HEAL);
		assertEquals(playableClasses(), recoveryByRecipient.keySet());
		assertTrue(recoveryByRecipient.values().stream().allMatch(skills -> skills.equals(Set.of(1229))));
		BotSkillPolicy phoenixLife = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("phoenix-knight-chant-of-life")).findFirst().orElseThrow();
		BotSkillPolicy abyssLife = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("abyss-walker-chant-of-life")).findFirst().orElseThrow();
		assertEquals(27475, phoenixLife.priority());
		assertEquals(27216, abyssLife.priority());
		assertTrue(phoenixLife.priority() > (abyssLife.priority() + 235));
		assertTrue(phoenixLife.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.SELF) && (status.status() == BotRuleCondition.StatusType.IN_COMBAT) && status.expected()));
		assertTrue(phoenixLife.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 45)));
		assertTrue(phoenixLife.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.group() == BotRuleCondition.Group.PARTY) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.comparison() == BotRuleCondition.Comparison.AT_LEAST) && (count.count() == 2) && (count.threshold() == 75) && (count.radius() == 1000)));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> (policy.intent() == BotSkillPolicy.Intent.HEAL) || (policy.intent() == BotSkillPolicy.Intent.CURE) || (policy.intent() == BotSkillPolicy.Intent.RESURRECT)));
	}

	@Test
	void doomcryerOwnsExactThirdClassChantsAndRecovery() throws IOException
	{
		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(productionDirectory());
		BotClassProfile profile = registry.require(ClassId.doomcryer, "world");
		assertSame(profile, new BotProfileBrain(profile).getProfile());
		assertEquals(914, profile.getSkillPolicies().size());

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

		BotSkillPolicy duelistBattle = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("duelist-chant-of-battle")).findFirst().orElseThrow();
		BotSkillPolicy duelistVictory = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("duelist-chant-of-victory")).findFirst().orElseThrow();
		assertEquals(9891, duelistBattle.priority());
		assertEquals(12291, duelistVictory.priority());
		BotSkillPolicy mysticFlame = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("mystic-muse-flame-chant")).findFirst().orElseThrow();
		BotSkillPolicy mysticMagnus = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("mystic-muse-magnus-chant")).findFirst().orElseThrow();
		assertEquals(9821, mysticFlame.priority());
		assertEquals(12221, mysticMagnus.priority());
		BotRuleCondition.GroupClassCount prophecyFamily = mysticMagnus.conditions().stream().filter(BotRuleCondition.GroupClassCount.class::isInstance).map(BotRuleCondition.GroupClassCount.class::cast).findFirst().orElseThrow();
		assertEquals(List.of(1355, 1356, 1357, 1363, 1413, 1414), prophecyFamily.effectSkillIds());
		assertTrue(mysticMagnus.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 45)));
		BotSkillPolicy cardinalSpirit = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("cardinal-chant-of-spirit")).findFirst().orElseThrow();
		assertEquals(11451, cardinalSpirit.priority());
		BotRuleCondition.GroupClassCount spiritCount = cardinalSpirit.conditions().stream().filter(BotRuleCondition.GroupClassCount.class::isInstance).map(BotRuleCondition.GroupClassCount.class::cast).findFirst().orElseThrow();
		assertEquals(900, spiritCount.radius());
		assertTrue(cardinalSpirit.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 40)));

		Map<ClassId, Set<Integer>> recoveryByRecipient = groupSkillsByRecipient(profile, BotSkillPolicy.Intent.GROUP_HEAL);
		assertEquals(playableClasses(), recoveryByRecipient.keySet());
		assertTrue(recoveryByRecipient.values().stream().allMatch(skills -> skills.equals(Set.of(1229))));
		BotSkillPolicy phoenixLife = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("phoenix-knight-chant-of-life")).findFirst().orElseThrow();
		BotSkillPolicy abyssLife = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("abyss-walker-chant-of-life")).findFirst().orElseThrow();
		BotSkillPolicy cardinalLife = profile.getSkillPolicies().stream().filter(policy -> policy.key().equals("cardinal-chant-of-life")).findFirst().orElseThrow();
		assertEquals(27492, phoenixLife.priority());
		assertEquals(27224, abyssLife.priority());
		assertEquals(27600, cardinalLife.priority());
		assertTrue(phoenixLife.priority() > (abyssLife.priority() + 235));
		assertTrue(cardinalLife.priority() > phoenixLife.priority());
		assertTrue(phoenixLife.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.SELF) && (status.status() == BotRuleCondition.StatusType.IN_COMBAT) && status.expected()));
		assertTrue(phoenixLife.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.value() == 45)));
		assertTrue(phoenixLife.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.GroupCount count) && (count.group() == BotRuleCondition.Group.PARTY) && (count.filter() == BotRuleCondition.GroupFilter.HP_PERCENT_BELOW) && (count.comparison() == BotRuleCondition.Comparison.AT_LEAST) && (count.count() == 2) && (count.threshold() == 75) && (count.radius() == 1000)));
		assertFalse(profile.getSkillPolicies().stream().anyMatch(policy -> (policy.intent() == BotSkillPolicy.Intent.HEAL) || (policy.intent() == BotSkillPolicy.Intent.CURE) || (policy.intent() == BotSkillPolicy.Intent.RESURRECT)));
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

	private static Map<ClassId, Set<Integer>> repertoireSkillsByRecipient(BotClassProfile profile, BotSkillPolicy.Intent intent)
	{
		Map<ClassId, Set<Integer>> skillsByRecipient = new EnumMap<>(ClassId.class);
		for (BotSkillPolicy policy : profile.getSkillPolicies())
		{
			if (policy.intent() != intent)
			{
				continue;
			}
			assertEquals(BotSkillPolicy.Target.SELF, policy.target());
			List<BotRuleCondition.GroupClassSetCount> counts = policy.conditions().stream().filter(BotRuleCondition.GroupClassSetCount.class::isInstance).map(BotRuleCondition.GroupClassSetCount.class::cast).toList();
			assertEquals(1, counts.size(), () -> policy.key() + " must name one exact repertoire beneficiary set");
			BotRuleCondition.GroupClassSetCount count = counts.get(0);
			assertEquals(BotRuleCondition.Group.PARTY, count.group());
			assertTrue(count.effectSkillIds().contains(policy.skillId()));
			assertEquals(BotRuleCondition.EffectState.ABSENT, count.effectState());
			assertEquals(BotRuleCondition.Comparison.AT_LEAST, count.comparison());
			assertEquals(1, count.count());
			assertTrue((count.radius() > 0) && (count.radius() <= BotRuleCondition.MAXIMUM_RANGE));
			List<BotRuleCondition.Resource> reserves = policy.conditions().stream().filter(BotRuleCondition.Resource.class::isInstance).map(BotRuleCondition.Resource.class::cast).filter(resource -> (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.ABOVE)).toList();
			assertEquals(1, reserves.size(), () -> policy.key() + " must preserve one explicit MP reserve");
			assertTrue((reserves.get(0).value() >= 0) && (reserves.get(0).value() < 100));
			for (ClassId classId : count.classIds())
			{
				skillsByRecipient.computeIfAbsent(classId, ignored -> new HashSet<>()).add(policy.skillId());
			}
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

	private static BotSkillPolicy assertRechargePolicy(Map<ClassId, List<BotSkillPolicy>> policiesByRecipient, ClassId recipient, int targetThreshold)
	{
		List<BotSkillPolicy> policies = policiesByRecipient.get(recipient);
		assertEquals(1, policies.size());
		BotSkillPolicy policy = policies.get(0);
		assertEquals(1013, policy.skillId());
		assertEquals(BotSkillPolicy.Target.PARTY_LOWEST_MP, policy.target());
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.TARGET) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.BELOW) && (resource.value() == targetThreshold)));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.ABOVE) && (resource.value() == 45)));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Distance distance) && (distance.subject() == BotRuleCondition.Subject.TARGET) && (distance.comparison() == BotRuleCondition.Comparison.AT_MOST) && (distance.value() == 1200)));
		return policy;
	}

	private static BotSkillPolicy assertBuffPolicy(Map<ClassId, List<BotSkillPolicy>> policiesByRecipient, ClassId recipient, int skillId, int selfReserve, int priority)
	{
		BotSkillPolicy policy = policiesByRecipient.get(recipient).stream().filter(candidate -> candidate.skillId() == skillId).findFirst().orElseThrow();
		assertEquals(BotSkillPolicy.Target.PARTY_MEMBER, policy.target());
		assertEquals(priority, policy.priority());
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.SELF) && (status.status() == BotRuleCondition.StatusType.ALIVE) && status.expected()));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.ABOVE) && (resource.value() == selfReserve)));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Distance distance) && (distance.subject() == BotRuleCondition.Subject.TARGET) && (distance.comparison() == BotRuleCondition.Comparison.AT_MOST) && (distance.value() == 1200)));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Effect effect) && (effect.subject() == BotRuleCondition.Subject.TARGET) && (effect.skillId() == skillId) && (effect.state() == BotRuleCondition.EffectState.ABSENT)));
		return policy;
	}

	private static BotSkillPolicy assertCurePolicy(Map<ClassId, List<BotSkillPolicy>> policiesByRecipient, ClassId recipient, int selfReserve, int priority)
	{
		BotSkillPolicy policy = policiesByRecipient.get(recipient).get(0);
		assertEquals(1012, policy.skillId());
		assertEquals(BotSkillPolicy.Target.PARTY_MEMBER, policy.target());
		assertEquals(priority, policy.priority());
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.TARGET) && (status.status() == BotRuleCondition.StatusType.ALIVE) && status.expected()));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Status status) && (status.subject() == BotRuleCondition.Subject.TARGET) && (status.status() == BotRuleCondition.StatusType.POISONED) && status.expected()));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Resource resource) && (resource.subject() == BotRuleCondition.Subject.SELF) && (resource.resource() == BotRuleCondition.ResourceType.MP) && (resource.amount() == BotRuleCondition.AmountType.PERCENT) && (resource.comparison() == BotRuleCondition.Comparison.ABOVE) && (resource.value() == selfReserve)));
		assertTrue(policy.conditions().stream().anyMatch(condition -> (condition instanceof BotRuleCondition.Distance distance) && (distance.subject() == BotRuleCondition.Subject.TARGET) && (distance.comparison() == BotRuleCondition.Comparison.AT_MOST) && (distance.value() == 1200)));
		return policy;
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

	private static BotRuleCondition.StatusType targetAbnormality(BotSkillPolicy policy)
	{
		List<BotRuleCondition.StatusType> abnormalities = policy.conditions().stream().filter(BotRuleCondition.Status.class::isInstance).map(BotRuleCondition.Status.class::cast).filter(status -> (status.subject() == BotRuleCondition.Subject.TARGET) && status.expected() && status.status().isAbnormal()).map(BotRuleCondition.Status::status).toList();
		assertEquals(1, abnormalities.size(), () -> policy.key() + " must require one target abnormality");
		return abnormalities.get(0);
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
