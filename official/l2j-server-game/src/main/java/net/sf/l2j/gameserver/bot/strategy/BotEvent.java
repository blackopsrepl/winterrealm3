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
 * Adapted from CMaNGOS playerbots Event. Original C++ copyright
 * (C) 2005-2018 MaNGOS project. Translated and modified 2026-07-13.
 */
package net.sf.l2j.gameserver.bot.strategy;

/**
 * Immutable reason that placed an action into the decision queue.
 * @param source trigger, strategy, or continuation name
 * @param detail optional bounded diagnostic detail
 */
public record BotEvent(String source, String detail)
{
	public BotEvent
	{
		if (source == null)
		{
			throw new IllegalArgumentException("Bot event source cannot be null");
		}
		source = source.trim();
		if (source.isEmpty())
		{
			throw new IllegalArgumentException("Bot event source cannot be blank");
		}
		detail = (detail == null) ? "" : detail.trim();
	}

	public BotEvent(String source)
	{
		this(source, "");
	}
}
