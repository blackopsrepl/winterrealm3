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
package net.sf.l2j.gameserver.bot.profile;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

import net.sf.l2j.gameserver.bot.perception.BotObservedCharacter;
import net.sf.l2j.gameserver.bot.perception.BotPerceptionSnapshot;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.AmountType;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.Group;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.ResourceType;
import net.sf.l2j.gameserver.bot.profile.BotSkillPolicy.Target;

/** Deterministic object-id target selection over one immutable snapshot. */
public final class BotTargetResolver
{
	private static final Comparator<BotObservedCharacter> NEAREST = Comparator.comparingInt(BotObservedCharacter::distance).thenComparingInt(BotObservedCharacter::objectId);
	private static final Comparator<BotObservedCharacter> HIGHEST_LEVEL = Comparator.comparingInt(BotObservedCharacter::level).reversed().thenComparing(NEAREST);

	public OptionalInt resolve(Target selector, BotPerceptionSnapshot snapshot)
	{
		List<Integer> candidates = candidates(selector, snapshot);
		return candidates.isEmpty() ? OptionalInt.empty() : OptionalInt.of(candidates.get(0));
	}

	/** Selects the first ordered candidate satisfying this entire skill rule. */
	public OptionalInt resolveEligible(BotSkillPolicy policy, BotPerceptionSnapshot snapshot, BotConditionEvaluator conditions)
	{
		if ((policy == null) || (conditions == null))
		{
			throw new IllegalArgumentException("Eligible target resolution requires policy and conditions");
		}
		for (int candidate : candidates(policy.target(), snapshot))
		{
			if (conditions.matches(policy.conditions(), snapshot, candidate))
			{
				return OptionalInt.of(candidate);
			}
		}
		return OptionalInt.empty();
	}

	public List<Integer> candidates(Target selector, BotPerceptionSnapshot snapshot)
	{
		if ((selector == null) || (snapshot == null))
		{
			throw new IllegalArgumentException("Target resolution requires a selector and snapshot");
		}
		return switch (selector)
		{
			case SELF -> List.of(snapshot.selfObjectId());
			case CURRENT_TARGET -> observed(snapshot, snapshot.currentTargetObjectId());
			case OWNER -> observed(snapshot, snapshot.ownerObjectId());
			case SUMMON -> observed(snapshot, snapshot.summonObjectId());
			case PARTY_LEADER -> observed(snapshot, snapshot.partyLeaderObjectId());
			case PARTY_MEMBER -> ordered(snapshot, snapshot.group(Group.PARTY), BotObservedCharacter::alive, NEAREST);
			case PARTY_LOWEST_HP -> lowestResource(snapshot, snapshot.group(Group.PARTY), ResourceType.HP);
			case PARTY_LOWEST_MP -> lowestResource(snapshot, snapshot.group(Group.PARTY), ResourceType.MP);
			case PARTY_LOWEST_CP -> lowestResource(snapshot, snapshot.group(Group.PARTY), ResourceType.CP);
			case PARTY_DEAD_NEAREST -> ordered(snapshot, snapshot.group(Group.PARTY), character -> !character.alive(), NEAREST);
			case PARTY_DEAD_HIGHEST_LEVEL -> ordered(snapshot, snapshot.group(Group.PARTY), character -> !character.alive(), HIGHEST_LEVEL);
			case CLAN_MEMBER -> ordered(snapshot, snapshot.group(Group.CLAN), BotObservedCharacter::alive, NEAREST);
			case CLAN_LOWEST_HP -> lowestResource(snapshot, snapshot.group(Group.CLAN), ResourceType.HP);
			case CLAN_LOWEST_MP -> lowestResource(snapshot, snapshot.group(Group.CLAN), ResourceType.MP);
			case CLAN_LOWEST_CP -> lowestResource(snapshot, snapshot.group(Group.CLAN), ResourceType.CP);
			case CLAN_DEAD_NEAREST -> ordered(snapshot, snapshot.group(Group.CLAN), character -> !character.alive(), NEAREST);
			case CLAN_DEAD_HIGHEST_LEVEL -> ordered(snapshot, snapshot.group(Group.CLAN), character -> !character.alive(), HIGHEST_LEVEL);
			case NEAREST_HOSTILE -> ordered(snapshot, snapshot.group(Group.NEARBY_HOSTILES), BotObservedCharacter::alive, NEAREST);
			case ASSISTED_HOSTILE -> assistedHostile(snapshot);
			case NEAREST_CORPSE -> ordered(snapshot, snapshot.group(Group.NEARBY_HOSTILES), character -> !character.alive(), NEAREST);
		};
	}

	private static List<Integer> lowestResource(BotPerceptionSnapshot snapshot, Collection<Integer> ids, ResourceType resource)
	{
		Comparator<BotObservedCharacter> comparator = Comparator.<BotObservedCharacter> comparingDouble(character -> character.resource(resource, AmountType.PERCENT)).thenComparing(NEAREST);
		return ordered(snapshot, ids, character -> character.alive() && maximum(character, resource) > 0, comparator);
	}

	private static double maximum(BotObservedCharacter character, ResourceType resource)
	{
		return switch (resource)
		{
			case HP -> character.maximumHp();
			case MP -> character.maximumMp();
			case CP -> character.maximumCp();
		};
	}

	private static List<Integer> assistedHostile(BotPerceptionSnapshot snapshot)
	{
		List<Integer> assistants = List.of(snapshot.ownerObjectId(), snapshot.partyLeaderObjectId(), snapshot.currentTargetObjectId());
		for (int assistantId : assistants)
		{
			Optional<BotObservedCharacter> assistant = snapshot.find(assistantId);
			if (assistant.isEmpty())
			{
				continue;
			}
			int targetId = assistant.orElseThrow().targetObjectId();
			if (snapshot.group(Group.NEARBY_HOSTILES).contains(targetId))
			{
				Optional<BotObservedCharacter> target = snapshot.find(targetId);
				if (target.isPresent() && target.orElseThrow().alive())
				{
					return List.of(targetId);
				}
			}
		}
		return List.of();
	}

	private static List<Integer> ordered(BotPerceptionSnapshot snapshot, Collection<Integer> ids, Predicate<BotObservedCharacter> filter, Comparator<BotObservedCharacter> comparator)
	{
		return ids.stream().map(snapshot::find).flatMap(Optional::stream).filter(filter).sorted(comparator).map(BotObservedCharacter::objectId).toList();
	}

	private static List<Integer> observed(BotPerceptionSnapshot snapshot, int objectId)
	{
		return (objectId != 0) && snapshot.find(objectId).isPresent() ? List.of(objectId) : List.of();
	}
}
