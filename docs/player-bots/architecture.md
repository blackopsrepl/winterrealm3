# Native Player-Bot Architecture

## Purpose

Player bots are real, persisted `L2PcInstance` characters controlled by the
game server. They use the same lifecycle, action validation, world rules, and
database state as human-controlled characters.

This fork does not require upstream API compatibility. Once a shared internal
service replaces duplicated behavior, the duplicated path should be removed
instead of retained behind compatibility adapters.

## Current implementation

As of 2026-07-13, the native engine is wired into game-server startup and
shutdown. It restores enabled `character_bots` rows as clientless persisted
players, binds each player to one exact active-class profile, enters it through
the shared player lifecycle, schedules bounded decisions, and stores it through
the same character lifecycle during shutdown.

The production datapack currently contains 17 exact support-class profiles and
18,492 explicit policies. Every one of those profiles owns a distinct
89-recipient relationship matrix with all six ally and hostile priorities for
every playable Interlude `ClassId`. Prophet, Hierophant, Warcryer, Doomcryer,
Cleric, Oracle, Shillien Oracle, Bishop, and Cardinal have additional
class-specific compiler certification. Remaining healer/recharger profiles and
the song/dance repertoire model are present but still await the same
certification pass; offensive, control, movement, resource, and encounter
toolkits are later phases. An unsupported active class fails activation instead
of receiving another class's behavior.

## Architectural invariants

1. There is one authoritative player entity and one authoritative character
   schema.
2. Bot control is composed around `L2PcInstance`; bots do not subclass it.
3. A bot never fabricates client packets or owns a dummy `L2GameClient`.
4. Human packet handlers and bot actuators call the same semantic player
   commands.
5. Bots obey normal skill, item, adena, range, geodata, zone, death, and quest
   rules.
6. Tactical perception is limited to information legitimately available to the
   player.
7. Decision execution is serialized per bot and bounded globally.
8. Tactical ticks never write directly to the database.
9. Every decision is explainable through a bounded diagnostic trace.
10. Generated deployment artifacts are never the source of a bot change.
11. Behavior profiles bind to one exact active `ClassId`; there is no generic
    fighter, mage, melee, healer, buffer, or parent-class fallback.
12. Direct heals, CP heals, cures, resurrections, buffs, and recharge bind to
    the recipient's exact active `ClassId`; recipient roles and archetypes are
    not substitutes.

## Control flow

```text
Human packet -> packet decoder ------------------+
                                                  +-> player command -> L2PlayerAI -> world
Bot scheduler -> perception -> brain -> actuator-+
```

`L2PlayerAI` remains the low-level movement, follow, casting, and attack motor.
The bot brain supplies strategic and tactical intent above it.

## Runtime ownership

`BotRegistry` maps a bot character ID to a `BotRuntime`. The runtime owns only
controller state: immutable control metadata, its exact class profile, one
decision gate, perception and tactical services, lifecycle state, and recent
decision records. Inventory, skills, effects, quests, relationships, location,
combat state, and persistence remain owned by the player and existing game
systems.

Each runtime compiles one `BotProfileBrain`, owns one non-overlapping decision
gate, and retains only its 32 most recent immutable decision records. Those
records identify the exact profile, terminal status, chosen action, bounded
trace, pending actions, and sanitized failure detail without retaining live
world objects or exception graphs. A live active-class/profile mismatch faults
and pauses the runtime; it never continues on the old or a generic profile.

## Decision hierarchy

1. `BotPerceptionService` copies visible world state into an immutable,
   live-reference-free snapshot.
2. `BotProfileBrain` compiles one exact profile into indexed tactical actions.
3. `BotDecisionEngine` scores eligible actions and resolves prerequisites,
   alternatives, and continuers through the deterministic strategy kernel.
4. `BotLiveActionExecutor` re-resolves snapshot object IDs and invokes the
   shared `PlayerCommandService` surface.
5. `L2PlayerAI` and the existing game systems perform and validate movement,
   attacks, casts, resource consumption, reuse, inventory costs, and world
   effects.

The tactical engine has actions, triggers, a stable scored queue,
prerequisites, alternatives, continuers, strategies, contextual multipliers,
and bounded diagnostic traces. Long-horizon goals, coordinated party intent,
travel/progression planning, equipment management, and social behavior remain
planned layers; the documentation does not present them as implemented runtime
components.

Profile priority establishes explicit tactical bands. Within one band, direct
HP healing, CP healing, and Recharge add a bounded missing-resource score for
the already eligible exact recipient; HP and CP support also account for
whether that recipient is fighting and for up to five visible hostiles actively
targeting it. Resurrection adds the recipient level. Exact-class profiles use
separate priority bands to represent party duty, so raw missing HP cannot make
a disengaged skirmisher displace the party's active tank. Context then chooses
the most urgent member inside that authored duty band instead of falling back
to filename or class-enum order. Resurrection profiles separately author safe
and battlefield policies for every recipient class. Their complete score
bands remain below emergency healing, and class-duty differences exceed the
maximum Interlude level bonus so a higher-level damage dealer cannot displace a
critical healer or tank.

## Perception and support scope

Perception records position, range, resources, combat/casting/movement state,
the exact active class of every observed player, current targets, effect skill
IDs, and a validated set of abnormal statuses. The typed abnormality vocabulary
currently covers root, stun, magic silence, physical mute, poison, bleed,
paralysis, fear, sleep, confusion, weakness, and generic debuffs. These values
come from live character flags and active effect skill types; profiles do not
infer them from animation, class role, or a generic debuff flag.

