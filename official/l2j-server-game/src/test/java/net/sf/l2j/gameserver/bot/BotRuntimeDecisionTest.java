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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.sf.l2j.gameserver.bot.profile.BotClassProfileKey;
import net.sf.l2j.gameserver.bot.strategy.BotDecisionStatus;
import net.sf.l2j.gameserver.bot.strategy.BotDecisionStep;
import net.sf.l2j.gameserver.bot.strategy.BotDecisionTraceEntry;
import net.sf.l2j.gameserver.model.base.ClassId;

class BotRuntimeDecisionTest
{
	@Test
	void copiesBoundedTraceDataWithoutRetainingAThrowable()
	{
		List<BotDecisionTraceEntry> trace = new ArrayList<>();
		trace.add(new BotDecisionTraceEntry(0, 7, "skill:duelist-might", 900, BotDecisionStep.EXECUTED, "hierophant.party", "target=40"));

		BotRuntimeDecision decision = new BotRuntimeDecision(7, new BotClassProfileKey(ClassId.hierophant, "party"), BotDecisionStatus.ERROR, "", trace, false, 1, "java.lang.IllegalStateException: rejected");
		trace.clear();

		assertEquals(1, decision.trace().size());
		assertEquals(ClassId.hierophant, decision.profile().classId());
		assertEquals("java.lang.IllegalStateException: rejected", decision.failureDetail());
		assertThrows(UnsupportedOperationException.class, () -> decision.trace().clear());
	}

	@Test
	void rejectsFailureDetailsOnSuccessfulOutcomes()
	{
		BotClassProfileKey profile = new BotClassProfileKey(ClassId.duelist, "field");

		assertThrows(IllegalArgumentException.class, () -> new BotRuntimeDecision(1, profile, BotDecisionStatus.IDLE, "", List.of(), false, 0, "unexpected"));
		assertThrows(IllegalArgumentException.class, () -> new BotRuntimeDecision(1, profile, BotDecisionStatus.ERROR, "", List.of(), false, 0, ""));
	}
}
