package org.virgil.akiasync.mixin.bridge;

import java.util.concurrent.ExecutorService;

public interface Bridge {
    
    boolean isNitoriOptimizationsEnabled();
    
    boolean isVirtualThreadEnabled();
    
    boolean isWorkStealingEnabled();
    
    boolean isBlockPosCacheEnabled();
    
    boolean isOptimizedCollectionsEnabled();
    
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
    boolean isBlockEntityParallelTickEnabled();
    int getBlockEntityParallelMinBlockEntities();
    int getBlockEntityParallelBatchSize();
    boolean isBlockEntityParallelProtectContainers();
    int getBlockEntityParallelTimeoutMs();
    boolean isItemEntityOptimizationEnabled();
    int getItemEntityAgeInterval();
    int getItemEntityMinNearbyItems();
    
    boolean isSimpleEntitiesOptimizationEnabled();
    
    boolean isSimpleEntitiesUsePOISnapshot();
    
    boolean isMobSpawningEnabled();
    
    int getMaxEntitiesPerChunk();
    
    boolean isSpawnerOptimizationEnabled();
    
    boolean isEntityTrackerEnabled();
    
    int getEntityTrackerQueueSize();
    
    boolean isPredicateCacheEnabled();
    
    boolean isBlockPosPoolEnabled();
    
    boolean isListPreallocEnabled();
    
    int getListPreallocCapacity();
    
    boolean isPushOptimizationEnabled();
    
    boolean isEntityLookupCacheEnabled();
    
    int getEntityLookupCacheDurationMs();
    
    boolean isCollisionOptimizationEnabled();
    
    ExecutorService getGeneralExecutor();
    
    ExecutorService getTNTExecutor();
    
    ExecutorService getChunkTickExecutor();
    
    ExecutorService getVillagerBreedExecutor();
    
    ExecutorService getBrainExecutor();
    
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
    
    boolean isTNTVanillaCompatibilityEnabled();
    
    boolean isTNTUseVanillaPower();
    
    boolean isTNTUseVanillaFireLogic();
    
    boolean isTNTUseVanillaDamageCalculation();
    
    boolean isBeeFixEnabled();

    boolean isTNTUseFullRaycast();
    
    boolean isTNTUseVanillaBlockDestruction();
    
    boolean isTNTUseVanillaDrops();
    
    boolean isFoliaEnvironment();
    
    boolean isOwnedByCurrentRegion(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos);
    void scheduleRegionTask(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, Runnable task);
    
    boolean canAccessEntityDirectly(net.minecraft.world.entity.Entity entity);
    boolean canAccessBlockPosDirectly(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos);

    void safeExecute(Runnable task, String context);
    
    String checkExecutorHealth(java.util.concurrent.ExecutorService executor, String name);
    
    String getBlockId(net.minecraft.world.level.block.Block block);
    
    boolean isAsyncVillagerBreedEnabled();
    
    boolean isVillagerAgeThrottleEnabled();
    
    int getVillagerBreedThreads();
    
    int getVillagerBreedCheckInterval();
    
    boolean isChunkTickAsyncEnabled();
    
    int getChunkTickThreads();
    
    long getChunkTickTimeoutMicros();
    
    boolean isStructureLocationAsyncEnabled();
    
    int getStructureLocationThreads();
    
    boolean isLocateCommandEnabled();
    
    int getLocateCommandSearchRadius();
    
    boolean isLocateCommandSkipKnownStructures();
    
    boolean isVillagerTradeMapsEnabled();
    
    java.util.Set<String> getVillagerTradeMapTypes();
    
    int getVillagerMapGenerationTimeoutSeconds();
    
    boolean isDolphinTreasureHuntEnabled();
    
    int getDolphinTreasureSearchRadius();
    
    int getDolphinTreasureHuntInterval();
    
    boolean isChestExplorationMapsEnabled();
    
    java.util.Set<String> getChestExplorationLootTables();
    
    boolean isChestMapPreserveProbability();
    
    boolean isStructureLocationDebugEnabled();
    
    boolean isStructureAlgorithmOptimizationEnabled();
    
    String getStructureSearchPattern();
    
    boolean isStructureCachingEnabled();
    
    boolean isStructurePrecomputationEnabled();
    
    boolean isBiomeAwareSearchEnabled();
    
    int getStructureCacheMaxSize();
    
    long getStructureCacheExpirationMinutes();
    
    void handleLocateCommandResult(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.core.BlockPos structurePos, Throwable throwable);
    
    void handleLocateCommandAsyncStart(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.commands.arguments.ResourceOrTagKeyArgument.Result<net.minecraft.world.level.levelgen.structure.Structure> structureResult, net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> holderSet);
    
    void handleDolphinTreasureResult(net.minecraft.world.entity.animal.Dolphin dolphin, net.minecraft.core.BlockPos treasurePos, Throwable throwable);
    
    void handleChestExplorationMapResult(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, Throwable throwable, Object cir);
    
    void handleVillagerTradeMapResult(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Throwable throwable, Object cir);
    
    int getVillagerTradeMapsSearchRadius();
    
    boolean isVillagerTradeMapsSkipKnownStructures();
    
    boolean isDolphinTreasureSkipKnownStructures();
    
    boolean isDataPackOptimizationEnabled();
    
    int getDataPackFileLoadThreads();
    
    int getDataPackZipProcessThreads();
    
    int getDataPackBatchSize();
    
    long getDataPackCacheExpirationMinutes();
    
    boolean isDataPackDebugEnabled();
    
    boolean isDebugLoggingEnabled();
    
    void debugLog(String message);
    void debugLog(String format, Object... args);
    void errorLog(String message);
    void errorLog(String format, Object... args);
    
    boolean isVirtualEntity(net.minecraft.world.entity.Entity entity);
    
    boolean isSecureSeedEnabled();
    boolean isSecureSeedProtectStructures();
    boolean isSecureSeedProtectOres();
    boolean isSecureSeedProtectSlimes();
    int getSecureSeedBits();
    boolean isSecureSeedDebugLogging();
}