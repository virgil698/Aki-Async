---
name: mc-mixin-craft
description: Write, debug, and maintain server-side Sponge Mixin injections, Mixin Extras hooks, accessors, invokers, and access wideners for the Aki-Async Leaves plugin. Use when creating or editing files under src/mixin, *.mixins.json, or *.accesswidener, or when working with @Inject, @WrapMethod, @WrapOperation, @Redirect, @ModifyVariable, @Accessor, @Invoker, or @Overwrite.
---

Apply this guidance to server-side Mixin code in the Aki-Async Leaves plugin. Defer template layout, lifecycle, and packaging to `leaves-plugin-development`; apply `leaves-mixin-development` for mapped-target verification, bridge boundaries, and Leaves runtime validation.

Keep all actual `@Mixin` classes, accessors, invokers, and injection logic under `src/mixin/java/com/akiasync/mixin/mixins`. Keep Bridge contracts/managers and the conditional config plugin in the parent `com.akiasync.mixin` package. Never import `src/main` implementation classes from an injection; communicate through the Bridge.

## Mixin class structure

Mixin classes are never instantiated by plugin code. Use an abstract class when shadowing instance state or extending the target's parent for lexical access to protected fields; a static-only mixin may be final. A stub constructor satisfies the Java compiler only — Mixin does not merge it into the target class:

```java
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin extends ItemCombinerMenu {

    // Stub constructor — satisfies Java, never called
    private AnvilMenuMixin(@Nullable MenuType<?> type, int id, Inventory inv,
                           ContainerLevelAccess access) {
        super(type, id, inv, access);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void akiasync$onCreateResult(CallbackInfo ci) {
        // Can access this.inputSlots, this.resultSlots, this.player from parent
    }
}
```

## Injection method naming

Prefix injected method names with `akiasync$` to avoid collisions with other Mixin plugins:

```java
private void akiasync$onCreateResult(CallbackInfo ci) { ... }
```

## @Unique fields

Fields added to the mixin class that do not exist on the target must be annotated `@Unique` and prefixed with `akiasync$`:

```java
@Unique
@Nullable
private AnvilResult akiasync$pendingResult;

@Unique
private boolean akiasync$takingResult;
```

## Injection types — when to use each

### @Inject (default choice)
Insert code at a specific point. This is the default because other plugins' mixins can usually coexist.

```java
@Inject(method = "targetMethod", at = @At("HEAD"))
private void akiasync$beforeTarget(CallbackInfo ci) { ... }

@Inject(method = "targetMethod", at = @At("RETURN"))
private void akiasync$afterTarget(CallbackInfo ci) { ... }

@Inject(method = "targetMethod", at = @At("TAIL"))
private void akiasync$atEnd(CallbackInfo ci) { ... }
```

- `HEAD` — before any code runs. Use for guards, early-exit checks.
- `RETURN` — before every `return` opcode. Fires on all exit paths (including early returns).
- `TAIL` — before the final `return` only. Use when you only care about normal completion.

### @Inject with cancellation
For methods returning void:
```java
@Inject(method = "targetMethod", at = @At("HEAD"), cancellable = true)
private void akiasync$cancel(CallbackInfo ci) {
    if (shouldCancel) ci.cancel();
}
```

For methods returning a value:
```java
@Inject(method = "targetMethod", at = @At("HEAD"), cancellable = true)
private void akiasync$override(CallbackInfoReturnable<ItemStack> cir) {
    if (shouldOverride) cir.setReturnValue(ItemStack.EMPTY);
}
```

### @Inject at INVOKE
Target a specific method call within the target method:
```java
@Inject(method = "targetMethod",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
private void akiasync$beforeIsEmpty(CallbackInfo ci) { ... }
```

The `target` uses JVM internal descriptor format: `L<owner>;<name>(<params>)<return>`.

### @Redirect
Replace a single method call. Use sparingly because redirects are conflict-prone. Prefer Mixin Extras `@WrapOperation` when wrapping the operation composes better with other injectors.

```java
@Redirect(method = "targetMethod",
          at = @At(value = "INVOKE",
                   target = "Lnet/minecraft/world/item/ItemStack;getCount()I"))
private int akiasync$modifyCount(ItemStack stack) {
    return stack.getCount() * 2;
}
```

### @WrapOperation (preferred call-site wrapper)
Wrap the original operation and call it exactly once unless intentionally suppressing it:

```java
@WrapOperation(
        method = "targetMethod",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/item/ItemStack;getCount()I")
)
private int akiasync$wrapCount(ItemStack stack, Operation<Integer> original) {
    return original.call(stack) * 2;
}
```

Use `@Redirect` only when the wrapper cannot express the required transformation.

### @ModifyVariable
Modify a local variable's value:
```java
@ModifyVariable(method = "targetMethod", at = @At("STORE"), ordinal = 0)
private int akiasync$modifyLocalVar(int original) {
    return original + 10;
}
```

### @ModifyArg
Modify a single argument to a method call:
```java
@ModifyArg(method = "targetMethod",
           at = @At(value = "INVOKE",
                    target = "Lsome/Class;someMethod(II)V"),
           index = 1)
private int akiasync$modifySecondArg(int original) {
    return original * 2;
}
```

