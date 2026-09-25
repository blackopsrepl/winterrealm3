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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BotConfigTest
{
	@TempDir
	Path _temporaryDirectory;

	@Test
	void loadsExplicitEnabledAndDisabledValues() throws IOException
	{
		Path enabled = write("enabled.properties", "BotsEnabled = TRUE\n");
		Path disabled = write("disabled.properties", "BotsEnabled = false\n");

		BotConfig enabledConfig = BotConfig.load(enabled);
		assertTrue(enabledConfig.enabled());
		assertEquals(BotConfig.DEFAULT_PULSE_MILLIS, enabledConfig.schedulePolicy().pulseMillis());
		assertEquals(BotConfig.DEFAULT_FOREGROUND_MILLIS, enabledConfig.schedulePolicy().foregroundCadenceMillis());
		assertEquals(BotConfig.DEFAULT_BACKGROUND_MILLIS, enabledConfig.schedulePolicy().backgroundCadenceMillis());
		assertEquals(BotConfig.DEFAULT_MAX_DISPATCH_PER_PULSE, enabledConfig.schedulePolicy().maxDispatchPerPulse());
		assertEquals(BotConfig.DEFAULT_MAX_IN_FLIGHT, enabledConfig.schedulePolicy().maxInFlight());
		assertFalse(BotConfig.load(disabled).enabled());
	}

	@Test
	void loadsExplicitBoundedSchedulingPolicy() throws IOException
	{
		Path file = write("schedule.properties", """
			BotsEnabled = true
			BotSchedulerPulseMillis = 50
			BotForegroundDecisionMillis = 150
			BotBackgroundDecisionMillis = 3000
			BotMaxDispatchPerPulse = 24
			BotMaxInFlightDecisions = 3
			""");

		BotConfig config = BotConfig.load(file);
		assertEquals(50, config.schedulePolicy().pulseMillis());
		assertEquals(150, config.schedulePolicy().foregroundCadenceMillis());
		assertEquals(3000, config.schedulePolicy().backgroundCadenceMillis());
		assertEquals(24, config.schedulePolicy().maxDispatchPerPulse());
		assertEquals(3, config.schedulePolicy().maxInFlight());
	}

	@Test
	void rejectsMissingOrAmbiguousEnablement() throws IOException
	{
		Path missingProperty = write("missing.properties", "Unrelated = true\n");
		Path invalidProperty = write("invalid.properties", "BotsEnabled = yes\n");

		assertThrows(IllegalArgumentException.class, () -> BotConfig.load(missingProperty));
		assertThrows(IllegalArgumentException.class, () -> BotConfig.load(invalidProperty));
	}

	@Test
	void rejectsMalformedOrInvertedSchedulingPolicy() throws IOException
	{
		Path malformed = write("malformed.properties", "BotsEnabled = true\nBotMaxInFlightDecisions = many\n");
		Path inverted = write("inverted.properties", "BotsEnabled = true\nBotSchedulerPulseMillis = 500\nBotForegroundDecisionMillis = 250\n");

		assertThrows(IllegalArgumentException.class, () -> BotConfig.load(malformed));
		assertThrows(IllegalArgumentException.class, () -> BotConfig.load(inverted));
	}

	@Test
	void rejectsMissingConfigurationFile()
	{
		assertThrows(IOException.class, () -> BotConfig.load(_temporaryDirectory.resolve("absent.properties")));
	}

	private Path write(String fileName, String contents) throws IOException
	{
		return Files.writeString(_temporaryDirectory.resolve(fileName), contents);
	}
}
