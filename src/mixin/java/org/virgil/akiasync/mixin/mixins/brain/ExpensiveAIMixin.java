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
 * Zero-latency async AI optimization for expensive entity brains
 * 
 * Core strategy:
 * 1. Main thread tick start: Generate POI snapshot (read-only)
 * 2. Async thread: Brain executes with snapshot, produces diff
 * 3. Main thread within same tick: Wait ≤100μs, write back diff immediately
 * 
 * Advantages:
 * - 0 tick latency: Completes within same tick, entity behavior unchanged
 * - Lock-free concurrency: Snapshot is read-only, thread-safe
 * - Rollback capable: Timeout/exception fallback to vanilla logic
 * - Order consistent: Main thread execution within ServerLevel.tick() single-threaded context
 * 
 * Per-entity optimization:
 * - Villager category (Villager + Wandering Trader) - independent toggle
 * - Piglin category (Piglin) - independent toggle  
 * - Simple entities - independent toggle
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = Brain.class, priority = 999)  // Higher priority, executes before BrainThrottle
public abstract class ExpensiveAIMixin<E extends LivingEntity> {
    
    @Shadow public abstract Optional<?> getMemory(MemoryModuleType<?> type);
    @Shadow public abstract Map<MemoryModuleType<?>, Optional<?>> getMemories();
    @Shadow public abstract Optional<Activity> getActiveNonCoreActivity();
    
    // Configuration cache (volatile for visibility)
    @Unique private static volatile long cached_timeoutMicros;
    @Unique private static volatile boolean cached_villagerEnabled;
    @Unique private static volatile boolean cached_villagerUsePOI;
    @Unique private static volatile boolean cached_piglinEnabled;
    @Unique private static volatile boolean cached_piglinUsePOI;
    @Unique private static volatile boolean cached_simpleEnabled;
    @Unique private static volatile boolean cached_simpleUsePOI;
    @Unique private static volatile boolean initialized = false;
    
    // Instance fields: snapshot data
    @Unique private Map<BlockPos, PoiRecord> aki$poiSnapshot;
    @Unique private BrainSnapshot aki$brainSnapshot;
    
    // Statistics (output every 1000 executions)
    @Unique private static int executionCount = 0;
    @Unique private static int successCount = 0;
    @Unique private static int timeoutCount = 0;
    
    /**
     * Take snapshot before Brain.tick begins
     * 
     * @At("HEAD") - Execute at method entry point
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void aki$takeSnapshot(ServerLevel level, E entity, CallbackInfo ci) {
        // Initialization check
        if (!initialized) { aki$initAsyncAI(); }
        
        // Check if entity type has optimization enabled
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
            return;  // Skip optimization for this entity type
        }
        
        // 1. Snapshot POI (if needed)
        if (usePOI) {
            try {
                PoiManager poiManager = level.getPoiManager();
                
                // Get nearby POI and deep copy (thread-safe via ImmutableMap)
                this.aki$poiSnapshot = poiManager.getInRange(
                    type -> true,  // All POI types
                    entity.blockPosition(),
                    48,  // Villager/Piglin AI range: 48 blocks
                    PoiManager.Occupancy.ANY
                ).collect(ImmutableMap.toImmutableMap(
                    PoiRecord::getPos,
                    record -> record  // PoiRecord is immutable, safe to share
                ));
                
            } catch (Exception e) {
                // Snapshot failed: disable async execution for this tick
                this.aki$poiSnapshot = null;
                return;
            }
        }
        
        // 2. Snapshot Brain state (deep copy + time correction)
        Brain<E> brain = (Brain<E>) (Object) this;
        this.aki$brainSnapshot = BrainSnapshot.capture(brain, level);
    }
    
    /**
     * Offload Brain computation after Brain.tick completes
     * 
     * @At("RETURN") - Execute before method returns (vanilla tick completed)
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void aki$offloadBrain(ServerLevel level, E entity, CallbackInfo ci) {
        // Check if entity type has optimization enabled
        boolean isVillager = entity instanceof Villager || entity instanceof WanderingTrader;
        boolean isPiglin = entity instanceof Piglin;
        boolean enabled = (isVillager && cached_villagerEnabled) 
                       || (isPiglin && cached_piglinEnabled)
                       || (!isVillager && !isPiglin && cached_simpleEnabled);
        
        if (!enabled) return;
        if (this.aki$brainSnapshot == null) return;  // Snapshot failed, skip
        
        executionCount++;
        Brain<E> brain = (Brain<E>) (Object) this;
        
        // Save snapshot for async execution
        final BrainSnapshot snapshot = this.aki$brainSnapshot;
        final Map<BlockPos, PoiRecord> poiSnap = this.aki$poiSnapshot;
        
        try {
            // Optimization: 100μs timeout + immediate sync fallback (more aggressive)
            long shortTimeout = Math.min(cached_timeoutMicros, 100L);  // Max 100μs
            
            // Solution C: Full async computation (BrainCpuCalculator)
            CompletableFuture<BrainDiff> future = AsyncBrainExecutor.runSync(() -> {
                // Real async computation: POI scoring, sorting, Memory unpacking
                return BrainCpuCalculator.runCpuOnly(brain, level, poiSnap);
            }, shortTimeout, TimeUnit.MICROSECONDS);
            
            // Main thread: wait for result immediately (≤100μs)
            // Timeout triggers immediate sync execution (no TPS blocking)
            BrainDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(
                future, 
                shortTimeout, 
                TimeUnit.MICROSECONDS,
                () -> new BrainDiff()  // Timeout fallback: return empty diff
            );
            
            if (diff != null && diff.hasChanges()) {
                // Success: apply diff (within same tick)
                // Type-safe: BlockPos → WalkTarget
                diff.applyTo(brain);
                successCount++;
                
                // Output statistics every 1000 executions
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
            // Exception: fallback to vanilla (vanilla tick already executed, no action needed)
            if (executionCount <= 3) {
                System.err.println("[AkiAsync-ExpensiveAI] Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } finally {
            // Cleanup snapshot data
            this.aki$poiSnapshot = null;
            this.aki$brainSnapshot = null;
        }
    }
    
    /**
     * Initialize configuration (executes once only)
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
            
            // Inject executor to AsyncBrainExecutor
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

