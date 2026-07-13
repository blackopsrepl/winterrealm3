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

import net.sf.l2j.gameserver.bot.persistence.BotCharacterDefinition;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Owns controller state for one loaded bot without duplicating player state.
 */
public final class BotRuntime
{
	private final BotCharacterDefinition _definition;
	private final L2PcInstance _player;
	private BotRuntimeState _state = BotRuntimeState.LOADING;

	/**
	 * Creates a runtime around an authoritative persisted player.
	 * @param definition the bot control metadata
	 * @param player the restored player entity
	 */
	public BotRuntime(BotCharacterDefinition definition, L2PcInstance player)
	{
		if (definition == null)
		{
			throw new IllegalArgumentException("Bot definition cannot be null");
		}
		if (!definition.enabled())
		{
			throw new IllegalArgumentException("Cannot create a runtime for a disabled bot");
		}
		if (player == null)
		{
			throw new IllegalArgumentException("Bot player cannot be null");
		}
		if (player.getObjectId() != definition.characterId())
		{
			throw new IllegalArgumentException("Bot player id does not match its definition");
		}
		if (!definition.accountName().equals(player.getAccountName()))
		{
			throw new IllegalArgumentException("Bot player account does not match its definition");
		}
		if (!player.isBotControlled())
		{
			throw new IllegalArgumentException("Bot runtime requires a bot-controlled player");
		}
		if (player.getClient() != null)
		{
			throw new IllegalArgumentException("Bot runtime cannot own a game client");
		}

		_definition = definition;
		_player = player;
	}

	public BotCharacterDefinition getDefinition()
	{
		return _definition;
	}

	public L2PcInstance getPlayer()
	{
		return _player;
	}

	public int getCharacterId()
	{
		return _definition.characterId();
	}

	public synchronized BotRuntimeState getState()
	{
		return _state;
	}

	/** Marks a fully entered player ready for decisions. */
	public synchronized void activate()
	{
		transitionTo(BotRuntimeState.ACTIVE);
	}

	/** Begins teardown for either a loading or active runtime. */
	public synchronized void beginStopping()
	{
		if (_state != BotRuntimeState.STOPPING)
		{
			transitionTo(BotRuntimeState.STOPPING);
		}
	}

	/** Marks a successfully stored and unloaded runtime dormant. */
	public synchronized void markDormant()
	{
		transitionTo(BotRuntimeState.DORMANT);
	}

	/** Marks a runtime that could not complete its current lifecycle phase. */
	public synchronized void markFaulted()
	{
		if (_state != BotRuntimeState.FAULTED)
		{
			transitionTo(BotRuntimeState.FAULTED);
		}
	}

	private void transitionTo(BotRuntimeState next)
	{
		if (!_state.canTransitionTo(next))
		{
			throw new IllegalStateException("Invalid bot runtime transition from " + _state + " to " + next);
		}
		_state = next;
	}
}
