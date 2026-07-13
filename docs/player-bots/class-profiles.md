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
- self, current-party, owned-summon, enemy, corpse, and ground-target selection;
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
caster's active `ClassId`, while every direct heal, CP heal, cure,
resurrection, buff, or recharge policy also identifies exactly one recipient
active `ClassId` unless its target is the caster or a summon. A Hierophant
therefore owns separate policies for buffing a Duelist, Mystic Muse, Cardinal,
and every other supported recipient class; a generic physical, magical,
healer, or tank recipient category is not a valid substitute.

Support candidates are limited to the caster, its owned summon, and its current
party. The party set includes the bot itself when it is solo. A visible player,
configured owner, or clan member outside that party is never eligible merely
because perception can observe it. `CLAN_BUFF` is rejected under this contract;
Overlord and Dominator clan-wide support remains disabled unless the server's
support boundary is changed explicitly.

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

`PARTY_MEMBER` evaluates the complete policy against each deterministically
ordered party candidate before selecting a target. This lets the engine skip a
nearer member with the wrong exact class or an already present effect and
continue to the correct recipient. Clan and nearby actors may remain available
to non-support perception, but the validator rejects every support intent that
uses their selectors. Exact-class group conditions can separately count party
members of one `ClassId` with a present or missing effect family for group-wide
support decisions. Every skill ID in that family is evaluated on the same
member: an `ABSENT` condition qualifies only a member that has none of the
blocking effects, while `PRESENT` requires all of them. This prevents group
chants from retrying or overwriting a stack-equivalent direct buff just because
the self-cast group skill's own ID is absent.

The loader rejects unknown elements and attributes, duplicate keys, dummy or
unknown classes, invalid numeric domains, malformed booleans, filename/key
mismatches, unsupported schema versions, non-regular files, DOCTYPEs, and
external entities. Runtime validation also rejects support selectors outside
self, owned summon, or current party, rejects the clan-buff intent, and rejects
native skill target types that expand to nearby allies or clan members. All
combat ranges and basic-attack priorities are mandatory class data. A class
that should not perform normal combat must explicitly use `targeting="NONE"`
with zero ranges and priority.

## Production authoring

Large caster-by-recipient matrices are authored in
`scripts/bots/profile_catalog.py` and deterministically compiled into the
datapack XML files. The catalog names every caster and recipient by exact
`ClassId`. Its tuples are authoring deduplication only: each generated rule
for direct support contains one positive exact-recipient condition. Self-cast
group effects instead contain one exact-class group count requiring a named
beneficiary to be present and missing that effect. No tuple or category is
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

Cleric is independently authored from its own second-class tree. Each of the 89
exact recipients has an emergency Battle Heal rule and an efficient Heal rule
using the same non-overlapping tactical bands and class-duty separation as the
advanced healers. Group Heal requires three actual party members below 45% HP;
visible outsiders never contribute to that count. Shield, Mental Shield, Wind
Walk, Resist Fire, Might, Focus, Acumen, Concentration, Berserker Spirit, and
Regeneration each retain their exact recipient matrices and stack-equivalent
blocking families. Cleric does not inherit Recharge or any higher-class heal.
Cure Poison is withheld until poison state is represented in typed perception,
and Resurrection is withheld until a distinct exact-class resurrection-duty
matrix is authored; neither skill is allowed to fire from a generic healer
fallback.

Oracle is another independently compiled second-class profile. It owns the same
two locally learned direct-heal skills and party-only Group Heal trigger, then
adds Oracle's exact Recharge matrix, Resist Poison, Agility, and Decrease
Weight decisions. Recharge retains the local six-class prohibition and the
three exact MP thresholds. Decrease Weight is limited to the seven explicitly
named Dwarven gathering and crafting progressions instead of being broadcast
to every party member. An Abyss Walker can therefore request Agility, a Mystic
Muse requests Concentration, and a Fortune Seeker requests weight support;
none of those decisions comes from an `elven healer` profile or role test.

