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
package net.sf.l2j.gameserver.bot.perception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.sf.l2j.gameserver.bot.profile.BotRuleCondition;
import net.sf.l2j.gameserver.model.base.ClassId;

class BotPerceptionSnapshotTest
{
	@Test
	void copiesAndSortsEveryDecisionInput()
	{
		Set<Integer> effects = new HashSet<>(Set.of(1204));
		BotObservedCharacter self = character(10, 0, 80, 40, effects);
		BotObservedCharacter ally = character(30, 500, 25, 90, Set.of());
		BotObservedCharacter hostile = character(20, 300, 100, 100, Set.of());
		List<BotObservedCharacter> characters = new ArrayList<>(List.of(ally, self, hostile));
		List<Integer> party = new ArrayList<>(List.of(30, 10, 30));

		BotPerceptionSnapshot snapshot = new BotPerceptionSnapshot(7, 10, 20, 30, 0, 30, characters, party, List.of(30, 10), List.of(20));
		characters.clear();
		party.clear();
		effects.clear();

		assertEquals(List.of(10, 30), snapshot.group(BotRuleCondition.Group.PARTY));
		assertEquals(List.of(10, 30), snapshot.group(BotRuleCondition.Group.CLAN));
		assertEquals(List.of(20), snapshot.group(BotRuleCondition.Group.NEARBY_HOSTILES));
		assertEquals(List.of(10, 20, 30), snapshot.characters().stream().map(BotObservedCharacter::objectId).toList());
		assertTrue(snapshot.self().hasEffect(1204));
		assertEquals(80.0, snapshot.self().resource(BotRuleCondition.ResourceType.HP, BotRuleCondition.AmountType.PERCENT));
		assertEquals(60.0, snapshot.self().resource(BotRuleCondition.ResourceType.MP, BotRuleCondition.AmountType.MISSING));
		assertThrows(UnsupportedOperationException.class, () -> snapshot.group(BotRuleCondition.Group.PARTY).add(99));
	}

	@Test
	void resolvesSubjectsWithoutLiveWorldReferences()
	{
		BotPerceptionSnapshot snapshot = new BotPerceptionSnapshot(1, 10, 20, 30, 0, 10, List.of(character(10, 0, 100, 100, Set.of()), character(20, 100, 100, 100, Set.of()), character(30, 200, 100, 100, Set.of())), List.of(10), List.of(), List.of(20));

		assertEquals(10, snapshot.subject(BotRuleCondition.Subject.SELF, 20).orElseThrow().objectId());
		assertEquals(20, snapshot.subject(BotRuleCondition.Subject.TARGET, 20).orElseThrow().objectId());
		assertEquals(30, snapshot.subject(BotRuleCondition.Subject.OWNER, 20).orElseThrow().objectId());
		assertFalse(snapshot.subject(BotRuleCondition.Subject.SUMMON, 20).isPresent());
	}

	@Test
	void rejectsDanglingOrDuplicateObjectIds()
	{
		BotObservedCharacter self = character(10, 0, 100, 100, Set.of());

		assertThrows(IllegalArgumentException.class, () -> new BotPerceptionSnapshot(1, 10, 99, 0, 0, 10, List.of(self), List.of(10), List.of(), List.of()));
		assertThrows(IllegalArgumentException.class, () -> new BotPerceptionSnapshot(1, 10, 0, 0, 0, 10, List.of(self, self), List.of(10), List.of(), List.of()));
	}

	private static BotObservedCharacter character(int objectId, int distance, double hp, double mp, Set<Integer> effects)
	{
		return new BotObservedCharacter(objectId, BotObservedCharacter.Kind.PLAYER, ClassId.duelist, 80, 0, 0, 0, distance, true, hp, 100, mp, 100, 0, 0, false, false, false, false, false, false, false, false, false, effects, 0);
	}
}
