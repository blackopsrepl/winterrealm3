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

import java.util.OptionalInt;
import java.util.function.Function;

import net.sf.l2j.gameserver.bot.perception.BotPerceptionSnapshot;
import net.sf.l2j.gameserver.bot.strategy.BotAction;
import net.sf.l2j.gameserver.bot.strategy.BotActionResult;
import net.sf.l2j.gameserver.bot.strategy.BotEvent;

/** Explicit target-selection prerequisite for a cast or weapon attack. */
final class BotSelectTargetAction implements BotAction<BotDecisionContext>
{
	private final String _name;
	private final Function<BotPerceptionSnapshot, OptionalInt> _resolver;

	BotSelectTargetAction(String name, Function<BotPerceptionSnapshot, OptionalInt> resolver)
	{
		if ((name == null) || name.isBlank() || (resolver == null))
		{
			throw new IllegalArgumentException("Target action requires a name and resolver");
		}
		_name = name;
		_resolver = resolver;
	}

	@Override
	public String name()
	{
		return _name;
	}

	@Override
	public boolean isUseful(BotDecisionContext context)
	{
		OptionalInt target = _resolver.apply(context.perception());
		return target.isPresent() && (target.orElseThrow() != context.perception().currentTargetObjectId());
	}

	@Override
	public boolean isPossible(BotDecisionContext context)
	{
		return context.executor().isActive() && _resolver.apply(context.perception()).isPresent();
	}

	@Override
	public BotActionResult execute(BotDecisionContext context, BotEvent event)
	{
		OptionalInt target = _resolver.apply(context.perception());
		if (target.isEmpty())
		{
			return BotActionResult.FAILED;
		}
		return context.executor().selectTarget(target.orElseThrow()).isAccepted() ? BotActionResult.SUCCEEDED : BotActionResult.FAILED;
	}
}
