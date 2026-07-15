---
name: mc-tick-work
description: Design and review lifecycle-safe, budgeted tick work, scheduled jobs, hot Mixin hooks, event listeners, and tracked world state in the Aki-Async Leaves plugin. Use when adding per-tick or interval work, Bukkit/Paper/Leaves listeners, scheduler tasks, Mixin hooks on hot server paths, entity or chunk trackers, cleanup on disable/unload/disconnect, or performance-sensitive scans.
---

# Leaves Tick Work

Apply `leaves-plugin-development` for lifecycle ownership and `leaves-mixin-development` for transformed hooks. Keep event wiring and lifecycle ownership in the Leaves/Paper plugin layer. Use Mixins only for hooks the public server API cannot provide, and keep those hooks narrow.

Apply `mc-shared-state` whenever the work crosses threads or owns shared mutable state.

## Choose the trigger

Prefer the least invasive trigger that exposes the required semantics:

1. Existing Leaves/Paper/Bukkit event
2. Plugin scheduler task owned by the main plugin
3. Narrow service callback already present in the project
4. Mixin injection into the smallest stable server target

Do not add Fabric event registration or Fabric lifecycle callbacks. Register listeners and tasks from the `JavaPlugin` lifecycle, retain cancellable task handles, and release them in `onDisable`.

## Keep hot hooks cheap

At the top of every hot listener, scheduler callback, or Mixin injection:

- return when the feature is disabled;
- reject irrelevant worlds, entities, chunks, phases, or packet types;
- avoid allocation, logging, reflection, streams, and blocking I/O;
- avoid scheduling one task per object per tick;
- avoid capturing live NMS objects into worker lambdas.

Mixin hooks should capture minimal immutable context and delegate policy to a testable service. Do not swallow required-injection or invariant failures. For best-effort per-object work, isolate failures with contextual rate-limited logging so one bad object does not abort an entire bounded batch.

## Pick the cheapest recurring shape

| Shape | Use when | Cost model |
|---|---|---|
| Event-driven update | A server event already marks the state change | No standing scan |
| Interval-gated sweep | Time passing creates transitions | Constant cheap gate; bounded scan every N ticks |
| Opportunistic prune | Stale data matters only when accessed | No standing task |
| Deadline queue or timing wheel | Many expirations must be processed | Cost tracks due entries, not all entries |
| Bounded per-tick batch | A large backlog must be drained smoothly | Fixed maximum work per tick |

Act on transitions, not repeatedly on a condition that remains true. Size intervals to the phenomenon; second- or minute-scale state rarely needs a per-tick full scan.

## Bound scans

- Scale work with active players, tracked owners, or an explicit queue, not total world population.
- Iterate already-loaded chunks only. Never force-load a chunk from maintenance or cleanup work.
- Put an explicit maximum on objects, chunks, packets, or nanoseconds processed in one tick.
- Carry unfinished work to a later tick with a stable cursor or queue.
- Coalesce duplicate work by target and generation.
- Define overload behavior: reject, replace stale work, degrade precision, or postpone. Unbounded queues are not acceptable.

Measure hot-path changes where practical. A smaller algorithmic bound matters more than micro-optimizing an unbounded scan.

## Respect thread ownership

All live world/entity/chunk mutations occur on the owning server or region thread. For expensive computation:

1. snapshot immutable inputs on the owner;
2. compute on a bounded worker;
3. schedule back to the owner;
4. validate plugin state, target identity, ownership, and generation;
5. commit or discard.

Never wait synchronously for worker completion on the tick thread. Do not claim Folia support until every scheduler and ownership path has been audited and metadata agrees.

## Lifecycle cleanup

Every owned resource needs a cleanup path:

- cancel tasks and stop accepting new work on plugin disable;
- shut down plugin-owned executors with bounded termination handling;
- clear per-player state on quit/kick when it is no longer needed;
- clear per-world and per-chunk state on unload;
- invalidate pending results with a generation increment;
- remove temporary entities, tickets, tags, or blocks only after verifying ownership;
- clear static references and ThreadLocals so reload/test JVMs do not retain old server state.

Use UUIDs or stable keys for long-lived tracking rather than retaining `Player`, entity, world, or chunk objects.

## Temporary world changes

- Record an ownership marker and deadline before or atomically with applying the change.
- Before reverting, confirm the target still contains the value owned by this plugin; a player or another plugin may have changed it.
- Avoid one scheduler task per temporary change. Prefer a deadline queue or bounded sweep.
- For persistent temporary entities that can survive a crash, store a recognizable tag and define orphan cleanup on load. Register tracking before spawning if the load callback may fire synchronously.

## Sync only on change

Cache the last sent immutable value per player or connection and send only when the observable value changes. Clear the cache on disconnect and invalidate it on configuration reload. Bound cache lifetime and never retain the live connection object as the key.

## Validation checklist

- Feature-disabled path returns before meaningful work.
- Work per tick has an explicit bound.
- No scan force-loads chunks or walks all world entities without proof.
- Every task, tracker, executor, and ThreadLocal has lifecycle cleanup.
- Async stages use immutable snapshots and owner-thread validated commits.
- Stale generations and missing targets are discarded.
- Runtime tests exercise the actual listener or Mixin target on a matching Leaves build.
- Metrics or logs can reveal backlog, rejection, slow batches, and repeated failures without spamming hot paths.

## Guardrails

- Never wire Fabric server events or client disconnect caches into this plugin.
- Never run blocking I/O, dependency resolution, or unbounded computation on the tick thread.
- Never force-load chunks from a periodic sweep.
- Never schedule unbounded per-object tasks from a Mixin hook.
- Never mutate mutable Minecraft state from a worker thread without proven ownership.
- Never leave static or lifecycle-owned state without disable/unload cleanup.
- Always validate ownership before reverting or removing world state.
