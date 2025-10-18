<div align="center">

# 🚀 AkiAsync

**🍃 用于 Leaves 服务器的综合异步优化插件**

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Leaves](https://img.shields.io/badge/Leaves-1.21.8+-green.svg)](https://github.com/LeavesMC/Leaves)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)
[![Version](https://img.shields.io/badge/version-2.0.0--Community-orange.svg)](https://github.com/virgil698/Aki-Async/releases)

**🎉 Community Edition（社区版）**

[快速开始](#-快速开始) • [功能特性](#-功能特性) • [配置说明](#️-配置说明) • [常见问题](#-常见问题)

</div>

---

## ✨ 核心特性

### 📊 **性能表现**

- ✅ **TPS稳定20.00** - 无波动，无卡顿
- ✅ **多核均衡** - CPU占用分散到多个核心
- ✅ **零延迟** - 同tick内完成，实体行为完全一致
- ✅ **零破坏** - 完全兼容Paper/Spigot配置

### 🎯 **支持的生物（65种）**

<table>
<tr>
<td width="25%">

**专用优化**
- 村民
- 猪灵/猪灵蛮兵
- 劫掠者家族
- 唤魔者
- 烈焰人
- 守卫者
- 女巫

</td>
<td width="75%">

**统一优化（55种通用模板）**

**攻击型**：骷髅、僵尸、苦力怕、蜘蛛、末影人、幻翼、溺尸、疣猪兽、僵尸疣猪兽、流浪者、尸壳、僵尸村民、潜影贝、蠹虫、末影螨、洞穴蜘蛛

**防御型**：铁傀儡、雪傀儡

**被动型**：猪、牛、羊、鸡、兔子、蝙蝠、狐狸、炽足兽、青蛙、骆驼、山羊、哞菇

**水生型**：海豚、鱿鱼、发光鱿鱼、海龟、河豚、热带鱼、鳕鱼、鲑鱼、美西螈

**驯服/中立型**：狼、猫、马、驴、骡、羊驼、鹦鹉、北极熊、熊猫、蜜蜂、豹猫、行商羊驼

</td>
</tr>
</table>

### 🔧 **优化模块（36个）**

- ⚡ **异步AI优化**（11个）- 村民、猪灵、劫掠者、唤魔者、烈焰人、守卫者、女巫、通用模板 + 辅助
- 🏭 **方块实体优化**（6个）- 漏斗（基础+异步链）、熔炉、箱子、酿造台、信标
- 💎 **掉落物优化**（1个）- 零扫描tick节流（不破坏Paper/Spigot配置）
- 💣 **爆炸优化**（1个）- TNT 3阶段异步
- 👨‍🌾 **村民繁殖优化**（2个）- 异步繁殖检查 + 年龄降频
- 🎯 **并行优化**（3个）- 实体追踪、实体Tick、生物生成
- 💡 **光照优化**（2个）- Starlight算法、天空光缓存
- 💥 **性能优化**（3个）- 推挤、查找缓存、碰撞检测
- 🗄️ **内存优化**（3个）- 对象池、谓词缓存、列表预分配
- 🔧 **其他优化**（3个）- AI降频、寻路预算、漏斗智能跳过

---

## 📋 系统要求

| 组件 | 要求 |
|------|------|
| **服务端** | [Leaves](https://github.com/LeavesMC/Leaves) 1.21.8+ |
| **JDK** | Java 21+ |
| **内存** | 推荐4GB+ |
| **CPU** | 推荐4核心+（多线程优化）|

---

## 🚀 快速开始

### 1. 下载插件

从 [Releases](https://github.com/virgil698/Aki-Async/releases) 下载最新版本

### 2. 安装插件

```bash
# 将JAR文件放入plugins文件夹
cp Aki-Async-2.0.0.jar /path/to/server/plugins/
```

### 3. 启用Mixin支持

**修改启动脚本，添加以下参数：**

```bash
java -Dleavesclip.enable.mixin=true -Xms4G -Xmx4G -jar leaves.jar
```

⚠️ **重要**：必须添加 `-Dleavesclip.enable.mixin=true` 参数，否则插件无法工作

### 4. 启动服务器

首次启动会自动生成配置文件：`plugins/AkiAsync/config.yml`

---

## ⚙️ 配置说明

配置文件位于 `plugins/AkiAsync/config.yml`

<details>
<summary><b>📖 完整配置项（点击展开）</b></summary>

```yaml
# ==========================================
# AkiAsync 配置文件 / Configuration File
# ==========================================
# 版本 / Version: 1.1.0
# 适用服务端 / Server: Leaves 1.21.8+
# ==========================================

# 异步实体追踪器 / Async Entity Tracker
# 说明：将实体位置更新移至异步线程处理
# Description: Offload entity position tracking to async threads
entity-tracker:
  enabled: true
  # 线程池大小 / Thread pool size
  # 推荐值：CPU核心数 / Recommended: CPU core count
  thread-pool-size: 4
  # 更新间隔（tick）/ Update interval (ticks)
  update-interval-ticks: 1
  # 最大队列大小 / Maximum queue size
  max-queue-size: 1000
  # 批量处理大小 / Batch processing size
  batch-size: 50

# 异步生物生成 / Async Mob Spawning
# 说明：异步处理生物自然生成逻辑
# Description: Async natural mob spawning logic
mob-spawning:
  enabled: true
  # 刷怪笼优化 / Spawner block optimization
  spawner-optimization: true

# 实体密度控制 / Entity Density Control
# 说明：限制单区块实体数量
# Description: Limit entities per chunk
density:
  # 单区块最大实体数 / Max entities per chunk
  max-per-chunk: 80

# 寻路预算 / Pathfinding Budget
# 说明：限制每tick寻路计算量
# Description: Limit pathfinding computations per tick
pathfinding:
  # 每tick预算（0=禁用）/ Budget per tick (0=disabled)
  tick-budget: 0

# AI降频 / Brain Throttle
# 说明：静止实体降低AI更新频率
# Description: Reduce AI update frequency for stationary entities
brain:
  # 启用降频 / Enable throttling
  throttle: true
  # 降频间隔（tick）/ Throttle interval (ticks)
  throttle-interval: 10

# ==========================================
# 零延迟异步AI / Zero-Latency Async AI
# ==========================================
# 核心思路 / Core Strategy:
#   1. 主线程拍快照 / Main thread takes snapshot
#   2. 异步线程计算 / Async thread computes
#   3. 主线程写回结果 / Main thread writes back
# ==========================================
async-ai:
  # 全局超时时间（微秒）/ Global timeout (microseconds)
  # 100μs = 0.1ms
  timeout-microseconds: 100
  
  # 执行模式 / Execution mode
  mode: simple
  
  # ---------- 村民优化 / Villager Optimization ----------
  # 支持实体 / Supported: Villager, Wandering Trader
  # 技术特性 / Features: POI快照 + 职业原子占坑
  villager-optimization:
    # 启用开关 / Enable toggle
    enabled: false
    # 使用POI快照 / Use POI snapshot
    # 说明：村民需要POI系统（床、工作站）
    # Description: Villagers need POI system (beds, job sites)
    use-poi-snapshot: true
  
  # ---------- 猪灵家族优化 / Piglin Family Optimization ----------
  # 支持实体 / Supported: Piglin, PiglinBrute
  # 技术特性 / Features: UUID虚拟引用 + 物品比价 + 恐惧向量
  piglin-optimization:
    enabled: false
    use-poi-snapshot: false
    # 注视距离（格）/ Look distance (blocks)
    look-distance: 16
    # 交易距离（格）/ Barter distance (blocks)
    barter-distance: 16
  
  # ---------- 掠夺者家族优化 / Pillager Family Optimization ----------
  # 支持实体 / Supported: Pillager, Vindicator, Ravager
  # 技术特性 / Features: 安全反射写字段 + Raid检测
  # 注意 / Note: Evoker单独优化（见下方）
  pillager-family-optimization:
    enabled: false
    use-poi-snapshot: false
  
  # ---------- 高级AI实体优化 / High-AI Entity Optimization ----------
  # v1.1新增 / v1.1 New
  
  # Evoker优化 / Evoker Optimization
  # 支持实体 / Supported: Evoker
  # 计算内容 / Computations: 法术CD + 召唤Vex + 空地块检测
  evoker-optimization:
    enabled: false
  
  # Blaze优化 / Blaze Optimization  
  # 支持实体 / Supported: Blaze
  # 计算内容 / Computations: 火焰弹CD + 火柱空位检测
  blaze-optimization:
    enabled: false
  
  # Guardian优化 / Guardian Optimization
  # 支持实体 / Supported: Guardian, ElderGuardian
  # 计算内容 / Computations: 激光CD + 水下玩家扫描
  # 说明：远古守卫者自动包含（Java继承）
  # Description: Elder guardian auto-included (Java inheritance)
  guardian-optimization:
    enabled: false
  
  # Witch优化 / Witch Optimization (v2.1: safe reflection)
  # 支持实体 / Supported: Witch
  # 计算内容 / Computations: 最近玩家目标筛选
  # 技术特性 / Features: printStackTrace + no rethrow（安全异常处理）
  witch-optimization:
    enabled: false
  
  # ========== 统一AI优化 / Universal AI Optimization ==========
  # 支持实体 / Supported: 所有剩余生物（见下方列表）
  # 技术特性 / Features: 统一模板 + 配置化实体列表 + 区域数量异步
  universal-ai-optimization:
    enabled: true
    # 启用的实体列表 / Enabled entity types
    # 格式 / Format: "minecraft:entity_name"
    entities:
      # 攻击型 / Attack type (hostile)
      - "minecraft:skeleton"
      - "minecraft:zombie"
      - "minecraft:creeper"
      - "minecraft:spider"
      - "minecraft:enderman"
      - "minecraft:phantom"
      - "minecraft:drowned"
      - "minecraft:hoglin"
      - "minecraft:zoglin"
      - "minecraft:stray"
      - "minecraft:husk"
      - "minecraft:zombie_villager"
      - "minecraft:shulker"
      - "minecraft:silverfish"
      - "minecraft:endermite"
      - "minecraft:cave_spider"
      # 防御型 / Defense type (golems)
      - "minecraft:iron_golem"
      - "minecraft:snow_golem"
      # 被动型 / Passive type (animals)
      - "minecraft:pig"
      - "minecraft:cow"
      - "minecraft:sheep"
      - "minecraft:chicken"
      - "minecraft:rabbit"
      - "minecraft:bat"
      - "minecraft:fox"
      - "minecraft:strider"
      - "minecraft:frog"
      - "minecraft:camel"
      - "minecraft:goat"
      - "minecraft:mushroom_cow"
      # 水生型 / Aquatic type
      - "minecraft:dolphin"
      - "minecraft:squid"
      - "minecraft:glow_squid"
      - "minecraft:turtle"
      - "minecraft:pufferfish"
      - "minecraft:tropical_fish"
      - "minecraft:cod"
      - "minecraft:salmon"
      - "minecraft:axolotl"
      # 驯服/中立型 / Tamed/Neutral type
      - "minecraft:wolf"
      - "minecraft:cat"
      - "minecraft:horse"
      - "minecraft:donkey"
      - "minecraft:mule"
      - "minecraft:llama"
      - "minecraft:parrot"
      - "minecraft:polar_bear"
      - "minecraft:panda"
      - "minecraft:bee"
      - "minecraft:ocelot"
      - "minecraft:trader_llama"
    # 节流间隔（tick）/ Tick interval
    tick-interval: 3
    # 超时时间（微秒）/ Timeout (microseconds)
    timeout-us: 100
    # 反射写回 / Reflection writeback
    control-goal: true
    # 批量大小 / Batch size
    batch-size: 8

# ==========================================
# 并行实体Tick / Parallel Entity Tick
# ==========================================
# 说明：将实体tick分批并行处理（76%热点优化）
# Description: Batch entities for parallel tick processing
entity-tick-parallel:
  # 启用开关 / Enable toggle
  enabled: true
  # 线程数 / Thread count
  # 推荐：CPU核心数×1.5 / Recommended: CPU cores × 1.5
  threads: 6
  # 最小实体数阈值 / Minimum entities threshold
  # 说明：低于此值不启用并行（避免overhead）
  # Description: Skip parallel if entities < threshold
  min-entities: 50
  # 批量大小（实体级粒度）/ Batch size (entity-level granularity)
  # 说明：8个实体一批，任务均衡
  # Description: 8 entities per batch, balanced task size
  batch-size: 8

# ==========================================
# 性能优化套件 / Performance Optimizations
# ==========================================
# ServerCore启发 / ServerCore-inspired
servercore-optimizations:
  # 推挤优化 / Push optimization
  # 热点占比 / Hotspot: 26.72%
  push-optimization:
    enabled: true
    interval: 2
  
  # 实体查找缓存 / Entity lookup cache
  # 热点占比 / Hotspot: 23.12%
  entity-lookup-cache:
    enabled: true
    # 缓存有效期（毫秒）/ Cache duration (milliseconds)
    duration-ms: 50
  
  # 碰撞检测优化 / Collision optimization
  # 热点占比 / Hotspot: 9%
  collision-optimization:
    enabled: true
    min-movement: 0.001

# ==========================================
# 内存优化套件 / Memory Optimizations
# ==========================================
# FerriteCore启发 / FerriteCore-inspired
memory-optimizations:
  # 谓词缓存 / Predicate cache
  # 说明：缓存常用谓词，降低GC压力
  # Description: Cache common predicates, reduce GC pressure
  predicate-cache:
    enabled: true
  
  # BlockPos对象池 / BlockPos object pool
  # 说明：复用BlockPos对象，减少26%内存分配
  # Description: Reuse BlockPos objects, 26% allocation reduction
  blockpos-pool:
    enabled: true
  
  # 列表预分配 / List pre-allocation
  # 说明：预分配容量，减少ArrayList扩容
  # Description: Pre-allocate capacity, reduce ArrayList resizing
  list-prealloc:
    enabled: true
    # 默认初始容量 / Default initial capacity
    default-capacity: 32

# ==========================================
# 掉落物优化 / Item Entity Optimizations (v5.0)
# ==========================================
# 说明：掉落物是红石频控、刷石机的"第二性能瓶弹"
# Description: ItemEntity is the 2nd performance bottleneck for redstone farms
item-entity-optimizations:
  enabled: true  # v5.0启用
  # Tick节流间隔 / Tick throttle interval
  # 说明：每N tick更新一次掉落物（默认每tick）
  # Description: Update item entity every N ticks (default: every tick)
  # 推荐值 / Recommended: 5-10 (reduce 80-90% tick frequency)
  age-increment-interval: 10

# ==========================================
# 方块实体优化 / Block Entity Optimizations
# ==========================================
block-entity-optimizations:
  # 支持实体 / Supported: Furnace, Hopper, Chest, Barrel
  # 技术特性 / Features: 配方缓存+物品传输+库存扫描异步化
  zero-delay-factory-optimization:
    enabled: true  # v3.0启用
    # 启用的实体列表 / Enabled entity types
    entities:
      - "minecraft:furnace"
      - "minecraft:blast_furnace"
      - "minecraft:smoker"
      - "minecraft:hopper"
      - "minecraft:chest"
      - "minecraft:barrel"
      - "minecraft:trapped_chest"
    tick-interval: 1      # 每tick提交（生电需要0 tick延迟）
    timeout-us: 100
    control-goal: true
    batch-size: 8
  
  # 熔炉空闲优化 / Furnace idle skip
  # 说明：空闲时跳过tick（已被v3.0包含）
  # Description: Skip tick when idle (included in v3.0)
  furnace-optimization:
    enabled: false  # 关闭（被v3.0替代）

# ==========================================
# 光照优化套件 / Lighting Optimizations
# ==========================================
# Starlight/ScalableLux启发 / Starlight-inspired
lighting-optimizations:
  # 异步光照 / Async lighting
  async-lighting:
    enabled: true
    # 光照线程池大小 / Lighting thread pool size
    thread-pool-size: 2
    # 批量处理阈值 / Batch threshold
    batch-threshold: 16
  
  # 光照传播队列 / Propagation queue
  propagation-queue:
    # 分层队列（Starlight核心算法）/ Layered queue (Starlight core)
    # 说明：16层队列，按光照等级0-15分层处理
    # Description: 16-layer queue, processes by light level 0-15
    use-layered-queue: true
    # 最大传播距离 / Max propagation distance
    max-propagation-distance: 15
  
  # 天空光缓存 / Skylight cache
  skylight-cache:
    enabled: true
    # 缓存有效期（毫秒）/ Cache duration (milliseconds)
    cache-duration-ms: 100
  
  # 高级优化 / Advanced optimizations
  advanced:
    # 启用去重 / Enable deduplication
    # 说明：防止同一位置重复排队
    # Description: Prevent same position from queuing multiple times
    enable-deduplication: true
    # 动态批量大小调整 / Dynamic batch adjustment
    # 说明：根据TPS自动调整批量大小
    # Description: Auto-adjust batch size based on TPS
    dynamic-batch-adjustment: true
    # 输出高级统计 / Log advanced stats
    log-advanced-stats: false

# ==========================================
# 性能监控 / Performance Monitoring
# ==========================================
performance:
  # 调试日志 / Debug logging
  # 警告：会刷屏 / Warning: Console spam!
  debug-logging: false
  
  # 性能指标收集 / Metrics collection
  # 说明：显示异步任务执行时间和队列大小
  # Description: Show async task execution times and queue sizes
  enable-metrics: true

# ==========================================
# 配置建议 / Configuration Tips
# ==========================================
# 小型服务器 / Small servers (1-10人):
#   - thread-pool-size: 2-4
#   - threads: 4
#
# 中型服务器 / Medium servers (10-50人):
#   - thread-pool-size: 4-8
#   - threads: 6-8
#
# 大型服务器 / Large servers (50+人):
#   - thread-pool-size: 8-16
#   - threads: 8-12
#
# 提示 / Tip: 更多线程≠更好性能！从推荐值开始调整。
# More threads ≠ better performance! Start with recommended values.

```

</details>

### 快速配置

<details>
<summary><b>小型服务器（1-10人）</b></summary>

```yaml
entity-tick-parallel:
  threads: 4

async-ai:
  villager-optimization:
    enabled: true  # 推荐开启
```

</details>

<details>
<summary><b>中型服务器（10-50人）</b></summary>

```yaml
entity-tick-parallel:
  threads: 6-8

async-ai:
  villager-optimization:
    enabled: true
  piglin-optimization:
    enabled: true
  universal-ai-optimization:
    enabled: true  # 开启通用优化
```

</details>

<details>
<summary><b>大型服务器（50+人）</b></summary>

```yaml
entity-tick-parallel:
  threads: 8-12
  batch-size: 16

async-ai:
  villager-optimization:
    enabled: true
  piglin-optimization:
    enabled: true
  pillager-family-optimization:
    enabled: true
  evoker-optimization:
    enabled: true
  blaze-optimization:
    enabled: true
  guardian-optimization:
    enabled: true
  universal-ai-optimization:
    enabled: true  # 全部开启
```

</details>

---

## 🔧 常见问题

<details>
<summary><b>Q: 插件无法加载？</b></summary>

检查启动参数是否包含 `-Dleavesclip.enable.mixin=true`

</details>

<details>
<summary><b>Q: MSPT没有明显改善？</b></summary>

1. 确认实体数量 > 50（少于50不会启用并行优化）
2. 逐步开启异步AI优化（从村民开始）
3. 使用Spark查看详细性能

</details>

<details>
<summary><b>Q: 如何验证优化效果？</b></summary>

使用 [Spark](https://spark.lucko.me/) 性能分析工具：
```
/spark profiler start --timeout 5m
# 等待5分钟
/spark profiler stop
```

观察MSPT变化和CompletableFuture占比

</details>

<details>
<summary><b>Q: 插件兼容性？</b></summary>

✅ 与大部分插件兼容  
⚠️ 可能与其他Mixin插件冲突（如ISeeYou）  
📧 遇到冲突请提交Issue

</details>

<details>
<summary><b>Q: 推荐的开启顺序？</b></summary>

建议逐步开启，观察效果：
1. 先开启 `entity-tick-parallel`（并行实体Tick）
2. 再开启 `villager-optimization`（村民优化）
3. 最后开启 `universal-ai-optimization`（通用优化）

每次开启后运行一段时间，确认无问题再开启下一个。

</details>

---

## 🛠️ 开发构建

```bash
# 克隆项目
git clone https://github.com/virgil698/Aki-Async.git
cd Aki-Async

# 构建
./gradlew clean build

# 产物位于
build/libs/Aki-Async-2.0.0-SNAPSHOT.jar
```

---

## 📄 开源协议

本项目采用 [GPL-3.0 License](LICENSE) 开源协议

### 🆓 AkiAsync Community Edition（社区版）

这是**完全免费的社区版本**：
- ✅ 完全免费使用
- ✅ 允许商业用途
- ✅ 源代码开放
- ✅ 24个优化模块完整功能
- ✅ 65种生物全覆盖
- ✅ 长期维护更新

根据GPL-3.0协议：
- ✅ 可自由使用、修改、分发
- ✅ 可用于商业服务器
- ⚠️ 修改后的版本必须同样开源（GPL-3.0）
- ⚠️ 必须保留原作者版权声明

---

## 🤝 参考与致谢

本项目参考了以下优秀开源项目：

| 项目 | 贡献 |
|------|------|
| [Starlight](https://github.com/PaperMC/Starlight) / [ScalableLux](https://github.com/RelativityMC/ScalableLux) | 光照传播算法 |
| [Lithium](https://github.com/CaffeineMC/lithium) | 性能优化思路 |
| [ServerCore](https://github.com/Wesley1808/ServerCore) | 热点优化方案 |
| [FerriteCore](https://github.com/malte0811/FerriteCore) | 内存优化策略 |
| [Async](https://github.com/AxalotLDev/Async) | 异步化边界设计 |
| [Leaves](https://github.com/LeavesMC/Leaves) | Mixin支持 |

---

## ⚠️ 免责声明

- 本插件通过Mixin修改服务端核心代码，**请先在测试服务器验证**
- 不保证与所有插件100%兼容
- 如遇问题请提交 [Issue](https://github.com/virgil698/Aki-Async/issues)

---

<div align="center">

### 📮 联系方式

💬 **Issues**: [GitHub Issues](https://github.com/virgil698/Aki-Async/issues)  
📧 **Email**: virgil698@sky233.top  
💬 **Discord**: xiaokong23357(virgil698)

---

**Edition**: Community（社区版） | **Version**: 2.0.0 | **License**: GPL-3.0 | **Author**: [Virgil](https://github.com/virgil698)

**如果觉得有用，请给个 ⭐ Star 支持开发！**

Made with ❤️ for Minecraft Server Optimization

</div>
