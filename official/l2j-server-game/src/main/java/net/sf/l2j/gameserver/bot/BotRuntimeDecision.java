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
package net.sf.l2j.gameserver.bot;

import java.util.List;

import net.sf.l2j.gameserver.bot.profile.BotClassProfileKey;
import net.sf.l2j.gameserver.bot.strategy.BotDecisionOutcome;
import net.sf.l2j.gameserver.bot.strategy.BotDecisionStatus;
import net.sf.l2j.gameserver.bot.strategy.BotDecisionTraceEntry;

/** Immutable operator-facing record of one exact-class decision. */
public record BotRuntimeDecision(long tick, BotClassProfileKey profile, BotDecisionStatus status, String actionName, List<BotDecisionTraceEntry> trace, boolean traceTruncated, int pendingActions, String failureDetail)
{
	public BotRuntimeDecision
	{
		if ((tick <= 0) || (profile == null) || (status == null) || (trace == null) || (pendingActions < 0))
		{
			throw new IllegalArgumentException("Invalid runtime decision record");
		}
		actionName = actionName == null ? "" : actionName;
		trace = List.copyOf(trace);
		failureDetail = failureDetail == null ? "" : failureDetail;
		if ((status == BotDecisionStatus.ERROR) != !failureDetail.isEmpty())
		{
			throw new IllegalArgumentException("Only failed runtime decisions carry failure detail");
		}
	}

	public static BotRuntimeDecision from(long tick, BotClassProfileKey profile, BotDecisionOutcome outcome)
	{
		if (outcome == null)
		{
			throw new IllegalArgumentException("Bot decision outcome cannot be null");
		}
		RuntimeException failure = outcome.failure();
		String failureDetail = "";
		if (failure != null)
		{
			String message = failure.getMessage();
			failureDetail = failure.getClass().getName() + ((message == null) || message.isBlank() ? "" : ": " + message);
		}
		return new BotRuntimeDecision(tick, profile, outcome.status(), outcome.actionName(), outcome.trace().entries(), outcome.trace().isTruncated(), outcome.pendingActions(), failureDetail);
	}
}
