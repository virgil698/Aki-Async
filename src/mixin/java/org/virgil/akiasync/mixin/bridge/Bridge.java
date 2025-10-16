package org.virgil.akiasync.mixin.bridge;

import java.util.concurrent.ExecutorService;

/**
 * Bridge interface for communication between plugin and mixins.
 * This interface is in the mixin source set and defines the contract.
 * The plugin provides an implementation that mixins can access.
 * 
 * Following Leaves plugin template pattern:
 * - Bridge.java (mixin) = Interface definition
 * - BridgeManager.java (mixin) = Bridge instance manager
 * - AkiAsyncBridge.java (main) = Implementation provided by plugin
 * 
 * @author Virgil
 */
public interface Bridge {
    
    // ========== Entity Tick Parallel ==========
    
    /**
     * Check if entity tick parallelization is enabled
     * @return true if enabled
     */
    boolean isEntityTickParallel();
    
    /**
     * Get the number of threads for entity tick parallelization
     * @return thread count
     */
    int getEntityTickThreads();
    
    /**
     * Get the minimum entities required for parallel processing
     * @return minimum entity count
     */
    int getMinEntitiesForParallel();
    
    /**
     * Get the entity tick batch size (entities per task)
     * @return batch size
     */
    int getEntityTickBatchSize();
    
    // ========== Brain Throttle ==========
    
    /**
     * Check if brain throttling is enabled
     * @return true if enabled
     */
    boolean isBrainThrottleEnabled();
    
    /**
     * Get the brain throttle interval
     * @return interval in ticks
     */
    int getBrainThrottleInterval();
    
    // ========== Async AI (Zero Latency) - Per-Entity Optimization ==========
    
    /**
     * Get the async AI timeout in microseconds
     * @return timeout in microseconds (default: 500μs = 0.5ms)
     */
    long getAsyncAITimeoutMicros();
    
    /**
     * Check if villager optimization is enabled
     * @return true if enabled
     */
    boolean isVillagerOptimizationEnabled();
    
    /**
     * Check if villager uses POI snapshot
     * @return true if POI snapshot is used
     */
    boolean isVillagerUsePOISnapshot();
    
    /**
     * Check if piglin optimization is enabled
     * @return true if enabled
     */
    boolean isPiglinOptimizationEnabled();
    
    /**
     * Check if piglin uses POI snapshot
     * @return true if POI snapshot is used
     */
    boolean isPiglinUsePOISnapshot();
    
    /**
     * Get piglin look distance threshold
     * @return distance in blocks
     */
    int getPiglinLookDistance();
    
    /**
     * Get piglin barter distance threshold
     * @return distance in blocks
     */
    int getPiglinBarterDistance();
    
    /**
     * Check if pillager family optimization is enabled
     * @return true if enabled
     */
    boolean isPillagerFamilyOptimizationEnabled();
    
    /**
     * Check if pillager family uses POI snapshot
     * @return true if POI snapshot is used
     */
    boolean isPillagerFamilyUsePOISnapshot();
    
    boolean isEvokerOptimizationEnabled();
    boolean isBlazeOptimizationEnabled();
    boolean isGuardianOptimizationEnabled();
    boolean isWitchOptimizationEnabled();
    boolean isUniversalAiOptimizationEnabled();
    java.util.Set<String> getUniversalAiEntities();
    boolean isZeroDelayFactoryOptimizationEnabled();
    java.util.Set<String> getZeroDelayFactoryEntities();
    boolean isItemEntityOptimizationEnabled();
    int getItemEntityAgeInterval();
    int getItemEntityMinNearbyItems();
    
    /**
     * Check if simple entities optimization is enabled
     * @return true if enabled
     */
    boolean isSimpleEntitiesOptimizationEnabled();
    
    /**
     * Check if simple entities use POI snapshot
     * @return true if POI snapshot is used
     */
    boolean isSimpleEntitiesUsePOISnapshot();
    
    // ========== Mob Spawning ==========
    
    /**
     * Check if mob spawning optimization is enabled
     * @return true if enabled
     */
    boolean isMobSpawningEnabled();
    
    /**
     * Get the maximum entities per chunk limit
     * @return max entities
     */
    int getMaxEntitiesPerChunk();
    
    /**
     * Check if spawner optimization is enabled
     * @return true if enabled
     */
    boolean isSpawnerOptimizationEnabled();
    
    // ========== Entity Tracker ==========
    
    /**
     * Check if entity tracker optimization is enabled
     * @return true if enabled
     */
    boolean isEntityTrackerEnabled();
    
    // ========== Pathfinding ==========
    
    /**
     * Get the pathfinding tick budget (0 = unlimited)
     * @return tick budget
     */
    int getPathfindingTickBudget();
    