Visibility is broader than support eligibility. Direct player support can
select only the caster and current party; owned summons use their explicit
summon selectors. Owners, clan members, and other visible players outside the
party remain observable for non-support decisions but cannot receive a heal,
cure, resurrection, buff, Recharge, song, dance, or chant through a support
policy. Both catalog compilation and runtime validation reject support skills
whose native L2 target type can expand beyond that boundary.

## Exact-class profiles

Lineage II classes that occupy similar party roles still have materially
different skill sets, target rules, buff scopes, resource constraints, and
combat rhythms. The active `ClassId` is therefore the behavior boundary.

Each profile declares exactly one playable `ClassId`. The profile owns its
skill priorities, target selection rules, buff or dance/song maintenance,
healing and recharge thresholds, resource reserves, equipment preferences,
summon behavior, solo rotation, party contribution, retreat conditions, and
progression decisions. A persisted `profile_name` may select a variant, but
that variant must declare the player's exact active class.

Profiles may compose shared perception predicates, actions, score functions,
and coordination primitives. Composition never supplies a behavioral
fallback: a missing or class-mismatched profile is an activation error. The
current runtime pins its exact class at construction and faults if that active
class changes. Transactional profile rebinding after a class or subclass switch
is planned but not yet implemented.

Support behavior forms an explicit caster-by-recipient matrix inside those
profiles. Each caster class owns separate direct heal, CP heal, cure,
resurrection, buff, and recharge rules for each exact recipient class it
supports. Candidate selection applies the entire recipient rule before
choosing the caster, its owned summon, or a current party member, so proximity,
resource order, or a generic role label cannot hide the correct target. Visible
outsiders—including configured owners and clan members outside the party—are
not support candidates. The compiler and runtime validator also inspect the
underlying L2 skill target type so a self-selected policy cannot conceal a
clan- or alliance-wide native effect. Group-wide support may use exact-class
party predicates to decide whether any member of a particular `ClassId` still
lacks an effect.
Songs and dances are one self-cast party-wide action, never a cast delivered to
one member. Exact party-class knowledge is used only to choose whether the
repertoire is useful and whether its effect family is missing; once cast, the
native skill affects the whole party. A group predicate may name an entire
blocking-effect family, and all family checks apply to the same exact member;
this preserves stack-equivalence behavior for chants. Clan-wide support is
disabled by profile validation. Consolidating the existing per-beneficiary
demand rows into a certified party-repertoire model is the next song/dance
profile phase.

The implementation and coverage gates are detailed in
[`class-profiles.md`](class-profiles.md).

## Scheduling

A central scheduler dispatches due runtimes onto the existing AI executors.
There is no thread, scheduled future, or coroutine per bot. Two activity tiers
control the cadence of loaded automatic bots:

- foreground for combat, nearby humans, and active parties;
- background for spawned but currently irrelevant players.

Dormant runtimes do not tick or gain offline rewards. The scheduler uses a fair
cursor, enforces global admission bounds, prevents overlapping decisions for a
single bot, and records queue lag and decision duration.

`WORLD` and `COMPANION` runtimes are admitted automatically. `SCENARIO` and
`MANUAL` runtimes are excluded from automatic admission; dedicated scenario
and operator advancement surfaces are still pending. Combat, casting,
attacking, movement, an active party member, or a human player within 3000
world units selects foreground cadence; other loaded automatic bots use
background cadence.

The production defaults are one 100 ms pulse, 250 ms foreground decisions,
2000 ms background decisions, at most 16 admissions per pulse, and at most four
in-flight bot decisions including work queued on the shared AI executor. These
limits are configured by `BotSchedulerPulseMillis`,
`BotForegroundDecisionMillis`, `BotBackgroundDecisionMillis`,
`BotMaxDispatchPerPulse`, and `BotMaxInFlightDecisions` in `bots.properties`.
Shutdown cancels the single pulse and waits for every admitted decision before
any bot player leaves the world or is stored.

## Persistence

Existing character, inventory, skill, quest, clan, and social tables remain
authoritative. `character_bots` stores only control metadata. Additional bot
tables may store explicit strategy overrides, schedules, or bounded
relationship memory, but never duplicate character state.

The subsystem is disabled by default in `bots.properties`. Enabling it does not
create characters, accounts, items, skills, or metadata: operators must first
install the `character_bots` schema and deliberately provision normal persisted
characters whose synthetic account names use the `bot:` prefix.

## Import policy

CMaNGOS playerbots is the reference for the strategy engine. L2Autobots is the
reference for Lineage II combat knowledge and data. Each source or data port is
performed deliberately into Java and the Interlude datapack, with provenance
recorded in `import-manifest.md`. Foreign dummy-client, player-subclass,
parallel-persistence, resource-injection, and per-bot-coroutine designs are not
imported.

## Authorship and attribution

Wholly fork-authored files identify the `l2j-interlude` contributors rather
than presenting new work as untouched L2J code. Files substantially derived
from or modified from upstream retain the relevant upstream copyright and
license notice and add an explicit fork copyright and modification notice.
Substantive external imports also require a provenance entry in
[`import-manifest.md`](import-manifest.md).
