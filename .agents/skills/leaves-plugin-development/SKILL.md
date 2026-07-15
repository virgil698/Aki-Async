---
name: leaves-plugin-development
description: Develop, initialize, review, debug, and package Leaves server plugins built from the official Leaves Gradle plugin template. Use when working with settings.gradle.kts, build.gradle.kts, leavesPluginJson metadata, leavesweight userdev, the main and mixin source sets, Bridge communication, plugin lifecycle, commands, listeners, configuration, plugin dependencies, runServer, Shadow packaging, or deciding which packages may reference Paper APIs, NMS, Mojang classes, or Mixin annotations.
---

# Leaves Plugin Development

Treat the checked-in official template and the repository's current Gradle files as authoritative. Read [`references/template-structure.md`](references/template-structure.md) when initializing a project or changing build wiring, source layout, generated metadata, Mixin resources, or packaging.

## Establish the project context

1. Read `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, and the wrapper properties.
2. Read the `JavaPlugin` main class, `src/mixin` tree, Mixin JSON, AccessWidener, Mixin config plugin, and template-style Bridge contract/manager.
3. Derive the Minecraft version from the configured Leaves API/dev bundle; do not assume a version from memory.
4. Identify which template features are active: NMS userdev, Mixin, Mixin Extras, conditional Mixins, AccessWidener, run-server support, and shading.

Do not replace the official template with a generic Paper, Fabric, or hand-rolled Gradle layout.

## Route code to the correct layer

- Put plugin lifecycle, commands, listeners, configuration, services, Paper/Leaves API integration, and ordinary business logic in `src/main`.
- Keep `src/main` on the plugin-safe surface: JDK/project code, Bukkit/Paper/Leaves plugin APIs, plus only the template-style neutral `Bridge` and `BridgeManager` contracts exposed from Mixin output. Main must not reference any other Mixin-side class.
- Put Bridge interfaces/managers, the conditional Mixin config plugin, and Mixin-side support code under `src/mixin/java/<base-package>/mixin`; do not create a separate Bridge package or source set.
- Put every actual injection class—including `@Mixin` classes, accessors, invokers, wrappers, redirects, and overwrites—under `src/mixin/java/<base-package>/mixin/mixins`.
- Put Mixin JSON and AccessWidener files in `src/mixin/resources`.
- Keep pure policy in normal testable Java classes. Keep injected methods narrow and delegate when practical.
- Communicate between injected code and the plugin layer only through Bridge contracts/managers. Mixin injection classes must not import plugin implementation classes from `src/main`.
- Keep Bridge signatures neutral: do not expose `net.minecraft` or `com.mojang` types that would force main code to reference vanilla internals.
- Define Bridge behavior before enable, during disable, and after disable; template bridge classes are examples, not production lifecycle guarantees.

```text
src/mixin/.../mixins ──calls──> Bridge/BridgeManager in src/mixin
                                      ▲
                                      │ main compiles against mixin output
src/main/.../BridgeImpl ──installs implementation during plugin lifecycle
```

Apply `leaves-mixin-development` and `mc-mixin-craft` for injection details. Apply `mc-shared-state` and `mc-tick-work` for shared state, workers, or hot paths.

## Manage metadata and lifecycle

- Edit the `leavesPluginJson` DSL in `build.gradle.kts`; do not hand-edit generated `leaves-plugin.json` under `build/` or `bin/`.
- Keep name/version inheritance from the Gradle project unless the project intentionally overrides it.
- Keep the configured main class aligned with its package and file path.
- Declare required or optional Mixin support deliberately.
- Declare plugin dependencies through the template's Leaves metadata API, not Fabric metadata.
- Register plugin-owned listeners, commands, services, schedulers, and configuration from the `JavaPlugin` lifecycle.
- Cancel tasks, stop executors, detach bridges, and release lifecycle-owned state in `onDisable`.
- Do not claim Folia support until scheduler and world-ownership paths have been audited and metadata agrees.

## Initialize from the template

When creating or reinitializing a project:

1. Copy the template build, wrapper, Gradle catalog, and source layout.
2. Preserve an existing repository `.gitignore` unless the user explicitly asks to replace it.
3. Set the project name, group, version, main class, package paths, description, and author only from known user input.
4. Rename Mixin JSON and AccessWidener resources consistently in files, Gradle metadata, and package paths.
5. Replace all `com.example`, `Template*`, `MyBridge`, and template resource identifiers.
6. Keep demo injections or bridges only when the user wants examples; otherwise remove them or mark them clearly as scaffolding.
7. Scan for stale template identifiers before building.

Never invent author identity or silently restore unrelated deleted files.

## Build and validate

- Use the checked-in wrapper and Java version configured by the template.
- Run the smallest relevant compile/test task, then a standalone `build --no-daemon --stacktrace` final gate.
- Verify the final Shadow JAR contains generated `leaves-plugin.json`, main classes, Mixin classes, configured Mixin JSON, and AccessWidener.
- Verify embedded metadata matches the project name, version, main class, API version, features, package, and resource names.
- Use `runServer` for runtime plugin and Mixin checks. A successful build does not prove an injection target matches.
- Install the artifact in `plugins/`, not `mods/`.

Use `mc-gradle-builds` for detailed Gradle execution and artifact inspection, and `mc-mod-testing` for test selection.

## Guardrails

- Never create `fabric.mod.json`, Fabric Loader entrypoints, Loom configuration, client source sets, or client Mixins.
- Never copy Yarn/Fabric names or descriptors into Leaves Mixin code; use the current mapped Leaves dev bundle.
- Never import `net.minecraft`, `com.mojang`, Sponge Mixin, or Mixin Extras from `src/main`.
- Never import a Mixin-side type from `src/main` except the neutral parent-package `Bridge`/`BridgeManager` API required by the official template pattern.
- Never put NMS/Mojang or injection types in Bridge contracts or DTOs.
- Never place an injection class outside the `mixin.mixins` package.
- Never let Mixin injection classes import main/plugin implementation packages; use Bridge contracts.
- Never move Mixin code into `src/main` merely to bypass source-set wiring.
- Never duplicate Mixin Extras or AccessWidener setup already supplied by the template.
- Never edit generated outputs as source.
- Never mutate live Minecraft world state from a worker without proven ownership and a validated owner-thread commit.

Run `scripts/check-source-boundaries.ps1` after moving packages or adding Bridge/Mixin code.
