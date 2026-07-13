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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.sf.l2j.gameserver.bot.perception.BotObservedCharacter;
import net.sf.l2j.gameserver.bot.perception.BotPerceptionSnapshot;
import net.sf.l2j.gameserver.model.base.ClassId;

class BotPolicyEvaluationTest
{
	private final BotTargetResolver _targets = new BotTargetResolver();
	private final BotConditionEvaluator _conditions = new BotConditionEvaluator();

	@Test
	void selectsDifferentPartyMembersForHpMpAndCpPolicies()
	{
		BotPerceptionSnapshot snapshot = snapshot(List.of(character(10, 0, true, 90, 20, 80, 80, 0, Set.of()), character(20, 200, true, 25, 90, 60, 80, 0, Set.of()), character(30, 100, true, 60, 50, 10, 80, 0, Set.of())), List.of(10, 20, 30), List.of(), List.of(), 0, 0, 10);

		assertEquals(20, _targets.resolve(BotSkillPolicy.Target.PARTY_LOWEST_HP, snapshot).orElseThrow());
		assertEquals(10, _targets.resolve(BotSkillPolicy.Target.PARTY_LOWEST_MP, snapshot).orElseThrow());
		assertEquals(30, _targets.resolve(BotSkillPolicy.Target.PARTY_LOWEST_CP, snapshot).orElseThrow());
	}

	@Test
	void makesCorpsePriorityAnExplicitSelectorChoice()
	{
		BotPerceptionSnapshot snapshot = snapshot(List.of(character(10, 0, true, 100, 100, 100, 80, 0, Set.of()), character(20, 100, false, 0, 0, 0, 20, 0, Set.of()), character(30, 400, false, 0, 0, 0, 80, 0, Set.of())), List.of(10, 20, 30), List.of(), List.of(), 0, 0, 10);

		assertEquals(20, _targets.resolve(BotSkillPolicy.Target.PARTY_DEAD_NEAREST, snapshot).orElseThrow());
		assertEquals(30, _targets.resolve(BotSkillPolicy.Target.PARTY_DEAD_HIGHEST_LEVEL, snapshot).orElseThrow());
	}

	@Test
	void resolvesAssistTargetsAndStableNearestTies()
	{
		BotObservedCharacter self = character(10, 0, true, 100, 100, 100, 80, 0, Set.of());
		BotObservedCharacter owner = character(40, 100, true, 100, 100, 100, 80, 30, Set.of());
		BotObservedCharacter first = character(20, 300, true, 100, 100, 0, 80, 0, Set.of());
		BotObservedCharacter assisted = character(30, 300, true, 100, 100, 0, 80, 0, Set.of());
		BotPerceptionSnapshot snapshot = snapshot(List.of(self, owner, first, assisted), List.of(10), List.of(), List.of(30, 20), 0, 40, 10);

		assertEquals(20, _targets.resolve(BotSkillPolicy.Target.NEAREST_HOSTILE, snapshot).orElseThrow());
		assertEquals(30, _targets.resolve(BotSkillPolicy.Target.ASSISTED_HOSTILE, snapshot).orElseThrow());
	}

	@Test
	void evaluatesAllTypedConditionsWithAndSemantics()
	{
		BotObservedCharacter self = character(10, 0, true, 40, 80, 100, 80, 20, Set.of(1204));
		BotObservedCharacter target = character(20, 500, true, 30, 50, 0, 80, 0, Set.of());
		BotPerceptionSnapshot snapshot = snapshot(List.of(self, target), List.of(10, 20), List.of(), List.of(20), 20, 0, 10);
		List<BotRuleCondition> conditions = List.of(new BotRuleCondition.Resource(BotRuleCondition.Subject.TARGET, BotRuleCondition.ResourceType.HP, BotRuleCondition.AmountType.PERCENT, BotRuleCondition.Comparison.BELOW, 50), new BotRuleCondition.Distance(BotRuleCondition.Subject.TARGET, BotRuleCondition.Comparison.AT_MOST, 600), new BotRuleCondition.Effect(BotRuleCondition.Subject.SELF, 1204, BotRuleCondition.EffectState.PRESENT), new BotRuleCondition.Status(BotRuleCondition.Subject.TARGET, BotRuleCondition.StatusType.ALIVE, true), new BotRuleCondition.GroupCount(BotRuleCondition.Group.PARTY, BotRuleCondition.GroupFilter.HP_PERCENT_BELOW, BotRuleCondition.Comparison.AT_LEAST, 2, 50, 1200));

		assertTrue(_conditions.matches(conditions, snapshot, 20));
		assertFalse(_conditions.matches(List.of(new BotRuleCondition.Resource(BotRuleCondition.Subject.TARGET, BotRuleCondition.ResourceType.HP, BotRuleCondition.AmountType.PERCENT, BotRuleCondition.Comparison.BELOW, 20)), snapshot, 20));
		assertFalse(_conditions.matches(List.of(new BotRuleCondition.Status(BotRuleCondition.Subject.OWNER, BotRuleCondition.StatusType.ALIVE, true)), snapshot, 20));
	}

