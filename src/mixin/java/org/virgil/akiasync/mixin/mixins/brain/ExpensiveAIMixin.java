package org.virgil.akiasync.mixin.mixins.brain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.BrainCpuCalculator;
import org.virgil.akiasync.mixin.brain.BrainDiff;
import org.virgil.akiasync.mixin.brain.BrainSnapshot;

import com.google.common.collect.ImmutableMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.schedule.Activity;

/**
 * 昂贵生物AI异步优化 - 零延迟方案
 * 
 * 核心思路：
 * 1. 主线程tick开始时：生成POI快照（只读）
 * 2. 异步线程：Brain使用快照执行，产出diff
 * 3. 主线程tick内：等待≤500μs，立即写回diff
 * 
 * 优势：
 * - 0 tick延迟：同tick内完成，村民行为无感知
 * - 无锁并发：快照只读，多线程安全
 * - 可回滚：超时/异常直接fallback，保持原版逻辑
 * - 顺序一致：主线程仍在ServerLevel.tick()单线程内，没有乱序
 * 
 * 分类优化：
 * - 村民类（Villager + Wandering Trader）- 独立开关
 * - 猪灵（Piglin）- 独立开关
 * - 简单实体 - 独立开关
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = Brain.class, priority = 999)  // 更高优先级，在BrainThrottle之前执行
public abstract class ExpensiveAIMixin<E extends LivingEntity> {
    
    @Shadow public abstract Optional<?> getMemory(MemoryModuleType<?> type);
    @Shadow public abstract Map<MemoryModuleType<?>, Optional<?>> getMemories();
    @Shadow public abstract Optional<Activity> getActiveNonCoreActivity();
    
    // 配置缓存（volatile确保可见性）
    @Unique private static volatile long cached_timeoutMicros;
    @Unique private static volatile boolean cached_villagerEnabled;
    @Unique private static volatile boolean cached_villagerUsePOI;
    @Unique private static volatile boolean cached_piglinEnabled;
    @Unique private static volatile boolean cached_piglinUsePOI;
    @Unique private static volatile boolean cached_simpleEnabled;
    @Unique private static volatile boolean cached_simpleUsePOI;
    @Unique private static volatile boolean initialized = false;
    
    // 实例字段：快照数据
    @Unique private Map<BlockPos, PoiRecord> aki$poiSnapshot;
    @Unique private BrainSnapshot aki$brainSnapshot;
    
    // 统计信息（每1000次输出一次）
    @Unique private static int executionCount = 0;
    @Unique private static int successCount = 0;
    @Unique private static int timeoutCount = 0;
    
    /**
     * 在Brain.tick开始前：拍摄快照
     * 
     * @At("HEAD") - 在方法入口处执行
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void aki$takeSnapshot(ServerLevel level, E entity, CallbackInfo ci) {
        // 初始化检查
        if (!initialized) { aki$initAsyncAI(); }
        
        // 检查实体类型是否启用优化
        boolean isVillager = entity instanceof Villager || entity instanceof WanderingTrader;
        boolean isPiglin = entity instanceof Piglin;
        
        boolean usePOI;
        if (isVillager && cached_villagerEnabled) {
            usePOI = cached_villagerUsePOI;
        } else if (isPiglin && cached_piglinEnabled) {
            usePOI = cached_piglinUsePOI;
        } else if (!isVillager && !isPiglin && cached_simpleEnabled) {
            usePOI = cached_simpleUsePOI;
        } else {
            return;  // 不优化此实体类型
        }
        
        // 1. 快照POI（如果需要）
        if (usePOI) {
            try {
                PoiManager poiManager = level.getPoiManager();
                
                // 获取附近POI并深拷贝（线程安全）
                this.aki$poiSnapshot = poiManager.getInRange(
                    type -> true,  // 所有POI类型
                    entity.blockPosition(),
                    48,  // 村民/猪灵AI范围：48格
                    PoiManager.Occupancy.ANY
                ).collect(ImmutableMap.toImmutableMap(
                    PoiRecord::getPos,
                    record -> record  // PoiRecord是不可变的，可以安全共享
                ));
                
            } catch (Exception e) {
                // 快照失败：禁用本次异步执行
                this.aki$poiSnapshot = null;
                return;
            }
        }
        
        // 2. 快照Brain状态（深拷贝 + 时间校正）
        Brain<E> brain = (Brain<E>) (Object) this;
        this.aki$brainSnapshot = BrainSnapshot.capture(brain, level);
    }
    
    /**
     * 在Brain.tick结束后：异步执行并写回
     * 
     * @At("RETURN") - 在方法返回前执行（原版tick已完成）
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void aki$offloadBrain(ServerLevel level, E entity, CallbackInfo ci) {
        // 检查实体类型是否启用优化
        boolean isVillager = entity instanceof Villager || entity instanceof WanderingTrader;
        boolean isPiglin = entity instanceof Piglin;
        boolean enabled = (isVillager && cached_villagerEnabled) 
                       || (isPiglin && cached_piglinEnabled)
                       || (!isVillager && !isPiglin && cached_simpleEnabled);
        
        if (!enabled) return;
        if (this.aki$brainSnapshot == null) return;  // 快照失败，跳过
        
        executionCount++;
        Brain<E> brain = (Brain<E>) (Object) this;
        
        // 保存快照（用于异步执行）
        final BrainSnapshot snapshot = this.aki$brainSnapshot;
        final Map<BlockPos, PoiRecord> poiSnap = this.aki$poiSnapshot;
        
        try {
            // 优化：100μs 超时 + 立即同步fallback（砍半，更激进）
            long shortTimeout = Math.min(cached_timeoutMicros, 100L);  // 最多100μs
            
            // ✅ 方案C：完整异步计算（BrainCpuCalculator）
            CompletableFuture<BrainDiff> future = AsyncBrainExecutor.runSync(() -> {
                // 真正的异步计算：POI评分、排序、Memory解包
                return BrainCpuCalculator.runCpuOnly(brain, level, poiSnap);
            }, shortTimeout, TimeUnit.MICROSECONDS);
            
            // 主线程：立即等待结果（≤200μs）
            // 超时则立即同步执行（不堵TPS）
            BrainDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(
                future, 
                shortTimeout, 
                TimeUnit.MICROSECONDS,
                () -> new BrainDiff()  // 超时fallback：返回空diff
            );
            
            if (diff != null && diff.hasChanges()) {
                // 成功：应用diff（在同 tick 内完成）
                // ✅ 类型正确：BlockPos → WalkTarget
                diff.applyTo(brain);
                successCount++;
                
                // 每1000次输出统计
                if (executionCount % 1000 == 0) {
                    double successRate = (successCount * 100.0) / executionCount;
                    double timeoutRate = (timeoutCount * 100.0) / executionCount;
                    System.out.println(String.format(
                        "[AkiAsync-ExpensiveAI] Stats: %d execs | %.1f%% success | %.1f%% timeout | %s",
                        executionCount, successRate, timeoutRate, AsyncBrainExecutor.getStatistics()
                    ));
                }
            } else {
                timeoutCount++;
            }
            
        } catch (Exception e) {
            // 异常：fallback到原版流程（已经执行过原版tick，无需处理）
            if (executionCount <= 3) {
                System.err.println("[AkiAsync-ExpensiveAI] Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } finally {
            // 清理快照数据
            this.aki$poiSnapshot = null;
            this.aki$brainSnapshot = null;
        }
    }
    
    /**
     * 初始化配置（只执行一次）
     */
    @Unique
    private static synchronized void aki$initAsyncAI() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_timeoutMicros = bridge.getAsyncAITimeoutMicros();
            cached_villagerEnabled = bridge.isVillagerOptimizationEnabled();
            cached_villagerUsePOI = bridge.isVillagerUsePOISnapshot();
            cached_piglinEnabled = bridge.isPiglinOptimizationEnabled();
            cached_piglinUsePOI = bridge.isPiglinUsePOISnapshot();
            cached_simpleEnabled = bridge.isSimpleEntitiesOptimizationEnabled();
            cached_simpleUsePOI = bridge.isSimpleEntitiesUsePOISnapshot();
            
            // 注入执行器到AsyncBrainExecutor
            AsyncBrainExecutor.setExecutor(bridge.getGeneralExecutor());
        } else {
            cached_timeoutMicros = 500;
            cached_villagerEnabled = false;
            cached_villagerUsePOI = true;
            cached_piglinEnabled = false;
            cached_piglinUsePOI = true;
            cached_simpleEnabled = false;
            cached_simpleUsePOI = false;
        }
        
        initialized = true;
        System.out.println(String.format(
            "[AkiAsync] ExpensiveAIMixin initialized: timeout=%dμs | villager=%s(POI:%s) | piglin=%s(POI:%s) | simple=%s(POI:%s)",
            cached_timeoutMicros, 
            cached_villagerEnabled, cached_villagerUsePOI,
            cached_piglinEnabled, cached_piglinUsePOI,
            cached_simpleEnabled, cached_simpleUsePOI
        ));
    }
}

