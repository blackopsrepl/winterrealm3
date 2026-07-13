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

import java.util.OptionalInt;

import net.sf.l2j.gameserver.bot.profile.BotConditionEvaluator;
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
		return context.executor().knowsSkill(_policy.skillId()) && target(context).isPresent();
	}

	@Override
	public boolean isPossible(BotDecisionContext context)
	{
		OptionalInt target = target(context);
		return context.executor().isActive() && context.executor().knowsSkill(_policy.skillId()) && context.perception().self().alive() && !context.perception().self().casting() && target.isPresent() && (context.perception().currentTargetObjectId() == target.orElseThrow());
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
}
