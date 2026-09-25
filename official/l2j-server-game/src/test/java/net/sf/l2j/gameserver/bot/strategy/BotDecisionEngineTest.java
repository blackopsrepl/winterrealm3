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
package net.sf.l2j.gameserver.bot.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class BotDecisionEngineTest
{
	@Test
	void executesAtMostOneHighestScoredActionPerTick()
	{
		TestContext context = new TestContext();
		BotDecisionEngine<TestContext> engine = engine(List.of(node("low"), node("high")), new OneShotStrategy(List.of(next("low", 10), next("high", 20))));

		BotDecisionOutcome first = engine.tick(context, 1);
		BotDecisionOutcome second = engine.tick(context, 2);

		assertEquals(BotDecisionStatus.EXECUTED, first.status());
		assertEquals("high", first.actionName());
		assertEquals(1, first.pendingActions());
		assertEquals("low", second.actionName());
		assertEquals(List.of("high", "low"), context.executedActions);
		assertThrows(IllegalArgumentException.class, () -> engine.tick(context, 2));
	}

	@Test
	void resolvesEqualScoresByStableActionNameOrder()
	{
		TestContext context = new TestContext();
		BotDecisionEngine<TestContext> engine = engine(List.of(node("bravo"), node("alpha")), new OneShotStrategy(List.of(next("bravo", 10), next("alpha", 10))));

		BotDecisionOutcome outcome = engine.tick(context, 1);

		assertEquals("alpha", outcome.actionName());
	}

	@Test
	void retainsMainActionAcrossATickThatExecutesItsPrerequisite()
	{
		TestContext context = new TestContext();
		BotActionNode<TestContext> main = new BotActionNode<>(new TestAction("main"), List.of(next("prepare", 10)), List.of(), List.of());
		BotDecisionEngine<TestContext> engine = engine(List.of(main, node("prepare")), new OneShotStrategy(List.of(next("main", 20))));

		BotDecisionOutcome first = engine.tick(context, 1);
		BotDecisionOutcome second = engine.tick(context, 2);

		assertEquals("prepare", first.actionName());
		assertEquals(1, first.pendingActions());
		assertEquals("main", second.actionName());
		assertEquals(List.of("prepare", "main"), context.executedActions);
		assertTrue(first.trace().entries().stream().anyMatch(entry -> entry.step() == BotDecisionStep.PREREQUISITE_QUEUED));
	}

	@Test
	void executesAnAlternativeWhenThePreferredActionIsImpossibleOrFails()
	{
		TestContext impossibleContext = new TestContext();
		BotActionNode<TestContext> impossible = new BotActionNode<>(new TestAction("primary", true, false, BotActionResult.SUCCEEDED), List.of(), List.of(next("alternative", 5)), List.of());
		BotDecisionEngine<TestContext> impossibleEngine = engine(List.of(impossible, node("alternative")), new OneShotStrategy(List.of(next("primary", 20))));

		BotDecisionOutcome impossibleOutcome = impossibleEngine.tick(impossibleContext, 1);

		assertEquals("alternative", impossibleOutcome.actionName());
		assertTrue(impossibleOutcome.trace().entries().stream().anyMatch(entry -> entry.step() == BotDecisionStep.IMPOSSIBLE));

		TestContext failedContext = new TestContext();
		BotActionNode<TestContext> failed = new BotActionNode<>(new TestAction("primary", true, true, BotActionResult.FAILED), List.of(), List.of(next("alternative", 5)), List.of());
		BotDecisionEngine<TestContext> failedEngine = engine(List.of(failed, node("alternative")), new OneShotStrategy(List.of(next("primary", 20))));

		BotDecisionOutcome failedOutcome = failedEngine.tick(failedContext, 1);

		assertEquals("alternative", failedOutcome.actionName());
		assertEquals(List.of("primary", "alternative"), failedContext.executedActions);
		assertTrue(failedOutcome.trace().entries().stream().anyMatch(entry -> entry.step() == BotDecisionStep.FAILED));
	}

	@Test
	void appliesMultipliersBeforeSelectingTheExecutableAction()
	{
		TestContext context = new TestContext();
		BotScoreMultiplier<TestContext> suppressHigh = new BotScoreMultiplier<>()
		{
			@Override
			public String name()
			{
				return "suppress high";
			}

			@Override
			public double value(TestContext ignored, BotAction<TestContext> action, BotEvent event)
			{
				return action.name().equals("high") ? 0.0 : 1.0;
			}
		};
		OneShotStrategy strategy = new OneShotStrategy(List.of(next("low", 10), next("high", 20)), List.of(suppressHigh));
		BotDecisionEngine<TestContext> engine = engine(List.of(node("low"), node("high")), strategy);

		BotDecisionOutcome outcome = engine.tick(context, 1);

		assertEquals("low", outcome.actionName());
		assertTrue(outcome.trace().entries().stream().anyMatch(entry -> entry.step() == BotDecisionStep.SUPPRESSED));
	}

	@Test
	void deDuplicatesTriggeredActionsAtTheirHighestScore()
	{
		TestContext context = new TestContext();
		BotTrigger<TestContext> low = trigger("low trigger", "attack", 10);
		BotTrigger<TestContext> high = trigger("high trigger", "attack", 30);
		OneShotStrategy strategy = new OneShotStrategy(List.of(), List.of(), List.of(low, high));
		BotDecisionEngine<TestContext> engine = engine(List.of(node("attack")), strategy);

		BotDecisionOutcome outcome = engine.tick(context, 1);

		assertEquals("attack", outcome.actionName());
		BotDecisionTraceEntry execution = outcome.trace().entries().stream().filter(entry -> entry.step() == BotDecisionStep.EXECUTED).findFirst().orElseThrow();
		assertEquals(30.0, execution.relevance());
		assertEquals("high trigger", execution.source());
	}

	@Test
	void retainsContinuersAndExpiresStaleQueuedActions()
	{
		TestContext continuationContext = new TestContext();
		BotActionNode<TestContext> first = new BotActionNode<>(new TestAction("first"), List.of(), List.of(), List.of(next("second", 10)));
		BotDecisionEngine<TestContext> continuationEngine = engine(List.of(first, node("second")), new OneShotStrategy(List.of(next("first", 20))));

		BotDecisionOutcome firstOutcome = continuationEngine.tick(continuationContext, 1);
		BotDecisionOutcome secondOutcome = continuationEngine.tick(continuationContext, 2);

		assertEquals("first", firstOutcome.actionName());
		assertEquals("second", secondOutcome.actionName());
		assertEquals(List.of("first", "second"), continuationContext.executedActions);
		assertTrue(firstOutcome.trace().entries().stream().anyMatch(entry -> entry.step() == BotDecisionStep.CONTINUER_QUEUED));

		TestContext expiryContext = new TestContext();
		BotDecisionEngine<TestContext> expiryEngine = engine(List.of(node("high"), node("stale")), new OneShotStrategy(List.of(next("high", 20), next("stale", 10))));
		expiryEngine.tick(expiryContext, 1);

		BotDecisionOutcome expired = expiryEngine.tick(expiryContext, 22);

		assertEquals(BotDecisionStatus.IDLE, expired.status());
		assertTrue(expired.trace().entries().stream().anyMatch(entry -> entry.step() == BotDecisionStep.EXPIRED));
	}

	@Test
	void reportsIterationBudgetExhaustionWithPendingWork()
	{
		BotDecisionEngine<TestContext> engine = new BotDecisionEngine<>(new BotActionCatalog<>(List.of()), List.of(new OneShotStrategy(List.of(next("unknown-a", 20), next("unknown-b", 10)))), 1, 20, 16);

		BotDecisionOutcome outcome = engine.tick(new TestContext(), 1);

		assertEquals(BotDecisionStatus.BUDGET_EXHAUSTED, outcome.status());
		assertEquals(1, outcome.pendingActions());
		assertTrue(outcome.trace().entries().stream().anyMatch(entry -> entry.step() == BotDecisionStep.BUDGET_EXHAUSTED));
	}

	@Test
	void surfacesErrorsAndBoundsDiagnosticTraces()
	{
		TestContext errorContext = new TestContext();
		BotAction<TestContext> exploding = new TestAction("explode")
		{
			@Override
			public BotActionResult execute(TestContext context, BotEvent event)
			{
				throw new IllegalStateException("boom");
			}
		};
		BotDecisionEngine<TestContext> errorEngine = engine(List.of(new BotActionNode<>(exploding)), new OneShotStrategy(List.of(next("explode", 10))));

		BotDecisionOutcome error = errorEngine.tick(errorContext, 1);

		assertEquals(BotDecisionStatus.ERROR, error.status());
		assertNotNull(error.failure());
		assertTrue(error.trace().entries().stream().anyMatch(entry -> entry.step() == BotDecisionStep.ERROR));

		BotDecisionEngine<TestContext> bounded = new BotDecisionEngine<>(new BotActionCatalog<>(List.of()), List.of(new OneShotStrategy(List.of(next("unknown-a", 20), next("unknown-b", 10)))), 8, 20, 2);
		BotDecisionOutcome boundedOutcome = bounded.tick(new TestContext(), 1);

		assertTrue(boundedOutcome.trace().isTruncated());
		assertEquals(2, boundedOutcome.trace().entries().size());
	}

	@Test
	void validatesEveryActionNodeLinkAtCatalogConstruction()
	{
		BotActionNode<TestContext> invalid = new BotActionNode<>(new TestAction("main"), List.of(next("missing", 1)), List.of(), List.of());

		assertThrows(IllegalArgumentException.class, () -> new BotActionCatalog<>(List.of(invalid)));
	}

	private static BotDecisionEngine<TestContext> engine(List<BotActionNode<TestContext>> nodes, BotStrategy<TestContext> strategy)
	{
		return new BotDecisionEngine<>(new BotActionCatalog<>(nodes), List.of(strategy));
	}

	private static BotActionNode<TestContext> node(String name)
	{
		return new BotActionNode<>(new TestAction(name));
	}

	private static BotNextAction next(String name, double relevance)
	{
		return new BotNextAction(name, relevance);
	}

	private static BotTrigger<TestContext> trigger(String name, String action, double relevance)
	{
		return new BotTrigger<>()
		{
			@Override
			public String name()
			{
				return name;
			}

			@Override
			public Optional<BotTriggerActivation> evaluate(TestContext context)
			{
				return Optional.of(new BotTriggerActivation(new BotEvent(name), List.of(next(action, relevance))));
			}
		};
	}

	private static final class TestContext
	{
		private final List<String> executedActions = new ArrayList<>();
	}

	private static class TestAction implements BotAction<TestContext>
	{
		private final String _name;
		private final boolean _useful;
		private final boolean _possible;
		private final BotActionResult _result;

		TestAction(String name)
		{
			this(name, true, true, BotActionResult.SUCCEEDED);
		}

		TestAction(String name, boolean useful, boolean possible, BotActionResult result)
		{
			_name = name;
			_useful = useful;
			_possible = possible;
			_result = result;
		}

		@Override
		public String name()
		{
			return _name;
		}

		@Override
		public boolean isUseful(TestContext context)
		{
			return _useful;
		}

		@Override
		public boolean isPossible(TestContext context)
		{
			return _possible;
		}

		@Override
		public BotActionResult execute(TestContext context, BotEvent event)
		{
			context.executedActions.add(_name);
			return _result;
		}
	}

	private static final class OneShotStrategy implements BotStrategy<TestContext>
	{
		private final List<BotNextAction> _defaults;
		private final List<BotScoreMultiplier<TestContext>> _multipliers;
		private final List<BotTrigger<TestContext>> _triggers;
		private boolean _defaultsEmitted;

		OneShotStrategy(List<BotNextAction> defaults)
		{
			this(defaults, List.of(), List.of());
		}

		OneShotStrategy(List<BotNextAction> defaults, List<BotScoreMultiplier<TestContext>> multipliers)
		{
			this(defaults, multipliers, List.of());
		}

		OneShotStrategy(List<BotNextAction> defaults, List<BotScoreMultiplier<TestContext>> multipliers, List<BotTrigger<TestContext>> triggers)
		{
			_defaults = defaults;
			_multipliers = multipliers;
			_triggers = triggers;
		}

		@Override
		public String name()
		{
			return "test";
		}

		@Override
		public List<BotNextAction> defaultActions(TestContext context)
		{
			if (_defaultsEmitted)
			{
				return List.of();
			}
			_defaultsEmitted = true;
			return _defaults;
		}

		@Override
		public List<BotScoreMultiplier<TestContext>> multipliers()
		{
			return _multipliers;
		}

		@Override
		public List<BotTrigger<TestContext>> triggers()
		{
			return _triggers;
		}
	}
}
