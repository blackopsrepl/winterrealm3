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

/**
 * Explicit lifecycle state for one loaded bot runtime.
 */
public enum BotRuntimeState
{
	LOADING,
	ACTIVE,
	STOPPING,
	DORMANT,
	FAULTED;

	/**
	 * Checks whether this state may move directly to another state.
	 * @param next the requested next state
	 * @return {@code true} when the transition is valid
	 */
	public boolean canTransitionTo(BotRuntimeState next)
	{
		if (next == null)
		{
			return false;
		}
		return switch (this)
		{
			case LOADING -> (next == ACTIVE) || (next == STOPPING) || (next == FAULTED);
			case ACTIVE -> (next == STOPPING) || (next == FAULTED);
			case STOPPING -> (next == DORMANT) || (next == FAULTED);
			case FAULTED -> next == STOPPING;
			case DORMANT -> false;
		};
	}
}
