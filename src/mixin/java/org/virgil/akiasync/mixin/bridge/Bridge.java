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

    boolean isVillagerOptimizationEnabled();

    boolean isVillagerUsePOISnapshot();

    boolean isVillagerPoiCacheEnabled();

    int getVillagerPoiCacheExpireTime();

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
    boolean isAsyncPathfindingEnabled();
    int getAsyncPathfindingMaxThreads();
    int getAsyncPathfindingKeepAliveSeconds();
    int getAsyncPathfindingMaxQueueSize();
    int getAsyncPathfindingTimeoutMs();
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

    boolean isItemEntityParallelEnabled();
    int getMinItemEntitiesForParallel();
    int getItemEntityBatchSize();
    boolean isItemEntityMergeOptimizationEnabled();
    int getItemEntityMergeInterval();
    int getItemEntityMinNearbyItems();
    double getItemEntityMergeRange();
    boolean isItemEntityAgeOptimizationEnabled();
    int getItemEntityAgeInterval();
    double getItemEntityPlayerDetectionRange();

    boolean isNetworkOptimizationEnabled();
    boolean isPacketPriorityEnabled();
    boolean isChunkRateControlEnabled();
    boolean isChunkSendOptimizationEnabled();
    boolean isCongestionDetectionEnabled();
    int getHighPingThreshold();
    int getCriticalPingThreshold();
    long getHighBandwidthThreshold();
    int getBaseChunkSendRate();
    int getMaxChunkSendRate();
    int getMinChunkSendRate();

    int getPacketSendRateBase();
    int getPacketSendRateMedium();
    int getPacketSendRateHeavy();
    int getPacketSendRateExtreme();

    int getQueueLimitMaxTotal();
    int getQueueLimitMaxCritical();
    int getQueueLimitMaxHigh();
    int getQueueLimitMaxNormal();

    int getAccelerationThresholdMedium();
    int getAccelerationThresholdHeavy();
    int getAccelerationThresholdExtreme();

    boolean isCleanupEnabled();
    int getCleanupStaleThreshold();
    int getCleanupCriticalCleanup();
    int getCleanupNormalCleanup();

    int getPlayerCongestionLevel(java.util.UUID playerId);

    boolean shouldPacketUseQueue(net.minecraft.network.protocol.Packet<?> packet);
    int classifyPacketPriority(net.minecraft.network.protocol.Packet<?> packet);
    boolean enqueuePacket(net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.Packet<?> packet, int priority);
    int getPlayerPacketQueueSize(java.util.UUID playerId);
    void recordPacketSent(java.util.UUID playerId, int bytes);

    void updatePlayerChunkLocation(net.minecraft.server.level.ServerPlayer player);
    int calculatePlayerChunkSendRate(java.util.UUID playerId);
    double calculateChunkPriority(java.util.UUID playerId, int chunkX, int chunkZ);
    boolean isChunkInPlayerViewDirection(java.util.UUID playerId, int chunkX, int chunkZ);
    void recordPlayerChunkSent(java.util.UUID playerId, boolean inViewDirection);
    int detectPlayerCongestion(java.util.UUID playerId);

    boolean isFastMovementChunkLoadEnabled();
    double getFastMovementSpeedThreshold();
    int getFastMovementPreloadDistance();
    int getFastMovementMaxConcurrentLoads();
    int getFastMovementPredictionTicks();

    boolean isCenterOffsetEnabled();
    double getMinOffsetSpeed();
    double getMaxOffsetSpeed();
    double getMaxOffsetRatio();
    int getAsyncLoadingBatchSize();
    long getAsyncLoadingBatchDelayMs();
    
    void submitChunkLoad(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, int priority, double speed);
    
    boolean isPlayerUsingViaVersion(java.util.UUID playerId);
    boolean isViaConnectionInPlayState(java.util.UUID playerId);
    int getPlayerProtocolVersion(java.util.UUID playerId);

    boolean isTeleportOptimizationEnabled();
    boolean isTeleportPacketBypassEnabled();
    int getTeleportBoostDurationSeconds();
    int getTeleportMaxChunkRate();
    boolean isTeleportFilterNonEssentialPackets();
    boolean isTeleportDebugEnabled();
    
    boolean isTeleportPacket(net.minecraft.network.protocol.Packet<?> packet);
    void markPlayerTeleportStart(java.util.UUID playerId);
    boolean isPlayerTeleporting(java.util.UUID playerId);
    boolean shouldSendPacketDuringTeleport(net.minecraft.network.protocol.Packet<?> packet, java.util.UUID playerId);
    void recordTeleportBypassedPacket();
    String getTeleportStatistics();
    
    boolean shouldVirtualEntityPacketBypassQueue(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.server.level.ServerPlayer player);
    
    boolean isViewFrustumFilterEnabled();
    boolean shouldFilterPacketByViewFrustum(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.server.level.ServerPlayer player);
    
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
    boolean isRedstoneNetworkCacheEnabled();
    int getRedstoneNetworkCacheExpireTicks();
    
    void clearSakuraOptimizationCaches();
    java.util.Map<String, Object> getSakuraCacheStatistics();
    void performSakuraCacheCleanup();
    
    void notifySmoothSchedulerTick(Object scheduler);
    void updateSmoothSchedulerMetrics(Object scheduler, double tps, double mspt);
    
    boolean isCollisionOptimizationEnabled();
    boolean isCollisionAggressiveMode();
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
}
