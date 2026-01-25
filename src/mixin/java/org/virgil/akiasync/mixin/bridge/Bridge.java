package org.virgil.akiasync.mixin.bridge;

import java.util.concurrent.ExecutorService;

public interface Bridge {

    boolean isNitoriOptimizationsEnabled();

    boolean isVirtualThreadEnabled();

    boolean isWorkStealingEnabled();

    boolean isBlockPosCacheEnabled();

    boolean isOptimizedCollectionsEnabled();

    boolean isMobSunBurnOptimizationEnabled();

    boolean isEntitySpeedOptimizationEnabled();

    boolean isEntityFallDamageOptimizationEnabled();

    boolean isEntitySectionStorageOptimizationEnabled();

    boolean isChunkPosOptimizationEnabled();

    boolean isNoiseOptimizationEnabled();

    boolean isNbtOptimizationEnabled();

    boolean isBitSetPoolingEnabled();

    boolean isCompletableFutureOptimizationEnabled();

    boolean isChunkOptimizationEnabled();

    boolean isEntityTickParallel();

    int getMinEntitiesForParallel();

    int getEntityTickBatchSize();

    boolean isBrainThrottleEnabled();

    int getBrainThrottleInterval();

    boolean isLivingEntityTravelOptimizationEnabled();

    int getLivingEntityTravelSkipInterval();

    boolean isMobDespawnOptimizationEnabled();

    int getMobDespawnCheckInterval();

    boolean isAiSensorOptimizationEnabled();
    int getAiSensorRefreshInterval();

    boolean isGameEventOptimizationEnabled();
    boolean isGameEventEarlyFilter();
    boolean isGameEventThrottleLowPriority();
    long getGameEventThrottleIntervalMs();
    boolean isGameEventDistanceFilter();
    double getGameEventMaxDetectionDistance();

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

    boolean isAsyncPathfindingCacheEnabled();
    int getAsyncPathfindingCacheMaxSize();
    int getAsyncPathfindingCacheExpireSeconds();
    int getAsyncPathfindingCacheReuseTolerance();
    int getAsyncPathfindingCacheCleanupIntervalSeconds();

    boolean shouldThrottleEntity(Object entity);

    boolean isEntityThrottlingEnabled();

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

    boolean isMobSpawningEnabled();

    boolean isDensityControlEnabled();

    int getMaxEntitiesPerChunk();

    int getMobSpawnInterval();

    boolean isSpawnerOptimizationEnabled();

    boolean isMultithreadedTrackerEnabled();
    int getMultithreadedTrackerParallelism();
    int getMultithreadedTrackerBatchSize();
    int getMultithreadedTrackerAssistBatchSize();

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

    int getLightingParallelism();

    boolean useLayeredPropagationQueue();

    int getMaxLightPropagationDistance();

    boolean isSkylightCacheEnabled();

    int getSkylightCacheDurationMs();

    boolean isLightDeduplicationEnabled();

    boolean isDynamicBatchAdjustmentEnabled();

    boolean isAdvancedLightingStatsEnabled();

    boolean isLightingDebugEnabled();

    boolean isSpawnChunkRemovalEnabled();

    boolean isPlayerChunkLoadingOptimizationEnabled();

    int getMaxConcurrentChunkLoadsPerPlayer();

    boolean isEntityTrackingRangeOptimizationEnabled();

    double getEntityTrackingRangeMultiplier();

    boolean isTNTOptimizationEnabled();

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

    boolean isLeaderZombieHealthFixEnabled();

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

    boolean isExperienceOrbMergeEnabled();
    int getExperienceOrbMergeInterval();

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

    boolean isProjectileOptimizationEnabled();
    int getMaxProjectileLoadsPerTick();
    int getMaxProjectileLoadsPerProjectile();

    void runOnMainThread(Runnable task);

    double getCurrentTPS();
    double getCurrentMSPT();

    Object getBlockTickSmoothingScheduler();
    Object getEntityTickSmoothingScheduler();
    Object getBlockEntitySmoothingScheduler();

    boolean submitSmoothTask(Object scheduler, Runnable task, int priority, String category);

    int submitSmoothTaskBatch(Object scheduler, java.util.List<Runnable> tasks, int priority, String category);

    boolean isTNTUseSakuraDensityCache();
    boolean isTNTUseVectorizedAABB();
    boolean isTNTUseUnifiedEngine();
    boolean isTNTMergeEnabled();
    double getTNTMergeRadius();
    int getTNTMaxFuseDifference();
    float getTNTMergedPowerMultiplier();
    float getTNTMaxPower();
    boolean isTNTCacheEnabled();
    int getTNTCacheExpiryTicks();

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

    void clearSakuraOptimizationCaches();
    java.util.Map<String, Object> getSakuraCacheStatistics();

    void clearEntityThrottleCache(int entityId);
    void performSakuraCacheCleanup();

    void notifySmoothSchedulerTick(Object scheduler);
    void updateSmoothSchedulerMetrics(Object scheduler, double tps, double mspt);

    boolean isCollisionOptimizationEnabled();
    boolean isCollisionAggressiveMode();
    java.util.Set<String> getCollisionExcludedEntities();

    boolean isNativeCollisionsEnabled();
    boolean isNativeCollisionsFallbackEnabled();

    boolean isCollisionBlockCacheEnabled();
    int getCollisionBlockCacheSize();
    int getCollisionBlockCacheExpireTicks();

