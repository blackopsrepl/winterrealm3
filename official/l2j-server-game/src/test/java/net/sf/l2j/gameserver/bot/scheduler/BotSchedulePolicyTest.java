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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class BotSchedulePolicyTest
{
	@Test
	void mapsActivityTiersToTheirExactCadence()
	{
		BotSchedulePolicy policy = new BotSchedulePolicy(100, 250, 2000, 32, 8);

		assertEquals(BotActivityTier.FOREGROUND, BotActivityTier.classify(true, false, false));
		assertEquals(BotActivityTier.FOREGROUND, BotActivityTier.classify(false, true, false));
		assertEquals(BotActivityTier.FOREGROUND, BotActivityTier.classify(false, false, true));
		assertEquals(BotActivityTier.BACKGROUND, BotActivityTier.classify(false, false, false));
		assertEquals(TimeUnit.MILLISECONDS.toNanos(250), policy.cadenceNanos(BotActivityTier.FOREGROUND));
		assertEquals(TimeUnit.MILLISECONDS.toNanos(2000), policy.cadenceNanos(BotActivityTier.BACKGROUND));
	}

	@Test
	void rejectsUnboundedOrInvertedPolicies()
	{
		assertThrows(IllegalArgumentException.class, () -> new BotSchedulePolicy(0, 250, 2000, 32, 8));
		assertThrows(IllegalArgumentException.class, () -> new BotSchedulePolicy(500, 250, 2000, 32, 8));
		assertThrows(IllegalArgumentException.class, () -> new BotSchedulePolicy(100, 2000, 1000, 32, 8));
		assertThrows(IllegalArgumentException.class, () -> new BotSchedulePolicy(100, 250, 2000, 0, 8));
		assertThrows(IllegalArgumentException.class, () -> new BotSchedulePolicy(100, 250, 2000, 32, 0));
		assertThrows(IllegalArgumentException.class, () -> new BotSchedulePolicy(100, 250, 2000, 32, 8).cadenceNanos(null));
	}
}
