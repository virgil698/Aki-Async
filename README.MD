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

### 🔧 **优化模块（40个）**
- ⚡ **异步AI优化**（10个）- 村民繁殖、猪灵、劫掠者、唤魔者、烈焰人、守卫者、女巫、通用AI模板（65种生物）、AI节流、职业占坑
- 🏭 **方块实体优化**（1个）- 方块实体并行Tick（漏斗、熔炉、箱子、酿造台、信标等）
- 💣 **TNT爆炸优化**（1个）- 三阶段异步：快照→计算→应用，支持大规模同时爆炸，原版兼容伤害
- 🎯 **并行处理优化**（3个）- 实体追踪异步、实体Tick并行、生物生成优化
- 💡 **光照系统优化**（2个）- 光照引擎异步、天空光缓存
- 🗺️ **结构定位优化**（4个）- 异步定位、海豚寻宝、箱子地图、村民交易地图
- 🔐 **安全种子系统**（6个）- 1024/2048/4096位种子加密、结构保护、矿石保护、史莱姆区块保护
- 📦 **数据包加载优化**（2个）- 多线程ZIP文件处理、智能缓存机制
- 🔨 **原版Bug修复**（1个）- 蜜蜂修复（7个MC bug）
- 💥 **实体性能优化**（5个）- 推挤优化、实体查找缓存、碰撞检测优化、实体节流、异步寻路
- 🗄️ **内存管理优化**（3个）- BlockPos对象池、谓词缓存、列表预分配
- 🔧 **区块系统优化**（2个）- 区块Tick异步（红石保护）、区块保存异步（Folia兼容）
> **安全保证**：所有优化都经过严格测试，确保不破坏任何原版游戏机制

---

## 📋 系统要求

