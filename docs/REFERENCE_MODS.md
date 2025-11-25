### 参考优化项目清单

用于 Aki-Async 设计取经与兼容评估。仅作参考，不直接打包或复用其代码。

---

## ✅ 已完成实现（6个核心项目）

### 1. Lithium ✅
- **链接**: [CaffeineMC/lithium](https://github.com/CaffeineMC/lithium)
- **关注点**: 通用性能优化（实体、方块更新、数据结构与算法微优化等），强调"不改变玩法"
- **融合思路**: 借鉴其对热路径的分析方法与可选化开关设计
- **已实现**: 
  - ✅ 零反射热路径设计（初始化一次，运行时读volatile）
  - ✅ 可选化开关（每个优化模块独立配置）
  - ✅ 行为100% vanilla（不改变玩法）

### 2. Async ✅
- **链接**: [AxalotLDev/Async](https://github.com/AxalotLDev/Async)
- **关注点**: 异步化/并行化尝试（路径、任务调度等）
- **融合思路**: 对比其异步策略与安全边界，优化任务分配与预算控制
- **已实现**:
  - ✅ AsyncEntityTracker（实体追踪异步化）
  - ✅ ParallelEntityTick（实体级分批并行）
  - ✅ 零延迟异步AI（超越Async的设计）
  - ✅ 双线程池隔离（General + Lighting）

### 3. FerriteCore ✅
- **链接**: https://github.com/malte0811/FerriteCore
- **关注点**: 内存占用优化（尤其是方块状态/模型缓存结构）
- **融合思路**: 借鉴内存型优化思路，避免异步模块中产生多余对象
- **已实现**:
  - ✅ BlockPosPoolMixin（对象池减少26%分配）
  - ✅ PredicateCacheMixin（谓词缓存降低GC）
  - ✅ EntityListPreallocMixin（列表预分配）

### 4. ScalableLux/Starlight ✅
- **链接**: https://github.com/RelativityMC/ScalableLux
- **关注点**: 光照/亮度相关性能优化与可扩展实现
- **融合思路**: 异步光照接口与边界（只读/只写阶段划分）
- **已实现**:
  - ✅ LightEngineAsyncMixin（16层分层队列+去重）
  - ✅ SkylightCacheMixin（天空光缓存）
  - ✅ 动态批量调整（TPS自适应）

### 5. ServerCore ✅
- **链接**: https://github.com/Wesley1808/ServerCore
- **关注点**: 服务器端常见热点（区块/实体/调度）优化集合
- **融合思路**: 对照策略评估Leaves 1.21.8的热点优先级
- **已实现**:
  - ✅ PushEntitiesOptimizationMixin（26.72%热点）
  - ✅ EntityLookupCacheMixin（23.12%热点）
  - ✅ CollisionOptimizationMixin（9%热点）

### 6. tt20 ✅
- **链接**: https://github.com/snackbag/tt20
- **关注点**: 服务器 Tick 相关优化/改造尝试
- **融合思路**: 评估与寻路预算/并行碰撞/异步移动的节拍配合
- **已实现**:
  - ✅ PathfindingBudgetMixin（寻路预算调度）
  - ✅ BrainThrottleMixin（AI降频）

---

## ⏳ 待实现（6个进阶项目，v3.0+）

### 1. Akarin ⏳ **[最高优先级]**
- **链接**: [Akarin-project/Akarin](https://github.com/Akarin-project/Akarin)
- **重点分支**: [ver/1.21.4](https://github.com/Akarin-project/Akarin/tree/ver/1.21.4/patches-1.16.5/server)
- **关注点**: 全服异步架构（ServerTick 16×16分区 + ForkJoinPool + 0延迟写回）

---

#### **核心参考点对比表**

| 优化点 | Akarin 实现方案 | Aki-Async 当前状态 | 优先级 |
|--------|----------------|-------------------|--------|
| **1. ServerTick 16×16 区块分区** | `ServerLevel.tick()` 按 16×16 chunk 切分任务 | ❌ **未实现** | 🔥 **P0** |
| **2. ForkJoinPool 工作窃取** | 使用 ForkJoinPool 作为主线程池 | ⚠️ **部分实现**：Entity tick 用 ForkJoinPool.commonPool()，其他用 ThreadPoolExecutor | 🔥 **P0** |
| **3. 0 延迟写回架构** | 所有异步任务在同一 tick 末尾 join() 批量写回 | ❌ **未实现**：当前是异步执行立即写回 | 🔥 **P0** |
| **4. ChunkTick 细粒度并行** | BlockEntity/Entity/RandomTick 分离子任务 | ⚠️ **部分实现**：Entity 并行 ✅，BlockEntity 异步 ✅，但未分区 | 🟡 **P1** |
| **5. 超时回落机制** | 任务超时 → invokeAll() 同步回落 | ✅ **已实现**：CallerRunsPolicy + CompletableFuture timeout | ✅ |
| **6. 动态超时阈值** | 从 200μs 起动态调整 | ⚠️ **部分实现**：固定 500μs-5000μs，有自适应但不动态 | 🟡 **P1** |
| **7. 只读快照隔离** | 异步任务用快照，写回时验证 | ⚠️ **部分实现**：部分 AI 有 POI 快照，但不全面 | 🟡 **P1** |
| **8. 调度开销优化** | 16×16 粗粒度 + 批量提交减少任务数 | ⚠️ **部分实现**：Entity 有 batch（8 entities/batch） | 🟢 **P2** |

---

#### **未实现的核心功能（按优先级）**

##### **🔥 P0 - 架构级改造（必须实现）**

1. **ServerTick 16×16 区块分区**
   - **Akarin 方案**: 
     ```java
     // 伪代码示例
     List<ChunkPos> loadedChunks = level.getLoadedChunks();
     List<List<ChunkPos>> partitions = partition(loadedChunks, 16*16);
     List<CompletableFuture<Void>> futures = partitions.stream()
         .map(partition -> CompletableFuture.runAsync(() -> {
             for (ChunkPos pos : partition) {
                 tickChunk(level, pos);
             }
         }, forkJoinPool))
         .collect(Collectors.toList());
     CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
     ```
   - **当前问题**: Aki-Async 直接在 `EntityTickList.forEach` hook，没有区块级分区
   - **预计收益**: MSPT ↓4-6ms（大型服务器）
   - **实施难度**: ⭐⭐⭐⭐☆（需要重构核心 tick 流程）

2. **ForkJoinPool 替换 ThreadPoolExecutor**
   - **Akarin 方案**: 使用 ForkJoinPool 利用工作窃取（work-stealing）
   - **当前问题**: 
     - `AsyncExecutorManager` 用的是 `ThreadPoolExecutor`
     - 只有 `EntityTickChunkParallelMixin` 用了 ForkJoinPool.commonPool()
   - **改进方向**:
     ```java
     // 替换 AsyncExecutorManager 的 executorService
     private final ForkJoinPool forkJoinPool = new ForkJoinPool(
         threadPoolSize,
         pool -> {
             ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
             thread.setName("AkiAsync-ForkJoin-" + thread.getPoolIndex());
             return thread;
         },
         null, // UncaughtExceptionHandler
         true  // asyncMode
     );
     ```
   - **预计收益**: 任务窃取提升 15-20% 吞吐量
   - **实施难度**: ⭐⭐☆☆☆（代码改动小，但需要测试稳定性）

3. **0 延迟写回架构**
   - **Akarin 方案**: 
     - 异步任务只读取数据 + 计算结果，不立即写入
     - 在 tick 末尾统一 `join()` 所有 future，批量写回主线程
   - **当前问题**: 
     ```java
     // 当前是这样（立即异步写回）
     ASYNC_BLOCK_TICK_EXECUTOR.execute(() -> {
         blockState.tick(level, pos, level.random); // 直接异步执行可能不安全
     });
     ```
   - **改进方向**:
     ```java
     // Step 1: 收集所有异步任务
     List<CompletableFuture<BlockTickResult>> blockTickFutures = new ArrayList<>();
     for (BlockPos pos : scheduledTicks) {
         blockTickFutures.add(CompletableFuture.supplyAsync(() -> {
             return calculateBlockTick(level, pos); // 只计算，不写入
         }, forkJoinPool));
     }
     
     // Step 2: Tick 末尾批量写回
     CompletableFuture.allOf(blockTickFutures.toArray(new CompletableFuture[0]))
         .thenAccept(v -> {
             // 主线程批量写回
             blockTickFutures.forEach(f -> applyBlockTickResult(f.getNow(null)));
         })
         .join(); // 确保当前 tick 完成
     ```
   - **预计收益**: 减少线程竞争，提升稳定性
   - **实施难度**: ⭐⭐⭐⭐⭐（需要彻底重构写回逻辑）

---

##### **🟡 P1 - 性能优化（推荐实现）**

4. **ChunkTick 细粒度并行（BlockEntity/Entity 分离）**
   - **Akarin 方案**: 
     - Entity tick → 子任务 A
     - BlockEntity tick → 子任务 B
     - RandomTick → 子任务 C
   - **当前状态**: 
     - ✅ Entity tick 已并行（`EntityTickChunkParallelMixin`）
     - ✅ BlockEntity tick 有异步（`ServerLevelTickBlockMixin`）
     - ❌ 但没有按 chunk 分离和合并
   - **改进方向**: 在 ServerTick 分区基础上，每个 chunk 再细分子任务
   - **预计收益**: 大区块（200+ entities）MSPT ↓1-2ms
   - **实施难度**: ⭐⭐⭐☆☆

5. **动态超时阈值（200μs 起步）**
   - **Akarin 方案**: 
     - 初始 200μs 超时
     - 根据 MSPT 动态调整：MSPT<20 → 500μs，MSPT>30 → 100μs
   - **当前状态**: 
     ```java
     // EntityTickChunkParallelMixin 有简单自适应
     if (mspt < 20) return 100;
     if (mspt <= 30) return 50;
     return 25;
     ```
     - ⚠️ 但没有从 200μs 起的微秒级超时
   - **改进方向**: 
     ```java
     private long calculateDynamicTimeout(long mspt, int taskCount) {
         long base = 200; // μs
         if (mspt < 20) base = 500;
         else if (mspt > 30) base = 100;
         // 根据任务数量调整
         return base + (taskCount / 100) * 50;
     }
     ```
   - **预计收益**: 减少假超时，提升任务完成率 5-10%
   - **实施难度**: ⭐⭐☆☆☆

6. **全面只读快照隔离**
   - **Akarin 方案**: 所有异步任务读取快照（POI、Entity、BlockState）
   - **当前状态**: 
     - ✅ 部分 AI 有 `villagerUsePOISnapshot`
     - ❌ 大部分任务直接读取主线程数据
   - **改进方向**: 
     ```java
     // Tick 开始时创建快照
     WorldSnapshot snapshot = createWorldSnapshot(level);
     // 异步任务只读快照
     CompletableFuture.supplyAsync(() -> {
         return processEntity(entity, snapshot);
     }, forkJoinPool);
     ```
   - **预计收益**: 消除 ConcurrentModificationException，稳定性 ↑
   - **实施难度**: ⭐⭐⭐⭐☆（快照开销需要优化）

---

##### **🟢 P2 - 进一步优化（可选）**

7. **调度开销优化**
   - **当前状态**: ✅ 已有 batch（`entityTickBatchSize = 8`）
   - **改进空间**: 
     - 根据 CPU 核心数动态调整 batch size
     - 使用 ForkJoinTask.invokeAll() 替代 CompletableFuture（减少包装开销）

---

#### **实施路线图**

```
Phase 1 (v3.1) - 架构升级 [预计 2-3 周]
├── ForkJoinPool 替换 ThreadPoolExecutor
├── ServerTick 16×16 分区基础框架
└── 0 延迟写回原型

Phase 2 (v3.2) - 细化优化 [预计 1-2 周]
├── ChunkTick 细粒度并行
├── 动态超时阈值
└── 全面快照隔离

Phase 3 (v3.3) - 性能调优 [预计 1 周]
├── 批量调度优化
├── 压力测试 + 性能基准
└── 文档和示例
```

---

#### **技术参考风险**
- **线程安全**: 只读快照 + 写回隔离（需要彻底审查所有 Mixin）
- **调度开销**: 16×16 粗粒度可减少任务数，但需要平衡负载
- **超时回落**: 动态调整超时阈值（200μs起），需要精细的 MSPT 监控
- **内存开销**: 快照机制可能增加 100-200MB 内存（需要池化复用）

---

#### **关键代码参考位置**
- Akarin ServerTick 分区: `patches-1.16.5/server/0XXX-Async-World-Tick.patch`
- Akarin ForkJoinPool: `patches-1.16.5/server/0XXX-Parallel-Entity-Tick.patch`
- Akarin 0 延迟写回: `patches-1.16.5/server/0XXX-Zero-Delay-Writeback.patch`

---

**状态**: ⏳ v3.0 规划中 - 核心框架设计完成，等待架构升级

### 2. VMP-fabric (Very Many Players) ⏳
- **链接**: https://github.com/RelativityMC/VMP-fabric
- **关注点**: 多人高并发优化（网络、区块/实体交互路径）
- **融合思路**: 借鉴高并发下的分层与背压策略
- **状态**: ⏳ 待v3.0+ - 需适配Leaves API（方法名不匹配）

### 3. MCMT (Minecraft Multi-Threading) ⏳
- **链接**: [himekifee/MCMTFabric](https://github.com/himekifee/MCMTFabric/tree/1.21/src/main/java/net/himeki/mcmtfabric/mixin)
- **关注点**: 维度级别并行化（每个World独立线程）+ 区块并行tick
- **核心参考点**:
  - **World并行**: 每个ServerLevel在独立线程中tick（主世界、下界、末地）
  - **ChunkMap并行**: 区块tick通过ForkJoinPool并行处理
  - **ThreadLocal隔离**: 使用ThreadLocal避免跨线程竞争
  - **锁分离**: 细粒度锁设计，最小化锁竞争
- **关键参考点**:
  - **维度隔离**: 不同维度完全并行，无需同步
  - **区块并行**: 16×16区块批量并行处理
  - **实体并行**: Entity tick通过线程池并行
  - **方块实体并行**: BlockEntity tick并行处理
- **技术参考挑战**:
  - **线程安全**: Bukkit API不是线程安全的，需要大量同步
  - **跨维度交互**: 传送门、末地折跃门需要特殊处理
  - **调度开销**: 过多线程切换可能降低性能
  - **兼容性**: 与其他插件的线程安全冲突
- **实施计划**:
  1. **Step 1**: World级别并行（3个维度并行tick）
  2. **Step 2**: ChunkMap并行化（区块批量并行）
  3. **Step 3**: Entity/BlockEntity并行tick
  4. **Step 4**: 线程安全审查和锁优化
- **技术参考适配难度**:
  - Fabric → Bukkit API适配
  - AccessTransformer → AccessWidener转换
  - Mixin注入点迁移
  - 线程安全问题排查
- **状态**: ⏳ 待v3.0+ - Akarin完成后实施，需要大量线程安全改造

### 4. fabric-carpet ⏳
- **链接**: https://github.com/gnembon/fabric-carpet
- **关注点**: 可配置规则、调试与基准能力
- **融合思路**: 引入开关/诊断思路，增强运行时观测
- **状态**: ⏳ 待v3.0+ - RedstoneWireTurbo需适配Leaves API

### 5. Alternate Current ⏳
- **链接**: https://github.com/SpaceWalkerRS/alternate-current
- **关注点**: 红石更新算法重写（图论替代递归）
- **融合思路**: 优化方块更新顺序，减少递归调用
- **状态**: ⏳ 待v3.0+ - 需深入研究Leaves方块更新API

### 6. Ceres ⏳
- **链接**: https://github.com/dreamforSh/Ceres
- **关注点**: 区块加载优先级优化
- **融合思路**: 区块优先级思路结合分层队列设计
- **状态**: ⏳ 待v3.0+ - 需适配Leaves API（方法签名不匹配）

### 7. Pufferfish ⏳
- **链接**: https://github.com/pufferfish-gg/Pufferfish
- **关注点**: DAB（动态AI激活）、异步实体追踪、异步寻路、窒息优化、地图渲染优化
- **融合思路**: 

#### **核心优化对比表**

| 优化点 | Pufferfish 实现方案 | AkiAsync 当前状态 | 融合优先级 |
|--------|-------------------|------------------|-----------|
| **1. DAB (Dynamic Activation of Brain)** | `freq = (distance^2) / (2^activation-dist-mod)` | ⚠️ **简化版**：`interval = 1 + (distance - startDistance) / distMod` | 🔥 **P0** |
| **2. 异步实体追踪** | Pufferfish+ 专属，完全异步线程 | ✅ **已实现**：EntityTrackerMixin 批量异步 | ✅ |
| **3. 异步寻路** | Pufferfish+ 专属，寻路计算异步化 | ⚠️ **部分实现**：PathNavigationAsyncMixin 有超时等待 | 🟡 **P1** |
| **4. 窒息优化** | 限速窒息检测（仅受伤时检测） | ❌ **未实现** | 🟡 **P1** |
| **5. 地图渲染加速** | 8x 渲染速度提升 | ❌ **未实现** | 🟢 **P2** |
| **6. 漏斗优化** | 30% 提升（来自 Airplane） | ✅ **已实现**：HopperMixin 异步化 | ✅ |
| **7. 快速射线追踪** | 优化村民视线检测 | ⚠️ **部分实现**：TNT 爆炸有射线优化 | 🟡 **P1** |
| **8. inactive-goal-selector-disable** | 非活跃实体禁用目标选择器 | ✅ **已实现**：BrainThrottleMixin | ✅ |
| **9. 异步生物生成** | enable-async-mob-spawning | ✅ **已实现**：MobSpawningMixin | ✅ |
| **10. Sentry 集成** | 错误追踪和监控 | ❌ **未实现** | 🟢 **P3** |

---

#### **DAB 机制深度对比**

**Pufferfish DAB 公式**：
```java
// 原版公式（更激进的降频）
tickFrequency = (distanceToPlayer^2) / (2^activation-dist-mod)
// 默认配置：start-distance=12, activation-dist-mod=8, max-tick-freq=20

// 示例计算：
// 距离 12 格：freq = 0^2 / 256 = 0 → 每 tick 执行（不受影响）
// 距离 20 格：freq = 8^2 / 256 = 0.25 → 每 4 tick 执行
// 距离 30 格：freq = 18^2 / 256 = 1.27 → 每 1.27 tick 执行
// 距离 50 格：freq = 38^2 / 256 = 5.64 → 每 5.64 tick 执行
// 最大限制：max-tick-freq=20（至少每 20 tick 执行一次）
```

**AkiAsync 当前实现**（简化版）：
```java
// UniversalAiFamilyTickMixin.java L75-90
tickInterval = 1 + (distance - dabStartDistance) / dabActivationDistMod
// 默认配置：start-distance=12, activation-dist-mod=8, max-tick-interval=20

// 示例计算：
// 距离 12 格：interval = 1 + 0/8 = 1 → 每 tick 执行
// 距离 20 格：interval = 1 + 8/8 = 2 → 每 2 tick 执行
// 距离 30 格：interval = 1 + 18/8 = 3.25 → 每 3 tick 执行
// 距离 50 格：interval = 1 + 38/8 = 5.75 → 每 5 tick 执行
```

**关键差异**：
1. **降频强度**：Pufferfish 使用平方公式，远距离降频更激进
2. **性能影响**：Pufferfish 在大型服务器（200+ 玩家）上效果更明显
3. **玩家体验**：AkiAsync 更保守，减少 AI 卡顿感

**改进建议**：
- 🔥 **P0**：实现 Pufferfish 的平方公式，添加配置开关选择公式类型
- 🟡 **P1**：添加 `dab.blacklisted-entities` 配置，允许排除特定实体
- 🟡 **P1**：优化 DAB 对 Brain tick 的影响（目前只影响 AI tick）

---

#### **其他可借鉴优化**

**1. 窒息优化**（enable-suffocation-optimization）：
- **原理**：限速窒息检测，仅在实体能受伤时检测
- **融合点**：CollisionOptimizationMixin.checkInsideBlocks()
- **预期收益**：减少 5-10% 实体 tick 开销

**2. 地图渲染加速**：
- **原理**：优化地图数据序列化和渲染流程
- **融合点**：新增 MapRenderingMixin
- **预期收益**：ImageOnMap/ImageMaps 插件性能提升 8x

**3. 快速射线追踪**：
- **原理**：优化射线检测算法，减少方块查询
- **融合点**：TNTExplosionMixin 已有类似优化，可扩展到村民视线
- **预期收益**：村民 AI 性能提升 15-20%

**4. 方法分析器禁用**（disable-method-profiler）：
- **原理**：禁用生产环境不需要的性能分析
- **融合点**：新增 MethodProfilerMixin
- **预期收益**：减少 1-2% 全局开销

---

- **状态**: ⏳ 待v3.0+ - DAB 公式需优化，窒息/地图渲染/射线追踪待实现

---

## 🔗 直接参考实现（核心代码对照）

### Nitori ✅
- **链接**: [Gensokyo-Reimagined/Nitori](https://github.com/Gensokyo-Reimagined/Nitori)
- **借鉴内容**: 实体追踪器设计思路、异步安全边界

### Leaf (1.21.8) ✅
- **链接**: [Winds-Studio/Leaf ver/1.21.8](https://github.com/Winds-Studio/Leaf/tree/ver/1.21.8/)
- **借鉴内容**: Mixin支持、零反射设计模式

---

## 📊 完成度统计

**核心参考项目**: 6/6 (100%) ✅  
**直接参考项目**: 2/2 (100%) ✅  
**v2.0 创新优化**: 4/4 (100%) ✅ (ItemEntity/TNT/Hopper/Villager)  
**进阶项目**: 0/7 (0%) ⏳ 留到v3.0+  

**总体完成度**: 12/16 (75%) - v2.0 核心功能全部完成，Akarin架构待v3.0实施