### @Overwrite (last resort)
Replace the entire method body. This is incompatible with other mixins on the method. Use only when no injection or Mixin Extras wrapper can achieve the goal.

## Re-entrancy guard pattern

When your mixin's RETURN hook triggers code that re-enters the same method (e.g., setting a slot triggers `slotsChanged` which calls `createResult` again), use a boolean guard:

```java
@Unique
private boolean akiasync$takingResult;

@Inject(method = "onTake", at = @At("HEAD"))
private void akiasync$beginTake(Player player, ItemStack stack, CallbackInfo ci) {
    this.akiasync$takingResult = true;
}

@Inject(method = "onTake", at = @At("TAIL"))
private void akiasync$endTake(Player player, ItemStack stack, CallbackInfo ci) {
    this.akiasync$takingResult = false;
    // ... do post-take work here
}

@Inject(method = "createResult", at = @At("RETURN"))
private void akiasync$dispatch(CallbackInfo ci) {
    if (this.akiasync$takingResult) return; // skip re-entrant call
    // ... dispatch logic
}
```

## Accessor mixin (read-only field/method access)

When you need to read a private field or call a private method but don't need to inject code:

```java
@Mixin(AnvilMenu.class)
public interface AnvilMenuAccessor {
    @Accessor("cost")
    DataSlot akiasync$getCost();

    @Accessor("repairItemCountCost")
    void akiasync$setRepairItemCountCost(int value);
}
```

Usage: `((AnvilMenuAccessor) (Object) menu).akiasync$getCost()`

## Invoker mixin (call-only for methods)

```java
@Mixin(LivingEntity.class)
public interface LivingEntityLootInvoker {
    @Invoker("dropFromLootTable")
    void akiasync$invokeDropFromLootTable(ServerLevel level, DamageSource source, boolean killedByPlayer);
}
```

## mixins.json configuration

```json
{
    "required": true,
    "minVersion": "0.8",
    "package": "com.akiasync.mixin.mixins",
    "compatibilityLevel": "JAVA_21",
    "mixins": [
        "AnvilMenuAccessor",
        "AnvilMenuMixin",
        "EnchantmentTableBlockMixin"
    ],
    "plugin": "com.akiasync.mixin.AkiAsyncMixinConfigPlugin",
    "injectors": {
        "defaultRequire": 1
    }
}
```

Key settings:
- **`"mixins"`** — dedicated-server mixins packaged by the Leaves plugin
- **`"plugin"`** — the configured conditional Mixin plugin for build/version gates
- **`"defaultRequire": 1`** — fail startup when a required injection target is missing instead of silently disabling behavior

Register the config and AccessWidener through the existing `leavesPluginJson` block in `build.gradle.kts`. Do not create `fabric.mod.json`.

## Access wideners

For fields/methods you need to access from normal (non-mixin) code, use an access widener instead of an accessor mixin:

File: `src/mixin/resources/aki-async.accesswidener`
```
accessWidener v2 named

# Make a private field accessible
accessible field net/minecraft/world/inventory/EnchantmentMenu enchantSlots Lnet/minecraft/world/Container;

# Make a private server method accessible after verifying the current mapped descriptor
accessible method example/server/Target methodName (I)V

# Make a final field mutable
mutable field net/minecraft/world/level/block/EnchantingTableBlock BOOKSHELF_OFFSETS Ljava/util/List;
```

Keep the existing AccessWidener registration in the Leaves template. Never add Loom configuration.

### AW vs accessor mixin

| Use case | Prefer |
|----------|--------|
| Read a field from non-mixin code | Access widener |
| Read a field only inside a mixin class | Accessor interface |
| Call a method from non-mixin code | Access widener |
| Invoke a method only inside a mixin | Invoker interface |
| Modify a final field | AW with `mutable` |

AW is simpler and has no runtime overhead. Use accessor/invoker when you want to avoid widening the field globally (e.g., for a very targeted read inside one mixin).

## Target scope — inject into the narrowest class

Always target the most specific class possible. An overly broad target means your injection runs on every instance in the game, gated only by an `instanceof` check you have to remember to add.

**Broken — targets all block placements to intercept one item:**
```java
@Mixin(BlockItem.class) // fires for every block placed in the game
public class VillagerPlacementMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void akiasync$onUseOn(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(ctx.getItemInHand().getItem() instanceof PlayerHeadItem)) return;
        // ...
    }
}
```

**Fixed — targets only the relevant item class:**
```java
@Mixin(PlayerHeadItem.class) // fires only for player head placements
```

Same principle for entity mixins: don't inject into `LivingEntity.doPush` with an `instanceof Villager` check when you can inject into `Villager` directly.

## Cancellation scope — don't cancel more than you mean to

`ci.cancel()` on a method that bundles multiple state changes discards ALL of them. If you only need to suppress one aspect, use a targeted injection instead.

**Broken — cancelling `setVillagerData` to prevent profession change also discards level/type changes:**
```java
@Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
private void akiasync$lockProfession(net.minecraft.world.entity.npc.VillagerData data, CallbackInfo ci) {
    if (isLocked && data.getProfession() != currentProfession) ci.cancel(); // kills the entire update
}
```

