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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.sf.l2j.gameserver.model.base.ClassId;

class BotClassProfileValidatorTest
{
	@Test
	void acceptsOnlySkillsLearnableByTheDeclaredExactClass()
	{
		BotClassProfileValidator validator = new BotClassProfileValidator((classId, skillId) -> (classId == ClassId.cardinal) && (skillId == 1218), skillId -> (skillId == 1218) || (skillId == 1068));

		assertDoesNotThrow(() -> validator.validateAll(List.of(profile(ClassId.cardinal, 1218, 1068))));
		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> validator.validateAll(List.of(profile(ClassId.evaSaint, 1218, 1068))));

		assertTrue(failure.getMessage().contains("evaSaint"));
		assertTrue(failure.getMessage().contains("outside the exact class skill tree"));
	}

	@Test
	void rejectsUnknownEffectReferencesAndAggregatesDiagnostics()
	{
		BotClassProfileValidator validator = new BotClassProfileValidator((classId, skillId) -> false, skillId -> false);

		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> validator.validateAll(List.of(profile(ClassId.cardinal, 1218, 99999))));

		assertTrue(failure.getMessage().contains("skill 1218"));
		assertTrue(failure.getMessage().contains("unknown effect skill 99999"));
	}

	@Test
	void requiresOneExactRecipientClassForEveryDirectSupportIntent()
	{
		BotClassProfileValidator validator = new BotClassProfileValidator((classId, skillId) -> (classId == ClassId.hierophant) && (skillId == 1068), skillId -> skillId == 1068);
		BotRuleCondition.Effect missingEffect = new BotRuleCondition.Effect(BotRuleCondition.Subject.TARGET, 1068, BotRuleCondition.EffectState.ABSENT);
		for (BotSkillPolicy.Intent intent : List.of(BotSkillPolicy.Intent.HEAL, BotSkillPolicy.Intent.CP_HEAL, BotSkillPolicy.Intent.RECHARGE, BotSkillPolicy.Intent.CURE, BotSkillPolicy.Intent.RESURRECT, BotSkillPolicy.Intent.BUFF))
		{
			BotSkillPolicy generic = new BotSkillPolicy("generic-support", 1068, intent, BotSkillPolicy.Target.PARTY_MEMBER, 100, false, false, List.of(missingEffect));
			BotSkillPolicy exact = new BotSkillPolicy("duelist-support", 1068, intent, BotSkillPolicy.Target.PARTY_MEMBER, 100, false, false, List.of(new BotRuleCondition.ExactClass(BotRuleCondition.Subject.TARGET, ClassId.duelist, true), missingEffect));

			IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> validator.validateAll(List.of(profile(ClassId.hierophant, generic))));
			assertTrue(failure.getMessage().contains("exactly one positive TARGET ClassId"));
			assertTrue(failure.getMessage().contains(intent.name()));
			assertDoesNotThrow(() -> validator.validateAll(List.of(profile(ClassId.hierophant, exact))));
		}
	}

	@Test
	void rejectsAmbiguousMultipleExactRecipients()
	{
		BotClassProfileValidator validator = new BotClassProfileValidator((classId, skillId) -> true, skillId -> true);
		BotSkillPolicy ambiguous = new BotSkillPolicy("ambiguous-support", 1068, BotSkillPolicy.Intent.HEAL, BotSkillPolicy.Target.PARTY_LOWEST_HP, 100, false, false, List.of(new BotRuleCondition.ExactClass(BotRuleCondition.Subject.TARGET, ClassId.duelist, true), new BotRuleCondition.ExactClass(BotRuleCondition.Subject.TARGET, ClassId.cardinal, true)));

		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> validator.validateAll(List.of(profile(ClassId.hierophant, ambiguous))));
		assertTrue(failure.getMessage().contains("exactly one positive TARGET ClassId"));
	}

	@Test
	void limitsEverySupportIntentToSelfOwnedSummonOrCurrentParty()
	{
		BotClassProfileValidator validator = new BotClassProfileValidator((classId, skillId) -> true, skillId -> true);
		EnumSet<BotSkillPolicy.Target> allowedTargets = EnumSet.of(BotSkillPolicy.Target.SELF, BotSkillPolicy.Target.SUMMON, BotSkillPolicy.Target.PARTY_LEADER, BotSkillPolicy.Target.PARTY_MEMBER, BotSkillPolicy.Target.PARTY_LOWEST_HP, BotSkillPolicy.Target.PARTY_LOWEST_MP, BotSkillPolicy.Target.PARTY_LOWEST_CP, BotSkillPolicy.Target.PARTY_DEAD_NEAREST, BotSkillPolicy.Target.PARTY_DEAD_HIGHEST_LEVEL);
		for (BotSkillPolicy.Target target : allowedTargets)
		{
			assertDoesNotThrow(() -> validator.validateAll(List.of(profile(ClassId.hierophant, support(BotSkillPolicy.Intent.BUFF, target)))));
		}
		for (BotSkillPolicy.Target target : BotSkillPolicy.Target.values())
		{
			if (allowedTargets.contains(target))
			{
				continue;
			}
			IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> validator.validateAll(List.of(profile(ClassId.hierophant, support(BotSkillPolicy.Intent.BUFF, target)))));
			assertTrue(failure.getMessage().contains("current-party support boundary"));
		}

		for (BotSkillPolicy.Intent intent : List.of(BotSkillPolicy.Intent.HEAL, BotSkillPolicy.Intent.GROUP_HEAL, BotSkillPolicy.Intent.CP_HEAL, BotSkillPolicy.Intent.RECHARGE, BotSkillPolicy.Intent.CURE, BotSkillPolicy.Intent.RESURRECT, BotSkillPolicy.Intent.BUFF, BotSkillPolicy.Intent.GROUP_BUFF, BotSkillPolicy.Intent.SONG, BotSkillPolicy.Intent.DANCE, BotSkillPolicy.Intent.CHANT, BotSkillPolicy.Intent.SERVITOR_SUPPORT))
		{
			IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> validator.validateAll(List.of(profile(ClassId.hierophant, support(intent, BotSkillPolicy.Target.CURRENT_TARGET)))));
			assertTrue(failure.getMessage().contains("current-party support boundary"));
		}
	}

	@Test
	void rejectsClanBuffEvenWhenTheSkillIsSelfCast()
	{
		BotClassProfileValidator validator = new BotClassProfileValidator((classId, skillId) -> true, skillId -> true);

		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> validator.validateAll(List.of(profile(ClassId.hierophant, support(BotSkillPolicy.Intent.CLAN_BUFF, BotSkillPolicy.Target.SELF)))));

		assertTrue(failure.getMessage().contains("CLAN_BUFF"));
		assertTrue(failure.getMessage().contains("current-party support boundary"));
	}

	@Test
	void rejectsNativeSupportSkillsThatExpandBeyondTheParty()
	{
		BotClassProfileValidator validator = new BotClassProfileValidator((classId, skillId) -> true, skillId -> true, skillId -> false);

		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> validator.validateAll(List.of(profile(ClassId.hierophant, support(BotSkillPolicy.Intent.GROUP_BUFF, BotSkillPolicy.Target.SELF)))));

		assertTrue(failure.getMessage().contains("native target can expand beyond the current party"));
	}

	private static BotClassProfile profile(ClassId classId, int skillId, int effectId)
	{
		BotRuleCondition.Effect effect = new BotRuleCondition.Effect(BotRuleCondition.Subject.TARGET, effectId, BotRuleCondition.EffectState.ABSENT);
		BotRuleCondition.ExactClass recipient = new BotRuleCondition.ExactClass(BotRuleCondition.Subject.TARGET, ClassId.duelist, true);
		BotSkillPolicy skill = new BotSkillPolicy("test-skill", skillId, BotSkillPolicy.Intent.HEAL, BotSkillPolicy.Target.PARTY_LOWEST_HP, 100, false, false, List.of(recipient, effect));
		return new BotClassProfile(BotClassProfile.SCHEMA_VERSION, classId, "party", new BotCombatPolicy(BotCombatPolicy.Targeting.NONE, 0, 0, 0, 0), List.of(skill));
	}

	private static BotClassProfile profile(ClassId classId, BotSkillPolicy skill)
	{
		return new BotClassProfile(BotClassProfile.SCHEMA_VERSION, classId, "party", new BotCombatPolicy(BotCombatPolicy.Targeting.NONE, 0, 0, 0, 0), List.of(skill));
	}

	private static BotSkillPolicy support(BotSkillPolicy.Intent intent, BotSkillPolicy.Target target)
	{
		BotRuleCondition.Effect effect = new BotRuleCondition.Effect(BotRuleCondition.Subject.TARGET, 1068, BotRuleCondition.EffectState.ABSENT);
		if ((target == BotSkillPolicy.Target.SELF) || (target == BotSkillPolicy.Target.SUMMON))
		{
			return new BotSkillPolicy("support", 1068, intent, target, 100, false, false, List.of(effect));
		}
		BotRuleCondition.ExactClass recipient = new BotRuleCondition.ExactClass(BotRuleCondition.Subject.TARGET, ClassId.duelist, true);
		return new BotSkillPolicy("support", 1068, intent, target, 100, false, false, List.of(recipient, effect));
	}
}
