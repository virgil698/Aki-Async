# Canvas服务端优化分析与AkiAsync改进方案

## 一、Canvas核心架构特点

### 1.1 区域线程模型 (Region Threading)

Canvas基于Folia的区域并行架构，实现了真正的多线程区块处理：

**核心不变量 (Invariants)**:
1. **第一不变量**: 每个区块持有者(chunk holder)有且仅有一个对应的区域
2. **第二不变量**: 区域拥有"合并半径"内的所有区块，确保区域间不会太近
3. **第三不变量**: 正在tick的区域不能扩展其拥有的区块位置
4. **第四不变量**: 区域只能处于四种状态之一：transient、ready、ticking、dead

**区域状态机**:
```
transient (临时) → ready (就绪) → ticking (执行中) → dead (已死亡)
                      ↑                    ↓
                      └────── merge ───────┘
```

### 1.2 独立区域调度 (Independent Region Scheduling)

**EDF-like调度算法**:
- 每个区域维护独立的tick截止时间
- 使用SchedulerThreadPool实现最早开始时间优先调度
- 区域A的延迟不会影响区域B的调度
- 只要线程池未满载，所有<=50ms的区域都能保持20TPS

**示例场景**:
```
t=0ms:   区域B开始tick
t=15ms:  区域A开始tick (独立调度)
t=20ms:  区域B完成tick，下次调度在t=50ms
t=50ms:  区域B开始第2次tick (不受A影响)
t=95ms:  区域A完成tick (落后不影响B)
```

### 1.3 区域间操作安全机制

**跨区域操作处理**:
- 使用Region Scheduler调度跨区域任务
- 实体传送通过异步队列处理
- 命令系统重写为异步验证+区域调度
- /save-all等命令标记所有区域在下次tick保存

## 二、Canvas的关键优化技术

### 2.1 异步命令系统

**重写的命令**:
- `/spreadplayers`: 完全异步化，只在需要验证数据时调度到区域
- `/save-all`: 异步标记所有区域保存，避免线程所有权问题
- `/tick`: 配合调度器重写，支持动态tick速率

**设计原则**:
```java
// 伪代码示例
async {
    // 1. 异步收集数据
    List<Data> data = collectDataAsync();
    
    // 2. 调度到区域验证
    for (Region region : regions) {
        region.schedule(() -> {
            validateAndApply(data);
        });
    }
}
```

### 2.2 区域合并与分裂优化

**智能合并策略**:
- 相邻区域在合适时机自动合并
- 避免"战斗"现象（多个区域争夺同一区块）
- Transient区域作为缓冲，等待ticking区域完成

**分裂优化**:
- 检测独立子区域并自动分裂
- 最大化并行度
- 保持区域数据对象的一致性

### 2.3 Tick计数器独立性

**问题**: 不同区域有独立的tick计数器，可能导致不一致

**Canvas解决方案**:
- 区域级别的tick计数
- 跨区域操作使用全局时间戳
- 实体传送时同步tick状态

## 三、AkiAsync可借鉴的优化思路

### 3.1 区域感知的异步优化

**当前问题**:
- AkiAsync的异步优化在Folia环境下可能跨区域操作
- 缺少区域边界检测和智能调度

**改进方案**:

#### 3.1.1 增强区域边界检测

```java
// 新增：智能区域边界检测
public class RegionBoundaryDetector {
    
    /**
     * 检测操作是否跨越多个区域
     * Canvas启发：使用"合并半径"概念
     */
    public static boolean isCrossRegionOperation(Level level, BlockPos center, int radius) {
        if (!FoliaUtils.isFoliaEnvironment()) {
            return false;
        }
        
        // Canvas的合并半径通常是2-3个区块
        int chunkRadius = (radius >> 4) + 1;
        return chunkRadius > 2;
    }
    
    /**
     * 获取操作涉及的所有区域
     */
    public static Set<ChunkPos> getAffectedRegions(BlockPos center, int radius) {
        Set<ChunkPos> regions = new HashSet<>();
        int chunkRadius = (radius >> 4) + 1;
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        
        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                regions.add(new ChunkPos(centerChunkX + x, centerChunkZ + z));
            }
        }
        return regions;
    }
}
```

#### 3.1.2 区域调度优化器

