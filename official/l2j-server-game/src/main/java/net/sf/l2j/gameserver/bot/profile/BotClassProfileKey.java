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
package net.sf.l2j.gameserver.bot.profile;

import java.util.Locale;

import net.sf.l2j.gameserver.model.base.ClassId;

/**
 * Exact identity of one class-specific behavior profile.
 * @param classId the one playable class owned by the profile
 * @param profileName the normalized variant name persisted for a bot
 */
public record BotClassProfileKey(ClassId classId, String profileName)
{
	public static final int MAXIMUM_NAME_LENGTH = 64;

	public BotClassProfileKey
	{
		if (classId == null)
		{
			throw new IllegalArgumentException("Bot profile class cannot be null");
		}
		if (classId.getRace() == null)
		{
			throw new IllegalArgumentException("Bot profile class must be playable: " + classId);
		}
		if (profileName == null)
		{
			throw new IllegalArgumentException("Bot profile name cannot be null");
		}
		profileName = profileName.trim().toLowerCase(Locale.ROOT);
		if (profileName.isEmpty())
		{
			throw new IllegalArgumentException("Bot profile name cannot be blank");
		}
		if (profileName.length() > MAXIMUM_NAME_LENGTH)
		{
			throw new IllegalArgumentException("Bot profile name cannot exceed " + MAXIMUM_NAME_LENGTH + " characters");
		}
	}
}
