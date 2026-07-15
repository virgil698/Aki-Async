# Aki-Async

Aki-Async 是一个基于 Leaves 插件模板初始化的项目，包含 NMS、Mixin、Mixin Extras 与 AccessWidener 的开发配置。

## 环境要求

- JDK 21 或更高版本
- Gradle Wrapper（已包含在项目中）

## 项目信息

- 项目名：`Aki-Async`
- 包路径：`com.akiasync`
- 版本：`4.0`
- 目标服务端：Leaves

## 构建

```bash
./gradlew build
```

Windows 下可以运行：

```powershell
.\gradlew.bat build
```

构建产物位于 `build/libs/Aki-Async-4.0.jar`。

1.21.11 版本可独立构建：

```powershell
.\gradlew.bat -p versions/1.21.11 build
```

## 卡顿来源检测

```text
/akiasync lag status
/akiasync lag report
/akiasync lag inspect <tickId>
/akiasync lag top [count]
/akiasync lag ranges
/akiasync lag categories
/akiasync lag arm [ticks]
```

探针精确记录世界、区块、生物、普通实体、方块实体、插件同步任务和事件监听器的边界耗时。服务器 tick 持续超过 25 ms 时，独立守护采样器每 2 ms 捕获一次服务器线程栈，用于定位生物 AI、插件、命令、区块、世界生成、网络和 I/O 等内部热点。

对外报告统一使用 16 类来源：生物、实体、方块、区块、插件、世界保存、网络、GC、CPU、内存、磁盘IO、地图生成、玩家行为、命令、数据包、配置问题。其中“数据包”指 Minecraft datapack，协议 packet 处理归入“网络”。只有具备计时、采样栈或 JVM 指标证据时才会归类；证据不足的通用世界 tick 会保留为“未归因”，不会强行猜测成区块卡顿。

卡顿等级范围：正常 `< 50 ms`、轻微 `50-75 ms`、中度 `75-150 ms`、严重 `150-500 ms`、致命 `>= 500 ms`。上限均为开区间。

## 源码结构

- `src/main/java/com/akiasync`：插件主代码
- `src/mixin/java/com/akiasync/mixin`：Bridge、Mixin 支持代码与注入实现
- `src/mixin/resources`：Mixin 配置和 AccessWidener
- `versions/1.21.11`：Leaves 1.21.11 独立源码与构建配置
