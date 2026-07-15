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
