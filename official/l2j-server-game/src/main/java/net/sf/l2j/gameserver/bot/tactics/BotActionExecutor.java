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
package net.sf.l2j.gameserver.bot.tactics;

import net.sf.l2j.gameserver.bot.profile.BotSkillPolicy;
import net.sf.l2j.gameserver.model.actor.player.PlayerCommandResult;

/** Narrow execution boundary between pure tactical policy and live L2J state. */
public interface BotActionExecutor
{
	boolean isActive();

	boolean knowsSkill(int skillId);

	/**
	 * Returns whether a known skill currently has the reusable resources needed
	 * for an execution attempt. Target-specific validation remains in the normal
	 * player command path.
	 */
	boolean isSkillReady(int skillId);

	PlayerCommandResult selectTarget(int targetObjectId);

	PlayerCommandResult castSkill(BotSkillPolicy policy, int targetObjectId);

	PlayerCommandResult attack(int targetObjectId);
}
