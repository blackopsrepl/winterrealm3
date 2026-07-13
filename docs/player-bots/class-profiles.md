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

## Current production catalog

The generated catalog currently contains 17 exact-class profiles, 1,513
relationship rows, and 19,123 explicit policies. Every listed profile already
owns all 89 playable-class relationships and all six relationship dimensions;
the status below describes the stricter class-specific behavior certification,
not matrix completeness.

| Exact `ClassId` | Policies | Current status |
| --- | ---: | --- |
| `prophet` | 1,650 | Exact support contract certified |
| `hierophant` | 1,790 | Exact support contract certified |
| `warcryer` | 736 | Exact chant and party-recovery contract certified |
| `doomcryer` | 914 | Exact chant and party-recovery contract certified |
| `cleric` | 975 | Exact heal, cure, resurrection, and buff contract certified |
| `oracle` | 1,020 | Exact heal, cure, resurrection, Recharge, and buff contract certified |
| `shillienOracle` | 1,040 | Exact heal, cure, resurrection, Recharge, and buff contract certified |
| `bishop` | 1,604 | Exact specialist recovery and party-wide support contract certified |
| `cardinal` | 2,495 | Exact specialist recovery, Cleanse, and party-wide support contract certified |
| `elder` | 1,591 | Exact heal, Vitalize, cure, resurrection, Recharge, and buff contract certified |
| `evaSaint` | 1,169 | Matrix-complete profile; class-specific recertification pending |
| `shillenElder` | 1,081 | Matrix-complete profile; class-specific recertification pending |
| `shillienSaint` | 1,185 | Matrix-complete profile; class-specific recertification pending |
| `swordSinger` | 536 | Matrix-complete profile; party-wide repertoire recertification pending |
| `swordMuse` | 662 | Matrix-complete profile; party-wide repertoire recertification pending |
| `bladedancer` | 332 | Matrix-complete profile; party-wide repertoire recertification pending |
| `spectralDancer` | 343 | Matrix-complete profile; party-wide repertoire recertification pending |

“Certified” means the catalog compiler enforces that class's complete authored
skill inventory, all-class recipient coverage, exact relationship-priority
source, tactical band separation, party-wide policies, and forbidden skills.
Pending profiles remain exact-keyed and matrix-complete, but are not yet treated
as the final behavior contract for their class.

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

This abbreviated fragment shows the schema. Production files contain one
relationship row for every playable class; the omitted rows are not optional.

```xml
<botClassProfile classId="hierophant" name="world" schemaVersion="2">
	<combat targeting="PARTY_ASSIST" acquisitionRange="1200"
		maximumChaseDistance="1800" preferredRange="40"
		basicAttackPriority="0" />
	<relationships>
		<!-- 88 other playable ClassId rows omitted from this example. -->
		<class classId="duelist" allySupportPriority="2754"
			allyProtectionPriority="2730" hostileAttackPriority="3325"
			hostileThreatPriority="4305" hostileControlPriority="3780"
			hostileInterruptPriority="1320" />
	</relationships>
	<skills>
		<skill key="duelist-might" skillId="1068" intent="BUFF"
			target="PARTY_MEMBER" priority="9175"
			forceUse="false" dontMove="false">
			<when>
				<class subject="TARGET" classId="duelist" expected="true" />
				<effect subject="TARGET" skillId="1003" state="ABSENT" />
				<effect subject="TARGET" skillId="1007" state="ABSENT" />
				<effect subject="TARGET" skillId="1068" state="ABSENT" />
				<status subject="SELF" status="ALIVE" expected="true" />
				<resource subject="SELF" resource="MP" amount="PERCENT"
					comparison="ABOVE" value="30" />
				<distance subject="TARGET" comparison="AT_MOST" value="1200" />
			</when>
		</skill>
	</skills>
</botClassProfile>
```

Every skill has an exact-class-local key and at least one explicit condition.
Conditions within one `<when>` block use AND semantics. Multiple policies may
reference the same learned skill under distinct keys to express alternative
contexts without introducing an implicit OR or fallback. The supported typed
conditions cover resources, distance, effects, exact classes, basic actor
state, twelve abnormal-status families, filtered group counts, exact-class
group counts, and party effect-family coverage.

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

