<div align="center">

# 🚀 AkiAsync

**Leaves服务端专用的零延迟异步AI优化插件**

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

- **MSPT大幅优化** - 单实体效率提升最高达88%
- **TPS稳定20.00** - 无波动，无卡顿
- **多核均衡** - CPU占用分散到多个核心
- **零延迟AI** - 同tick内完成，实体行为完全一致

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

### 🔧 **优化模块（24个）**

- ⚡ **异步AI优化**（11个）- 村民、猪灵、劫掠者、唤魔者、烈焰人、守卫者、女巫、通用模板 + 辅助
- 🎯 **并行优化**（3个）- 实体追踪、实体Tick、生物生成
- 💡 **光照优化**（2个）- Starlight算法、天空光缓存
- 💥 **性能优化**（3个）- 推挤、查找缓存、碰撞检测
- 🗄️ **内存优化**（3个）- 对象池、谓词缓存、列表预分配
- 🔧 **其他优化**（2个）- AI降频、寻路预算

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
# 异步实体追踪器
entity-tracker:
  enabled: true
  thread-pool-size: 4          # 线程池大小
  update-interval-ticks: 1     # 更新间隔
  max-queue-size: 1000         # 最大队列
  batch-size: 50               # 批量大小

# 异步生物生成
mob-spawning:
  enabled: true
  spawner-optimization: true   # 刷怪笼优化

# 实体密度控制
density:
  max-per-chunk: 80           # 单区块最大实体数

# 寻路预算
pathfinding:
  tick-budget: 0              # 每tick预算（0=禁用）

# AI降频
brain:
  throttle: true
  throttle-interval: 10       # 降频间隔（tick）

# ========== 零延迟异步AI ==========
async-ai:
  timeout-microseconds: 100   # 全局超时（微秒）
  mode: simple                # 执行模式
  
  # 村民优化
  villager-optimization:
    enabled: false            # 默认关闭
    use-poi-snapshot: true
  
  # 猪灵优化
  piglin-optimization:
    enabled: false
    use-poi-snapshot: false
    look-distance: 16
    barter-distance: 16
  
  # 劫掠者家族优化
  pillager-family-optimization:
    enabled: false
    use-poi-snapshot: false
  
  # 唤魔者优化
  evoker-optimization:
    enabled: false
  
  # 烈焰人优化
  blaze-optimization:
    enabled: false
  
  # 守卫者优化（含远古守卫者）
  guardian-optimization:
    enabled: false
  
  # 女巫优化
  witch-optimization:
    enabled: false
  
  # 通用AI优化（55种生物）
  universal-ai-optimization:
    enabled: false
    entities:                 # 实体列表（见config.yml）
      - "minecraft:skeleton"
      - "minecraft:zombie"
      - "minecraft:pig"
      # ... 共55种
    tick-interval: 3
    timeout-us: 100
    control-goal: true
    batch-size: 8

# 并行实体Tick
entity-tick-parallel:
  enabled: true
  threads: 6                  # 线程数
  min-entities: 50           # 最小实体阈值
  batch-size: 8              # 批量大小

# 性能优化套件
servercore-optimizations:
  push-optimization:
    enabled: true
    interval: 2
  entity-lookup-cache:
    enabled: true
    duration-ms: 50
  collision-optimization:
    enabled: true
    min-movement: 0.001

# 内存优化套件
memory-optimizations:
  predicate-cache:
    enabled: true
  blockpos-pool:
    enabled: true
  list-prealloc:
    enabled: true
    default-capacity: 32

# 光照优化套件
lighting-optimizations:
  async-lighting:
    enabled: true
    thread-pool-size: 2
    batch-threshold: 16
  propagation-queue:
    use-layered-queue: true
    max-propagation-distance: 15
  skylight-cache:
    enabled: true
    cache-duration-ms: 100
  advanced:
    enable-deduplication: true
    dynamic-batch-adjustment: true

# 性能监控
performance:
  debug-logging: false
  enable-metrics: true
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
💬 **Discord**: xiaokou23357(virgil698)

---

**Edition**: Community（社区版） | **Version**: 2.0.0 | **License**: GPL-3.0 | **Author**: [Virgil](https://github.com/virgil698)

**如果觉得有用，请给个 ⭐ Star 支持开发！**

Made with ❤️ for Minecraft Server Optimization

</div>
