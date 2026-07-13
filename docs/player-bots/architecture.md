# Native Player-Bot Architecture

## Purpose

Player bots are real, persisted `L2PcInstance` characters controlled by the
game server. They use the same lifecycle, action validation, world rules, and
database state as human-controlled characters.

This fork does not require upstream API compatibility. Once a shared internal
service replaces duplicated behavior, the duplicated path should be removed
instead of retained behind compatibility adapters.

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
controller state: configuration, event mailbox, blackboard, decision state,
random seed, scheduling state, and recent traces. Inventory, skills, effects,
quests, relationships, location, combat state, and persistence remain owned by
the player and existing game systems.

## Decision hierarchy

1. `BotPerception` builds an immutable, fair snapshot.
2. `BotReactionEngine` handles urgent preemption.
3. `BotDecisionEngine` scores strategy-provided tactical actions.
4. `BotGoalPlanner` maintains a long-horizon agenda.
5. `BotPartyCoordinator` supplies shared roles and party intent.
6. `BotActuator` invokes the shared player-command surface.

The tactical engine uses cached values, triggers, actions, prerequisites,
alternatives, continuers, strategies, and contextual score multipliers. The
long-horizon planner uses bounded, domain-specific plans for travel,
progression, equipment, quests, and social activity.

## Scheduling

A central scheduler dispatches due runtimes onto the existing AI executors.
There is no thread, scheduled future, or coroutine per bot. Activity tiers
control cadence:

- foreground for combat, nearby humans, and active parties;
- background for spawned but currently irrelevant players;
- dormant for unloaded bots, which do not tick or gain offline rewards;
- scenario/manual for explicit testing and operator control.

The scheduler coalesces redundant events, prevents overlapping decisions for a
single bot, and records queue lag and decision duration.

## Persistence

Existing character, inventory, skill, quest, clan, and social tables remain
authoritative. `character_bots` stores only control metadata. Additional bot
tables may store explicit strategy overrides, schedules, or bounded
relationship memory, but never duplicate character state.

## Import policy

CMaNGOS playerbots is the reference for the strategy engine. L2Autobots is the
reference for Lineage II combat knowledge and data. Each source or data port is
performed deliberately into Java and the Interlude datapack, with provenance
recorded in `import-manifest.md`. Foreign dummy-client, player-subclass,
parallel-persistence, resource-injection, and per-bot-coroutine designs are not
imported.
