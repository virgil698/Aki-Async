package org.virgil.akiasync.bridge;

import java.util.concurrent.ExecutorService;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.ConfigManager;

public class AkiAsyncBridge implements org.virgil.akiasync.mixin.bridge.Bridge {
    
    private ConfigManager config;
    private final ExecutorService generalExecutor;
    private final ExecutorService lightingExecutor;
    
    public AkiAsyncBridge(AkiAsyncPlugin plugin, ExecutorService generalExecutor, ExecutorService lightingExecutor) {
        this.config = plugin.getConfigManager();
        this.generalExecutor = generalExecutor;
        this.lightingExecutor = lightingExecutor;
    }
    
    @Override
    public boolean isEntityTickParallel() { return config.isEntityTickParallel(); }
    
    @Override
    public int getEntityTickThreads() { return config.getEntityTickThreads(); }
    
    @Override
    public int getMinEntitiesForParallel() {return config.getMinEntitiesForParallel();}
    
    @Override
    public int getEntityTickBatchSize() {return config.getEntityTickBatchSize();}
    
    @Override
    public boolean isBrainThrottleEnabled() {return config.isBrainThrottleEnabled();}
    
    @Override
    public int getBrainThrottleInterval() {return config.getBrainThrottleInterval();}
    
    @Override
    public long getAsyncAITimeoutMicros() {return config.getAsyncAITimeoutMicros();}
    
    @Override
    public boolean isVillagerOptimizationEnabled() {return config.isVillagerOptimizationEnabled();}
    
    @Override
    public boolean isVillagerUsePOISnapshot() {return config.isVillagerUsePOISnapshot();}
    
    @Override
    public boolean isPiglinOptimizationEnabled() {return config.isPiglinOptimizationEnabled();}
    
    @Override
    public boolean isPiglinUsePOISnapshot() { return config.isPiglinUsePOISnapshot(); }
    
    @Override
    public int getPiglinLookDistance() { return config.getPiglinLookDistance(); }
    
    @Override
    public int getPiglinBarterDistance() { return config.getPiglinBarterDistance(); }
    
    @Override
    public boolean isPillagerFamilyOptimizationEnabled() { return config.isPillagerFamilyOptimizationEnabled(); }
    
    @Override
    public boolean isPillagerFamilyUsePOISnapshot() { return config.isPillagerFamilyUsePOISnapshot(); }
    
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
    public boolean isItemEntityOptimizationEnabled() { return config.isItemEntityOptimizationEnabled(); }
    
    @Override
    public int getItemEntityAgeInterval() { return config.getItemEntityAgeInterval(); }
    
    @Override
    public int getItemEntityMinNearbyItems() { return config.getItemEntityMinNearbyItems(); }
    
    @Override
    public boolean isSimpleEntitiesOptimizationEnabled() {return config.isSimpleEntitiesOptimizationEnabled();}
    
    @Override
    public boolean isSimpleEntitiesUsePOISnapshot() {return config.isSimpleEntitiesUsePOISnapshot();}
    
    @Override
    public boolean isMobSpawningEnabled() {return config.isMobSpawningEnabled();}
    
    @Override
    public int getMaxEntitiesPerChunk() {return config.getMaxEntitiesPerChunk();}
    
    @Override
    public boolean isSpawnerOptimizationEnabled() {return config.isSpawnerOptimizationEnabled();}
    
    @Override
    public boolean isEntityTrackerEnabled() {return config.isEntityTrackerEnabled();}
    
    @Override
    public int getPathfindingTickBudget() {return config.getPathfindingTickBudget();}
    
    @Override
    public boolean isPredicateCacheEnabled() {return true;}
    
    @Override
    public boolean isBlockPosPoolEnabled() {return true;}
    
    @Override
    public boolean isListPreallocEnabled() {return true;}
    
    @Override
    public int getListPreallocCapacity() {return 32;}
    
    @Override
    public boolean isPushOptimizationEnabled() {return true;}
    
    @Override
    public boolean isEntityLookupCacheEnabled() {return true;}
    
    @Override
    public int getEntityLookupCacheDurationMs() {return 50;}
    
    @Override
    public boolean isCollisionOptimizationEnabled() {return true;}
    
    @Override
    public ExecutorService getGeneralExecutor() { return generalExecutor; }
    
