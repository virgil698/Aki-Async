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

    boolean isLivingEntityTravelOptimizationEnabled();

    int getLivingEntityTravelSkipInterval();

    boolean isBehaviorThrottleEnabled();

    int getBehaviorThrottleInterval();

    boolean isMobDespawnOptimizationEnabled();

    int getMobDespawnCheckInterval();

    long getAsyncAITimeoutMicros();

    boolean isAiSpatialIndexEnabled();
    int getAiSpatialIndexGridSize();
    boolean isAiSpatialIndexAutoUpdate();
    boolean isAiSpatialIndexPlayerIndexEnabled();
    boolean isAiSpatialIndexPoiIndexEnabled();
    boolean isAiSpatialIndexStatisticsEnabled();
    int getAiSpatialIndexLogIntervalSeconds();

    boolean isVillagerOptimizationEnabled();

    boolean isVillagerUsePOISnapshot();

    boolean isVillagerPoiCacheEnabled();

    int getVillagerPoiCacheExpireTime();
    
    boolean isWanderingTraderOptimizationEnabled();
    boolean isWardenOptimizationEnabled();
    boolean isHoglinOptimizationEnabled();
    boolean isAllayOptimizationEnabled();
    boolean isEndermanOptimizationEnabled();
    int getEndermanTickInterval();
    boolean isEndermanAllowPickupBlocks();
    boolean isEndermanAllowPlaceBlocks();
    
    
    boolean isArmadilloOptimizationEnabled();
    int getArmadilloTickInterval();
    
    boolean isSnifferOptimizationEnabled();
    int getSnifferTickInterval();
    
    boolean isCamelOptimizationEnabled();
    int getCamelTickInterval();
    
    boolean isFrogOptimizationEnabled();
    int getFrogTickInterval();
    
    boolean isGoatOptimizationEnabled();
    int getGoatTickInterval();
    
    boolean isPandaOptimizationEnabled();
    int getPandaTickInterval();

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
    boolean isDabEnabled();
    int getDabStartDistance();
    int getDabActivationDistMod();
    int getDabMaxTickInterval();
    
    boolean isBrainMemoryOptimizationEnabled();
    boolean isPoiSnapshotEnabled();
    boolean isAsyncPathfindingEnabled();
    int getAsyncPathfindingMaxThreads();
    int getAsyncPathfindingKeepAliveSeconds();
    int getAsyncPathfindingMaxQueueSize();
    int getAsyncPathfindingTimeoutMs();
    boolean isAsyncPathfindingSyncFallbackEnabled();
    
    boolean isEnhancedPathfindingEnabled();
    int getEnhancedPathfindingMaxConcurrentRequests();
    int getEnhancedPathfindingMaxRequestsPerTick();
    int getEnhancedPathfindingHighPriorityDistance();
    int getEnhancedPathfindingMediumPriorityDistance();
    boolean isPathPrewarmEnabled();
    int getPathPrewarmRadius();
    int getPathPrewarmMaxMobsPerBatch();
    int getPathPrewarmMaxPoisPerMob();
    boolean shouldThrottleEntity(Object entity);
    boolean isZeroDelayFactoryOptimizationEnabled();
    java.util.Set<String> getZeroDelayFactoryEntities();
    boolean isBlockEntityParallelTickEnabled();
    int getBlockEntityParallelMinBlockEntities();
    int getBlockEntityParallelBatchSize();
    boolean isBlockEntityParallelProtectContainers();
    int getBlockEntityParallelTimeoutMs();

    boolean isHopperOptimizationEnabled();

    int getHopperCacheExpireTime();

    boolean isMinecartOptimizationEnabled();

    int getMinecartTickInterval();

    boolean isSimpleEntitiesOptimizationEnabled();

    boolean isSimpleEntitiesUsePOISnapshot();

    boolean isMobSpawningEnabled();

    boolean isDensityControlEnabled();

    int getMaxEntitiesPerChunk();
    
    int getMobSpawnInterval();

    boolean isSpawnerOptimizationEnabled();

    boolean isEntityTrackerEnabled();

    int getEntityTrackerQueueSize();

    boolean isPredicateCacheEnabled();

    boolean isBlockPosPoolEnabled();

    boolean isListPreallocEnabled();

    int getListPreallocCapacity();

    boolean isEntityLookupCacheEnabled();

    int getEntityLookupCacheDurationMs();

    ExecutorService getGeneralExecutor();

    ExecutorService getTNTExecutor();

    ExecutorService getChunkTickExecutor();

    ExecutorService getVillagerBreedExecutor();

    ExecutorService getBrainExecutor();
    
    ExecutorService getCollisionExecutor();

    boolean isAsyncLightingEnabled();

    ExecutorService getLightingExecutor();

    int getLightBatchThreshold();
    
    int getLightUpdateIntervalMs();

    boolean useLayeredPropagationQueue();

    int getMaxLightPropagationDistance();

    boolean isSkylightCacheEnabled();

    int getSkylightCacheDurationMs();

    boolean isLightDeduplicationEnabled();

    boolean isDynamicBatchAdjustmentEnabled();

    boolean isAdvancedLightingStatsEnabled();
    
    boolean isLightingDebugEnabled();

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

    boolean isEndIslandDensityFixEnabled();
    
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
    
    int getChunkTickAsyncBatchSize();

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

    boolean isChestExplorationMapsEnabled();

    java.util.Set<String> getChestExplorationLootTables();

    boolean isStructureLocationDebugEnabled();

    boolean isStructureAlgorithmOptimizationEnabled();

    String getStructureSearchPattern();

    boolean isStructureCachingEnabled();

    boolean isBiomeAwareSearchEnabled();

    int getStructureCacheMaxSize();

    long getStructureCacheExpirationMinutes();

    void handleLocateCommandResult(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.core.BlockPos structurePos, Throwable throwable);

    void handleLocateCommandAsyncStart(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.commands.arguments.ResourceOrTagKeyArgument.Result<net.minecraft.world.level.levelgen.structure.Structure> structureResult, net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> holderSet);

    void handleDolphinTreasureResult(net.minecraft.world.entity.animal.Dolphin dolphin, net.minecraft.core.BlockPos treasurePos, Throwable throwable);

    void handleChestExplorationMapAsyncStart(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> destination, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, int searchRadius, boolean skipKnownStructures, Object cir);

    void handleChestExplorationMapResult(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, Throwable throwable, Object cir);

    void handleVillagerTradeMapAsyncStart(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> destination, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Object cir);

    void handleVillagerTradeMapResult(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Throwable throwable, Object cir);

    int getVillagerTradeMapsSearchRadius();

    boolean isVillagerTradeMapsSkipKnownStructures();

    boolean isDolphinTreasureSkipKnownStructures();

    boolean isDataPackOptimizationEnabled();

    int getDataPackFileLoadThreads();

    int getDataPackZipProcessThreads();

    int getDataPackBatchSize();

    long getDataPackCacheExpirationMinutes();

    int getDataPackMaxFileCacheSize();

    int getDataPackMaxFileSystemCacheSize();

    boolean isDataPackDebugEnabled();

    boolean isDebugLoggingEnabled();
    
    boolean isSmartLagCompensationEnabled();
    double getSmartLagTPSThreshold();
    
    boolean isSmartLagItemPickupDelayEnabled();
    
    boolean isSmartLagPotionEffectsEnabled();
    
    boolean isSmartLagTimeAccelerationEnabled();
    
    boolean isSmartLagDebugEnabled();
    boolean isSmartLagLogMissedTicks();
    boolean isSmartLagLogCompensation();

    boolean isExperienceOrbInactiveTickEnabled();
    double getExperienceOrbInactiveRange();
    int getExperienceOrbInactiveMergeInterval();

    void debugLog(String message);
    void debugLog(String format, Object... args);
    void errorLog(String message);
    void errorLog(String format, Object... args);

    boolean isVirtualEntity(net.minecraft.world.entity.Entity entity);

    boolean isSeedProtectionEnabled();
    boolean shouldReturnFakeSeed();
    long getFakeSeedValue();
    
    boolean isQuantumSeedEnabled();
    byte[] getQuantumServerKey();
    long getEncryptedSeed(long originalSeed, int chunkX, int chunkZ, String dimension, String generationType, long gameTime);
    
    boolean isSecureSeedEnabled();
    long[] getSecureSeedWorldSeed();
    void initializeSecureSeed(long originalSeed);
    int getSecureSeedBits();
    
    boolean isSeedEncryptionProtectStructures();
    boolean isSeedEncryptionProtectOres();
    boolean isSeedEncryptionProtectSlimes();
    boolean isSeedEncryptionProtectBiomes();

    boolean isTNTLandProtectionEnabled();
    boolean canTNTExplodeAt(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos);
    Boolean checkChunkProtection(net.minecraft.server.level.ServerLevel level, int chunkX, int chunkZ);
    
    boolean isBlockLockerProtectionEnabled();
    boolean isBlockLockerProtected(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state);

    boolean isFurnaceRecipeCacheEnabled();
    int getFurnaceRecipeCacheSize();
    boolean isFurnaceCacheApplyToBlastFurnace();
    boolean isFurnaceCacheApplyToSmoker();
    boolean isFurnaceFixBurnTimeBug();

    boolean isCraftingRecipeCacheEnabled();
    int getCraftingRecipeCacheSize();
    boolean isCraftingOptimizeBatchCrafting();
    boolean isCraftingReduceNetworkTraffic();

    boolean isMinecartCauldronDestructionEnabled();

    boolean isFallingBlockParallelEnabled();
    int getMinFallingBlocksForParallel();
    int getFallingBlockBatchSize();

    boolean isItemEntityMergeOptimizationEnabled();
    boolean isItemEntityCancelVanillaMerge();
    int getItemEntityMergeInterval();
    int getItemEntityMinNearbyItems();
    double getItemEntityMergeRange();
    boolean isItemEntityAgeOptimizationEnabled();
    int getItemEntityAgeInterval();
    double getItemEntityPlayerDetectionRange();
    boolean isItemEntityInactiveTickEnabled();
    double getItemEntityInactiveRange();
    int getItemEntityInactiveMergeInterval();

    boolean isSuffocationOptimizationEnabled();
    boolean isFastRayTraceEnabled();
    boolean isMapRenderingOptimizationEnabled();
    int getMapRenderingThreads();
    
    void runOnMainThread(Runnable task);
    
    double getCurrentTPS();
    double getCurrentMSPT();
    
    Object getBlockTickSmoothingScheduler();
    Object getEntityTickSmoothingScheduler();
    Object getBlockEntitySmoothingScheduler();
    
    boolean submitSmoothTask(Object scheduler, Runnable task, int priority, String category);
    
    int submitSmoothTaskBatch(Object scheduler, java.util.List<Runnable> tasks, int priority, String category);
    
    boolean isTNTUseSakuraDensityCache();
    boolean isTNTMergeEnabled();
    double getTNTMergeRadius();
    int getTNTMaxFuseDifference();
    float getTNTMergedPowerMultiplier();
    
    boolean isUsePandaWireAlgorithm();
    
    boolean isPortalSuffocationCheckDisabled();
    boolean isShulkerBulletSelfHitFixEnabled();
    
    boolean isExecuteCommandInactiveSkipEnabled();
    int getExecuteCommandSkipLevel();
    double getExecuteCommandSimulationDistanceMultiplier();
    long getExecuteCommandCacheDurationMs();
    java.util.Set<String> getExecuteCommandWhitelistTypes();
    boolean isExecuteCommandDebugEnabled();
    
    boolean isCommandDeduplicationEnabled();
    boolean isCommandDeduplicationDebugEnabled();
    
    boolean isRedstoneNetworkCacheEnabled();
    int getRedstoneNetworkCacheExpireTicks();
    
    void clearSakuraOptimizationCaches();
    java.util.Map<String, Object> getSakuraCacheStatistics();
    
    void clearEntityThrottleCache(int entityId);
    void performSakuraCacheCleanup();
    
    void notifySmoothSchedulerTick(Object scheduler);
    void updateSmoothSchedulerMetrics(Object scheduler, double tps, double mspt);
    
    boolean isCollisionOptimizationEnabled();
    boolean isCollisionAggressiveMode();
    java.util.Set<String> getCollisionExcludedEntities();
    boolean isCollisionCacheEnabled();
    int getCollisionCacheLifetimeMs();
    double getCollisionCacheMovementThreshold();
    boolean isCollisionSpatialPartitionEnabled();
    int getCollisionSpatialGridSize();
    int getCollisionSpatialDensityThreshold();
    int getCollisionSpatialUpdateIntervalMs();
    double getCollisionSkipMinMovement();
    int getCollisionSkipCheckIntervalMs();
    
    boolean isPushOptimizationEnabled();
    double getPushMaxPushPerTick();
    double getPushDampingFactor();
    int getPushHighDensityThreshold();
    double getPushHighDensityMultiplier();
    
    boolean isAdvancedCollisionOptimizationEnabled();
    int getCollisionThreshold();
    float getSuffocationDamage();
    int getMaxPushIterations();
    
    boolean isLightingPrioritySchedulingEnabled();
    int getLightingHighPriorityRadius();
    int getLightingMediumPriorityRadius();
    int getLightingLowPriorityRadius();
    long getLightingMaxLowPriorityDelay();
    
    boolean isLightingDebouncingEnabled();
    long getLightingDebounceDelay();
    int getLightingMaxUpdatesPerSecond();
    long getLightingResetOnStableMs();
    
    boolean isLightingMergingEnabled();
    int getLightingMergeRadius();
    long getLightingMergeDelay();
    int getLightingMaxMergedUpdates();
    
    boolean isLightingChunkBorderEnabled();
    boolean isLightingBatchBorderUpdates();
    long getLightingBorderUpdateDelay();
    int getLightingCrossChunkBatchSize();
    
    boolean isLightingAdaptiveEnabled();
    int getLightingMonitorInterval();
    boolean isLightingAutoAdjustThreads();
    boolean isLightingAutoAdjustBatchSize();
    int getLightingTargetQueueSize();
    int getLightingTargetLatency();
    
    boolean isLightingChunkUnloadEnabled();
    boolean isLightingAsyncCleanup();
    int getLightingCleanupBatchSize();
    long getLightingCleanupDelay();
    
    String getLightingThreadPoolMode();
    String getLightingThreadPoolCalculation();
    int getLightingMinThreads();
    int getLightingMaxThreads();
    int getLightingBatchThresholdMax();
    boolean isLightingAggressiveBatching();

    boolean isNoiseOptimizationEnabled();
    boolean isJigsawOptimizationEnabled();
    
    void initializeJigsawOctree(net.minecraft.world.phys.AABB bounds);
    boolean hasJigsawOctree();
    void insertIntoJigsawOctree(net.minecraft.world.phys.AABB box);
    boolean jigsawOctreeIntersects(net.minecraft.world.phys.AABB box);
    void clearJigsawOctree();
    String getJigsawOctreeStats();
    
    boolean isEntityPacketThrottleEnabled();
    boolean shouldSendEntityUpdate(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.entity.Entity entity);
    void tickEntityPacketThrottler();
    
    boolean isEntityDataThrottleEnabled();
    boolean shouldSendMetadata(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.entity.Entity entity, int metadataHash);
    boolean shouldSendNBT(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.entity.Entity entity, boolean forceUpdate);
    void tickEntityDataThrottler();
    
    boolean isChunkVisibilityFilterEnabled();
    boolean isChunkVisible(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, net.minecraft.server.level.ServerLevel level);
    void tickChunkVisibilityFilter();
    
    boolean isFastMovementChunkLoadEnabled();
    double getFastMovementSpeedThreshold();
    int getFastMovementPreloadDistance();
    int getFastMovementMaxConcurrentLoads();
    int getFastMovementPredictionTicks();
    boolean isCenterOffsetEnabled();
    double getMinOffsetSpeed();
    double getMaxOffsetSpeed();
    double getMaxOffsetRatio();
    void submitChunkLoad(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, int priority, double speed);
    
    void clearWorldCaches(String worldName);
    void prewarmPlayerPaths(java.util.UUID playerId);
    void cleanupPlayerPaths(java.util.UUID playerId);
    
    
    void restartVillagerExecutor();
    void restartTNTExecutor();
    void restartBrainExecutor();
    void restartChunkExecutor(int threadCount);
    
    
    void clearVillagerBreedCache();
    void resetBrainExecutorStatistics();
    void resetAsyncMetrics();
}
