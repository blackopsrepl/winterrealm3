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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.bot.persistence.BotCharacterDefinition;
import net.sf.l2j.gameserver.bot.persistence.BotCharacterRepository;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.player.PlayerControlType;
import net.sf.l2j.gameserver.model.actor.player.PlayerWorldLifecycle;

/**
 * Loads, owns, and cleanly unloads native clientless player characters.
 */
public final class BotEngine
{
	private static final Logger _log = Logger.getLogger(BotEngine.class.getName());

	private final BotRegistry _registry = new BotRegistry();
	private final BotCharacterRepository _repository = new BotCharacterRepository();
	private final PlayerWorldLifecycle _playerLifecycle = PlayerWorldLifecycle.getInstance();
	private boolean _started;
	private boolean _enabled;

	private BotEngine()
	{
	}

	public static BotEngine getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Loads configuration and enters every enabled persisted bot. No bot table is
	 * queried while the subsystem is disabled.
	 * @throws Exception when configuration, metadata, or player entry fails
	 */
	public synchronized void start() throws Exception
	{
		if (_started)
		{
			throw new IllegalStateException("Bot engine has already been started");
		}

		BotConfig config = BotConfig.load();
		_started = true;
		_enabled = config.enabled();
		if (!_enabled)
		{
			_log.info("Native player bots are disabled");
			return;
		}

		try
		{
			List<BotCharacterDefinition> definitions = _repository.loadEnabled();
			for (BotCharacterDefinition definition : definitions)
			{
				load(definition);
			}
			_log.info("Loaded " + _registry.size() + " native player bots");
		}
		catch (Exception e)
		{
			Exception failure = new Exception("Native player-bot startup failed", e);
			stopLoadedBots(failure);
			_started = false;
			_enabled = false;
			throw failure;
		}
	}

	/**
	 * Quiesces controller ownership and stores all loaded bots before shared
	 * executors and the database are stopped.
	 */
	public synchronized void stop()
	{
		if (!_started && _registry.isEmpty())
		{
			return;
		}

		int loadedBots = _registry.size();
		stopLoadedBots(null);
		_started = false;
		_enabled = false;
		if (loadedBots > 0)
		{
			_log.info("Stopped " + loadedBots + " native player bots");
		}
	}

	public synchronized boolean isStarted()
	{
		return _started;
	}

	public synchronized boolean isEnabled()
	{
		return _enabled;
	}

	public BotRegistry getRegistry()
	{
		return _registry;
	}

	private void load(BotCharacterDefinition definition)
	{
		L2PcInstance player = L2PcInstance.load(definition.characterId());
		if (player == null)
		{
			throw new IllegalStateException("Could not restore bot character " + definition.characterId());
		}

		player.setControlType(PlayerControlType.BOT);
		BotRuntime runtime = null;
		try
		{
			runtime = new BotRuntime(definition, player);
			_registry.register(runtime);
			_playerLifecycle.enterClientlessPlayer(player);
			runtime.activate();
		}
		catch (RuntimeException e)
		{
			if (runtime != null)
			{
				Exception cleanupFailure = unload(runtime);
				if (cleanupFailure != null)
				{
					e.addSuppressed(cleanupFailure);
				}
			}
			else
			{
				cleanupUnregisteredPlayer(player, e);
			}
			throw new IllegalStateException("Could not enter bot character " + definition.characterId(), e);
		}
	}

	private void stopLoadedBots(Exception startupFailure)
	{
		List<BotRuntime> runtimes = _registry.snapshot();
		for (int index = runtimes.size() - 1; index >= 0; index--)
		{
			BotRuntime runtime = runtimes.get(index);
			Exception failure = unload(runtime);
			if (failure == null)
			{
				continue;
			}
			if (startupFailure != null)
			{
				startupFailure.addSuppressed(failure);
			}
			else
			{
				_log.log(Level.SEVERE, "Could not cleanly unload bot character " + runtime.getCharacterId(), failure);
			}
		}
	}

	private Exception unload(BotRuntime runtime)
	{
		try
		{
			runtime.beginStopping();
			_playerLifecycle.leaveClientlessPlayer(runtime.getPlayer());
			runtime.markDormant();
			return null;
		}
		catch (RuntimeException e)
		{
			try
			{
				runtime.markFaulted();
			}
			catch (RuntimeException stateFailure)
			{
				e.addSuppressed(stateFailure);
			}
			return new Exception("Failed to unload bot character " + runtime.getCharacterId(), e);
		}
		finally
		{
			_registry.unregister(runtime);
		}
	}

	private static void cleanupUnregisteredPlayer(L2PcInstance player, RuntimeException failure)
	{
		try
		{
			player.deleteMe();
		}
		catch (RuntimeException cleanupFailure)
		{
			failure.addSuppressed(cleanupFailure);
		}
		try
		{
			player.store();
		}
		catch (RuntimeException cleanupFailure)
		{
			failure.addSuppressed(cleanupFailure);
		}
	}

	private static final class SingletonHolder
	{
		private static final BotEngine INSTANCE = new BotEngine();
	}
}
