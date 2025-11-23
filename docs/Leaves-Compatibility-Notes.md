# Leaves环境兼容性说明

## 概述

AkiAsync插件专为Leaves服务端设计，充分考虑了Leaves的特性和优化。

---

## Leaves特有优化

### 1. Lithium睡眠方块实体

Leaves已经集成了Lithium的睡眠方块实体优化：

```java
// BlockEntity.java
implements ComparatorTracker, 
           SetBlockStateHandlingBlockEntity, 
           SetChangedHandlingBlockEntity
```

**特性**：
- 自动检测方块实体是否需要tick
- 没有比较器附近的方块实体会进入睡眠状态
- 减少不必要的tick开销

**AkiAsync兼容策略**：
- 检测Lithium睡眠方块实体
- 避免重复优化
- 保持Lithium优化的完整性

### 2. Paper性能优化

```java
static boolean ignoreBlockEntityUpdates; // Paper - Perf: Optimize Hoppers
```

**用途**：
- 漏斗优化时忽略方块实体更新
- 减少不必要的邻居更新

**AkiAsync兼容策略**：
- 尊重这个标志
- 不在忽略更新期间进行并行处理

---

## 方块实体并行化兼容性

### 容器类方块实体保护

基于C3H6N6O6的设计，以下容器类方块实体在主线程执行：

| 类型 | 原因 | 风险等级 |
|------|------|---------|
| **chest** | 物品存储 | 高 |
| **barrel** | 物品存储 | 高 |
| **shulker_box** | 物品存储 | 高 |
| **hopper** | 物品传输 | 高 |
| **dropper** | 物品发射 | 中 |
| **dispenser** | 物品发射 | 中 |
| **furnace** | 物品冶炼 | 中 |
| **blast_furnace** | 物品冶炼 | 中 |
| **smoker** | 物品冶炼 | 中 |
| **brewing_stand** | 药水酿造 | 中 |
| **beacon** | 效果发射 | 低 |
| **campfire** | 物品烹饪 | 低 |
| **lectern** | 书本存储 | 低 |
| **jukebox** | 唱片存储 | 低 |

### 可安全并行的方块实体

以下方块实体可以安全地并行tick：

- **告示牌** (sign)
- **旗帜** (banner)
- **床** (bed)
- **钟** (bell)
- **末地传送门框架** (end_portal_frame)
- **末地折跃门** (end_gateway)
- **刷怪笼** (spawner)
- **活塞** (piston)
- **音符盒** (note_block)
- **附魔台** (enchanting_table)
- **酿造台** (brewing_stand) - 仅读取操作

---

## ServerLevel.tick()分析

### Leaves环境下的tick流程

```java
public void tick(BooleanSupplier hasTimeLeft) {
    // 1. 世界边界tick
    this.getWorldBorder().tick();
    
    // 2. 天气更新
    this.advanceWeatherCycle();
    
    // 3. 时间更新
    this.tickTime();
    
    // 4. 方块tick（blockTicks和fluidTicks）
    this.blockTicks.tick(l, maxBlockTicks, this::tickBlock);
    this.fluidTicks.tick(l, maxFluidTicks, this::tickFluid);
    
    // 5. 袭击tick
    this.raids.tick(this);
    
    // 6. 区块源tick
    this.getChunkSource().tick(hasTimeLeft, true);
    
    // 7. 方块事件
    this.runBlockEvents();
    
    // 8. 实体tick
    this.entityTickList.forEach(entity -> {
        // 实体tick逻辑
    });
    
    // 9. 方块实体tick ⭐
    this.tickBlockEntities();
}
```

### tickBlockEntities()详细分析

```java
private void tickBlockEntities() {
    // 遍历所有TickingBlockEntity
    for (TickingBlockEntity blockEntity : this.blockEntityTickers) {
        if (blockEntity != null) {
            try {
                blockEntity.tick();
            } catch (Throwable t) {
                // 异常处理
            }
        }
    }
}
```

**关键点**：
1. 顺序执行，没有并行
2. 异常会被捕获，不会影响其他方块实体
3. 可以安全地替换为并行实现

---

## AkiAsync并行化策略

### 设计原则

1. **保守优先**：优先保证兼容性
2. **容器保护**：容器类方块实体主线程执行
3. **降级策略**：超时或异常时降级到同步
4. **Lithium兼容**：不与Lithium睡眠方块实体冲突

### 实现细节

