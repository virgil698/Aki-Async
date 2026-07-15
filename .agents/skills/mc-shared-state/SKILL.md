---
name: mc-shared-state
description: Design and review thread-safe managers, caches, registries, reload paths, and asynchronous computation in the Aki-Async Leaves server plugin. Use when editing *Manager.java, *Cache.java, executors, CompletableFuture pipelines, ConcurrentHashMap, volatile or atomic state, ThreadLocal, synchronized blocks, generation tokens, or any worker task that reads from or commits to Minecraft world state.
---

Apply this guidance to shared state in the Aki-Async Leaves plugin. State may be observed from the server tick, network event loops, reload tasks, and plugin-owned workers. Apply `leaves-plugin-development` for lifecycle ownership and `leaves-mixin-development` when the state originates in a transformed hook.

## Rule 0: Preserve Minecraft thread ownership

Treat mutable worlds, chunks, entities, block entities, inventories, registries, and plugin lifecycle state as owned by their server or region thread unless the current Leaves implementation proves otherwise.

Use this pipeline for expensive work:

1. Snapshot immutable inputs on the owning thread. Copy IDs, coordinates, primitive values, immutable records, and a generation token; do not pass live Minecraft objects to workers.
2. Compute on a bounded executor without calling mutable Bukkit, Paper, Leaves, or NMS APIs.
3. Schedule the result back to the owning thread.
4. Validate the plugin is enabled, the world/chunk/entity still exists, identity and ownership still match, and the generation token is current.
5. Commit only after validation; discard stale results.

Never block the tick thread with `Future.get()`, `join()`, or an equivalent wait for worker work. Define queue bounds, cancellation, and overload behavior for hot paths.

## Rule 1: Snapshot volatile fields to a local before multiple reads

A volatile field guarantees visibility but NOT consistency across multiple reads in the same method. If a concurrent reload swaps the reference between your two reads, you silently mix old and new data.

**Broken — two reads may see different snapshots:**
```java
private static volatile Map<String, List<String>> NAME_POOLS = Map.of();

public static String getRandomName(String profession) {
    List<String> names = NAME_POOLS.get(profession);      // read 1
    if (names == null) names = NAME_POOLS.get("default");  // read 2 — may see a different map
    ...
}
```

**Fixed — snapshot to a local:**
```java
public static String getRandomName(String profession) {
    Map<String, List<String>> pools = NAME_POOLS; // single volatile read
    List<String> names = pools.get(profession);
    if (names == null) names = pools.get("default");
    ...
}
```

The same applies when reading multiple related volatile fields — snapshot all of them at the top of the method:
```java
public static void injectOffers(String profession, List<Offer> offers) {
    Map<String, List<Offer>> profTrades = PROFESSION_TRADES;
    Map<String, List<Offer>> crossTrades = CROSS_PROFESSION_TRADES;
    // Both references are now from the same generation
    ...
}
```

## Rule 2: Compute derived values before publishing, not after

After you assign a new value to a volatile/shared reference, any subsequent read of that reference may see a different thread's write. Compute stats, log messages, and derived values from the local variable you're about to publish.

**Broken — reads the volatile after publishing:**
```java
public static void loadNamePools(Map<String, List<String>> next) {
    NAME_POOLS = next; // publish
    int total = NAME_POOLS.values().stream().mapToInt(List::size).sum(); // reads the volatile — may see someone else's write
    LOGGER.info("Loaded {} names", total);
}
```

**Fixed — compute from the local before publishing:**
```java
public static void loadNamePools(Map<String, List<String>> next) {
    int total = next.values().stream().mapToInt(List::size).sum();
    NAME_POOLS = next; // publish
    LOGGER.info("Loaded {} names", total);
}
```

## Rule 3: Multi-map invariants need atomic compound operations

`ConcurrentHashMap` makes each individual operation atomic, but a cross-map invariant (e.g., a bidirectional mapping where `mapA[v] == p` implies `mapB[p].contains(v)`) is NOT maintained by separate `put` + `add` calls. Concurrent register/unregister calls interleave between the two operations and break the invariant.