Songs and dances are not direct-recipient support. Each policy selects the
caster as the execution target and asks whether current-party composition
justifies one party-wide cast; the exact-class row is a repertoire demand
signal, not delivery to that member. The native skill then affects the entire
party regardless of which class made the policy eligible.

The compiler reads this fork's `ClassId.java`, `skill_trees.sql`, and skill XML
definitions. It rejects drift in the playable-class list, unknown recipients,
duplicate policy keys, skills outside the caster's inherited skill tree,
unknown blocking effects, incomplete declared recipient coverage, and overlap
or gaps in mutually exclusive support choices. It also requires every current
profile to own all 89 relationships with all six priority dimensions and
rejects a support skill when its native L2 target can expand beyond self, owned
summon, or current party. Class ancestry is used only to prove that a caster can
legally learn a referenced skill; it never selects runtime behavior from a
parent profile, `isMage`, race, or a role.

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

Both classes also own two direct-heal lanes, observed-poison Cure Poison, safe
and battlefield Resurrection, and the inherited party Group Heal trigger for
all 89 recipient classes. Those policies use Prophet- or Hierophant-owned
ally-protection priorities; the advanced class does not inherit the Prophet's
generated decisions at runtime.

Cleric is independently authored from its own second-class tree. Each of the 89
exact recipients has an emergency Battle Heal rule and an efficient Heal rule
using the same non-overlapping tactical bands and class-duty separation as the
advanced healers. Group Heal requires three actual party members below 45% HP;
visible outsiders never contribute to that count. Shield, Mental Shield, Wind
Walk, Might, Focus, Acumen, Concentration, Berserker Spirit, and
Regeneration each retain their exact recipient matrices and stack-equivalent
blocking families. Cleric does not inherit Recharge or any higher-class heal.
Cure Poison now has one observed-poison policy for every exact recipient and
preserves a 15% caster MP reserve; it is never inferred from a generic healer
fallback.

Cleric independently owns its Resurrection matrix for all 89 exact recipient
classes in both safe and battlefield contexts. Safe-state policies preserve
25% MP and battlefield policies preserve 35% MP during the six-second combat
cast. Their authored duty band outranks recipient level and distance: a Phoenix
Knight is recovered before a closer, higher-level Abyss Walker, while Cardinal
outranks the tank because restoring the main healer restores party recovery.
Emergency Battle Heal and party-wide Group Heal remain above every resurrection
score, so Cleric never abandons a living tank in immediate danger to begin a
resurrection. These decisions belong to Cleric; Oracle, Shillien Oracle, and
the advanced healer profiles do not inherit them implicitly.

Oracle is another independently compiled second-class profile. It owns the same
two locally learned direct-heal skills and party-only Group Heal trigger, then
adds Oracle's exact Recharge matrix, Resist Poison, Agility, and Decrease
Weight decisions. Recharge retains the local six-class prohibition and the
three exact MP thresholds. Decrease Weight is limited to the seven explicitly
named Dwarven gathering and crafting progressions instead of being broadcast
to every party member. An Abyss Walker can therefore request Agility, a Mystic
Muse requests Concentration, and a Fortune Seeker requests weight support;
none of those decisions comes from an `elven healer` profile or role test.

Oracle separately authors all 89 safe-state and all 89 battlefield Resurrection
recipients. Its safe and combat reserves rise to 35% and 45% MP so a long
Resurrection cast does not casually consume the resource pool used for Recharge
and healing. Dreadnought, Titan, Maestro, and every other exact class remain
materialized in both contexts; Oracle expresses lower duty through priority,
never through a missing recipient row. Cardinal has the highest exact duty,
then Eva's Saint, and both remain above every tank by more than the full
level-80 context bonus. This is an Oracle-owned policy, not behavior inherited
from Cleric or supplied by an `Elven healer` category.

Shillien Oracle independently owns the same level-appropriate recovery skills
and Recharge eligibility, but its support matrix is not copied from Oracle.
Empower and Concentration name exact magic recipients; Might and Focus name
exact physical recipients; Vampiric Rage names only exact melee-sustain
recipients. A Duelist receives the physical and vampiric set, a Mystic Muse the
magic set, and a Hawkeye the physical set without the melee-only effect. Shield,
Mental Shield, and Wind Walk provide explicit all-class coverage.
Agility, Decrease Weight, Death Whisper, Guidance, and Wild Magic are absent
because this exact class does not learn them at this progression stage.

