### 参考优化项目清单

用于 Aki-Async 设计取经与兼容评估。仅作参考，不直接打包或复用其代码。

- 项目名: Lithium
  - 链接: [CaffeineMC/lithium](https://github.com/CaffeineMC/lithium)
  - 关注点: 通用性能优化（实体、方块更新、数据结构与算法微优化等），强调“不改变玩法”。
  - 融合思路: 借鉴其对热路径的分析方法与可选化开关设计。

- 项目名: Async
  - 链接: [AxalotLDev/Async](https://github.com/AxalotLDev/Async)
  - 关注点: 异步化/并行化尝试（路径、任务调度等）。
  - 融合思路: 对比其异步策略与安全边界，优化我们在 Leaves 的任务分配与预算控制。

- 项目名: FerriteCore
  - 链接: https://github.com/malte0811/FerriteCore
  - 关注点: 内存占用优化（尤其是方块状态/模型缓存结构）。
  - 融合思路: 借鉴内存型优化的思路，避免在异步模块中产生多余对象与重复结构。

- 项目名: ScalableLux
  - 链接: https://github.com/RelativityMC/ScalableLux
  - 关注点: 光照/亮度相关性能优化与可扩展实现。
  - 融合思路: 为后续"异步光照"预研接口与边界（只读/只写阶段划分）。
  - ✅ 已实现: ChunkLightingAsyncMixin, LightEngineAsyncMixin, SkylightCacheMixin

- 项目名: ServerCore
  - 链接: https://github.com/Wesley1808/ServerCore
  - 关注点: 服务器端常见热点（区块/实体/调度）优化集合。
  - 融合思路: 对照其策略评估我们在 Leaves 1.21.8 的热点优先级。

- 项目名: tt20
  - 链接: https://github.com/snackbag/tt20
  - 关注点: 服务器 Tick 相关优化/改造尝试。
  - 融合思路: 评估与我们的“寻路预算/并行碰撞/异步移动”的节拍配合。

- 项目名: VMP-fabric (Very Many Players)
  - 链接: https://github.com/RelativityMC/VMP-fabric
  - 关注点: 多人高并发优化（网络、区块/实体交互路径）。
  - 融合思路: 借鉴其在高并发下的分层与背压策略，完善我们的执行器与限流。
  - ⏳ 待实现: 需要适配Leaves API（方法名不匹配）

- 项目名: fabric-carpet
  - 链接: https://github.com/gnembon/fabric-carpet
  - 关注点: 可配置规则、调试与基准能力。
  - 融合思路: 引入类似的开关/诊断思路，增强我们对异步/并行策略的运行时观测。
  - ⏳ 待实现: RedstoneWireTurbo（需适配Leaves API）

- 项目名: Alternate Current
  - 链接: https://github.com/SpaceWalkerRS/alternate-current
  - 关注点: 红石更新算法重写，使用图论算法替代递归。
  - 融合思路: 优化方块更新顺序，减少不必要的递归调用。
  - ⏳ 待实现: 需要深入研究Leaves的方块更新API

- 项目名: Ceres
  - 链接: https://github.com/dreamforSh/Ceres
  - 关注点: 区块加载优先级优化
  - 融合思路: 参考其区块优先级思路，结合我们的分层队列设计
  - ⏳ 待实现: 需适配Leaves API（方法签名不匹配）

---

直接参考实现的项目（核心代码对照来源）：
- Nitori: [Gensokyo-Reimagined/Nitori](https://github.com/Gensokyo-Reimagined/Nitori)
- Leaf (1.21.8): [Winds-Studio/Leaf ver/1.21.8](https://github.com/Winds-Studio/Leaf/tree/ver/1.21.8/)