**Broken — check-then-act across two maps:**
```java
// Thread A and B can both pass this check for the same villager
if (getFollowerCount(playerUuid) < max) {
    villagerToPlayer.put(villagerUuid, playerUuid);   // step 1
    playerToVillagers.get(playerUuid).add(villagerUuid); // step 2
    // Between steps 1 and 2, another thread can stopFollowing(villagerUuid),
    // removing from villagerToPlayer but not from the (not-yet-populated) set
}
```

**Fix options (pick one):**

**Option A — single lock for the compound operation:**
```java
private static final Object FOLLOW_LOCK = new Object();

public static boolean tryRegister(UUID villager, UUID player, int max) {
    synchronized (FOLLOW_LOCK) {
        if (getFollowerCount(player) >= max) return false;
        villagerToPlayer.put(villager, player);
        playerToVillagers.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(villager);
        return true;
    }
}

public static void stopFollowing(UUID villager) {
    synchronized (FOLLOW_LOCK) {
        UUID player = villagerToPlayer.remove(villager);
        if (player != null) {
            Set<UUID> set = playerToVillagers.get(player);
            if (set != null) set.remove(villager);
        }
    }
}
```

**Option B — atomic claim + rollback (when lock contention matters):**
```java
public static boolean tryRegister(UUID villager, UUID player, int max) {
    // Atomic claim — only one thread can claim a villager
    UUID existing = villagerToPlayer.putIfAbsent(villager, player);
    if (existing != null) return existing.equals(player); // already following

    // Count check — roll back the claim on overflow
    Set<UUID> set = playerToVillagers.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet());
    set.add(villager);
    if (set.size() > max) {
        set.remove(villager);
        villagerToPlayer.remove(villager, player); // conditional remove — only if still ours
        return false;
    }
    return true;
}
```

Either way, `stopFollowing` must use the same atomicity strategy and verify the villager-to-player mapping before mutating the player's set.

## Rule 4: Data + cache coherence

When a primary data map and a derived cache exist side by side, updating them with two separate operations always leaves a race window. A concurrent reader between the two ops either re-populates the cache from stale data (unbounded staleness) or reads the cache while it's inconsistent with the data.

**Broken — any ordering of these two ops has a race:**
```java
TEXTURES.put(id, newTexture);
PROFILE_CACHE.remove(id);
// Between the two lines, a concurrent getProfile() can computeIfAbsent
// against the new TEXTURES value and cache it — then the remove kills it (or misses it)
```

**Fix options:**

**Option A — synchronized pair (simplest):**
```java
public static void register(String id, String texture) {
    synchronized (TEXTURES) {
        TEXTURES.put(id, texture);
        PROFILE_CACHE.remove(id);
    }
}

public static ResolvableProfile getProfile(String id) {
    synchronized (TEXTURES) {
        return PROFILE_CACHE.computeIfAbsent(id, k -> buildProfile(TEXTURES.get(k)));
    }
}
```

**Option B — generation counter (for hot-path reads):**
Increment an `AtomicLong` on every mutation. Readers snapshot the generation before reading the cache; if it changed, discard and retry.

**Option C — single-source cache (no separate data map):**
If the cache always derives from the data, store the derived value directly and eliminate the two-step update.

## Rule 5: Persisted format changes need migration

When you change the format of data persisted to NBT, codecs, or saved hashes (e.g., changing a hash from `item|item` to `itemxN|itemxN`), existing world saves contain the old format. Without migration, the old values silently stop matching and data is lost.

**Required for any format change:**
1. **Keep a legacy parser/hasher** alongside the new one (package-private or `@Deprecated`).
2. **Write a one-shot migration** that converts old→new on first load. Gate it with a persisted flag so it runs exactly once per entity/chunk/data blob.
3. **Don't run migration on every read path** — if the migration iterates all entries and recomputes hashes, a per-read invocation is O(n) on every access. Use a one-shot flag.
4. **Guard preconditions** — if the migration is meaningless on empty data (e.g., no offers loaded yet), early-return rather than marking migration as complete.

## Rule 6: Codec and persistence safety

### Validate on deserialization
Codecs that accept numeric fields must clamp or validate on decode, not just in the UI. NBT can be hand-edited with external tools, and corrupt data can arrive from older world saves.

```java
public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance ->
    instance.group(
        Codec.INT.fieldOf("score").forGetter(d -> d.score)
    ).apply(instance, score -> new PlayerData(Mth.clamp(score, MIN_SCORE, MAX_SCORE)))
);
```

