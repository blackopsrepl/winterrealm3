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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Immutable native player-bot configuration.
 * @param enabled whether persisted bots should be loaded into the world
 */
public record BotConfig(boolean enabled)
{
	public static final Path DEFAULT_PATH = Path.of("./config/bots.properties");
	private static final String ENABLED_PROPERTY = "BotsEnabled";

	/**
	 * Loads the production bot configuration.
	 * @return the parsed configuration
	 * @throws IOException when the configuration file cannot be read
	 */
	public static BotConfig load() throws IOException
	{
		return load(DEFAULT_PATH);
	}

	/**
	 * Loads bot configuration from an explicit path.
	 * @param path the properties file to read
	 * @return the parsed configuration
	 * @throws IOException when the configuration file cannot be read
	 */
	public static BotConfig load(Path path) throws IOException
	{
		if (path == null)
		{
			throw new IllegalArgumentException("Bot configuration path cannot be null");
		}

		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(path))
		{
			properties.load(input);
		}
		return new BotConfig(parseBoolean(properties.getProperty(ENABLED_PROPERTY), ENABLED_PROPERTY));
	}

	private static boolean parseBoolean(String value, String property)
	{
		if (value == null)
		{
			throw new IllegalArgumentException("Missing required bot property: " + property);
		}
		return switch (value.trim().toLowerCase(Locale.ROOT))
		{
			case "true" -> true;
			case "false" -> false;
			default -> throw new IllegalArgumentException("Bot property " + property + " must be true or false");
		};
	}
}
