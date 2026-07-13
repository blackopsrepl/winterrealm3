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

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One explicitly gated skill decision owned by an exact class profile.
 * Conditions use AND semantics; alternative gates are represented by separate
 * policies with distinct keys.
 */
public record BotSkillPolicy(String key, int skillId, Intent intent, Target target, int priority, boolean forceUse, boolean dontMove, List<BotRuleCondition> conditions)
{
	private static final Pattern KEY_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");

	public BotSkillPolicy
	{
		if (key == null)
		{
			throw new IllegalArgumentException("Skill policy key cannot be null");
		}
		key = key.trim().toLowerCase(Locale.ROOT);
		if (!KEY_PATTERN.matcher(key).matches())
		{
			throw new IllegalArgumentException("Skill policy key must match " + KEY_PATTERN.pattern());
		}
		if (skillId <= 0)
		{
			throw new IllegalArgumentException("Skill policy id must be positive");
		}
		if ((intent == null) || (target == null))
		{
			throw new IllegalArgumentException("Skill policies require intent and target values");
		}
		if ((priority <= 0) || (priority > BotCombatPolicy.MAXIMUM_PRIORITY))
		{
			throw new IllegalArgumentException("Skill priority must be between 1 and " + BotCombatPolicy.MAXIMUM_PRIORITY);
		}
		if (conditions == null)
		{
			throw new IllegalArgumentException("Skill policy conditions cannot be null");
		}
		if (conditions.isEmpty())
		{
			throw new IllegalArgumentException("Skill policies require at least one explicit condition");
		}
		if (conditions.stream().anyMatch(condition -> condition == null))
		{
			throw new IllegalArgumentException("Skill policy conditions cannot contain null");
		}
		conditions = List.copyOf(conditions);
	}

	public enum Intent
	{
		DAMAGE,
		AREA_DAMAGE,
		CONTROL,
		DEBUFF,
		HEAL,
		GROUP_HEAL,
		CP_HEAL,
		RECHARGE,
		CURE,
		RESURRECT,
		BUFF,
		GROUP_BUFF,
		CLAN_BUFF,
		SONG,
		DANCE,
		CHANT,
		SEAL,
		SUMMON,
		SERVITOR_SUPPORT,
		SPOIL,
		SWEEP,
		CRAFT,
		RESOURCE_BUILD,
		RESOURCE_SPEND,
		TOGGLE,
		ESCAPE,
		UTILITY
	}

	public enum Target
	{
		SELF,
		CURRENT_TARGET,
		OWNER,
		SUMMON,
		PARTY_LEADER,
		PARTY_MEMBER,
		PARTY_LOWEST_HP,
		PARTY_LOWEST_MP,
		PARTY_LOWEST_CP,
		PARTY_DEAD_NEAREST,
		PARTY_DEAD_HIGHEST_LEVEL,
		CLAN_MEMBER,
		CLAN_LOWEST_HP,
		CLAN_LOWEST_MP,
		CLAN_LOWEST_CP,
		CLAN_DEAD_NEAREST,
		CLAN_DEAD_HIGHEST_LEVEL,
		NEAREST_HOSTILE,
		ASSISTED_HOSTILE,
		NEAREST_CORPSE
	}
}
