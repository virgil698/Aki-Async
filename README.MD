<div align="center">

# 🚀 AkiAsync
> 基于 Mixin 实现的异步优化插件，皆在为了不改变核心特性下提高 Leaves/Luminol 以及其分支的性能提升
> 该分支添加了许多实践性配置标识 以及减弱异步效果保证稳定性！
> 使用前 请先清除原配置文件

**🍃 Leaves / Luminol 异步优化插件**

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Leaves](https://img.shields.io/badge/Leaves-1.21.8+-green.svg)](https://github.com/LeavesMC/Leaves)
[![Luminol](https://img.shields.io/badge/Luminol-1.21.8+-purple.svg)](https://github.com/LuminolMC/Luminol)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)

![Logo](aki-xixi.png)

**🎉 Community Edition（社区版）**

[快速开始](#-快速开始) • [命令列表](#-命令列表) • [配置说明](#️-配置说明) • [常见问题](#-常见问题)

</div>

---

## ✨ 核心特性

### 📊 **性能表现**
- ✅ **TPS优化稳步提升** - 减轻对主线程的压力，MSPT显著降低
- ✅ **多线程均衡** - CPU占用分散到多个核心/线程，支持工作窃取模式
- ✅ **零延迟快照** - 快照-计算-写回模式，同tick内完成，无延迟
- ✅ **完全兼容** - 保持所有原版特性，兼容Paper/Spigot/Leaves/folia/luminol配置

### 🎯 **支持生物（65种+）**
- **专用优化**：村民、猪灵、劫掠者、唤魔者、烈焰人、守卫者、女巫（7种）
- **通用优化**：骷髅、僵尸、苦力怕、蜘蛛、末影人、铁傀儡、雪傀儡、猪、牛、羊、鸡等（58种+）

### 🔧 **优化模块（42个+）**
- ⚡ **异步AI优化**（11个）- 村民、猪灵、劫掠者、唤魔者、烈焰人、守卫者、女巫、通用模板
- 🏭 **方块实体优化**（6个）- 漏斗、熔炉、箱子、酿造台、信标、零延迟工厂模式
- 💣 **TNT爆炸优化**（1个）- 三阶段异步：快照→计算→应用，支持大规模同时爆炸
- 🎯 **并行处理优化**（4个）- 实体追踪、实体Tick、生物生成、区块Tick
- 💡 **光照系统优化**（3个）- Starlight算法、分层队列、天空光缓存
- 🗺️ **结构定位优化**（5个）- 异步定位、地图生成、海豚寻宝、箱子地图、预计算缓存
- 📦 **数据包加载优化**（2个）- 多线程ZIP文件处理，智能缓存机制
- 💥 **性能系统优化**（4个）- 推挤优化、实体查找缓存、碰撞检测、寻路预算
- 🗄️ **内存管理优化**（4个）- BlockPos对象池、谓词缓存、列表预分配、集合优化
- 🔧 **其他优化**（2个）- 寻路预算、区块保存异步化

---

## 📋 系统要求

| 组件 | 要求 |
|------|------|
| **服务端** | [Leaves](https://github.com/LeavesMC/Leaves) 或 [Luminol](https://github.com/LuminolMC/Luminol) 以及其分支 |
| **服务端版本** | 1.21.8+ |
| **JDK** | Java 21＋ |
| **内存** | 推荐6GB+ |
| **CPU** | 推荐4核心+(虽多线程优化，但依旧以单核性能为主)|

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
java -Dleavesclip.enable.mixin=true -Xms4G -Xmx4G -jar server.jar
```

⚠️ **重要**：必须添加 `-Dleavesclip.enable.mixin=true` 参数，否则插件无法工作

### 4. 启动服务器

首次启动会自动生成配置文件：`plugins/AkiAsync/config.yml`

---

## 📋 命令列表

<details>
<summary><b>💡 /aki-reload - 热重载配置</b></summary>

**功能**：无需重启服务器即可重新加载配置并应用更改

**权限**：`akiasync.reload`（默认仅OP可用）

**⚠️ 重要警告**：
- **非必要情况下不推荐使用热重载**
- **修改优化类配置后更推荐关服重启**
- 热重载可能一些神秘问题
- 生产环境建议维护时间重启服务器

**使用方法**：
```bash
/aki-reload
```

**效果**：
- ✅ 重新加载 `config.yml` 配置
- ✅ 平滑重启所有线程池（不丢失任务）
- ✅ 清除所有内部缓存
- ✅ 重置性能统计计数器
- ✅ 更新所有模块配置引用

**示例**：
```yaml
# 修改 config.yml
entity-tick-parallel:
  threads: 8  # 从 4 改为 8

# 执行命令
/aki-reload
# 输出：§a[AkiAsync] Configuration hot-reloaded, thread pools smoothly restarted.
```

**建议使用场景**：
- 🟢 调试日志开关切换
- 🟢 性能监控参数调整
- 🟡 线程数量微调（谨慎使用）
- 🔴 避免：启用/禁用核心优化功能

</details>

<details>
<summary><b>🔍 /aki-debug <true|false> - 调试模式</b></summary>

**功能**：动态开启/关闭调试日志输出

**权限**：`akiasync.debug`（默认仅OP可用）

**使用方法**：
```bash
/aki-debug true   # 开启调试模式
/aki-debug false  # 关闭调试模式
```

**别名**：支持 `on/off`、`enable/disable`

**效果**：
- ✅ 实时切换调试日志开关
- ✅ 自动保存到配置文件
- ✅ 触发配置重载以应用更改
- ✅ 控制性能指标输出（每60秒）

**使用场景**：
- 🔍 性能调试时开启，查看详细指标
- 🧹 日常使用时关闭，避免控制台刷屏
- 📊 需要监控线程池状态时开启
- 🔥 推荐搭配Spark插件使用

**示例**：
```bash
# 开启调试模式
/aki-debug true
# 输出：§a[AkiAsync] Debug logging enabled successfully!
# 输出：§a[AkiAsync] Configuration reloaded to apply debug changes.

# 关闭调试模式  
/aki-debug false
# 输出：§a[AkiAsync] Debug logging disabled successfully!
```

</details>

<details>
<summary><b>📋 /aki-version - 版本信息</b></summary>

**功能**：显示插件详细信息和系统环境

**权限**：`akiasync.version`（默认仅OP可用）

**使用方法**：
```bash
/aki-version   # 显示版本信息
```

**显示内容**：
- ✅ 插件名称、版本号、作者
- ✅ 服务器类型和版本（Leaves/Paper/Spigot）
- ✅ Minecraft 版本
- ✅ Java 版本
- ✅ 操作系统信息
- ✅ 当前启用的优化功能状态

**使用场景**：
- 🐛 反馈问题时提供版本信息
- 📊 检查当前启用的优化功能
- 🔍 确认服务器环境配置
- 📝 生成技术支持报告

**示例**：
```bash
# 查看版本信息
/aki-version

# 输出示例：
# [AkiAsync] ========================================
# [AkiAsync] Plugin: AkiAsync
# [AkiAsync] Version: 3.0.0-SNAPSHOT
# [AkiAsync] Authors: Virgil
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
# [AkiAsync]   Async Lighting: ON
# [AkiAsync]   Chunk Tick Async: ON
# [AkiAsync]   Brain Throttle: ON
# [AkiAsync] ========================================
```

**提示**：
- 💡 反馈 Bug 时建议先执行此命令，并附上输出截图
- 💡 可用于快速检查配置是否生效

</details>

---

## ⚙️ 配置说明

配置文件位于 `plugins/AkiAsync/config.yml`

<details>
<summary><b>📖 完整配置项（点击展开）</b></summary>

```yaml
# ==========================================
# AkiAsync 配置文件 / Configuration File
# ==========================================

# 配置版本 / Configuration version
# 当前版本 / Current version: 5
# 说明：当前配置版本为5，如果配置版本大于或小于5，则需要更新配置文件
# Description: The current configuration version is 5, if the configuration version is greater than 5, you need to update the configuration file
version: 5

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

# AI降频 / Brain Throttle
# 说明：静止实体降低AI更新频率
# Description: Reduce AI update frequency for stationary entities
# 注意：UniversalAI会自动感知降频状态，保持一致性
# Note: UniversalAI respects throttle state automatically for consistency
brain:
  # 启用降频 / Enable throttling
  throttle: true
  # 降频间隔（tick）/ Throttle interval (ticks)
  throttle-interval: 10

# ==========================================
# 零延迟异步AI / Zero-Latency Async AI
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
servercore-optimizations:
  # 推挤优化 / Push optimization
  push-optimization:
    enabled: true
    interval: 2
  
  # 实体查找缓存 / Entity lookup cache
  entity-lookup-cache:
    enabled: true
    # 缓存有效期（毫秒）/ Cache duration (milliseconds)
    duration-ms: 50
  
  # 碰撞检测优化 / Collision optimization
  # 说明：优化实体碰撞检测，但保留重要状态检测
  # Description: Optimize entity collision detection while preserving important status detection
  # 保护的状态 / Protected status:
  #   - 细雪冰冻效果 / Powder snow freezing effect
  #   - 岩浆火焰伤害 / Lava and fire damage  
  #   - 掉落物销毁检测 / Item entity destruction detection
  collision-optimization:
    enabled: true
    # 最小移动阈值，低于此值时跳过碰撞检测 / Minimum movement threshold for skipping collision detection
    min-movement: 0.001

# ==========================================
# 内存优化套件 / Memory Optimizations
# ==========================================
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
  # 说明：每N tick更新一次掉落物（空区域悬浮时）
  # Description: Update item entity every N ticks (when in empty region)
  # 推荐值 / Recommended: 5-10 (reduce 80-90% tick frequency)
  age-increment-interval: 10
  # 最少附近物品数 / Minimum nearby items for merge
  # 说明：附近物品少于此值时跳过tryToMerge检查
  # Description: Skip tryToMerge when nearby items < threshold
  min-nearby-items: 3

# ==========================================
# 村民繁殖优化 / Villager Breed Optimization (v8.0)
# ==========================================
# 说明：异步繁殖检查，降低村门+村民+床遍历开销
# Description: Async breed check to reduce village scan overhead
villager-breed-optimization:
  # 启用异步繁殖检查 / Enable async breed check
  async-villager-breed: true
  # 线程池大小 / Thread pool size (breed check threads)
  threads: 4
  # 启用年龄降频 / Enable age throttle (skip idle villagers for 20 ticks)
  age-throttle: true
  # 繁殖检查间隔 / Breed check interval (ticks)
  check-interval: 5

# ==========================================
# 蜜蜂修复 / Bee Fix
# ==========================================
# 说明：修复多个蜜蜂相关的Minecraft bug
# Description: Fix multiple Minecraft bugs related to bees
# 基于BeeFix项目 / Based on BeeFix project
bee-fix:
  # 启用开关 / Enable toggle
  enabled: true
  # 修复的bug列表 / Fixed bugs list:
  #   - MC-168329: 蜜蜂拒绝在末地/下界离开蜂巢
  #   - MC-190042: 蜜蜂在不寻路时会掉落
  #   - MC-206401: 蜜蜂有西北方向的漫游偏向
  #   - MC-229321: 蜜蜂可能消失
  #   - MC-234364: 蜜蜂死亡动画只翻转90度
  #   - MC-248332: 蜜蜂可以破坏海龟蛋
  #   - MC-255743: 蜜蜂寻路系统可能导致性能问题

# ==========================================
# TNT爆炸优化 / TNT Explosion Optimization (v7.0)
# ==========================================
# 说明：异步计算爆炸抗性和射线可见性，优化"大量TNT同时爆炸"场景
# Description: Async blast resistance & raycast for massive TNT explosions
# 
# 支持实体 / Supported Entities:
#   - minecraft:tnt (TNT方块)
#   - minecraft:tnt_minecart (TNT矿车)
#   - minecraft:wither_skull (凋灵头颅)
tnt-explosion-optimization:
  # 启用开关 / Enable toggle
  enabled: true
  # 启用的爆炸实体白名单 / Enabled explosion entities
  # 说明：只对这些实体的爆炸进行异步计算优化，其他爆炸走原版
  # Description: Only async optimize explosions from these entities
  entities:
    - "minecraft:tnt"           # TNT方块
    - "minecraft:tnt_minecart"  # TNT矿车（高频刷石机）
    - "minecraft:wither_skull"  # 凋灵头颅（威力8+刷石机）
  # 线程池大小 / Thread pool size
  # 推荐值 / Recommended: CPU核心数×1.5
  threads: 6

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
# 区块Tick异步化 / Chunk Tick Async
# ==========================================
# 说明：将区块Tick移至ForkJoinPool异步执行，失败回退主线程同步
# Description: Offload chunk tick to ForkJoinPool async, fallback to main thread on timeout
chunk-tick-async:
  # 启用开关 / Enable toggle
  enabled: false
  # 线程数 / Thread count
  # 推荐值 / Recommended: 4
  threads: 4
  # 超时时间（微秒）/ Timeout (microseconds)
  # 说明：超时立即回退主线程同步，保证最坏TPS
  # Description: Fallback to sync on timeout, ensure worst-case TPS
  timeout-us: 200

# ==========================================
# 结构查找异步化 / Structure Location Async
# ==========================================
# 说明：异步化/locate命令、探险家地图、海豚寻宝、箱子探险地图的结构查找
# Description: Async structure finding for /locate command, explorer maps, dolphin treasure hunt, chest maps
structure-location-async:
  # 启用开关 / Enable toggle
  enabled: true
  
  # 线程池大小 / Thread pool size
  # 推荐值 / Recommended: 2-4 (结构查找CPU密集)
  threads: 3
  
  # /locate命令异步化 / Async /locate command
  locate-command:
    enabled: true
    # 搜索半径（区块）/ Search radius (chunks)
    search-radius: 100
    # 跳过已知结构 / Skip known structures
    skip-known-structures: false
  
  # 村民交易探险家地图异步化 / Async villager trade explorer maps
  villager-trade-maps:
    enabled: true
    # 支持的交易类型 / Supported trade types
    trade-types:
      - "minecraft:ocean_monument_map"    # 海底神殿地图
      - "minecraft:woodland_mansion_map"  # 林地府邸地图
      - "minecraft:buried_treasure_map"   # 埋藏宝藏地图
    # 地图生成超时（秒）/ Map generation timeout (seconds)
    generation-timeout-seconds: 30
  
  # 海豚寻宝异步化 / Async dolphin treasure hunt
  dolphin-treasure-hunt:
    enabled: true
    # 搜索半径（区块）/ Search radius (chunks)
    search-radius: 50
    # 寻宝间隔（tick）/ Hunt interval (ticks)
    hunt-interval: 100
  
  # 箱子探险地图异步化 / Async chest exploration maps
  chest-exploration-maps:
    enabled: true
    # 支持的战利品表 / Supported loot tables
    loot-tables:
      - "minecraft:chests/shipwreck_map"
      - "minecraft:chests/underwater_ruin_big"
      - "minecraft:chests/underwater_ruin_small"
    # 地图生成概率保持不变 / Keep original map generation probability
    preserve-probability: true
  
  # 算法优化配置 / Algorithm Optimization Configuration
  algorithm-optimization:
    # 启用算法优化 / Enable algorithm optimization
    enabled: true
    
    # 搜索模式 / Search pattern
    # 可选值 / Options: "spiral"(螺旋), "layered"(分层), "hybrid"(混合)
    search-pattern: "hybrid"
    
    # 启用结构缓存 / Enable structure caching
    caching:
      enabled: true
      # 缓存最大条目数 / Max cache entries
      max-size: 1000
      # 缓存过期时间（分钟）/ Cache expiration (minutes)
      expiration-minutes: 30
    
    # 启用预计算 / Enable precomputation
    precomputation:
      enabled: true
    
    # 生物群系感知搜索 / Biome-aware search
    biome-aware-search:
      enabled: true

# ==========================================
# 数据包加载优化 / DataPack Loading Optimization
# ==========================================
# 说明：基于QuickPack优化思路，加速数据包和资源包的zip文件加载
# Description: Based on QuickPack optimization, accelerate datapack and resourcepack zip file loading
datapack-optimization:
  # 启用开关 / Enable toggle
  enabled: true
  
  # 文件加载线程数 / File loading threads
  # 推荐值 / Recommended: 4-8 (根据CPU核心数调整)
  file-load-threads: 4
  
  # Zip处理线程数 / Zip processing threads
  # 推荐值 / Recommended: 2-4 (I/O密集型)
  zip-process-threads: 2
  
  # 批处理大小 / Batch size
  # 说明：每批处理的文件数量，影响内存使用和性能
  # Description: Number of files per batch, affects memory usage and performance
  batch-size: 100
  
  # 缓存过期时间（分钟）/ Cache expiration (minutes)
  # 说明：文件系统和文件内容的缓存时间
  # Description: Cache time for file systems and file contents
  cache-expiration-minutes: 30
  
  # 调试模式 / Debug mode
  # 说明：启用详细的数据包加载日志
  # Description: Enable detailed datapack loading logs
  debug-enabled: false

# ==========================================
# 高级并发优化 / Advanced Concurrency Optimizations
# ==========================================
# 说明：高级并发和内存优化技术，提供额外的性能提升
# Description: Advanced concurrency and memory optimization techniques for additional performance gains
advanced-optimizations:
  # 总开关 / Master switch
  # 说明：控制所有高级并发优化的启用/禁用
  # Description: Controls enable/disable of all advanced concurrency optimizations
  enabled: true
  
  # Virtual Thread支持 / Virtual Thread support
  # 说明：Java 21+环境下使用Virtual Thread提升并发性能
  # Description: Use Virtual Threads for better concurrency on Java 21+
  # 注意：仅在Java 21+环境下有效，会自动降级到传统线程
  # Note: Only effective on Java 21+, automatically falls back to regular threads
  virtual-threads: true
  
  # 工作窃取调度器 / Work-stealing scheduler
  # 说明：使用工作窃取模式进行批量任务处理，提升多核CPU利用率
  # Description: Use work-stealing pattern for batch processing, improves multi-core utilization
  work-stealing: true
  
  # BlockPos缓存预计算 / BlockPos cache pre-computation
  # 说明：预计算常用范围的BlockPos，减少重复计算开销
  # Description: Pre-compute common BlockPos ranges to reduce calculation overhead
  blockpos-cache: true
  
  # 优化集合类 / Optimized collections
  # 说明：使用高性能集合类替代原生集合，减少内存分配和提升访问速度
  # Description: Use high-performance collections instead of native ones
  optimized-collections: true

# ==========================================
# 性能监控 / Performance Monitoring
# ==========================================
performance:
  # 统一调试日志开关 / Unified debug logging switch
  # 说明：控制所有模块的调试输出（TNT、结构定位等）
  # Description: Controls debug output for all modules (TNT, structure location, etc.)
  # 警告：会刷屏 / Warning: Console spam!
  debug-logging: false
  
  # 性能指标收集 / Metrics collection
  # 说明：显示异步任务执行时间和队列大小
  # Description: Show async task execution times and queue sizes
  enable-metrics: true

# ==========================================
# 指令配置 / Command Configuration
# ==========================================
commands:
  # /aki-reload 指令配置 / /aki-reload command configuration
  reload:
    # 启用二次确认 / Enable confirmation
    # 说明：启用后，首次执行指令会发出警告，需要再次执行才能重载
    # Description: When enabled, first execution shows warning, requires second execution to reload
    require-confirmation: true
    # 确认超时时间（秒）/ Confirmation timeout (seconds)
    # 说明：超过此时间后需要重新确认
    # Description: After this time, confirmation is reset and warning will show again
    confirmation-timeout: 30

# 提示 / Tip: 更多线程≠更好性能！从推荐值开始调整。
# More threads ≠ better performance! Start with recommended values.

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
/spark profiler start --timeout 60
# 等待5分钟
/spark profiler stop
```

观察MSPT变化和CompletableFuture占比

如果出现一些性能问题请提交Issue并提交您的Spark报告链接或文件以供排查问题

</details>

<details>
<summary><b>Q: 插件兼容性？</b></summary>

- ✅ 与大部分插件兼容
- 📧 遇到冲突请提交Issue

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
