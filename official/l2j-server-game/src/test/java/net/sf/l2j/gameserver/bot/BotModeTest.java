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
package net.sf.l2j.gameserver.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BotModeTest
{
	@Test
	void parsesPersistedModesWithoutCaseOrWhitespaceSensitivity()
	{
		assertEquals(BotMode.COMPANION, BotMode.parse("  companion "));
	}

	@Test
	void rejectsMissingAndUnknownModes()
	{
		assertThrows(IllegalArgumentException.class, () -> BotMode.parse(null));
		assertThrows(IllegalArgumentException.class, () -> BotMode.parse(""));
		assertThrows(IllegalArgumentException.class, () -> BotMode.parse("unknown"));
	}

	@Test
	void schedulesOnlyAutonomousWorldModes()
	{
		assertTrue(BotMode.WORLD.isAutomaticallyScheduled());
		assertTrue(BotMode.COMPANION.isAutomaticallyScheduled());
		assertFalse(BotMode.SCENARIO.isAutomaticallyScheduled());
		assertFalse(BotMode.MANUAL.isAutomaticallyScheduled());
	}
}
