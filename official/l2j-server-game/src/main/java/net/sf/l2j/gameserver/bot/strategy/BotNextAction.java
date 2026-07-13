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
package net.sf.l2j.gameserver.bot.strategy;

/**
 * Named action plus deterministic relevance score.
 * @param actionName catalog action name
 * @param relevance positive finite priority
 */
public record BotNextAction(String actionName, double relevance)
{
	public BotNextAction
	{
		if (actionName == null)
		{
			throw new IllegalArgumentException("Bot action name cannot be null");
		}
		actionName = actionName.trim();
		if (actionName.isEmpty())
		{
			throw new IllegalArgumentException("Bot action name cannot be blank");
		}
		if (!Double.isFinite(relevance) || (relevance <= 0.0))
		{
			throw new IllegalArgumentException("Bot action relevance must be positive and finite");
		}
	}
}
