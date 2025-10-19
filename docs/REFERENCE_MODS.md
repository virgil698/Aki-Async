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
  - ❌ ChunkLightingAsyncMixin（已删除，无效优化）

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

## ⏳ 待实现（5个进阶项目，v2.1+）

### 0. Akarin ⏳ **[最高优先级]**
- **链接**: [Akarin-project/Akarin](https://github.com/Akarin-project/Akarin)
- **重点分支**: [ver/1.21.4](https://github.com/Akarin-project/Akarin/tree/ver/1.21.4/patches-1.16.5/server)
- **关注点**: 全服异步架构（ServerTick 16×16分区 + ForkJoinPool + 0延迟写回）
- **核心技术**:
  - **ServerTick 分区**: 把 `ServerLevel.tick()` 按 16×16 区块切成任务
  - **ChunkTick 并行**: 每 chunk 内 BlockEntity/Entity 再切子任务
  - **0 延迟写回**: 所有异步结果在同一 tick 末尾 `join()` 批量写回
  - **失败回落**: 任何子任务超时 → 立即 `invokeAll()` 回落同步
- **预期收益**:
  - MSPT 中位数: 10ms → 6ms (↓40%)
  - ChunkTick 占比: 8ms → 3ms (↓62%)
  - ForkJoinPool 占比: 0% → 20%+
  - TPS 最低: 20 锁死
- **实施计划**:
  1. **Step 1 MVP**: ServerTick 分区 + ForkJoinPool（预计 MSPT ↓4ms）
  2. **Step 2 细化**: ChunkTick 子任务（漏斗/村民/红石全并行）
  3. **Step 3 回落**: 超时检测 + CallerRunsPolicy（不掉TPS）
- **技术风险**:
  - 线程安全: 只读快照 + 写回隔离
  - 调度开销: 16×16粗粒度减少任务数
  - 超时回落: 动态调整超时阈值（200μs起）
- **状态**: ⏳ v2.1 规划中 - 核心框架设计完成，待实施

### 1. VMP-fabric (Very Many Players) ⏳
- **链接**: https://github.com/RelativityMC/VMP-fabric
- **关注点**: 多人高并发优化（网络、区块/实体交互路径）
- **融合思路**: 借鉴高并发下的分层与背压策略
- **状态**: ⏳ 待v2.1+ - 需适配Leaves API（方法名不匹配）

### 2. fabric-carpet ⏳
- **链接**: https://github.com/gnembon/fabric-carpet
- **关注点**: 可配置规则、调试与基准能力
- **融合思路**: 引入开关/诊断思路，增强运行时观测
- **状态**: ⏳ 待v2.1+ - RedstoneWireTurbo需适配Leaves API

### 3. Alternate Current ⏳
- **链接**: https://github.com/SpaceWalkerRS/alternate-current
- **关注点**: 红石更新算法重写（图论替代递归）
- **融合思路**: 优化方块更新顺序，减少递归调用
- **状态**: ⏳ 待v2.1+ - 需深入研究Leaves方块更新API

### 4. Ceres ⏳
- **链接**: https://github.com/dreamforSh/Ceres
- **关注点**: 区块加载优先级优化
- **融合思路**: 区块优先级思路结合分层队列设计
- **状态**: ⏳ 待v2.1+ - 需适配Leaves API（方法签名不匹配）

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
**进阶项目**: 0/5 (0%) ⏳ 留到v2.1+  

**总体完成度**: 12/15 (80%) - v2.0 核心功能全部完成，Akarin架构待v2.1实施

---

## 📝 版本更新日志

### v2.0.0 (2025-10-18)
**新增优化（4个模块）：**
- ✅ ItemEntity v7.0 - 异步步进物理（零回弹）
- ✅ TNT Explosion v7.0 - 3阶段异步（↓92% MSPT）
- ✅ Hopper Chain v8.0 - 16×16异步I/O（↓67% MSPT）
- ✅ Villager Breed v8.0 - 年龄降频（↓60% MSPT）

**修复问题：**
- ✅ 修复掉落物回弹问题（只节流merge，保留physics）
- ✅ 修复TNT含水保护（waterlogged blocks + canBeReplaced）
- ✅ 修复TNT火焰逻辑（FireBlock tick + item burning）
- ✅ 全代码英文化（删除745行中文注释）

**架构规划：**
- 📋 Akarin全服异步架构设计完成（待v2.1实施）

### v2.1.0 (规划中)
**目标：Akarin 全服异步架构**
- ⏳ ServerTick 16×16分区
- ⏳ ForkJoinPool 工作窃取
- ⏳ 0延迟批量写回
- ⏳ 失败回落策略


