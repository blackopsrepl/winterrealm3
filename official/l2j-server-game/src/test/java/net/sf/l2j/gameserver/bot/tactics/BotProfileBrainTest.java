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
package net.sf.l2j.gameserver.bot.tactics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.sf.l2j.gameserver.bot.perception.BotObservedCharacter;
import net.sf.l2j.gameserver.bot.perception.BotPerceptionSnapshot;
import net.sf.l2j.gameserver.bot.profile.BotClassProfile;
import net.sf.l2j.gameserver.bot.profile.BotCombatPolicy;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition;
import net.sf.l2j.gameserver.bot.profile.BotSkillPolicy;
import net.sf.l2j.gameserver.bot.strategy.BotDecisionOutcome;
import net.sf.l2j.gameserver.model.actor.player.PlayerCommandResult;
import net.sf.l2j.gameserver.model.base.ClassId;

class BotProfileBrainTest
{
	@Test
	void selectsThenCastsHighestUsefulExactClassSkill()
	{
		BotSkillPolicy heal = skill("urgent-heal", 1218, BotSkillPolicy.Intent.HEAL, BotSkillPolicy.Target.PARTY_LOWEST_HP, 900, BotRuleCondition.ResourceType.HP, 50);
		BotSkillPolicy recharge = skill("recharge", 1013, BotSkillPolicy.Intent.RECHARGE, BotSkillPolicy.Target.PARTY_LOWEST_MP, 600, BotRuleCondition.ResourceType.MP, 40);
		BotProfileBrain brain = new BotProfileBrain(profile(ClassId.cardinal, List.of(heal, recharge)));
		FakeExecutor executor = new FakeExecutor();

		BotDecisionOutcome selectHeal = brain.tick(snapshot(1, 0, 80, 20, 80, 10), executor);
		BotDecisionOutcome castHeal = brain.tick(snapshot(2, 20, 80, 20, 80, 10), executor);
		BotDecisionOutcome selectRecharge = brain.tick(snapshot(3, 20, 100, 100, 80, 10), executor);
		BotDecisionOutcome castRecharge = brain.tick(snapshot(4, 30, 100, 100, 80, 10), executor);

		assertEquals("target:urgent-heal", selectHeal.actionName());
		assertEquals("skill:urgent-heal", castHeal.actionName());
		assertEquals("target:recharge", selectRecharge.actionName());
		assertEquals("skill:recharge", castRecharge.actionName());
		assertEquals(List.of("1218@20", "1013@30"), executor.casts);
	}

	@Test
	void compilesDifferentActionsForDifferentExactClasses()
	{
		BotSkillPolicy heal = skill("cardinal-heal", 1218, BotSkillPolicy.Intent.HEAL, BotSkillPolicy.Target.PARTY_LOWEST_HP, 900, BotRuleCondition.ResourceType.HP, 50);
		BotSkillPolicy recharge = skill("eva-recharge", 1013, BotSkillPolicy.Intent.RECHARGE, BotSkillPolicy.Target.PARTY_LOWEST_MP, 900, BotRuleCondition.ResourceType.MP, 40);
		BotProfileBrain cardinal = new BotProfileBrain(profile(ClassId.cardinal, List.of(heal)));
		BotProfileBrain evaSaint = new BotProfileBrain(profile(ClassId.evaSaint, List.of(recharge)));

		assertEquals("target:cardinal-heal", cardinal.tick(snapshot(1, 0, 20, 20, 80, 10), new FakeExecutor()).actionName());
		assertEquals("target:eva-recharge", evaSaint.tick(snapshot(1, 0, 20, 20, 80, 10), new FakeExecutor()).actionName());
	}

	@Test
	void basicAttackAlsoUsesAnExplicitSelectionPrerequisite()
	{
		BotCombatPolicy combat = new BotCombatPolicy(BotCombatPolicy.Targeting.NEAREST_HOSTILE, 1200, 2000, 40, 100);
		BotClassProfile profile = new BotClassProfile(BotClassProfile.SCHEMA_VERSION, ClassId.duelist, "field", combat, List.of());
		BotProfileBrain brain = new BotProfileBrain(profile);
		FakeExecutor executor = new FakeExecutor();
		BotPerceptionSnapshot first = combatSnapshot(1, 0);
		BotPerceptionSnapshot second = combatSnapshot(2, 20);

		assertEquals("target:basic-attack", brain.tick(first, executor).actionName());
		assertEquals("combat:basic-attack", brain.tick(second, executor).actionName());
		assertEquals(List.of(20), executor.attacks);
	}

	@Test
	void skipsLargeIrrelevantRecipientMatricesBeforeQueueing()
	{
		List<BotSkillPolicy> matrix = new ArrayList<>();
		for (int index = 0; index < 100; index++)
		{
			matrix.add(buff("mystic-might-" + index, ClassId.mysticMuse, 2000 - index));
		}
		matrix.add(buff("duelist-might", ClassId.duelist, 100));
		BotProfileBrain brain = new BotProfileBrain(profile(ClassId.hierophant, matrix));
		FakeExecutor executor = new FakeExecutor();

		BotDecisionOutcome outcome = brain.tick(snapshot(1, 0, 100, 100, 100, 100), executor);

		assertEquals("target:duelist-might", outcome.actionName());
		assertEquals(2, executor.skillReadinessChecks);
	}

