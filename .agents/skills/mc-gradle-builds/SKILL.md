---
name: mc-gradle-builds
description: Build, test, run, and troubleshoot the Aki-Async Leaves Mixin plugin with its Gradle wrapper, leavesweight userdev, resource factory, Shadow, Mixin Extras, and AccessWidener tasks. Use when running Gradle commands, editing build.gradle.kts, settings.gradle.kts, gradle.properties, libs.versions.toml, repositories or dependencies, diagnosing dependency resolution, or verifying the plugin JAR and generated Leaves metadata.
---

# Leaves Gradle Builds

Apply `leaves-plugin-development` first for the official template architecture. Treat the checked-in Gradle wrapper and current Leaves plugin template as authoritative. Do not convert the build to Fabric Loom or add Fabric dependency scopes.

## Inspect before changing the build

Read these files first:

- `settings.gradle.kts` for the project name and plugin repositories
- `build.gradle.kts` for `leavesPluginJson`, source sets, dependencies, tasks, and output naming
- `gradle/libs.versions.toml` for the Leaves/Minecraft and plugin versions
- `gradle.properties` for JVM and caching settings
- `gradle/wrapper/gradle-wrapper.properties` for the Gradle distribution

Preserve the template's `main` and `mixin` source sets, mapped server setup, AccessWidener application, resource generation, and `shadowJar` packaging unless the requested change specifically requires altering them.

## Run the smallest useful task

Use the wrapper from the repository root:

```powershell
.\gradlew.bat compileJava --no-daemon --stacktrace
.\gradlew.bat compileMixinJava --no-daemon --stacktrace
.\gradlew.bat test --no-daemon --stacktrace
.\gradlew.bat build --no-daemon --stacktrace
.\gradlew.bat runServer --no-daemon --stacktrace
```

On Unix, use the equivalent `./gradlew` commands.

- Use `compileJava` for plugin-layer compilation failures.
- Use `compileMixinJava` for Mixin, mapping, accessor, or descriptor compilation failures.
- Use `test --tests '<FQN>'` for a single unit test.
- Use `shadowJar` when only packaging needs verification.
- Use `build` for the final gate.
- Use `runServer` for runtime injection validation.
- Use `clean` only when stale outputs are plausible; do not make every build a clean build.

## Preserve output and exit status

- Capture the full Gradle output. Do not use `head`, `tail`, or an initial grep that hides the first failure.
- Give dependency setup and server tasks a generous timeout.
- Do not append unrelated commands in a way that replaces Gradle's exit code. Run artifact inspection only after the build command itself returns success.
- When output is truncated, inspect `build/reports`, `build/test-results`, or the captured log before rerunning.
- After a failure, rerun only the failing task once the cause has been fixed.

## Dependency and repository rules

- Keep `compileOnly(libs.leavesApi)` for the server API and `paperweight.devBundle(libs.leavesDevBundle)` for mapped server development.
- Keep Mixin dependencies on the `mixin` source set and preserve its annotation processor configuration.
- Keep `mixinSourceSet.output` visible to normal plugin compilation when bridge contracts require it.
- Declare plugin load dependencies through `leavesPluginJson`; do not use Fabric entrypoints or mod dependency metadata.
- Shade only libraries that must ship inside the plugin. Never shade the Leaves/Paper API or the server implementation.
- Reuse the version catalog instead of scattering dependency versions through the build script.
- Add a repository only for an actual unresolved dependency. Scope third-party repositories to their groups when safe, but do not break the official Leaves/Paper repositories required by the template.
- Explain any disabled transitives next to the dependency; never disable transitives globally.

## Verify the artifact

After a successful `build`:

1. Confirm `build/libs/Aki-Async-<version>.jar` exists.
2. Inspect the JAR for `leaves-plugin.json`, `aki-async.mixins.json`, `aki-async.accesswidener`, `com/akiasync/`, and `com/akiasync/mixin/`.
3. Read `leaves-plugin.json` from the JAR and verify the name, version, main class, required Mixin feature, Mixin package, config name, and AccessWidener name.
4. Confirm the artifact is intended for `plugins/`, not `mods/`.
5. For Mixin changes, run a matching Leaves server and exercise the injection. Compilation and packaging do not prove a runtime target matches.

Before the build gate, run `.agents/skills/leaves-plugin-development/scripts/check-source-boundaries.ps1` to reject vanilla/Mixin imports from `src/main`, direct main imports from Mixin code, and injection classes outside `mixin.mixins`.

## Guardrails

- Never create `fabric.mod.json`, Loom runs, Fabric datagen tasks, Fabric GameTest tasks, client source sets, or `modImplementation` dependencies.
- Never edit generated metadata under `build/` or `bin/`; edit `leavesPluginJson` in `build.gradle.kts`.
- Never guess NMS signatures from a different server version; use the mapped artifacts from the current Leaves dev bundle.
- Never claim success from a command chain when Gradle itself failed.
- Never skip tests merely to obtain a green build without explaining and resolving the failure.
- Always finish build changes with a standalone successful Gradle task and artifact inspection proportional to the risk.
