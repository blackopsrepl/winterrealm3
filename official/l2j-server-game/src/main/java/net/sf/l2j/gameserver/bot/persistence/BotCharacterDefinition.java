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
package net.sf.l2j.gameserver.bot.persistence;

import net.sf.l2j.gameserver.bot.BotMode;

/**
 * Immutable control metadata for one persisted bot character.
 * @param characterId the authoritative character object id
 * @param accountName the synthetic account that owns the character row
 * @param enabled whether the bot may be loaded
 * @param mode the operating mode
 * @param ownerObjectId an optional controlling player object id
 * @param profileName the data-driven behavior profile
 * @param activityPolicy the scheduling policy name
 * @param homeX an optional home x coordinate
 * @param homeY an optional home y coordinate
 * @param homeZ an optional home z coordinate
 * @param personalitySeed the stable deterministic random seed
 * @param metadataVersion the persisted metadata schema version
 */
public record BotCharacterDefinition(int characterId, String accountName, boolean enabled, BotMode mode, Integer ownerObjectId, String profileName, String activityPolicy, Integer homeX, Integer homeY, Integer homeZ, long personalitySeed, int metadataVersion)
{
	public static final String ACCOUNT_PREFIX = "bot:";

	/**
	 * Validates and normalizes persisted bot metadata.
	 */
	public BotCharacterDefinition
	{
		if (characterId <= 0)
		{
			throw new IllegalArgumentException("Bot character id must be positive");
		}
		accountName = requireText(accountName, "Bot account name", 45);
		if (!accountName.startsWith(ACCOUNT_PREFIX))
		{
			throw new IllegalArgumentException("Bot account name must start with " + ACCOUNT_PREFIX);
		}
		if (mode == null)
		{
			throw new IllegalArgumentException("Bot mode cannot be null");
		}
		if ((ownerObjectId != null) && (ownerObjectId <= 0))
		{
			throw new IllegalArgumentException("Bot owner object id must be positive");
		}
		profileName = requireText(profileName, "Bot profile name", 64);
		activityPolicy = requireText(activityPolicy, "Bot activity policy", 32);

		int homeCoordinates = 0;
		homeCoordinates += (homeX != null) ? 1 : 0;
		homeCoordinates += (homeY != null) ? 1 : 0;
		homeCoordinates += (homeZ != null) ? 1 : 0;
		if ((homeCoordinates != 0) && (homeCoordinates != 3))
		{
			throw new IllegalArgumentException("Bot home coordinates must be either complete or absent");
		}
		if (personalitySeed == 0)
		{
			throw new IllegalArgumentException("Bot personality seed cannot be zero");
		}
		if (metadataVersion < 1)
		{
			throw new IllegalArgumentException("Bot metadata version must be positive");
		}
	}

	/**
	 * Checks whether an explicit home location is configured.
	 * @return {@code true} when all home coordinates are present
	 */
	public boolean hasHome()
	{
		return homeX != null;
	}

	private static String requireText(String value, String field, int maximumLength)
	{
		if (value == null)
		{
			throw new IllegalArgumentException(field + " cannot be null");
		}
		String normalized = value.trim();
		if (normalized.isEmpty())
		{
			throw new IllegalArgumentException(field + " cannot be blank");
		}
		if (normalized.length() > maximumLength)
		{
			throw new IllegalArgumentException(field + " cannot exceed " + maximumLength + " characters");
		}
		return normalized;
	}
}
