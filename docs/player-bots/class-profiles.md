# Exact-Class Bot Profiles

## Contract

A bot behavior profile belongs to exactly one playable `ClassId`. Similar
classes may reuse tactical building blocks, but they may not share a terminal
generic profile. In particular, there is no `melee`, `healer`, `buffer`,
`mage`, or inherited parent-class fallback.

The registry resolves the player's current class ID by exact key. A named
variant from `character_bots.profile_name` is valid only when the profile
declares that same class ID. Resolution failure or a class mismatch prevents
the bot from becoming active and reports the character, class ID, and requested
profile.

## Required profile ownership

Every exact-class profile owns these decisions even when its implementation
composes shared primitives:

- learned skill IDs, intent, target type, prerequisites, priority, reuse, and
  alternatives;
- self, party, clan, summon, enemy, corpse, and ground-target selection;
- HP, MP, CP, item, shot, and consumable reserves;
- range, approach, kiting, weapon, and basic-attack policy;
- buff, debuff, toggle, chant, song, dance, heal, cure, recharge, resurrection,
  spoil, sweep, summon, craft, and stance rules that apply to that class;
- solo rotation and party contribution;
- party role offers and coordination signals;
- retreat, rest, death recovery, and regroup thresholds;
- equipment evaluation and class-change progression goals.

Party roles remain dynamic coordination facts, not profile identities. For
example, a Cardinal and Eva's Saint may both offer healing, but their profiles
must independently decide healing coverage, cures, resurrection, recharge,
buff upkeep, MP reserves, and offensive downtime.

Support policy has two exact-class dimensions. The profile identifies the
caster's active `ClassId`, while every single-target buff or recharge policy
also identifies exactly one recipient active `ClassId`. A Hierophant therefore
owns separate policies for buffing a Duelist, Mystic Muse, Cardinal, and every
other supported recipient class; a generic physical, magical, healer, or tank
recipient category is not a valid substitute.

## On-disk contract

Production policies live under `data/bots/profiles/classes` in the datapack.
Each XML file contains exactly one profile and repeats its exact key in the
filename: `<ClassId>--<profile-name>.xml`. A file may not contain multiple
classes or a generic role declaration.

```xml
<botClassProfile classId="hierophant" name="party" schemaVersion="1">
	<combat targeting="NONE" acquisitionRange="0"
		maximumChaseDistance="0" preferredRange="0"
		basicAttackPriority="0" />
	<skills>
		<skill key="duelist-might" skillId="1068" intent="BUFF"
			target="PARTY_MEMBER" priority="900"
			forceUse="false" dontMove="false">
			<when>
				<class subject="TARGET" classId="duelist" expected="true" />
				<effect subject="TARGET" skillId="1068" state="ABSENT" />
			</when>
		</skill>
	</skills>
</botClassProfile>
```

Every skill has an exact-class-local key and at least one explicit condition.
Conditions within one `<when>` block use AND semantics. Multiple policies may
reference the same learned skill under distinct keys to express alternative
contexts without introducing an implicit OR or fallback. The supported typed
conditions cover resources, distance, effects, actor state, and filtered
party, clan, or nearby-hostile counts.

`PARTY_MEMBER` and `CLAN_MEMBER` selectors evaluate the complete policy against
each deterministically ordered candidate before selecting a target. This lets
the engine skip a nearer member with the wrong exact class or an already
present effect and continue to the correct recipient. Exact-class group
conditions can separately count members of one `ClassId` with a present or
missing effect for group-wide support decisions.

The loader rejects unknown elements and attributes, duplicate keys, dummy or
unknown classes, invalid numeric domains, malformed booleans, filename/key
mismatches, unsupported schema versions, non-regular files, DOCTYPEs, and
external entities. All combat ranges and basic-attack priorities are mandatory
class data. A class that should not perform normal combat must explicitly use
`targeting="NONE"` with zero ranges and priority.

## Production authoring

Large caster-by-recipient matrices are authored in
`scripts/bots/profile_catalog.py` and deterministically compiled into the
datapack XML files. The catalog names every caster and recipient by exact
`ClassId`. Its tuples are authoring deduplication only: each generated rule
contains one positive exact-recipient condition, and no tuple or category is
available to the runtime as a fallback.

