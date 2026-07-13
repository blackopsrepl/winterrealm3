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
package net.sf.l2j.gameserver.bot.tactics;

import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalInt;

import net.sf.l2j.gameserver.bot.perception.BotObservedCharacter;
import net.sf.l2j.gameserver.bot.perception.BotPerceptionSnapshot;
import net.sf.l2j.gameserver.bot.profile.BotCombatPolicy;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.Group;

/** Resolves the combat target chosen explicitly by one exact-class policy. */
final class BotCombatTargetResolver
{
	OptionalInt resolve(BotCombatPolicy policy, BotPerceptionSnapshot snapshot)
	{
		if ((policy == null) || (snapshot == null))
		{
			throw new IllegalArgumentException("Combat target resolution requires policy and perception");
		}
		return switch (policy.targeting())
		{
			case NONE -> OptionalInt.empty();
			case CURRENT_HOSTILE -> validHostile(snapshot, snapshot.currentTargetObjectId(), policy.maximumChaseDistance());
			case NEAREST_HOSTILE -> nearestHostile(snapshot, policy.acquisitionRange());
			case PARTY_ASSIST -> assistedHostile(snapshot, snapshot.partyLeaderObjectId(), policy.acquisitionRange());
			case OWNER_ASSIST -> assistedHostile(snapshot, snapshot.ownerObjectId(), policy.acquisitionRange());
		};
	}

	private static OptionalInt nearestHostile(BotPerceptionSnapshot snapshot, int range)
	{
		return snapshot.group(Group.NEARBY_HOSTILES).stream().map(snapshot::find).flatMap(Optional::stream).filter(BotObservedCharacter::alive).filter(character -> character.distance() <= range).min(Comparator.comparingInt(BotObservedCharacter::distance).thenComparingInt(BotObservedCharacter::objectId)).map(character -> OptionalInt.of(character.objectId())).orElseGet(OptionalInt::empty);
	}

	private static OptionalInt assistedHostile(BotPerceptionSnapshot snapshot, int assistantId, int range)
	{
		return snapshot.find(assistantId).map(BotObservedCharacter::targetObjectId).map(targetId -> validHostile(snapshot, targetId, range)).orElseGet(OptionalInt::empty);
	}

	private static OptionalInt validHostile(BotPerceptionSnapshot snapshot, int targetId, int range)
	{
		if (!snapshot.group(Group.NEARBY_HOSTILES).contains(targetId))
		{
			return OptionalInt.empty();
		}
		return snapshot.find(targetId).filter(BotObservedCharacter::alive).filter(character -> character.distance() <= range).map(character -> OptionalInt.of(character.objectId())).orElseGet(OptionalInt::empty);
	}
}
