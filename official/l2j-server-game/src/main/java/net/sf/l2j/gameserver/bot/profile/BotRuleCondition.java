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
package net.sf.l2j.gameserver.bot.profile;

import java.util.HashSet;
import java.util.List;

import net.sf.l2j.gameserver.model.base.ClassId;

/** Typed, immutable condition vocabulary shared by exact-class rules. */
public sealed interface BotRuleCondition permits BotRuleCondition.Resource, BotRuleCondition.Distance, BotRuleCondition.Effect, BotRuleCondition.Status, BotRuleCondition.ExactClass, BotRuleCondition.GroupCount, BotRuleCondition.GroupClassCount
{
	int MAXIMUM_RANGE = 20000;

	enum Subject
	{
		SELF,
		TARGET,
		OWNER,
		SUMMON
	}

	enum Comparison
	{
		BELOW,
		AT_MOST,
		EQUAL,
		AT_LEAST,
		ABOVE;

		public boolean test(double actual, double expected)
		{
			return switch (this)
			{
				case BELOW -> actual < expected;
				case AT_MOST -> actual <= expected;
				case EQUAL -> Double.compare(actual, expected) == 0;
				case AT_LEAST -> actual >= expected;
				case ABOVE -> actual > expected;
			};
		}
	}

	enum ResourceType
	{
		HP,
		MP,
		CP
	}

	enum AmountType
	{
		PERCENT,
		CURRENT,
		MISSING
	}

	enum EffectState
	{
		PRESENT,
		ABSENT
	}

	enum StatusType
	{
		ALIVE,
		IN_COMBAT,
		ATTACKING,
		CASTING,
		MOVING,
		ROOTED,
		STUNNED,
		SILENCED,
		MUTED
	}

	enum Group
	{
		PARTY,
		CLAN,
		NEARBY_HOSTILES
	}

	enum GroupFilter
	{
		ALL(false),
		ALIVE(false),
		DEAD(false),
		HP_PERCENT_BELOW(true),
		MP_PERCENT_BELOW(true),
		CP_PERCENT_BELOW(true);

		private final boolean _requiresThreshold;

		GroupFilter(boolean requiresThreshold)
		{
			_requiresThreshold = requiresThreshold;
		}

		public boolean requiresThreshold()
		{
			return _requiresThreshold;
		}
	}

	record Resource(Subject subject, ResourceType resource, AmountType amount, Comparison comparison, int value) implements BotRuleCondition
	{
		public Resource
		{
			requireSubject(subject);
			if ((resource == null) || (amount == null) || (comparison == null))
			{
				throw new IllegalArgumentException("Resource conditions require resource, amount, and comparison values");
			}
			if (value < 0)
			{
				throw new IllegalArgumentException("Resource condition values cannot be negative");
			}
			if ((amount == AmountType.PERCENT) && (value > 100))
			{
				throw new IllegalArgumentException("Percentage resource conditions cannot exceed 100");
			}
		}
	}

	record Distance(Subject subject, Comparison comparison, int value) implements BotRuleCondition
	{
		public Distance
		{
			requireSubject(subject);
			if (subject == Subject.SELF)
			{
				throw new IllegalArgumentException("Distance conditions cannot measure the actor against itself");
			}
			if (comparison == null)
			{
				throw new IllegalArgumentException("Distance conditions require a comparison");
			}
			if ((value < 0) || (value > MAXIMUM_RANGE))
			{
				throw new IllegalArgumentException("Distance condition values must be between 0 and " + MAXIMUM_RANGE);
			}
		}
	}

	record Effect(Subject subject, int skillId, EffectState state) implements BotRuleCondition
	{
		public Effect
		{
			requireSubject(subject);
			if (skillId <= 0)
			{
				throw new IllegalArgumentException("Effect skill id must be positive");
			}
			if (state == null)
			{
				throw new IllegalArgumentException("Effect conditions require a state");
			}
		}
	}

	record Status(Subject subject, StatusType status, boolean expected) implements BotRuleCondition
	{
		public Status
		{
			requireSubject(subject);
			if (status == null)
			{
				throw new IllegalArgumentException("Status conditions require a status");
			}
		}
	}

	record ExactClass(Subject subject, ClassId classId, boolean expected) implements BotRuleCondition
	{
		public ExactClass
		{
			requireSubject(subject);
			requirePlayableClass(classId);
		}
	}

	record GroupCount(Group group, GroupFilter filter, Comparison comparison, int count, int threshold, int radius) implements BotRuleCondition
	{
		public static final int NO_THRESHOLD = -1;

		public GroupCount
		{
			if ((group == null) || (filter == null) || (comparison == null))
			{
				throw new IllegalArgumentException("Group-count conditions require group, filter, and comparison values");
			}
			if (count < 0)
			{
				throw new IllegalArgumentException("Group-count values cannot be negative");
			}
			if ((radius <= 0) || (radius > MAXIMUM_RANGE))
			{
				throw new IllegalArgumentException("Group-count radius must be between 1 and " + MAXIMUM_RANGE);
			}
			if (filter.requiresThreshold())
			{
				if ((threshold < 0) || (threshold > 100))
				{
					throw new IllegalArgumentException("Threshold group filters require a percentage between 0 and 100");
				}
			}
			else if (threshold != NO_THRESHOLD)
			{
				throw new IllegalArgumentException("Group filter " + filter + " does not accept a threshold");
			}
		}
	}

	/** Counts alive exact-class members with every named effect present or absent. */
	record GroupClassCount(Group group, ClassId classId, List<Integer> effectSkillIds, EffectState effectState, Comparison comparison, int count, int radius) implements BotRuleCondition
	{
		public GroupClassCount
		{
			if ((group == null) || (effectSkillIds == null) || (effectState == null) || (comparison == null))
			{
				throw new IllegalArgumentException("Group-class conditions require group, effect skills, effect state, and comparison values");
			}
			requirePlayableClass(classId);
			if (effectSkillIds.isEmpty())
			{
				throw new IllegalArgumentException("Group-class conditions require at least one effect skill id");
			}
			if (effectSkillIds.stream().anyMatch(skillId -> (skillId == null) || (skillId <= 0)))
			{
				throw new IllegalArgumentException("Group-class effect skill ids must be positive");
			}
			if (new HashSet<>(effectSkillIds).size() != effectSkillIds.size())
			{
				throw new IllegalArgumentException("Group-class effect skill ids cannot repeat");
			}
			if (count < 0)
			{
				throw new IllegalArgumentException("Group-class count cannot be negative");
			}
			if ((radius <= 0) || (radius > MAXIMUM_RANGE))
			{
				throw new IllegalArgumentException("Group-class radius must be between 1 and " + MAXIMUM_RANGE);
			}
			effectSkillIds = List.copyOf(effectSkillIds);
		}
	}

	private static void requireSubject(Subject subject)
	{
		if (subject == null)
		{
			throw new IllegalArgumentException("Condition subject cannot be null");
		}
	}

	private static void requirePlayableClass(ClassId classId)
	{
		if ((classId == null) || (classId.getRace() == null))
		{
			throw new IllegalArgumentException("Target class conditions require an exact playable ClassId");
		}
	}
}
