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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.base.ClassId;

/** Validates profile skill references against the loaded Interlude data. */
public final class BotClassProfileValidator
{
	private final BiPredicate<ClassId, Integer> _classSkill;
	private final IntPredicate _knownSkill;
	private final IntPredicate _partyScopedSupportSkill;

	public BotClassProfileValidator()
	{
		this(BotClassProfileValidator::isLoadedClassSkill, BotClassProfileValidator::isLoadedSkill, BotClassProfileValidator::isPartyScopedSupportSkill);
	}

	BotClassProfileValidator(BiPredicate<ClassId, Integer> classSkill, IntPredicate knownSkill)
	{
		this(classSkill, knownSkill, skillId -> true);
	}

	BotClassProfileValidator(BiPredicate<ClassId, Integer> classSkill, IntPredicate knownSkill, IntPredicate partyScopedSupportSkill)
	{
		if ((classSkill == null) || (knownSkill == null) || (partyScopedSupportSkill == null))
		{
			throw new IllegalArgumentException("Bot profile validation catalogs cannot be null");
		}
		_classSkill = classSkill;
		_knownSkill = knownSkill;
		_partyScopedSupportSkill = partyScopedSupportSkill;
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
			if (policy.intent() == BotSkillPolicy.Intent.CLAN_BUFF)
			{
				violations.add(profile.getKey() + " policy " + policy.key() + " uses CLAN_BUFF outside the self, owned summon, and current-party support boundary");
			}
			else if (isSupportIntent(policy.intent()) && !isOwnedOrPartySupportTarget(policy.target()))
			{
				violations.add(profile.getKey() + " policy " + policy.key() + " targets " + policy.target() + " for " + policy.intent() + " outside the self, owned summon, and current-party support boundary");
			}
			if (isSupportIntent(policy.intent()) && !_partyScopedSupportSkill.test(policy.skillId()))
			{
				violations.add(profile.getKey() + " policy " + policy.key() + " uses support skill " + policy.skillId() + " whose native target can expand beyond the current party");
			}
			int positiveTargetClasses = 0;
			for (BotRuleCondition condition : policy.conditions())
			{
				if ((condition instanceof BotRuleCondition.Effect effect) && !_knownSkill.test(effect.skillId()))
				{
					violations.add(profile.getKey() + " policy " + policy.key() + " references unknown effect skill " + effect.skillId());
				}
				else if (condition instanceof BotRuleCondition.GroupEffectCount count)
				{
					for (int skillId : count.effectSkillIds())
					{
						if (!_knownSkill.test(skillId))
						{
							violations.add(profile.getKey() + " policy " + policy.key() + " references unknown group effect skill " + skillId);
						}
					}
				}
				else if (condition instanceof BotRuleCondition.GroupClassCount count)
				{
					for (int skillId : count.effectSkillIds())
					{
						if (!_knownSkill.test(skillId))
						{
							violations.add(profile.getKey() + " policy " + policy.key() + " references unknown group effect skill " + skillId);
						}
					}
				}
				else if (condition instanceof BotRuleCondition.GroupClassSetCount count)
				{
					for (int skillId : count.effectSkillIds())
					{
						if (!_knownSkill.test(skillId))
						{
							violations.add(profile.getKey() + " policy " + policy.key() + " references unknown group effect skill " + skillId);
						}
					}
				}
				if ((condition instanceof BotRuleCondition.ExactClass exactClass) && (exactClass.subject() == BotRuleCondition.Subject.TARGET) && exactClass.expected())
				{
					positiveTargetClasses++;
				}
			}
			if (requiresExactRecipient(policy) && (positiveTargetClasses != 1))
			{
				violations.add(profile.getKey() + " policy " + policy.key() + " requires exactly one positive TARGET ClassId condition for " + policy.intent());
			}
		}
	}

	private static boolean requiresExactRecipient(BotSkillPolicy policy)
	{
		boolean directSupport = switch (policy.intent())
		{
			case HEAL, CP_HEAL, RECHARGE, CURE, RESURRECT, BUFF -> true;
			default -> false;
		};
		return directSupport && (policy.target() != BotSkillPolicy.Target.SELF) && (policy.target() != BotSkillPolicy.Target.SUMMON);
	}

	private static boolean isSupportIntent(BotSkillPolicy.Intent intent)
	{
		return switch (intent)
		{
			case HEAL, GROUP_HEAL, CP_HEAL, RECHARGE, CURE, RESURRECT, BUFF, GROUP_BUFF, SONG, DANCE, CHANT, SERVITOR_SUPPORT -> true;
			default -> false;
		};
	}

	private static boolean isOwnedOrPartySupportTarget(BotSkillPolicy.Target target)
	{
		return switch (target)
		{
			case SELF, SUMMON, PARTY_LEADER, PARTY_MEMBER, PARTY_LOWEST_HP, PARTY_LOWEST_MP, PARTY_LOWEST_CP, PARTY_DEAD_NEAREST, PARTY_DEAD_HIGHEST_LEVEL -> true;
			default -> false;
		};
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

	private static boolean isPartyScopedSupportSkill(int skillId)
	{
		int maximumLevel = SkillTable.getInstance().getMaxLevel(skillId, 0);
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, maximumLevel);
		if (skill == null)
		{
			return false;
		}
		return switch (skill.getTargetType())
		{
			case TARGET_SELF, TARGET_ONE, TARGET_PARTY, TARGET_PET, TARGET_CORPSE_PLAYER, TARGET_CORPSE_PET, TARGET_PARTY_MEMBER, TARGET_PARTY_OTHER, TARGET_OWNER_PET -> true;
			default -> false;
		};
	}
}
