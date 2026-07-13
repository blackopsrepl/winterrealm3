# Player-Bot Import Manifest

This file records substantive source and data imports into the native
player-bot subsystem. A project listed as a design reference in `NOTICE.md` is
not considered imported until an entry appears here.

## Required fields

Each import entry must include:

- source project and exact revision;
- original file or directory;
- destination file or directory;
- source license and retained notices;
- whether the result is copied, translated, adapted, or data-normalized;
- behavioral changes and deliberately omitted behavior;
- validation covering the imported behavior.

## Imports

### Deterministic strategy kernel

- Source project: CMaNGOS playerbots.
- Exact revision: `a5b202b146338ac280551ce5e29158149c5e3d37`.
- Original paths: `playerbot/strategy/Action.{h,cpp}`,
  `Engine.{h,cpp}`, `Event.h`, `Multiplier.h`, `Queue.{h,cpp}`, `Strategy.h`,
  and `Trigger.h`.
- Destination: `official/l2j-server-game/src/main/java/net/sf/l2j/gameserver/bot/strategy/`.
- Source license: GNU GPL version 2 or later. Original MaNGOS copyright and
  adaptation notices are retained in translated files; this GPLv3 project uses
  the GPLv3 option permitted by the source license.
- Transformation: translated and substantially adapted from C++ to typed Java.
  The trigger, scored action queue, multiplier, prerequisite, alternative,
  continuer, and one-successful-action-per-tick model is retained.
- Behavioral changes: queue ordering has a stable action-name tie break;
  expiration uses deterministic scheduler ticks instead of wall time; action
  links and duplicates are validated; iterations and traces are bounded;
  errors are surfaced as typed outcomes; and the queue enforces monotonic,
  serialized ticks.
- Deliberately omitted: World of Warcraft objects and packets, raw pointers,
  global named-object factories, master/client messaging, wall-clock trigger
  jitter, per-state engine duplication, and CMaNGOS logging/configuration.
- Validation: `BotDecisionEngineTest` covers scoring, stable ties, queue
  persistence, prerequisites, alternatives, failures, multipliers, trigger
  de-duplication, error surfacing, trace bounds, and link validation.
