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

		assertTrue(BotConfig.load(enabled).enabled());
		assertFalse(BotConfig.load(disabled).enabled());
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
	void rejectsMissingConfigurationFile()
	{
		assertThrows(IOException.class, () -> BotConfig.load(_temporaryDirectory.resolve("absent.properties")));
	}

	private Path write(String fileName, String contents) throws IOException
	{
		return Files.writeString(_temporaryDirectory.resolve(fileName), contents);
	}
}
