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
package net.sf.l2j.gameserver.bot.tactics;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.sf.l2j.gameserver.bot.perception.BotPerceptionSnapshot;
import net.sf.l2j.gameserver.bot.profile.BotClassProfile;
import net.sf.l2j.gameserver.bot.profile.BotConditionEvaluator;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition;
import net.sf.l2j.gameserver.bot.profile.BotSkillPolicy;
import net.sf.l2j.gameserver.bot.profile.BotTargetResolver;
import net.sf.l2j.gameserver.bot.strategy.BotActionCatalog;
import net.sf.l2j.gameserver.bot.strategy.BotActionNode;
import net.sf.l2j.gameserver.bot.strategy.BotDecisionEngine;
import net.sf.l2j.gameserver.bot.strategy.BotDecisionOutcome;
import net.sf.l2j.gameserver.bot.strategy.BotNextAction;
import net.sf.l2j.gameserver.model.base.ClassId;

/** Persistent tactical decision engine compiled from one exact-class profile. */
public final class BotProfileBrain
{
	private static final double PREREQUISITE_RELEVANCE = 1.0;

	private final BotClassProfile _profile;
	private final BotDecisionEngine<BotDecisionContext> _engine;

	public BotProfileBrain(BotClassProfile profile)
	{
		if (profile == null)
		{
			throw new IllegalArgumentException("Bot profile brain requires an exact class profile");
		}
		_profile = profile;

		BotTargetResolver skillTargets = new BotTargetResolver();
		BotConditionEvaluator conditions = new BotConditionEvaluator();
		List<BotActionNode<BotDecisionContext>> nodes = new ArrayList<>();
		List<BotContextualNextAction> defaults = new ArrayList<>();
		for (BotSkillPolicy policy : profile.getSkillPolicies())
		{
			String actionName = "skill:" + policy.key();
			String targetName = "target:" + policy.key();
			BotSelectTargetAction targetAction = new BotSelectTargetAction(targetName, snapshot -> skillTargets.resolveEligible(policy, snapshot, conditions));
			BotSkillAction skillAction = new BotSkillAction(actionName, profile, policy, skillTargets, conditions);
			nodes.add(new BotActionNode<>(targetAction));
			nodes.add(new BotActionNode<>(skillAction, List.of(new BotNextAction(targetName, PREREQUISITE_RELEVANCE)), List.of(), List.of()));
			defaults.add(new BotContextualNextAction(skillAction, actionName, skillAction::relevance, requiredObservedClasses(policy)));
		}

		if (profile.getCombatPolicy().basicAttackPriority() > 0)
		{
			BotCombatTargetResolver combatTargets = new BotCombatTargetResolver();
			String targetName = "target:basic-attack";
			BotBasicAttackAction basicAttack = new BotBasicAttackAction(profile, combatTargets);
			nodes.add(new BotActionNode<>(new BotSelectTargetAction(targetName, snapshot -> combatTargets.resolve(profile, snapshot))));
			nodes.add(new BotActionNode<>(basicAttack, List.of(new BotNextAction(targetName, PREREQUISITE_RELEVANCE)), List.of(), List.of()));
			defaults.add(new BotContextualNextAction(basicAttack, new BotNextAction(BotBasicAttackAction.NAME, profile.getCombatPolicy().basicAttackPriority())));
		}

		_engine = new BotDecisionEngine<>(new BotActionCatalog<>(nodes), List.of(new BotExactClassStrategy(profile, defaults)));
	}

	public BotClassProfile getProfile()
	{
		return _profile;
	}

	public BotDecisionOutcome tick(BotPerceptionSnapshot perception, BotActionExecutor executor)
	{
		BotDecisionContext context = new BotDecisionContext(perception, executor);
		return _engine.tick(context, perception.tick());
	}

	public int pendingActions()
	{
		return _engine.pendingActions();
	}

	/**
	 * Finds exact classes whose required presence can be proven without running
	 * the full rule. Conditions true when no such class exists are not indexed.
	 */
	private static Set<ClassId> requiredObservedClasses(BotSkillPolicy policy)
	{
		EnumSet<ClassId> required = EnumSet.noneOf(ClassId.class);
		for (var condition : policy.conditions())
		{
			if ((condition instanceof BotRuleCondition.ExactClass exactClass) && (exactClass.subject() == BotRuleCondition.Subject.TARGET) && exactClass.expected())
			{
				required.add(exactClass.classId());
			}
			else if ((condition instanceof BotRuleCondition.GroupClassCount count) && !count.comparison().test(0, count.count()))
			{
				required.add(count.classId());
			}
		}
		return Set.copyOf(required);
	}
}