```java
// 新增：Canvas风格的区域调度
public class RegionAwareScheduler {
    
    /**
     * Canvas启发：异步收集+区域调度应用
     */
    public static <T> CompletableFuture<Void> scheduleRegionOperation(
            Level level,
            BlockPos center,
            int radius,
            Supplier<T> asyncCollector,
            BiConsumer<T, ChunkPos> regionApplier) {
        
        // 阶段1：异步收集数据（Canvas的异步验证阶段）
        return CompletableFuture.supplyAsync(asyncCollector, executor)
            .thenAccept(data -> {
                // 阶段2：调度到各个区域应用（Canvas的区域调度阶段）
                Set<ChunkPos> regions = RegionBoundaryDetector.getAffectedRegions(center, radius);
                
                for (ChunkPos region : regions) {
                    FoliaRegionContext.executeChunkOperation(plugin, level, region, () -> {
                        regionApplier.accept(data, region);
                    });
                }
            });
    }
}
```

### 3.2 TNT爆炸优化改进

**Canvas启发**: 跨区域操作应该分阶段处理

**改进方案**:

```java
// 修改：TNTExplosionMixin
public class TNTExplosionMixin {
    
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void optimizeExplosion(CallbackInfo ci) {
        if (FoliaUtils.isFoliaEnvironment()) {
            // Canvas风格：检测是否跨区域
            if (RegionBoundaryDetector.isCrossRegionOperation(level, blockPos, (int)power * 2)) {
                handleCrossRegionExplosion(ci);
                return;
            }
        }
        
        // 单区域爆炸：保持现有优化
        handleSingleRegionExplosion(ci);
    }
    
    private void handleCrossRegionExplosion(CallbackInfo ci) {
        ci.cancel();
        
        // 阶段1：异步计算爆炸影响（不访问世界数据）
        CompletableFuture<ExplosionData> dataFuture = CompletableFuture.supplyAsync(() -> {
            return calculateExplosionData(power, center);
        }, executor);
        
        // 阶段2：调度到各个区域应用
        dataFuture.thenAccept(data -> {
            Set<ChunkPos> affectedRegions = data.getAffectedRegions();
            
            for (ChunkPos region : affectedRegions) {
                FoliaRegionContext.executeChunkOperation(plugin, level, region, () -> {
                    applyExplosionToRegion(data, region);
                });
            }
        });
    }
}
```

### 3.3 实体追踪优化改进

**Canvas启发**: 区域内仍需优化，但要避免跨区域竞争

**改进方案**:

```java
// 修改：EntityTrackerMixin
public class EntityTrackerMixin {
    
    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void optimizeTracking(ServerPlayer player, CallbackInfo ci) {
        if (FoliaUtils.isFoliaEnvironment()) {
            // Canvas启发：区域内批量处理，区域间隔离
            if (!FoliaRegionContext.canAccessEntityDirectly(entity)) {
                // 跨区域实体：延迟到正确的区域处理
                FoliaRegionContext.executeEntityOperation(plugin, entity, () -> {
                    updatePlayerOriginal(player);
                });
                ci.cancel();
                return;
            }
            
            // 同区域实体：批量优化（Canvas：区域内仍是单线程，可以优化）
            batchUpdateInRegion(player);
            ci.cancel();
        }
    }
    
    private void batchUpdateInRegion(ServerPlayer player) {
        // Canvas启发：利用区域内的单线程特性，安全地批量处理
        List<Entity> regionEntities = collectRegionEntities(player);
        
        // 异步计算可见性（不访问世界数据）
        CompletableFuture.runAsync(() -> {
            Map<Entity, Boolean> visibility = calculateVisibility(player, regionEntities);
            
            // 回到区域线程应用
            FoliaSchedulerAdapter.runEntityTask(plugin, player.getBukkitEntity(), () -> {
                applyVisibilityChanges(player, visibility);
            });
        }, executor);
    }
}
```

### 3.4 AI优化改进

**Canvas启发**: 区域独立tick，AI可以更激进地优化

**改进方案**:

```java
// 修改：UniversalAiFamilyTickMixin
public class UniversalAiFamilyTickMixin {
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void optimizeAI(CallbackInfo ci) {
        if (FoliaUtils.isFoliaEnvironment()) {
            // Canvas启发：区域内AI可以更激进地节流
            // 因为区域独立tick，不会影响其他区域
            
            // Folia环境：1tick间隔 → 可以改为2tick
            // 理由：Canvas的区域并行已经提供了性能提升
            int tickInterval = 2; // 之前是1
            
            if (level.getGameTime() < aki$next) {
                ci.cancel();
                return;
            }
            
            aki$next = level.getGameTime() + tickInterval;
        }
        
        // 保持所有保护机制
        if (aki$shouldProtectAI(mob)) {
            return;
        }
        
        // ... 其他逻辑
    }
}
```