Shillien Oracle independently owns the same level-appropriate recovery skills
and Recharge eligibility, but its support matrix is not copied from Oracle.
Empower and Concentration name exact magic recipients; Might and Focus name
exact physical recipients; Vampiric Rage names only exact melee-sustain
recipients. A Duelist receives the physical and vampiric set, a Mystic Muse the
magic set, and a Hawkeye the physical set without the melee-only effect. Resist
Wind, Shield, Mental Shield, and Wind Walk provide explicit all-class coverage.
Agility, Decrease Weight, Death Whisper, Guidance, and Wild Magic are absent
because this exact class does not learn them at this progression stage.

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

Their direct healing layer is another independent matrix: each caster emits
three policies for each of the 89 exact recipient classes. Every recipient owns
an explicit duty priority plus separate Battle Heal, Major Heal, and Greater
Heal HP thresholds. The three skill families occupy non-overlapping emergency,
major-recovery, and efficient-recovery bands, so a genuinely critical member
can preempt a routine tank top-up. Inside the same band, however, the minimum
primary-tank priority remains above the maximum skirmisher priority even after
the runtime's full 235-point health and combat-pressure adjustment. An exposed
Phoenix Knight is therefore not abandoned for a disengaging Abyss Walker just
because the latter has a lower HP percentage. All policies require a living
target within 1200 units and preserve 5%, 15%, or 25% caster MP according to
the selected healing skill.

Shillien Elder is authored as a fifth independent caster profile. Its 89-way
buff matrix distinguishes magic recipients that need Empower and
Concentration, physical recipients that need Might, Focus, Death Whisper, and
Guidance, and melee recipients that can actually benefit from Vampiric Rage;
an archer such as Hawkeye therefore does not receive the melee-only effect.
Its Recharge matrix uses the same local skill eligibility partition but is
compiled into Shillien Elder-owned rules. Its healing matrix intentionally has
only Battle Heal and Greater Heal lanes because this class does not learn Major
Heal; the compiler may not borrow that skill or the Eva's Saint profile.

Shillien Saint owns the corresponding third-class profile rather than
extending Shillien Elder at runtime. Arcane Protection has 89 exact recipient
rules, while Prophecy of Wind is limited to the explicitly named dagger and
archer progressions for which it is preferred. It does not lock a Duelist or
Phoenix Knight out of a later Prophecy of Fire or Chant of Victory. Mass
Recharge is deliberately absent for now: the local skill requires level-three
Spell Force and a Magic Symbol, and it will be added only with explicit party
force coordination rather than as a cast-failure loop.

Bishop is a separate Human healer profile, not another instance of the Elven
or Shillien matrices. Every exact recipient gets Greater Battle Heal, Major
Heal, and Greater Heal duty rules; the weaker base Battle Heal is intentionally
absent. Celestial Shield adds a fourth exact-recipient emergency lane that
requires the selected member to be fighting, critically injured, and not
already invincible. Benediction, Major Group Heal, Greater Group Heal, and the
base Group Heal occupy explicit non-overlapping party-recovery bands with
different injured-member counts, HP thresholds, and MP reserves. The profile
also retains the Bishop's own exact-recipient inherited buffs, but it has no
Recharge policy and cannot borrow one from either elven healer line.

Cardinal independently materializes that Human healing foundation and adds
only Cardinal capabilities. Cleanse has four exact-recipient policies per
class—stun, magic silence, physical mute, and root—with the same class-duty
separation used for life-saving heals. Balance Life requires at least three
living nearby party members and at least one member below 35% HP. Divine
Protection is present for all 89 exact recipients. Miracle is intentionally
absent until its level-two Spell Force and Magic Symbol requirements have an
explicit coordinated-party plan; Cardinal never acquires Recharge merely
because other healer classes have it.

