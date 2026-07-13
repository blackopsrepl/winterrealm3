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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BotRuntimeStateTest
{
	@Test
	void allowsOnlyExplicitLifecycleTransitions()
	{
		assertTrue(BotRuntimeState.LOADING.canTransitionTo(BotRuntimeState.ACTIVE));
		assertTrue(BotRuntimeState.LOADING.canTransitionTo(BotRuntimeState.STOPPING));
		assertTrue(BotRuntimeState.ACTIVE.canTransitionTo(BotRuntimeState.STOPPING));
		assertTrue(BotRuntimeState.STOPPING.canTransitionTo(BotRuntimeState.DORMANT));
		assertTrue(BotRuntimeState.STOPPING.canTransitionTo(BotRuntimeState.FAULTED));
		assertTrue(BotRuntimeState.FAULTED.canTransitionTo(BotRuntimeState.STOPPING));

		assertFalse(BotRuntimeState.ACTIVE.canTransitionTo(BotRuntimeState.DORMANT));
		assertFalse(BotRuntimeState.DORMANT.canTransitionTo(BotRuntimeState.ACTIVE));
		assertFalse(BotRuntimeState.FAULTED.canTransitionTo(BotRuntimeState.ACTIVE));
		assertFalse(BotRuntimeState.LOADING.canTransitionTo(null));
	}
}