### 3.5 光照引擎优化改进

**Canvas启发**: 区域边界的光照传播需要特殊处理

**改进方案**:

```java
// 修改：LightEngineAsyncMixin
public class LightEngineAsyncMixin {
    
    public void checkBlock(BlockPos pos) {
        if (FoliaUtils.isFoliaEnvironment()) {
            // Canvas启发：检测光照传播是否跨区域
            if (isLightPropagationCrossRegion(pos)) {
                handleCrossRegionLight(pos);
                return;
            }
        }
        
        // 单区域光照：保持异步优化
        handleSingleRegionLight(pos);
    }
    
    private boolean isLightPropagationCrossRegion(BlockPos pos) {
        // 光照最多传播15格，检测是否跨越区域边界
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        
        // 检测周围15格是否在同一区域
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                BlockPos neighbor = pos.offset(dx * 16, 0, dz * 16);
                if (!FoliaRegionContext.canAccessBlockPosDirectly(level, neighbor)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void handleCrossRegionLight(BlockPos pos) {
        // Canvas风格：分区域处理光照
        Set<ChunkPos> affectedChunks = getLightAffectedChunks(pos);
        
        for (ChunkPos chunk : affectedChunks) {
            FoliaRegionContext.executeChunkOperation(plugin, level, chunk, () -> {
                propagateLightInChunk(pos, chunk);
            });
        }
    }
}
```

## 四、新特性建议

### 4.1 区域性能监控

**Canvas启发**: 每个区域独立tick，可以单独监控

```java
// 新增：区域性能分析器
public class RegionPerformanceAnalyzer {
    
    private static final ConcurrentHashMap<ChunkPos, RegionStats> regionStats = new ConcurrentHashMap<>();
    
    public static class RegionStats {
        private final AtomicLong totalTickTime = new AtomicLong(0);
        private final AtomicInteger tickCount = new AtomicInteger(0);
        private final AtomicInteger entityCount = new AtomicInteger(0);
        private final AtomicInteger chunkCount = new AtomicInteger(0);
        
        public void recordTick(long nanos, int entities, int chunks) {
            totalTickTime.addAndGet(nanos);
            tickCount.incrementAndGet();
            entityCount.set(entities);
            chunkCount.set(chunks);
        }
        
        public double getAverageTickTime() {
            int count = tickCount.get();
            return count > 0 ? totalTickTime.get() / (double)count / 1_000_000.0 : 0.0;
        }
    }
    
    /**
     * 记录区域tick性能
     */
    public static void recordRegionTick(Level level, ChunkPos region, long nanos, int entities, int chunks) {
        if (!FoliaUtils.isFoliaEnvironment()) return;
        
        regionStats.computeIfAbsent(region, k -> new RegionStats())
                   .recordTick(nanos, entities, chunks);
    }
    
    /**
     * 获取性能报告
     */
    public static String getPerformanceReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6=== 区域性能报告 ===\n");
        
        List<Map.Entry<ChunkPos, RegionStats>> sorted = regionStats.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue().getAverageTickTime(), a.getValue().getAverageTickTime()))
            .limit(10)
            .toList();
        
        for (Map.Entry<ChunkPos, RegionStats> entry : sorted) {
            ChunkPos pos = entry.getKey();
            RegionStats stats = entry.getValue();
            sb.append(String.format("§e区域 [%d, %d]: §f%.2fms §7(实体: %d, 区块: %d)\n",
                pos.x, pos.z,
                stats.getAverageTickTime(),
                stats.entityCount.get(),
                stats.chunkCount.get()));
        }
        
        return sb.toString();
    }
}
```

### 4.2 智能负载均衡

**Canvas启发**: 利用区域独立性，实现动态负载均衡

