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
package net.sf.l2j.gameserver.bot.profile;

/**
 * Exact-class policy for acquiring and physically engaging hostile targets.
 * A support class may explicitly select {@link Targeting#NONE}; that choice is
 * profile data rather than a role-derived default.
 * @param targeting how this exact class acquires a combat target
 * @param acquisitionRange maximum initial target-search radius
 * @param maximumChaseDistance maximum distance from the engagement origin
 * @param preferredRange range the class attempts to maintain
 * @param basicAttackPriority decision relevance of a normal weapon attack
 */
public record BotCombatPolicy(Targeting targeting, int acquisitionRange, int maximumChaseDistance, int preferredRange, int basicAttackPriority)
{
	public static final int MAXIMUM_WORLD_RANGE = 20000;
	public static final int MAXIMUM_PRIORITY = 1000000;

	public BotCombatPolicy
	{
		if (targeting == null)
		{
			throw new IllegalArgumentException("Combat targeting cannot be null");
		}
		if ((acquisitionRange < 0) || (acquisitionRange > MAXIMUM_WORLD_RANGE))
		{
			throw new IllegalArgumentException("Combat acquisition range must be between 0 and " + MAXIMUM_WORLD_RANGE);
		}
		if ((maximumChaseDistance < 0) || (maximumChaseDistance > MAXIMUM_WORLD_RANGE))
		{
			throw new IllegalArgumentException("Maximum chase distance must be between 0 and " + MAXIMUM_WORLD_RANGE);
		}
		if ((preferredRange < 0) || (preferredRange > MAXIMUM_WORLD_RANGE))
		{
			throw new IllegalArgumentException("Preferred combat range must be between 0 and " + MAXIMUM_WORLD_RANGE);
		}
		if (preferredRange > maximumChaseDistance)
		{
			throw new IllegalArgumentException("Preferred combat range cannot exceed maximum chase distance");
		}
		if ((basicAttackPriority < 0) || (basicAttackPriority > MAXIMUM_PRIORITY))
		{
			throw new IllegalArgumentException("Basic attack priority must be between 0 and " + MAXIMUM_PRIORITY);
		}
		if ((targeting == Targeting.NONE) && ((acquisitionRange != 0) || (maximumChaseDistance != 0) || (preferredRange != 0) || (basicAttackPriority != 0)))
		{
			throw new IllegalArgumentException("A non-combat profile must use zero combat ranges and priority");
		}
		if ((targeting != Targeting.NONE) && ((acquisitionRange == 0) || (maximumChaseDistance == 0)))
		{
			throw new IllegalArgumentException("A combat profile must define positive acquisition and chase ranges");
		}
	}

	public enum Targeting
	{
		NONE,
		CURRENT_HOSTILE,
		NEAREST_HOSTILE,
		PARTY_ASSIST,
		OWNER_ASSIST
	}
}
