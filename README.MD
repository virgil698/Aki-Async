<div align="center">

# AkiAsync

> Mixin-based async optimization plugin designed to enhance performance for Leaves/Luminol/LightingLuminol/Lophine/Mint and their forks without altering core features

**Leaves / Luminol Async Optimization Plugin**

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Leaves](https://img.shields.io/badge/Leaves-1.21.8+-green.svg)](https://github.com/LeavesMC/Leaves)
[![Luminol](https://img.shields.io/badge/Luminol-1.21.8+-purple.svg)](https://github.com/LuminolMC/Luminol)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)

**Community Edition**

[Quick Start](#quick-start) • [Commands](#commands) • [Configuration](#configuration) • [FAQ](#faq)

</div>

English | [简体中文](READMECN.MD)

---

## Core Features

### Performance
- **Steady TPS Improvement** - Reduces main thread pressure, significantly lowers MSPT
- **Multi-threaded Balance** - CPU usage distributed across multiple cores/threads with work-stealing support
- **Zero-delay Snapshots** - Snapshot-compute-writeback pattern completes within the same tick
- **Full Compatibility** - Maintains all vanilla features, compatible with Paper/Spigot/Leaves/Folia/Luminol configs
- **Java 21 Virtual Threads** - Leverages modern Java features with automatic fallback for older versions

### Optimization Modules (90+ Configurable Optimizations)
- **Entity System** (20+) - Parallel entity tick, entity tracker, density control, entity throttling, travel optimization, mob despawn optimization, projectile optimization, suffocation optimization, Nitori entity optimizations (sunburn, speed, fall damage, section storage), VMP entity optimizations (zero velocity skip, tracker distance cache)
- **Async AI** (15+) - Enhanced async pathfinding with priority queue and multi-layer cache, AI sensor optimization, game event optimization, brain throttling, villager optimization (fast raytrace, async breeding), universal AI template (65+ mobs)
- **Collision System** (10+) - Native collision detection (AVX SIMD), DDA ray collision, block shape cache, shape precomputation, aggressive mode, block cache with expiration
- **Item & Experience Orbs** (6) - Smart merge, age optimization, inactive tick optimization for both items and experience orbs
- **Block Entity** (8) - Parallel block entity tick, zero-delay factory, hopper container cache, minecart tick optimization, furnace/crafting recipe cache, C2ME NBT optimization
- **TNT Explosion** (8) - Unified explosion engine with ByteVector optimization, density cache, vectorized AABB detection, entity merge, vanilla-compatible damage calculation, full protection plugin support
- **Chunk System** (15+) - Async chunk loading/saving/serialization, noise optimization, Jigsaw optimization, ChunkPos optimization, Perlin noise optimization, mid-tick tasks, async blending, storage IO worker, NBT cache limit
- **Network** (20+) - Native compression (libdeflate), native encryption (OpenSSL), fast VarInt encoding, MTU-aware batching, flush consolidation, AFK packet throttle, dynamic chunk send rate, packet priority queue, chunk visibility filter, fast movement chunk preload with center offset
- **Structure Location** (6) - Async locate command, dolphin treasure hunting, chest exploration maps, villager trade maps, algorithm optimization with hybrid search pattern, biome-aware search
- **Lighting System** (2) - Async lighting engine with time window control, light update batching
- **Secure Seed** (4) - Quantum encryption scheme, structure/ore/slime protection, anti-plugin theft, /seed command restriction
- **Datapack** (3) - Parallel datapack loading, command deduplication, execute command inactive skip
- **Smart Lag Compensation** (4) - Time acceleration, item pickup delay compensation, potion effect compensation, TPS-aware auto-adjustment
- **Vanilla Bug Fixes** (8) - Bee fixes (7 MC bugs), End island density fix, portal suffocation check, shulker bullet self-hit fix, minecart cauldron destruction fix, leader zombie health fix
- **Memory & Math** (8) - BlockPos cache, BitSet pooling, CompletableFuture optimization, optimized collections, compact sine lookup table, C2ME resource optimizations
- **Advanced** (8) - Virtual threads, work-stealing scheduler, multi-netty event loop, PalettedContainer lock removal, type-filterable list, spawn density array, biome access optimization

> **Safety Guarantee**: All optimization targets are rigorously tested to ensure no vanilla game mechanics are broken

---

## System Requirements

| Component | Requirement |
|-----------|-------------|
| **Server** | [Leaves](https://github.com/LeavesMC/Leaves) or [Luminol](https://github.com/LuminolMC/Luminol) and their forks (Tested: Leaves, Luminol, LightingLuminol, Lophine, Mint) |
| **Server Version** | 1.21.8+ (Tested: 1.21.8, 1.21.10, 1.21.11) |
| **JDK** | Java 21+ (Tested: Java21, Java 25) |
| **Memory** | Recommended 6GB+ |
| **CPU** | Recommended 4+ cores (multi-threaded optimization, but single-core performance still matters) |

---

## Quick Start

### 1. Download Plugin

Download the latest version from [MineBBS](https://www.minebbs.com/resources/aki-async-leaves-luminol.14159/) or [Releases](https://github.com/virgil698/Aki-Async/releases)

### 2. Install Plugin

```bash
# Place JAR file in plugins folder
cp Aki-Async-2.0.0.jar /path/to/server/plugins/
```

### 3. Enable Mixin Support

Modify your startup script and add the following parameter:

```bash
java -Dleavesclip.enable.mixin=true -Xms4G -Xmx4G -jar server.jar
```

**Important**: You must add the `-Dleavesclip.enable.mixin=true` parameter, otherwise the plugin will not work

### 4. Start Server

On first startup, configuration files will be automatically generated: `plugins/AkiAsync/config.yml`

---

## Commands

<details>
<summary><b>/aki-reload - Hot Reload Configuration</b></summary>

**Function**: Reload configuration and apply changes without restarting the server

**Permission**: `akiasync.reload` (OP only by default)

**Important Warning**:
- Hot reload is not recommended unless necessary
- Server restart is preferred after modifying optimization settings
- Hot reload may cause mysterious issues
- Production environments should restart during maintenance windows

**Usage**:
```bash
/aki-reload
```

**Effects**:
- Reloads `config.yml` configuration
- Smoothly restarts all thread pools (no task loss)
- Clears all internal caches
- Resets performance statistics counters
- Updates all module configuration references

**Example**:
```yaml
# Modify config.yml
entity-tick-parallel:
  threads: 8  # Changed from 4 to 8

# Execute command
/aki-reload
# Output: §a[AkiAsync] Configuration hot-reloaded, thread pools smoothly restarted.
```

**Recommended Use Cases**:
- Toggle debug logging
- Adjust performance monitoring parameters
- Fine-tune thread counts (use with caution)
- Avoid: Enabling/disabling core optimization features

</details>

<details>
<summary><b>/aki-debug &lt;true|false&gt; - Debug Mode</b></summary>

**Function**: Dynamically enable/disable debug logging

**Permission**: `akiasync.debug` (OP only by default)

**Usage**:
```bash
/aki-debug true   # Enable debug mode
/aki-debug false  # Disable debug mode
```

**Aliases**: Supports `on/off`, `enable/disable`

**Effects**:
- Real-time toggle of debug logging
- Automatically saves to configuration file
- Triggers config reload to apply changes
- Controls performance metrics output (every 60 seconds)

**Use Cases**:
- Enable during performance debugging to view detailed metrics
- Disable during normal use to avoid console spam
- Enable when monitoring thread pool status
- Recommended to use with Spark plugin

**Example**:
```bash
# Enable debug mode
/aki-debug true
# Output: §a[AkiAsync] Debug logging enabled successfully!
# Output: §a[AkiAsync] Configuration reloaded to apply debug changes.

# Disable debug mode  
/aki-debug false
# Output: §a[AkiAsync] Debug logging disabled successfully!
```

</details>

<details>
<summary><b>/aki-version - Version Information</b></summary>

**Function**: Display detailed plugin information and system environment

**Permission**: `akiasync.version` (OP only by default)

**Usage**:
```bash
/aki-version   # Display version information
```

**Display Content**:
- Plugin name, version, author
- Server type and version (Leaves/Paper/Spigot)
- Minecraft version
- Java version
- Operating system information
- Current enabled optimization features status

**Use Cases**:
- Provide version information when reporting issues
- Check currently enabled optimization features
- Verify server environment configuration
- Generate technical support reports

**Example**:
```bash
# View version information
/aki-version

# Example output:
# [AkiAsync] ========================================
# [AkiAsync] Plugin: AkiAsync
# [AkiAsync] Version: 3.2.10-SNAPSHOT
# [AkiAsync] Authors: Virgil
# [AkiAsync] Config Version: 11
# [AkiAsync] 
# [AkiAsync] Server: Leaves 1.21.4
# [AkiAsync] Minecraft: 1.21.4
# [AkiAsync] Java: 21.0.5
# [AkiAsync] OS: Windows Server 2019 10.0
# [AkiAsync] 
# [AkiAsync] Active Optimizations:
# [AkiAsync]   Entity Tracker: ON
# [AkiAsync]   Mob Spawning: ON
# [AkiAsync]   Entity Tick Parallel: ON
# [AkiAsync]   Block Entity Parallel: ON
# [AkiAsync]   Async Lighting: ON
# [AkiAsync]   Async Pathfinding: ON
# [AkiAsync]   Chunk Tick Async: OFF
# [AkiAsync]   Brain Throttle: ON
# [AkiAsync]   TNT Optimization: ON
# [AkiAsync]   Universal AI: ON
# [AkiAsync]   BeeFix: ON
# [AkiAsync]   Structure Location Async: ON
# [AkiAsync]   SecureSeed: OFF
# [AkiAsync]   Falling Block Parallel: ON
# [AkiAsync]   Item Entity Parallel: ON
# [AkiAsync]   Item Entity Smart Merge: ON
# [AkiAsync]   Item Entity Age Optimization: ON
# [AkiAsync]   Minecart Cauldron Destruction: ON
# [AkiAsync]   Network Optimization: ON
# [AkiAsync]   Fast Movement Chunk Load: ON
# [AkiAsync]   Center Offset Loading: ON
# [AkiAsync] ========================================
```

**Tip**:
- When reporting bugs, execute this command first and attach the output screenshot
- Can be used to quickly check if configuration is effective

</details>

<details>
<summary><b>/aki-network &lt;true|false&gt; - Network Traffic Monitor</b></summary>

**Function**: Toggle real-time network throughput display in ActionBar

**Permission**: `akiasync.network` (OP only by default)

**Usage**:
```bash
/aki-network true   # Enable network monitor
/aki-network false  # Disable network monitor
```

**Display Content**:
- Download rate (↓): Server incoming traffic per second
- Upload rate (↑): Server outgoing traffic per second
- Displayed in pink-purple gradient colors via ActionBar

**Use Cases**:
- Monitor server network load in real-time
- Diagnose network bottlenecks
- Observe traffic changes during peak hours
- Evaluate network optimization effectiveness

**Example**:
```bash
# Enable network monitor
/aki-network true
# ActionBar displays: ↓ 1.25 MB/s | ↑ 3.42 MB/s

# Disable network monitor
/aki-network false
```

**Note**:
- Traffic values are estimated based on packet count
- Each player can independently toggle their display
- Does not affect server performance

</details>

---

## FAQ

<details>
<summary><b>Q: Plugin won't load?</b></summary>

Check if startup parameters include `-Dleavesclip.enable.mixin=true`

</details>

<details>
<summary><b>Q: No significant MSPT improvement?</b></summary>

1. Confirm entity count > 50 (parallel optimization won't activate below 50)
2. Gradually enable async AI optimizations (start with villagers)
3. Use Spark for detailed performance analysis

</details>

<details>
<summary><b>Q: How to verify optimization effects?</b></summary>

Use [Spark](https://spark.lucko.me/) performance profiler:
```
/spark profiler start --timeout 60
# Wait 5 minutes
/spark profiler stop
```

Observe MSPT changes and CompletableFuture percentage

If you encounter performance issues, please submit an Issue with your Spark report link or file for troubleshooting

</details>

<details>
<summary><b>Q: Plugin compatibility?</b></summary>

- Compatible with most plugins
- Submit an Issue if conflicts occur

</details>

<details>
<summary><b>Q: Recommended activation order?</b></summary>

Suggested gradual activation while observing effects:
1. First enable `entity-tick-parallel` (parallel entity ticking)
2. Then enable `villager-optimization` (villager optimization)
3. Finally enable `universal-ai-optimization` (universal optimization)

After each activation, run for a while and confirm no issues before enabling the next.

</details>

---

## Development Build

```bash
# Clone project
git clone https://github.com/virgil698/Aki-Async.git
cd Aki-Async

# Build
./gradlew clean build

# Output located at
build/libs/Aki-Async-2.0.0-SNAPSHOT.jar
```

---

## License

This project is licensed under the [GPL-3.0 License](LICENSE)

---

## References & Acknowledgments

This project references the following excellent open source projects:

| Project | Contribution |
|---------|--------------|
| [Starlight](https://github.com/PaperMC/Starlight) / [ScalableLux](https://github.com/RelativityMC/ScalableLux) | Light propagation algorithm |
| [Lithium](https://github.com/CaffeineMC/lithium) | Performance optimization ideas |
| [C2ME](https://github.com/RelativityMC/C2ME-fabric) | Chunk system optimization reference |
| [ServerCore](https://github.com/Wesley1808/ServerCore) | Hotspot optimization solutions |
| [FerriteCore](https://github.com/malte0811/FerriteCore) | Memory optimization strategies |
| [Async](https://github.com/AxalotLDev/Async) | Async boundary design |
| [SecureSeed](https://github.com/Earthcomputer/SecureSeed) | Secure seed design |
| [Leaves](https://github.com/LeavesMC/Leaves) | Mixin support |
| [Pufferfish](https://github.com/pufferfish-gg/Pufferfish) | Async design reference and compatibility evaluation |
| [Akarin](https://github.com/Akarin-project/Akarin) | Async design reference and compatibility evaluation |
| [tt20](https://github.com/snackbag/tt20) | Async design reference and compatibility evaluation |
| [Sakura](https://github.com/Samsuik/Sakura) | TNT explosion optimization reference |
| [C3H6N6O6](https://github.com/KenRouKoro/C3H6N6O6) | Entity multithreading computation reference |
| [Velocity](https://github.com/PaperMC/Velocity) | Network optimization reference |

> **Note**: The above projects are used for Aki-Async design reference and compatibility evaluation. They are for reference only and their code is not directly packaged or reused.

---

## Disclaimer

- This plugin modifies server core code through Mixin, please test on a test server first
- 100% compatibility with all plugins is not guaranteed
- Submit an [Issue](https://github.com/virgil698/Aki-Async/issues) if problems occur

---

<div align="center">

### Contact

**Issues**: [GitHub Issues](https://github.com/virgil698/Aki-Async/issues) 
**Discord**：[Discord](https://discord.gg/pv9KdHwXWM)
**Email**: virgil698@sky233.top  
**Discord**: xiaokong23357(virgil698)

---

**If you find this useful, please give it a Star to support development!**

Made with ❤️ for Minecraft Server Optimization

</div>
