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
 * Adapted from CMaNGOS playerbots Engine. Original C++ copyright
 * (C) 2005-2018 MaNGOS project. Translated and substantially modified
 * for deterministic L2J execution on 2026-07-13.
 */
package net.sf.l2j.gameserver.bot.strategy;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.sf.l2j.gameserver.bot.strategy.BotDecisionQueue.QueuedAction;

/**
 * Deterministic trigger-and-score strategy kernel. The engine retains pending
 * actions between ticks and executes at most one successful action per tick.
 * @param <C> immutable decision context type
 */
public final class BotDecisionEngine<C>
{
	private static final double LINK_RELEVANCE_SCALE = 0.001;
	private static final double REQUEUE_BONUS = 0.001;

	private final BotActionCatalog<C> _catalog;
	private final List<BotStrategy<C>> _strategies;
	private final BotDecisionQueue _queue = new BotDecisionQueue();
	private final int _maximumIterations;
	private final long _maximumQueueAgeTicks;
	private final int _traceCapacity;
	private long _lastTick = -1;

	public BotDecisionEngine(BotActionCatalog<C> catalog, List<BotStrategy<C>> strategies)
	{
		this(catalog, strategies, 64, 20, 128);
	}

	public BotDecisionEngine(BotActionCatalog<C> catalog, List<BotStrategy<C>> strategies, int maximumIterations, long maximumQueueAgeTicks, int traceCapacity)
	{
		if (catalog == null)
		{
			throw new IllegalArgumentException("Bot action catalog cannot be null");
		}
		if ((strategies == null) || strategies.stream().anyMatch(strategy -> strategy == null))
		{
			throw new IllegalArgumentException("Bot strategies cannot be null or contain null");
		}
		if ((maximumIterations < 1) || (maximumQueueAgeTicks < 1) || (traceCapacity < 1))
		{
			throw new IllegalArgumentException("Bot decision limits must be positive");
		}

		Set<String> strategyNames = new HashSet<>();
		for (BotStrategy<C> strategy : strategies)
		{
			String name = requireName(strategy.name(), "strategy");
			if (!strategyNames.add(name))
			{
				throw new IllegalArgumentException("Duplicate bot strategy: " + name);
			}
		}

		_catalog = catalog;
		_strategies = List.copyOf(strategies);
		_maximumIterations = maximumIterations;
		_maximumQueueAgeTicks = maximumQueueAgeTicks;
		_traceCapacity = traceCapacity;
	}

	/**
	 * Evaluates one bounded decision tick.
	 * @param context immutable snapshot and controller context
	 * @param tick monotonic deterministic scheduler tick
	 * @return observable outcome and bounded trace
	 */
	public synchronized BotDecisionOutcome tick(C context, long tick)
	{
		if (context == null)
		{
			throw new IllegalArgumentException("Bot decision context cannot be null");
		}
		if (tick <= _lastTick)
		{
			throw new IllegalArgumentException("Bot decision tick must increase monotonically");
		}
		_lastTick = tick;

		BotDecisionTrace trace = new BotDecisionTrace(_traceCapacity);
		String currentAction = "<engine>";
		double currentRelevance = 0.0;
		String currentSource = "engine";
		try
		{
			for (QueuedAction expired : _queue.removeExpired(tick, _maximumQueueAgeTicks))
			{
				trace.add(tick, expired.actionName(), expired.relevance(), BotDecisionStep.EXPIRED, expired.event().source(), "queue age exceeded");
			}
			enqueueStrategies(context, tick, trace);

			int iterations = 0;
			while (iterations++ < _maximumIterations)
			{
				QueuedAction queued = _queue.poll();
				if (queued == null)
				{
					return new BotDecisionOutcome(BotDecisionStatus.IDLE, "", trace, 0, null);
				}

				currentAction = queued.actionName();
				currentRelevance = queued.relevance();
				currentSource = queued.event().source();
				BotActionNode<C> node = _catalog.get(queued.actionName());
				if (node == null)
				{
					trace.add(tick, queued.actionName(), queued.relevance(), BotDecisionStep.UNKNOWN_ACTION, currentSource, "not present in action catalog");
					continue;
				}

				BotAction<C> action = node.action();
				if (!action.isUseful(context))
				{
					trace.add(tick, node.name(), queued.relevance(), BotDecisionStep.USELESS, currentSource, "action reported no current utility");
					continue;
				}

				double relevance = applyMultipliers(context, action, queued.event(), queued.relevance(), tick, trace);
				currentRelevance = relevance;
				if (relevance <= 0.0)
				{
					trace.add(tick, node.name(), relevance, BotDecisionStep.SUPPRESSED, currentSource, "score multiplier suppressed action");
					continue;
				}

				QueuedAction next = _queue.peek();
				if ((relevance < queued.relevance()) && (next != null) && (next.relevance() > relevance))
				{
					_queue.offer(node.name(), relevance, queued.skipPrerequisites(), queued.event(), tick);
					trace.add(tick, node.name(), relevance, BotDecisionStep.DEFERRED, currentSource, "another action now has a higher score");
					continue;
				}

				if (!queued.skipPrerequisites() && !node.prerequisites().isEmpty())
				{
					_queue.offer(node.name(), relevance + REQUEUE_BONUS, true, queued.event(), tick);
					enqueueLinked(node.prerequisites(), relevance, queued.event(), tick, trace, BotDecisionStep.PREREQUISITE_QUEUED, "prerequisite for " + node.name());
					continue;
				}

				if (!action.isPossible(context))
				{
					trace.add(tick, node.name(), relevance, BotDecisionStep.IMPOSSIBLE, currentSource, "action preconditions failed");
					enqueueLinked(node.alternatives(), relevance, queued.event(), tick, trace, BotDecisionStep.ALTERNATIVE_QUEUED, "alternative to impossible " + node.name());
					continue;
				}

				BotActionResult result = action.execute(context, queued.event());
				if (result == null)
				{
					throw new IllegalStateException("Bot action returned no result: " + node.name());
				}
				if (result == BotActionResult.FAILED)
				{
					trace.add(tick, node.name(), relevance, BotDecisionStep.FAILED, currentSource, "action execution failed");
					enqueueLinked(node.alternatives(), relevance, queued.event(), tick, trace, BotDecisionStep.ALTERNATIVE_QUEUED, "alternative to failed " + node.name());
					continue;
				}

				trace.add(tick, node.name(), relevance, BotDecisionStep.EXECUTED, currentSource, queued.event().detail());
				enqueueLinked(node.continuers(), 0.0, new BotEvent("continuer:" + node.name()), tick, trace, BotDecisionStep.CONTINUER_QUEUED, "continuation after " + node.name());
				return new BotDecisionOutcome(BotDecisionStatus.EXECUTED, node.name(), trace, _queue.size(), null);
			}

			if (_queue.size() > 0)
			{
				trace.add(tick, "<engine>", 0.0, BotDecisionStep.BUDGET_EXHAUSTED, "engine", "iteration budget reached with pending actions");
				return new BotDecisionOutcome(BotDecisionStatus.BUDGET_EXHAUSTED, "", trace, _queue.size(), null);
			}
			return new BotDecisionOutcome(BotDecisionStatus.IDLE, "", trace, 0, null);
		}
		catch (RuntimeException e)
		{
			trace.add(tick, currentAction, currentRelevance, BotDecisionStep.ERROR, currentSource, e.getClass().getSimpleName() + ": " + e.getMessage());
			return new BotDecisionOutcome(BotDecisionStatus.ERROR, "", trace, _queue.size(), e);
		}
	}