    // ========== Memory Optimizations ==========
    
    /**
     * Check if predicate cache is enabled
     * @return true if enabled
     */
    boolean isPredicateCacheEnabled();
    
    /**
     * Check if BlockPos pooling is enabled
     * @return true if enabled
     */
    boolean isBlockPosPoolEnabled();
    
    /**
     * Check if list preallocation is enabled
     * @return true if enabled
     */
    boolean isListPreallocEnabled();
    
    /**
     * Get the list preallocation capacity
     * @return capacity
     */
    int getListPreallocCapacity();
    
    // ========== ServerCore Optimizations ==========
    
    /**
     * Check if push optimization is enabled
     * @return true if enabled
     */
    boolean isPushOptimizationEnabled();
    
    /**
     * Check if entity lookup cache is enabled
     * @return true if enabled
     */
    boolean isEntityLookupCacheEnabled();
    
    /**
     * Get the entity lookup cache duration in milliseconds
     * @return duration in ms
     */
    int getEntityLookupCacheDurationMs();
    
    /**
     * Check if collision optimization is enabled
     * @return true if enabled
     */
    boolean isCollisionOptimizationEnabled();
    
    // ========== Executors ==========
    
    /**
     * Get the general executor service
     * @return executor service
     */
    ExecutorService getGeneralExecutor();
    
    // ========== Lighting Optimizations (ScalableLux/Starlight) ==========
    
    /**
     * Check if async lighting is enabled
     * @return true if enabled
     */
    boolean isAsyncLightingEnabled();
    
    /**
     * Get the dedicated lighting executor service
     * @return lighting executor service (may be null if using general executor)
     */
    ExecutorService getLightingExecutor();
    
    /**
     * Get the light batch threshold
     * Lighting updates are batched before processing
     * @return batch threshold (number of updates to accumulate)
     */
    int getLightBatchThreshold();
    
    /**
     * Check if layered propagation queue is enabled (Starlight optimization)
     * @return true if enabled
     */
    boolean useLayeredPropagationQueue();
    
    /**
     * Get the maximum light propagation distance
     * Limits how far light can propagate in one batch
     * @return max propagation distance in blocks (default: 15)
     */
    int getMaxLightPropagationDistance();
    
    /**
     * Check if skylight cache is enabled
     * @return true if enabled
     */
    boolean isSkylightCacheEnabled();
    
    /**
     * Get the skylight cache duration in milliseconds
     * @return cache duration in ms
     */
    int getSkylightCacheDurationMs();
    
    /**
     * Check if light deduplication is enabled
     * @return true if enabled
     */
    boolean isLightDeduplicationEnabled();
    
    /**
     * Check if dynamic batch adjustment is enabled
     * @return true if enabled
     */
    boolean isDynamicBatchAdjustmentEnabled();
    
    /**
     * Check if advanced lighting stats logging is enabled
     * @return true if enabled
     */
    boolean isAdvancedLightingStatsEnabled();
    
    // ========== VMP (Very Many Players) Optimizations ==========
    
    /**
     * Check if player chunk loading optimization is enabled
     * @return true if enabled
     */
    boolean isPlayerChunkLoadingOptimizationEnabled();
    
    /**
     * Get the max concurrent chunk loads per player
     * @return max concurrent loads
     */
    int getMaxConcurrentChunkLoadsPerPlayer();
    
    /**
     * Check if entity tracking range optimization is enabled
     * @return true if enabled
     */
    boolean isEntityTrackingRangeOptimizationEnabled();
    
    /**
     * Get the entity tracking range multiplier
     * Applied to vanilla tracking ranges (0.5-1.0 = reduce range, >1.0 = increase)
     * @return tracking range multiplier
     */
    double getEntityTrackingRangeMultiplier();
    
    // ========== Redstone Optimizations (Alternate Current + Carpet Turbo) ==========
    
    /**
     * Check if Alternate Current optimization is enabled
     * @return true if enabled
     */
    boolean isAlternateCurrentEnabled();
    
    /**
     * Check if redstone wire turbo is enabled
     * @return true if enabled
     */
    boolean isRedstoneWireTurboEnabled();
    
    /**
     * Check if redstone update batching is enabled
     * @return true if enabled
     */
    boolean isRedstoneUpdateBatchingEnabled();
    
    /**
     * Get the redstone update batch threshold
     * @return batch threshold
     */
    int getRedstoneUpdateBatchThreshold();
    
    /**
     * Check if redstone cache is enabled
     * @return true if enabled
     */
    boolean isRedstoneCacheEnabled();
    
    /**
     * Get the redstone cache duration in milliseconds
     * @return cache duration in ms
     */
    int getRedstoneCacheDurationMs();
}

