package org.virgil.akiasync.bridge;

import java.util.concurrent.ExecutorService;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.ConfigManager;

/**
 * Bridge implementation for AkiAsync plugin.
 * This class implements the Bridge interface and provides access to plugin configuration.
 * 
 * Following Leaves plugin template pattern:
 * - This implementation is in the main source set
 * - It implements the Bridge interface from the mixin source set
 * - The plugin registers this implementation with BridgeManager
 * 
 * @author Virgil
 */
public class AkiAsyncBridge implements org.virgil.akiasync.mixin.bridge.Bridge {
    
    private final ConfigManager config;
    private final ExecutorService generalExecutor;
    private final ExecutorService lightingExecutor;
    
    public AkiAsyncBridge(AkiAsyncPlugin plugin, ExecutorService generalExecutor, ExecutorService lightingExecutor) {
        this.config = plugin.getConfigManager();
        this.generalExecutor = generalExecutor;
        this.lightingExecutor = lightingExecutor;
    }
    
    // ========== Entity Tick Parallel ==========
    
    @Override
    public boolean isEntityTickParallel() {
        return config.isEntityTickParallel();
    }
    
    @Override
    public int getEntityTickThreads() {
        return config.getEntityTickThreads();
    }
    
    @Override
    public int getMinEntitiesForParallel() {
        return config.getMinEntitiesForParallel();
    }
    
    @Override
    public int getEntityTickBatchSize() {
        return config.getEntityTickBatchSize();
    }
    
    // ========== Brain Throttle ==========
    
    @Override
    public boolean isBrainThrottleEnabled() {
        return config.isBrainThrottleEnabled();
    }
    
    @Override
    public int getBrainThrottleInterval() {
        return config.getBrainThrottleInterval();
    }
    
    // ========== Async AI (Zero Latency) - Per-Entity Optimization ==========
    
    @Override
    public long getAsyncAITimeoutMicros() {
        return config.getAsyncAITimeoutMicros();
    }
    
    @Override
    public boolean isVillagerOptimizationEnabled() {
        return config.isVillagerOptimizationEnabled();
    }
    
    @Override
    public boolean isVillagerUsePOISnapshot() {
        return config.isVillagerUsePOISnapshot();
    }
    
    @Override
    public boolean isPiglinOptimizationEnabled() {
        return config.isPiglinOptimizationEnabled();
    }
    
    @Override
    public boolean isPiglinUsePOISnapshot() {
        return config.isPiglinUsePOISnapshot();
    }
    
    @Override
    public int getPiglinLookDistance() {
        return config.getPiglinLookDistance();
    }
    
    @Override
    public int getPiglinBarterDistance() {
        return config.getPiglinBarterDistance();
    }
    
    @Override
    public boolean isPillagerFamilyOptimizationEnabled() {
        return config.isPillagerFamilyOptimizationEnabled();
    }
    
    @Override
    public boolean isPillagerFamilyUsePOISnapshot() {
        return config.isPillagerFamilyUsePOISnapshot();
    }
    
    @Override
    public boolean isEvokerOptimizationEnabled() { return config.isEvokerOptimizationEnabled(); }
    
    @Override
    public boolean isBlazeOptimizationEnabled() { return config.isBlazeOptimizationEnabled(); }
    
    @Override
    public boolean isGuardianOptimizationEnabled() { return config.isGuardianOptimizationEnabled(); }
    
    @Override
    public boolean isWitchOptimizationEnabled() { return config.isWitchOptimizationEnabled(); }
    
    @Override
    public boolean isUniversalAiOptimizationEnabled() { return config.isUniversalAiOptimizationEnabled(); }
    
    @Override
    public java.util.Set<String> getUniversalAiEntities() { return config.getUniversalAiEntities(); }
    
    @Override
    public boolean isZeroDelayFactoryOptimizationEnabled() { return config.isZeroDelayFactoryOptimizationEnabled(); }
    
    @Override
    public java.util.Set<String> getZeroDelayFactoryEntities() { return config.getZeroDelayFactoryEntities(); }
    
    @Override
    public boolean isSimpleEntitiesOptimizationEnabled() {
        return config.isSimpleEntitiesOptimizationEnabled();
    }
    
    @Override
    public boolean isSimpleEntitiesUsePOISnapshot() {
        return config.isSimpleEntitiesUsePOISnapshot();
    }
    
    // ========== Mob Spawning ==========
    
    @Override
    public boolean isMobSpawningEnabled() {
        return config.isMobSpawningEnabled();
    }
    
    @Override
    public int getMaxEntitiesPerChunk() {
        return config.getMaxEntitiesPerChunk();
    }
    
