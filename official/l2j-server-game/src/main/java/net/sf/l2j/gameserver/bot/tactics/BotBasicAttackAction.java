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

import net.sf.l2j.gameserver.bot.profile.BotCombatPolicy;
import net.sf.l2j.gameserver.bot.strategy.BotAction;
import net.sf.l2j.gameserver.bot.strategy.BotActionResult;
import net.sf.l2j.gameserver.bot.strategy.BotEvent;

/** Explicit normal-attack action owned and scored by the exact class profile. */
final class BotBasicAttackAction implements BotAction<BotDecisionContext>
{
	static final String NAME = "combat:basic-attack";

	private final BotCombatPolicy _policy;
	private final BotCombatTargetResolver _targets;

	BotBasicAttackAction(BotCombatPolicy policy, BotCombatTargetResolver targets)
	{
		if ((policy == null) || (targets == null))
		{
			throw new IllegalArgumentException("Basic attack action requires combat policy and target resolver");
		}
		_policy = policy;
		_targets = targets;
	}

	@Override
	public String name()
	{
		return NAME;
	}

	@Override
	public boolean isUseful(BotDecisionContext context)
	{
		return target(context).isPresent();
	}

	@Override
	public boolean isPossible(BotDecisionContext context)
	{
		OptionalInt target = target(context);
		return context.executor().isActive() && context.perception().self().alive() && !context.perception().self().casting() && target.isPresent() && (context.perception().currentTargetObjectId() == target.orElseThrow());
	}

	@Override
	public BotActionResult execute(BotDecisionContext context, BotEvent event)
	{
		OptionalInt target = target(context);
		if (target.isEmpty())
		{
			return BotActionResult.FAILED;
		}
		return context.executor().attack(target.orElseThrow()).isAccepted() ? BotActionResult.SUCCEEDED : BotActionResult.FAILED;
	}

	private OptionalInt target(BotDecisionContext context)
	{
		return _targets.resolve(_policy, context.perception());
	}
}