**Fixed — modify only the profession field, preserve everything else:**
```java
@ModifyVariable(method = "setVillagerData", at = @At("HEAD"), argsOnly = true)
private net.minecraft.world.entity.npc.VillagerData akiasync$preserveProfession(
        net.minecraft.world.entity.npc.VillagerData data) {
    if (isLocked && data.getProfession() != currentProfession) {
        return data.setProfession(currentProfession); // only override profession
    }
    return data;
}
```

## Priority on shared targets

When multiple mixins target the same method on the same class (e.g., several `@Inject(method = "mobInteract", at = @At("HEAD"))` across different mixin classes), add explicit `priority` to control ordering. Without it, ordering depends on declaration order in `mixins.json`, which is fragile and breaks silently on refactoring.

```java
@Mixin(value = Villager.class, priority = 1100) // applied before default (1000)
public class VillagerPickupMixin { ... }

@Mixin(value = Villager.class, priority = 900) // applied after default
public class VillagerFollowMixin { ... }
```

Higher Mixin priorities are applied first. Document the rationale for ordering and verify the transformed behavior because injector ordering can still differ by injector type and target.

## Exception-safe context patterns (HEAD + RETURN pairs)

When using HEAD/RETURN injection pairs to bracket a method (e.g., setting a ThreadLocal flag on entry, clearing on exit), `@At("RETURN")` only fires on normal returns — not on exceptions. If the target method can throw, the exit hook never fires and state leaks for the thread's lifetime.

**Broken — exit never fires if target throws:**
```java
@Inject(method = "doSomething", at = @At("HEAD"))
private void akiasync$enter(CallbackInfo ci) {
    MyContext.enter(); // sets ThreadLocal = true
}

@Inject(method = "doSomething", at = @At("RETURN"))
private void akiasync$exit(CallbackInfo ci) {
    MyContext.exit(); // never called if doSomething() throws
}
```

**Fix — @WrapMethod (Mixin Extras) gives a natural try/finally:**
```java
@WrapMethod(method = "doSomething")
private void akiasync$wrapDoSomething(Operation<Void> original) {
    MyContext.enter();
    try {
        original.call();
    } finally {
        MyContext.exit();
    }
}
```

**Fix — without Mixin Extras:** Restructure the context to be self-resetting (e.g., check a tick counter or frame ID rather than a boolean flag), or move the try/finally into the calling code rather than the mixin. Vanilla Mixin has no clean exception-catch injection point.

### ThreadLocal cleanup

Always use `ThreadLocal.remove()` instead of `set(null)` or `set(false)`. `remove()` deletes the entry from the thread-local map entirely. `set()` retains a dead entry for the thread's lifetime — on pooled server threads, that's forever.

## Guardrails

- **Never** target a broad superclass (`BlockItem`, `LivingEntity`) when a narrower subclass (`PlayerHeadItem`, `Villager`) is the actual target. Broad targets fire on every instance in the game.
- **Never** use `ci.cancel()` on a method that bundles multiple state changes when you only want to suppress one. Use `@ModifyVariable` or `@Redirect` on the specific field/call instead.
- **Always** add explicit `priority` when multiple mixin classes target the same method on the same class. Document the ordering rationale.
- **Never** `@Overwrite` when `@Inject`, `@WrapMethod`, or `@WrapOperation` can achieve the goal. Overwrites block other mixins.
- **Never** inject into lambda synthetic methods without verifying the exact method name in the decompiled source. Lambda names are compiler-generated and brittle.
- **Always** use `defaultRequire: 1` in `mixins.json`. Silent mixin failures are the hardest bugs to diagnose.
- **Always** prefix `@Unique` fields and injected methods with `akiasync$`.
- **Always** verify owners, names, and descriptors against the mappings produced by the current Leaves dev bundle. Signatures shift between versions.
- **Never** create client-only mixins or reference client classes. This artifact runs on a dedicated Leaves server.
- **Never** place an `@Mixin`, accessor, invoker, or injected method outside `com.akiasync.mixin.mixins`.
- **Never** leak `net.minecraft` or `com.mojang` types through a Bridge implemented by main plugin code.
- **Never** treat mixin stub constructors as runtime initialization. Put initialization logic in `@Inject(method = "<init>", ...)` if `@Unique` instance state must be initialized.
- **Never** use HEAD + RETURN injection pairs to bracket a method without considering the exception path. If the target method can throw, the RETURN hook is skipped and state (ThreadLocals, boolean guards) leaks. Use `@WrapMethod` with try/finally, or restructure the context to be self-resetting.
- **Always** use `ThreadLocal.remove()` for cleanup, never `set(null)` or `set(false)`.

## Version notes

- **1.20.5+:** Many method signatures changed with the Data Components rewrite. `ItemStack.getTag()` no longer exists. Verify all `@At(target = ...)` descriptors.
- **1.21+:** Enchantment-related mixins need special care — `EnchantmentHelper` was rewritten to use `Holder<Enchantment>`.