```java
// 新增：负载均衡器
public class RegionLoadBalancer {
    
    /**
     * 根据区域负载动态调整优化强度
     */
    public static int getOptimalTickInterval(Level level, BlockPos pos) {
        if (!FoliaUtils.isFoliaEnvironment()) {
            return 3; // 默认值
        }
        
        ChunkPos region = new ChunkPos(pos);
        RegionStats stats = RegionPerformanceAnalyzer.getStats(region);
        
        if (stats == null) {
            return 3;
        }
        
        double avgTickTime = stats.getAverageTickTime();
        
        // Canvas启发：根据区域负载动态调整
        if (avgTickTime < 20.0) {
            return 3; // 轻负载：正常节流
        } else if (avgTickTime < 40.0) {
            return 2; // 中负载：减少节流
        } else {
            return 1; // 重负载：最小节流
        }
    }
}
```

### 4.3 区域预热机制

**Canvas启发**: 区域状态转换需要时间，可以预热

```java
// 新增：区域预热器
public class RegionPrewarmer {
    
    private static final Set<ChunkPos> prewarmingRegions = ConcurrentHashMap.newKeySet();
    
    /**
     * 预热即将活跃的区域
     */
    public static void prewarmRegion(Level level, ChunkPos region) {
        if (!FoliaUtils.isFoliaEnvironment()) return;
        
        if (prewarmingRegions.add(region)) {
            // Canvas启发：提前加载区域数据
            FoliaSchedulerAdapter.runLocationTask(plugin, 
                new Location(level.getWorld(), region.x * 16, 64, region.z * 16),
                () -> {
                    // 预加载区块
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            level.getChunk(region.x, region.z);
                        }
                    }
                    
                    // 预初始化缓存
                    initializeRegionCaches(level, region);
                    
                    prewarmingRegions.remove(region);
                });
        }
    }
}
```

## 五、配置系统改进

### 5.1 区域感知配置

```yaml
# 新增：Canvas风格的区域配置
region-optimization:
  enabled: true
  
  # 区域边界检测
  boundary-detection:
    enabled: true
    merge-radius: 2  # Canvas的合并半径
    
  # 跨区域操作策略
  cross-region-strategy:
    tnt-explosion: "ASYNC_COLLECT_REGION_APPLY"  # Canvas风格
    entity-tracking: "REGION_BATCH"
    light-propagation: "REGION_ISOLATED"
    
  # 区域性能监控
  performance-monitoring:
    enabled: true
    report-interval: 300  # 秒
    top-regions: 10
    
  # 动态负载均衡
  load-balancing:
    enabled: true
    adjust-tick-interval: true
    light-load-threshold: 20.0  # ms
    heavy-load-threshold: 40.0  # ms
```

## 六、实施优先级

### 高优先级 (立即实施)
1. **区域边界检测**: 避免跨区域竞争
2. **TNT爆炸改进**: Canvas风格的分阶段处理
3. **实体追踪优化**: 区域内批量+区域间隔离

### 中优先级 (近期实施)
4. **AI优化调整**: 利用区域并行提升节流强度
5. **光照引擎改进**: 跨区域光照传播处理
6. **区域性能监控**: 帮助诊断问题

### 低优先级 (长期规划)
7. **智能负载均衡**: 动态调整优化参数
8. **区域预热机制**: 提升玩家体验
9. **高级配置系统**: 更细粒度的控制

## 七、预期效果

### 7.1 性能提升
- **Folia环境**: 充分利用区域并行，避免跨区域冲突
- **Paper环境**: 保持现有优化效果
- **混合场景**: 智能切换优化策略

### 7.2 稳定性提升
- **消除竞态条件**: 区域边界检测避免冲突
- **线程安全**: Canvas风格的异步+区域调度
- **兼容性**: 与Folia原生优化协同工作

### 7.3 可维护性提升
- **清晰的架构**: 区域感知的设计模式
- **详细的监控**: 区域级别的性能分析
- **灵活的配置**: 适应不同服务器需求

## 八、总结

Canvas的核心优势在于：
1. **区域独立性**: 真正的并行处理，互不干扰
2. **智能调度**: EDF-like算法保证公平性
3. **异步优先**: 能异步的都异步，只在必要时同步
4. **区域感知**: 所有优化都考虑区域边界

AkiAsync应该：
1. **增强区域检测**: 避免跨区域操作的竞争
2. **分阶段处理**: 异步收集+区域调度应用
3. **利用并行性**: 在区域内仍可以优化
4. **监控和调优**: 区域级别的性能分析

通过借鉴Canvas的设计思想，AkiAsync可以在Folia环境下发挥更大的优势，同时保持Paper环境的兼容性。
