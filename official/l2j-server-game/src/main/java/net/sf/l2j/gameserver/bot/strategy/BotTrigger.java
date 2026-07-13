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
 *
 * Adapted from CMaNGOS playerbots Trigger and TriggerNode. Original C++
 * copyright (C) 2005-2018 MaNGOS project. Modified 2026-07-13.
 */
package net.sf.l2j.gameserver.bot.strategy;

import java.util.Optional;

/** Evaluates one tactical condition against an immutable context. */
public interface BotTrigger<C>
{
	String name();

	Optional<BotTriggerActivation> evaluate(C context);
}
