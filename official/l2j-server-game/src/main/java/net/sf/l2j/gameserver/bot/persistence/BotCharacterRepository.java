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
package net.sf.l2j.gameserver.bot.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.bot.BotMode;

/**
 * Loads native bot control metadata while leaving all player state in the
 * existing character repositories.
 */
public final class BotCharacterRepository
{
	private static final String LOAD_ENABLED = "SELECT b.char_obj_id,b.enabled,b.mode,b.owner_obj_id,b.profile_name,b.activity_policy,b.home_x,b.home_y,b.home_z,b.personality_seed,b.metadata_version,c.account_name FROM character_bots b INNER JOIN characters c ON c.obj_Id=b.char_obj_id WHERE b.enabled=1 ORDER BY b.char_obj_id";

	/**
	 * Loads every enabled bot definition joined to its authoritative character.
	 * @return immutable bot definitions ordered by character id
	 * @throws SQLException when metadata cannot be read or is invalid
	 */
	public List<BotCharacterDefinition> loadEnabled() throws SQLException
	{
		List<BotCharacterDefinition> definitions = new ArrayList<>();
		try (Connection connection = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = connection.prepareStatement(LOAD_ENABLED); ResultSet result = statement.executeQuery())
		{
			while (result.next())
			{
				definitions.add(readDefinition(result));
			}
		}
		return List.copyOf(definitions);
	}

	private static BotCharacterDefinition readDefinition(ResultSet result) throws SQLException
	{
		int characterId = result.getInt("char_obj_id");
		try
		{
			return new BotCharacterDefinition(characterId, result.getString("account_name"), result.getBoolean("enabled"), BotMode.parse(result.getString("mode")), nullableInt(result, "owner_obj_id"), result.getString("profile_name"), result.getString("activity_policy"), nullableInt(result, "home_x"), nullableInt(result, "home_y"), nullableInt(result, "home_z"), result.getLong("personality_seed"), result.getInt("metadata_version"));
		}
		catch (IllegalArgumentException e)
		{
			throw new SQLException("Invalid bot metadata for character " + characterId + ": " + e.getMessage(), e);
		}
	}

	private static Integer nullableInt(ResultSet result, String column) throws SQLException
	{
		int value = result.getInt(column);
		return result.wasNull() ? null : value;
	}
}