	public synchronized int pendingActions()
	{
		return _queue.size();
	}

	private void enqueueStrategies(C context, long tick, BotDecisionTrace trace)
	{
		for (BotStrategy<C> strategy : _strategies)
		{
			String strategyName = requireName(strategy.name(), "strategy");
			List<BotNextAction> defaults = requireList(strategy.defaultActions(context), "default actions", strategyName);
			BotEvent defaultEvent = new BotEvent("strategy:" + strategyName);
			for (BotNextAction action : defaults)
			{
				_queue.offer(action.actionName(), action.relevance(), false, defaultEvent, tick);
				trace.add(tick, action.actionName(), action.relevance(), BotDecisionStep.DEFAULT_QUEUED, defaultEvent.source(), "");
			}

			for (BotTrigger<C> trigger : requireList(strategy.triggers(), "triggers", strategyName))
			{
				if (trigger == null)
				{
					throw new IllegalStateException("Strategy " + strategyName + " contains a null trigger");
				}
				requireName(trigger.name(), "trigger");
				Optional<BotTriggerActivation> activation = trigger.evaluate(context);
				if ((activation == null) || activation.isEmpty())
				{
					continue;
				}
				BotTriggerActivation active = activation.orElseThrow();
				for (BotNextAction action : active.actions())
				{
					_queue.offer(action.actionName(), action.relevance(), false, active.event(), tick);
					trace.add(tick, action.actionName(), action.relevance(), BotDecisionStep.TRIGGERED, active.event().source(), trigger.name());
				}
			}
		}
	}

	private double applyMultipliers(C context, BotAction<C> action, BotEvent event, double relevance, long tick, BotDecisionTrace trace)
	{
		double adjusted = relevance;
		for (BotStrategy<C> strategy : _strategies)
		{
			for (BotScoreMultiplier<C> multiplier : requireList(strategy.multipliers(), "multipliers", strategy.name()))
			{
				if (multiplier == null)
				{
					throw new IllegalStateException("Strategy " + strategy.name() + " contains a null multiplier");
				}
				double factor = multiplier.value(context, action, event);
				if (!Double.isFinite(factor) || (factor < 0.0))
				{
					throw new IllegalStateException("Invalid score multiplier " + multiplier.name() + ": " + factor);
				}
				if (factor != 1.0)
				{
					adjusted *= factor;
					trace.add(tick, action.name(), adjusted, BotDecisionStep.SCORE_MULTIPLIED, event.source(), multiplier.name() + " x" + factor);
				}
			}
		}
		if (!Double.isFinite(adjusted))
		{
			throw new IllegalStateException("Bot action score overflowed for " + action.name());
		}
		return adjusted;
	}

	private void enqueueLinked(List<BotNextAction> links, double parentRelevance, BotEvent parentEvent, long tick, BotDecisionTrace trace, BotDecisionStep step, String detail)
	{
		for (BotNextAction link : links)
		{
			double relevance = (parentRelevance > 0.0) ? parentRelevance + REQUEUE_BONUS + (link.relevance() * LINK_RELEVANCE_SCALE) : link.relevance();
			BotEvent event = new BotEvent(parentEvent.source(), detail);
			_queue.offer(link.actionName(), relevance, false, event, tick);
			trace.add(tick, link.actionName(), relevance, step, event.source(), detail);
		}
	}

	private static String requireName(String name, String type)
	{
		if ((name == null) || name.trim().isEmpty())
		{
			throw new IllegalArgumentException("Bot " + type + " name cannot be blank");
		}
		return name.trim();
	}

	private static <T> List<T> requireList(List<T> values, String type, String owner)
	{
		if (values == null)
		{
			throw new IllegalStateException("Bot " + owner + " returned null " + type);
		}
		return values;
	}
}
