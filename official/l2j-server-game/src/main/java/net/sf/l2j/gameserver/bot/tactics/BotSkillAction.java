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

import java.util.Optional;
import java.util.OptionalInt;

import net.sf.l2j.gameserver.bot.perception.BotObservedCharacter;
import net.sf.l2j.gameserver.bot.profile.BotConditionEvaluator;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.AmountType;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.Group;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.ResourceType;
import net.sf.l2j.gameserver.bot.profile.BotSkillPolicy;
import net.sf.l2j.gameserver.bot.profile.BotTargetResolver;
import net.sf.l2j.gameserver.bot.strategy.BotAction;
import net.sf.l2j.gameserver.bot.strategy.BotActionResult;
import net.sf.l2j.gameserver.bot.strategy.BotEvent;

/** One exact-class skill policy as an executable strategy-kernel action. */
final class BotSkillAction implements BotAction<BotDecisionContext>
{
	private final String _name;
	private final BotSkillPolicy _policy;
	private final BotTargetResolver _targets;
	private final BotConditionEvaluator _conditions;

	BotSkillAction(String name, BotSkillPolicy policy, BotTargetResolver targets, BotConditionEvaluator conditions)
	{
		if ((name == null) || name.isBlank() || (policy == null) || (targets == null) || (conditions == null))
		{
			throw new IllegalArgumentException("Skill action requires its name, policy, target resolver, and condition evaluator");
		}
		_name = name;
		_policy = policy;
		_targets = targets;
		_conditions = conditions;
	}

	@Override
	public String name()
	{
		return _name;
	}

	@Override
	public boolean isUseful(BotDecisionContext context)
	{
		return context.executor().knowsSkill(_policy.skillId()) && context.executor().isSkillReady(_policy.skillId()) && target(context).isPresent();
	}

	double relevance(BotDecisionContext context)
	{
		OptionalInt target = target(context);
		if (target.isEmpty())
		{
			return _policy.priority();
		}
		return context.perception().find(target.orElseThrow()).map(character -> _policy.priority() + urgency(context, character)).orElse((double) _policy.priority());
	}

	@Override
	public boolean isPossible(BotDecisionContext context)
	{
		OptionalInt target = target(context);
		return context.executor().isActive() && context.executor().knowsSkill(_policy.skillId()) && context.executor().isSkillReady(_policy.skillId()) && context.perception().self().alive() && !context.perception().self().casting() && target.isPresent() && (context.perception().currentTargetObjectId() == target.orElseThrow());
	}

	@Override
	public BotActionResult execute(BotDecisionContext context, BotEvent event)
	{
		OptionalInt target = target(context);
		if (target.isEmpty())
		{
			return BotActionResult.FAILED;
		}
		return context.executor().castSkill(_policy, target.orElseThrow()).isAccepted() ? BotActionResult.SUCCEEDED : BotActionResult.FAILED;
	}

	private OptionalInt target(BotDecisionContext context)
	{
		return _targets.resolveEligible(_policy, context.perception(), _conditions);
	}

	private double urgency(BotDecisionContext context, BotObservedCharacter target)
	{
		return switch (_policy.intent())
		{
			case HEAL -> missingPercent(target, ResourceType.HP) + combatExposure(context, target);
			case CP_HEAL -> missingPercent(target, ResourceType.CP) + combatExposure(context, target);
			case RECHARGE -> missingPercent(target, ResourceType.MP);
			case RESURRECT -> target.level();
			default -> 0.0;
		};
	}

	private static double missingPercent(BotObservedCharacter target, ResourceType resource)
	{
		return 100.0 - target.resource(resource, AmountType.PERCENT);
	}

	private static double combatExposure(BotDecisionContext context, BotObservedCharacter target)
	{
		double score = target.inCombat() ? 25.0 : 0.0;
		score += target.attacking() ? 10.0 : 0.0;
		long attackers = context.perception().group(Group.NEARBY_HOSTILES).stream().map(context.perception()::find).flatMap(Optional::stream).filter(hostile -> hostile.alive() && (hostile.targetObjectId() == target.objectId())).limit(5).count();
		return score + (attackers * 20.0);
	}
}
