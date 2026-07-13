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
package net.sf.l2j.gameserver.bot.strategy;

/** One explainable stage in a tactical decision. */
public enum BotDecisionStep
{
	EXPIRED,
	DEFAULT_QUEUED,
	TRIGGERED,
	UNKNOWN_ACTION,
	USELESS,
	SCORE_MULTIPLIED,
	SUPPRESSED,
	DEFERRED,
	PREREQUISITE_QUEUED,
	IMPOSSIBLE,
	ALTERNATIVE_QUEUED,
	FAILED,
	CONTINUER_QUEUED,
	EXECUTED,
	BUDGET_EXHAUSTED,
	ERROR
}
