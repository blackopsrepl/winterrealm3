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

/** Cadence tier for an automatically scheduled, loaded bot. */
public enum BotActivityTier
{
	FOREGROUND,
	BACKGROUND;

	/** Collapses live urgency facts into one deterministic cadence tier. */
	public static BotActivityTier classify(boolean selfActive, boolean nearbyHuman, boolean partyActive)
	{
		return selfActive || nearbyHuman || partyActive ? FOREGROUND : BACKGROUND;
	}
}
