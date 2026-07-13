#!/usr/bin/env python3
"""Compile explicit exact-class bot policy data into datapack XML."""

from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from xml.sax.saxutils import escape, quoteattr

from class_relationship_catalog import (
	EXACT_CLASS_RELATIONSHIP_MATRICES,
	EXACT_OBSERVED_CLASS_KNOWLEDGE,
	MAXIMUM_PRIORITY as MAXIMUM_RELATIONSHIP_PRIORITY,
	MINIMUM_PRIORITY as MINIMUM_RELATIONSHIP_PRIORITY,
)
from profile_catalog import (
	ALL_PLAYABLE_CLASSES,
	CATALOG,
	CHANT_OF_VICTORY_RECIPIENTS,
	CLERIC_BATTLEFIELD_RESURRECTION_BASE_PRIORITY,
	CLERIC_CURE_BASE_PRIORITY,
	CLERIC_RECOVERY_RECIPIENT_POLICIES,
	CLERIC_SAFE_RESURRECTION_BASE_PRIORITY,
	GREATER_MIGHT_RECIPIENTS,
	GREATER_SHIELD_RECIPIENTS,
	HEAL_PRIMARY_TANK_RECIPIENTS,
	HEAL_RECIPIENT_POLICIES,
	HEAL_SKIRMISHER_RECIPIENTS,
	HIEROPHANT_BATTLEFIELD_RESURRECTION_BASE_PRIORITY,
	HIEROPHANT_CURE_BASE_PRIORITY,
	HIEROPHANT_RECOVERY_RECIPIENT_POLICIES,
	HIEROPHANT_SAFE_RESURRECTION_BASE_PRIORITY,
	MAGNUS_CHANT_RECIPIENTS,
	ORACLE_BATTLEFIELD_RESURRECTION_BASE_PRIORITY,
	ORACLE_CURE_BASE_PRIORITY,
	ORACLE_RECOVERY_RECIPIENT_POLICIES,
	ORACLE_SAFE_RESURRECTION_BASE_PRIORITY,
	PROPHET_BATTLEFIELD_RESURRECTION_BASE_PRIORITY,
	PROPHET_CURE_BASE_PRIORITY,
	PROPHET_RECOVERY_RECIPIENT_POLICIES,
	PROPHET_SAFE_RESURRECTION_BASE_PRIORITY,
	RECHARGE_FORBIDDEN_RECIPIENTS,
	RECHARGE_PRIORITY_RECIPIENTS,
	RECHARGE_RESERVE_RECIPIENTS,
	RECHARGE_SKILL_RECIPIENTS,
)

REPOSITORY = Path(__file__).resolve().parents[2]
CLASS_ID_SOURCE = REPOSITORY / "official/l2j-server-game/src/main/java/net/sf/l2j/gameserver/model/base/ClassId.java"
SKILL_TREE_SOURCE = REPOSITORY / "official/l2j-server-datapack/src/main/resources/sql/skill_trees.sql"
SKILL_DATA_DIRECTORY = REPOSITORY / "official/l2j-server-datapack/src/main/resources/data/stats/skills"
OUTPUT_DIRECTORY = REPOSITORY / "official/l2j-server-datapack/src/main/resources/data/bots/profiles/classes"

CLASS_PATTERN = re.compile(
	r"^\s*([A-Za-z][A-Za-z0-9]*)\((0x[0-9a-f]+|\d+),\s*(?:true|false),\s*Race\.[a-z]+,\s*([A-Za-z][A-Za-z0-9]*|null)\)",
	re.MULTILINE,
)
SKILL_TREE_PATTERN = re.compile(
	r"^\s*INSERT INTO skill_trees VALUES \((\d+),(\d+),(\d+),'((?:\\.|''|[^'])*)',(\d+),(\d+)\);",
	re.MULTILINE,
)
KEY_PATTERN = re.compile(r"[a-z0-9][a-z0-9._-]{0,63}")
SUPPORT_INTENTS = frozenset(("HEAL", "GROUP_HEAL", "CP_HEAL", "RECHARGE", "CURE", "RESURRECT", "BUFF", "GROUP_BUFF", "SONG", "DANCE", "CHANT", "SERVITOR_SUPPORT"))
OWNED_OR_PARTY_SUPPORT_TARGETS = frozenset(("SELF", "SUMMON", "PARTY_LEADER", "PARTY_MEMBER", "PARTY_LOWEST_HP", "PARTY_LOWEST_MP", "PARTY_LOWEST_CP", "PARTY_DEAD_NEAREST", "PARTY_DEAD_HIGHEST_LEVEL"))
PARTY_SCOPED_NATIVE_TARGETS = frozenset(("TARGET_SELF", "TARGET_ONE", "TARGET_PARTY", "TARGET_PET", "TARGET_CORPSE_PLAYER", "TARGET_CORPSE_PET", "TARGET_PARTY_MEMBER", "TARGET_PARTY_OTHER", "TARGET_OWNER_PET"))


class CatalogError(ValueError):
	"""Raised when explicit authoring data violates the production contract."""


def load_classes():
	classes = []
	for name, numeric_id, parent in CLASS_PATTERN.findall(CLASS_ID_SOURCE.read_text(encoding="utf-8")):
		classes.append(
			{
				"name": name,
				"id": int(numeric_id, 0),
				"parent": None if parent == "null" else parent,
			}
		)
	if not classes:
		raise CatalogError(f"No playable ClassId declarations found in {CLASS_ID_SOURCE}")
	return tuple(classes)


def load_class_skills():
	direct = {}
	for class_id, skill_id, _level, _name, _sp, _minimum_level in SKILL_TREE_PATTERN.findall(
		SKILL_TREE_SOURCE.read_text(encoding="utf-8")
	):
		direct.setdefault(int(class_id), set()).add(int(skill_id))
	return direct


