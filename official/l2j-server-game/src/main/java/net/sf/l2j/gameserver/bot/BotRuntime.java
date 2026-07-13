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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import net.sf.l2j.gameserver.bot.perception.BotPerceptionService;
import net.sf.l2j.gameserver.bot.persistence.BotCharacterDefinition;
import net.sf.l2j.gameserver.bot.profile.BotClassProfile;
import net.sf.l2j.gameserver.bot.profile.BotClassProfileKey;
import net.sf.l2j.gameserver.bot.scheduler.BotActivityTier;
import net.sf.l2j.gameserver.bot.scheduler.BotDecisionGate;
import net.sf.l2j.gameserver.bot.scheduler.BotScheduledRuntime;
import net.sf.l2j.gameserver.bot.strategy.BotDecisionOutcome;
import net.sf.l2j.gameserver.bot.tactics.BotLiveActionExecutor;
import net.sf.l2j.gameserver.bot.tactics.BotProfileBrain;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Owns controller state for one loaded bot without duplicating player state.
 */
public final class BotRuntime implements BotScheduledRuntime
{
	public static final int DECISION_HISTORY_CAPACITY = 32;
	public static final int FOREGROUND_HUMAN_RANGE = 3000;

	private final BotCharacterDefinition _definition;
	private final L2PcInstance _player;
	private final BotClassProfile _classProfile;
	private final BotDecisionGate _decisionGate = new BotDecisionGate();
	private final BotPerceptionService _perception = new BotPerceptionService();
	private final BotProfileBrain _brain;
	private final BotLiveActionExecutor _actions;
	private final Deque<BotRuntimeDecision> _recentDecisions = new ArrayDeque<>(DECISION_HISTORY_CAPACITY);
	private BotRuntimeState _state = BotRuntimeState.LOADING;

	/**
	 * Creates a runtime around an authoritative persisted player.
	 * @param definition the bot control metadata
	 * @param player the restored player entity
	 * @param classProfile the exact active-class behavior policy
	 */
	public BotRuntime(BotCharacterDefinition definition, L2PcInstance player, BotClassProfile classProfile)
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
		if (classProfile == null)
		{
			throw new IllegalArgumentException("Bot runtime class profile cannot be null");
		}
		BotClassProfileKey requiredProfile = new BotClassProfileKey(player.getClassId(), definition.profileName());
		if (!requiredProfile.equals(classProfile.getKey()))
		{
			throw new IllegalArgumentException("Bot runtime profile " + classProfile.getKey() + " does not match required exact profile " + requiredProfile);
		}

		_definition = definition;
		_player = player;
		_classProfile = classProfile;
		_brain = new BotProfileBrain(classProfile);
		_actions = new BotLiveActionExecutor(this);
	}

	public BotCharacterDefinition getDefinition()
	{
		return _definition;
	}

	public L2PcInstance getPlayer()
	{
		return _player;
	}

	public BotClassProfile getClassProfile()
	{
		return _classProfile;
	}

	public int getCharacterId()
	{
		return _definition.characterId();
	}

	public synchronized BotRuntimeState getState()
	{
		return _state;
	}

	/** Returns oldest-to-newest bounded decision diagnostics. */
	public synchronized List<BotRuntimeDecision> getRecentDecisions()
	{
		return List.copyOf(_recentDecisions);
	}

	@Override
	public synchronized boolean isDecisionEligible()
	{
		if (_state != BotRuntimeState.ACTIVE)
		{
			return false;
		}
		return _definition.mode().isAutomaticallyScheduled();
	}

	@Override
	public BotActivityTier getActivityTier()
	{
		requireActiveExactClassBinding();
		boolean selfActive = isActive(_player);
		boolean nearbyHuman = false;
		for (L2Object object : _player.getKnownList().getKnownObjects().values())
		{
			if ((object instanceof L2PcInstance player) && (player != _player) && !player.isBotControlled() && _player.isInsideRadius(player, FOREGROUND_HUMAN_RANGE, true, false))
			{
				nearbyHuman = true;
				break;
			}
		}

		boolean partyActive = false;
		if (_player.getParty() != null)
		{
			for (L2PcInstance member : List.copyOf(_player.getParty().getPartyMembers()))
			{
				if ((member != null) && (member != _player) && isActive(member))
				{
					partyActive = true;
					break;
				}
			}
		}
		return BotActivityTier.classify(selfActive, nearbyHuman, partyActive);
	}

	@Override
	public boolean tryBeginDecision(long nowNanos, long cadenceNanos)
	{
		synchronized (this)
		{
			if (!isDecisionEligible())
			{
				return false;
			}
			requireExactClassBinding();
		}
		return _decisionGate.tryAcquire(nowNanos, cadenceNanos);
	}

	@Override
	public void runDecision(long tick)
	{
		if (!_decisionGate.isInFlight())
		{
			throw new IllegalStateException("Bot decision ran without owning its admission gate");
		}
		requireActiveExactClassBinding();
		BotDecisionOutcome outcome = _brain.tick(_perception.capture(this, tick), _actions);
		recordDecision(BotRuntimeDecision.from(tick, _classProfile.getKey(), outcome));
		if (outcome.failure() != null)
		{
			throw outcome.failure();
		}
	}

	@Override
	public void finishDecision()
	{
		_decisionGate.release();
	}

	/** Waits for a class-change or teardown boundary to own this runtime. */
	public void awaitDecisionIdle()
	{
		_decisionGate.awaitIdle();
	}

	/** Marks a fully entered player ready for decisions. */
	public synchronized void activate()
	{
		transitionTo(BotRuntimeState.ACTIVE);
	}

	/** Begins teardown for a loading, active, or faulted runtime. */
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

	private synchronized void requireActiveExactClassBinding()
	{
		if (_state != BotRuntimeState.ACTIVE)
		{
			throw new IllegalStateException("Bot character " + getCharacterId() + " is not active for decisions");
		}
		requireExactClassBinding();
	}

	private void requireExactClassBinding()
	{
		if (_player.getClassId() == _classProfile.getClassId())
		{
			return;
		}
		transitionTo(BotRuntimeState.FAULTED);
		throw new IllegalStateException("Bot character " + getCharacterId() + " changed active class from exact profile " + _classProfile.getClassId() + " to " + _player.getClassId() + "; decisions are paused until an exact profile is installed");
	}

	private synchronized void recordDecision(BotRuntimeDecision decision)
	{
		if (_recentDecisions.size() == DECISION_HISTORY_CAPACITY)
		{
			_recentDecisions.removeFirst();
		}
		_recentDecisions.addLast(decision);
	}

	private static boolean isActive(L2PcInstance player)
	{
		return player.isInCombat() || player.isAttackingNow() || player.isCastingNow() || player.isMoving();
	}
}
