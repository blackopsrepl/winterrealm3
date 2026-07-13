#!/usr/bin/env python3
"""Compile explicit exact-class bot policy data into datapack XML."""

from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from xml.sax.saxutils import escape, quoteattr

from profile_catalog import (
	ALL_PLAYABLE_CLASSES,
	CATALOG,
	CHANT_OF_VICTORY_RECIPIENTS,
	GREATER_MIGHT_RECIPIENTS,
	GREATER_SHIELD_RECIPIENTS,
	HEAL_PRIMARY_TANK_RECIPIENTS,
	HEAL_RECIPIENT_POLICIES,
	HEAL_SKIRMISHER_RECIPIENTS,
	MAGNUS_CHANT_RECIPIENTS,
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
	r"INSERT INTO skill_trees VALUES \((\d+),(\d+),(\d+),'((?:\\.|''|[^'])*)',(\d+),(\d+)\);"
)
KEY_PATTERN = re.compile(r"[a-z0-9][a-z0-9._-]{0,63}")


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


def expanded_policies(class_name, profile, playable_order, known_skills, allowed_skills):
	policies = []
	keys = set()
	covered_recipients = set()

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
		validate_policy(class_name, policy, keys, known_skills, allowed_skills)
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

		for recipient in sorted(recipients, key=playable_order.index):
			covered_recipients.add(recipient)
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
			policy = {
				"key": f"{kebab(recipient)}-{matrix['key']}",
				"skill_id": matrix["skill_id"],
				"intent": "BUFF",
				"target": "PARTY_MEMBER",
				"priority": matrix["priority"],
				"force_use": False,
				"dont_move": False,
				"conditions": tuple(conditions),
			}
			validate_policy(class_name, policy, keys, known_skills, allowed_skills)
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

		for recipient in sorted(recipients, key=playable_order.index):
			covered_recipients.add(recipient)
			policy = {
				"key": f"{kebab(recipient)}-{matrix['key']}",
				"skill_id": matrix["skill_id"],
				"intent": matrix["intent"],
				"target": "SELF",
				"priority": matrix["priority"],
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
				),
			}
			validate_policy(class_name, policy, keys, known_skills, allowed_skills)
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
			validate_policy(class_name, policy, keys, known_skills, allowed_skills)
			policies.append(policy)

	if profile.get("require_complete_recipient_coverage", False):
		missing = set(playable_order) - covered_recipients
		if missing:
			raise CatalogError(f"{class_name} has no explicit support policy for exact recipients: {sorted(missing)}")
	return tuple(policies)


def validate_policy(class_name, policy, keys, known_skills, allowed_skills):
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
	root_attributes = (("classId", class_name), ("name", profile["profile_name"]), ("schemaVersion", 1))
	combat_attributes = (
		("targeting", combat["targeting"]),
		("acquisitionRange", combat["acquisition_range"]),
		("maximumChaseDistance", combat["maximum_chase_distance"]),
		("preferredRange", combat["preferred_range"]),
		("basicAttackPriority", combat["basic_attack_priority"]),
	)
	lines = [
		'<?xml version="1.0" encoding="UTF-8"?>',
		"<!-- Generated from scripts/bots/profile_catalog.py; edit the catalog, not this file. -->",
		f"<botClassProfile {xml_attributes(root_attributes)}>",
		f"\t<combat {xml_attributes(combat_attributes)} />",
		"\t<skills>",
	]
	for policy in policies:
		lines.extend(render_skill(policy))
	lines.extend(("\t</skills>", "</botClassProfile>", ""))
	return "\n".join(lines)


def compile_catalog():
	classes = load_classes()
	playable_order = tuple(entry["name"] for entry in classes)
	if playable_order != ALL_PLAYABLE_CLASSES:
		raise CatalogError("ALL_PLAYABLE_CLASSES must exactly match playable ClassId declaration order")
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

	classes_by_name = {entry["name"]: entry for entry in classes}
	direct_skills = load_class_skills()
	known_skills = load_known_skills()
	generated = {}
	total_policies = 0
	for class_name, profile in CATALOG.items():
		if class_name not in classes_by_name:
			raise CatalogError(f"Catalog profile names unknown exact ClassId {class_name}")
		allowed_skills = inherited_skill_ids(class_name, classes_by_name, direct_skills)
		policies = expanded_policies(class_name, profile, playable_order, known_skills, allowed_skills)
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