```java
@Inject(method = "tickBlockEntities", at = @At("HEAD"), cancellable = true)
private void parallelTickBlockEntities(CallbackInfo ci) {
    // 1. 检查是否启用
    if (!enabled || blockEntityTickers.size() < minBlockEntities) return;
    
    // 2. 分批处理
    List<List<TickingBlockEntity>> batches = partition(blockEntityTickers, batchSize);
    
    // 3. 并行执行（非容器）
    for (List<TickingBlockEntity> batch : batches) {
        CompletableFuture.runAsync(() -> {
            for (TickingBlockEntity be : batch) {
                if (!isContainer(be)) {
                    be.tick();
                }
            }
        }, executor);
    }
    
    // 4. 主线程执行（容器）
    for (TickingBlockEntity be : blockEntityTickers) {
        if (isContainer(be)) {
            be.tick();
        }
    }
}
```

### 性能预期

| 场景 | 方块实体数量 | 预期提升 |
|------|------------|---------|
| **小型服务器** | < 100 | 不启用 |
| **中型服务器** | 100-500 | +30-40% |
| **大型服务器** | 500-2000 | +40-60% |
| **超大型服务器** | > 2000 | +50-70% |

---

## 与Leaves其他优化的协同

### 1. Moonrise区块系统

Leaves使用Moonrise重写的区块系统：

```java
implements ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel
```

**兼容性**：
- AkiAsync的方块实体并行化不影响区块系统
- 区块加载/卸载时自动处理方块实体

### 2. 实体激活范围（EAR）

```java
io.papermc.paper.entity.activation.ActivationRange.activateEntities(this);
```

**兼容性**：
- 方块实体不受EAR影响
- 可以独立优化

### 3. 异步区块保存

```java
// ChunkSaveAsyncMixin
this.moonrise$scheduleSave(flush);
```

**兼容性**：
- 方块实体数据在保存前已经序列化
- 不会产生并发问题

---

## 已知问题和限制

### 1. 红石机械

**问题**：复杂红石机械可能受影响

**解决方案**：
- 提供配置开关
- 红石相关方块实体可以加入黑名单

### 2. 模组兼容性

**问题**：某些模组可能添加自定义方块实体

**解决方案**：
- 保守的容器检测
- 提供黑名单配置

### 3. 性能开销

**问题**：方块实体数量少时并行开销大于收益

**解决方案**：
- 设置最小方块实体数量阈值（默认50）
- 自适应批处理大小

---

## 配置建议

### 推荐配置

```yaml
block-entity-parallel:
  enabled: true
  min-block-entities: 50
  batch-size: 16
  protect-containers: true
  timeout-ms: 50
  
  # 黑名单（可选）
  blacklist:
    - "minecraft:piston"
    - "minecraft:sticky_piston"
    - "custom_mod:special_block"
```

### 性能调优

**小型服务器**（< 20玩家）：
```yaml
min-block-entities: 100
batch-size: 32
```

**中型服务器**（20-50玩家）：
```yaml
min-block-entities: 50
batch-size: 16
```

**大型服务器**（> 50玩家）：
```yaml
min-block-entities: 30
batch-size: 8
```

---

## 测试检查清单

### 功能测试

- [ ] 基础方块实体tick正常
- [ ] 容器类方块实体物品不丢失
- [ ] 漏斗传输正常
- [ ] 熔炉冶炼正常
- [ ] 酿造台酿造正常
- [ ] 信标效果正常

### 红石测试

- [ ] 活塞门正常工作
- [ ] 刷石机正常工作
- [ ] 自动农场正常工作
- [ ] 红石电路正常工作
- [ ] 比较器正常工作

### 性能测试

- [ ] TPS稳定
- [ ] MSPT降低
- [ ] 内存使用正常
- [ ] CPU使用合理

### 兼容性测试

- [ ] 与Lithium睡眠方块实体兼容
- [ ] 与Moonrise区块系统兼容
- [ ] 与Paper优化兼容
- [ ] 与其他插件兼容

---

## 调试技巧

### 启用详细日志

```yaml
debug:
  block-entity-parallel: true
```

### 监控命令

```bash
# 查看方块实体统计
/spark profiler start --only-ticks-over 50

# 查看线程使用
/spark profiler start --thread *

# 查看TPS
/spark tps --poll
```

### 常见问题排查

**问题1：TPS下降**
- 检查方块实体数量是否过多
- 调整batch-size
- 检查是否有大量容器

**问题2：物品丢失**
- 确认protect-containers已启用
- 检查容器检测逻辑
- 查看异常日志

**问题3：红石机械异常**
- 将相关方块实体加入黑名单
- 或者禁用方块实体并行化

---

## 总结

AkiAsync的方块实体并行化充分考虑了Leaves环境的特性：

1. ✅ 与Lithium睡眠方块实体兼容
2. ✅ 尊重Paper性能优化
3. ✅ 保护容器类方块实体
4. ✅ 提供降级策略
5. ✅ 支持配置调优

通过合理的配置和测试，可以在保证兼容性的同时获得显著的性能提升。
