---
name: mc-mod-testing
description: Design, write, run, and review tests for the Aki-Async Leaves Mixin plugin, including pure JUnit tests, plugin-layer integration tests, concurrency and generation-token tests, resource and metadata contracts, and runtime injection checks on a Leaves development server. Use when editing *Test.java, changing Mixin targets, async pipelines, persistence formats, configuration behavior, generated metadata, or asking how to verify a Leaves plugin change.
---

# Leaves Plugin Testing

Apply `leaves-plugin-development` for the template's source sets and runtime model, and `leaves-mixin-development` for injection verification. Do not use Fabric Loader JUnit, Fabric GameTest entrypoints, Loom test runs, or `fabric.mod.json`.

## Choose the cheapest valid test

Use these layers in order:

1. **Pure unit test:** logic accepts primitives, immutable records, collections, or project-owned POJOs and does not require a running Minecraft server.
2. **Build/resource contract:** compilation, generated metadata, Mixin JSON, AccessWidener, and packaged JAR contents can prove the requirement.
3. **Plugin integration test:** project services can be exercised with explicit fakes or narrow interfaces without pretending Bukkit/NMS objects are thread-safe POJOs.
4. **Leaves runtime test:** the behavior depends on plugin lifecycle, a live world, event dispatch, scheduler ownership, transformed classes, or a Mixin injection point.

Extract pure policy from Mixin hooks and Bukkit/NMS shells so most cases stay in layer 1. Keep a small number of runtime checks for wiring that only a transformed server can prove.

## Pure JUnit tests

Place tests in `src/test/java` and mirror the package under test. Before adding dependencies, inspect the current build; add JUnit Jupiter and `useJUnitPlatform()` only if absent and required.

Good unit-test targets include:

- math, prioritization, batching, throttling, and admission-control policy
- immutable snapshot construction and validation
- state transitions and generation-token rejection
- configuration parsing, defaulting, and disabled paths
- deterministic serialization and persisted-format migration
- cache eviction and compound invariants
- bridge behavior when the plugin is unavailable or disabled

Do not instantiate or mock complex NMS world/entity objects merely to call a helper. Move the decision logic behind a small project-owned interface or into a pure function.

Run the narrow test first:

```powershell
.\gradlew.bat test --tests "com.akiasync.SomeTest" --no-daemon --stacktrace
```

Then run the complete `test` and `build` tasks.

## Concurrency tests

- Assert the actual cross-structure invariant, not only that no exception was thrown.
- Use overlapping keys and coordinated starts so operations genuinely race.
- Check the return value of `awaitTermination`; fail and call `shutdownNow()` if workers do not terminate.
- Bound all waits with timeouts.
- Reset static maps, atomics, executors, and ThreadLocals after each test.
- Verify an older async result is rejected after the generation changes.
- Verify commits are not performed after plugin disable, target removal, world unload, identity change, or ownership change.
- Test saturation behavior for bounded executors and queues.

Never write a unit test that accesses live world state from a worker. The production contract is snapshot -> compute -> validate -> owning-thread commit; tests should make each stage and rejection path observable.

## Persistence and configuration tests

For persisted format changes, cover:

1. legacy data migrates to the new form;
2. migration is idempotent;
3. already-current data passes through unchanged;
4. invalid numeric or collection values are bounded or rejected;
5. serialization order is deterministic.

For every feature toggle, test that the disabled path is inert. Restore global configuration in `finally` or teardown even when the assertion fails.

## Metadata and resource contracts

After `build`, inspect the final JAR rather than generated intermediates:

- `leaves-plugin.json` has the expected name, version, main class, Mixin feature, package, config, and AccessWidener.
- every configured Mixin class exists in the JAR and its JSON package matches the compiled path;
- the AccessWidener resource name matches `build.gradle.kts`;
- no `fabric.mod.json`, client class, or Loom metadata is packaged;
- the artifact name matches `Aki-Async-<version>.jar`.

Prefer a deterministic contract test or inspection script when the same packaging failure could recur.

## Runtime Mixin validation

A successful unit test or build does not prove an injection matches. For any changed target, descriptor, ordinal, slice, local capture, condition, bridge timing, or AccessWidener:

1. Build the plugin with the configured Leaves dev bundle.
2. Start the matching development server through the existing `runServer` task with Mixin enabled.
3. Confirm the plugin enables and all required mixins apply without warnings or target errors.
4. Exercise the exact target path and assert an observable behavior, not merely that startup succeeded.
5. Exercise disable/reload/world-unload behavior when the change owns persistent state or tasks.
6. Record the server version/build used for the check.

## No vacuous assertions

Avoid tests whose only evidence is `assertNotNull`, `assertDoesNotThrow`, or an unconditional success callback. Assert the value, state transition, invariant, rejection reason, side effect, or packaged contract that would regress.

## Guardrails

- Never add Fabric testing dependencies or entrypoints to this project.
- Never widen production access solely for a test; prefer package-private seams, project-owned interfaces, or observable behavior.
- Never assume plain JUnit applies mixins or boots a Leaves server.
- Never claim a Mixin target is validated without a matching runtime test.
- Never let test workers or runtime probes mutate world state off the owning thread.
- Always run the narrowest relevant test, then the full `build` gate before finishing.