Elder now owns a certified recovery and support contract derived from Elder's
own relationship matrix. Each exact recipient has Battle Heal, Major Heal, and
Greater Heal intervention lanes plus separate injured-and-poisoned and
injured-and-bleeding Vitalize decisions. An emergency Phoenix Knight remains
above the maximum health and combat-pressure score of a mobile Abyss Walker.
Cure Poison handles poison when a combined heal is unnecessary; Vitalize also
has a bleed-cure lane that remains eligible when the target is otherwise
healthy. The class independently owns all 89 safe and battlefield Resurrection
decisions and retains one party-only Group Heal trigger.

Elder's 83 eligible Recharge recipients are partitioned into the explicit 60%,
35%, and 20% target-MP thresholds. Every policy preserves 45% caster MP and
adds that recipient's Elder-owned ally-support priority; Oracle, Elder, Eva's
Saint, Shillien Oracle, Shillien Elder, and Shillien Saint remain ineligible by
the local skill definition. The buff catalog similarly uses Elder's own
ally-support priorities for Resist Shock, Resist Poison, Shield, Mental Shield,
Wind Walk, Clarity, and the exact physical, casting, evasion, magic-critical,
shield-block, and Dwarven weight-support recipients. Holy Weapon, Kiss of Eva,
Unholy Resistance, travel, and hostile-control skills remain gated for later
encounter, travel, and combat phases rather than being cast unconditionally.

Eva's Saint has a separate exact profile rather than inheriting Elder behavior
at runtime. Arcane Protection and Divine Protection have explicit all-class
coverage, while Prophecy of Water names only the exact caster, healer,
summoner, and magic-damage recipients selected for that effect. Duelist and
Doomcryer therefore do not receive Prophecy of Water merely because another
system might label them as damage or support roles. Its current direct-heal and
Recharge rows still use the provisional shared authoring, and cure and
resurrection coverage is not yet present. Eva's Saint therefore remains
explicitly pending until its own relationship priorities and inherited Elder
toolkit are independently regenerated and certified.

Shillien Elder has its own 89-way generated profile. Its buff matrix
distinguishes magic recipients that need Empower and
Concentration, physical recipients that need Might, Focus, Death Whisper, and
Guidance, and melee recipients that can actually benefit from Vampiric Rage;
an archer such as Hawkeye therefore does not receive the melee-only effect.
Its current healing matrix has only Battle Heal and Greater Heal lanes because
this class does not learn Major Heal, and its Recharge matrix uses the same
local 83-recipient eligibility partition. Caster-owned priority certification
and inherited cure/resurrection coverage remain pending; the compiler may not
borrow a missing skill or the Eva's Saint profile in the meantime.

Shillien Saint owns the corresponding third-class generated profile rather
than extending Shillien Elder at runtime. Arcane Protection has 89 exact recipient
rules, while Prophecy of Wind is limited to the explicitly named dagger and
archer progressions for which it is preferred. It does not lock a Duelist or
Phoenix Knight out of a later Prophecy of Fire or Chant of Victory. Mass
Recharge is deliberately absent for now: the local skill requires level-three
Spell Force and a Magic Symbol, and it will be added only with explicit party
force coordination rather than as a cast-failure loop. Its remaining inherited
recovery kit shares Shillien Elder's pending recertification boundary.

Bishop is a separate Human healer profile, not another instance of the Elven
or Shillien matrices. Each exact recipient has six direct recovery decisions:
Greater Battle Heal, Major Heal, Greater Heal, safe-state Restore Life, and
separate poison/bleed Vitalize policies. Celestial Shield adds an exact emergency
protection lane that requires the selected member to be fighting, critically
injured, and not already invincible. Cure Poison, Purify for bleed, Purify for
paralysis, and both safe and battlefield Resurrection are also complete
89-recipient matrices derived from Bishop's own ally-protection priorities.

Benediction, Major Group Heal, Greater Group Heal, base Group Heal, and the
emergency Body of Avatar policy occupy explicit party-recovery bands. Prayer
and maintenance Body of Avatar inspect current-party effect-family coverage and
perform one self-cast party-wide action. The profile retains Bishop's own exact
targeted buff decisions, but it has no Recharge policy, does not blanket-cast
encounter-only resistances, and rejects Mass Resurrection because its native
target can expand beyond the current party.

