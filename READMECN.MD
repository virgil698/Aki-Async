<div align="center">

# AkiAsync

> 基于 Mixin 实现的异步优化插件，旨在不改变核心特性的前提下提升 Leaves/Luminol/LightingLuminol/Lophine/Mint 及其分支的性能

**Leaves / Luminol 异步优化插件**

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Leaves](https://img.shields.io/badge/Leaves-1.21.8+-green.svg)](https://github.com/LeavesMC/Leaves)
[![Luminol](https://img.shields.io/badge/Luminol-1.21.8+-purple.svg)](https://github.com/LuminolMC/Luminol)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)

**Community Edition（社区版）**

[快速开始](#快速开始) • [命令列表](#命令列表) • [配置说明](#配置说明) • [常见问题](#常见问题)

</div>

[English](README.MD) | 简体中文

---

## 核心特性

### 性能表现
- **TPS优化稳步提升** - 减轻对主线程的压力，MSPT显著降低
- **多线程均衡** - CPU占用分散到多个核心/线程，支持工作窃取模式
- **零延迟快照** - 快照-计算-写回模式，同tick内完成，无延迟
- **完全兼容** - 保持所有原版特性，兼容Paper/Spigot/Leaves/Folia/Luminol配置
- **Java 21虚拟线程** - 利用现代Java特性，旧版本自动降级

### 优化模块（90+个可配置优化）
- **实体系统**（20+个）- 实体Tick并行、实体追踪器、密度控制、实体节流、移动优化、生物消失优化、抛射物优化、窒息优化、Nitori实体优化（日光燃烧、速度、掉落伤害、区段快速检索）、VMP实体优化（零速度跳过、追踪距离缓存）
- **异步AI**（15+个）- 增强异步寻路（优先级队列+多层缓存）、AI传感器优化、游戏事件优化、Brain降频、村民优化（快速射线追踪、异步繁殖）、通用AI模板（65+种生物）
- **碰撞系统**（10+个）- 原生碰撞检测（AVX SIMD）、DDA射线碰撞、方块形状缓存、形状预计算、激进模式、带过期的方块缓存
- **掉落物与经验球**（6个）- 智能合并、年龄优化、掉落物和经验球的Inactive Tick优化
- **方块实体**（8个）- 方块实体并行Tick、零延迟工厂、漏斗容器缓存、矿车Tick优化、熔炉/工作台配方缓存、C2ME NBT优化
- **TNT爆炸**（8个）- 统一爆炸引擎（ByteVector优化）、密度缓存、向量化AABB检测、实体合并、原版兼容伤害计算、完整保护插件支持
- **区块系统**（15+个）- 异步区块加载/保存/序列化、噪声优化、Jigsaw优化、ChunkPos优化、Perlin噪声优化、Mid-Tick任务、异步Blending、存储IO工作线程、NBT缓存限制
- **网络优化**（20+个）- 原生压缩（libdeflate）、原生加密（OpenSSL）、快速VarInt编码、MTU感知批处理、Flush合并、AFK包节流、动态区块发送速率、包优先级队列、区块可见性过滤、快速移动区块预加载（中心偏移）
- **结构定位**（6个）- 异步定位命令、海豚寻宝、箱子探险地图、村民地图交易、混合搜索模式算法优化、生物群系感知搜索
- **光照系统**（2个）- 异步光照引擎（时间窗口控制）、光照更新批处理
- **安全种子**（4个）- 量子加密方案、结构/矿石/史莱姆保护、防插件窃取、/seed命令限制
- **数据包优化**（3个）- 并行数据包加载、命令去重、Execute命令Inactive跳过
- **智能滞后补偿**（4个）- 时间加速、掉落物拾取延迟补偿、药水效果补偿、TPS感知自动调整
- **原版Bug修复**（8个）- 蜜蜂修复（7个MC Bug）、末地岛屿密度修复、传送门窒息检查、潜影贝子弹自伤修复、矿车炼药锅销毁修复、首领僵尸血量修复
- **内存与数学**（8个）- BlockPos缓存、BitSet对象池、CompletableFuture优化、优化集合类、紧凑正弦查找表、C2ME资源优化
- **高级优化**（8个）- 虚拟线程、工作窃取调度器、多Netty事件循环、PalettedContainer锁移除、类型过滤列表、生成密度数组、生物群系访问优化

> **安全保证**：所有优化目标都经过严格测试，确保不破坏任何原版游戏机制

---

## 系统要求

| 组件 | 要求 |
|------|------|
| **服务端** | [Leaves](https://github.com/LeavesMC/Leaves) 或 [Luminol](https://github.com/LuminolMC/Luminol) 以及其分支（已测试核心：Leaves、Luminol、LightingLuminol、Lophine、Mint） |
| **服务端版本** | 1.21.8+（已测试版本：1.21.8、1.21.10、1.21.11） |
| **JDK** | Java 21+（已测试环境：Java 21、Java 25） |
| **内存** | 推荐 6GB+ |
| **CPU** | 推荐 4核心+（虽多线程优化，但依旧以单核性能为主）|

---

## 快速开始

### 1. 下载插件

从 [MineBBS](https://www.minebbs.com/resources/aki-async-leaves-luminol.14159/) 或 [Releases](https://github.com/virgil698/Aki-Async/releases) 下载最新版本

### 2. 安装插件

```bash
# 将JAR文件放入plugins文件夹
cp Aki-Async-2.0.0.jar /path/to/server/plugins/
```

### 3. 启用Mixin支持

修改启动脚本，添加以下参数：

```bash
java -Dleavesclip.enable.mixin=true -Xms4G -Xmx4G -jar server.jar
```

**重要**：必须添加 `-Dleavesclip.enable.mixin=true` 参数，否则插件无法工作

### 4. 启动服务器

首次启动会自动生成配置文件：`plugins/AkiAsync/config.yml`

---

## 命令列表

<details>
<summary><b>/aki-reload - 热重载配置</b></summary>

**功能**：无需重启服务器即可重新加载配置并应用更改

**权限**：`akiasync.reload`（默认仅OP可用）

**重要警告**：
- 非必要情况下不推荐使用热重载
- 修改优化类配置后更推荐关服重启
- 热重载可能导致一些神秘问题
- 生产环境建议维护时间重启服务器

**使用方法**：
```bash
/aki-reload
```

**效果**：
- 重新加载 `config.yml` 配置
- 平滑重启所有线程池（不丢失任务）
- 清除所有内部缓存
- 重置性能统计计数器
- 更新所有模块配置引用

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
- 调试日志开关切换
- 性能监控参数调整
- 线程数量微调（谨慎使用）
- 避免：启用/禁用核心优化功能

</details>

<details>
<summary><b>/aki-debug &lt;true|false&gt; - 调试模式</b></summary>

**功能**：动态开启/关闭调试日志输出

**权限**：`akiasync.debug`（默认仅OP可用）

**使用方法**：
```bash
/aki-debug true   # 开启调试模式
/aki-debug false  # 关闭调试模式
```

**别名**：支持 `on/off`、`enable/disable`

**效果**：
- 实时切换调试日志开关
- 自动保存到配置文件
- 触发配置重载以应用更改
- 控制性能指标输出（每60秒）

**使用场景**：
- 性能调试时开启，查看详细指标
- 日常使用时关闭，避免控制台刷屏
- 需要监控线程池状态时开启
- 推荐搭配Spark插件使用

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
<summary><b>/aki-version - 版本信息</b></summary>

**功能**：显示插件详细信息和系统环境

**权限**：`akiasync.version`（默认仅OP可用）

**使用方法**：
```bash
/aki-version   # 显示版本信息
```

**显示内容**：
- 插件名称、版本号、作者
- 服务器类型和版本（Leaves/Paper/Spigot）
- Minecraft 版本
- Java 版本
- 操作系统信息
- 当前启用的优化功能状态

**使用场景**：
- 反馈问题时提供版本信息
- 检查当前启用的优化功能
- 确认服务器环境配置
- 生成技术支持报告

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
- 反馈 Bug 时建议先执行此命令，并附上输出截图
- 可用于快速检查配置是否生效

</details>

<details>
<summary><b>/aki-network &lt;true|false&gt; - 网络流量监控</b></summary>

**功能**：在 ActionBar 实时显示服务器网络吞吐量

**权限**：`akiasync.network`（默认仅OP可用）

**使用方法**：
```bash
/aki-network true   # 开启网络监控
/aki-network false  # 关闭网络监控
```

**显示内容**：
- 下行速率（↓）：服务器每秒接收的流量
- 上行速率（↑）：服务器每秒发送的流量
- 使用粉紫色渐变颜色在 ActionBar 显示

**使用场景**：
- 实时监控服务器网络负载
- 诊断网络瓶颈问题
- 观察高峰期流量变化
- 评估网络优化效果

**示例**：
```bash
# 开启网络监控
/aki-network true
# ActionBar 显示：↓ 1.25 MB/s | ↑ 3.42 MB/s

# 关闭网络监控
/aki-network false
```

**说明**：
- 流量数值基于数据包数量估算
- 每个玩家可独立开关显示
- 不影响服务器性能

</details>

---

## 常见问题

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

- 与大部分插件兼容
- 遇到冲突请提交Issue

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

## 开发构建

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

## 开源协议

本项目采用 [GPL-3.0 License](LICENSE) 开源协议

---

## 参考与致谢

本项目参考了以下优秀开源项目：

| 项目 | 贡献 |
|------|------|
| [Starlight](https://github.com/PaperMC/Starlight) / [ScalableLux](https://github.com/RelativityMC/ScalableLux) | 光照传播算法 |
| [Lithium](https://github.com/CaffeineMC/lithium) | 性能优化思路 |
| [C2ME](https://github.com/RelativityMC/C2ME-fabric) | 区块系统优化参考 |
| [ServerCore](https://github.com/Wesley1808/ServerCore) | 热点优化方案 |
| [FerriteCore](https://github.com/malte0811/FerriteCore) | 内存优化策略 |
| [Async](https://github.com/AxalotLDev/Async) | 异步化边界设计 |
| [SecureSeed](https://github.com/Earthcomputer/SecureSeed) | 安全种子设计 |
| [Leaves](https://github.com/LeavesMC/Leaves) | Mixin支持 |
| [Pufferfish](https://github.com/pufferfish-gg/Pufferfish) | 异步设计取经与兼容评估 |
| [Akarin](https://github.com/Akarin-project/Akarin) | 异步设计取经与兼容评估 |
| [tt20](https://github.com/snackbag/tt20) | 异步设计取经与兼容评估 |
| [Sakura](https://github.com/Samsuik/Sakura) | TNT爆炸优化参考 |
| [C3H6N6O6](https://github.com/KenRouKoro/C3H6N6O6) | 实体多线程运算参考 |
| [Velocity](https://github.com/PaperMC/Velocity) | 网络优化参考 |

> **注意**：以上项目用于 Aki-Async 设计取经与兼容评估。仅作参考，不直接打包或复用其代码。

---

## 免责声明

- 本插件通过Mixin修改服务端核心代码，请先在测试服务器验证
- 不保证与所有插件100%兼容
- 如遇问题请提交 [Issue](https://github.com/virgil698/Aki-Async/issues)

---

<div align="center">

### 联系方式

**Issues**: [GitHub Issues](https://github.com/virgil698/Aki-Async/issues)  
**Discord**：[Discord](https://discord.gg/pv9KdHwXWM)
**Email**: virgil698@sky233.top  
**Discord**: xiaokong23357(virgil698)

---

**如果觉得有用，请给个 Star 支持开发！**

Made with ❤️ for Minecraft Server Optimization

</div>
