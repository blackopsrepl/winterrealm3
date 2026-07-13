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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.GroupCount;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.GroupClassCount;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.Resource;
import net.sf.l2j.gameserver.model.base.ClassId;

class BotClassProfileLoaderTest
{
	@TempDir
	Path _directory;

	@Test
	void loadsTypedPolicyOwnedByOneExactClass() throws IOException
	{
		write("cardinal--party.xml", completeProfile("""
			<?xml version="1.0" encoding="UTF-8"?>
			<botClassProfile classId="cardinal" name="party" schemaVersion="1">
				<combat targeting="NONE" acquisitionRange="0" maximumChaseDistance="0" preferredRange="0" basicAttackPriority="0" />
				<skills>
					<skill key="urgent-major-heal" skillId="1218" intent="HEAL" target="PARTY_LOWEST_HP" priority="900" forceUse="false" dontMove="false">
						<when>
							<status subject="TARGET" status="ALIVE" expected="true" />
							<resource subject="TARGET" resource="HP" amount="PERCENT" comparison="BELOW" value="55" />
							<distance subject="TARGET" comparison="AT_MOST" value="1200" />
							<effect subject="TARGET" skillId="1218" state="ABSENT" />
							<class subject="TARGET" classId="duelist" expected="true" />
							<count group="PARTY" filter="HP_PERCENT_BELOW" comparison="AT_LEAST" value="2" threshold="70" radius="1200" />
							<classCount group="PARTY" classId="duelist" effectSkillIds="1218,1401" effectState="ABSENT" comparison="AT_LEAST" value="1" radius="1200" />
						</when>
					</skill>
				</skills>
			</botClassProfile>
			"""));

		BotClassProfileRegistry registry = new BotClassProfileLoader().loadDirectory(_directory);
		BotClassProfile profile = registry.require(ClassId.cardinal, "party");

		assertEquals(ClassId.cardinal, profile.getClassId());
		assertEquals(BotCombatPolicy.Targeting.NONE, profile.getCombatPolicy().targeting());
		assertEquals(89, profile.getClassRelationships().size());
		assertEquals(100, profile.requireClassRelationship(ClassId.duelist).hostileAttackPriority());
		assertEquals(1, profile.getSkillPolicies().size());
		BotSkillPolicy skill = profile.getSkillPolicies().get(0);
		assertEquals(BotSkillPolicy.Intent.HEAL, skill.intent());
		assertEquals(BotSkillPolicy.Target.PARTY_LOWEST_HP, skill.target());
		assertEquals(7, skill.conditions().size());
		assertInstanceOf(Resource.class, skill.conditions().get(1));
		GroupCount count = assertInstanceOf(GroupCount.class, skill.conditions().get(5));
		assertEquals(70, count.threshold());
		GroupClassCount classCount = assertInstanceOf(GroupClassCount.class, skill.conditions().get(6));
		assertEquals(ClassId.duelist, classCount.classId());
		assertEquals(List.of(1218, 1401), classCount.effectSkillIds());
	}

	@Test
	void rejectsFilenameThatDoesNotRepeatTheExactKey() throws IOException
	{
		Path file = write("healer.xml", emptyProfile("cardinal", "party"));

		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> new BotClassProfileLoader().load(file));

