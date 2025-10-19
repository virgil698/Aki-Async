package org.virgil.akiasync.mixin.bridge;

import java.util.concurrent.ExecutorService;

public interface Bridge {
    
    boolean isEntityTickParallel();
    
    int getEntityTickThreads();
    
    int getMinEntitiesForParallel();
    
    int getEntityTickBatchSize();
    
    boolean isBrainThrottleEnabled();
    
    int getBrainThrottleInterval();
    
    long getAsyncAITimeoutMicros();
    
    boolean isVillagerOptimizationEnabled();
    
    boolean isVillagerUsePOISnapshot();
    
    boolean isPiglinOptimizationEnabled();
    
    boolean isPiglinUsePOISnapshot();
    
    int getPiglinLookDistance();
    
    int getPiglinBarterDistance();
    
    boolean isPillagerFamilyOptimizationEnabled();
    
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
    
    boolean isSimpleEntitiesOptimizationEnabled();
    
    boolean isSimpleEntitiesUsePOISnapshot();
    
    boolean isMobSpawningEnabled();
    
    int getMaxEntitiesPerChunk();
    
    boolean isSpawnerOptimizationEnabled();
    
    boolean isEntityTrackerEnabled();
    
    int getPathfindingTickBudget();
    
    boolean isPredicateCacheEnabled();
    
    boolean isBlockPosPoolEnabled();
    
    boolean isListPreallocEnabled();
    
    int getListPreallocCapacity();
    
    boolean isPushOptimizationEnabled();
    
    boolean isEntityLookupCacheEnabled();
    
    int getEntityLookupCacheDurationMs();
    
    boolean isCollisionOptimizationEnabled();
    
    ExecutorService getGeneralExecutor();
    
    boolean isAsyncLightingEnabled();
    
    ExecutorService getLightingExecutor();
    
    int getLightBatchThreshold();
    
    boolean useLayeredPropagationQueue();
    
    int getMaxLightPropagationDistance();
    
    boolean isSkylightCacheEnabled();
    
    int getSkylightCacheDurationMs();
    
    boolean isLightDeduplicationEnabled();
    
    boolean isDynamicBatchAdjustmentEnabled();
    
    boolean isAdvancedLightingStatsEnabled();
    
    boolean isPlayerChunkLoadingOptimizationEnabled();
    
    int getMaxConcurrentChunkLoadsPerPlayer();
    
    boolean isEntityTrackingRangeOptimizationEnabled();
    
    double getEntityTrackingRangeMultiplier();
    
    boolean isAlternateCurrentEnabled();
    
    boolean isRedstoneWireTurboEnabled();
    
    boolean isRedstoneUpdateBatchingEnabled();
    
    int getRedstoneUpdateBatchThreshold();
    
    boolean isRedstoneCacheEnabled();
    
    int getRedstoneCacheDurationMs();
    
    boolean isTNTOptimizationEnabled();
    
    java.util.Set<String> getTNTExplosionEntities();
    
    int getTNTThreads();
    
    int getTNTMaxBlocks();
    
    long getTNTTimeoutMicros();
    
    int getTNTBatchSize();
    
    boolean isTNTDebugEnabled();
    
    boolean isAsyncHopperChainEnabled();
    
    boolean isHopperNBTCacheEnabled();
    
    int getHopperChainThreads();
    
    boolean isAsyncVillagerBreedEnabled();
    
    boolean isVillagerAgeThrottleEnabled();
    
    int getVillagerBreedThreads();
    
    int getVillagerBreedCheckInterval();
}