    boolean isRayCollisionEnabled();
    double getRayCollisionMaxDistance();

    boolean isShapeOptimizationEnabled();
    boolean isShapePrecomputeArrays();
    boolean isShapeBlockShapeCache();
    int getShapeBlockShapeCacheSize();

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

    boolean isJigsawOptimizationEnabled();

    void initializeJigsawOctree(net.minecraft.world.phys.AABB bounds);
    boolean hasJigsawOctree();
    void insertIntoJigsawOctree(net.minecraft.world.phys.AABB box);
    boolean jigsawOctreeIntersects(net.minecraft.world.phys.AABB box);
    void clearJigsawOctree();
    String getJigsawOctreeStats();

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

    boolean isPlayerJoinWarmupEnabled();
    long getPlayerJoinWarmupDurationMs();
    double getPlayerJoinWarmupInitialRate();

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

    boolean isEndermanBlockCarryLimiterEnabled();
    int getEndermanMaxCarrying();
    boolean isEndermanCountTowardsMobCap();
    boolean isEndermanPreventPickup();

    boolean isMultithreadedEntityTrackerEnabled();

    boolean isVelocityCompressionEnabled();

    boolean isAdvancedNetworkOptimizationEnabled();
    boolean isFastVarIntEnabled();
    boolean isEventLoopAffinityEnabled();
    boolean isByteBufOptimizerEnabled();
    boolean isStrictEventLoopChecking();
    boolean isPooledByteBufAllocator();
    boolean isDirectByteBufPreferred();

    boolean isSkipZeroMovementPacketsEnabled();
    boolean isSkipZeroMovementPacketsStrictMode();

    int getHighLatencyThreshold();
    int getHighLatencyMinViewDistance();
    long getHighLatencyDurationMs();

    boolean isAfkPacketThrottleEnabled();
    long getAfkDurationMs();
    double getAfkParticleMaxDistance();
    double getAfkSoundMaxDistance();

    int getMidTickChunkTasksIntervalMs();

    boolean isMultiNettyEventLoopEnabled();
    boolean isPalettedContainerLockRemovalEnabled();
    boolean isSpawnDensityArrayEnabled();
    boolean isTypeFilterableListOptimizationEnabled();
    boolean isEntityTrackerLinkedHashMapEnabled();
    boolean isBiomeAccessOptimizationEnabled();
    boolean isEntityMoveZeroVelocityOptimizationEnabled();
    boolean isEntityTrackerDistanceCacheEnabled();

    void handleConnectionProtocolChange(Object connection, int protocolOrdinal);

    boolean isDynamicChunkSendRateEnabled();
    long getDynamicChunkLimitBandwidth();
    long getDynamicChunkGuaranteedBandwidth();

    boolean isPacketCompressionOptimizationEnabled();
    boolean isAdaptiveCompressionThresholdEnabled();
    boolean isSkipSmallPacketsEnabled();
    int getSkipSmallPacketsThreshold();

    boolean isChunkBatchOptimizationEnabled();
    float getChunkBatchMinChunks();
    float getChunkBatchMaxChunks();
    int getChunkBatchMaxUnacked();

    boolean isPacketPriorityQueueEnabled();
    boolean isPrioritizePlayerPacketsEnabled();
    boolean isPrioritizeChunkPacketsEnabled();
    boolean isDeprioritizeParticlesEnabled();
    boolean isDeprioritizeSoundsEnabled();

    long getNetworkTrafficInRate();
    long getNetworkTrafficOutRate();
    long getNetworkTrafficTotalIn();
    long getNetworkTrafficTotalOut();
    void calculateNetworkTrafficRates();

    void setPacketStatisticsEnabled(boolean enabled);
    boolean isPacketStatisticsEnabled();
    void resetPacketStatistics();
    long getPacketStatisticsElapsedSeconds();
    java.util.List<Object[]> getTopOutgoingPackets(int limit);
    java.util.List<Object[]> getTopIncomingPackets(int limit);
    long getTotalOutgoingPacketCount();
    long getTotalIncomingPacketCount();

    int getGeneralThreadPoolSize();

    java.util.List<net.minecraft.core.BlockPos> fireEntityExplodeEvent(
        net.minecraft.server.level.ServerLevel level,
        net.minecraft.world.entity.Entity entity,
        net.minecraft.world.phys.Vec3 center,
        java.util.List<net.minecraft.core.BlockPos> blocks,
        float yield
    );

    boolean isMtuAwareBatchingEnabled();
    int getMtuLimit();
    int getMtuHardCapPackets();

    boolean isFlushConsolidationEnabled();
    int getFlushConsolidationExplicitFlushAfterFlushes();
    boolean isFlushConsolidationConsolidateWhenNoReadInProgress();

    boolean isNativeCompressionEnabled();
    int getNativeCompressionLevel();

    boolean isNativeEncryptionEnabled();

    boolean isExplosionBlockUpdateOptimizationEnabled();
    int getExplosionBlockChangeThreshold();

    long getConnectionPendingBytes(Object connection);
    boolean addFlushConsolidationHandler(Object channel, int explicitFlushAfterFlushes, boolean consolidateWhenNoReadInProgress);

    void sendPacketWithoutFlush(Object connection, Object packet);
    void flushConnection(Object connection);

    Object getConnectionFromListener(Object listener);
}