### Deterministic serialization
Use sorted or insertion-ordered collections for any set or map that gets serialized. `HashSet` and `HashMap` iteration order varies between JVM runs, causing unnecessary chunk dirtying (the serialized bytes change even though the logical data hasn't).

```java
// In codec or save path:
Codec.STRING.listOf().xmap(
    list -> new LinkedHashSet<>(list),         // decode
    set -> set.stream().sorted().toList()      // encode — deterministic
)
```

### Bound persisted collections
Collections that grow per-entity or per-interaction (trade stats maps, cured-villager sets, locked-trade sets) need eviction or size caps. Without them, long-running worlds accumulate unbounded data that inflates save files and slows serialization.

```java
if (tradeStats.size() > MAX_TRACKED_VILLAGERS) {
    // Evict oldest entry or least-recently-used
}
```

## Rule 7: ThreadLocal hygiene

- **Always** use `ThreadLocal.remove()` for cleanup, never `set(null)` or `set(false)`. `remove()` deletes the entry from the thread-local map. `set()` retains a dead entry for the thread's lifetime — on Minecraft's pooled server/netty threads, that's the entire server session.
- **Always** ensure the cleanup path is reachable even on exceptions. If the entry is set in one method and cleared in another, an exception in between leaks the entry permanently. See the `mc-mixin-craft` skill for exception-safe patterns in mixin HEAD/RETURN pairs.

## Quick-reference checklist

Before marking a manager/cache class as done, verify:

| Check | What to look for |
|-------|-----------------|
| Volatile snapshot | Every method reads each volatile field at most once into a local |
| Compute-before-publish | Stats, logs, derived values computed from local, not from the field post-assignment |
| Multi-map atomicity | Cross-map invariants maintained by lock or atomic-claim-with-rollback, not separate ops |
| Cache coherence | Data + cache updates are atomic (lock, generation counter, or single-source) |
| Format migration | Persisted format changes include legacy parser, one-shot migration, and migration tests |
| Codec validation | Numeric fields clamped/validated on deserialization, not just in UI |
| Deterministic serialization | Serialized sets/maps use sorted or insertion-ordered collections |
| Bounded collections | Persisted per-entity/per-interaction collections have eviction or size caps |
| ThreadLocal cleanup | Uses `remove()`, cleanup path reachable on exceptions |
| World ownership | Worker tasks hold immutable snapshots, not live world objects |
| Async commit | Result is scheduled to the owner and revalidates identity and generation |
| Backpressure | Executors and queues are bounded and define overload behavior |

## Guardrails

- **Never** assume `ConcurrentHashMap` alone makes a multi-map invariant thread-safe. Individual operations are atomic; compound cross-map operations are not.
- **Never** read or mutate live Minecraft world state from a plugin worker unless ownership and thread safety have been proven from the current Leaves implementation.
- **Never** treat an NMS object reference as a snapshot. Copy the immutable values required by the computation.
- **Never** commit an async result without revalidating the captured generation and target identity on the owning thread.
- **Never** block the tick thread waiting for plugin-owned worker completion.
- **Never** read a volatile field twice in a method when both reads must see the same value. Snapshot to a local.
- **Never** compute derived values (counts, logs, validation) from a volatile/shared reference after publishing a new value to it.
- **Never** update a data map and its derived cache as two separate unsynchronized operations. The ordering of the two ops does not matter — both orderings have a race window.
- **Never** run an O(n) migration on every read path. Use a persisted one-shot flag.
- **Never** use `ThreadLocal.set(null)` or `set(false)` for cleanup. Use `remove()`.
- **Always** pair `ThreadLocal.set()`/`remove()` with exception-safe cleanup (try/finally or equivalent).
- **Always** use conditional removes (`ConcurrentHashMap.remove(key, expectedValue)`) when rolling back a claim that another thread may have already replaced.
- **Always** clamp/validate numeric fields in codec constructors, not just in the config UI. NBT and JSON can be hand-edited.
- **Always** use sorted or insertion-ordered collections for serialized sets/maps. `HashSet` iteration order varies and causes unnecessary chunk dirtying.
- **Always** cap or evict persisted collections that grow per-entity or per-interaction. Unbounded growth inflates save files on long-running worlds.
- **Always** use bounded executors or explicit admission control for work created by hot Mixin hooks.
