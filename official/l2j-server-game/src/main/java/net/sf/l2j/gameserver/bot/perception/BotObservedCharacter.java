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

import java.util.Set;

import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.AmountType;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.ResourceType;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.StatusType;

/** Immutable, live-reference-free observation of one visible character. */
public record BotObservedCharacter(int objectId, Kind kind, int level, int x, int y, int z, int distance, boolean alive, double currentHp, double maximumHp, double currentMp, double maximumMp, double currentCp, double maximumCp, boolean inCombat, boolean attacking, boolean casting, boolean moving, boolean rooted, boolean stunned, boolean silenced, boolean muted, Set<Integer> effectSkillIds, int targetObjectId)
{
	public BotObservedCharacter
	{
		if (objectId <= 0)
		{
			throw new IllegalArgumentException("Observed character id must be positive");
		}
		if (kind == null)
		{
			throw new IllegalArgumentException("Observed character kind cannot be null");
		}
		if ((level < 0) || (distance < 0))
		{
			throw new IllegalArgumentException("Observed level and distance cannot be negative");
		}
		requireResource(currentHp, maximumHp, "HP");
		requireResource(currentMp, maximumMp, "MP");
		requireResource(currentCp, maximumCp, "CP");
		if (effectSkillIds == null)
		{
			throw new IllegalArgumentException("Observed effects cannot be null");
		}
		if (effectSkillIds.stream().anyMatch(skillId -> skillId == null || skillId <= 0))
		{
			throw new IllegalArgumentException("Observed effect skill ids must be positive");
		}
		effectSkillIds = Set.copyOf(effectSkillIds);
		if (targetObjectId < 0)
		{
			throw new IllegalArgumentException("Observed target id cannot be negative");
		}
	}

	public double resource(ResourceType resource, AmountType amount)
	{
		if ((resource == null) || (amount == null))
		{
			throw new IllegalArgumentException("Observed resource query cannot contain null");
		}
		double current;
		double maximum;
		switch (resource)
		{
			case HP ->
			{
				current = currentHp;
				maximum = maximumHp;
			}
			case MP ->
			{
				current = currentMp;
				maximum = maximumMp;
			}
			case CP ->
			{
				current = currentCp;
				maximum = maximumCp;
			}
			default -> throw new IllegalStateException("Unhandled observed resource " + resource);
		}
		return switch (amount)
		{
			case CURRENT -> current;
			case MISSING -> Math.max(0, maximum - current);
			case PERCENT -> maximum > 0 ? Math.max(0, Math.min(100, (current * 100.0) / maximum)) : 0;
		};
	}

	public boolean status(StatusType status)
	{
		if (status == null)
		{
			throw new IllegalArgumentException("Observed status query cannot be null");
		}
		return switch (status)
		{
			case ALIVE -> alive;
			case IN_COMBAT -> inCombat;
			case ATTACKING -> attacking;
			case CASTING -> casting;
			case MOVING -> moving;
			case ROOTED -> rooted;
			case STUNNED -> stunned;
			case SILENCED -> silenced;
			case MUTED -> muted;
		};
	}

	public boolean hasEffect(int skillId)
	{
		return effectSkillIds.contains(skillId);
	}

	private static void requireResource(double current, double maximum, String name)
	{
		if (!Double.isFinite(current) || !Double.isFinite(maximum) || (current < 0) || (maximum < 0))
		{
			throw new IllegalArgumentException("Observed " + name + " values must be finite and non-negative");
		}
	}

	public enum Kind
	{
		PLAYER,
		SUMMON,
		ATTACKABLE,
		NPC
	}
}
