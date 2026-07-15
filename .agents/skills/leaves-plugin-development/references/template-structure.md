# Official Leaves template structure

This reference describes the template snapshot under `look/leaves-plugin-template-master`. Read the actual template and project files before editing because plugin and dependency versions can change.

## Contents

- [Project files](#project-files)
- [Build plugins and their responsibilities](#build-plugins-and-their-responsibilities)
- [Metadata flow](#metadata-flow)
- [Source-set and dependency flow](#source-set-and-dependency-flow)
- [Communication and package boundaries](#communication-and-package-boundaries)
- [Task flow](#task-flow)
- [Template examples](#template-examples)
- [Initialization consistency map](#initialization-consistency-map)
- [Output contract](#output-contract)

## Project files

| Path | Role |
|---|---|
| `settings.gradle.kts` | Sets `rootProject.name` and resolves Leaves Gradle plugins from release/snapshot repositories. |
| `build.gradle.kts` | Declares plugin metadata, repositories, source sets, dependencies, AccessWidener input, run-server behavior, compile ordering, and Shadow packaging. |
| `gradle/libs.versions.toml` | Centralizes Leaves API/dev bundle, build plugins, Sponge Mixin, Mixin Extras, conditional Mixin, and AccessWidener versions. |
| `gradle.properties` | Configures Gradle heap, caching, parallelism, and the IDE AccessWidener task. |
| `gradle/wrapper/*`, `gradlew*` | Pins and launches Gradle. Use the wrapper instead of a machine-global Gradle. |
| `src/main` | Normal Leaves/Paper plugin code and generated plugin metadata resources. |
| `src/mixin` | Dedicated Mixin Java and resource source set. |

## Build plugins and their responsibilities

- `java`: Java compilation and standard source sets.
- `org.leavesmc.leavesweight.userdev`: Leaves/Paperweight mapped server development bundle.
- `com.gradleup.shadow`: Packages main output plus Mixin source-set output into one plugin JAR.
- `xyz.jpenilla.run-paper`: Starts a local development server and downloads optional runtime plugins.
- `org.leavesmc.resource-factory`: Generates `leaves-plugin.json` from the `leavesPluginJson` DSL.
- `io.github.gliczdev.access-widen`: Applies AccessWidener rules to the mapped server JAR used for compilation.

Do not replace these with Fabric Loom. The `net.fabricmc` group on the Sponge Mixin artifact does not make the project a Fabric mod.

## Metadata flow

`settings.gradle.kts` supplies the project name. Gradle `group` and `version` supply coordinates. `leavesPluginJson` defaults plugin name/version from the project and declares:

- main `JavaPlugin` class;
- authors and description when known;
- API version derived from the configured Leaves API version;
- Folia support;
- required or optional Mixin feature;
- Mixin package, config resource, and AccessWidener resource;
- plugin load dependencies when needed.

The resource factory writes the metadata into processed resources. Edit the DSL, not generated JSON.

## Source-set and dependency flow

The template creates a `mixin` source set while retaining the conventional `src/mixin/java` and `src/mixin/resources` tree. Preserve the source-set block as a unit; do not infer paths from one `srcDir` call alone.

Dependency intent:

- `compileOnly(libs.leavesApi)` exposes the Leaves API without shading it.
- `paperweight.devBundle(libs.leavesDevBundle)` supplies current mapped server classes and sources.
- main compilation sees `mixinSourceSet.output` so plugin code can implement the neutral template Bridge contract.
- Mixin compilation receives Sponge Mixin, Mixin Extras and its annotation processor, conditional-Mixin support, and the access-widened mapped server JAR.

The mapped server JAR is produced under the project Gradle cache by userdev setup. Treat it and attached mapped sources as authoritative for Mixin owners, names, and descriptors.

## Communication and package boundaries

The template establishes an intentional asymmetric dependency:

```text
Mixin injections (`src/mixin/.../mixin/mixins`)
                    │
                    ▼
Bridge contract + manager (`src/mixin/.../mixin`)
                    ▲
                    │ `compileOnly(mixinSourceSet.output)`
Main Bridge implementation (`src/main`)
```

- Main plugin code may implement and install Bridge contracts because main compilation sees Mixin source-set output.
- Mixin code must not import or call main implementation classes. It calls the Bridge manager/contract instead.
- Main code must not directly reference `net.minecraft.*`, `com.mojang.*`, Sponge Mixin, or Mixin Extras. Keep vanilla/Mixin-aware code on the Mixin side.
- Bridge method signatures must use types the main side may legally reference: primitives, JDK types, Bukkit/Paper/Leaves API types, or neutral contract DTOs. Do not leak vanilla types through the Bridge.
- Only the `mixin.mixins` package contains actual `@Mixin` injection classes, accessors, invokers, wrappers, redirects, or overwrites. The parent `mixin` package holds the Bridge API, Mixin-side support, and conditional config plugin.

The dev bundle may make vanilla classes visible during compilation, but visibility is not permission to couple the main plugin layer to them.

## Task flow

```text
paperweightUserdevSetup
        │ finalizedBy
        ▼
applyAccessWideners
        ▲
        │ dependsOn
compileMixinJava
        │
        ▼
mixin output ──► shadowJar ◄── main output/resources
                         │
                         ▼
                       build
```

`runServer` derives the Minecraft version from the Leaves API version and sets `leavesclip.enable.mixin=true`. Keep that property for runtime Mixin validation.

## Template examples

- `TemplatePlugin` demonstrates the `JavaPlugin` lifecycle.
- `TemplateMixin` demonstrates `@Inject`, Mixin Extras `@WrapMethod`, and AccessWidener-backed access.
- `TemplateConditionalMixin` demonstrates a build-gated Mixin through `ConditionalMixinConfigPlugin` and `@ServerBuild`.
- `TemplateBridgeMixin`, `BridgeManager`, `Bridge`, and `MyBridge` demonstrate cross-source-set communication.

Every example is marked TODO. Treat it as disposable teaching code. A production bridge needs safe publication, unavailable-state handling, and disable cleanup; a production injection needs current mapped-target verification and runtime testing.

## Initialization consistency map

When renaming a project, update these together:

| Concept | Locations |
|---|---|
| Project name | `settings.gradle.kts`, expected artifact name, documentation |
| Group/version | `build.gradle.kts`, generated metadata, artifact name |
| Main class | Java package/path and `leavesPluginJson.main` |
| Mixin package | Java package/path, Mixin JSON `package`/`plugin`, Gradle `mixin.packageName` |
| Mixin config name | resource filename, Gradle `mixins.add(...)`, packaged JAR |
| AccessWidener name | resource filename, Gradle `accessWidener`, packaged JAR |
| Mixin class names | Java filenames/classes and Mixin JSON array |

Preserve an existing `.gitignore` by default. After renaming, scan for `com.example`, `Template`, `MyBridge`, and old resource identifiers.

## Output contract

`shadowJar` names the artifact `${project.name}-${version}.jar` and includes Mixin output. `build` depends on `shadowJar`. The final JAR belongs in a Leaves server's `plugins/` directory. Leavesclip disables Mixin unless `-Dleavesclip.enable.mixin=true`; the template's development server already supplies it.
