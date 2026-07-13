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
	void requiresOneExactRecipientClassForSingleTargetBuffsAndRecharge()
	{
		BotClassProfileValidator validator = new BotClassProfileValidator((classId, skillId) -> (classId == ClassId.hierophant) && (skillId == 1068), skillId -> skillId == 1068);
		BotRuleCondition.Effect missingEffect = new BotRuleCondition.Effect(BotRuleCondition.Subject.TARGET, 1068, BotRuleCondition.EffectState.ABSENT);
		BotSkillPolicy generic = new BotSkillPolicy("generic-might", 1068, BotSkillPolicy.Intent.BUFF, BotSkillPolicy.Target.PARTY_MEMBER, 100, false, false, List.of(missingEffect));
		BotSkillPolicy duelist = new BotSkillPolicy("duelist-might", 1068, BotSkillPolicy.Intent.BUFF, BotSkillPolicy.Target.PARTY_MEMBER, 100, false, false, List.of(new BotRuleCondition.ExactClass(BotRuleCondition.Subject.TARGET, ClassId.duelist, true), missingEffect));

		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> validator.validateAll(List.of(profile(ClassId.hierophant, generic))));
		assertTrue(failure.getMessage().contains("exactly one positive TARGET ClassId"));
		assertDoesNotThrow(() -> validator.validateAll(List.of(profile(ClassId.hierophant, duelist))));
	}

	private static BotClassProfile profile(ClassId classId, int skillId, int effectId)
	{
		BotRuleCondition.Effect effect = new BotRuleCondition.Effect(BotRuleCondition.Subject.TARGET, effectId, BotRuleCondition.EffectState.ABSENT);
		BotSkillPolicy skill = new BotSkillPolicy("test-skill", skillId, BotSkillPolicy.Intent.HEAL, BotSkillPolicy.Target.PARTY_LOWEST_HP, 100, false, false, List.of(effect));
		return new BotClassProfile(BotClassProfile.SCHEMA_VERSION, classId, "party", new BotCombatPolicy(BotCombatPolicy.Targeting.NONE, 0, 0, 0, 0), List.of(skill));
	}

	private static BotClassProfile profile(ClassId classId, BotSkillPolicy skill)
	{
		return new BotClassProfile(BotClassProfile.SCHEMA_VERSION, classId, "party", new BotCombatPolicy(BotCombatPolicy.Targeting.NONE, 0, 0, 0, 0), List.of(skill));
	}
}
