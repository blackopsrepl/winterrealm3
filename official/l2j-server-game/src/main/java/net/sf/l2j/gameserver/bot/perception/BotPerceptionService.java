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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.l2j.gameserver.bot.BotRuntime;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/** Copies fair, bounded world state into an immutable decision snapshot. */
public final class BotPerceptionService
{
	public static final int MINIMUM_PERCEPTION_RANGE = 1200;
	public static final int MAXIMUM_PERCEPTION_RANGE = 5000;

	public BotPerceptionSnapshot capture(BotRuntime runtime, long tick)
	{
		if (runtime == null)
		{
			throw new IllegalArgumentException("Bot runtime cannot be null");
		}
		if (tick <= 0)
		{
			throw new IllegalArgumentException("Bot perception tick must be positive");
		}

		L2PcInstance player = runtime.getPlayer();
		int range = perceptionRange(runtime);
		Map<Integer, L2Character> observed = new LinkedHashMap<>();
		add(observed, player);
		for (L2Object object : player.getKnownList().getKnownObjects().values())
		{
			if ((object instanceof L2Character character) && player.isInsideRadius(character, range, true, false))
			{
				add(observed, character);
			}
		}

		List<Integer> partyIds = new ArrayList<>();
		int partyLeaderId = player.getObjectId();
		if (player.getParty() == null)
		{
			partyIds.add(player.getObjectId());
		}
		else
		{
			partyLeaderId = player.getParty().getLeader().getObjectId();
			for (L2PcInstance member : List.copyOf(player.getParty().getPartyMembers()))
			{
				if (member != null)
				{
					add(observed, member);
					partyIds.add(member.getObjectId());
				}
			}
		}

		L2Summon summon = player.getPet();
		int summonId = 0;
		if (summon != null)
		{
			add(observed, summon);
			summonId = summon.getObjectId();
		}

		List<Integer> clanIds = new ArrayList<>();
		if (player.getClan() != null)
		{
			for (L2Character character : observed.values())
			{
				if ((character instanceof L2PcInstance member) && (member.getClanId() == player.getClanId()))
				{
					clanIds.add(member.getObjectId());
				}
			}
		}

		List<Integer> hostileIds = new ArrayList<>();
		for (L2Character character : observed.values())
		{
			if ((character != player) && character.isAutoAttackable(player))
			{
				hostileIds.add(character.getObjectId());
			}
		}

		int ownerId = 0;
		Integer configuredOwnerId = runtime.getDefinition().ownerObjectId();
		if ((configuredOwnerId != null) && observed.containsKey(configuredOwnerId))
		{
			ownerId = configuredOwnerId;
		}

		int currentTargetId = 0;
		if ((player.getTarget() instanceof L2Character target) && observed.containsKey(target.getObjectId()))
		{
			currentTargetId = target.getObjectId();
		}

		Collection<BotObservedCharacter> snapshots = observed.values().stream().map(character -> observe(player, character)).toList();
		return new BotPerceptionSnapshot(tick, player.getObjectId(), currentTargetId, ownerId, summonId, partyLeaderId, snapshots, partyIds, clanIds, hostileIds);
	}

	private static int perceptionRange(BotRuntime runtime)
	{
		int range = Math.max(MINIMUM_PERCEPTION_RANGE, runtime.getClassProfile().getCombatPolicy().acquisitionRange());
		for (var skill : runtime.getClassProfile().getSkillPolicies())
		{
			for (BotRuleCondition condition : skill.conditions())
			{
				if (condition instanceof BotRuleCondition.Distance distance)
				{
					range = Math.max(range, distance.value());
				}
				else if (condition instanceof BotRuleCondition.GroupCount count)
				{
					range = Math.max(range, count.radius());
				}
			}
		}
		return Math.min(range, MAXIMUM_PERCEPTION_RANGE);
	}

	private static void add(Map<Integer, L2Character> observed, L2Character character)
	{
		observed.put(character.getObjectId(), character);
	}

	private static BotObservedCharacter observe(L2PcInstance self, L2Character character)
	{
		Set<Integer> effects = new TreeSet<>();
		for (L2Effect effect : character.getAllEffects())
		{
			if ((effect != null) && (effect.getSkill() != null))
			{
				effects.add(effect.getSkill().getId());
			}
		}
		int targetObjectId = character.getTarget() != null ? character.getTarget().getObjectId() : 0;
		return new BotObservedCharacter(character.getObjectId(), kind(character), character.getLevel(), character.getX(), character.getY(), character.getZ(), distance(self, character), !character.isAlikeDead(), character.getCurrentHp(), character.getMaxHp(), character.getCurrentMp(), character.getMaxMp(), character.getCurrentCp(), character.getMaxCp(), character.isInCombat(), character.isAttackingNow(), character.isCastingNow(), character.isMoving(), character.isRooted(), character.isStunned(), character.isMuted(), character.isPsychicalMuted(), effects, targetObjectId);
	}

	private static BotObservedCharacter.Kind kind(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			return BotObservedCharacter.Kind.PLAYER;
		}
		if (character instanceof L2Summon)
		{
			return BotObservedCharacter.Kind.SUMMON;
		}
		if (character instanceof L2Attackable)
		{
			return BotObservedCharacter.Kind.ATTACKABLE;
		}
		return BotObservedCharacter.Kind.NPC;
	}

	private static int distance(L2Character left, L2Character right)
	{
		long dx = (long) left.getX() - right.getX();
		long dy = (long) left.getY() - right.getY();
		long dz = (long) left.getZ() - right.getZ();
		return (int) Math.min(Integer.MAX_VALUE, Math.round(Math.sqrt((dx * dx) + (dy * dy) + (dz * dz))));
	}
}
