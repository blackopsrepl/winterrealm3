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
package net.sf.l2j.gameserver.bot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import net.sf.l2j.gameserver.bot.scheduler.BotSchedulePolicy;

/**
 * Immutable native player-bot configuration.
 * @param enabled whether persisted bots should be loaded into the world
 * @param schedulePolicy central scheduler cadence and admission bounds
 */
public record BotConfig(boolean enabled, BotSchedulePolicy schedulePolicy)
{
	public static final Path DEFAULT_PATH = Path.of("./config/bots.properties");
	public static final long DEFAULT_PULSE_MILLIS = 100;
	public static final long DEFAULT_FOREGROUND_MILLIS = 250;
	public static final long DEFAULT_BACKGROUND_MILLIS = 2000;
	public static final int DEFAULT_MAX_DISPATCH_PER_PULSE = 16;
	public static final int DEFAULT_MAX_IN_FLIGHT = 4;
	private static final String ENABLED_PROPERTY = "BotsEnabled";
	private static final String PULSE_PROPERTY = "BotSchedulerPulseMillis";
	private static final String FOREGROUND_PROPERTY = "BotForegroundDecisionMillis";
	private static final String BACKGROUND_PROPERTY = "BotBackgroundDecisionMillis";
	private static final String MAX_DISPATCH_PROPERTY = "BotMaxDispatchPerPulse";
	private static final String MAX_IN_FLIGHT_PROPERTY = "BotMaxInFlightDecisions";

	public BotConfig
	{
		if (schedulePolicy == null)
		{
			throw new IllegalArgumentException("Bot schedule policy cannot be null");
		}
	}

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
		BotSchedulePolicy schedulePolicy = new BotSchedulePolicy(parseLong(properties, PULSE_PROPERTY, DEFAULT_PULSE_MILLIS), parseLong(properties, FOREGROUND_PROPERTY, DEFAULT_FOREGROUND_MILLIS), parseLong(properties, BACKGROUND_PROPERTY, DEFAULT_BACKGROUND_MILLIS), parseInt(properties, MAX_DISPATCH_PROPERTY, DEFAULT_MAX_DISPATCH_PER_PULSE), parseInt(properties, MAX_IN_FLIGHT_PROPERTY, DEFAULT_MAX_IN_FLIGHT));
		return new BotConfig(parseBoolean(properties.getProperty(ENABLED_PROPERTY), ENABLED_PROPERTY), schedulePolicy);
	}

	private static long parseLong(Properties properties, String property, long defaultValue)
	{
		String value = properties.getProperty(property, Long.toString(defaultValue));
		try
		{
			return Long.parseLong(value.trim());
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Bot property " + property + " must be an integer", e);
		}
	}

	private static int parseInt(Properties properties, String property, int defaultValue)
	{
		long value = parseLong(properties, property, defaultValue);
		if ((value < Integer.MIN_VALUE) || (value > Integer.MAX_VALUE))
		{
			throw new IllegalArgumentException("Bot property " + property + " is outside the integer range");
		}
		return (int) value;
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
