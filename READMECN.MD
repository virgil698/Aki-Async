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
- **完全兼容** - 保持所有原版特性，兼容Paper/Spigot/Leaves/folia/luminol配置

### 优化模块（70+个可配置优化）
- **异步AI优化**（10个）-- 村民、末影人、流浪商人、监守者、疣猪兽、悦灵、犰狳、嗅探兽、骆驼、青蛙、山羊、熊猫、猪灵、劫掠者家族、唤魔者、烈焰人、守卫者、女巫、通用AI模板（65种生物）、异步寻路、游戏事件优化、AI传感器优化、Brain降频、行为节流
- **实体性能优化**（15+个）- 掉落物智能合并、掉落物年龄优化、掉落物Inactive Tick、经验球合并tive Tick、高级碰撞优化、碰撞优化、实体推挤优化、实体节流、实体移动优化、生物消失优化、实体密度控制、抛射物优化、实体Tick并行、Nitori实体优化（日光燃烧、速度、掉落伤害、区段快速检索）
- **方块实体优化**（5个）- 方块实体并行Tick、漏斗优化、矿车优化、熔炉配方缓存、工作台配方缓存
- **TNT爆炸优化**（1个）- 三阶段异步：快照→计算→应用，支持大规模同时爆炸，原版兼容伤害
- **碰撞系统优化**（6个）- 碰撞优化、高级碰撞优化、推挤优化、方块形状缓存、立方体体素形状优化、碰撞缓存
- **光照系统优化**（1个）- 异步光照引擎
- **红石优化**（1个）- 红石线Turbo优化
- **结构定位优化**（4个）- 异步定位命令、海豚寻宝、箱子探险地图、村民地图交易
- **安全种子系统**（3个）- 装饰种子加密、结构种子加密、史莱姆区块种子加密
- **数据包优化**（2个）- 命令去重、Execute命令Inactive跳过
- **原版Bug修复**（8个）- 蜜蜂修复、末地岛屿密度修复、传送门窒息检查、潜影贝导弹自伤修复、矿车炼药锅销毁修复、POI索引更新、闪电生成修复
- **内存管理优化**（7个）- BlockPos对象池、实体BlockPos池、谓词缓存、实体查找缓存、列表预分配、ResourceLocation缓存、ChunkPos优化
- **区块系统优化**（2个）- 异步区块Tick、异步村民繁殖
- **世界生成优化**（3个）- Perlin噪声优化、拼图结构放置优化、结构池元素优化
- **智能滞后补偿**（3个）- 时间补偿、掉落物拾取延迟补偿、药水效果补偿
- **其他优化**（5个）- 数学优化（正弦查找表）、末影人方块携带限制、快速移动区块加载息优化、下落方块并行

> **安全保证**：所有优化目标都经过严格测试，确保不破坏任何原版游戏机制

---

## 系统要求

| 组件 | 要求 |
|------|------|
| **服务端** | [Leaves](https://github.com/LeavesMC/Leaves) 或 [Luminol](https://github.com/LuminolMC/Luminol) 以及其分支 |
| **服务端版本** | 1.21.8+（已测试版本：1.21.8、1.21.10） |
| **JDK** | Java 21+ |
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
