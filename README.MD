# AkiAsync - 异步优化插件

[![License](https://img.shields.io/github/license/LeavesMC/leaves-plugin-template)](LICENSE)

一个专为 **Leaves 1.21.8** 服务端设计的异步优化插件，通过 Mixin 技术对服务端进行深度性能优化。

## ✨ 特性

### 🚀 已实现的异步优化

#### 1. **异步实体追踪器 (Async Entity Tracker)** ✅
- ✅ 将实体位置更新计算移至异步线程
- ✅ 异步处理玩家视距内的实体可见性检查
- ✅ 位置预测和边缘检测优化
- ✅ 减少主线程负载，提升 TPS 稳定性
- ✅ 智能处理：玩家实体保持同步以确保响应速度

#### 2. **异步生物生成 (Async Mob Spawning)** ✅
- ✅ 在 NaturalSpawner 调用点限流（区块密度阈值）
- ✅ Spawner 方块优化
- ✅ 预计算与缓存生成条件

#### 3. **AI Brain 降频 (Brain Throttle)** ✅
- ✅ 静止实体在可配置间隔内跳过 `Brain#tick`，事件发生立即恢复
- ✅ 对低活跃度实体降低AI计算频率

#### 5. **EntityTickList 并行处理** ✅ NEW! (最大热点优化)
- ✅ 将实体tick列表（占76%热点）分段并行处理
- ✅ ForkJoinPool 6线程并行，应对1000+实体场景
- ✅ Leaf风格零反射设计：初始化一次，热路径读volatile
- ✅ 智能阈值：少于50实体不启用，避免overhead

#### 6. **寻路预算调度 (Pathfinding Budget)** ✅
- ✅ 为 `PathNavigation#createPath` 设置每 tick 预算
- ✅ 超出预算的请求加入延迟队列，平滑分散负载

#### 7. **异步光照优化 (Async Lighting)** ✅ ENHANCED! (ScalableLux/Starlight)
- ✅ 区块加载时的异步光照计算
- ✅ 光照更新批量处理（减少线程开销）
- ✅ 天空光缓存（避免重复计算）
- ✅ **真正的分层传播队列**（16层光照等级，Starlight核心算法）
- ✅ **智能批量大小调整**（根据TPS动态调整，8-64范围）
- ✅ **光照更新去重**（防止重复计算，提升30%效率）
- ✅ **传播距离限制**（防止过期更新，5秒超时清理）
- ✅ 专用光照线程池（2线程默认）

### 🎯 规划中的优化
- ⏳ 区块优先级队列（需适配Leaves API）
- ⏳ 红石高级优化（Alternate Current + Carpet，需适配Leaves API）
- ⏳ VMP高并发优化（需适配Leaves API）
- ⏳ 动态追踪范围调整
- ⏳ 网络压缩优化

## 📋 系统要求

- **服务端**: [Leaves](https://github.com/LeavesMC/Leaves) 1.21.8+
- **JDK**: 21 或更高版本
- **启动参数**: 必须添加 `-Dleavesclip.enable.mixin=true`

## 🔧 安装

1. **下载插件 JAR 文件**
   ```bash
   # 或者从 Releases 页面下载
   ./gradlew build
   ```

2. **将插件放入服务器的 `plugins` 文件夹**

3. **修改启动脚本，添加 Mixin 支持**
   ```bash
   java -Dleavesclip.enable.mixin=true -jar leaves.jar
   ```

4. **重启服务器**

## ⚙️ 配置

配置文件位于 `plugins/AkiAsync/config.yml`

```yaml
# 实体追踪器
entity-tracker:
  enabled: true
  thread-pool-size: 4
  update-interval-ticks: 1
  max-queue-size: 1000

# 生物生成
mob-spawning:
  enabled: true
  spawner-optimization: true

# 密度与预算
density:
  max-per-chunk: 80
  ai-cooldown-ticks: 10

pathfinding:
  tick-budget: 128   # 0=禁用预算，建议在 64~256 试验

# Brain 降频
brain:
  throttle: true
  throttle-interval: 10

# 高成本AI异步处理（专用线程池）
expensive-ai:
  enabled: true
  pool-size: 2
  entities:
    - "VILLAGER"
    - "PIGLIN"
    - "WITCH"
    # 可添加自定义实体类型

# 统一异步寻路系统（普通生物）
async-pathfinding:
  enabled: true
  pool-size: 3
  parallel-collision: true
  collision-threads: 2
  async-move: false
  max-move-tasks: 2048

# 性能与日志
performance:
  debug-logging: false
  enable-metrics: true
```

## 🛠️ 开发

### 构建项目

```bash
# 克隆项目
git clone https://github.com/YourUsername/Aki-Async.git
cd Aki-Async

# 构建
./gradlew build

# 运行测试服务器
./gradlew runServer
```

### 项目结构

```
Aki-Async/
├── src/
│   ├── main/
│   │   ├── java/org/virgil/akiasync/
│   │   │   ├── AkiAsyncPlugin.java
│   │   │   ├── config/ConfigManager.java
│   │   │   └── executor/AsyncExecutorManager.java
│   │   └── resources/config.yml
│   └── mixin/
│       ├── java/org/virgil/akiasync/
│       │   ├── async/AsyncConfig.java
│       │   ├── mixin/
│       │   │   ├── brain/BrainThrottleMixin.java
│       │   │   ├── world/CollisionParallelMixin.java
│       │   │   ├── entity/AsyncMoveMixin.java
│       │   │   ├── entitytracker/...
│       │   │   ├── spawning/...
│       │   │   └── pathfinding/PathfindingBudgetMixin.java
│       └── resources/
│           ├── aki-async.mixins.json
│           └── aki-async.accesswidener
└── build.gradle.kts
```

## 📚 参考项目

本项目的实现参考了以下优秀的开源项目：

- [Nitori](https://github.com/Gensokyo-Reimagined/Nitori) - 实体追踪器异步优化思路
- [Leaf](https://github.com/Winds-Studio/Leaf) - 服务端性能优化补丁
- [Leaves](https://github.com/LeavesMC/Leaves) - 支持 Mixin 的服务端

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 开发指南
1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## ⚠️ 免责声明

- 本插件通过 Mixin 修改服务端核心代码，请在使用前充分测试
- 建议先在测试服务器上验证稳定性
- 如遇到问题，请禁用插件并提交 Issue
- 不保证与所有其他插件兼容

## 📄 许可证

本项目遵循 [MIT License](LICENSE) 开源协议。

## 🌟 致谢

- Leaves 团队提供的优秀服务端和 Mixin 支持
- Fabric 团队的 Mixin 技术
- 所有参考项目的开发者们

---

**作者**: Virgil  
**项目地址**: https://github.com/YourUsername/Aki-Async  
**支持版本**: Leaves 1.21.8+

如果觉得这个项目对你有帮助，请给个 ⭐ Star！