    @Override
    public boolean isAsyncLightingEnabled() {return config.isAsyncLightingEnabled();}
    
    @Override
    public ExecutorService getLightingExecutor() {return lightingExecutor != null ? lightingExecutor : generalExecutor;}
    
    @Override
    public int getLightBatchThreshold() {return config.getLightBatchThreshold();}
    
    @Override
    public boolean useLayeredPropagationQueue() {return config.useLayeredPropagationQueue();}
    
    @Override
    public int getMaxLightPropagationDistance() {return config.getMaxLightPropagationDistance();}
    
    @Override
    public boolean isSkylightCacheEnabled() {return config.isSkylightCacheEnabled();}
    
    @Override
    public int getSkylightCacheDurationMs() {return config.getSkylightCacheDurationMs();}
    
    @Override
    public boolean isLightDeduplicationEnabled() {return config.isLightDeduplicationEnabled();}
    
    @Override
    public boolean isDynamicBatchAdjustmentEnabled() {return config.isDynamicBatchAdjustmentEnabled();}
    
    @Override
    public boolean isAdvancedLightingStatsEnabled() {return config.isAdvancedLightingStatsEnabled();}
    
    @Override
    public boolean isPlayerChunkLoadingOptimizationEnabled() {return config.isPlayerChunkLoadingOptimizationEnabled();}
    
    @Override
    public int getMaxConcurrentChunkLoadsPerPlayer() {return config.getMaxConcurrentChunkLoadsPerPlayer();}
    
    @Override
    public boolean isEntityTrackingRangeOptimizationEnabled() {return config.isEntityTrackingRangeOptimizationEnabled();}
    
    @Override
    public double getEntityTrackingRangeMultiplier() {return config.getEntityTrackingRangeMultiplier();}
    
    @Override
    public boolean isAlternateCurrentEnabled() {return config.isAlternateCurrentEnabled();}
    
    @Override
    public boolean isRedstoneWireTurboEnabled() {return config.isRedstoneWireTurboEnabled();}
    
    @Override
    public boolean isRedstoneUpdateBatchingEnabled() {return config.isRedstoneUpdateBatchingEnabled();}
    
    @Override
    public int getRedstoneUpdateBatchThreshold() {return config.getRedstoneUpdateBatchThreshold();}
    
    @Override
    public boolean isRedstoneCacheEnabled() {return config.isRedstoneCacheEnabled();}
    
    @Override
    public int getRedstoneCacheDurationMs() {return config.getRedstoneCacheDurationMs();}
    
    @Override
    public boolean isTNTOptimizationEnabled() {return config.isTNTOptimizationEnabled();}
    
    @Override
    public java.util.Set<String> getTNTExplosionEntities() {return config.getTNTExplosionEntities();}
    
    @Override
    public int getTNTThreads() {return config.getTNTThreads();}
    
    @Override
    public int getTNTMaxBlocks() {return config.getTNTMaxBlocks();}
    
    @Override
    public long getTNTTimeoutMicros() {return config.getTNTTimeoutMicros();}
    
    @Override
    public int getTNTBatchSize() {return config.getTNTBatchSize();}
    
    @Override
    public boolean isTNTDebugEnabled() {return config.isTNTDebugEnabled();}
    
    @Override
    public boolean isDebugLoggingEnabled() {return config.isDebugLoggingEnabled();}
    
    public void updateConfiguration(ConfigManager newConfig) {
        this.config = newConfig;
    }
    
    @Override
    public boolean isAsyncVillagerBreedEnabled() {return config.isAsyncVillagerBreedEnabled();}
    
    @Override
    public boolean isVillagerAgeThrottleEnabled() {return config.isVillagerAgeThrottleEnabled();}
    
    @Override
    public int getVillagerBreedThreads() {return config.getVillagerBreedThreads();}
    
    @Override
    public int getVillagerBreedCheckInterval() {return config.getVillagerBreedCheckInterval();}
    
    @Override
    public boolean isChunkTickAsyncEnabled() {return config.isChunkTickAsyncEnabled();}
    
    @Override
    public int getChunkTickThreads() {return config.getChunkTickThreads();}
    
    @Override
    public long getChunkTickTimeoutMicros() {return config.getChunkTickTimeoutMicros();}
}