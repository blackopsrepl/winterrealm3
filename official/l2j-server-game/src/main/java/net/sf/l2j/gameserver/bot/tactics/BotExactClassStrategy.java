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

import java.util.List;

import net.sf.l2j.gameserver.bot.profile.BotClassProfile;
import net.sf.l2j.gameserver.bot.strategy.BotNextAction;
import net.sf.l2j.gameserver.bot.strategy.BotStrategy;

/** Queues only actions declared by one exact ClassId/profile-name policy. */
final class BotExactClassStrategy implements BotStrategy<BotDecisionContext>
{
	private final String _name;
	private final List<BotContextualNextAction> _actions;

	BotExactClassStrategy(BotClassProfile profile, List<BotContextualNextAction> actions)
	{
		if ((profile == null) || (actions == null))
		{
			throw new IllegalArgumentException("Exact class strategy requires profile and actions");
		}
		_name = "exact-class:" + profile.getClassId() + ":" + profile.getProfileName();
		_actions = List.copyOf(actions);
	}

	@Override
	public String name()
	{
		return _name;
	}

	@Override
	public List<BotNextAction> defaultActions(BotDecisionContext context)
	{
		return _actions.stream().filter(candidate -> candidate.action().isUseful(context)).map(BotContextualNextAction::next).toList();
	}
}
