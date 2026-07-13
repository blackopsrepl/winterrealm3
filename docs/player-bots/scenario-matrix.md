# Player-Bot Scenario Matrix

Each scenario must be deterministic under a recorded seed and must expose its
decision trace on failure.

| Area | Minimum scenario | Required invariant |
| --- | --- | --- |
| Lifecycle | Load, spawn, store, despawn, restart, and resume | No client object; normal character persistence |
| Identity | Human character selection and login-server resync with bots online | Bot accounts never enter human session state |
| Movement | Follow, approach, stop, blocked route, and stuck recovery | Normal geodata and movement validation |
| Combat | Acquire, approach, attack, use skill, disengage, and rest | Normal targeting, range, reuse, and resource use |
| Death | Die, release, return, regroup, and resume | Normal death penalties and teleport mechanisms |
| Equipment | Evaluate, obtain, equip, and replace an item | No item or adena injection |
| Consumables | Shots, potions, and return scrolls | Items are owned and consumed normally |
| Party | Human plus bots; pull, assist, heal, buff, resurrect, and retreat | Shared party rules and command authorization |
| Class binding | Activate and switch subclasses for every playable `ClassId` | Exact class profile only; no role, parent, or archetype fallback |
| Class policy | Run solo and party scenarios owned by each exact class profile | Every referenced skill, item, target rule, and resource reserve is valid |
| Support classes | Bishop/Cardinal, Prophet/Hierophant, Elder/Eva's Saint, Shillien Elder/Saint, Overlord/Dominator, Warcryer/Doomcryer, Sword Singer/Muse, and Bladedancer/Dancer scenarios | Distinct heal, recharge, buff, chant, song, and dance policies remain class-specific; clan-wide support stays disabled under the party boundary |
| Support recipients | Mixed parties containing multiple exact recipient `ClassId` values with different existing effects | Each caster applies its recipient-specific buff or recharge rule; no physical, magical, healer, tank, or other role fallback |
| Support boundary | Place a closer visible owner or clan member at critical HP beside a healthier party member | Self and current party remain the only player support candidates; visible outsiders never receive heals or buffs |
| Resurrection duty | Kill a tank, healer, and higher-level skirmisher during combat and again after combat | Emergency healing wins first; each caster's exact battlefield and safe-state matrix determines recovery order |
| Travel | Walk, gatekeeper, return scroll, route failure, and recovery | Costs and eligibility are enforced |
| Quest | Accept, advance, fail a condition, complete, and restart | Existing quest scripts grant all state and rewards |
| Population | Zone budgets, schedules, spawn pressure, and dormancy | No offline progression or unbounded clustering |
| Social | Chat, invitation, trade, refusal, and relationship update | Permissions, rate limits, and normal economy rules |
| Concurrency | Bursty events and slow decisions for one bot | At most one active decision per bot |
| Shutdown | Stop while foreground and background bots are active | Scheduler quiesces before player storage and pools |
| Soak | 8-hour and 24-hour staged population runs | Bounded queues, memory, failures, and tick latency |