    @Override
    public boolean isSpawnerOptimizationEnabled() {
        return config.isSpawnerOptimizationEnabled();
    }
    
    // ========== Entity Tracker ==========
    
    @Override
    public boolean isEntityTrackerEnabled() {
        return config.isEntityTrackerEnabled();
    }
    
    // ========== Pathfinding ==========
    
    @Override
    public int getPathfindingTickBudget() {
        return config.getPathfindingTickBudget();
    }
    
    // ========== Memory Optimizations ==========
    
    @Override
    public boolean isPredicateCacheEnabled() {
        // Always enabled by default
        return true;
    }
    
    @Override
    public boolean isBlockPosPoolEnabled() {
        // Always enabled by default
        return true;
    }
    
    @Override
    public boolean isListPreallocEnabled() {
        // Always enabled by default
        return true;
    }
    
    @Override
    public int getListPreallocCapacity() {
        // Default capacity
        return 32;
    }
    
    // ========== ServerCore Optimizations ==========
    
    @Override
    public boolean isPushOptimizationEnabled() {
        // Always enabled by default
        return true;
    }
    
    @Override
    public boolean isEntityLookupCacheEnabled() {
        // Always enabled by default
        return true;
    }
    
    @Override
    public int getEntityLookupCacheDurationMs() {
        // Default duration
        return 50;
    }
    
    @Override
    public boolean isCollisionOptimizationEnabled() {
        // Always enabled by default
        return true;
    }
    
    // ========== Executors ==========
    
    @Override
    public ExecutorService getGeneralExecutor() {
        return generalExecutor;
    }
    
    // ========== Lighting Optimizations (ScalableLux/Starlight) ==========
    
    @Override
    public boolean isAsyncLightingEnabled() {
        return config.isAsyncLightingEnabled();
    }
    
    @Override
    public ExecutorService getLightingExecutor() {
        return lightingExecutor != null ? lightingExecutor : generalExecutor;
    }
    
    @Override
    public int getLightBatchThreshold() {
        return config.getLightBatchThreshold();
    }
    
    @Override
    public boolean useLayeredPropagationQueue() {
        return config.useLayeredPropagationQueue();
    }
    
    @Override
    public int getMaxLightPropagationDistance() {
        return config.getMaxLightPropagationDistance();
    }
    
    @Override
    public boolean isSkylightCacheEnabled() {
        return config.isSkylightCacheEnabled();
    }
    
    @Override
    public int getSkylightCacheDurationMs() {
        return config.getSkylightCacheDurationMs();
    }
    
    @Override
    public boolean isLightDeduplicationEnabled() {
        return config.isLightDeduplicationEnabled();
    }
    
    @Override
    public boolean isDynamicBatchAdjustmentEnabled() {
        return config.isDynamicBatchAdjustmentEnabled();
    }
    
    @Override
    public boolean isAdvancedLightingStatsEnabled() {
        return config.isAdvancedLightingStatsEnabled();
    }
    
    // ========== VMP (Very Many Players) Optimizations ==========
    
    @Override
    public boolean isPlayerChunkLoadingOptimizationEnabled() {
        return config.isPlayerChunkLoadingOptimizationEnabled();
    }
    
    @Override
    public int getMaxConcurrentChunkLoadsPerPlayer() {
        return config.getMaxConcurrentChunkLoadsPerPlayer();
    }
    
    @Override
    public boolean isEntityTrackingRangeOptimizationEnabled() {
        return config.isEntityTrackingRangeOptimizationEnabled();
    }
    
    @Override
    public double getEntityTrackingRangeMultiplier() {
        return config.getEntityTrackingRangeMultiplier();
    }
    
    // ========== Redstone Optimizations (Alternate Current + Carpet Turbo) ==========
    
    @Override
    public boolean isAlternateCurrentEnabled() {
        return config.isAlternateCurrentEnabled();
    }
    
    @Override
    public boolean isRedstoneWireTurboEnabled() {
        return config.isRedstoneWireTurboEnabled();
    }
    
    @Override
    public boolean isRedstoneUpdateBatchingEnabled() {
        return config.isRedstoneUpdateBatchingEnabled();
    }
    
    @Override
    public int getRedstoneUpdateBatchThreshold() {
        return config.getRedstoneUpdateBatchThreshold();
    }
    
    @Override
    public boolean isRedstoneCacheEnabled() {
        return config.isRedstoneCacheEnabled();
    }
    
    @Override
    public int getRedstoneCacheDurationMs() {
        return config.getRedstoneCacheDurationMs();
    }
}

