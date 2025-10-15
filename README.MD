<div align="center">

# 🚀 AkiAsync

### 🍃 Comprehensive Async Optimization Plugin for Leaves Server

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Leaves](https://img.shields.io/badge/Leaves-1.21.8+-green.svg)](https://github.com/LeavesMC/Leaves)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)
[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)](build/libs)
[![Version](https://img.shields.io/badge/version-1.1.0--Community-orange.svg)](https://github.com/virgil698/Aki-Async/releases)

一个专为 **Leaves 1.21.8+** 服务端设计的全方位异步优化插件  
使用 Mixin 深度优化服务端部分卡顿问题，确保服务端稳定运行，提升服务器性能

</div>

---

## 🎯 核心特性

#### **异步AI优化** (9个模块)
- ⚡ **Villager** - POI快照+职业原子占坑（村民+流浪商人）
- 🐷 **Piglin/PiglinBrute** - UUID虚拟引用+物品比价+恐惧向量
- 🏹 **Pillager/Vindicator/Ravager** - 安全反射+Raid检测+攻击目标筛选
- 🧙 **Evoker** - 法术CD+Vex召唤+空地块检测
- 🔥 **Blaze** - 火焰弹CD+火柱空位检测
- 🌊 **Guardian/ElderGuardian** - 激光CD+水下玩家扫描
- 🧠 **Brain Throttle** - AI降频，静止实体每10 tick思考一次
- 🗺️ **Pathfinding Budget** - 寻路预算调度，平滑分散负载
- 🔒 **Villager Job Claim Atomic** - 村民职业原子占坑，防止皮肤横跳

#### **并行优化** (3个模块)
- 🔄 **Async Entity Tracker** - 实体追踪异步化，减少主线程负载
- 🎯 **Parallel Entity Tick（实体级分批）** - 8实体/批，自适应超时，allOf聚合
- 🐛 **Async Mob Spawning** - 异步生物生成，密度智能控制

#### **光照优化** (3个模块)
- 💡 **16-Layer Propagation Queue** - Starlight核心算法，分层处理光照
- 🔆 **Smart Batch Adjustment** - TPS自适应批量大小（8-64动态调整）
- ☀️ **Skylight Cache + Deduplication** - 天空光缓存 + 去重（+30%效率）

#### **性能优化** (ServerCore-inspired, 3个模块)
- 💥 **Push Optimization** - 推挤优化（26.72%热点）
- 🔍 **Entity Lookup Cache** - 实体查找缓存（23.12%热点）
- 🎲 **Collision Optimization** - 碰撞检测优化（9%热点）

#### **内存优化** (FerriteCore-inspired, 3个模块)
- 🗄️ **BlockPos Pooling** - 对象池减少26%内存分配
- 📦 **Predicate Cache** - 谓词缓存，降低GC压力
- 📋 **List Pre-allocation** - 列表预分配，减少ArrayList扩容

---

## ✨ 详细特性

<details>
<summary><b>🔄 异步实体追踪器 (Async Entity Tracker)</b></summary>

- 将实体位置更新计算移至异步线程
- 异步处理玩家视距内的实体可见性检查
- 位置预测和边缘检测优化
- 智能处理：玩家实体保持同步确保响应速度

</details>

<details>
<summary><b>🎯 并行实体Tick（实体级分批）</b> - 76%热点优化，扩容升级版</summary>

**做了什么**：
- 把chunk级分组改成实体级分批（8个实体一批）
- 避免某个chunk有150个实体，其他chunk只有几个的情况
- 超时时间根据MSPT自动调整（25-100ms）
- 用CompletableFuture.allOf等所有批次都完成

**改进点**：
- 任务大小均衡（每批固定8个实体）
- 没有长尾任务拖慢整体
- 不和其他异步任务抢线程池

**效果**：
- MSPT从14.6ms → 9.34ms（再降36%）
- 配置batch-size可调（默认8）

</details>

<details>
<summary><b>💡 增强版异步光照 (Enhanced Async Lighting)</b> - Starlight算法</summary>

**基础优化**：
- 区块加载时的异步光照计算
- 光照更新批量处理（减少线程开销）
- 天空光缓存（避免重复计算）
- 专用光照线程池（2线程默认）

**高级优化** ⭐：
- **真正的16层分层传播队列** - Starlight核心算法（光照等级0-15）
- **智能批量大小调整** - 根据TPS动态调整（8-64范围）
- **光照更新去重** - 防止重复计算，提升30%效率
- **传播距离限制** - 防止过期更新，5秒超时清理

</details>

<details>
<summary><b>⚡ 零延迟异步AI - 村民</b></summary>

**做了什么**：
- 把村民找工作站、找床位的POI查询放到异步线程
- 主线程只拍个快照，异步线程算完了再写回
- 同一tick内完成，村民行为看不出区别

**技术要点**：
- POI快照（不可变Map，线程安全）
- 异步算64个POI的距离评分
- ExpirableValue类型处理（避免ClassCastException）
- 职业申请加原子锁（防止村民皮肤来回跳）

**效果（44村民测试）**：
- MSPT：36ms → 14.6ms（降低60%）
- 村民职业稳定（不再粒子出现了但皮肤不变）
- 交易正常，不摇头

**配置**：
```yaml
villager-optimization:
  enabled: true
  use-poi-snapshot: true
```

</details>

<details>
<summary><b>🐷 零延迟异步AI - 猪灵/蛮兵</b></summary>

**支持**：猪灵 + 猪灵蛮兵

**做了什么**：
- 把金币检测、物品比价、恐惧向量计算放到异步线程
- 主线程只做最后的写回（< 0.1ms）
- 3 tick执行一次（比村民稀疏，避免过载）

**技术要点**：
- 异步阶段存UUID（不存Player引用，避免线程安全问题）
- 主线程用UUID查玩家，再检查距离和是否持金
- 防止玩家下线导致NPE

**效果**：
- 50只猪灵原本吃掉2个TPS
- 优化后不影响TPS
- 猪灵行为正常（寻路、交易、逃跑）

**配置**：
```yaml
piglin-optimization:
  enabled: false  # 需要时再开
  look-distance: 16  # 可调小（10/12）避免卡顿
```

</details>

<details>
<summary><b>🏹 零延迟异步AI - 劫掠者家族</b></summary>

**支持**：Pillager（劫掠者）+ Vindicator（卫道士）+ Ravager（劫掠兽）

**做了什么**：
- Crossbow装填检测、Raid位置扫描、攻击目标筛选、巡逻POI评分
- 全部放到异步线程，主线程安全反射写回
- 3 tick执行一次，避免过载

**技术要点**：
- 安全REFLECTIONS工具（lazy init，不会像Witch崩溃）
- 反射写字段：target（攻击目标）、raidCenter（Raid中心）、patrolTarget（巡逻目标）
- 字段自动搜索父类（兼容Paper字段重混淆）

**配置**：
```yaml
pillager-family-optimization:
  enabled: true
```

</details>

<details>
<summary><b>🧙 零延迟异步AI - Evoker（唤魔者）</b> - v1.1</summary>

**计算内容**：
- 法术CD检测（基于生命值简化）
- Vex召唤空地块检测（3x3范围）
- 最近玩家目标筛选

**技术**：
- Mob.tick()注入 + instanceof过滤（Paper inline方法规避）
- 安全反射写 Mob.target 字段

**配置**：
```yaml
evoker-optimization:
  enabled: true
```

</details>

<details>
<summary><b>🔥 零延迟异步AI - Blaze（烈焰人）</b> - v1.1</summary>

**计算内容**：
- 火焰弹CD检测
- 火柱空位检测（above(2)位置）
- 32格内玩家扫描

**技术**：
- Mob.tick()注入 + instanceof过滤
- 安全反射写 target + fireColumnPos

**配置**：
```yaml
blaze-optimization:
  enabled: true
```

</details>

<details>
<summary><b>🌊 零延迟异步AI - Guardian（守卫者）</b> - v1.1</summary>

**支持**：Guardian（守卫者）+ ElderGuardian（远古守卫者，自动包含）

**计算内容**：
- 激光CD检测
- 水下玩家扫描（isInWater过滤）
- 最近水下目标筛选

**技术**：
- Java继承：ElderGuardian extends Guardian → instanceof自动覆盖
- 安全反射写 target + beamTarget

**配置**：
```yaml
guardian-optimization:
  enabled: true  # 远古守卫者自动包含
```

</details>

<details>
<summary><b>🧠 AI Brain 降频 (Brain Throttle)</b></summary>

- 静止实体在可配置间隔内跳过 `Brain#tick`
- 事件发生立即恢复正常频率
- 对低活跃度实体降低AI计算频率
- 可配置降频间隔（默认10 tick）

</details>

<details>
<summary><b>🗺️ 寻路预算调度 (Pathfinding Budget)</b></summary>

- 为 `PathNavigation#createPath` 设置每tick预算
- 超出预算的请求加入延迟队列
- 平滑分散负载，避免单tick寻路爆炸
- 延迟队列自动在下一tick处理

</details>

<details>
<summary><b>🐛 异步生物生成 (Async Mob Spawning)</b></summary>

- 在 NaturalSpawner 调用点限流
- 区块密度阈值控制
- Spawner 方块优化
- 预计算与缓存生成条件

</details>

<details>
<summary><b>💥 ServerCore 性能优化套件</b></summary>

- **Push Optimization** (26.72%热点) - 推挤间隔控制
- **Entity Lookup Cache** (23.12%热点) - 查找结果缓存50ms
- **Collision Optimization** (9%热点) - 静止实体跳过碰撞检测

</details>

<details>
<summary><b>🗄️ FerriteCore 内存优化套件</b></summary>

- **BlockPos Pooling** - 对象池减少26%内存分配
- **Predicate Cache** - 谓词缓存降低GC压力
- **List Pre-allocation** - 减少ArrayList扩容开销

</details>

---

## 📋 系统要求

| 组件 | 要求 |
|------|------|
| **服务端** | [Leaves](https://github.com/LeavesMC/Leaves) 1.21.8+ |
| **JDK** | Java 21 或更高版本 |
| **内存** | 推荐 4GB+ |
| **CPU** | 推荐 4核心+ (支持多线程) |

## 🚀 快速开始

### 方式1：下载 Release（推荐）

> ⚠ 本项目正在开发中，暂时不提供 Releases 下载，请使用方式2从源码构建

### 方式2：从源码构建

```bash
# 1. 克隆项目
git clone https://github.com/virgil698/Aki-Async.git
cd Aki-Async

# 2. 构建
./gradlew clean build

# 3. JAR 文件位于 build/libs/Aki-Async-1.0.0-SNAPSHOT.jar
```

### ⚙️ 启动参数（重要！）

```bash
# 必须添加此参数启用 Mixin 支持，否则插件无法加载
-Dleavesclip.enable.mixin=true
```

## ⚙️ 配置

配置文件位于 `plugins/AkiAsync/config.yml`，首次启动自动生成

配置文件默认已添加所有优化模块，请根据需要自行调整

<details>
<summary><b>默认配置</b></summary>

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
  
  # 简单实体优化 / Simple Entities Optimization
  # 支持实体 / Supported: Zombie, Skeleton, Creeper, etc
  # 说明：不依赖POI的简单AI实体
  # Description: Simple AI entities without POI dependency
  simple-entities:
    enabled: false
    use-poi-snapshot: false

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

# 方块实体优化 / Block Entity Optimizations
block-entity-optimizations:
  # 熔炉优化 / Furnace optimization
  # 说明：空闲时跳过tick
  # Description: Skip tick when idle
  furnace-optimization:
    enabled: true

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

## 🛠️ 开发指南

### 构建项目

```bash
# 克隆项目
git clone https://github.com/virgil698/Aki-Async.git
cd Aki-Async

# 构建
./gradlew clean build

# 构建产物
# build/libs/Aki-Async-1.0.0-SNAPSHOT.jar
```

## 📚 参考与致谢

本项目参考了多个优秀的开源项目：

| 项目 | 贡献 |
|------|------|
| [Starlight](https://github.com/PaperMC/Starlight) / [ScalableLux](https://github.com/RelativityMC/ScalableLux) | 16层光照传播队列核心算法 |
| [Lithium](https://github.com/CaffeineMC/lithium) | 通用性能优化思路 |
| [ServerCore](https://github.com/Wesley1808/ServerCore) | Push/Collision/Lookup优化 |
| [FerriteCore](https://github.com/malte0811/FerriteCore) | 内存优化策略 |
| [Nitori](https://github.com/Gensokyo-Reimagined/Nitori) | 实体追踪器设计 |
| [Async](https://github.com/AxalotLDev/Async) | 异步化安全边界 |
| [Leaves](https://github.com/LeavesMC/Leaves) | Mixin支持和服务端基础 |

详见 [REFERENCE_MODS.md](docs/REFERENCE_MODS.md)

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

**开发指南**：
1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改（使用[约定式提交](https://www.conventionalcommits.org/zh-hans/)）
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启 Pull Request

### 提交规范

```bash
feat(module): add new optimization
fix(lighting): resolve chunk loading issue
perf(entity): improve parallel processing
docs: update configuration guide
```

---

## ⚠️ 免责声明

- ⚠️ 本插件通过 Mixin 修改服务端核心代码，**请在测试服务器充分验证后再用于生产**
- ⚠️ 不保证与所有插件100%兼容（特别是其他Mixin插件）
- ⚠️ 如遇问题请先禁用插件，然后提交 [Issue](https://github.com/virgil698/Aki-Async/issues)
- ✅ 已在生产环境验证稳定性（1400+实体场景）

## 📄 开源协议

本项目采用 **[GPL-3.0 License](LICENSE)** 开源协议。

### 🆓 社区版说明

这是 **AkiAsync Community Edition（社区版）**：
- ✅ 完全免费使用
- ✅ 源代码开放
- ✅ 允许商业使用
- ✅ 22个优化模块（9个零延迟异步AI，覆盖9种生物）
- ✅ 社区支持

根据GPL-3.0协议，你可以：
- ✅ 自由使用、修改、分发
- ✅ 用于商业服务器
- ⚠️ 修改后的版本必须同样开源（GPL-3.0）
- ⚠️ 必须保留版权声明

---

## ⭐ Star History

如果这个项目对你有帮助，请给个 ⭐ Star 支持一下！

