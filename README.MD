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

## 数据包优化

```text
/akiasync datapack status
```

当前实现包含以下不改变原版 reload 结果的优化：

- 对未变化的 zip datapack 复用资源目录索引，避免每个 reload、Tag 和 Function 查询都重新遍历整个压缩包。
- 以压缩包绝对路径、长度和修改时间作为代际指纹，在 32 MiB 有界 LRU 中复用最多 4 MiB 的已解压资源内容；文件指纹变化后自然失效。
- 在同一个 Commands dispatcher 和 Function 编译源代内按完整命令文本合并并发解析，缓存最多 4 代、每代 32768 条；不会把旧 dispatcher 的命令节点带入新 reload。
- 3-8 条命令的小型 Function 直接进入现有 ExecutionContext 队列，减少 ContinuationTask 分配，同时保留原有 frame、return、嵌套调用和命令额度语义。
- Commands 执行上下文结束后使用 `ThreadLocal.remove()`，避免服务端线程长期保留空槽位。

`status` 会显示最近一次 reload 耗时、Function 数量、编译复用、zip 索引/内容命中、节省解压字节数和当前缓存边界。完整 reload 仍会重建 registry、Tag、Recipe、Commands dispatcher，并执行 Paper 插件生命周期；目录型 datapack 仍按原版扫描，避免漏掉嵌套文件变化。Scoreboard 保持 Paper/Leaves 当前的“值未变化不发包”和批量队伍实现，本阶段不启用会改变同 tick 可观察顺序的延迟合并。

## 任务调度器

```text
/akiasync scheduler status
```

调度器采用 Redis 风格的控制面与多线程计算面：所有提交、完成、失败和取消消息先进入单协调线程，协调线程每批最多处理 64 条状态消息；实际用户计算交给 SpottedLeaf `PrioritisedTaskQueue` 上的有界工作线程并行执行。协调线程不执行用户任务，因此慢计算不会阻塞任务树状态推进。

- 整棵任务树提交时一次性预留节点额度，默认总容量 8192、单树最多 1024 个节点，容量不足时整树拒绝，不会执行一半。
- 父节点成功后，同一层子节点可以在多个 worker 上并行；某一分支失败只跳过它的后代，不影响其他分支。
- 支持 `CRITICAL`、`HIGH`、`NORMAL`、`LOW`、`BACKGROUND` 五级优先级，以及整树取消、失败传播、统计和 generation 失效。
- 默认 worker 数为 `min(4, CPU / 2)` 且至少 1 个，在常见 4 核及以上环境启用并行，同时限制最多 4 个线程，避免与 Leaves/Moonrise 自有线程池争抢全部核心。
- `commitGlobal`、`commitRegion`、`commitEntity` 通过 Paper scheduler 返回所有者线程；owner 提交另有 4096 条容量限制，并在插件 disable 时失效。
- 1.21.10/1.21.11 分别匹配 Leaves 的 concurrentutil 0.0.7/0.0.8，最终 JAR 将其 relocation 到 `com.akiasync.libs.concurrentutil`，不依赖服务端类加载器碰巧暴露的副本。

计算节点只能处理不可变快照，不得读写实时 World、Chunk、Entity、Inventory 或 NMS 对象。标准流程固定为：所有者线程快照 -> 任务树并行计算 -> 校验 generation 和目标身份 -> Paper region/entity scheduler 提交。坐标队列或锁只能约束插件任务，不能赋予 Minecraft 世界状态跨线程访问能力。

任务树 API 的基本形状：

```java
TaskTree<Integer> tree = TaskTree.root("snapshot", SchedulerPriority.NORMAL, context -> 21);
TaskNode<Integer> doubled = tree.root().then(
        "compute",
        SchedulerPriority.HIGH,
        (value, context) -> value * 2
);
TaskTreeHandle<Integer> handle = scheduler.submit(tree);
```

`TaskNode.result()` 表示单个节点结果，`TaskTreeHandle.completion()` 在所有分支都进入终态后返回成功、失败、跳过和取消数量。任务应定期调用 `TaskExecutionContext.checkCancellation()`，长任务不得忽略取消或线程中断。

## 源码结构

- `src/main/java/com/akiasync`：插件主代码
- `src/main/java/com/akiasync/scheduler`：任务树、Redis 风格协调器和 Paper 所有权提交
- `src/mixin/java/com/akiasync/mixin`：Bridge、Mixin 支持代码与注入实现
- `src/mixin/resources`：Mixin 配置和 AccessWidener
- `versions/1.21.11`：Leaves 1.21.11 独立源码与构建配置
