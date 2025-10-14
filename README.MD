<div align="center">

# 🚀 AkiAsync

### Comprehensive Async Optimization Plugin for Leaves Server

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Leaves](https://img.shields.io/badge/Leaves-1.21.8+-green.svg)](https://github.com/LeavesMC/Leaves)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)
[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)](build/libs)
[![Version](https://img.shields.io/badge/version-1.0.0--Community-orange.svg)](https://github.com/virgil698/Aki-Async/releases)


一个专为 **Leaves 1.21.8+** 服务端设计的全方位异步优化插件  
通过 Mixin 技术深度优化服务端性能，让你的服务器丝滑流畅

**🎉 Community Edition (开源社区版)** - 完全免费，永久开源

[特性](#-核心特性) • [安装](#-快速开始) • [配置](#️-配置) • [性能](#-性能测试) • [文档](docs/)

</div>

---

## 🎯 核心特性

### 📊 性能表现

<table>
<tr>
<td>

**优化前**
- MSPT: ~20-30ms
- TPS波动: 频繁
- CPU占用: 不均衡

</td>
<td>

**优化后** ✨
- **MSPT: ~9.34ms** (↓ 74%)
- **TPS: 20.00** (稳定)
- **CPU: 24.23%** (多核均衡)

</td>
</tr>
</table>

### 🚀 19个优化模块（1.21.8专用）

#### **异步AI优化** (4个模块)
- ⚡ **Zero-Latency Async AI - 村民** - UUID虚拟引用+POI快照+职业原子占坑
- 🐷 **Zero-Latency Async AI - 猪灵/蛮兵** - 物品比价+恐惧向量+虚拟引用（NEW!）
- 🧠 **Brain Throttle** - AI降频，静止实体每10 tick思考一次
- 🔒 **Villager Job Claim Atomic** - 村民职业原子占坑，防止皮肤横跳

#### **并行优化** (3个模块)
- 🔄 **Async Entity Tracker** - 实体追踪异步化，减少主线程负载
- 🎯 **Parallel Entity Tick（实体级分批）** - 8实体/批，自适应超时，allof聚合
- 🗺️ **Pathfinding Budget** - 寻路预算调度，平滑分散负载

#### **生成优化** (1个模块)
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

### 🎨 技术架构

```
┌─────────────────────────────────────────────────┐
│            AkiAsync Plugin Core (v1.0.0)        │
├─────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌─────────────────┐         │
│  │ General Pool │  │  Lighting Pool  │         │
│  │  (4 threads) │  │   (2 threads)   │         │
│  │ AI + Entity  │  │   Light Only    │         │
│  └──────────────┘  └─────────────────┘         │
├─────────────────────────────────────────────────┤
│         Bridge Pattern (Mixin ↔ Plugin)         │
│         UUID虚拟引用 + ExpirableValue时间校正     │
├─────────────────────────────────────────────────┤
│  19 Optimization Mixins (Priority 999-1200)     │
│  - Async AI (4)        - Parallel (3)           │
│  - Lighting (3)        - Performance (3)        │
│  - Memory (3)          - Spawning (1)           │
│  - Brain Throttle (1)  - Pathfinding (1)        │
└─────────────────────────────────────────────────┘
```

### 🏆 关键技术

- **UUID虚拟引用** - 异步存UUID，主线程查玩家（避免跨线程传Player）
- **ExpirableValue处理** - 避免类型错误（之前会崩）
- **实体级分批** - 8个一批，任务均衡
- **自适应超时** - 根据MSPT调整（25-100ms）
- **原子占坑** - 村民职业申请加锁（防止皮肤跳）
- **双线程池** - General + Lighting分开
- **100μs超时** - 超时立即fallback

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

```bash
# 1. 下载最新版本
wget https://github.com/virgil698/Aki-Async/releases/latest/download/Aki-Async.jar

# 2. 放入 plugins 文件夹
mv Aki-Async.jar /path/to/server/plugins/

# 3. 修改启动脚本添加 Mixin 支持
java -Dleavesclip.enable.mixin=true -Xms4G -Xmx4G -jar leaves.jar

# 4. 重启服务器
```

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
# 必须添加此参数启用 Mixin 支持
-Dleavesclip.enable.mixin=true
```

## ⚙️ 配置

配置文件位于 `plugins/AkiAsync/config.yml`，首次启动自动生成。

### 推荐配置（不同服务器规模）

<details>
<summary><b>小型服务器 (1-10人)</b></summary>

```yaml
entity-tracker:
  thread-pool-size: 2-4
  
entity-tick-parallel:
  threads: 4
  min-entities: 100

lighting-optimizations:
  async-lighting:
    thread-pool-size: 2
    batch-threshold: 16
```
</details>

<details>
<summary><b>中型服务器 (10-50人) - 推荐配置</b></summary>

```yaml
entity-tracker:
  enabled: true
  thread-pool-size: 4-6
  
entity-tick-parallel:
  enabled: true
  threads: 6-8
  min-entities: 50
  
brain:
  throttle: true
  throttle-interval: 10

async-ai:  # ⚡ 零延迟异步AI（1.21.8专用）
  timeout-microseconds: 100  # 100微秒超时
  
  villager-optimization:  # 村民优化（已稳定）
    enabled: true  # 可启用
    use-poi-snapshot: true
  
  piglin-optimization:  # 猪灵+蛮兵优化（NEW!）
    enabled: true  # 可启用
    use-poi-snapshot: false
    look-distance: 16  # 可调 10/12/16
    barter-distance: 16

entity-tick-parallel:  # 实体级分批并行
  enabled: true
  threads: 6-8
  min-entities: 50
  batch-size: 8  # NEW! 8实体/批

lighting-optimizations:
  async-lighting:
    enabled: true
    thread-pool-size: 2-3
    batch-threshold: 16
  propagation-queue:
    use-layered-queue: true  # ⭐ 16层队列
  advanced:
    enable-deduplication: true
    dynamic-batch-adjustment: true
```
</details>

<details>
<summary><b>大型服务器 (50+人)</b></summary>

```yaml
entity-tracker:
  thread-pool-size: 8
  
entity-tick-parallel:
  threads: 8-12
  min-entities: 50
  
brain:
  throttle-interval: 15  # 更激进的降频

lighting-optimizations:
  async-lighting:
    thread-pool-size: 3-4
    batch-threshold: 24
```
</details>

### 核心配置项说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `entity-tracker.thread-pool-size` | 4 | 异步线程池大小 |
| `entity-tick-parallel.threads` | 6 | 并行实体Tick线程数 |
| `entity-tick-parallel.batch-size` | 8 | 实体级分批大小（NEW!）⚡ |
| `brain.throttle-interval` | 10 | AI降频间隔（tick） |
| `async-ai.villager-optimization.enabled` | false | 村民异步AI（已稳定）⚡ |
| `async-ai.piglin-optimization.enabled` | false | 猪灵异步AI（NEW!）🐷 |
| `async-ai.timeout-microseconds` | 100 | AI执行超时（微秒）|
| `lighting-optimizations.async-lighting.batch-threshold` | 16 | 光照批量处理阈值 |
| `pathfinding.tick-budget` | 0 | 寻路预算（0=禁用） |

## 📊 性能测试

### 实测数据（开发者本地测试服务器）

**测试环境**：
- CPU: Intel Core Ultra 5 125H (18 threads)
- Server: Leaves 1.21.8-137
- Players: 1 online  
- Entities: 147总计（44村民 + 49猪灵/蛮兵 + 其他）

**性能指标（实测数据 - Spark 5分钟采样）**：

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| **MSPT (最小值)** | ~14.8ms | **4.58ms** | ↓ 69% |
| **MSPT (中位数)** | ~36ms | **9.34ms** | ↓ 74% |
| **MSPT (95分位)** | ~54ms | **19.6ms** | ↓ 64% |
| **TPS** | 19.95 | **20.00** | 稳定满TPS |
| **CPU占用（系统）** | 单核高负载 | **24.23%** | 多核均衡 |
| **GC (G1 Young)** | 频繁 | **4.4ms** | 低延迟 |

**分模块性能贡献**：
- 并行实体Tick（实体级分批）：~40%（主力）
- 零延迟异步AI（村民）：~25%
- 零延迟异步AI（猪灵/蛮兵）：~15%
- 其他优化（光照/内存/性能）：~20%

---

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

### 项目结构

```
Aki-Async/
├── src/main/                 # 插件主代码
│   ├── java/org/virgil/akiasync/
│   │   ├── AkiAsyncPlugin.java          # 插件入口
│   │   ├── bridge/AkiAsyncBridge.java   # Bridge实现
│   │   ├── config/ConfigManager.java    # 配置管理
│   │   └── executor/AsyncExecutorManager.java  # 线程池管理
│   └── resources/config.yml
│
├── src/mixin/                # Mixin代码
│   ├── java/org/virgil/akiasync/mixin/
│   │   ├── bridge/                # Bridge接口
│   │   └── mixins/
│   │       ├── entity/            # 实体优化 (5个)
│   │       ├── lighting/          # 光照优化 (3个)
│   │       ├── memory/            # 内存优化 (3个)
│   │       ├── brain/             # AI优化
│   │       ├── pathfinding/       # 寻路优化
│   │       └── spawning/          # 生成优化
│   └── resources/
│       ├── aki-async.mixins.json
│       └── aki-async.accesswidener
│
├── docs/                     # 文档
│   └── REFERENCE_MODS.md    # 参考项目清单
└── build.gradle.kts
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

## 🔧 故障排查

### 常见问题

<details>
<summary><b>Q: 插件无法加载？</b></summary>

检查启动参数是否包含 `-Dleavesclip.enable.mixin=true`

</details>

<details>
<summary><b>Q: MSPT 没有明显改善？</b></summary>

1. 确认实体数量 > 50（少于50不会启用并行优化）
2. 调整 `entity-tick-parallel.threads` 为CPU核心数的1.5倍
3. 启用 `performance.enable-metrics` 查看详细统计

</details>

<details>
<summary><b>Q: 光照异常或区块黑暗？</b></summary>

临时禁用光照优化：
```yaml
lighting-optimizations:
  async-lighting:
    enabled: false
```

</details>

<details>
<summary><b>Q: 如何查看优化效果？</b></summary>

使用 [Spark](https://spark.lucko.me/) 性能分析工具：
```
/spark profiler start
# 运行一段时间后
/spark profiler stop
```

</details>

<details>
<summary><b>Q: 如何启用零延迟异步AI？⚡ NEW!</b></summary>

**村民优化**：
```yaml
async-ai:
  villager-optimization:
    enabled: true
```

**猪灵优化**：
```yaml
async-ai:
  piglin-optimization:
    enabled: true
    look-distance: 16  # 猪灵太多卡可以改小
```

**验证**：
- 观察控制台有没有报错
- 村民找工作站是否正常
- 猪灵看到金锭是否会追
- /spark tps 看MSPT是否 ≤ 10ms

</details>

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
- ✅ 19个优化模块（4个零延迟异步AI专项）
- ✅ 社区支持

根据GPL-3.0协议，你可以：
- ✅ 自由使用、修改、分发
- ✅ 用于商业服务器
- ⚠️ 修改后的版本必须同样开源（GPL-3.0）
- ⚠️ 必须保留版权声明

---

## ⭐ Star History

如果这个项目对你有帮助，请给个 ⭐ Star 支持一下！

---

<div align="center">

### 🎯 项目状态

**Edition**: Community (开源版) | **Status**: ✅ Production Ready | **Version**: 1.0.0 | **Modules**: 19

**Author**: [Virgil](https://github.com/virgil698)   
**License**: GPL-3.0  
**Supported**: Leaves 1.21.8+

---

### 🌟 特别致谢

感谢 [Leaves 团队](https://github.com/LeavesMC/Leaves) 提供优秀的服务端  
感谢所有参考项目的开发者们  
感谢 Fabric/Sponge 团队的 Mixin 技术

---

**如果觉得有用，请考虑 Star ⭐ 支持开发！**

---

### 📮 联系与支持

- 💬 **Issues**: [GitHub Issues](https://github.com/virgil698/Aki-Async/issues)
- 📧 **Email**: virgil698@sky233.top
- 💬 **Discord**: xiaokou23357(virgil698)

---

Made with ❤️ for Minecraft Server Optimization | Licensed under GPL-3.0

</div>
