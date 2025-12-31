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

### Optimization Modules (70+ Configurable Optimizations)
- **Async AI Optimization** (20+) - Villagers, Endermen, Wandering Traders, Wardens, Hoglins, Allays, Armadillos, Sniffers, Camels, Frogs, Goats, Pandas, Piglins, Pillager family, Evokers, Blazes, Guardians, Witches, Universal AI template (65 mobs), async pathfinding, game event optimization, AI sensor optimization, brain throttling, behavior throttling
- **Entity Performance** (15+) - Smart item merge, item age optimization, item inactive tick, experience orb merge, experience orb inactive tick, advanced collision optimization, collision optimization, entity push optimization, entity throttling, entity travel optimization, mob despawn optimization, entity density control, projectile optimization, parallel entity tick, Nitori entity optimizations (sunburn, speed, fall damage, section storage)
- **Block Entity Optimization** (5) - Parallel block entity ticking, hopper optimization, minecart optimization, furnace recipe cache, crafting recipe cache
- **TNT Explosion Optimization** (1) - Three-phase async: snapshot → compute → apply, supports massive simultaneous explosions with vanilla-compatible damage
- **Collision System** (6) - Collision optimization, advanced collision optimization, push optimization, block shape cache, cube voxel shape optimization, collision cache
- **Lighting System** (1) - Async lighting engine
- **Redstone Optimization** (1) - Redstone wire turbo optimization
- **Structure Location** (4) - Async locate command, dolphin treasure hunting, chest exploration maps, villager trade maps
- **Secure Seed System** (3) - Decoration seed encryption, structure seed encryption, slime chunk seed encryption
- **Datapack Optimization** (2) - Command deduplication, execute command inactive skip
- **Vanilla Bug Fixes** (8) - Bee fixes, End island density fix, portal suffocation check, shulker bullet self-hit fix, minecart cauldron destruction fix, POI index update, lightning generation fix
- **Memory Management** (7) - BlockPos object pool, entity BlockPos pool, predicate cache, entity lookup cache, list pre-allocation, ResourceLocation cache, ChunkPos optimization
- **Chunk System** (2) - Async chunk ticking, async villager breeding
- **World Generation** (3) - Perlin noise optimization, jigsaw placement optimization, structure pool element optimization
- **Smart Lag Compensation** (3) - Time compensation, item pickup delay compensation, potion effect compensation
- **Other Optimizations** (5) - Math optimization (sine lookup table), enderman block carry limiter, fast movement chunk loading, map rendering optimization, suffocation optimization, falling block parallel

> **Safety Guarantee**: All optimization targets are rigorously tested to ensure no vanilla game mechanics are broken

---

## System Requirements

| Component | Requirement |
|-----------|-------------|
| **Server** | [Leaves](https://github.com/LeavesMC/Leaves) or [Luminol](https://github.com/LuminolMC/Luminol) and their forks |
| **Server Version** | 1.21.8+ (Tested: 1.21.8, 1.21.10) |
| **JDK** | Java 21+ |
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
