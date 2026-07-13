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
package net.sf.l2j.gameserver.bot;

import java.util.Locale;

/**
 * Declares the operating mode of a bot-controlled character.
 */
public enum BotMode
{
	WORLD,
	COMPANION,
	SCENARIO,
	MANUAL;

	/** Returns whether the central world scheduler owns this mode's cadence. */
	public boolean isAutomaticallyScheduled()
	{
		return (this == WORLD) || (this == COMPANION);
	}

	/**
	 * Parses the database representation of a bot mode.
	 * @param value the persisted mode
	 * @return the parsed mode
	 */
	public static BotMode parse(String value)
	{
		if (value == null)
		{
			throw new IllegalArgumentException("Bot mode cannot be null");
		}
		String normalized = value.trim();
		if (normalized.isEmpty())
		{
			throw new IllegalArgumentException("Bot mode cannot be blank");
		}
		return valueOf(normalized.toUpperCase(Locale.ROOT));
	}
}