| 组件 | 要求 |
|------|------|
| **服务端** | [Leaves](https://github.com/LeavesMC/Leaves) 或 [Luminol](https://github.com/LuminolMC/Luminol) 以及其分支 |
| **服务端版本** | 1.21.8+（已测试版本：1.21.8、1.21.10） |
| **JDK** | Java 21＋ |
| **内存** | 推荐6GB+ |
| **CPU** | 推荐4核心+(虽多线程优化，但依旧以单核性能为主)|

---

## 🚀 快速开始

### 1. 下载插件

从 [MineBBS](https://www.minebbs.com/resources/aki-async-leaves-luminol.14159/) 或 [Releases](https://github.com/virgil698/Aki-Async/releases) 下载最新版本

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

**提示**：
- 💡 反馈 Bug 时建议先执行此命令，并附上输出截图
- 💡 可用于快速检查配置是否生效

</details>

<details>
<summary><b>🛜 /aki-teleport-stats [reset] - 网络统计信息</b></summary>

**功能**：查看玩家传送优化的详细统计数据，监控传送性能和网络包处理情况

**权限**：`akiasync.teleportstats`（默认仅OP可用）

**前置条件**：
- 网络优化管理器必须启用
- 传送优化功能必须在配置中开启

**使用方法**：
```bash
# 查看统计信息
/aki-teleport-stats

# 重置所有统计数据
/aki-teleport-stats reset
```

**统计信息包含**：
- 📊 **Active teleports**：当前正在进行的传送数量
- 📈 **Total teleports**：总传送次数
- ✅ **Successful**：成功完成的传送次数和成功率
- ❌ **Failed**：失败的传送次数
- ⏱️ **Average delay**：平均传送延迟（毫秒）
- 🔝 **Maximum delay**：最大传送延迟（毫秒）
- 📦 **Bypassed packets**：优先发送的数据包数量
- 🚫 **Filtered packets**：被过滤的非必要数据包数量
- ⚙️ **Boost duration**：传送加速持续时间（秒）
- 🔧 **Filter enabled**：是否启用数据包过滤

**示例输出**：
```
========== Teleport Tracker Statistics ==========
Active teleports: 2
Total teleports: 156
Successful: 154 (98.7%)
Failed: 2
Average delay: 245.32ms
Maximum delay: 1203.45ms
Bypassed packets: 3421
Filtered packets: 8932
Boost duration: 5s
Filter enabled: true
================================================
```

**使用场景**：
- 🟢 监控传送优化效果
- 🟢 诊断传送延迟问题
- 🟢 评估网络包过滤效率
- 🟢 性能调优参考数据
- 🟡 定期重置统计以获取新数据

**配置关联**：
此命令的统计数据受以下配置影响：
- `network-optimization.teleport-optimization.enabled`
- `network-optimization.teleport-optimization.boost-duration-seconds`
- `network-optimization.teleport-optimization.filter-non-essential-packets`

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
# 当前版本 / Current version: 13
# 说明：当前配置版本为13，如果配置版本不等于13，则需要更新配置文件
# Description: The current configuration version is 13, if the configuration version is not equal to 13, you need to update the configuration file
version: 13

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
# 特殊保护 / Special protection:
#   - 掉落物在危险环境中不会被节流（岩浆、火焰、炼药锅）
#   - Items in dangerous environments won't be throttled (lava, fire, cauldron)
entity-throttling:
  # 启用开关 / Enable toggle
  enabled: false
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
  #   - 掉落物销毁检测（包括炼药锅岩浆） / Item entity destruction detection (including cauldron lava)
  collision-optimization:
    enabled: true
    # 最小移动阈值，低于此值时跳过碰撞检测 / Minimum movement threshold for skipping collision detection
    min-movement: 0.001
  
  # 矿车炼药锅销毁 / Minecart cauldron destruction
  # 说明：矿车进入炼药锅岩浆时销毁矿车并掉落内容物
  # Description: Destroy minecarts when entering lava cauldron and drop contents
  # 支持的矿车类型 / Supported minecart types:
  #   - 漏斗矿车 / Hopper Minecart
  #   - 箱子矿车 / Chest Minecart
  minecart-cauldron-destruction:
    enabled: true

# ==========================================
# 下落方块并行处理 / Falling Block Parallel Processing
# ==========================================
falling-block-optimization:
  # 启用下落方块并行处理 / Enable parallel falling block processing
  # 说明：针对刷沙机、重力方块等大量下落方块场景进行性能优化
  # Description: Performance optimization for sand dupers, gravity blocks and massive falling block scenarios
  # 优化效果 / Performance gain:
  #   - 大型刷沙机：30-50%性能提升 / Large sand dupers: 30-50% improvement
  #   - 多个刷沙机同时运行：更明显的提升 / Multiple dupers: even better improvement
  # 注意事项 / Notes:
  #   - 保持原版行为，不影响红石机械时序 / Maintains vanilla behavior, no impact on redstone timing
  #   - Folia环境下自动禁用（区域并行已优化） / Auto-disabled in Folia (region parallelism already optimized)
  enabled: true
  
  # 最小下落方块数量阈值 / Minimum falling blocks threshold
  # 说明：只有当下落方块数量超过此值时才启用并行处理
  # Description: Parallel processing only activates when falling block count exceeds this value
  # 推荐值 / Recommended: 20-50
  min-falling-blocks: 20
  
  # 批量处理大小 / Batch size
  # 说明：每个批次处理的下落方块数量
  # Description: Number of falling blocks processed per batch
  # 推荐值 / Recommended: 10-20
  # 注意：过大可能导致单批次耗时过长，过小则并行效果不明显
  # Note: Too large may cause long batch processing time, too small reduces parallel efficiency
  batch-size: 10

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
# 掉落物优化 / Item Entity Optimizations
# ==========================================
item-entity-optimizations:
  # 并行处理优化 / Parallel Processing Optimization
  # 说明：批量并行处理掉落物tick，针对刷怪塔、自动农场等大量掉落物场景
  # Description: Batch parallel processing of item entity ticks for mob farms, auto farms, etc.
  # 性能提升 / Performance gain:
  #   - 刷怪塔：40-60%性能提升 / Mob farms: 40-60% improvement
  #   - 自动农场：30-50%性能提升 / Auto farms: 30-50% improvement
  #   - 爆炸场景：50-70%性能提升 / Explosion scenarios: 50-70% improvement
  parallel-processing:
    enabled: true
    # 最小掉落物数量阈值 / Minimum item entities threshold
    # 说明：只有当掉落物数量超过此值时才启用并行处理
    # Description: Parallel processing only activates when item count exceeds this value
    # 推荐值 / Recommended: 50-100
    min-item-entities: 50
    # 批量处理大小 / Batch size
    # 说明：每个批次处理的掉落物数量
    # Description: Number of item entities processed per batch
    # 推荐值 / Recommended: 20-40
    batch-size: 20
  
  # 智能合并优化 / Smart Merge Optimization
  # 说明：优化tryToMerge()方法的调用频率和效率
  # Description: Optimize tryToMerge() call frequency and efficiency
  # 性能提升 / Performance gain:
  #   - 减少60-80%的合并检查次数 / 60-80% reduction in merge checks
  #   - 大量掉落物场景下MSPT降低2-4ms / 2-4ms MSPT reduction in massive item scenarios
  smart-merge:
    enabled: true
    # 合并检查间隔 / Merge check interval (ticks)
    # 说明：每N tick执行一次合并检查
    # Description: Execute merge check every N ticks
    # 推荐值 / Recommended: 5-10
    merge-interval: 5
    # 最少附近物品数 / Minimum nearby items
    # 说明：附近物品少于此值时跳过合并
    # Description: Skip merge when nearby items < threshold
    # 推荐值 / Recommended: 3-5
    min-nearby-items: 3
    # 合并检测范围 / Merge detection range (blocks)
    # 说明：检测附近物品的范围
    # Description: Range to detect nearby items
    # 推荐值 / Recommended: 1.5-2.5
    merge-range: 1.5
  
  # 年龄增长优化 / Age Optimization
  # 说明：根据掉落物状态智能调整tick频率
  # Description: Intelligently adjust tick frequency based on item state
  # 性能提升 / Performance gain:
  #   - 静止掉落物tick频率降低80-90% / 80-90% tick reduction for static items
  #   - 大量掉落物场景下MSPT降低3-5ms / 3-5ms MSPT reduction in massive item scenarios
  age-optimization:
    enabled: true
    # 年龄增长间隔 / Age increment interval (ticks)
    # 说明：静止掉落物每N tick增长一次年龄
    # Description: Static items age every N ticks
    # 推荐值 / Recommended: 10-20
    age-interval: 10
    # 玩家检测范围 / Player detection range (blocks)
    # 说明：此范围内有玩家时保持正常tick
    # Description: Maintain normal tick when player within this range
    # 推荐值 / Recommended: 8.0-16.0
    player-detection-range: 8.0

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
# 
# ⚠️ 重要警告 / IMPORTANT WARNING:
# 1. 一旦启用此功能，世界将使用加密种子生成
# 2. 启用后不能随意关闭，否则会导致：
#    - 新生成的区块与旧区块不匹配（区块边界错乱）
#    - 结构生成位置改变（村庄、要塞等消失或重新生成）
#    - 矿石分布完全改变
#    - 史莱姆区块位置改变
# 3. 如需关闭，必须删除世界重新生成，或接受上述后果
# 
# Once enabled, the world will use encrypted seeds for generation
# DO NOT disable after enabling, or it will cause:
#    - Chunk boundary mismatches between old and new chunks
#    - Structure positions changed (villages, strongholds may disappear)
#    - Ore distribution completely changed
#    - Slime chunk positions changed
# To disable, you must delete the world and regenerate, or accept the consequences
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
# 网络优化 / Network Optimization
# ==========================================
# 说明：智能数据包优先级和区块发送速率控制
# Description: Intelligent packet priority and chunk send rate control
# 
# 支持场景 / Supported Scenarios:
#   - PVP 战斗 / PVP Combat
#   - PVE 刷怪 / PVE Grinding
#   - 多生物农场 / Multi-mob Farm
#   - 刷沙机/重力方块 / Sand Duper/Falling Blocks
#   - 高密度区块 / High-density Chunk
#   - 鞘翅高速飞行 / Elytra Fast Flight
network-optimization:
  # 总开关 / Master switch
  enabled: true
  
  # 数据包优先级系统 / Packet Priority System
  # 说明：根据数据包类型自动分配优先级，改善弱网环境玩家体验
  # Description: Automatically assigns priority based on packet type, improves experience for poor network players
  # 
  # 优先级说明 / Priority Levels:
  #   CRITICAL (0): 玩家移动、战斗操作 / Player movement, combat actions
  #   HIGH (1):     实体更新、方块变化 / Entity updates, block changes
  #   NORMAL (2):   区块数据、容器更新 / Chunk data, container updates
  #   LOW (3):      粒子效果、音效 / Particle effects, sounds
  # 
  # 兼容性 / Compatibility:
  #   - 不修改数据包内容，完全兼容反作弊插件 / Doesn't modify packet content, fully compatible with anti-cheat
  #   - 只调整发送顺序，不影响数据包监听器 / Only adjusts send order, doesn't affect packet listeners
  packet-priority:
    enabled: true
    
    # 数据包发送速率控制 / Packet Send Rate Control
    # 说明：控制每个玩家每 tick 的数据包发送数量
    # Description: Controls packet send count per player per tick
    send-rate:
      # 基础发送速率（packets/tick）/ Base send rate (packets/tick)
      # 说明：正常场景下的发送速率
      # Description: Send rate in normal scenarios
      # 推荐值 / Recommended: 50 (默认), 40 (低配服务器), 60 (高配服务器)
      base: 50
      
      # 中度加速速率（packets/tick）/ Medium acceleration rate
      # 说明：队列积压 >100 时的发送速率（PVP、鞘翅飞行）
      # Description: Send rate when queue >100 (PVP, elytra flight)
      # 推荐值 / Recommended: 80 (默认), 60 (低配服务器), 100 (高配服务器)
      medium: 80
      
      # 重度加速速率（packets/tick）/ Heavy acceleration rate
      # 说明：队列积压 >300 时的发送速率（多生物农场、大规模 PVE）
      # Description: Send rate when queue >300 (multi-mob farms, large PVE)
      # 推荐值 / Recommended: 110 (默认), 80 (低配服务器), 130 (高配服务器)
      heavy: 110
      
      # 极限加速速率（packets/tick）/ Extreme acceleration rate
      # 说明：队列积压 >500 时的发送速率（刷沙机、单区块高密度）
      # Description: Send rate when queue >500 (sand dupers, high-density chunks)
      # 推荐值 / Recommended: 130 (默认), 100 (低配服务器), 150 (高配服务器)
      extreme: 130
    
    # 队列容量限制 / Queue Capacity Limits
    # 说明：控制每个玩家的数据包队列大小
    # Description: Controls packet queue size per player
    queue-limits:
      # 总队列容量 / Total queue capacity
      # 说明：单个玩家的最大队列大小
      # Description: Maximum queue size per player
      # 推荐值 / Recommended: 2500 (默认), 1500 (低配服务器), 3000 (高配服务器)
      max-total: 2500
      
      # CRITICAL 优先级配额 / CRITICAL priority quota
      # 说明：CRITICAL 优先级数据包的最大数量（玩家移动、战斗）
      # Description: Maximum CRITICAL priority packets (player movement, combat)
      # 推荐值 / Recommended: 1200 (默认), 800 (低配服务器), 1500 (高配服务器)
      max-critical: 1200
      
      # HIGH 优先级配额 / HIGH priority quota
      # 说明：HIGH 优先级数据包的最大数量（实体更新、方块变化）
      # Description: Maximum HIGH priority packets (entity updates, block changes)
      # 推荐值 / Recommended: 800 (默认), 500 (低配服务器), 1000 (高配服务器)
      max-high: 800
      
      # NORMAL 优先级配额 / NORMAL priority quota
      # 说明：NORMAL 优先级数据包的最大数量（区块数据、容器更新）
      # Description: Maximum NORMAL priority packets (chunk data, container updates)
      # 推荐值 / Recommended: 600 (默认), 400 (低配服务器), 800 (高配服务器)
      max-normal: 600
    
    # 动态加速阈值 / Dynamic Acceleration Thresholds
    # 说明：控制何时触发不同级别的加速
    # Description: Controls when to trigger different acceleration levels
    acceleration-thresholds:
      # 中度加速阈值 / Medium acceleration threshold
      # 说明：队列大小超过此值时触发中度加速
      # Description: Trigger medium acceleration when queue exceeds this value
      # 推荐值 / Recommended: 100 (默认), 150 (宽松), 50 (激进)
      medium: 100
      
      # 重度加速阈值 / Heavy acceleration threshold
      # 说明：队列大小超过此值时触发重度加速
      # Description: Trigger heavy acceleration when queue exceeds this value
      # 推荐值 / Recommended: 300 (默认), 400 (宽松), 200 (激进)
      heavy: 300
      
      # 极限加速阈值 / Extreme acceleration threshold
      # 说明：队列大小超过此值时触发极限加速
      # Description: Trigger extreme acceleration when queue exceeds this value
      # 推荐值 / Recommended: 500 (默认), 600 (宽松), 400 (激进)
      extreme: 500
    
    # 智能清理策略 / Smart Cleanup Strategy
    # 说明：控制队列满时的清理行为
    # Description: Controls cleanup behavior when queue is full
    cleanup:
      # 启用智能清理 / Enable smart cleanup
      # 说明：队列满时自动清理低优先级数据包
      # Description: Automatically clean low priority packets when queue is full
      enabled: true
      
      # 老旧数据包阈值（秒）/ Stale packet threshold (seconds)
      # 说明：超过此时间的 NORMAL 优先级数据包会被清理
      # Description: NORMAL priority packets older than this will be cleaned
      # 推荐值 / Recommended: 5 (默认), 10 (宽松), 3 (激进)
      stale-threshold: 5
      
      # CRITICAL/HIGH 清理数量 / CRITICAL/HIGH cleanup count
      # 说明：CRITICAL/HIGH 数据包入队时清理的低优先级数量
      # Description: Number of low priority packets to clean when CRITICAL/HIGH enqueues
      # 推荐值 / Recommended: 100 (默认), 50 (保守), 150 (激进)
      critical-cleanup: 100
      
      # NORMAL 清理数量 / NORMAL cleanup count
      # 说明：NORMAL 数据包入队时清理的低优先级数量
      # Description: Number of low priority packets to clean when NORMAL enqueues
      # 推荐值 / Recommended: 50 (默认), 30 (保守), 80 (激进)
      normal-cleanup: 50
  
  # 区块发送速率控制 / Chunk Send Rate Control
  # 说明：根据玩家网络状态和视线方向动态调整区块发送速率
  # Description: Dynamically adjusts chunk send rate based on player network status and view direction
  chunk-rate-control:
    enabled: true
    # 基础区块发送速率（chunks/tick）/ Base chunk send rate (chunks/tick)
    # 说明：正常网络状态下的发送速率
    # Description: Send rate under normal network conditions
    # 推荐值 / Recommended: 10 (默认), 8 (低配/多人), 12 (高配/少人)
    base-rate: 10
    # 最大区块发送速率 / Maximum chunk send rate
    # 说明：网络状况良好时的最大速率
    # Description: Maximum rate when network is good
    # 推荐值 / Recommended: 20 (默认), 15 (低配/多人), 25 (高配/少人)
    max-rate: 20
    # 最小区块发送速率 / Minimum chunk send rate
    # 说明：网络拥塞时的最小速率
    # Description: Minimum rate during network congestion
    # 推荐值 / Recommended: 3 (默认), 2 (激进), 5 (保守)
    min-rate: 3
  
  # 网络拥塞检测 / Network Congestion Detection
  # 说明：监控玩家ping和带宽使用，自动检测网络拥塞
  # Description: Monitors player ping and bandwidth usage, auto-detects network congestion
  congestion-detection:
    enabled: true
    # 高延迟阈值（毫秒）/ High ping threshold (milliseconds)
    # 说明：超过此值认为网络延迟较高
    # Description: Considered high latency when exceeding this value
    # 推荐值 / Recommended: 200 (默认), 150 (严格), 300 (宽松)
    high-ping-threshold: 200
    # 严重延迟阈值（毫秒）/ Critical ping threshold (milliseconds)
    # 说明：超过此值认为网络严重拥塞
    # Description: Considered severe congestion when exceeding this value
    # 推荐值 / Recommended: 500 (默认), 400 (严格), 600 (宽松)
    critical-ping-threshold: 500
    # 高带宽阈值（bytes/s）/ High bandwidth threshold (bytes/s)
    # 说明：超过此值认为带宽使用较高（默认1MB/s）
    # Description: Considered high bandwidth usage when exceeding this value (default 1MB/s)
    # 推荐值 / Recommended: 1048576 (1MB/s, 默认), 524288 (512KB/s, 严格), 2097152 (2MB/s, 宽松)
    high-bandwidth-threshold: 1048576

  # 传送优化 / Teleport Optimization
  # 说明：优化玩家传送时的网络性能，防止传送时被踢出
  # Description: Optimize network performance during player teleportation to prevent disconnection
  # 
  # 问题背景 / Background:
  #   - 传送时会发送大量关键数据包（位置、区块、状态等）
  #   - 这些包进入优先级队列可能导致延迟
  #   - 客户端等待超时后会被踢出（Connection reset）
  # 
  # 解决方案 / Solution:
  #   - 传送包直接发送，绕过队列系统
  #   - 传送期间提升区块发送速率
  #   - 过滤非必需包（粒子、音效等）
  # 
  # 适用场景 / Use Cases:
  #   - RTP 随机传送 / Random teleport
  #   - 登录系统传送 / Login system teleport
  #   - 命令传送 / Command teleport
  #   - 跨维度传送 / Cross-dimension teleport
  teleport-optimization:
    # 启用传送优化 / Enable teleport optimization
    enabled: true
    
    # 传送包绕过队列 / Bypass queue for teleport packets
    # 说明：传送相关的数据包直接发送，不进入优先级队列
    # Description: Teleport-related packets are sent directly, bypassing priority queue
    # 包括的包类型 / Included packet types:
    #   - ClientboundPlayerPositionPacket (玩家位置同步)
    #   - ClientboundRespawnPacket (重生/维度切换)
    #   - ClientboundGameEventPacket (游戏状态变更)
    #   - ClientboundSetDefaultSpawnPositionPacket (设置出生点)
    #   - ClientboundChangeDifficultyPacket (难度变更)
    bypass-queue: true
    
    # 传送后速率提升持续时间（秒）/ Boost duration after teleport (seconds)
    # 说明：传送后保持高速率发送区块的时间
    # Description: Duration to maintain high chunk send rate after teleport
    # 推荐值 / Recommended: 5 (默认), 3 (低配服务器), 7 (高配服务器)
    # 优化：延长到7秒，配合降低的速率实现渐进式加载 / Optimized: Extended to 7s for gradual loading with reduced rate
    boost-duration-seconds: 7
    
    # 传送期间最大区块发送速率 / Max chunk send rate during teleport
    # 说明：传送期间的区块发送速率（chunks/tick）
    # Description: Chunk send rate during teleport (chunks/tick)
    # 推荐值 / Recommended: 25 (默认), 20 (低配服务器), 30 (高配服务器)
    # 注意：此值应大于 chunk-rate-control.max-rate
    # Note: This value should be greater than chunk-rate-control.max-rate
    # 优化：降低到15以减少MSPT飙升 / Optimized: Reduced to 15 to prevent MSPT spikes
    max-chunk-rate: 15
    
    # 过滤非必需包 / Filter non-essential packets
    # 说明：传送期间暂停发送非必需的装饰性数据包
    # Description: Pause sending non-essential decorative packets during teleport
    # 
    # 必需包（继续发送）/ Essential packets (continue sending):
    #   - 传送包 / Teleport packets
    #   - 区块数据 / Chunk data
    #   - 玩家状态（生命值、经验、能力）/ Player status (health, exp, abilities)
    #   - 容器内容 / Container contents
    #   - KeepAlive 包 / KeepAlive packets
    # 
    # 非必需包（暂停发送）/ Non-essential packets (paused):
    #   - 粒子效果 / Particle effects
    #   - 音效 / Sound effects
    #   - 标题/副标题 / Titles/subtitles
    #   - 记分板更新 / Scoreboard updates
    #   - 实体移动（非玩家）/ Entity movement (non-player)
    filter-non-essential-packets: true
    
    # 调试模式 / Debug mode
    # 说明：启用详细的传送优化日志
    # Description: Enable detailed teleport optimization logs
    # 注意：仅用于调试，生产环境建议关闭
    # Note: For debugging only, recommended to disable in production
    debug-enabled: false

# ==========================================
# 高速移动区块预加载 / Fast Movement Chunk Loading
# ==========================================
# 说明：检测玩家高速移动（鞘翅飞行、疾跑等），提前加载前方区块
# Description: Detects fast-moving players (elytra, sprinting) and preloads chunks ahead
# 适用场景 / Use Cases:
# - 鞘翅飞行 / Elytra flying
# - 创造模式飞行 / Creative mode flying  
# - 载具高速移动 / Fast vehicle movement
# - 大型服务器优化玩家体验 / Optimize player experience on large servers
fast-movement-chunk-load:
  # 启用 / Enable
  # 说明：启用后会监控玩家移动速度并预加载区块，支持鞘翅、疾跑、创造飞行等
  # Description: When enabled, monitors player movement speed and preloads chunks
  # 特性：自动检测烟花鞘翅超高速移动，动态调整所有参数
  # Features: Auto-detects firework elytra ultra-fast movement, dynamically adjusts all parameters
  enabled: true
  
  # 速度阈值（方块/tick）/ Speed threshold (blocks/tick)
  # 说明：超过此速度时触发预加载（0.5 ≈ 10方块/秒）
  # Description: Triggers preloading when exceeding this speed (0.5 ≈ 10 blocks/sec)
  # 推荐值 / Recommended: 0.5 (默认), 0.3 (更敏感), 0.8 (仅极快速度)
  # 注意：烟花鞘翅速度可达2-3方块/tick，系统会自动适配
  speed-threshold: 0.5
  
  # 预加载距离（区块）/ Preload distance (chunks)
  # 说明：基础预加载距离，实际距离会根据速度动态调整
  # Description: Base preload distance, actual distance adjusts dynamically based on speed
  # 推荐值 / Recommended: 8 (默认), 6 (保守), 10 (激进)
  # 动态调整 / Dynamic adjustment:
  #   - 普通移动 (<1.0 b/t): 8区块 + 周围4区块圆形
  #   - 高速移动 (1.0-1.5 b/t): 10区块 (x1.2) + 周围4区块
  #   - 超高速移动 (>1.5 b/t): 12区块 (x1.5) + 周围4区块 [烟花鞘翅]
  preload-distance: 8
  
  # 最大并发加载数 / Max concurrent loads
  # 说明：基础并发数，实际并发数会根据速度动态调整
  # Description: Base concurrent loads, actual count adjusts dynamically based on speed
  # 推荐值 / Recommended: 4 (默认), 2 (低配), 6 (高配)
  # 动态调整 / Dynamic adjustment:
  #   - 普通移动: 4个并发
  #   - 高速移动: 8个并发 (x2)
  #   - 超高速移动: 12个并发 (x3) [烟花鞘翅]
  max-concurrent-loads: 4
  
  # 预测时间（ticks）/ Prediction time (ticks)
  # 说明：基础预测时间，实际时间会根据速度动态调整
  # Description: Base prediction time, actual time adjusts dynamically based on speed
  # 推荐值 / Recommended: 40 (默认), 30 (保守), 50 (激进)
  # 动态调整 / Dynamic adjustment:
  #   - 普通移动: 40 ticks (2秒)
  #   - 高速移动: 48 ticks (2.4秒, x1.2)
  #   - 超高速移动: 60 ticks (3秒, x1.5) [烟花鞘翅]
  prediction-ticks: 40
  
  # 智能偏移式区块加载中心 / Smart Center Offset Chunk Loading
  # 说明：根据玩家移动速度和方向，动态偏移区块加载中心，实现类似远视距的体验
  # Description: Dynamically offset chunk loading center based on player speed and direction
  # 适用场景 / Use Cases:
  # - 鞘翅高速飞行时提前加载前方地形 / Preload terrain ahead during fast elytra flight
  # - 小视距服务器模拟远视距效果 / Simulate far render distance on low view distance servers
  # - 减少高速移动时的卡顿和撞山 / Reduce lag and crashes during high-speed movement
  center-offset:
    # 启用智能偏移 / Enable center offset
    # 说明：启用后，区块加载中心会根据速度方向偏移
    # Description: When enabled, chunk loading center shifts based on movement direction
    enabled: true
    
    # 最小触发速度（方块/tick）/ Minimum trigger speed (blocks/tick)
    # 说明：低于此速度时不进行偏移（60 b/s = 3 b/t）
    # Description: No offset below this speed (60 blocks/sec = 3 blocks/tick)
    # 推荐值 / Recommended: 3.0 (默认), 2.0 (更敏感), 4.0 (更保守)
    min-speed: 3.0
    
    # 最大触发速度（方块/tick）/ Maximum trigger speed (blocks/tick)
    # 说明：超过此速度时不再增加偏移（180 b/s = 9 b/t）
    # Description: No additional offset above this speed (180 blocks/sec = 9 blocks/tick)
    # 推荐值 / Recommended: 9.0 (默认), 7.0 (更保守), 12.0 (更激进)
    # 注意：速度过高可能导致玩家被卸载，请谨慎调整
    # Warning: Too high speed may cause player to be unloaded, adjust carefully
    max-speed: 9.0
    
    # 最大偏移比例 / Maximum offset ratio
    # 说明：偏移距离占视距的最大比例（0.75 = 75%）
    # Description: Maximum offset distance as ratio of view distance (0.75 = 75%)
    # 推荐值 / Recommended: 0.75 (默认), 0.5 (保守), 0.9 (激进)
    # 计算公式 / Formula: 最大偏移 = 视距 × 比例 × 速度比例
    # 示例 / Example: 视距10，速度6 b/t，比例0.75 → 偏移 = 10 × 0.75 × 0.5 = 3.75区块
    max-offset-ratio: 0.75
  
  # 异步加载批量控制 / Async Loading Batch Control
  # 说明：控制区块异步加载的批次大小和延迟，防止MSPT飙升
  # Description: Control chunk async loading batch size and delay to prevent MSPT spikes
  async-loading:
    # 批次大小 / Batch size
    # 说明：每批加载的区块数量，加载完一批后会延迟再加载下一批
    # Description: Number of chunks to load per batch, delay between batches
    # 推荐值 / Recommended: 2 (默认，最保守), 3 (平衡), 4 (激进)
    # 注意：值越大，加载越快，但MSPT可能越高
    # Warning: Higher values load faster but may cause higher MSPT
    batch-size: 2
    
    # 批次延迟（毫秒）/ Batch delay (milliseconds)
    # 说明：每批加载后的延迟时间，给服务器时间处理
    # Description: Delay after each batch to give server time to process
    # 推荐值 / Recommended: 20 (默认，最保守), 15 (平衡), 10 (激进)
    # 注意：值越小，加载越快，但MSPT可能越高
    # Warning: Lower values load faster but may cause higher MSPT
    batch-delay-ms: 20

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
<summary><b>📖 throttling.yml 完整配置项（点击展开）</b></summary>

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
