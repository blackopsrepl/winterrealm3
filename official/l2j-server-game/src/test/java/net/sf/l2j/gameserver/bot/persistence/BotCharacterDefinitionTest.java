/*
 * Copyright © 2026 l2j-interlude contributors
 *
 * This file is part of l2j-interlude.
 *
 * l2j-interlude is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * l2j-interlude is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.bot.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.sf.l2j.gameserver.bot.BotMode;

class BotCharacterDefinitionTest
{
	@Test
	void normalizesTextAndRecognizesCompleteHomeLocation()
	{
		BotCharacterDefinition definition = definition(" bot:ada ", 10, 20, 30, 42);

		assertEquals("bot:ada", definition.accountName());
		assertEquals("duelist.default", definition.profileName());
		assertTrue(definition.hasHome());
	}

	@Test
	void acceptsAnAbsentHomeLocation()
	{
		BotCharacterDefinition definition = definition("bot:ada", null, null, null, 42);

		assertFalse(definition.hasHome());
	}

	@Test
	void rejectsHumanAccountsAndPartialHomeLocations()
	{
		assertThrows(IllegalArgumentException.class, () -> definition("human-account", null, null, null, 42));
		assertThrows(IllegalArgumentException.class, () -> definition("bot:ada", 10, null, 30, 42));
	}

	@Test
	void rejectsNonDeterministicZeroSeed()
	{
		assertThrows(IllegalArgumentException.class, () -> definition("bot:ada", null, null, null, 0));
	}

	private static BotCharacterDefinition definition(String accountName, Integer homeX, Integer homeY, Integer homeZ, long seed)
	{
		return new BotCharacterDefinition(100, accountName, true, BotMode.WORLD, null, " duelist.default ", "foreground", homeX, homeY, homeZ, seed, 1);
	}
}