	@Test
	void skipsUnavailableHighPrioritySkillsBeforeQueueing()
	{
		BotSkillPolicy heal = skill("urgent-heal", 1218, BotSkillPolicy.Intent.HEAL, BotSkillPolicy.Target.PARTY_LOWEST_HP, 900, BotRuleCondition.ResourceType.HP, 50);
		BotSkillPolicy recharge = skill("recharge", 1013, BotSkillPolicy.Intent.RECHARGE, BotSkillPolicy.Target.PARTY_LOWEST_MP, 600, BotRuleCondition.ResourceType.MP, 40);
		BotProfileBrain brain = new BotProfileBrain(profile(ClassId.evaSaint, List.of(heal, recharge)));
		FakeExecutor executor = new FakeExecutor();
		executor.unavailableSkills.add(1218);

		BotDecisionOutcome outcome = brain.tick(snapshot(1, 0, 20, 20, 20, 10), executor);

		assertEquals("target:recharge", outcome.actionName());
	}

	private static BotClassProfile profile(ClassId classId, List<BotSkillPolicy> skills)
	{
		return new BotClassProfile(BotClassProfile.SCHEMA_VERSION, classId, "party", new BotCombatPolicy(BotCombatPolicy.Targeting.NONE, 0, 0, 0, 0), skills);
	}

	private static BotSkillPolicy skill(String key, int skillId, BotSkillPolicy.Intent intent, BotSkillPolicy.Target target, int priority, BotRuleCondition.ResourceType resource, int threshold)
	{
		BotRuleCondition condition = new BotRuleCondition.Resource(BotRuleCondition.Subject.TARGET, resource, BotRuleCondition.AmountType.PERCENT, BotRuleCondition.Comparison.BELOW, threshold);
		return new BotSkillPolicy(key, skillId, intent, target, priority, false, false, List.of(condition));
	}

	private static BotSkillPolicy buff(String key, ClassId recipientClass, int priority)
	{
		return new BotSkillPolicy(key, 1068, BotSkillPolicy.Intent.BUFF, BotSkillPolicy.Target.PARTY_MEMBER, priority, false, false, List.of(new BotRuleCondition.ExactClass(BotRuleCondition.Subject.TARGET, recipientClass, true), new BotRuleCondition.Effect(BotRuleCondition.Subject.TARGET, 1068, BotRuleCondition.EffectState.ABSENT)));
	}

	private static BotPerceptionSnapshot snapshot(long tick, int currentTarget, double allyHp, double injuredHp, double injuredMp, double lowMp)
	{
		BotObservedCharacter self = character(10, 0, 100, 100, 0);
		BotObservedCharacter injured = character(20, 200, injuredHp, injuredMp, 0);
		BotObservedCharacter depleted = character(30, 300, allyHp, lowMp, 0);
		return new BotPerceptionSnapshot(tick, 10, currentTarget, 0, 0, 10, List.of(self, injured, depleted), List.of(10, 20, 30), List.of(), List.of());
	}

	private static BotPerceptionSnapshot combatSnapshot(long tick, int currentTarget)
	{
		BotObservedCharacter self = character(10, 0, 100, 100, 0);
		BotObservedCharacter hostile = character(20, 300, 100, 100, 0);
		return new BotPerceptionSnapshot(tick, 10, currentTarget, 0, 0, 10, List.of(self, hostile), List.of(10), List.of(), List.of(20));
	}

	private static BotObservedCharacter character(int objectId, int distance, double hp, double mp, int targetId)
	{
		return new BotObservedCharacter(objectId, BotObservedCharacter.Kind.PLAYER, ClassId.duelist, 80, 0, 0, 0, distance, true, hp, 100, mp, 100, 100, 100, false, false, false, false, false, false, false, false, Set.of(), targetId);
	}

	private static final class FakeExecutor implements BotActionExecutor
	{
		private final List<String> casts = new ArrayList<>();
		private final List<Integer> attacks = new ArrayList<>();
		private final Set<Integer> unavailableSkills = new HashSet<>();
		private int selectedTarget;
		private int skillReadinessChecks;

		@Override
		public boolean isActive()
		{
			return true;
		}

		@Override
		public boolean knowsSkill(int skillId)
		{
			return true;
		}

		@Override
		public boolean isSkillReady(int skillId)
		{
			skillReadinessChecks++;
			return !unavailableSkills.contains(skillId);
		}

		@Override
		public PlayerCommandResult selectTarget(int targetObjectId)
		{
			selectedTarget = targetObjectId;
			return PlayerCommandResult.TARGET_SELECTED;
		}

		@Override
		public PlayerCommandResult castSkill(BotSkillPolicy policy, int targetObjectId)
		{
			if (selectedTarget != targetObjectId)
			{
				return PlayerCommandResult.TARGET_REJECTED;
			}
			casts.add(policy.skillId() + "@" + targetObjectId);
			return PlayerCommandResult.ACCEPTED;
		}

		@Override
		public PlayerCommandResult attack(int targetObjectId)
		{
			if (selectedTarget != targetObjectId)
			{
				return PlayerCommandResult.TARGET_REJECTED;
			}
			attacks.add(targetObjectId);
			return PlayerCommandResult.ACCEPTED;
		}
	}
}
