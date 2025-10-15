<div align="center">

# ⚡ AkiAsync

**Zero-Latency Async AI Optimization for Leaves Server**

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Leaves](https://img.shields.io/badge/Leaves-1.21.8+-green.svg)](https://github.com/LeavesMC/Leaves)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)
[![Version](https://img.shields.io/badge/version-2.0.0-orange.svg)](https://github.com/virgil698/Aki-Async/releases)

**MSPT: 36ms → 9-13ms** | **TPS: 20.00 Stable** | **65 Entities Supported** | **0 Crashes**

[Features](#-features) • [Install](#-installation) • [Config](#%EF%B8%8F-configuration) • [Performance](#-performance)

</div>

---

## 🎯 Features

### Core Optimizations (24 modules)

| Category | Modules | Coverage |
|----------|---------|----------|
| **Async AI** | 11 | 65 entity types (95%+ coverage) |
| **Parallel** | 3 | Entity tick batching + async tracking |
| **Lighting** | 2 | Starlight-inspired layered queue |
| **Performance** | 3 | Push/Lookup/Collision (ServerCore) |
| **Memory** | 3 | BlockPos pooling + cache (FerriteCore) |
| **Other** | 2 | Spawning + Pathfinding |

### Async AI Coverage

**Dedicated Optimization (10 entities):**
- Villager/WanderingTrader - POI snapshot + atomic job claiming
- Piglin/PiglinBrute - UUID virtual reference pattern
- Pillager/Vindicator/Ravager - Safe REFLECTIONS writeback
- Evoker, Blaze, Guardian/ElderGuardian, Witch

**Universal AI (55 entities):**
- Hostile: Skeleton, Zombie, Creeper, Spider, Enderman, Phantom, Drowned, Hoglin, etc. (16 types)
- Defensive: IronGolem, SnowGolem
- Passive: Pig, Cow, Sheep, Chicken, Rabbit, Bat, Fox, etc. (12 types)
- Aquatic: Dolphin, Squid, Turtle, Fish species, Axolotl (9 types)
- Tamed/Neutral: Wolf, Cat, Horse, Llama, Panda, Bee, etc. (16 types)

---

## 📊 Performance

**Test Environment:**
- Server: Leaves 1.21.8-137
- CPU: Intel Core Ultra 5 125H (18 threads)
- Entities: 147-855 (stress tested)

**Results:**

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| **MSPT (median)** | 36ms | **9-13ms** | ↓ 64-74% |
| **TPS** | 19.95 | **20.00** | Stable |
| **CPU Usage** | Single-core | **28-32%** Multi-core |
| **Entity Efficiency** | 0.245ms | **0.028ms** | ↑ 88% |
| **Crashes** | N/A | **0** | Zero |

---

## 🚀 Installation

### Requirements

- **Server**: Leaves 1.21.8+
- **JDK**: Java 21+
- **Memory**: 4GB+ recommended

### Quick Start

```bash
# 1. Download JAR
wget https://github.com/virgil698/Aki-Async/releases/latest/download/Aki-Async.jar

# 2. Place in plugins folder
mv Aki-Async.jar /path/to/server/plugins/

# 3. Add Mixin support to startup script
java -Dleavesclip.enable.mixin=true -Xms4G -Xmx4G -jar leaves.jar

# 4. Restart server
```

**Critical:** Must add `-Dleavesclip.enable.mixin=true` to JVM arguments.

---

## ⚙️ Configuration

Config file: `plugins/AkiAsync/config.yml` (auto-generated on first start)

### Quick Config

```yaml
# Async AI (enable per-entity type)
async-ai:
  timeout-microseconds: 100
  
  villager-optimization:
    enabled: true  # Villager + WanderingTrader
  
  piglin-optimization:
    enabled: true  # Piglin + PiglinBrute
    look-distance: 16
  
  pillager-family-optimization:
    enabled: true  # Pillager/Vindicator/Ravager
  
  evoker-optimization:
    enabled: true  # Evoker (spell + Vex summon)
  
  blaze-optimization:
    enabled: true  # Blaze (fireball + fire column)
  
  guardian-optimization:
    enabled: true  # Guardian + ElderGuardian
  
  witch-optimization:
    enabled: true  # Witch (v2.1 safe reflection)
  
  universal-ai-optimization:
    enabled: true  # 55 entity types (see config for full list)
    entities: ["minecraft:skeleton", "minecraft:zombie", ...]

# Parallel Entity Tick
entity-tick-parallel:
  enabled: true
  threads: 6
  batch-size: 8  # Entity-level batching
```

### Configuration Tips

| Server Size | Thread Pool | Entity Tick Threads |
|-------------|-------------|---------------------|
| Small (1-10) | 2-4 | 4 |
| Medium (10-50) | 4-8 | 6-8 |
| Large (50+) | 8-16 | 8-12 |

---

## 🔧 Technical Highlights

### UUID Virtual Reference Pattern
```java
// Async thread: Store UUID (thread-safe)
UUID playerId = player.getUUID();

// Main thread: Query player (O(1) hash lookup)
Player player = level.getPlayerByUUID(playerId);
// Defensive check + distance validation
if (player != null && !player.isRemoved() && distance < 16) {
    brain.setMemory(ATTACK_TARGET, player);
}
```

### Safe REFLECTIONS Utility
```java
// v2.1: printStackTrace + no rethrow (prevents static block crash)
static {
    try {
        TARGET_FIELD = Mob.class.getDeclaredField("target");
    } catch (Throwable t) {
        t.printStackTrace();  // Print full stack
        // Don't rethrow → static block continues → field = null
    }
}

// Defensive null check in apply
if (TARGET_FIELD != null) {
    TARGET_FIELD.set(mob, player);
}
```

### Entity-Level Batching
```java
// Before: Chunk-level (0-150+ entities per task)
Map<ChunkKey, List<Entity>> chunks;

// After: Entity-level (8 entities per task)
List<List<Entity>> batches = partition(entities, 8);
CompletableFuture.allOf(batches.stream()
    .map(batch -> CF.runAsync(() -> processBatch(batch)))
    .toArray(CF[]::new)
).get(adaptiveTimeout, MILLISECONDS);
```

---

## 📚 Project Structure

```
brain/
├── core/       - AsyncBrainExecutor (common utility)
├── villager/   - Villager AI (4 files)
├── piglin/     - Piglin AI (3 files)
├── pillager/   - Pillager family (3 files)
├── evoker/     - Evoker AI (3 files)
├── blaze/      - Blaze AI (3 files)
├── guardian/   - Guardian AI (3 files)
├── witch/      - Witch AI v2.1 (3 files)
└── universal/  - Universal AI template (3 files)
```

---

## 🛠️ Build from Source

```bash
git clone https://github.com/virgil698/Aki-Async.git
cd Aki-Async
./gradlew clean build

# Output: build/libs/Aki-Async-2.0.0-SNAPSHOT.jar
```

---

## 🙏 Credits

Inspired by excellent projects:

| Project | Contribution |
|---------|--------------|
| [Starlight](https://github.com/PaperMC/Starlight) | Layered light propagation queue |
| [Lithium](https://github.com/CaffeineMC/lithium) | General optimization strategies |
| [ServerCore](https://github.com/Wesley1808/ServerCore) | Push/Collision/Lookup optimizations |
| [FerriteCore](https://github.com/malte0811/FerriteCore) | Memory optimization patterns |
| [Nitori](https://github.com/Gensokyo-Reimagined/Nitori) | Entity tracker design |
| [Leaves](https://github.com/LeavesMC/Leaves) | Mixin support |

---

## ⚠️ Compatibility

- ✅ Tested: Leaves 1.21.8-137
- ✅ Verified: 855 entities stress test
- ⚠️ May conflict with other Mixin plugins
- ⚠️ Test on staging server before production

---

## 📄 License

GPL-3.0 License - See [LICENSE](LICENSE)

**Community Edition (Free & Open Source)**

---

<div align="center">

**Edition**: Community | **Status**: ✅ Production Ready | **Version**: 2.0.0 | **Coverage**: 65 Entities

Made with ❤️ for Minecraft Server Optimization

**[Report Bug](https://github.com/virgil698/Aki-Async/issues)** • **[Request Feature](https://github.com/virgil698/Aki-Async/issues)**

---

If this helps, please ⭐ Star the project!

</div>
