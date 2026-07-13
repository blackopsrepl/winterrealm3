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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.base.ClassId;

/** Validates profile skill references against the loaded Interlude data. */
public final class BotClassProfileValidator
{
	private final BiPredicate<ClassId, Integer> _classSkill;
	private final IntPredicate _knownSkill;

	public BotClassProfileValidator()
	{
		this(BotClassProfileValidator::isLoadedClassSkill, BotClassProfileValidator::isLoadedSkill);
	}

	BotClassProfileValidator(BiPredicate<ClassId, Integer> classSkill, IntPredicate knownSkill)
	{
		if ((classSkill == null) || (knownSkill == null))
		{
			throw new IllegalArgumentException("Bot profile validation catalogs cannot be null");
		}
		_classSkill = classSkill;
		_knownSkill = knownSkill;
	}

	/** Validates every loaded profile and reports all invalid references. */
	public void validateAll(Collection<BotClassProfile> profiles)
	{
		if (profiles == null)
		{
			throw new IllegalArgumentException("Bot profiles to validate cannot be null");
		}
		List<String> violations = new ArrayList<>();
		for (BotClassProfile profile : profiles)
		{
			if (profile == null)
			{
				violations.add("profile collection contains null");
				continue;
			}
			validate(profile, violations);
		}
		if (!violations.isEmpty())
		{
			throw new IllegalArgumentException("Invalid exact-class bot profile data: " + String.join("; ", violations));
		}
	}

	private void validate(BotClassProfile profile, List<String> violations)
	{
		for (BotSkillPolicy policy : profile.getSkillPolicies())
		{
			if (!_classSkill.test(profile.getClassId(), policy.skillId()))
			{
				violations.add(profile.getKey() + " policy " + policy.key() + " references skill " + policy.skillId() + " outside the exact class skill tree");
			}
			for (BotRuleCondition condition : policy.conditions())
			{
				if ((condition instanceof BotRuleCondition.Effect effect) && !_knownSkill.test(effect.skillId()))
				{
					violations.add(profile.getKey() + " policy " + policy.key() + " references unknown effect skill " + effect.skillId());
				}
			}
		}
	}

	private static boolean isLoadedClassSkill(ClassId classId, int skillId)
	{
		Collection<L2SkillLearn> skills = SkillTreeTable.getInstance().getAllowedSkills(classId);
		if (skills == null)
		{
			return false;
		}
		for (L2SkillLearn skill : skills)
		{
			if ((skill.getId() == skillId) && (SkillTable.getInstance().getInfo(skillId, skill.getLevel()) != null))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean isLoadedSkill(int skillId)
	{
		return SkillTable.getInstance().getMaxLevel(skillId, 0) > 0;
	}
}