	@Test
	void evaluatesPoisonAsAFirstClassObservedStatus()
	{
		BotObservedCharacter self = player(10, ClassId.prophet, 0, true, 100, 100, 100, 80, 0, Set.of());
		BotObservedCharacter target = player(20, ClassId.duelist, 100, true, 100, 100, 100, 80, 0, Set.of(), true);
		BotPerceptionSnapshot snapshot = snapshot(List.of(self, target), List.of(10, 20), List.of(), List.of(), 0, 0, 10);

		assertTrue(_conditions.matches(List.of(new BotRuleCondition.Status(BotRuleCondition.Subject.TARGET, BotRuleCondition.StatusType.POISONED, true)), snapshot, 20));
		assertFalse(_conditions.matches(List.of(new BotRuleCondition.Status(BotRuleCondition.Subject.SELF, BotRuleCondition.StatusType.POISONED, true)), snapshot, 20));
	}

	@Test
	void filtersBuffRecipientsByExactTargetClassBeforeChoosingOne()
	{
		BotObservedCharacter self = player(10, ClassId.hierophant, 0, true, 100, 100, 100, 80, 0, Set.of());
		BotObservedCharacter buffedDuelist = player(20, ClassId.duelist, 100, true, 100, 100, 100, 80, 0, Set.of(1068));
		BotObservedCharacter mysticMuse = player(30, ClassId.mysticMuse, 200, true, 100, 100, 100, 80, 0, Set.of());
		BotObservedCharacter unbuffedDuelist = player(40, ClassId.duelist, 300, true, 100, 100, 100, 80, 0, Set.of());
		BotPerceptionSnapshot snapshot = snapshot(List.of(self, buffedDuelist, mysticMuse, unbuffedDuelist), List.of(10, 20, 30, 40), List.of(), List.of(), 0, 0, 10);
		BotSkillPolicy duelistMight = buff("duelist-might", ClassId.duelist);
		BotSkillPolicy mysticMight = buff("mystic-might", ClassId.mysticMuse);

		assertEquals(40, _targets.resolveEligible(duelistMight, snapshot, _conditions).orElseThrow());
		assertEquals(30, _targets.resolveEligible(mysticMight, snapshot, _conditions).orElseThrow());
		BotRuleCondition.GroupClassCount missingDuelistMightFamily = new BotRuleCondition.GroupClassCount(BotRuleCondition.Group.PARTY, ClassId.duelist, List.of(1007, 1068), BotRuleCondition.EffectState.ABSENT, BotRuleCondition.Comparison.EQUAL, 1, 1200);
		assertTrue(_conditions.matches(List.of(missingDuelistMightFamily), snapshot, 10));
		BotRuleCondition.GroupClassCount completeDuelistMightFamily = new BotRuleCondition.GroupClassCount(BotRuleCondition.Group.PARTY, ClassId.duelist, List.of(1007, 1068), BotRuleCondition.EffectState.PRESENT, BotRuleCondition.Comparison.AT_LEAST, 1, 1200);
		assertFalse(_conditions.matches(List.of(completeDuelistMightFamily), snapshot, 10));
	}

	@Test
	void countsPartyWideEffectCoverageWithoutTreatingMembersAsTargets()
	{
		BotObservedCharacter self = player(10, ClassId.bishop, 0, true, 100, 100, 100, 80, 0, Set.of(1307));
		BotObservedCharacter missingPartyMember = player(20, ClassId.duelist, 500, true, 100, 100, 100, 80, 0, Set.of());
		BotObservedCharacter missingOutsider = player(30, ClassId.cardinal, 100, true, 100, 100, 100, 80, 0, Set.of());
		BotObservedCharacter deadPartyMember = player(40, ClassId.mysticMuse, 100, false, 0, 100, 100, 80, 0, Set.of());
		BotPerceptionSnapshot snapshot = snapshot(List.of(self, missingPartyMember, missingOutsider, deadPartyMember), List.of(10, 20, 40), List.of(), List.of(), 0, 0, 10);

		BotRuleCondition.GroupEffectCount missingPrayer = new BotRuleCondition.GroupEffectCount(BotRuleCondition.Group.PARTY, List.of(1307), BotRuleCondition.EffectState.ABSENT, BotRuleCondition.Comparison.AT_LEAST, 1, 1000);
		BotRuleCondition.GroupEffectCount twoMissingPrayer = new BotRuleCondition.GroupEffectCount(BotRuleCondition.Group.PARTY, List.of(1307), BotRuleCondition.EffectState.ABSENT, BotRuleCondition.Comparison.AT_LEAST, 2, 1000);
		BotRuleCondition.GroupEffectCount coveredPrayer = new BotRuleCondition.GroupEffectCount(BotRuleCondition.Group.PARTY, List.of(1307), BotRuleCondition.EffectState.PRESENT, BotRuleCondition.Comparison.EQUAL, 1, 1000);
		assertTrue(_conditions.matches(List.of(missingPrayer), snapshot, 10));
		assertFalse(_conditions.matches(List.of(twoMissingPrayer), snapshot, 10));
		assertTrue(_conditions.matches(List.of(coveredPrayer), snapshot, 10));
	}