def load_known_skills():
	known = set()
	for path in sorted(SKILL_DATA_DIRECTORY.glob("*.xml")):
		for skill in ET.parse(path).getroot().iter("skill"):
			known.add(int(skill.attrib["id"]))
	if not known:
		raise CatalogError(f"No skill definitions found in {SKILL_DATA_DIRECTORY}")
	return known


def load_skill_targets():
	targets = {}
	for path in sorted(SKILL_DATA_DIRECTORY.glob("*.xml")):
		for skill in ET.parse(path).getroot().iter("skill"):
			skill_id = int(skill.attrib["id"])
			declarations = tuple(node.attrib.get("val") for node in skill.findall("set") if node.attrib.get("name") == "target")
			if len(declarations) != 1:
				raise CatalogError(f"Skill {skill_id} must declare exactly one native target in {path}")
			target = declarations[0]
			previous = targets.setdefault(skill_id, target)
			if previous != target:
				raise CatalogError(f"Skill {skill_id} declares conflicting native targets {previous} and {target}")
	return targets


def inherited_skill_ids(class_name, classes_by_name, direct_skills):
	result = set()
	current = classes_by_name[class_name]
	while current is not None:
		result.update(direct_skills.get(current["id"], ()))
		current = classes_by_name.get(current["parent"])
	return result


def kebab(value):
	return re.sub(r"(?<!^)(?=[A-Z])", "-", value).lower()


def xml_attributes(attributes):
	return " ".join(f"{name}={quoteattr(str(value).lower() if isinstance(value, bool) else str(value))}" for name, value in attributes)


def render_condition(condition):
	tag = condition.get("tag")
	attributes = condition.get("attributes")
	if not isinstance(tag, str) or not isinstance(attributes, tuple):
		raise CatalogError(f"Invalid direct condition declaration: {condition!r}")
	return f"\t\t\t\t<{escape(tag)} {xml_attributes(attributes)} />"


def render_skill(policy):
	attributes = (
		("key", policy["key"]),
		("skillId", policy["skill_id"]),
		("intent", policy["intent"]),
		("target", policy["target"]),
		("priority", policy["priority"]),
		("forceUse", policy["force_use"]),
		("dontMove", policy["dont_move"]),
	)
	lines = [f"\t\t<skill {xml_attributes(attributes)}>", "\t\t\t<when>"]
	lines.extend(render_condition(condition) for condition in policy["conditions"])
	lines.extend(("\t\t\t</when>", "\t\t</skill>"))
	return lines


