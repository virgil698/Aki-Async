package org.virgil.akiasync.bridge;

import java.util.concurrent.ExecutorService;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.bridge.delegates.CacheBridgeDelegate;
import org.virgil.akiasync.bridge.delegates.ChunkVisibilityBridgeDelegate;
import org.virgil.akiasync.bridge.delegates.EntityBridgeDelegate;
import org.virgil.akiasync.bridge.delegates.ExecutorBridgeDelegate;
import org.virgil.akiasync.bridge.delegates.ExplosionBridgeDelegate;
import org.virgil.akiasync.bridge.delegates.JigsawBridgeDelegate;
import org.virgil.akiasync.bridge.delegates.NetworkBridgeDelegate;
import org.virgil.akiasync.bridge.delegates.SeedBridgeDelegate;
import org.virgil.akiasync.bridge.delegates.SmoothingSchedulerBridgeDelegate;
import org.virgil.akiasync.bridge.delegates.StructureBridgeDelegate;
import org.virgil.akiasync.bridge.delegates.UtilityBridgeDelegate;
import org.virgil.akiasync.compat.FoliaEntityAdapter;
import org.virgil.akiasync.compat.FoliaSchedulerAdapter;
import org.virgil.akiasync.config.ConfigManager;
import org.virgil.akiasync.executor.TaskSmoothingScheduler;
import org.virgil.akiasync.util.concurrency.ConfigReloadListener;
import org.virgil.akiasync.util.concurrency.ConfigReloader;

public class AkiAsyncBridge implements org.virgil.akiasync.mixin.bridge.Bridge, ConfigReloadListener {

    private final AkiAsyncPlugin plugin;
    private ConfigManager config;
    private final ExecutorService generalExecutor;
    private final ExecutorService lightingExecutor;
    private final ExecutorService tntExecutor;
    private final ExecutorService chunkTickExecutor;
    private final ExecutorService villagerBreedExecutor;
    private final ExecutorService brainExecutor;
    private final ExecutorService collisionExecutor;
    private final ExecutorService worldgenExecutor;

    // Delegates
    private final StructureBridgeDelegate structureDelegate;
    private final SeedBridgeDelegate seedDelegate;
    private final CacheBridgeDelegate cacheDelegate;
    private final ExecutorBridgeDelegate executorDelegate;
    private final NetworkBridgeDelegate networkDelegate;
    private final SmoothingSchedulerBridgeDelegate smoothingSchedulerDelegate;
    private final JigsawBridgeDelegate jigsawDelegate;
    private final ExplosionBridgeDelegate explosionDelegate;
    private final ChunkVisibilityBridgeDelegate chunkVisibilityDelegate;
    private final UtilityBridgeDelegate utilityDelegate;
    private final EntityBridgeDelegate entityDelegate;

    public AkiAsyncBridge(AkiAsyncPlugin plugin, ExecutorService generalExecutor, ExecutorService lightingExecutor, ExecutorService tntExecutor, ExecutorService chunkTickExecutor, ExecutorService villagerBreedExecutor, ExecutorService brainExecutor, ExecutorService collisionExecutor, ExecutorService worldgenExecutor) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.generalExecutor = generalExecutor;
        this.lightingExecutor = lightingExecutor;
        this.tntExecutor = tntExecutor;
        this.chunkTickExecutor = chunkTickExecutor;
        this.villagerBreedExecutor = villagerBreedExecutor;
        this.brainExecutor = brainExecutor;
        this.collisionExecutor = collisionExecutor;
        this.worldgenExecutor = worldgenExecutor;

        // Initialize delegates
        this.structureDelegate = new StructureBridgeDelegate(plugin, config, generalExecutor);
        this.seedDelegate = new SeedBridgeDelegate(plugin, config);
        this.cacheDelegate = new CacheBridgeDelegate(plugin);
        this.executorDelegate = new ExecutorBridgeDelegate(plugin);
        this.networkDelegate = new NetworkBridgeDelegate(plugin, config);
        this.smoothingSchedulerDelegate = new SmoothingSchedulerBridgeDelegate(plugin, config, generalExecutor);
        this.jigsawDelegate = new JigsawBridgeDelegate(config);
        this.explosionDelegate = new ExplosionBridgeDelegate(config);
        this.chunkVisibilityDelegate = new ChunkVisibilityBridgeDelegate(plugin, config);
        this.utilityDelegate = new UtilityBridgeDelegate(plugin, config);
        this.entityDelegate = new EntityBridgeDelegate(plugin);