	@Test
	void supportIncludesSelfAndPartyButExcludesVisibleOutsiders()
	{
		BotObservedCharacter self = player(10, ClassId.hierophant, 0, true, 100, 100, 100, 80, 0, Set.of());
		BotObservedCharacter outsider = player(20, ClassId.duelist, 50, true, 1, 100, 100, 80, 0, Set.of());
		BotObservedCharacter partyDuelist = player(30, ClassId.duelist, 500, true, 40, 100, 100, 80, 0, Set.of());
		BotPerceptionSnapshot snapshot = snapshot(List.of(self, outsider, partyDuelist), List.of(10, 30), List.of(), List.of(), 0, 0, 10);

		assertEquals(30, _targets.resolveEligible(buff("duelist-might", ClassId.duelist), snapshot, _conditions).orElseThrow());
		assertEquals(10, _targets.resolveEligible(buff("hierophant-might", ClassId.hierophant), snapshot, _conditions).orElseThrow());
		BotSkillPolicy heal = new BotSkillPolicy("duelist-heal", 1218, BotSkillPolicy.Intent.HEAL, BotSkillPolicy.Target.PARTY_LOWEST_HP, 100, false, false, List.of(new BotRuleCondition.ExactClass(BotRuleCondition.Subject.TARGET, ClassId.duelist, true), new BotRuleCondition.Resource(BotRuleCondition.Subject.TARGET, BotRuleCondition.ResourceType.HP, BotRuleCondition.AmountType.PERCENT, BotRuleCondition.Comparison.BELOW, 80)));
		assertEquals(30, _targets.resolveEligible(heal, snapshot, _conditions).orElseThrow());
		BotRuleCondition.GroupClassCount missingPartyDuelist = new BotRuleCondition.GroupClassCount(BotRuleCondition.Group.PARTY, ClassId.duelist, List.of(1068), BotRuleCondition.EffectState.ABSENT, BotRuleCondition.Comparison.EQUAL, 1, 1200);
		assertTrue(_conditions.matches(List.of(missingPartyDuelist), snapshot, 10));
	}

	private static BotSkillPolicy buff(String key, ClassId targetClass)
	{
		return new BotSkillPolicy(key, 1068, BotSkillPolicy.Intent.BUFF, BotSkillPolicy.Target.PARTY_MEMBER, 100, false, false, List.of(new BotRuleCondition.ExactClass(BotRuleCondition.Subject.TARGET, targetClass, true), new BotRuleCondition.Effect(BotRuleCondition.Subject.TARGET, 1068, BotRuleCondition.EffectState.ABSENT)));
	}

	private static BotPerceptionSnapshot snapshot(List<BotObservedCharacter> characters, List<Integer> party, List<Integer> clan, List<Integer> hostiles, int currentTarget, int owner, int leader)
	{
		return new BotPerceptionSnapshot(1, 10, currentTarget, owner, 0, leader, characters, party, clan, hostiles);
	}

	private static BotObservedCharacter character(int objectId, int distance, boolean alive, double hp, double mp, double cp, int level, int targetId, Set<Integer> effects)
	{
		return player(objectId, ClassId.duelist, distance, alive, hp, mp, cp, level, targetId, effects);
	}

	private static BotObservedCharacter player(int objectId, ClassId classId, int distance, boolean alive, double hp, double mp, double cp, int level, int targetId, Set<Integer> effects)
	{
		return player(objectId, classId, distance, alive, hp, mp, cp, level, targetId, effects, false);
	}

	private static BotObservedCharacter player(int objectId, ClassId classId, int distance, boolean alive, double hp, double mp, double cp, int level, int targetId, Set<Integer> effects, boolean poisoned)
	{
		return new BotObservedCharacter(objectId, BotObservedCharacter.Kind.PLAYER, classId, level, 0, 0, 0, distance, alive, hp, 100, mp, 100, cp, 100, false, false, false, false, false, false, false, false, poisoned, effects, targetId);
	}
}