Sword Singer owns a separate group-support matrix. Each song policy targets the
caster, but it becomes eligible only when a named exact `ClassId` is alive
within 1000 units and lacks that song. Earth, Warding, Vitality, Wind, and the
low-priority Life song cover all 89 exact classes. Hunter is limited to the
explicit physical-weapon list, Water to the explicit evasion list, and
Vengeance to the explicit shield-bearing front line. The singer therefore does
not spend MP on Hunter merely because a Mystic Muse is present, nor on
Vengeance merely because an Abyss Walker is nearby. Invocation, Flame Guard,
and Storm Guard remain absent until enemy damage-type perception can justify
their elemental cost; the profile preserves a 40% MP reserve rather than
blindly casting a generic full song suite.

Sword Muse materializes those inherited decisions into its own exact-class
profile and independently adds its third-class songs. Renewal and Meditation
name the exact spell-casting progressions that benefit from magic reuse, MP
regeneration, and MP-cost support. Champion names the exact physical-skill
progressions instead; it is not offered to a Hierophant merely because that
class can carry and swing a weapon. Song of Silence remains absent until the
combat layer can select and value hostile control targets. The shared authoring
tuple only prevents catalog duplication—the generated Sword Muse XML owns all
of its policies and the runtime performs no Sword Singer inheritance.

Bladedancer has its own exact beneficiary matrix rather than a full-suite
macro. Warrior, Fire, Fury, and Inspiration are triggered only by the explicit
physical-skill progressions. Mystic is triggered by the explicit magic-power
recipients, Concentration by the explicit casting progressions, and Vampire by
the explicit melee-sustain recipients. Thus a Hawkeye can request the physical
offense suite without the melee-only Vampire effect, while a Mystic Muse asks
for Mystic and Concentration without causing physical dances. Light and the
elemental guard dances remain absent until hostile creature and damage-type
perception exists. Protection is also withheld because the local Interlude
skill data currently encodes an elemental resistance effect rather than a
general defensive effect; the bot does not spend MP based on an assumed
upstream meaning that this fork does not implement.

Spectral Dancer independently materializes the Bladedancer matrix and adds
Siren's Dance only for the exact magic-damage progressions selected for magic
critical support. Mystic Muse and Storm Screamer can request Siren, while
Cardinal and Elemental Master retain their applicable Mystic and Concentration
dances without triggering it. Dance of Shadows is withheld until travel and
stealth coordination can accept its movement penalty; Dance of Medusa is
withheld until hostile area-control targeting can account for nearby allies and
follow-up damage. Neither skill is treated as a routine party buff simply
because it belongs to Spectral Dancer.

Warcryer owns an exact party-chant matrix rather than broadcasting every chant
whenever any party exists. War Chant and Earth Chant use the same checked
89-class offensive/defensive partition as their mutually exclusive Greater
Might family. Battle, Fury, Rage, Predator, and Eagle name physical-skill
beneficiaries; Flame names casting beneficiaries; Evasion names evasion
beneficiaries; Revenge names the shield-bearing front line; and Vampire names
melee-sustain beneficiaries. Fire and Shielding provide exact all-class
coverage. Every rule preserves 30% caster MP and treats the corresponding
single-target, Pa'agrio, and chant IDs as one blocking family, so an existing
Might, Acumen, Haste, or equivalent effect prevents a redundant chant. Chant of
Life remains absent until the group predicate can bind HP pressure and missing
effect state to the same exact member instead of maintaining a costly heal over
time on healthy parties.

Doomcryer materializes that complete Warcryer matrix in its own profile and
adds a checked third-class partition. The 31 explicitly named magic-oriented
classes receive Magnus' Chant; the remaining 58 exact classes receive Chant of
Victory. Every playable class receives exactly one of those mutually exclusive
rules, never both, and their shared blocking family includes Prophecy of Water,
Fire, and Wind plus both Pa'agrio/chant alternatives. Both high-cost chants
require more than 45% caster MP and still pass normal Spirit Ore readiness
checks. Chant of Spirit has independent all-class coverage, its actual 900-unit
radius, and a 40% reserve. The catalog compiler parses the local MySQL-escaped
`Magnus\' Chant` skill-tree row and proves the skill belongs to Doomcryer's
inherited tree rather than bypassing the ownership gate.

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
- a direct heal, CP heal, cure, resurrection, buff, or recharge policy without
  exactly one positive target `ClassId` condition;
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