        ConfigReloader.registerListener(this);
    }

    @Override
    public void onConfigReload(ConfigManager newConfig) {
        this.config = newConfig;
        // Update delegates
        structureDelegate.updateConfig(newConfig);
        seedDelegate.updateConfig(newConfig);
        networkDelegate.updateConfig(newConfig);
        smoothingSchedulerDelegate.updateConfig(newConfig);
        jigsawDelegate.updateConfig(newConfig);
        explosionDelegate.updateConfig(newConfig);
        chunkVisibilityDelegate.updateConfig(newConfig);
        utilityDelegate.updateConfig(newConfig);
        plugin.getLogger().info("[AkiAsync] Bridge configuration updated after reload");
    }

    @Override
    public boolean isNitoriOptimizationsEnabled() {
        return config != null ? config.isNitoriOptimizationsEnabled() : true;
    }

    @Override
    public boolean isVirtualThreadEnabled() {
        return config != null ? config.isVirtualThreadEnabled() : true;
    }

    @Override
    public boolean isWorkStealingEnabled() {
        return config != null ? config.isWorkStealingEnabled() : true;
    }

    @Override
    public boolean isBlockPosCacheEnabled() {
        return config != null ? config.isBlockPosCacheEnabled() : true;
    }

    @Override
    public boolean isOptimizedCollectionsEnabled() {
        return config != null ? config.isOptimizedCollectionsEnabled() : true;
    }

    @Override
    public boolean isMobSunBurnOptimizationEnabled() {
        return config != null ? config.isMobSunBurnOptimizationEnabled() : true;
    }

    @Override
    public boolean isEntitySpeedOptimizationEnabled() {
        return config != null ? config.isEntitySpeedOptimizationEnabled() : true;
    }

    @Override
    public boolean isEntityFallDamageOptimizationEnabled() {
        return config != null ? config.isEntityFallDamageOptimizationEnabled() : true;
    }

    @Override
    public boolean isEntitySectionStorageOptimizationEnabled() {
        return config != null ? config.isEntitySectionStorageOptimizationEnabled() : true;
    }

    @Override
    public boolean isChunkPosOptimizationEnabled() {
        return config != null ? config.isChunkPosOptimizationEnabled() : true;
    }

    @Override
    public boolean isNoiseOptimizationEnabled() {
        return config != null ? config.isNoiseOptimizationEnabled() : true;
    }

    @Override
    public boolean isNbtOptimizationEnabled() {
        return config != null ? config.isNbtOptimizationEnabled() : true;
    }

    @Override
    public boolean isBitSetPoolingEnabled() {
        return config != null ? config.isBitSetPoolingEnabled() : true;
    }

    @Override
    public boolean isCompletableFutureOptimizationEnabled() {
        return config != null ? config.isCompletableFutureOptimizationEnabled() : true;
    }

    @Override
    public boolean isChunkOptimizationEnabled() {
        return config != null ? config.isChunkOptimizationEnabled() : true;
    }

    @Override
    public boolean isEntityTickParallel() { return config.isEntityTickParallel(); }

    @Override
    public int getMinEntitiesForParallel() {return config.getMinEntitiesForParallel();}

    @Override
    public int getEntityTickBatchSize() {return config.getEntityTickBatchSize();}

    @Override
    public boolean isBrainThrottleEnabled() {return config.isBrainThrottleEnabled();}

    @Override
    public int getBrainThrottleInterval() {return config.getBrainThrottleInterval();}

    @Override
    public boolean isLivingEntityTravelOptimizationEnabled() {return config.isLivingEntityTravelOptimizationEnabled();}

    @Override
    public int getLivingEntityTravelSkipInterval() {return config.getLivingEntityTravelSkipInterval();}

    @Override
    public boolean isMobDespawnOptimizationEnabled() {return config.isMobDespawnOptimizationEnabled();}

    @Override
    public int getMobDespawnCheckInterval() {return config.getMobDespawnCheckInterval();}

    @Override
    public boolean isAiSensorOptimizationEnabled() { return config.isAiSensorOptimizationEnabled(); }

    @Override
    public int getAiSensorRefreshInterval() { return config.getAiSensorRefreshInterval(); }

    @Override
    public boolean isGameEventOptimizationEnabled() { return config.isGameEventOptimizationEnabled(); }

    @Override
    public boolean isGameEventEarlyFilter() { return config.isGameEventEarlyFilter(); }

    @Override
    public boolean isGameEventThrottleLowPriority() { return config.isGameEventThrottleLowPriority(); }

    @Override
    public long getGameEventThrottleIntervalMs() { return config.getGameEventThrottleIntervalMs(); }

    @Override
    public boolean isGameEventDistanceFilter() { return config.isGameEventDistanceFilter(); }

    @Override
    public double getGameEventMaxDetectionDistance() { return config.getGameEventMaxDetectionDistance(); }

    @Override
    public boolean isAsyncPathfindingEnabled() { return config.isAsyncPathfindingEnabled(); }

    @Override
    public int getAsyncPathfindingMaxThreads() { return config.getAsyncPathfindingMaxThreads(); }

    @Override
    public int getAsyncPathfindingKeepAliveSeconds() { return config.getAsyncPathfindingKeepAliveSeconds(); }

    @Override
    public int getAsyncPathfindingMaxQueueSize() { return config.getAsyncPathfindingMaxQueueSize(); }

    @Override
    public int getAsyncPathfindingTimeoutMs() { return config.getAsyncPathfindingTimeoutMs(); }

    @Override
    public boolean isAsyncPathfindingSyncFallbackEnabled() {
        return config.isAsyncPathfindingSyncFallbackEnabled();
    }

    @Override
    public boolean isEnhancedPathfindingEnabled() { return config.isEnhancedPathfindingEnabled(); }

    @Override
    public int getEnhancedPathfindingMaxConcurrentRequests() {
        return config.getEnhancedPathfindingMaxConcurrentRequests();
    }

    @Override
    public int getEnhancedPathfindingMaxRequestsPerTick() {
        return config.getEnhancedPathfindingMaxRequestsPerTick();
    }

    @Override
    public int getEnhancedPathfindingHighPriorityDistance() {
        return config.getEnhancedPathfindingHighPriorityDistance();
    }

    @Override
    public int getEnhancedPathfindingMediumPriorityDistance() {
        return config.getEnhancedPathfindingMediumPriorityDistance();
    }

    @Override
    public boolean isPathPrewarmEnabled() { return config.isPathPrewarmEnabled(); }

    @Override
    public int getPathPrewarmRadius() { return config.getPathPrewarmRadius(); }

    @Override
    public int getPathPrewarmMaxMobsPerBatch() { return config.getPathPrewarmMaxMobsPerBatch(); }

    @Override
    public int getPathPrewarmMaxPoisPerMob() { return config.getPathPrewarmMaxPoisPerMob(); }

    @Override
    public boolean isAsyncPathfindingCacheEnabled() { return config.isAsyncPathfindingCacheEnabled(); }

    @Override
    public int getAsyncPathfindingCacheMaxSize() { return config.getAsyncPathfindingCacheMaxSize(); }

    @Override
    public int getAsyncPathfindingCacheExpireSeconds() { return config.getAsyncPathfindingCacheExpireSeconds(); }

    @Override
    public int getAsyncPathfindingCacheReuseTolerance() { return config.getAsyncPathfindingCacheReuseTolerance(); }

    @Override
    public int getAsyncPathfindingCacheCleanupIntervalSeconds() { return config.getAsyncPathfindingCacheCleanupIntervalSeconds(); }

    @Override
    public boolean shouldThrottleEntity(Object entity) { return entityDelegate.shouldThrottleEntity(entity); }

    @Override
    public boolean isEntityThrottlingEnabled() { return config != null ? config.isEntityThrottlingEnabled() : true; }

    @Override
    public boolean isZeroDelayFactoryOptimizationEnabled() { return config.isZeroDelayFactoryOptimizationEnabled(); }

    @Override
    public java.util.Set<String> getZeroDelayFactoryEntities() { return config.getZeroDelayFactoryEntities(); }

    @Override
    public boolean isBlockEntityParallelTickEnabled() { return config.isBlockEntityParallelTickEnabled(); }

    @Override
    public int getBlockEntityParallelMinBlockEntities() { return config.getBlockEntityParallelMinBlockEntities(); }

    @Override
    public int getBlockEntityParallelBatchSize() { return config.getBlockEntityParallelBatchSize(); }

    @Override
    public boolean isBlockEntityParallelProtectContainers() { return config.isBlockEntityParallelProtectContainers(); }

    @Override
    public int getBlockEntityParallelTimeoutMs() { return config.getBlockEntityParallelTimeoutMs(); }

    @Override
    public boolean isHopperOptimizationEnabled() {return config.isHopperOptimizationEnabled();}

    @Override
    public int getHopperCacheExpireTime() {return config.getHopperCacheExpireTime();}

    @Override
    public boolean isMinecartOptimizationEnabled() {return config.isMinecartOptimizationEnabled();}

    @Override
    public int getMinecartTickInterval() {return config.getMinecartTickInterval();}

    @Override
    public boolean isMobSpawningEnabled() {return config.isMobSpawningEnabled();}

    @Override
    public boolean isDensityControlEnabled() {return config.isDensityControlEnabled();}

    @Override
    public int getMaxEntitiesPerChunk() {return config.getMaxEntitiesPerChunk();}

    @Override
    public int getMobSpawnInterval() {return config.getMobSpawnInterval();}

    @Override
    public boolean isSpawnerOptimizationEnabled() {return config.isSpawnerOptimizationEnabled();}

    @Override
    public boolean isMultithreadedTrackerEnabled() {return config.isMultithreadedTrackerEnabled();}

    @Override
    public int getMultithreadedTrackerParallelism() {return config.getMultithreadedTrackerParallelism();}

    @Override
    public int getMultithreadedTrackerBatchSize() {return config.getMultithreadedTrackerBatchSize();}

    @Override
    public int getMultithreadedTrackerAssistBatchSize() {return config.getMultithreadedTrackerAssistBatchSize();}

    @Override
    public boolean isPredicateCacheEnabled() {return true;}

    @Override
    public boolean isBlockPosPoolEnabled() {return true;}

    @Override
    public boolean isListPreallocEnabled() {return true;}

    @Override
    public int getListPreallocCapacity() {return 32;}

    @Override
    public boolean isEntityLookupCacheEnabled() {return true;}

    @Override
    public int getEntityLookupCacheDurationMs() {return 50;}

    @Override
    public ExecutorService getGeneralExecutor() { return generalExecutor; }

    @Override
    public ExecutorService getTNTExecutor() { return tntExecutor != null ? tntExecutor : generalExecutor; }

    @Override
    public ExecutorService getChunkTickExecutor() { return chunkTickExecutor != null ? chunkTickExecutor : generalExecutor; }

    @Override
    public ExecutorService getVillagerBreedExecutor() { return villagerBreedExecutor != null ? villagerBreedExecutor : generalExecutor; }

    @Override
    public ExecutorService getBrainExecutor() { return brainExecutor != null ? brainExecutor : generalExecutor; }

    @Override
    public ExecutorService getCollisionExecutor() { return collisionExecutor != null ? collisionExecutor : generalExecutor; }

    @Override
    public ExecutorService getWorldgenExecutor() { return worldgenExecutor != null ? worldgenExecutor : generalExecutor; }

    @Override
    public boolean isAsyncLightingEnabled() {return config.isAsyncLightingEnabled();}

    @Override
    public ExecutorService getLightingExecutor() {return lightingExecutor != null ? lightingExecutor : generalExecutor;}

    @Override
    public int getLightBatchThreshold() {return config.getLightBatchThreshold();}

    @Override
    public int getLightUpdateIntervalMs() {return config.getLightUpdateIntervalMs();}

    @Override
    public int getLightingParallelism() { return utilityDelegate.getLightingParallelism(); }

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
    public boolean isLightingDebugEnabled() {return config.isLightingDebugEnabled();}

    @Override
    public boolean isSpawnChunkRemovalEnabled() {return config.isSpawnChunkRemovalEnabled();}

    @Override
    public boolean isPlayerChunkLoadingOptimizationEnabled() {return config.isPlayerChunkLoadingOptimizationEnabled();}

    @Override
    public int getMaxConcurrentChunkLoadsPerPlayer() {return config.getMaxConcurrentChunkLoadsPerPlayer();}

    @Override
    public boolean isEntityTrackingRangeOptimizationEnabled() {return config.isEntityTrackingRangeOptimizationEnabled();}

    @Override
    public double getEntityTrackingRangeMultiplier() {return config.getEntityTrackingRangeMultiplier();}

    @Override
    public boolean isTNTUseSakuraDensityCache() {return config.isTNTUseSakuraDensityCache();}

    @Override
    public boolean isTNTUseVectorizedAABB() {return config.isTNTUseVectorizedAABB();}

    @Override
    public boolean isTNTUseUnifiedEngine() {return config.isTNTUseUnifiedEngine();}

    @Override
    public boolean isTNTMergeEnabled() {return config.isTNTMergeEnabled();}

    @Override
    public double getTNTMergeRadius() {return config.getTNTMergeRadius();}

    @Override
    public int getTNTMaxFuseDifference() {return config.getTNTMaxFuseDifference();}

    @Override
    public float getTNTMergedPowerMultiplier() {return config.getTNTMergedPowerMultiplier();}

    @Override
    public float getTNTMaxPower() {return config.getTNTMaxPower();}

    @Override
    public boolean isTNTCacheEnabled() {return config.isTntCacheEnabled();}

    @Override
    public int getTNTCacheExpiryTicks() {return config.getTntCacheExpiryTicks();}

    @Override
    public boolean isTNTOptimizationEnabled() {return config.isTNTOptimizationEnabled();}

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
    public boolean isTNTVanillaCompatibilityEnabled() {return config.isTNTVanillaCompatibilityEnabled();}

    @Override
    public boolean isTNTUseVanillaPower() {return config.isTNTUseVanillaPower();}

    @Override
    public boolean isTNTUseVanillaFireLogic() {return config.isTNTUseVanillaFireLogic();}

    @Override
    public boolean isTNTUseVanillaDamageCalculation() {return config.isTNTUseVanillaDamageCalculation();}

    @Override
    public boolean isBeeFixEnabled() {return config.isBeeFixEnabled();}

    @Override
    public boolean isEndIslandDensityFixEnabled() {return config.isEndIslandDensityFixEnabled();}

    @Override
    public boolean isLeaderZombieHealthFixEnabled() {return config.isLeaderZombieHealthFixEnabled();}

    @Override
    public boolean isEquipmentHealthCapFixEnabled() {return config.isEquipmentHealthCapFixEnabled();}

    @Override
    public boolean isPortalSuffocationCheckDisabled() {return config.isPortalSuffocationCheckDisabled();}

    @Override
    public boolean isShulkerBulletSelfHitFixEnabled() {return config.isShulkerBulletSelfHitFixEnabled();}

    @Override
    public boolean isExecuteCommandInactiveSkipEnabled() {return config.isExecuteCommandInactiveSkipEnabled();}

    @Override
    public int getExecuteCommandSkipLevel() {return config.getExecuteCommandSkipLevel();}

    @Override
    public double getExecuteCommandSimulationDistanceMultiplier() {return config.getExecuteCommandSimulationDistanceMultiplier();}

    @Override
    public long getExecuteCommandCacheDurationMs() {return config.getExecuteCommandCacheDurationMs();}

    @Override
    public java.util.Set<String> getExecuteCommandWhitelistTypes() {return config.getExecuteCommandWhitelistTypes();}

    @Override
    public boolean isExecuteCommandDebugEnabled() {return config.isExecuteCommandDebugEnabled();}

    @Override
    public boolean isCommandDeduplicationEnabled() {return config.isCommandDeduplicationEnabled();}

    @Override
    public boolean isCommandDeduplicationDebugEnabled() {return config.isCommandDeduplicationDebugEnabled();}

    @Override
    public boolean isTNTUseFullRaycast() {return config.isTNTUseFullRaycast();}

    @Override
    public boolean isTNTUseVanillaBlockDestruction() {return config.isTNTUseVanillaBlockDestruction();}

    @Override
    public boolean isTNTUseVanillaDrops() {return config.isTNTUseVanillaDrops();}

    @Override
    public boolean isDebugLoggingEnabled() {return config.isDebugLoggingEnabled();}

    @Override
    public boolean isSmartLagCompensationEnabled() {
        return config != null && config.isSmartLagCompensationEnabled();
    }

    @Override
    public double getSmartLagTPSThreshold() {
        return config != null ? config.getSmartLagTPSThreshold() : 18.0;
    }

    @Override
    public boolean isSmartLagItemPickupDelayEnabled() {
        return config != null && config.isSmartLagItemPickupDelayEnabled();
    }

    @Override
    public boolean isSmartLagPotionEffectsEnabled() {
        return config != null && config.isSmartLagPotionEffectsEnabled();
    }

    @Override
    public boolean isSmartLagTimeAccelerationEnabled() {
        return config != null && config.isSmartLagTimeAccelerationEnabled();
    }

    @Override
    public boolean isSmartLagDebugEnabled() {
        return config != null && config.isSmartLagDebugEnabled();
    }

    @Override
    public boolean isSmartLagLogMissedTicks() {
        return config != null && config.isSmartLagLogMissedTicks();
    }

    @Override
    public boolean isSmartLagLogCompensation() {
        return config != null && config.isSmartLagLogCompensation();
    }

    @Override
    public boolean isExperienceOrbInactiveTickEnabled() {
        return config != null && config.isExperienceOrbInactiveTickEnabled();
    }

    @Override
    public double getExperienceOrbInactiveRange() {
        return config != null ? config.getExperienceOrbInactiveRange() : 32.0;
    }

    @Override
    public int getExperienceOrbInactiveMergeInterval() {
        return config != null ? config.getExperienceOrbInactiveMergeInterval() : 100;
    }

    @Override
    public boolean isExperienceOrbMergeEnabled() {

        return config != null && config.isExperienceOrbMergeEnabled();
    }

    @Override
    public int getExperienceOrbMergeInterval() {
        return config != null ? config.getExperienceOrbMergeInterval() : 20;
    }

    @Override
    public boolean isFoliaEnvironment() {return org.virgil.akiasync.mixin.util.FoliaUtils.isFoliaEnvironment();}

    @Override
    public boolean isOwnedByCurrentRegion(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.phys.Vec3 vec = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        return org.virgil.akiasync.mixin.util.FoliaUtils.isOwnedByCurrentRegion(level, vec);
    }

    @Override
    public void scheduleRegionTask(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, Runnable task) {
        net.minecraft.world.phys.Vec3 vec = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        org.virgil.akiasync.mixin.util.FoliaUtils.scheduleRegionTask(level, vec, task);
    }

    @Override
    public boolean canAccessEntityDirectly(net.minecraft.world.entity.Entity entity) {
        return org.virgil.akiasync.mixin.compat.FoliaRegionContext.canAccessEntityDirectly(entity);
    }

    @Override
    public boolean canAccessBlockPosDirectly(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        return org.virgil.akiasync.mixin.compat.FoliaRegionContext.canAccessBlockPosDirectly(level, pos);
    }

    @Override
    public void safeExecute(Runnable task, String context) {
        org.virgil.akiasync.mixin.util.ExceptionHandler.safeExecute(task, context);
    }

    @Override
    public String checkExecutorHealth(java.util.concurrent.ExecutorService executor, String name) {
        org.virgil.akiasync.util.ExecutorHealthChecker.HealthStatus status =
            org.virgil.akiasync.util.ExecutorHealthChecker.checkHealth(executor, name);
        return status.toString();
    }

    @Override
    public String getBlockId(net.minecraft.world.level.block.Block block) { return utilityDelegate.getBlockId(block); }

    public void updateConfiguration(ConfigManager newConfig) {
        this.config = newConfig;
        org.virgil.akiasync.util.DebugLogger.updateDebugState(newConfig.isDebugLoggingEnabled());
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

    @Override
    public int getChunkTickAsyncBatchSize() {return config.getChunkTickAsyncBatchSize();}

    @Override
    public boolean isStructureLocationAsyncEnabled() {return config.isStructureLocationAsyncEnabled();}

    @Override
    public int getStructureLocationThreads() {return config.getStructureLocationThreads();}

    @Override
    public boolean isLocateCommandEnabled() {return config.isLocateCommandEnabled();}

    @Override
    public int getLocateCommandSearchRadius() {return config.getLocateCommandSearchRadius();}

    @Override
    public boolean isLocateCommandSkipKnownStructures() {return config.isLocateCommandSkipKnownStructures();}

    @Override
    public boolean isVillagerTradeMapsEnabled() {return config.isVillagerTradeMapsEnabled();}

    @Override
    public java.util.Set<String> getVillagerTradeMapTypes() {return config.getVillagerTradeMapTypes();}

    @Override
    public int getVillagerMapGenerationTimeoutSeconds() {return config.getVillagerMapGenerationTimeoutSeconds();}

    @Override
    public boolean isDolphinTreasureHuntEnabled() {return config.isDolphinTreasureHuntEnabled();}

    @Override
    public int getDolphinTreasureSearchRadius() {return config.getDolphinTreasureSearchRadius();}

    @Override
    public boolean isChestExplorationMapsEnabled() {return config.isChestExplorationMapsEnabled();}

    @Override
    public java.util.Set<String> getChestExplorationLootTables() {return config.getChestExplorationLootTables();}

    @Override
    public boolean isStructureLocationDebugEnabled() {return config.isStructureLocationDebugEnabled();}

    @Override
    public boolean isStructureAlgorithmOptimizationEnabled() {return config.isStructureAlgorithmOptimizationEnabled();}

    @Override
    public String getStructureSearchPattern() {return config.getStructureSearchPattern();}

    @Override
    public boolean isStructureCachingEnabled() {return config.isStructureCachingEnabled();}

    @Override
    public boolean isBiomeAwareSearchEnabled() {return config.isBiomeAwareSearchEnabled();}

    @Override
    public int getStructureCacheMaxSize() {return config.getStructureCacheMaxSize();}

    @Override
    public long getStructureCacheExpirationMinutes() {return config.getStructureCacheExpirationMinutes();}

    @Override
    public boolean isDataPackOptimizationEnabled() {return config.isDataPackOptimizationEnabled();}

    @Override
    public int getDataPackFileLoadThreads() {return config.getDataPackFileLoadThreads();}

    @Override
    public int getDataPackZipProcessThreads() {return config.getDataPackZipProcessThreads();}

    @Override
    public int getDataPackBatchSize() {return config.getDataPackBatchSize();}

    @Override
    public long getDataPackCacheExpirationMinutes() {return config.getDataPackCacheExpirationMinutes();}

    @Override
    public int getDataPackMaxFileCacheSize() {return config.getDataPackMaxFileCacheSize();}

    @Override
    public int getDataPackMaxFileSystemCacheSize() {return config.getDataPackMaxFileSystemCacheSize();}

    @Override
    public boolean isDataPackDebugEnabled() {return config.isDataPackDebugEnabled();}

    @Override
    public void handleLocateCommandAsyncStart(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.commands.arguments.ResourceOrTagKeyArgument.Result<net.minecraft.world.level.levelgen.structure.Structure> structureResult, net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> holderSet) {
        structureDelegate.handleLocateCommandAsyncStart(sourceStack, structureResult, holderSet);
    }

    @Override
    public void handleLocateCommandResult(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.core.BlockPos structurePos, Throwable throwable) {
        structureDelegate.handleLocateCommandResult(sourceStack, structurePos, throwable);
    }

    @Override
    public void handleDolphinTreasureResult(net.minecraft.world.entity.animal.Dolphin dolphin, net.minecraft.core.BlockPos treasurePos, Throwable throwable) {
        structureDelegate.handleDolphinTreasureResult(dolphin, treasurePos, throwable);
    }

    @Override
    public void handleChestExplorationMapAsyncStart(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> destination, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, int searchRadius, boolean skipKnownStructures, Object cir) {
        structureDelegate.handleChestExplorationMapAsyncStart(stack, context, destination, mapDecoration, zoom, searchRadius, skipKnownStructures, cir);
    }

    @Override
    public void handleChestExplorationMapResult(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, Throwable throwable, Object cir) {
        structureDelegate.handleChestExplorationMapResult(stack, context, structurePos, mapDecoration, zoom, throwable, cir);
    }

    @Override
    public void handleVillagerTradeMapAsyncStart(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> destination, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Object cir) {
        structureDelegate.handleVillagerTradeMapAsyncStart(offer, trader, destination, destinationType, displayName, maxUses, villagerXp, cir);
    }

    @Override
    public void handleVillagerTradeMapResult(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Throwable throwable, Object cir) {
        structureDelegate.handleVillagerTradeMapResult(offer, trader, structurePos, destinationType, displayName, maxUses, villagerXp, throwable, cir);
    }

    @Override
    public int getVillagerTradeMapsSearchRadius() {return config.getVillagerTradeMapsSearchRadius();}

    @Override
    public boolean isVillagerTradeMapsSkipKnownStructures() {return config.isVillagerTradeMapsSkipKnownStructures();}

    @Override
    public boolean isDolphinTreasureSkipKnownStructures() {return config.isLocateCommandSkipKnownStructures();}

    @Override
    public void debugLog(String message) {
        org.virgil.akiasync.util.DebugLogger.debug(message);
    }

    @Override
    public void debugLog(String format, Object... args) {
        org.virgil.akiasync.util.DebugLogger.debug(format, args);
    }

    @Override
    public void errorLog(String message) {
        org.virgil.akiasync.util.DebugLogger.error(message);
    }

    @Override
    public void errorLog(String format, Object... args) {
        org.virgil.akiasync.util.DebugLogger.error(format, args);
    }

    @Override
    public void clearSakuraOptimizationCaches() { cacheDelegate.clearSakuraOptimizationCaches(); }

    @Override
    public void clearEntityThrottleCache(int entityId) { cacheDelegate.clearEntityThrottleCache(entityId); }

    @Override
    public java.util.Map<String, Object> getSakuraCacheStatistics() { return cacheDelegate.getSakuraCacheStatistics(); }

    @Override
    public void performSakuraCacheCleanup() { cacheDelegate.performSakuraCacheCleanup(); }

    @Override
    public boolean isVirtualEntity(net.minecraft.world.entity.Entity entity) { return entityDelegate.isVirtualEntity(entity); }

    @Override
    public boolean isSeedProtectionEnabled() { return seedDelegate.isSeedProtectionEnabled(); }

    @Override
    public boolean shouldReturnFakeSeed() { return seedDelegate.shouldReturnFakeSeed(); }

    @Override
    public long getFakeSeedValue() { return seedDelegate.getFakeSeedValue(); }

    @Override
    public boolean isQuantumSeedEnabled() { return seedDelegate.isQuantumSeedEnabled(); }

    @Override
    public byte[] getQuantumServerKey() { return seedDelegate.getQuantumServerKey(); }

    @Override
    public long getEncryptedSeed(long originalSeed, int chunkX, int chunkZ, String dimension, String generationType, long gameTime) {
        return seedDelegate.getEncryptedSeed(originalSeed, chunkX, chunkZ, dimension, generationType, gameTime);
    }

    @Override
    public boolean isSecureSeedEnabled() { return seedDelegate.isSecureSeedEnabled(); }

    @Override
    public long[] getSecureSeedWorldSeed() { return seedDelegate.getSecureSeedWorldSeed(); }

    @Override
    public void initializeSecureSeed(long originalSeed) { seedDelegate.initializeSecureSeed(originalSeed); }

    @Override
    public int getSecureSeedBits() { return seedDelegate.getSecureSeedBits(); }

    @Override
    public boolean isSeedEncryptionProtectStructures() { return seedDelegate.isSeedEncryptionProtectStructures(); }

    @Override
    public boolean isSeedEncryptionProtectOres() { return seedDelegate.isSeedEncryptionProtectOres(); }

    @Override
    public boolean isSeedEncryptionProtectSlimes() { return seedDelegate.isSeedEncryptionProtectSlimes(); }

    @Override
    public boolean isSeedEncryptionProtectBiomes() { return seedDelegate.isSeedEncryptionProtectBiomes(); }

    @Override
    public boolean isFurnaceRecipeCacheEnabled() {
        return config != null && config.isFurnaceRecipeCacheEnabled();
    }

    @Override
    public int getFurnaceRecipeCacheSize() {
        return config != null ? config.getFurnaceRecipeCacheSize() : 100;
    }

    @Override
    public boolean isFurnaceCacheApplyToBlastFurnace() {
        return config != null && config.isFurnaceCacheApplyToBlastFurnace();
    }

    @Override
    public boolean isFurnaceCacheApplyToSmoker() {
        return config != null && config.isFurnaceCacheApplyToSmoker();
    }

    @Override
    public boolean isFurnaceFixBurnTimeBug() {
        return config != null && config.isFurnaceFixBurnTimeBug();
    }

    @Override
    public boolean isCraftingRecipeCacheEnabled() {
        return config != null && config.isCraftingRecipeCacheEnabled();
    }

    @Override
    public int getCraftingRecipeCacheSize() {
        return config != null ? config.getCraftingRecipeCacheSize() : 200;
    }

    @Override
    public boolean isCraftingOptimizeBatchCrafting() {
        return config != null && config.isCraftingOptimizeBatchCrafting();
    }

    @Override
    public boolean isCraftingReduceNetworkTraffic() {
        return config != null && config.isCraftingReduceNetworkTraffic();
    }

    @Override
    public boolean isMinecartCauldronDestructionEnabled() {
        return config != null && config.isMinecartCauldronDestructionEnabled();
    }

    @Override
    public boolean isFallingBlockParallelEnabled() {
        return config != null && config.isFallingBlockParallelEnabled();
    }

    @Override
    public int getMinFallingBlocksForParallel() {
        return config != null ? config.getMinFallingBlocksForParallel() : 20;
    }

    @Override
    public int getFallingBlockBatchSize() {
        return config != null ? config.getFallingBlockBatchSize() : 10;
    }

    @Override
    public boolean isItemEntityMergeOptimizationEnabled() {
        return config != null && config.isItemEntityMergeOptimizationEnabled();
    }

    @Override
    public boolean isItemEntityCancelVanillaMerge() {
        return config != null && config.isItemEntityCancelVanillaMerge();
    }

    @Override
    public int getItemEntityMergeInterval() {
        return config != null ? config.getItemEntityMergeInterval() : 5;
    }

    @Override
    public int getItemEntityMinNearbyItems() {
        return config != null ? config.getItemEntityMinNearbyItems() : 3;
    }

    @Override
    public double getItemEntityMergeRange() {
        return config != null ? config.getItemEntityMergeRange() : 1.5;
    }

    @Override
    public boolean isItemEntityAgeOptimizationEnabled() {
        return config != null && config.isItemEntityAgeOptimizationEnabled();
    }

    @Override
    public int getItemEntityAgeInterval() {
        return config != null ? config.getItemEntityAgeInterval() : 10;
    }

    @Override
    public double getItemEntityPlayerDetectionRange() {
        return config != null ? config.getItemEntityPlayerDetectionRange() : 8.0;
    }

    @Override
    public boolean isItemEntityInactiveTickEnabled() {
        return config != null && config.isItemEntityInactiveTickEnabled();
    }

    @Override
    public double getItemEntityInactiveRange() {
        return config != null ? config.getItemEntityInactiveRange() : 32.0;
    }

    @Override
    public int getItemEntityInactiveMergeInterval() {
        return config != null ? config.getItemEntityInactiveMergeInterval() : 100;
    }

    @Override
    public boolean isFastMovementChunkLoadEnabled() {
        return config != null ? config.isFastMovementChunkLoadEnabled() : false;
    }

    @Override
    public double getFastMovementSpeedThreshold() {
        return config != null ? config.getFastMovementSpeedThreshold() : 0.5;
    }

    @Override
    public int getFastMovementPreloadDistance() {
        return config != null ? config.getFastMovementPreloadDistance() : 8;
    }

    @Override
    public int getFastMovementMaxConcurrentLoads() {
        return config != null ? config.getFastMovementMaxConcurrentLoads() : 4;
    }

    @Override
    public int getFastMovementPredictionTicks() {
        return config != null ? config.getFastMovementPredictionTicks() : 40;
    }

    @Override
    public boolean isCenterOffsetEnabled() {
        return config != null ? config.isCenterOffsetEnabled() : true;
    }

    @Override
    public double getMinOffsetSpeed() {
        return config != null ? config.getMinOffsetSpeed() : 3.0;
    }

    @Override
    public double getMaxOffsetSpeed() {
        return config != null ? config.getMaxOffsetSpeed() : 9.0;
    }

    @Override
    public double getMaxOffsetRatio() {
        return config != null ? config.getMaxOffsetRatio() : 0.75;
    }

    @Override
    public boolean isPlayerJoinWarmupEnabled() {
        return config != null ? config.isPlayerJoinWarmupEnabled() : true;
    }

    @Override
    public long getPlayerJoinWarmupDurationMs() {
        return config != null ? config.getPlayerJoinWarmupDurationMs() : 3000L;
    }

    @Override
    public double getPlayerJoinWarmupInitialRate() {
        return config != null ? config.getPlayerJoinWarmupInitialRate() : 0.5;
    }

    @Override
    public void submitChunkLoad(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, int priority, double speed) {
        chunkVisibilityDelegate.submitChunkLoad(player, chunkPos, priority, speed);
    }

    @Override
    public boolean isSuffocationOptimizationEnabled() {
        return config != null && config.isSuffocationOptimizationEnabled();
    }

    @Override
    public boolean isFastRayTraceEnabled() {
        return config != null && config.isFastRayTraceEnabled();
    }

    @Override
    public boolean isMapRenderingOptimizationEnabled() {
        return config != null && config.isMapRenderingOptimizationEnabled();
    }

    @Override
    public boolean isProjectileOptimizationEnabled() {
        return config != null && config.isProjectileOptimizationEnabled();
    }

    @Override
    public int getMaxProjectileLoadsPerTick() {
        return config != null ? config.getMaxProjectileLoadsPerTick() : 10;
    }

    @Override
    public int getMaxProjectileLoadsPerProjectile() {
        return config != null ? config.getMaxProjectileLoadsPerProjectile() : 10;
    }

    @Override
    public int getMapRenderingThreads() {
        return config != null ? config.getMapRenderingThreads() : 2;
    }

    @Override
    public void runOnMainThread(Runnable task) { utilityDelegate.runOnMainThread(task); }

    @Override
    public double getCurrentTPS() { return utilityDelegate.getCurrentTPS(); }

    @Override
    public double getCurrentMSPT() { return utilityDelegate.getCurrentMSPT(); }

    @Override
    public Object getBlockTickSmoothingScheduler() { return smoothingSchedulerDelegate.getBlockTickSmoothingScheduler(); }

    @Override
    public Object getEntityTickSmoothingScheduler() { return smoothingSchedulerDelegate.getEntityTickSmoothingScheduler(); }

    @Override
    public Object getBlockEntitySmoothingScheduler() { return smoothingSchedulerDelegate.getBlockEntitySmoothingScheduler(); }

    @Override
    public boolean submitSmoothTask(Object scheduler, Runnable task, int priority, String category) {
        return smoothingSchedulerDelegate.submitSmoothTask(scheduler, task, priority, category);
    }

    @Override
    public int submitSmoothTaskBatch(Object scheduler, java.util.List<Runnable> tasks, int priority, String category) {
        return smoothingSchedulerDelegate.submitSmoothTaskBatch(scheduler, tasks, priority, category);
    }

    @Override
    public void notifySmoothSchedulerTick(Object scheduler) { smoothingSchedulerDelegate.notifySmoothSchedulerTick(scheduler); }

    @Override
    public void updateSmoothSchedulerMetrics(Object scheduler, double tps, double mspt) {
        smoothingSchedulerDelegate.updateSmoothSchedulerMetrics(scheduler, tps, mspt);
    }

    @Override
    public boolean isCollisionOptimizationEnabled() {
        return config != null ? config.isCollisionOptimizationEnabled() : true;
    }

    @Override
    public boolean isCollisionAggressiveMode() {
        return config != null ? config.isCollisionAggressiveMode() : true;
    }

    @Override
    public java.util.Set<String> getCollisionExcludedEntities() {
        return config != null ? config.getCollisionExcludedEntities() : java.util.Collections.emptySet();
    }

    @Override
    public boolean isNativeCollisionsEnabled() {
        return config != null ? config.isNativeCollisionsEnabled() : true;
    }

    @Override
    public boolean isNativeCollisionsFallbackEnabled() {
        return config != null ? config.isNativeCollisionsFallbackEnabled() : true;
    }

    @Override
    public boolean isCollisionBlockCacheEnabled() {
        return config != null ? config.isCollisionBlockCacheEnabled() : true;
    }

    @Override
    public int getCollisionBlockCacheSize() {
        return config != null ? config.getCollisionBlockCacheSize() : 512;
    }

    @Override
    public int getCollisionBlockCacheExpireTicks() {
        return config != null ? config.getCollisionBlockCacheExpireTicks() : 600;
    }

    @Override
    public boolean isRayCollisionEnabled() {
        return config != null ? config.isRayCollisionEnabled() : true;
    }

    @Override
    public double getRayCollisionMaxDistance() {
        return config != null ? config.getRayCollisionMaxDistance() : 64.0;
    }

    @Override
    public boolean isShapeOptimizationEnabled() {
        return config != null ? config.isShapeOptimizationEnabled() : true;
    }

    @Override
    public boolean isShapePrecomputeArrays() {
        return config != null ? config.isShapePrecomputeArrays() : true;
    }

    @Override
    public boolean isShapeBlockShapeCache() {
        return config != null ? config.isShapeBlockShapeCache() : true;
    }

    @Override
    public int getShapeBlockShapeCacheSize() {
        return config != null ? config.getShapeBlockShapeCacheSize() : 512;
    }

    @Override
    public boolean isLightingPrioritySchedulingEnabled() {
        return config != null && config.isLightingPrioritySchedulingEnabled();
    }

    @Override
    public int getLightingHighPriorityRadius() {
        return config != null ? config.getLightingHighPriorityRadius() : 32;
    }

    @Override
    public int getLightingMediumPriorityRadius() {
        return config != null ? config.getLightingMediumPriorityRadius() : 64;
    }

    @Override
    public int getLightingLowPriorityRadius() {
        return config != null ? config.getLightingLowPriorityRadius() : 128;
    }

    @Override
    public long getLightingMaxLowPriorityDelay() {
        return config != null ? config.getLightingMaxLowPriorityDelay() : 500;
    }

    @Override
    public boolean isLightingDebouncingEnabled() {
        return config != null && config.isLightingDebouncingEnabled();
    }

    @Override
    public long getLightingDebounceDelay() {
        return config != null ? config.getLightingDebounceDelay() : 50;
    }

    @Override
    public int getLightingMaxUpdatesPerSecond() {
        return config != null ? config.getLightingMaxUpdatesPerSecond() : 20;
    }

    @Override
    public long getLightingResetOnStableMs() {
        return config != null ? config.getLightingResetOnStableMs() : 200;
    }

    @Override
    public boolean isLightingMergingEnabled() {
        return config != null && config.isLightingMergingEnabled();
    }

    @Override
    public int getLightingMergeRadius() {
        return config != null ? config.getLightingMergeRadius() : 2;
    }

    @Override
    public long getLightingMergeDelay() {
        return config != null ? config.getLightingMergeDelay() : 10;
    }

    @Override
    public int getLightingMaxMergedUpdates() {
        return config != null ? config.getLightingMaxMergedUpdates() : 64;
    }

    @Override
    public boolean isLightingChunkBorderEnabled() {
        return config != null && config.isLightingChunkBorderEnabled();
    }

    @Override
    public boolean isLightingBatchBorderUpdates() {
        return config != null && config.isLightingBatchBorderUpdates();
    }

    @Override
    public long getLightingBorderUpdateDelay() {
        return config != null ? config.getLightingBorderUpdateDelay() : 20;
    }

    @Override
    public int getLightingCrossChunkBatchSize() {
        return config != null ? config.getLightingCrossChunkBatchSize() : 32;
    }

    @Override
    public boolean isLightingAdaptiveEnabled() {
        return config != null && config.isLightingAdaptiveEnabled();
    }

    @Override
    public int getLightingMonitorInterval() {
        return config != null ? config.getLightingMonitorInterval() : 10;
    }

    @Override
    public boolean isLightingAutoAdjustThreads() {
        return config != null && config.isLightingAutoAdjustThreads();
    }

    @Override
    public boolean isLightingAutoAdjustBatchSize() {
        return config != null && config.isLightingAutoAdjustBatchSize();
    }

    @Override
    public int getLightingTargetQueueSize() {
        return config != null ? config.getLightingTargetQueueSize() : 100;
    }

    @Override
    public int getLightingTargetLatency() {
        return config != null ? config.getLightingTargetLatency() : 50;
    }

    @Override
    public boolean isLightingChunkUnloadEnabled() {
        return config != null && config.isLightingChunkUnloadEnabled();
    }

    @Override
    public boolean isLightingAsyncCleanup() {
        return config != null && config.isLightingAsyncCleanup();
    }

    @Override
    public int getLightingCleanupBatchSize() {
        return config != null ? config.getLightingCleanupBatchSize() : 16;
    }

    @Override
    public long getLightingCleanupDelay() {
        return config != null ? config.getLightingCleanupDelay() : 100;
    }

    @Override
    public String getLightingThreadPoolMode() {
        return config != null ? config.getLightingThreadPoolMode() : "auto";
    }

    @Override
    public String getLightingThreadPoolCalculation() {
        return config != null ? config.getLightingThreadPoolCalculation() : "cores/3";
    }

    @Override
    public int getLightingMinThreads() {
        return config != null ? config.getLightingMinThreads() : 1;
    }

    @Override
    public int getLightingMaxThreads() {
        return config != null ? config.getLightingMaxThreads() : 8;
    }

    @Override
    public int getLightingBatchThresholdMax() {
        return config != null ? config.getLightingBatchThresholdMax() : 64;
    }

    @Override
    public boolean isLightingAggressiveBatching() {
        return config != null && config.isLightingAggressiveBatching();
    }

    @Override
    public boolean isJigsawOptimizationEnabled() { return jigsawDelegate.isJigsawOptimizationEnabled(); }

    @Override
    public void initializeJigsawOctree(net.minecraft.world.phys.AABB bounds) { jigsawDelegate.initializeJigsawOctree(bounds); }

    @Override
    public boolean hasJigsawOctree() { return jigsawDelegate.hasJigsawOctree(); }

    @Override
    public void insertIntoJigsawOctree(net.minecraft.world.phys.AABB box) { jigsawDelegate.insertIntoJigsawOctree(box); }

    @Override
    public boolean jigsawOctreeIntersects(net.minecraft.world.phys.AABB box) { return jigsawDelegate.jigsawOctreeIntersects(box); }

    @Override
    public void clearJigsawOctree() { jigsawDelegate.clearJigsawOctree(); }

    @Override
    public String getJigsawOctreeStats() { return jigsawDelegate.getJigsawOctreeStats(); }

    @Override
    public boolean isChunkVisibilityFilterEnabled() { return chunkVisibilityDelegate.isChunkVisibilityFilterEnabled(); }

    @Override
    public boolean isChunkVisible(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, net.minecraft.server.level.ServerLevel level) {
        return chunkVisibilityDelegate.isChunkVisible(player, chunkPos, level); }

    @Override
    public void tickChunkVisibilityFilter() { networkDelegate.tickChunkVisibilityFilter(); }

    @Override
    public void clearWorldCaches(String worldName) { chunkVisibilityDelegate.clearWorldCaches(worldName); }

    @Override
    public void prewarmPlayerPaths(java.util.UUID playerId) { executorDelegate.prewarmPlayerPaths(playerId); }

    @Override
    public void cleanupPlayerPaths(java.util.UUID playerId) { executorDelegate.cleanupPlayerPaths(playerId); }

    @Override
    public void restartVillagerExecutor() { executorDelegate.restartVillagerExecutor(); }

    @Override
    public void restartTNTExecutor() { executorDelegate.restartTNTExecutor(); }

    @Override
    public void restartBrainExecutor() { executorDelegate.restartBrainExecutor(); }

    @Override
    public void restartChunkExecutor(int threadCount) { executorDelegate.restartChunkExecutor(threadCount); }

    @Override
    public void clearVillagerBreedCache() { cacheDelegate.clearVillagerBreedCache(); }

    @Override
    public void resetBrainExecutorStatistics() { executorDelegate.resetBrainExecutorStatistics(); }

    @Override
    public void resetAsyncMetrics() { executorDelegate.resetAsyncMetrics(); }

    @Override
    public boolean isEndermanBlockCarryLimiterEnabled() {
        return config.isEndermanBlockCarryLimiterEnabled();
    }

    @Override
    public int getEndermanMaxCarrying() {
        return config.getEndermanMaxCarrying();
    }

    @Override
    public boolean isEndermanCountTowardsMobCap() {
        return config.isEndermanCountTowardsMobCap();
    }

    @Override
    public boolean isEndermanPreventPickup() {
        return config.isEndermanPreventPickup();
    }

    @Override
    public boolean isMultithreadedEntityTrackerEnabled() {
        return config.isMultithreadedEntityTrackerEnabled();
    }

    @Override
    public boolean isVelocityCompressionEnabled() {
        return true;
    }

    @Override
    public boolean isAdvancedNetworkOptimizationEnabled() {
        return config != null && config.isAdvancedNetworkOptimizationEnabled();
    }

    @Override
    public boolean isFastVarIntEnabled() {
        return config != null && config.isFastVarIntEnabled();
    }

    @Override
    public boolean isEventLoopAffinityEnabled() {
        return config != null && config.isEventLoopAffinityEnabled();
    }

    @Override
    public boolean isByteBufOptimizerEnabled() {
        return config != null && config.isByteBufOptimizerEnabled();
    }

    @Override
    public boolean isStrictEventLoopChecking() {
        return config != null && config.isStrictEventLoopChecking();
    }

    @Override
    public boolean isPooledByteBufAllocator() {
        return config != null ? config.isPooledByteBufAllocator() : true;
    }

    @Override
    public boolean isDirectByteBufPreferred() {
        return config != null ? config.isDirectByteBufPreferred() : true;
    }

    @Override
    public boolean isSkipZeroMovementPacketsEnabled() {
        return config != null && config.isSkipZeroMovementPacketsEnabled();
    }

    @Override
    public boolean isSkipZeroMovementPacketsStrictMode() {
        return config != null && config.isSkipZeroMovementPacketsStrictMode();
    }

    @Override
    public boolean isMtuAwareBatchingEnabled() {
        return config != null && config.isMtuAwareBatchingEnabled();
    }

    @Override
    public int getMtuLimit() {
        return config != null ? config.getMtuLimit() : 1396;
    }

    @Override
    public int getMtuHardCapPackets() {
        return config != null ? config.getMtuHardCapPackets() : 4096;
    }

    @Override
    public boolean isFlushConsolidationEnabled() {
        return config != null && config.isFlushConsolidationEnabled();
    }

    @Override
    public int getFlushConsolidationExplicitFlushAfterFlushes() {
        return config != null ? config.getFlushConsolidationExplicitFlushAfterFlushes() : 256;
    }

    @Override
    public boolean isFlushConsolidationConsolidateWhenNoReadInProgress() {
        return config != null && config.isFlushConsolidationConsolidateWhenNoReadInProgress();
    }

    @Override
    public boolean isNativeCompressionEnabled() {
        return config != null && config.isNativeCompressionEnabled();
    }

    @Override
    public int getNativeCompressionLevel() {
        return config != null ? config.getNativeCompressionLevel() : 6;
    }

    @Override
    public boolean isNativeEncryptionEnabled() {
        return config != null && config.isNativeEncryptionEnabled();
    }

    @Override
    public boolean isExplosionBlockUpdateOptimizationEnabled() {
        return config != null && config.isExplosionBlockUpdateOptimizationEnabled();
    }

    @Override
    public int getExplosionBlockChangeThreshold() {
        return config != null ? config.getExplosionBlockChangeThreshold() : 512;
    }

    @Override
    public long getConnectionPendingBytes(Object connection) { return networkDelegate.getConnectionPendingBytes(connection); }

    @Override
    public boolean addFlushConsolidationHandler(Object channel, int explicitFlushAfterFlushes, boolean consolidateWhenNoReadInProgress) {
        return networkDelegate.addFlushConsolidationHandler(channel, explicitFlushAfterFlushes, consolidateWhenNoReadInProgress);
    }

    @Override
    public void sendPacketWithoutFlush(Object connection, Object packet) { networkDelegate.sendPacketWithoutFlush(connection, packet); }

    @Override
    public void flushConnection(Object connection) { networkDelegate.flushConnection(connection); }

    @Override
    public Object getConnectionFromListener(Object listener) { return networkDelegate.getConnectionFromListener(listener); }

    @Override
    public int getHighLatencyThreshold() {
        return config != null ? config.getHighLatencyThreshold() : 150;
    }

    @Override
    public int getHighLatencyMinViewDistance() {
        return config != null ? config.getHighLatencyMinViewDistance() : 5;
    }

    @Override
    public long getHighLatencyDurationMs() {
        return config != null ? config.getHighLatencyDurationMs() : 60000L;
    }

    @Override
    public boolean isAfkPacketThrottleEnabled() {
        return config != null && config.isAfkPacketThrottleEnabled();
    }

    @Override
    public long getAfkDurationMs() {
        return config != null ? config.getAfkDurationMs() : 120000L;
    }

    @Override
    public double getAfkParticleMaxDistance() {
        return config != null ? config.getAfkParticleMaxDistance() : 0.0;
    }

    @Override
    public double getAfkSoundMaxDistance() {
        return config != null ? config.getAfkSoundMaxDistance() : 64.0;
    }

    @Override
    public int getMidTickChunkTasksIntervalMs() {

        return config != null ? config.getMidTickChunkTasksIntervalMs() : 1;
    }

    @Override
    public boolean isMultiNettyEventLoopEnabled() {
        return config != null && config.isMultiNettyEventLoopEnabled();
    }

    @Override
    public boolean isPalettedContainerLockRemovalEnabled() {
        return config != null && config.isPalettedContainerLockRemovalEnabled();
    }

    @Override
    public boolean isSpawnDensityArrayEnabled() {
        return config != null && config.isSpawnDensityArrayEnabled();
    }

    @Override
    public boolean isTypeFilterableListOptimizationEnabled() {
        return config != null && config.isTypeFilterableListOptimizationEnabled();
    }

    @Override
    public boolean isEntityTrackerLinkedHashMapEnabled() {
        return config != null && config.isEntityTrackerLinkedHashMapEnabled();
    }

    @Override
    public boolean isBiomeAccessOptimizationEnabled() {
        return config != null && config.isBiomeAccessOptimizationEnabled();
    }

    @Override
    public boolean isEntityMoveZeroVelocityOptimizationEnabled() {
        return config != null && config.isEntityMoveZeroVelocityOptimizationEnabled();
    }

    @Override
    public boolean isEntityTrackerDistanceCacheEnabled() {
        return config != null && config.isEntityTrackerDistanceCacheEnabled();
    }

    @Override
    public void handleConnectionProtocolChange(Object connection, int protocolOrdinal) {
        networkDelegate.handleConnectionProtocolChange(connection, protocolOrdinal);
    }

    @Override
    public boolean isDynamicChunkSendRateEnabled() {
        return config != null && config.isDynamicChunkSendRateEnabled();
    }

    @Override
    public long getDynamicChunkLimitBandwidth() {
        return config != null ? config.getDynamicChunkLimitBandwidth() : 10240L;
    }

    @Override
    public long getDynamicChunkGuaranteedBandwidth() {
        return config != null ? config.getDynamicChunkGuaranteedBandwidth() : 512L;
    }

    @Override
    public boolean isPacketCompressionOptimizationEnabled() {
        return config != null && config.isPacketCompressionOptimizationEnabled();
    }

    @Override
    public boolean isAdaptiveCompressionThresholdEnabled() {
        return config != null && config.isAdaptiveCompressionThresholdEnabled();
    }

    @Override
    public boolean isSkipSmallPacketsEnabled() {
        return config != null && config.isSkipSmallPacketsEnabled();
    }

    @Override
    public int getSkipSmallPacketsThreshold() {
        return config != null ? config.getSkipSmallPacketsThreshold() : 32;
    }

    @Override
    public boolean isChunkBatchOptimizationEnabled() {
        return config != null && config.isChunkBatchOptimizationEnabled();
    }

    @Override
    public float getChunkBatchMinChunks() {
        return config != null ? config.getChunkBatchMinChunks() : 2.0f;
    }

    @Override
    public float getChunkBatchMaxChunks() {
        return config != null ? config.getChunkBatchMaxChunks() : 32.0f;
    }

    @Override
    public int getChunkBatchMaxUnacked() {
        return config != null ? config.getChunkBatchMaxUnacked() : 8;
    }

    @Override
    public boolean isPacketPriorityQueueEnabled() {
        return config != null && config.isPacketPriorityQueueEnabled();
    }

    @Override
    public boolean isPrioritizePlayerPacketsEnabled() {
        return config != null && config.isPrioritizePlayerPacketsEnabled();
    }

    @Override
    public boolean isPrioritizeChunkPacketsEnabled() {
        return config != null && config.isPrioritizeChunkPacketsEnabled();
    }

    @Override
    public boolean isDeprioritizeParticlesEnabled() {
        return config != null && config.isDeprioritizeParticlesEnabled();
    }

    @Override
    public boolean isDeprioritizeSoundsEnabled() {
        return config != null && config.isDeprioritizeSoundsEnabled();
    }

    @Override
    public long getNetworkTrafficInRate() {
        return org.virgil.akiasync.mixin.network.NetworkTrafficTracker.getCurrentInRate();
    }

    @Override
    public long getNetworkTrafficOutRate() {
        return org.virgil.akiasync.mixin.network.NetworkTrafficTracker.getCurrentOutRate();
    }

    @Override
    public long getNetworkTrafficTotalIn() {
        return org.virgil.akiasync.mixin.network.NetworkTrafficTracker.getTotalBytesIn();
    }

    @Override
    public long getNetworkTrafficTotalOut() {
        return org.virgil.akiasync.mixin.network.NetworkTrafficTracker.getTotalBytesOut();
    }

    @Override
    public void calculateNetworkTrafficRates() {
        org.virgil.akiasync.mixin.network.NetworkTrafficTracker.calculateRates();
    }

    @Override
    public void setPacketStatisticsEnabled(boolean enabled) {
        org.virgil.akiasync.mixin.network.PacketStatistics.setEnabled(enabled);
    }

    @Override
    public boolean isPacketStatisticsEnabled() { return networkDelegate.isPacketStatisticsEnabled(); }

    @Override
    public void resetPacketStatistics() { networkDelegate.resetPacketStatistics(); }

    @Override
    public long getPacketStatisticsElapsedSeconds() { return networkDelegate.getPacketStatisticsElapsedSeconds(); }

    @Override
    public java.util.List<Object[]> getTopOutgoingPackets(int limit) { return networkDelegate.getTopOutgoingPackets(limit); }

    @Override
    public java.util.List<Object[]> getTopIncomingPackets(int limit) { return networkDelegate.getTopIncomingPackets(limit); }

    @Override
    public long getTotalOutgoingPacketCount() { return networkDelegate.getTotalOutgoingPacketCount(); }

    @Override
    public long getTotalIncomingPacketCount() { return networkDelegate.getTotalIncomingPacketCount(); }

    @Override
    public int getGeneralThreadPoolSize() {
        return config.getThreadPoolSize();
    }

    @Override
    public java.util.List<net.minecraft.core.BlockPos> fireEntityExplodeEvent(
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.Entity entity,
            net.minecraft.world.phys.Vec3 center,
            java.util.List<net.minecraft.core.BlockPos> blocks,
            float yield) {
        return explosionDelegate.fireEntityExplodeEvent(level, entity, center, blocks, yield);
    }
}

