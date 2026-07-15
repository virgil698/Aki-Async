---
name: leaves-mixin-development
description: Design, implement, review, and runtime-validate server-side Sponge Mixins for Leaves plugins built from the official template. Use when changing injection classes under src/mixin/.../mixin/mixins, selecting NMS targets, verifying mapped owners or JVM descriptors, using Mixin Extras or AccessWidener, adding conditional Mixins, bridging transformed code to the JavaPlugin layer without direct cross-source imports, or moving expensive work away from a hot injection.
---

# Leaves Mixin Development

Apply `leaves-plugin-development` first for template layout, metadata, lifecycle, build wiring, and packaging. This skill owns Leaves-specific injection design and the boundary between transformed server code and plugin-owned behavior.

## Verify the target before editing

1. Read the configured Leaves API/dev-bundle version and current Mixin configuration.
2. Inspect the mapped server sources or mapped server JAR produced by the current userdev setup.
3. Record the target owner, mapped method name, full JVM descriptor, injection point, and any ordinal, slice, local-capture, or version dependency.
4. Confirm the hook's semantic branch and call count, not only that a matching method exists.

Never copy Yarn/Fabric descriptors or descriptors from another Minecraft/Leaves build.

## Choose the least invasive injector

- Prefer a public Leaves/Paper event or API when it exposes the required semantics.
- Prefer `@Inject` for observation, guards, or narrow cancellation.
- Prefer Mixin Extras `@WrapOperation` for one call site and `@WrapMethod` for exception-safe bracketing.
- Use `@Redirect`, local capture, ordinals, and slices only when the mapped bytecode proves they are necessary.
- Use `@Overwrite` only when no composable injection can implement the behavior.
- Keep hot injected methods constant-time where possible and delegate substantial policy to testable code.

Use `mc-mixin-craft` for detailed injector, accessor, invoker, priority, and collision patterns.

## Handle conditional targets and widening

- Use the template's conditional Mixin config plugin and build/version annotations when supported Leaves builds have different targets.
- Keep Mixin class names synchronized with the Mixin JSON.
- Use AccessWidener only after verifying the current named owner/member/descriptor.
- Prefer a targeted accessor/invoker when global widening is unnecessary.
- Keep the template's Mixin Extras annotation processor and AccessWidener task chain; do not add Loom.

## Bridge to plugin code safely

- Introduce a bridge only when `src/mixin` must invoke plugin-owned behavior.
- Keep the Bridge contract/manager in the parent `mixin` package, matching the official template, and the injected caller in `mixin.mixins`; do not create a separate Bridge source set/package.
- Main may reference only this neutral Bridge API from Mixin output, never injection classes or Mixin-side implementation helpers.
- Never import main/plugin implementation classes from an injection class. Call only the Bridge contract/manager.
- Keep Bridge signatures free of `net.minecraft` and `com.mojang` types so the main implementation remains plugin-safe.
- Define a safe unavailable/no-op state before plugin enable and after disable.
- Publish bridge state safely if hooks and lifecycle may observe it from different threads.
- Install the real bridge only after its services are ready, and detach it during disable.
- Do not copy the template's nullable example bridge into a hot path without lifecycle hardening.

## Offload work without breaking ownership

For expensive work, use:

1. snapshot immutable values on the owning server/region thread;
2. compute on a bounded worker;
3. schedule the result back to the owner;
4. validate lifecycle, world/chunk/entity identity, ownership, configuration, and generation;
5. commit or discard stale work.

Never pass live worlds, chunks, entities, block entities, inventories, or mutable registry views to workers. Apply `mc-shared-state` and `mc-tick-work` for queues, backpressure, lifecycle, and tick budgets.

## Validate the injection

1. Compile the Mixin source set against the current mapped server.
2. Run the standalone full build and inspect the packaged Mixin JSON/classes/AccessWidener.
3. Start the matching Leaves development server with Mixin enabled.
4. Exercise the exact target branch and verify the original operation runs the intended number of times.
5. Test disabled, enable/disable, unload, stale-result, and exception paths when relevant.
6. Treat descriptor mismatch, failed required injection, early bridge access, or thread-ownership violation as blockers.

Compilation alone does not prove runtime transformation correctness.

## Guardrails

- Never create Fabric Loader metadata, Loom configuration, client Mixins, or `mods/` packaging.
- Never place actual injection annotations outside the `mixin.mixins` package.
- Never reference main implementation classes from Mixin code or vanilla/Mojang classes from main code.
- Never mutate live world state from a plugin worker without proven ownership.
- Never guess a target because an external example looks similar.
- Never hide a brittle ordinal, local capture, or priority dependency; document and runtime-test it.
