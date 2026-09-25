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
package net.sf.l2j.gameserver.model.actor.player;

/**
 * Semantic outcome returned by shared player commands. These outcomes are
 * suitable for bot decision traces; packet presentation remains at the client
 * handler boundary.
 */
public enum PlayerCommandResult
{
	ACCEPTED(true),
	TARGET_SELECTED(true),
	TARGET_REJECTED(false),
	PLAYER_UNAVAILABLE(false),
	TARGET_UNAVAILABLE(false),
	ACTION_BLOCKED(false),
	SELF_TARGET(false),
	OUT_OF_CONTROL(false),
	MOVEMENT_BLOCKED(false),
	DESTINATION_TOO_FAR(false),
	SKILL_NOT_KNOWN(false),
	SKILL_NOT_FOUND(false),
	RECALL_RESTRICTED(false);

	private final boolean _accepted;

	PlayerCommandResult(boolean accepted)
	{
		_accepted = accepted;
	}

	public boolean isAccepted()
	{
		return _accepted;
	}
}
