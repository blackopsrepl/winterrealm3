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
 *
 * Adapted from CMaNGOS playerbots ActionNode. Original C++ copyright
 * (C) 2005-2018 MaNGOS project. Translated and modified 2026-07-13.
 */
package net.sf.l2j.gameserver.bot.strategy;

import java.util.List;

/**
 * Action plus its corrective prerequisites, failure alternatives, and
 * successful continuers.
 * @param <C> immutable decision context type
 */
public record BotActionNode<C>(BotAction<C> action, List<BotNextAction> prerequisites, List<BotNextAction> alternatives, List<BotNextAction> continuers)
{
	public BotActionNode
	{
		if (action == null)
		{
			throw new IllegalArgumentException("Bot action node requires an action");
		}
		validateName(action.name());
		prerequisites = immutableActions(prerequisites, "prerequisites");
		alternatives = immutableActions(alternatives, "alternatives");
		continuers = immutableActions(continuers, "continuers");
	}

	public BotActionNode(BotAction<C> action)
	{
		this(action, List.of(), List.of(), List.of());
	}

	public String name()
	{
		return action.name().trim();
	}

	private static List<BotNextAction> immutableActions(List<BotNextAction> actions, String field)
	{
		if (actions == null)
		{
			throw new IllegalArgumentException("Bot action " + field + " cannot be null");
		}
		if (actions.stream().anyMatch(action -> action == null))
		{
			throw new IllegalArgumentException("Bot action " + field + " cannot contain null");
		}
		return List.copyOf(actions);
	}

	private static void validateName(String name)
	{
		if ((name == null) || name.trim().isEmpty())
		{
			throw new IllegalArgumentException("Bot action name cannot be blank");
		}
	}
}