Cardinal independently materializes that Human healing foundation and adds
only Cardinal capabilities. It independently regenerates all six inherited
direct-heal decisions, the three cheaper inherited cure decisions, both
resurrection lanes, every targeted buff, Prayer, and both Body of Avatar
contexts from Cardinal's own relationship matrix; it does not reuse Bishop's
generated behavior at runtime.

Cleanse adds nine exact-recipient decisions for confusion, generic debuffs,
fear, magic silence, physical mute, root, sleep, stun, and weakness. Poison uses
Cure Poison, while bleed and paralysis use Purify, so Cleanse spends Einhasad
Holy Water only when the inherited cheaper skill cannot remove the observed
ailment. Together those paths cover all twelve typed abnormality families for
all 89 recipients. Balance Life is one self-cast party-wide decision requiring
at least three living current-party members and at least one member below 35%
HP. Divine Protection is exact-recipient support for all 89 classes. Miracle is
forbidden because the local `TARGET_ALLY` skill can heal alliance members
outside the party; Cardinal also has no Recharge or Mass Resurrection policy.

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

The exact-class rows above are demand signals only. Whichever row wins still
causes one `SELF` cast whose native effect applies to the entire party; there is
no per-member song delivery. Sword Singer, Sword Muse, Bladedancer, and
Spectral Dancer remain marked pending until these demand rows are consolidated
and certified as one party-repertoire choice per song or dance.

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
Life is a self-cast party-wide recovery decision: it requires combat, at least
two current-party members below 75% HP, a 45% caster MP reserve, and a named
exact beneficiary missing the effect. Its priority uses Warcryer's own
ally-protection relationship for that class.

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

The production compiler enumerates every non-dummy value in `ClassId.values()`.
For each current catalog profile it requires all 89 playable relationship rows
in declaration order, all six relationship priorities, and a matrix distinct
from every other profile. The runtime separately requires an exact
`(active ClassId, profile_name)` key before a bot can load. The gates reject:

- a missing playable relationship row in any current profile;
- a profile registered for more than one class ID;
- a profile whose declared class does not match its registry key;
- a skill unavailable to the declared class and progression level;
- a referenced skill, item, or strategy primitive that does not exist;
- a generic, parent-class, role, race, or mage/fighter fallback;
- a direct heal, CP heal, cure, resurrection, buff, or recharge policy without
  exactly one positive target `ClassId` condition;
- a supported resurrection caster missing either the safe-state or battlefield
  policy for any playable recipient `ClassId`;
- a support policy whose native L2 target can expand beyond self, owned summon,
  or current party;
- a named metadata profile that does not match the active class.

The catalog does not yet claim one behavior profile for all 89 possible caster
classes; it currently contains the 17 profiles listed above. During incremental
development, only those exact active classes may be activated. Other enabled
bots fail clearly and never receive a generic, parent, race, or role fallback.
Full caster coverage remains a completion gate for the project.

## Runtime class changes

`BotRuntime` pins the exact active class and profile at construction. Every
scheduler admission and decision verifies that binding. If the character's
active class changes underneath the runtime, the runtime faults and stops
making decisions; it never continues with the old, parent, or generic profile.
A transactional hot-rebind that quiesces the decision gate, resolves the new
exact profile, replaces class-specific tactical state, and resumes scheduling
is planned but not yet implemented.

## Next implementation sequence

1. Recertify Eva's Saint, Shillien Elder, and Shillien Saint one exact caster at
   a time, including inherited cures, resurrection, Recharge, and each caster's
   own 89-class duty priorities.
2. Consolidate Sword Singer/Muse and Bladedancer/Spectral Dancer demand rows
   into certified party-wide repertoire choices while preserving the fact that
   each song or dance is one cast affecting the whole party.
3. Add exact offensive, hostile-control, interrupt, movement, resource,
   consumable, summon, and encounter toolkits without introducing role
   fallbacks.
4. Add transactional class/subclass profile rebinding and deterministic live
   mixed-party scenario coverage.
5. Expand exact caster profiles until every playable Interlude class can be
   activated without fallback.
