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
package net.sf.l2j.gameserver.bot.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BotDecisionGateTest
{
	@Test
	void serializesDecisionsAndEnforcesCadence()
	{
		BotDecisionGate gate = new BotDecisionGate();

		assertTrue(gate.tryAcquire(-100, 50));
		assertTrue(gate.isInFlight());
		assertEquals(-50, gate.getNextDueNanos());
		assertFalse(gate.tryAcquire(100, 50));

		gate.release();
		assertFalse(gate.tryAcquire(-51, 50));
		assertTrue(gate.tryAcquire(-50, 50));
		gate.release();
		assertThrows(IllegalStateException.class, gate::release);
		assertThrows(IllegalArgumentException.class, () -> gate.tryAcquire(0, 0));
	}

	@Test
	void saturatesTheNextDueTimeAtLongMaximum()
	{
		BotDecisionGate gate = new BotDecisionGate();

		assertTrue(gate.tryAcquire(Long.MAX_VALUE - 5, 10));
		assertEquals(Long.MAX_VALUE, gate.getNextDueNanos());
	}
}