def expanded_policies(class_name, profile, playable_order, known_skills, skill_targets, allowed_skills):
	policies = []
	keys = set()

	for direct in profile.get("skills", ()):
		policy = {
			"key": direct["key"],
			"skill_id": direct["skill_id"],
			"intent": direct["intent"],
			"target": direct["target"],
			"priority": direct["priority"],
			"force_use": direct.get("force_use", False),
			"dont_move": direct.get("dont_move", False),
			"conditions": tuple(direct["conditions"]),
		}
		validate_policy(class_name, policy, keys, known_skills, skill_targets, allowed_skills)
		policies.append(policy)

	for matrix in profile.get("buffs", ()):
		recipients = tuple(matrix["recipients"])
		if len(set(recipients)) != len(recipients):
			raise CatalogError(f"{class_name} buff {matrix['key']} repeats an exact recipient")
		unknown_recipients = set(recipients) - set(playable_order)
		if unknown_recipients:
			raise CatalogError(f"{class_name} buff {matrix['key']} names unknown recipients: {sorted(unknown_recipients)}")
		blocking_effects = tuple(matrix["blocking_effects"])
		if matrix["skill_id"] not in blocking_effects:
			raise CatalogError(f"{class_name} buff {matrix['key']} must block its own effect {matrix['skill_id']}")
		unknown_effects = set(blocking_effects) - known_skills
		if unknown_effects:
			raise CatalogError(f"{class_name} buff {matrix['key']} names unknown blocking effects: {sorted(unknown_effects)}")

		relationship_priority = matrix.get("relationship_priority")
		if relationship_priority not in (None, "ally_support_priority"):
			raise CatalogError(f"{class_name} buff {matrix['key']} uses unsupported relationship priority {relationship_priority}")

		for recipient in sorted(recipients, key=playable_order.index):
			conditions = [
				{
					"tag": "class",
					"attributes": (("subject", "TARGET"), ("classId", recipient), ("expected", True)),
				}
			]
			conditions.extend(
				{
					"tag": "effect",
					"attributes": (("subject", "TARGET"), ("skillId", effect_id), ("state", "ABSENT")),
				}
				for effect_id in blocking_effects
			)
			conditions.extend(matrix.get("conditions", ()))
			priority = matrix["priority"]
			if relationship_priority is not None:
				priority += getattr(profile["relationships"][recipient], relationship_priority) // 10
			policy = {
				"key": f"{kebab(recipient)}-{matrix['key']}",
				"skill_id": matrix["skill_id"],
				"intent": "BUFF",
				"target": "PARTY_MEMBER",
				"priority": priority,
				"force_use": False,
				"dont_move": False,
				"conditions": tuple(conditions),
			}
			validate_policy(class_name, policy, keys, known_skills, skill_targets, allowed_skills)
			policies.append(policy)

	for matrix in profile.get("group_effects", ()):
		recipients = tuple(matrix["recipients"])
		if len(set(recipients)) != len(recipients):
			raise CatalogError(f"{class_name} group effect {matrix['key']} repeats an exact recipient")
		unknown_recipients = set(recipients) - set(playable_order)
		if unknown_recipients:
			raise CatalogError(f"{class_name} group effect {matrix['key']} names unknown recipients: {sorted(unknown_recipients)}")
		if not 0 <= matrix["mp_reserve"] < 100:
			raise CatalogError(f"{class_name} group effect {matrix['key']} has invalid MP reserve {matrix['mp_reserve']}")
		blocking_effects = tuple(matrix["blocking_effects"])
		if matrix["skill_id"] not in blocking_effects:
			raise CatalogError(f"{class_name} group effect {matrix['key']} must block its own effect {matrix['skill_id']}")
		if len(set(blocking_effects)) != len(blocking_effects):
			raise CatalogError(f"{class_name} group effect {matrix['key']} repeats a blocking effect")
		unknown_effects = set(blocking_effects) - known_skills
		if unknown_effects:
			raise CatalogError(f"{class_name} group effect {matrix['key']} names unknown blocking effects: {sorted(unknown_effects)}")
		relationship_priority = matrix.get("relationship_priority")
		if relationship_priority not in (None, "ally_support_priority", "ally_protection_priority"):
			raise CatalogError(f"{class_name} group effect {matrix['key']} uses unsupported relationship priority {relationship_priority}")
		relationship_priority_divisor = matrix.get("relationship_priority_divisor", 10)
		if not isinstance(relationship_priority_divisor, int) or relationship_priority_divisor <= 0:
			raise CatalogError(f"{class_name} group effect {matrix['key']} has invalid relationship priority divisor {relationship_priority_divisor}")

		for recipient in sorted(recipients, key=playable_order.index):
			priority = matrix["priority"]
			if relationship_priority is not None:
				priority += getattr(profile["relationships"][recipient], relationship_priority) // relationship_priority_divisor
			policy = {
				"key": f"{kebab(recipient)}-{matrix['key']}",
				"skill_id": matrix["skill_id"],
				"intent": matrix["intent"],
				"target": "SELF",
				"priority": priority,
				"force_use": False,
				"dont_move": False,
				"conditions": (
					{
						"tag": "status",
						"attributes": (("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
					},
					{
						"tag": "resource",
						"attributes": (("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", matrix["mp_reserve"])),
					},
					{
						"tag": "classCount",
						"attributes": (("group", "PARTY"), ("classId", recipient), ("effectSkillIds", ",".join(str(effect_id) for effect_id in blocking_effects)), ("effectState", "ABSENT"), ("comparison", "AT_LEAST"), ("value", 1), ("radius", matrix["radius"])),
					},
					*matrix.get("conditions", ()),
				),
			}
			validate_policy(class_name, policy, keys, known_skills, skill_targets, allowed_skills)
			policies.append(policy)

	for matrix in profile.get("targeted", ()):
		recipients = tuple(matrix["recipients"])
		if len(set(recipients)) != len(recipients):
			raise CatalogError(f"{class_name} targeted policy {matrix['key']} repeats an exact recipient")
		unknown_recipients = set(recipients) - set(playable_order)
		if unknown_recipients:
			raise CatalogError(f"{class_name} targeted policy {matrix['key']} names unknown recipients: {sorted(unknown_recipients)}")
		for condition in matrix["conditions"]:
			attributes = dict(condition.get("attributes", ()))
			if (condition.get("tag") == "class") and (attributes.get("subject") == "TARGET") and attributes.get("expected"):
				raise CatalogError(f"{class_name} targeted policy {matrix['key']} cannot add a second positive TARGET class")

		for recipient in sorted(recipients, key=playable_order.index):
			conditions = (
				{
					"tag": "class",
					"attributes": (("subject", "TARGET"), ("classId", recipient), ("expected", True)),
				},
				*matrix["conditions"],
			)
			policy = {
				"key": f"{kebab(recipient)}-{matrix['key']}",
				"skill_id": matrix["skill_id"],
				"intent": matrix["intent"],
				"target": matrix["target"],
				"priority": matrix["priority"],
				"force_use": matrix.get("force_use", False),
				"dont_move": matrix.get("dont_move", False),
				"conditions": conditions,
			}
			validate_policy(class_name, policy, keys, known_skills, skill_targets, allowed_skills)
			policies.append(policy)

	return tuple(policies)


def validate_policy(class_name, policy, keys, known_skills, skill_targets, allowed_skills):
	key = policy["key"]
	if not KEY_PATTERN.fullmatch(key):
		raise CatalogError(f"{class_name} policy key is invalid: {key!r}")
	if key in keys:
		raise CatalogError(f"{class_name} repeats policy key {key}")
	keys.add(key)
	if policy["skill_id"] not in known_skills:
		raise CatalogError(f"{class_name} policy {key} references unknown skill {policy['skill_id']}")
	if policy["skill_id"] not in allowed_skills:
		raise CatalogError(f"{class_name} policy {key} references skill {policy['skill_id']} outside its inherited class tree")
	if policy["intent"] == "CLAN_BUFF":
		raise CatalogError(f"{class_name} policy {key} uses CLAN_BUFF outside the self, owned summon, and current-party support boundary")
	if policy["intent"] in SUPPORT_INTENTS and policy["target"] not in OWNED_OR_PARTY_SUPPORT_TARGETS:
		raise CatalogError(f"{class_name} policy {key} targets {policy['target']} for {policy['intent']} outside the self, owned summon, and current-party support boundary")
	if policy["intent"] in SUPPORT_INTENTS and skill_targets.get(policy["skill_id"]) not in PARTY_SCOPED_NATIVE_TARGETS:
		raise CatalogError(f"{class_name} policy {key} uses support skill {policy['skill_id']} whose native target {skill_targets.get(policy['skill_id'])} can expand beyond the current party")
	if not 1 <= policy["priority"] <= 1_000_000:
		raise CatalogError(f"{class_name} policy {key} has invalid priority {policy['priority']}")
	if not policy["conditions"]:
		raise CatalogError(f"{class_name} policy {key} has no explicit conditions")
	for condition in policy["conditions"]:
		tag = condition.get("tag")
		attributes = dict(condition.get("attributes", ()))
		if tag == "effect":
			effect_id = attributes.get("skillId")
			if effect_id not in known_skills:
				raise CatalogError(f"{class_name} policy {key} references unknown effect skill {effect_id}")
		elif tag == "classCount":
			effect_ids = tuple(int(value) for value in attributes.get("effectSkillIds", "").split(",") if value)
			unknown_effects = set(effect_ids) - known_skills
			if not effect_ids or unknown_effects:
				raise CatalogError(f"{class_name} policy {key} references unknown group effect skills: {sorted(unknown_effects)}")


def render_profile(class_name, profile, policies):
	combat = profile["combat"]
	relationships = profile["relationships"]
	root_attributes = (("classId", class_name), ("name", profile["profile_name"]), ("schemaVersion", 2))
	combat_attributes = (
		("targeting", combat["targeting"]),
		("acquisitionRange", combat["acquisition_range"]),
		("maximumChaseDistance", combat["maximum_chase_distance"]),
		("preferredRange", combat["preferred_range"]),
		("basicAttackPriority", combat["basic_attack_priority"]),
	)
	lines = [
		'<?xml version="1.0" encoding="UTF-8"?>',
		"<!-- Generated from scripts/bots catalogs; edit the source catalogs, not this file. -->",
		f"<botClassProfile {xml_attributes(root_attributes)}>",
		f"\t<combat {xml_attributes(combat_attributes)} />",
		"\t<relationships>",
	]
	for observed_class, relationship in relationships.items():
		attributes = (
			("classId", observed_class),
			("allySupportPriority", relationship.ally_support_priority),
			("allyProtectionPriority", relationship.ally_protection_priority),
			("hostileAttackPriority", relationship.hostile_attack_priority),
			("hostileThreatPriority", relationship.hostile_threat_priority),
			("hostileControlPriority", relationship.hostile_control_priority),
			("hostileInterruptPriority", relationship.hostile_interrupt_priority),
		)
		lines.append(f"\t\t<class {xml_attributes(attributes)} />")
	lines.extend(("\t</relationships>", "\t<skills>"))
	for policy in policies:
		lines.extend(render_skill(policy))
	lines.extend(("\t</skills>", "</botClassProfile>", ""))
	return "\n".join(lines)


def validate_relationship_matrix(class_name, profile, playable_order):
	matrix = profile.get("relationships")
	if not isinstance(matrix, dict):
		raise CatalogError(f"{class_name} must own one exact class relationship matrix")
	if tuple(matrix) != tuple(playable_order):
		raise CatalogError(f"{class_name} relationship matrix must name every playable ClassId in declaration order")
	for observed_class, relationship in matrix.items():
		if len(relationship) != 6:
			raise CatalogError(f"{class_name} relationship for {observed_class} must declare all six priorities")
		for priority in relationship:
			if not isinstance(priority, int) or not (MINIMUM_RELATIONSHIP_PRIORITY <= priority <= MAXIMUM_RELATIONSHIP_PRIORITY):
				raise CatalogError(f"{class_name} relationship for {observed_class} has invalid priority {priority}")


def validate_resurrection_policies(caster_name, recipient_policies, safe_base_priority, battlefield_base_priority, playable_order, maximum_efficient_heal, minimum_battle_heal):
	if tuple(recipient_policies) != tuple(playable_order):
		raise CatalogError(f"{caster_name} exact resurrection policies must name every playable ClassId in declaration order")
	for recipient, policy in recipient_policies.items():
		if not (0 <= policy.duty_priority <= 6000):
			raise CatalogError(f"{caster_name} exact resurrection duty priority is outside the supported band for {recipient}")
	maximum_interlude_level_urgency = 80
	for higher_duty, lower_duty in (("cardinal", "phoenixKnight"), ("phoenixKnight", "abyssWalker")):
		if recipient_policies[higher_duty].duty_priority <= (recipient_policies[lower_duty].duty_priority + maximum_interlude_level_urgency):
			raise CatalogError(f"{caster_name} exact resurrection duty for {higher_duty} must dominate {lower_duty} plus recipient level urgency")
	minimum_safe_resurrection = safe_base_priority + min(policy.duty_priority for policy in recipient_policies.values())
	maximum_battlefield_resurrection = battlefield_base_priority + max(policy.duty_priority for policy in recipient_policies.values()) + maximum_interlude_level_urgency
	if maximum_efficient_heal >= minimum_safe_resurrection:
		raise CatalogError(f"{caster_name} efficient Heal scores overlap the exact safe-resurrection tactical band")
	if maximum_battlefield_resurrection >= minimum_battle_heal:
		raise CatalogError(f"{caster_name} battlefield resurrection scores overlap the emergency Battle Heal tactical band")


def validate_exact_support_tactical_bands(caster_name, recipient_policies, cure_base_priority, safe_resurrection_base_priority, battlefield_resurrection_base_priority, playable_order, maximum_context_urgency):
	minimum_protection = min(policy.duty_priority for policy in recipient_policies.values())
	maximum_protection = max(policy.duty_priority for policy in recipient_policies.values())
	minimum_battle_heal = 30000 + minimum_protection
	validate_resurrection_policies(caster_name, recipient_policies, safe_resurrection_base_priority, battlefield_resurrection_base_priority, playable_order, 10000 + maximum_protection + maximum_context_urgency, minimum_battle_heal)
	maximum_cure = cure_base_priority + maximum_protection
	minimum_cure = cure_base_priority + minimum_protection
	maximum_resurrection = battlefield_resurrection_base_priority + maximum_protection + 80
	if maximum_resurrection >= minimum_cure:
		raise CatalogError(f"{caster_name} battlefield resurrection scores overlap the ailment-cure tactical band")
	if minimum_cure <= 27000:
		raise CatalogError(f"{caster_name} Cure Poison scores overlap the inherited Group Heal tactical band")
	if maximum_cure >= minimum_battle_heal:
		raise CatalogError(f"{caster_name} Cure Poison scores overlap the emergency Battle Heal tactical band")


def exact_target_recipient(class_name, policy):
	recipients = []
	for condition in policy["conditions"]:
		if condition.get("tag") != "class":
			continue
		attributes = dict(condition.get("attributes", ()))
		if (attributes.get("subject") == "TARGET") and attributes.get("expected"):
			recipients.append(attributes.get("classId"))
	if len(recipients) != 1:
		raise CatalogError(f"{class_name} policy {policy['key']} must name one positive exact TARGET ClassId")
	return recipients[0]


def exact_policies_by_recipient(class_name, policies, intent, playable_order):
	result = {recipient: [] for recipient in playable_order}
	for policy in policies:
		if policy["intent"] != intent:
			continue
		recipient = exact_target_recipient(class_name, policy)
		if recipient not in result:
			raise CatalogError(f"{class_name} {intent} policy {policy['key']} names unknown exact recipient {recipient}")
		result[recipient].append(policy)
	return result


def validate_exact_party_support(caster_name, profile, policies, playable_order, buff_skill_ids, universal_buff_skill_ids, cure_base_priority, safe_resurrection_base_priority, battlefield_resurrection_base_priority, forbidden_unconditional_skills):
	"""Certify one exact class's independently authored party-support contract."""
	if {matrix["skill_id"] for matrix in profile.get("buffs", ())} != buff_skill_ids:
		raise CatalogError(f"{caster_name} exact buff catalog must contain its complete certified support skill set")
	if any(matrix.get("relationship_priority") != "ally_support_priority" for matrix in profile.get("buffs", ())):
		raise CatalogError(f"Every {caster_name} buff must use its exact ally-support relationship priority")

	buffs = exact_policies_by_recipient(caster_name, policies, "BUFF", playable_order)
	base_priority_by_skill = {matrix["skill_id"]: matrix["priority"] for matrix in profile["buffs"]}
	for recipient, recipient_policies in buffs.items():
		recipient_skills = {policy["skill_id"] for policy in recipient_policies}
		if not universal_buff_skill_ids <= recipient_skills:
			raise CatalogError(f"{caster_name} exact buff contract is incomplete for {recipient}")
		relationship = profile["relationships"][recipient]
		for policy in recipient_policies:
			expected_priority = base_priority_by_skill[policy["skill_id"]] + (relationship.ally_support_priority // 10)
			if policy["priority"] != expected_priority:
				raise CatalogError(f"{caster_name} buff {policy['key']} does not use {recipient}'s exact ally-support priority")

	heals = exact_policies_by_recipient(caster_name, policies, "HEAL", playable_order)
	for recipient, recipient_policies in heals.items():
		if (len(recipient_policies) != 2) or ({policy["skill_id"] for policy in recipient_policies} != {1011, 1015}):
			raise CatalogError(f"{caster_name} exact heal contract is incomplete for {recipient}")
		protection_priority = profile["relationships"][recipient].ally_protection_priority
		for policy in recipient_policies:
			expected_priority = (30000 if policy["skill_id"] == 1015 else 10000) + protection_priority
			if policy["priority"] != expected_priority:
				raise CatalogError(f"{caster_name} heal {policy['key']} does not use {recipient}'s exact ally-protection priority")

	cures = exact_policies_by_recipient(caster_name, policies, "CURE", playable_order)
	for recipient, recipient_policies in cures.items():
		if (len(recipient_policies) != 1) or (recipient_policies[0]["skill_id"] != 1012):
			raise CatalogError(f"{caster_name} exact Cure Poison contract is incomplete for {recipient}")
		policy = recipient_policies[0]
		expected_priority = cure_base_priority + profile["relationships"][recipient].ally_protection_priority
		if policy["priority"] != expected_priority:
			raise CatalogError(f"{caster_name} Cure Poison {policy['key']} does not use {recipient}'s exact ally-protection priority")
		poison_conditions = [condition for condition in policy["conditions"] if (condition.get("tag") == "status") and (dict(condition.get("attributes", ())).get("subject") == "TARGET") and (dict(condition.get("attributes", ())).get("status") == "POISONED") and dict(condition.get("attributes", ())).get("expected")]
		if len(poison_conditions) != 1:
			raise CatalogError(f"{caster_name} Cure Poison {policy['key']} must require an actually poisoned exact recipient")

	resurrections = exact_policies_by_recipient(caster_name, policies, "RESURRECT", playable_order)
	for recipient, recipient_policies in resurrections.items():
		if (len(recipient_policies) != 2) or ({policy["skill_id"] for policy in recipient_policies} != {1016}):
			raise CatalogError(f"{caster_name} exact resurrection contract is incomplete for {recipient}")
		protection_priority = profile["relationships"][recipient].ally_protection_priority
		expected_priorities = {
			safe_resurrection_base_priority + protection_priority,
			battlefield_resurrection_base_priority + protection_priority,
		}
		if {policy["priority"] for policy in recipient_policies} != expected_priorities:
			raise CatalogError(f"{caster_name} resurrection priorities do not preserve {recipient}'s exact ally-protection duty")

	group_heals = [policy for policy in policies if policy["intent"] == "GROUP_HEAL"]
	if (len(group_heals) != 1) or (group_heals[0]["skill_id"] != 1027):
		raise CatalogError(f"{caster_name} must retain its inherited exact party Group Heal policy")
	if any(policy["skill_id"] in forbidden_unconditional_skills for policy in policies):
		raise CatalogError(f"{caster_name} cannot blanket-cast forbidden situational or environmental skills")


def validate_exact_recharge_support(caster_name, profile, policies, playable_order, self_reserve):
	"""Certify caster-owned Recharge choices for every eligible exact class."""
	recharge_by_recipient = exact_policies_by_recipient(caster_name, policies, "RECHARGE", playable_order)
	tiers = (
		(12500, 60, set(RECHARGE_PRIORITY_RECIPIENTS)),
		(12000, 35, set(RECHARGE_SKILL_RECIPIENTS)),
		(11500, 20, set(RECHARGE_RESERVE_RECIPIENTS)),
	)
	for recipient, recipient_policies in recharge_by_recipient.items():
		if recipient in RECHARGE_FORBIDDEN_RECIPIENTS:
			if recipient_policies:
				raise CatalogError(f"{caster_name} cannot Recharge forbidden exact class {recipient}")
			continue
		if len(recipient_policies) != 1:
			raise CatalogError(f"{caster_name} exact Recharge contract is incomplete for {recipient}")
		matching_tiers = [(base_priority, threshold) for base_priority, threshold, recipients in tiers if recipient in recipients]
		if len(matching_tiers) != 1:
			raise CatalogError(f"{caster_name} exact Recharge tier is invalid for {recipient}")
		base_priority, threshold = matching_tiers[0]
		policy = recipient_policies[0]
		expected_priority = base_priority + (profile["relationships"][recipient].ally_support_priority // 10)
		if (policy["skill_id"] != 1013) or (policy["priority"] != expected_priority):
			raise CatalogError(f"{caster_name} Recharge {policy['key']} does not use {recipient}'s exact ally-support priority")
		resources = tuple(dict(condition.get("attributes", ())) for condition in policy["conditions"] if condition.get("tag") == "resource")
		if not any((attributes.get("subject") == "TARGET") and (attributes.get("resource") == "MP") and (attributes.get("comparison") == "BELOW") and (attributes.get("value") == threshold) for attributes in resources):
			raise CatalogError(f"{caster_name} Recharge {policy['key']} has the wrong exact-class MP threshold")
		if not any((attributes.get("subject") == "SELF") and (attributes.get("resource") == "MP") and (attributes.get("comparison") == "ABOVE") and (attributes.get("value") == self_reserve) for attributes in resources):
			raise CatalogError(f"{caster_name} Recharge {policy['key']} has the wrong self MP reserve")


def exact_group_policies_by_recipient(class_name, policies, intent, playable_order):
	result = {recipient: [] for recipient in playable_order}
	for policy in policies:
		if policy["intent"] != intent:
			continue
		counts = []
		for condition in policy["conditions"]:
			if condition.get("tag") != "classCount":
				continue
			attributes = dict(condition.get("attributes", ()))
			if attributes.get("group") == "PARTY":
				counts.append(attributes.get("classId"))
		if len(counts) != 1:
			raise CatalogError(f"{class_name} group policy {policy['key']} must name one exact PARTY beneficiary")
		recipient = counts[0]
		if recipient not in result:
			raise CatalogError(f"{class_name} {intent} policy {policy['key']} names unknown exact beneficiary {recipient}")
		result[recipient].append(policy)
	return result


def validate_exact_chant_support(caster_name, profile, policies, playable_order, chant_skill_ids, universal_chant_skill_ids, exclusive_chant_partition=frozenset()):
	"""Certify one exact class's independently ranked party chant decisions."""
	chant_matrices = [matrix for matrix in profile.get("group_effects", ()) if matrix["intent"] == "CHANT"]
	if {matrix["skill_id"] for matrix in chant_matrices} != chant_skill_ids:
		raise CatalogError(f"{caster_name} exact chant catalog must contain its complete certified support skill set")
	if any(matrix.get("relationship_priority") != "ally_support_priority" for matrix in chant_matrices):
		raise CatalogError(f"Every {caster_name} chant must use its exact ally-support relationship priority")

	chants = exact_group_policies_by_recipient(caster_name, policies, "CHANT", playable_order)
	base_priority_by_skill = {matrix["skill_id"]: matrix["priority"] for matrix in chant_matrices}
	for recipient, recipient_policies in chants.items():
		recipient_skill_ids = {policy["skill_id"] for policy in recipient_policies}
		if not universal_chant_skill_ids <= recipient_skill_ids:
			raise CatalogError(f"{caster_name} exact chant contract is incomplete for {recipient}")
		if exclusive_chant_partition and len(recipient_skill_ids & exclusive_chant_partition) != 1:
			raise CatalogError(f"{caster_name} exact chant partition is invalid for {recipient}")
		support_priority = profile["relationships"][recipient].ally_support_priority
		for policy in recipient_policies:
			if policy["priority"] != base_priority_by_skill[policy["skill_id"]] + (support_priority // 10):
				raise CatalogError(f"{caster_name} chant {policy['key']} does not use {recipient}'s exact ally-support priority")

	recoveries = exact_group_policies_by_recipient(caster_name, policies, "GROUP_HEAL", playable_order)
	for recipient, recipient_policies in recoveries.items():
		if (len(recipient_policies) != 1) or (recipient_policies[0]["skill_id"] != 1229):
			raise CatalogError(f"{caster_name} exact Chant of Life contract is incomplete for {recipient}")
		policy = recipient_policies[0]
		expected_priority = 27000 + (profile["relationships"][recipient].ally_protection_priority // 10)
		if policy["priority"] != expected_priority:
			raise CatalogError(f"{caster_name} Chant of Life {policy['key']} does not use {recipient}'s exact ally-protection priority")
		conditions = tuple((condition.get("tag"), dict(condition.get("attributes", ()))) for condition in policy["conditions"])
		if not any((tag == "status") and (attributes.get("subject") == "SELF") and (attributes.get("status") == "IN_COMBAT") and attributes.get("expected") for tag, attributes in conditions):
			raise CatalogError(f"{caster_name} Chant of Life {policy['key']} must require combat")
		if not any((tag == "count") and (attributes.get("group") == "PARTY") and (attributes.get("filter") == "HP_PERCENT_BELOW") and (attributes.get("comparison") == "AT_LEAST") and (attributes.get("value") == 2) and (attributes.get("threshold") == 75) and (attributes.get("radius") == 1000) for tag, attributes in conditions):
			raise CatalogError(f"{caster_name} Chant of Life {policy['key']} must require two injured current-party members")


def compile_catalog():
	classes = load_classes()
	playable_order = tuple(entry["name"] for entry in classes)
	if playable_order != ALL_PLAYABLE_CLASSES:
		raise CatalogError("ALL_PLAYABLE_CLASSES must exactly match playable ClassId declaration order")
	if tuple(EXACT_OBSERVED_CLASS_KNOWLEDGE) != playable_order:
		raise CatalogError("Exact observed-class knowledge must match playable ClassId declaration order")
	if set(EXACT_CLASS_RELATIONSHIP_MATRICES) != set(CATALOG):
		raise CatalogError("Every catalog profile must own exactly one relationship matrix")
	if len({tuple(matrix.items()) for matrix in EXACT_CLASS_RELATIONSHIP_MATRICES.values()}) != len(CATALOG):
		raise CatalogError("Exact bot classes cannot share an identical relationship matrix")
	if set(GREATER_MIGHT_RECIPIENTS) & set(GREATER_SHIELD_RECIPIENTS):
		raise CatalogError("Greater Might and Greater Shield exact recipient lists overlap")
	if set(GREATER_MIGHT_RECIPIENTS) | set(GREATER_SHIELD_RECIPIENTS) != set(playable_order):
		raise CatalogError("Greater Might and Greater Shield exact recipient lists must partition playable ClassId values")
	recharge_tiers = (set(RECHARGE_PRIORITY_RECIPIENTS), set(RECHARGE_SKILL_RECIPIENTS), set(RECHARGE_RESERVE_RECIPIENTS))
	if any(left & right for index, left in enumerate(recharge_tiers) for right in recharge_tiers[index + 1 :]):
		raise CatalogError("Exact Recharge recipient tiers overlap")
	if set().union(*recharge_tiers) | set(RECHARGE_FORBIDDEN_RECIPIENTS) != set(playable_order):
		raise CatalogError("Exact Recharge tiers and forbidden recipients must cover playable ClassId values")
	if set().union(*recharge_tiers) & set(RECHARGE_FORBIDDEN_RECIPIENTS):
		raise CatalogError("Recharge-forbidden exact classes cannot appear in a recipient tier")
	third_class_chants = (set(MAGNUS_CHANT_RECIPIENTS), set(CHANT_OF_VICTORY_RECIPIENTS))
	if third_class_chants[0] & third_class_chants[1]:
		raise CatalogError("Magnus' Chant and Chant of Victory exact recipient lists overlap")
	if set().union(*third_class_chants) != set(playable_order):
		raise CatalogError("Magnus' Chant and Chant of Victory must partition playable ClassId values")
	if tuple(HEAL_RECIPIENT_POLICIES) != tuple(playable_order):
		raise CatalogError("Exact heal recipient policies must name every playable ClassId in declaration order")
	for recipient, policy in HEAL_RECIPIENT_POLICIES.items():
		if not (0 < policy.battle_threshold < policy.major_threshold < policy.greater_threshold < 100):
			raise CatalogError(f"Exact heal thresholds are not strictly ordered for {recipient}")
		if not (0 <= policy.duty_priority <= 5000):
			raise CatalogError(f"Exact heal duty priority is outside the supported band for {recipient}")
	maximum_context_urgency = 235
	minimum_tank_priority = min(HEAL_RECIPIENT_POLICIES[recipient].duty_priority for recipient in HEAL_PRIMARY_TANK_RECIPIENTS)
	maximum_skirmisher_priority = max(HEAL_RECIPIENT_POLICIES[recipient].duty_priority for recipient in HEAL_SKIRMISHER_RECIPIENTS)
	if minimum_tank_priority <= (maximum_skirmisher_priority + maximum_context_urgency):
		raise CatalogError("Exact heal tank duty must dominate maximum skirmisher context urgency")
	minimum_duty_priority = min(policy.duty_priority for policy in HEAL_RECIPIENT_POLICIES.values())
	maximum_duty_priority = max(policy.duty_priority for policy in HEAL_RECIPIENT_POLICIES.values())
	if (10000 + maximum_duty_priority + maximum_context_urgency) >= (20000 + minimum_duty_priority):
		raise CatalogError("Greater Heal duty and context scores overlap the Major Heal tactical band")
	if (20000 + maximum_duty_priority + maximum_context_urgency) >= (30000 + minimum_duty_priority):
		raise CatalogError("Major Heal duty and context scores overlap the Battle Heal tactical band")
	maximum_efficient_heal = 10000 + maximum_duty_priority + maximum_context_urgency
	minimum_battle_heal = 30000 + minimum_duty_priority
	validate_exact_support_tactical_bands("Cleric", CLERIC_RECOVERY_RECIPIENT_POLICIES, CLERIC_CURE_BASE_PRIORITY, CLERIC_SAFE_RESURRECTION_BASE_PRIORITY, CLERIC_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, playable_order, maximum_context_urgency)
	validate_exact_support_tactical_bands("Oracle", ORACLE_RECOVERY_RECIPIENT_POLICIES, ORACLE_CURE_BASE_PRIORITY, ORACLE_SAFE_RESURRECTION_BASE_PRIORITY, ORACLE_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, playable_order, maximum_context_urgency)
	validate_exact_support_tactical_bands("Prophet", PROPHET_RECOVERY_RECIPIENT_POLICIES, PROPHET_CURE_BASE_PRIORITY, PROPHET_SAFE_RESURRECTION_BASE_PRIORITY, PROPHET_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, playable_order, maximum_context_urgency)
	validate_exact_support_tactical_bands("Hierophant", HIEROPHANT_RECOVERY_RECIPIENT_POLICIES, HIEROPHANT_CURE_BASE_PRIORITY, HIEROPHANT_SAFE_RESURRECTION_BASE_PRIORITY, HIEROPHANT_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, playable_order, maximum_context_urgency)

	classes_by_name = {entry["name"]: entry for entry in classes}
	direct_skills = load_class_skills()
	known_skills = load_known_skills()
	skill_targets = load_skill_targets()
	generated = {}
	total_policies = 0
	for class_name, profile in CATALOG.items():
		if class_name not in classes_by_name:
			raise CatalogError(f"Catalog profile names unknown exact ClassId {class_name}")
		allowed_skills = inherited_skill_ids(class_name, classes_by_name, direct_skills)
		validate_relationship_matrix(class_name, profile, playable_order)
		policies = expanded_policies(class_name, profile, playable_order, known_skills, skill_targets, allowed_skills)
		if class_name == "cleric":
			validate_exact_party_support(
				"Cleric",
				profile,
				policies,
				playable_order,
				{1035, 1040, 1044, 1062, 1068, 1077, 1078, 1085, 1204},
				{1035, 1040, 1204},
				CLERIC_CURE_BASE_PRIORITY,
				CLERIC_SAFE_RESURRECTION_BASE_PRIORITY,
				CLERIC_BATTLEFIELD_RESURRECTION_BASE_PRIORITY,
				{1043, 1073, 1191},
			)
		elif class_name == "oracle":
			validate_exact_party_support(
				"Oracle",
				profile,
				policies,
				playable_order,
				{1033, 1035, 1040, 1044, 1068, 1078, 1087, 1204, 1257},
				{1033, 1035, 1040, 1204},
				ORACLE_CURE_BASE_PRIORITY,
				ORACLE_SAFE_RESURRECTION_BASE_PRIORITY,
				ORACLE_BATTLEFIELD_RESURRECTION_BASE_PRIORITY,
				{1043, 1073},
			)
			validate_exact_recharge_support("Oracle", profile, policies, playable_order, 45)
		elif class_name == "hierophant":
			validate_exact_party_support(
				"Hierophant",
				profile,
				policies,
				playable_order,
				{1032, 1033, 1035, 1036, 1040, 1044, 1045, 1048, 1062, 1068, 1077, 1078, 1085, 1086, 1204, 1240, 1242, 1243, 1352, 1356, 1388, 1389},
				{1032, 1033, 1035, 1036, 1040, 1045, 1204, 1352},
				HIEROPHANT_CURE_BASE_PRIORITY,
				HIEROPHANT_SAFE_RESURRECTION_BASE_PRIORITY,
				HIEROPHANT_BATTLEFIELD_RESURRECTION_BASE_PRIORITY,
				{1043, 1073, 1182, 1189, 1191, 1392, 1393, 1411, 1412},
			)
		elif class_name == "prophet":
			validate_exact_party_support(
				"Prophet",
				profile,
				policies,
				playable_order,
				{1032, 1033, 1035, 1036, 1040, 1044, 1045, 1048, 1062, 1068, 1077, 1078, 1085, 1086, 1204, 1240, 1242, 1243, 1388, 1389},
				{1032, 1033, 1035, 1036, 1040, 1045, 1204},
				PROPHET_CURE_BASE_PRIORITY,
				PROPHET_SAFE_RESURRECTION_BASE_PRIORITY,
				PROPHET_BATTLEFIELD_RESURRECTION_BASE_PRIORITY,
				{1043, 1073, 1182, 1189, 1191, 1352, 1356, 1392, 1393},
			)
		elif class_name == "warcryer":
			validate_exact_chant_support("Warcryer", profile, policies, playable_order, {1002, 1006, 1007, 1009, 1251, 1252, 1253, 1284, 1308, 1309, 1310, 1390, 1391}, {1006, 1009})
		elif class_name == "doomcryer":
			validate_exact_chant_support("Doomcryer", profile, policies, playable_order, {1002, 1006, 1007, 1009, 1251, 1252, 1253, 1284, 1308, 1309, 1310, 1362, 1363, 1390, 1391, 1413}, {1006, 1009, 1362}, {1363, 1413})
		filename = f"{class_name}--{profile['profile_name']}.xml"
		generated[filename] = render_profile(class_name, profile, policies)
		total_policies += len(policies)
	return generated, total_policies


def write_profiles(generated):
	OUTPUT_DIRECTORY.mkdir(parents=True, exist_ok=True)
	for filename, contents in generated.items():
		(OUTPUT_DIRECTORY / filename).write_text(contents, encoding="utf-8", newline="\n")


def check_profiles(generated):
	existing = {path.name: path for path in OUTPUT_DIRECTORY.glob("*.xml")} if OUTPUT_DIRECTORY.is_dir() else {}
	errors = []
	for filename, contents in generated.items():
		path = existing.pop(filename, None)
		if path is None:
			errors.append(f"missing generated profile {filename}")
		elif path.read_text(encoding="utf-8") != contents:
			errors.append(f"generated profile is stale: {filename}")
	for filename in sorted(existing):
		errors.append(f"profile is not declared by the catalog: {filename}")
	if errors:
		raise CatalogError("; ".join(errors))


def main():
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("--check", action="store_true", help="fail unless committed XML exactly matches the explicit catalog")
	arguments = parser.parse_args()
	generated, total_policies = compile_catalog()
	if arguments.check:
		check_profiles(generated)
		verb = "verified"
	else:
		write_profiles(generated)
		verb = "generated"
	print(f"{verb} {len(generated)} exact-class profiles with {total_policies} explicit policies")


if __name__ == "__main__":
	try:
		main()
	except (CatalogError, ET.ParseError, OSError) as error:
		print(f"bot profile compilation failed: {error}", file=sys.stderr)
		raise SystemExit(1) from error