		assertTrue(failure.getMessage().contains("cardinal--party.xml"));
	}

	@Test
	void rejectsUnknownSchemaSurfaceAndUngatedSkills() throws IOException
	{
		Path unknown = write("cardinal--party.xml", emptyProfile("cardinal", "party").replace("<skills>", "<skills genericRole=\"healer\">"));
		assertThrows(IllegalArgumentException.class, () -> new BotClassProfileLoader().load(unknown));

		Path ungated = write("evaSaint--party.xml", completeProfile("""
			<botClassProfile classId="evaSaint" name="party" schemaVersion="1">
				<combat targeting="NONE" acquisitionRange="0" maximumChaseDistance="0" preferredRange="0" basicAttackPriority="0" />
				<skills>
					<skill key="recharge" skillId="1013" intent="RECHARGE" target="PARTY_LOWEST_MP" priority="600" forceUse="false" dontMove="false"><when /></skill>
				</skills>
			</botClassProfile>
			"""));
		assertThrows(IllegalArgumentException.class, () -> new BotClassProfileLoader().load(ungated));
	}

	@Test
	void rejectsDoctypesAndExternalEntities() throws IOException
	{
		Path file = write("cardinal--party.xml", completeProfile("""
			<?xml version="1.0"?>
			<!DOCTYPE botClassProfile [<!ENTITY external SYSTEM "file:///etc/passwd">]>
			<botClassProfile classId="cardinal" name="party" schemaVersion="1">
				<combat targeting="NONE" acquisitionRange="0" maximumChaseDistance="0" preferredRange="0" basicAttackPriority="0" />
				<skills>&external;</skills>
			</botClassProfile>
			"""));

		assertThrows(IllegalArgumentException.class, () -> new BotClassProfileLoader().load(file));
	}

	@Test
	void rejectsIncompleteAndDuplicateExactClassKnowledge() throws IOException
	{
		String complete = emptyProfile("cardinal", "party");
		Path file = write("cardinal--party.xml", complete.replace(relationshipRow(ClassId.fighter) + "\n", ""));
		IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, () -> new BotClassProfileLoader().load(file));
		assertTrue(missing.getMessage().contains("Missing exact class relationships"));
		assertTrue(missing.getMessage().contains("fighter"));

		Files.writeString(file, complete.replace("\t\t</relationships>", relationshipRow(ClassId.fighter) + "\n\t\t</relationships>"));
		IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class, () -> new BotClassProfileLoader().load(file));
		assertTrue(duplicate.getMessage().contains("Duplicate exact class relationship fighter"));
	}

	@Test
	void validatesConditionDomainsBeforeRegistration()
	{
		assertThrows(IllegalArgumentException.class, () -> new BotClassRelationship(ClassId.dummyEntry1, 100, 100, 100, 100, 100, 100));
		assertThrows(IllegalArgumentException.class, () -> new BotClassRelationship(ClassId.duelist, 0, 100, 100, 100, 100, 100));
		assertThrows(IllegalArgumentException.class, () -> new BotRuleCondition.Distance(BotRuleCondition.Subject.SELF, BotRuleCondition.Comparison.AT_MOST, 100));
		assertThrows(IllegalArgumentException.class, () -> new BotRuleCondition.Resource(BotRuleCondition.Subject.TARGET, BotRuleCondition.ResourceType.HP, BotRuleCondition.AmountType.PERCENT, BotRuleCondition.Comparison.BELOW, 101));
		assertThrows(IllegalArgumentException.class, () -> new GroupCount(BotRuleCondition.Group.PARTY, BotRuleCondition.GroupFilter.ALIVE, BotRuleCondition.Comparison.AT_LEAST, 1, 50, 1200));
		assertThrows(IllegalArgumentException.class, () -> new GroupClassCount(BotRuleCondition.Group.PARTY, ClassId.duelist, List.of(), BotRuleCondition.EffectState.ABSENT, BotRuleCondition.Comparison.AT_LEAST, 1, 1200));
		assertThrows(IllegalArgumentException.class, () -> new GroupClassCount(BotRuleCondition.Group.PARTY, ClassId.duelist, List.of(1068, 1068), BotRuleCondition.EffectState.ABSENT, BotRuleCondition.Comparison.AT_LEAST, 1, 1200));
	}

	private Path write(String filename, String xml) throws IOException
	{
		Path file = _directory.resolve(filename);
		Files.writeString(file, xml);
		return file;
	}

	private static String emptyProfile(String classId, String name)
	{
		return completeProfile("""
			<botClassProfile classId="%s" name="%s" schemaVersion="1">
				<combat targeting="NONE" acquisitionRange="0" maximumChaseDistance="0" preferredRange="0" basicAttackPriority="0" />
				<skills></skills>
			</botClassProfile>
			""".formatted(classId, name));
	}

	private static String completeProfile(String xml)
	{
		return xml.replace("schemaVersion=\"1\"", "schemaVersion=\"2\"").replace("<skills>", relationshipsXml() + "\n\t\t<skills>");
	}

	private static String relationshipsXml()
	{
		StringBuilder xml = new StringBuilder("<relationships>\n");
		for (ClassId classId : ClassId.values())
		{
			if (classId.getRace() != null)
			{
				xml.append(relationshipRow(classId)).append("\n");
			}
		}
		return xml.append("\t\t</relationships>").toString();
	}

	private static String relationshipRow(ClassId classId)
	{
		return "\t\t\t<class classId=\"" + classId.name() + "\" allySupportPriority=\"100\" allyProtectionPriority=\"100\" hostileAttackPriority=\"100\" hostileThreatPriority=\"100\" hostileControlPriority=\"100\" hostileInterruptPriority=\"100\" />";
	}
}