The compiler reads this fork's `ClassId.java`, `skill_trees.sql`, and skill XML
definitions. It rejects drift in the playable-class list, unknown recipients,
duplicate policy keys, skills outside the caster's inherited skill tree,
unknown blocking effects, incomplete declared recipient coverage, and overlap
or gaps in mutually exclusive support choices. It never selects behavior from
class ancestry, `isMage`, race, or a role.

Regenerate after an intentional catalog edit:

```bash
python3 scripts/bots/compile_class_profiles.py
```

CI and review use the non-mutating drift check:

```bash
python3 scripts/bots/compile_class_profiles.py --check
```

The Prophet and Hierophant `world` policies each use separate recipient rules
for all 89 playable Interlude classes. For example, Duelist receives the
physical damage set and Greater Might; Mystic Muse receives casting support
and Greater Shield; Phoenix Knight receives physical support, shield-block
support, Regeneration, and Greater Shield. The profiles remain separate:
Prophet never receives Hierophant-only Elemental Protection or Prophecy of
Fire rules. Stack-equivalent chants, Pa'agrio buffs, and mutually exclusive
prophecies are explicit blocking effects so cooperating support bots do not
continually overwrite or retry an already satisfied buff family.

Elder is a third independent exact-caster profile. Its recipient matrix assigns
Might, Agility, casting concentration, Wild Magic, Clarity, shield-block
support, Regeneration, and Resist Shock independently. It does not gain Eva's
Saint-only prophecy or protection skills. Healing, resurrection, cure, and
recharge matrices are separate policy layers because their thresholds and
eligibility depend on live resources and debuffs as well as exact class.

Eva's Saint extends that exact profile family without inheriting behavior at
runtime. Arcane Protection and Divine Protection have explicit all-class
coverage, while Prophecy of Water names only the exact caster, healer,
summoner, and magic-damage recipients selected for that effect. Duelist and
Doomcryer therefore do not receive Prophecy of Water merely because another
system might label them as damage or support roles.

Elder and Eva's Saint also own independent exact-recipient Recharge matrices.
The local Interlude skill definition forbids Oracle, Elder, Eva's Saint,
Shillien Oracle, Shillien Elder, and Shillien Saint recipients; those six exact
classes do not receive a generated Recharge rule. The remaining 83 classes are
explicitly partitioned into 60%, 35%, and 20% target-MP thresholds. Every rule
also preserves a caster reserve above 45% MP and requires the exact target to
be alive and within 1200 units. No `mage`, `skill user`, or low-priority role is
evaluated by the runtime—the threshold is already materialized in that exact
recipient's policy.

## Coverage rules

The production coverage gate enumerates `ClassId.values()` and requires one or
more validated profiles for every entry whose race is non-null. Dummy enum
entries are excluded. The gate rejects:

- a missing playable class;
- a profile registered for more than one class ID;
- a profile whose declared class does not match its registry key;
- a skill unavailable to the declared class and progression level;
- a referenced skill, item, or strategy primitive that does not exist;
- a generic, parent-class, role, race, or mage/fighter fallback;
- a single-target buff or recharge policy without exactly one positive target
  `ClassId` condition;
- a named metadata profile that does not match the active class.

During incremental development, only explicitly supported classes may be
activated. Unsupported enabled bots fail clearly; they never receive a generic
behavior. Completion requires the production coverage gate to pass for the
entire playable Interlude class set.

## Runtime class changes

Class and subclass changes are transactional controller events:

1. stop scheduling new decisions for the runtime;
2. let any in-flight decision finish without overlap;
3. resolve and validate the exact new class profile;
4. discard class-specific cached skill and rotation state;
5. retain class-independent memory such as relationships and long-term goals;
6. publish a trace event identifying the old class, new class, and profile;
7. resume scheduling only after the new profile is installed.

Failure leaves the bot inactive and visible to diagnostics. It does not fall
back to the base class, parent class, or a generic role.

## Implementation order

1. Define the immutable profile contract and exact-key registry.
2. Add validation for profile identity, duplicates, skills, items, and strategy
   references.
3. Bind `BotRuntime` to the active class profile and enforce binding during
   startup and subclass changes.
4. Create explicit data files for every playable class, beginning with focused
   end-to-end scenarios but never using those first classes as fallbacks.
5. Add a manifest test that proves complete playable-class coverage.
6. Add deterministic solo, mixed-party, and same-role/different-class scenarios
   whose traces prove that each class selected its own policy.
