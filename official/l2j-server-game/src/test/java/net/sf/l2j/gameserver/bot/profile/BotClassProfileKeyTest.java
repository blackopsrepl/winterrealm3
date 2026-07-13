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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import net.sf.l2j.gameserver.model.base.ClassId;

class BotClassProfileKeyTest
{
	@Test
	void normalizesProfileNamesWithinOneExactClass()
	{
		BotClassProfileKey key = new BotClassProfileKey(ClassId.bishop, "  BISHOP.PARTY  ");

		assertEquals(ClassId.bishop, key.classId());
		assertEquals("bishop.party", key.profileName());
	}

	@Test
	void rejectsDummyClassesAndInvalidNames()
	{
		assertThrows(IllegalArgumentException.class, () -> new BotClassProfileKey(ClassId.dummyEntry1, "dummy.default"));
		assertThrows(IllegalArgumentException.class, () -> new BotClassProfileKey(ClassId.bishop, " "));
		assertThrows(IllegalArgumentException.class, () -> new BotClassProfileKey(ClassId.bishop, "x".repeat(BotClassProfileKey.MAXIMUM_NAME_LENGTH + 1)));
	}
}
