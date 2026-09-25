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
package net.sf.l2j.gameserver.bot.profile;

import net.sf.l2j.gameserver.model.base.ClassId;

/**
 * One exact bot class's knowledge about one exact observed player class.
 * Priorities are intentionally separate because protecting an ally, attacking
 * an enemy, controlling it, and interrupting it are different decisions.
 */
public record BotClassRelationship(ClassId classId, int allySupportPriority, int allyProtectionPriority, int hostileAttackPriority, int hostileThreatPriority, int hostileControlPriority, int hostileInterruptPriority)
{
	public static final int MINIMUM_PRIORITY = 1;
	public static final int MAXIMUM_PRIORITY = 6000;

	public BotClassRelationship
	{
		if ((classId == null) || (classId.getRace() == null))
		{
			throw new IllegalArgumentException("Class relationships require one exact playable ClassId");
		}
		requirePriority(allySupportPriority, "ally support");
		requirePriority(allyProtectionPriority, "ally protection");
		requirePriority(hostileAttackPriority, "hostile attack");
		requirePriority(hostileThreatPriority, "hostile threat");
		requirePriority(hostileControlPriority, "hostile control");
		requirePriority(hostileInterruptPriority, "hostile interrupt");
	}

	private static void requirePriority(int priority, String label)
	{
		if ((priority < MINIMUM_PRIORITY) || (priority > MAXIMUM_PRIORITY))
		{
			throw new IllegalArgumentException("Class relationship " + label + " priority must be between " + MINIMUM_PRIORITY + " and " + MAXIMUM_PRIORITY);
		}
	}
}
