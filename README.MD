<div align="center">

# 🚀 AkiAsync
> 基于 Mixin 实现的异步优化插件，皆在为了不改变核心特性下提高 Leaves/Luminol 以及其分支的性能提升

**🍃 Leaves / Luminol 异步优化插件**

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Leaves](https://img.shields.io/badge/Leaves-1.21.8+-green.svg)](https://github.com/LeavesMC/Leaves)
[![Luminol](https://img.shields.io/badge/Luminol-1.21.8+-purple.svg)](https://github.com/LuminolMC/Luminol)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)

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
> **安全保证**：所有优化都经过严格测试，确保不破坏任何原版游戏机制

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
# [AkiAsync] Version: 9.0.0-SNAPSHOT
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
<summary><b>📖 config.yml 完整配置项（点击展开）</b></summary>

```yaml
# ==========================================
# AkiAsync 配置文件 / Configuration File
# ==========================================

# 配置版本 / Configuration version
# 当前版本 / Current version: 9
# 说明：当前配置版本为9，如果配置版本不等于9，则需要更新配置文件
# Description: The current configuration version is 9, if the configuration version is not equal to 9, you need to update the configuration file
version: 9

# ==========================================
# 实体系统优化 / Entity System Optimization
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
  # 说明：追踪任务的缓冲队列大小，队列满时会降级为同步执行
  # 推荐值：1000-20000，高负载服务器建议设置更大
  # Description: Buffer queue size for tracking tasks, falls back to sync execution when full
  # Recommended: 1000-20000, higher values for high-load servers
  max-queue-size: 10000

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
  # 启用密度控制 / Enable density control
  enabled: true
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
  
  # ---------- 异步寻路 / Async Pathfinding ----------
  # 技术来源：Pufferfish异步寻路技术
  # 说明：将A*寻路算法从主线程移到异步线程池
  # Description: Move A* pathfinding algorithm from main thread to async thread pool
  async-pathfinding:
    # 启用异步寻路 / Enable async pathfinding
    enabled: true
    # 寻路线程池大小 / Pathfinding thread pool size
    # 推荐：CPU核心数×1.5 / Recommended: CPU cores × 1.5
    max-threads: 8
    # 线程存活时间（秒）/ Thread keep-alive time (seconds)
    keep-alive-seconds: 60
    # 最大队列大小 / Maximum queue size
    max-queue-size: 500
    # 寻路超时时间（毫秒）/ Pathfinding timeout (milliseconds)
    timeout-ms: 50

  # ========== 统一AI优化 / Universal AI Optimization ==========
  # 支持实体 / Supported: 所有剩余生物（见entities.yml）
  # 技术特性 / Features: 统一模板 + 配置化实体列表 + 区域数量异步 + 动态激活大脑(DAB)
  universal-ai-optimization:
    enabled: true
    # 实体列表配置文件 / Entity list configuration file
    # 默认：plugins/AkiAsync/entities.yml
    # Default: plugins/AkiAsync/entities.yml
    entities-config-file: "entities.yml"
    # 节流间隔（tick）/ Tick interval
    tick-interval: 3
    # 超时时间（微秒）/ Timeout (microseconds)
    timeout-us: 100
    # 反射写回 / Reflection writeback
    control-goal: true
    # 批量大小 / Batch size
    batch-size: 8
    # 动态激活大脑 / Dynamic Activation of Brain (DAB)
    # 说明：基于距离的渐进式实体tick节流，距离越远tick频率越低
    # Description: Distance-based progressive entity tick throttling
    # 技术来源：Pufferfish优化技术
    dynamic-activation:
      # 启用DAB / Enable DAB
      enabled: true
      # 开始减速距离（格）/ Start distance for slowdown (blocks)
      # 说明：玩家此距离内的实体全速tick
      # Description: Entities within this distance tick at full speed
      start-distance: 12
      # 激活距离修正系数 / Activation distance modifier
      # 说明：控制减速梯度，值越大减速越慢
      # Description: Controls slowdown gradient, higher = slower deceleration
      activation-dist-mod: 8
      # 最大tick间隔 / Maximum tick interval
      # 说明：最远距离实体的最大tick间隔
      # Description: Maximum tick interval for farthest entities
      max-tick-interval: 20

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
# 实体节流 / Entity Throttling
# ==========================================
# 技术来源：Kaiiju实体节流技术
# Based on: Kaiiju entity throttling
# 说明：按实体类型限制数量，超过限制后节流或移除
# Description: Limit entity count by type, throttle or remove when exceeded
entity-throttling:
  # 启用开关 / Enable toggle
  enabled: true
  # 配置文件路径 / Configuration file path
  # 默认：plugins/AkiAsync/throttling.yml
  # Default: plugins/AkiAsync/throttling.yml
  config-file: "throttling.yml"
  # 检查间隔（tick）/ Check interval (ticks)
  # 说明：每N tick检查一次实体数量
  # Description: Check entity count every N ticks
  check-interval: 100
  # 节流tick间隔 / Throttle tick interval
  # 说明：超过limit后，实体每N tick更新一次
  # Description: Update entities every N ticks when limit exceeded
  throttle-interval: 3
  # 移除批量大小 / Removal batch size
  # 说明：每次检查最多移除N个实体
  # Description: Remove at most N entities per check
  removal-batch-size: 10

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
  
  # 领地保护集成 / Land Protection Integration (v3.2.7+)
  # 说明：集成领地保护插件，防止TNT在受保护区域破坏方块
  # Description: Integrate with land protection plugins to prevent TNT from destroying blocks in protected areas
  land-protection:
    # 启用领地保护检测 / Enable land protection check
    # 注意：需要安装Residence、Dominion、WorldGuard或Lands插件才能生效
    # Note: Requires Residence, Dominion, WorldGuard or Lands plugin to be installed
    enabled: true
    # 支持的插件 / Supported plugins:
    # - Residence: 检查领地的 "tnt" 标志 / Checks "tnt" flag
    # - Dominion: 检查领地的 "TNT_EXPLODE" 标志 / Checks "TNT_EXPLODE" flag
    # - WorldGuard: 检查区域的 "tnt" 标志 / Checks region "tnt" flag (StateFlag)
    # - Lands: 检查领地的 "block_ignite" 角色标志 / Checks land "block_ignite" role flag
    # 缓存时间（毫秒）/ Cache duration (milliseconds)
    # 说明：缓存领地检测结果以提升性能
    # Description: Cache land protection check results for performance
    cache-duration-ms: 5000
    # 最大缓存数量 / Maximum cache size
    max-cache-size: 1000

# ==========================================
# 方块实体优化 / Block Entity Optimizations
# ==========================================
block-entity-optimizations:
  # 支持实体 / Supported: Furnace, Hopper, Chest, Barrel（见entities.yml）
  # 技术特性 / Features: 配方缓存+物品传输+库存扫描异步化
  zero-delay-factory-optimization:
    enabled: true  # v3.0启用
    # 实体列表配置文件 / Entity list configuration file
    # 默认：plugins/AkiAsync/entities.yml
    # Default: plugins/AkiAsync/entities.yml
    # 注意：使用与universal-ai-optimization相同的配置文件
    # Note: Uses the same configuration file as universal-ai-optimization
    entities-config-file: "entities.yml"
    tick-interval: 1      # 每tick提交（生电需要0 tick延迟）
    timeout-us: 100
    control-goal: true
    batch-size: 8
  
  # 方块实体并行化tick / Block Entity Parallel Tick (v9.0)
  # 说明：将方块实体tick分批并行处理，参考C3H6N6O6设计
  # Description: Parallel block entity tick processing, inspired by C3H6N6O6
  # 技术特性 / Features:
  #   - 批量并行处理 / Batch parallel processing
  #   - 容器保护机制 / Container protection
  #   - Lithium睡眠方块实体兼容 / Lithium sleeping block entity compatibility
  #   - Folia区域化自动禁用 / Auto-disable in Folia regionized mode
  parallel-tick:
    # 启用开关 / Enable toggle
    # 注意：Folia/Luminol环境下会自动禁用（区域并行已优化）
    # Note: Auto-disabled in Folia/Luminol (regionized parallel already optimized)
    enabled: true
    # 最小方块实体数阈值 / Minimum block entities threshold
    # 说明：低于此值不启用并行（避免overhead）
    # Description: Skip parallel if block entities < threshold
    min-block-entities: 50
    # 批量大小 / Batch size
    # 说明：每批处理的方块实体数量
    # Description: Number of block entities per batch
    batch-size: 16
    # 容器保护 / Container protection
    # 说明：容器类方块实体（箱子、漏斗等）在主线程执行
    # Description: Container block entities (chest, hopper, etc.) tick on main thread
    # 原因：避免并发访问导致物品丢失或复制
    # Reason: Prevent item loss or duplication from concurrent access
    protect-containers: true
    # 超时时间（毫秒）/ Timeout (milliseconds)
    # 说明：并行执行超时后降级到同步执行
    # Description: Fallback to sync execution on timeout
    timeout-ms: 50

recipe-optimization:
  # FastFurnace熔炉配方缓存 / FastFurnace recipe cache
  # 说明：缓存熔炉配方查找结果，避免每tick扫描整个配方表
  # 性能提升：大型模组包环境下熔炉TPS提升30-50%
  # Description: Cache furnace recipe lookups to avoid scanning entire recipe registry every tick
  # Performance: 30-50% TPS improvement in large modpack environments
  furnace-recipe-cache:
    # 启用熔炉配方缓存 / Enable furnace recipe cache
    enabled: true
    
    # 缓存大小 / Cache size
    # 说明：每个熔炉的配方缓存数量
    # Description: Number of cached recipes per furnace
    cache-size: 100
    
    # 应用到高炉 / Apply to blast furnace
    # 说明：是否对高炉应用配方缓存优化
    # Description: Whether to apply recipe cache to blast furnace
    apply-to-blast-furnace: true
    
    # 应用到烟熏炉 / Apply to smoker
    # 说明：是否对烟熏炉应用配方缓存优化
    # Description: Whether to apply recipe cache to smoker
    apply-to-smoker: true
    
    # 修复燃烧时间Bug / Fix burn time bug
    # 说明：修复原版燃烧时间>32767时的序列化bug
    # Description: Fix vanilla burn time serialization bug when >32767
    fix-burn-time-bug: true
  
  # FastWorkbench工作台配方缓存 / FastWorkbench crafting recipe cache
  # 说明：缓存工作台配方，优化批量合成性能
  # 警告：此功能处于实验阶段，默认禁用，需要充分测试后再启用
  # Description: Cache crafting table recipes, optimize batch crafting performance
  # Warning: This feature is experimental, disabled by default, enable after thorough testing
  crafting-recipe-cache:
    # 启用工作台配方缓存 / Enable crafting recipe cache
    # 警告：实验性功能，可能与某些模组不兼容
    # Warning: Experimental feature, may be incompatible with some mods
    enabled: false
    
    # 缓存大小 / Cache size
    # 说明：工作台配方缓存数量
    # Description: Number of cached crafting recipes
    cache-size: 200
    
    # 优化批量合成 / Optimize batch crafting
    # 说明：优化shift+点击批量合成，避免卡顿
    # 原版：O(i×s)次配方匹配，优化后：1次匹配
    # Description: Optimize shift+click batch crafting to avoid freezing
    # Vanilla: O(i×s) recipe matches, optimized: 1 match
    optimize-batch-crafting: true
    
    # 减少网络流量 / Reduce network traffic
    # 说明：仅在必要时发送数据包，减少网络开销
    # Description: Send packets only when necessary to reduce network overhead
    reduce-network-traffic: true

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
# 安全种子保护 / Secure Seed Protection
# ==========================================
# 说明：使用1024位加密种子保护世界生成，防止种子破解
# Description: Use 1024-bit encrypted seed to protect world generation from seed cracking
secure-seed:
  # 启用安全种子 / Enable secure seed
  enabled: false
  
  # 保护结构生成 / Protect structure generation
  # 说明：保护村庄、要塞、遗迹等结构的生成种子
  # Description: Protect generation seeds for villages, strongholds, monuments, etc.
  protect-structures: true
  
  # 保护矿石生成 / Protect ore generation
  # 说明：保护钻石、金矿等矿石的生成种子
  # Description: Protect generation seeds for diamonds, gold, etc.
  protect-ores: true
  
  # 保护史莱姆区块 / Protect slime chunks
  # 说明：保护史莱姆区块的生成种子
  # Description: Protect slime chunk generation seeds
  protect-slimes: true
  
  # 种子位数 / Seed bits
  # 说明：加密种子的位数，支持1024/2048/4096
  # 推荐：1024（平衡安全性和性能）
  # Description: Encrypted seed bit length, supports 1024/2048/4096
  # Recommended: 1024 (balance security and performance)
  seed-bits: 1024

# ==========================================
# 性能监控 / Performance Monitoring
# ==========================================
performance:
  # 统一调试日志开关 / Unified debug logging switch
  # 说明：控制所有模块的调试输出（TNT、结构定位、SecureSeed等）
  # Description: Controls debug output for all modules (TNT, structure location, SecureSeed, etc.)
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

<details>
<summary><b>📖 entities.yml 完整配置项（点击展开）</b></summary>

```yml
# ==========================================
# AkiAsync 实体列表配置 / Entity List Configuration
# ==========================================

# 实体ID格式：minecraft:entity_name
# Entity ID format: minecraft:entity_name

# ==========================================
# 统一AI优化实体列表 / Universal AI Optimization Entity List
# ==========================================
universal-ai-entities:
  # 攻击型生物 / Hostile Mobs
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
  - "minecraft:wither_skeleton"
  - "minecraft:witch"
  - "minecraft:slime"
  - "minecraft:magma_cube"
  
  # 防御型生物 / Defense Mobs (Golems)
  - "minecraft:iron_golem"
  - "minecraft:snow_golem"
  
  # 被动型生物 / Passive Mobs (Animals)
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
  - "minecraft:sniffer"
  
  # 水生生物 / Aquatic Mobs
  - "minecraft:dolphin"
  - "minecraft:squid"
  - "minecraft:glow_squid"
  - "minecraft:turtle"
  - "minecraft:pufferfish"
  - "minecraft:tropical_fish"
  - "minecraft:cod"
  - "minecraft:salmon"
  - "minecraft:axolotl"
  
  # 驯服/中立型生物 / Tamed/Neutral Mobs
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
  
  # 村民相关 / Villager Related
  # 注意：村民和流浪商人有专门的优化模块，通常不需要在这里启用
  # Note: Villagers and wandering traders have dedicated optimization modules
  # - "minecraft:villager"
  # - "minecraft:wandering_trader"

# ==========================================
# 零延迟工厂实体列表 / Zero-Delay Factory Entity List
# ==========================================
# 说明：这些方块实体使用零延迟工厂模式优化
# Description: These block entities use zero-delay factory pattern optimization
zero-delay-factory-entities:
  - "minecraft:hopper"
  - "minecraft:furnace"
  - "minecraft:blast_furnace"
  - "minecraft:smoker"
  - "minecraft:brewing_stand"
  - "minecraft:beacon"
  - "minecraft:chest"
  - "minecraft:trapped_chest"
  - "minecraft:barrel"
  - "minecraft:shulker_box"
  - "minecraft:ender_chest"
  - "minecraft:dropper"
  - "minecraft:dispenser"

```

</details>

<details>
<summary><b>📖 entities.yml 完整配置项（点击展开）</b></summary>

```yml
# ==========================================
# AkiAsync 实体节流配置 / Entity Throttling Configuration
# ==========================================

# 配置说明 / Configuration Guide:
# limit: 实体数量限制，超过该数量后实体每3 tick才会被更新一次
#        Entity count limit, entities will be updated every 3 ticks when exceeded
# removal: 实体移除限制，超过该数量后最先生成的实体会被逐步移除
#          Entity removal limit, oldest entities will be gradually removed when exceeded

# 注意事项 / Notes:
# 1. limit应该小于removal（limit < removal）
# 2. 设置为-1表示不限制该项
# 3. 实体ID格式：minecraft:entity_name
# 4. 移除优先级：最早生成的实体优先被移除

# ==========================================
# 攻击型生物 / Hostile Mobs
# ==========================================
minecraft:zombie:
  limit: 500      # 超过500只僵尸后开始节流
  removal: 1000   # 超过1000只僵尸后开始移除

minecraft:skeleton:
  limit: 400
  removal: 800

minecraft:creeper:
  limit: 300
  removal: 600

minecraft:spider:
  limit: 300
  removal: 600

minecraft:enderman:
  limit: 200
  removal: 400

minecraft:phantom:
  limit: 100
  removal: 200

minecraft:drowned:
  limit: 200
  removal: 400

minecraft:husk:
  limit: 200
  removal: 400

minecraft:stray:
  limit: 200
  removal: 400

minecraft:zombie_villager:
  limit: 100
  removal: 200

minecraft:witch:
  limit: 50
  removal: 100

minecraft:blaze:
  limit: 100
  removal: 200

minecraft:ghast:
  limit: 50
  removal: 100

minecraft:magma_cube:
  limit: 200
  removal: 400

minecraft:slime:
  limit: 200
  removal: 400

minecraft:silverfish:
  limit: 100
  removal: 200

minecraft:endermite:
  limit: 50
  removal: 100

minecraft:shulker:
  limit: 100
  removal: 200

minecraft:hoglin:
  limit: 150
  removal: 300

minecraft:zoglin:
  limit: 100
  removal: 200

minecraft:piglin:
  limit: 200
  removal: 400

minecraft:piglin_brute:
  limit: 50
  removal: 100

minecraft:wither_skeleton:
  limit: 100
  removal: 200

# ==========================================
# 被动型生物 / Passive Mobs
# ==========================================
minecraft:pig:
  limit: 300
  removal: 600

minecraft:cow:
  limit: 300
  removal: 600

minecraft:sheep:
  limit: 300
  removal: 600

minecraft:chicken:
  limit: 400
  removal: 800

minecraft:rabbit:
  limit: 200
  removal: 400

minecraft:horse:
  limit: 100
  removal: 200

minecraft:donkey:
  limit: 50
  removal: 100

minecraft:mule:
  limit: 50
  removal: 100

minecraft:llama:
  limit: 100
  removal: 200

minecraft:fox:
  limit: 100
  removal: 200

minecraft:wolf:
  limit: 100
  removal: 200

minecraft:cat:
  limit: 100
  removal: 200

minecraft:ocelot:
  limit: 50
  removal: 100

minecraft:panda:
  limit: 50
  removal: 100

minecraft:polar_bear:
  limit: 50
  removal: 100

minecraft:bee:
  limit: 200
  removal: 400

minecraft:bat:
  limit: 100
  removal: 200

minecraft:parrot:
  limit: 100
  removal: 200

minecraft:turtle:
  limit: 100
  removal: 200

minecraft:strider:
  limit: 100
  removal: 200

minecraft:goat:
  limit: 100
  removal: 200

minecraft:axolotl:
  limit: 100
  removal: 200

minecraft:frog:
  limit: 100
  removal: 200

minecraft:camel:
  limit: 50
  removal: 100

# ==========================================
# 村民相关 / Villager Related
# ==========================================
minecraft:villager:
  limit: 500      # 村民较重要，限制较宽松
  removal: 1000

minecraft:wandering_trader:
  limit: 10
  removal: 20

minecraft:trader_llama:
  limit: 20
  removal: 40

# ==========================================
# 水生生物 / Aquatic Mobs
# ==========================================
minecraft:squid:
  limit: 200
  removal: 400

minecraft:glow_squid:
  limit: 100
  removal: 200

minecraft:dolphin:
  limit: 50
  removal: 100

minecraft:cod:
  limit: 300
  removal: 600

minecraft:salmon:
  limit: 300
  removal: 600

minecraft:tropical_fish:
  limit: 300
  removal: 600

minecraft:pufferfish:
  limit: 100
  removal: 200

minecraft:guardian:
  limit: 100
  removal: 200

minecraft:elder_guardian:
  limit: 10
  removal: 20

# ==========================================
# 特殊实体 / Special Entities
# ==========================================
minecraft:iron_golem:
  limit: 50
  removal: 100

minecraft:snow_golem:
  limit: 50
  removal: 100

minecraft:wither:
  limit: 5       # Boss实体严格限制
  removal: 10

minecraft:ender_dragon:
  limit: 1
  removal: 2

# ==========================================
# 掉落物和经验球 / Items and Experience
# ==========================================
minecraft:item:
  limit: 2000     # 掉落物限制较宽松
  removal: 5000

minecraft:experience_orb:
  limit: 1000
  removal: 3000

# ==========================================
# 投射物 / Projectiles
# ==========================================
minecraft:arrow:
  limit: 500
  removal: 1000

minecraft:spectral_arrow:
  limit: 200
  removal: 400

minecraft:trident:
  limit: 100
  removal: 200

minecraft:fireball:
  limit: 100
  removal: 200

minecraft:small_fireball:
  limit: 200
  removal: 400

minecraft:dragon_fireball:
  limit: 50
  removal: 100

minecraft:wither_skull:
  limit: 100
  removal: 200

# ==========================================
# 载具 / Vehicles
# ==========================================
minecraft:minecart:
  limit: 200
  removal: 400

minecraft:chest_minecart:
  limit: 100
  removal: 200

minecraft:furnace_minecart:
  limit: 50
  removal: 100

minecraft:hopper_minecart:
  limit: 100
  removal: 200

minecraft:tnt_minecart:
  limit: 50
  removal: 100

minecraft:boat:
  limit: 200
  removal: 400

minecraft:chest_boat:
  limit: 100
  removal: 200

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
| [SecureSeed](https://github.com/Earthcomputer/SecureSeed) | 安全种子设计 |
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
