# Aki-Async 1.21.11 sources

This directory is an independently buildable 1.21.11 branch of the root 1.21.10 sources.

- Leaves API/dev bundle: `1.21.11-R0.1-SNAPSHOT`
- Plugin version: `4.0`
- Main package: `com.akiasync`
- Artifact: `versions/1.21.11/build/libs/Aki-Async-4.0.jar`

Build from the repository root:

```powershell
.\gradlew.bat -p versions/1.21.11 build --no-daemon --stacktrace
```

Keep normal plugin code in `src/main`, Bridge/Mixin support in the parent `src/mixin/.../mixin` package, and actual injections under `src/mixin/.../mixin/mixins`.

The 1.21.11 source includes the same bounded datapack zip cache, Function compilation cache, small-Function scheduling path, reload metrics, and `/akiasync datapack status` command as the root source. Its Mixin targets use the 1.21.11 `Identifier` and `PermissionSet` signatures.

It also includes the same bounded Redis-style task-tree scheduler and `/akiasync scheduler status` command. The 1.21.11 build pins SpottedLeaf concurrentutil 0.0.8 and relocates it into the plugin JAR; computation stays on plugin workers, while world commits use Paper global, region, or entity schedulers.
