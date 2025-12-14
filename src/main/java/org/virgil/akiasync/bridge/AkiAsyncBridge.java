package org.virgil.akiasync.bridge;

import java.util.concurrent.ExecutorService;

import org.virgil.akiasync.AkiAsyncPlugin;
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
    
    private TaskSmoothingScheduler blockTickScheduler;
    private TaskSmoothingScheduler entityTickScheduler;
    private TaskSmoothingScheduler blockEntityScheduler;

    public AkiAsyncBridge(AkiAsyncPlugin plugin, ExecutorService generalExecutor, ExecutorService lightingExecutor, ExecutorService tntExecutor, ExecutorService chunkTickExecutor, ExecutorService villagerBreedExecutor, ExecutorService brainExecutor, ExecutorService collisionExecutor) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.generalExecutor = generalExecutor;
        this.lightingExecutor = lightingExecutor;
        this.tntExecutor = tntExecutor;
        this.chunkTickExecutor = chunkTickExecutor;
        this.villagerBreedExecutor = villagerBreedExecutor;
        this.brainExecutor = brainExecutor;
        this.collisionExecutor = collisionExecutor;
        
        initializeSmoothingSchedulers();
        
        ConfigReloader.registerListener(this);
    }
    
    private void initializeSmoothingSchedulers() {
        try {

            boolean isFolia = false;
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {

            }
            
            if (isFolia) {
                plugin.getLogger().info("[AkiAsync] Folia environment detected - TaskSmoothingScheduler disabled");
                return;
            }
            
            if (config != null && config.isChunkTickAsyncEnabled() && generalExecutor != null) {
                int batchSize = config.getChunkTickAsyncBatchSize();
                blockTickScheduler = new TaskSmoothingScheduler(
                    generalExecutor,
                    batchSize * 10,
                    batchSize * 2,
                    3
                );
                plugin.getLogger().info("[AkiAsync] BlockTick TaskSmoothingScheduler initialized");
            }
            
            if (config != null && config.isEntityTickParallel() && generalExecutor != null) {
                int batchSize = config.getEntityTickBatchSize();
                entityTickScheduler = new TaskSmoothingScheduler(
                    generalExecutor,
                    batchSize * 20,
                    batchSize * 3,
                    2
                );
                plugin.getLogger().info("[AkiAsync] EntityTick TaskSmoothingScheduler initialized");
            }
            
            if (config != null && config.isBlockEntityParallelTickEnabled() && generalExecutor != null) {
                int batchSize = config.getBlockEntityParallelBatchSize();
                blockEntityScheduler = new TaskSmoothingScheduler(
                    generalExecutor,
                    batchSize * 15,
                    batchSize * 2,
                    3
                );
                plugin.getLogger().info("[AkiAsync] BlockEntity TaskSmoothingScheduler initialized");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[AkiAsync] Failed to initialize TaskSmoothingSchedulers: " + e.getMessage());
        }
    }
    
    @Override
    public void onConfigReload(ConfigManager newConfig) {
        
        this.config = newConfig;
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
    public boolean isLivingEntityTravelOptimizationEnabled() {return config.isLivingEntityTravelOptimizationEnabled();}

    @Override
    public int getLivingEntityTravelSkipInterval() {return config.getLivingEntityTravelSkipInterval();}

    @Override
    public boolean isBehaviorThrottleEnabled() {return config.isBehaviorThrottleEnabled();}

    @Override
    public int getBehaviorThrottleInterval() {return config.getBehaviorThrottleInterval();}

    @Override
    public boolean isMobDespawnOptimizationEnabled() {return config.isMobDespawnOptimizationEnabled();}

    @Override
    public int getMobDespawnCheckInterval() {return config.getMobDespawnCheckInterval();}

    @Override
    public long getAsyncAITimeoutMicros() {return config.getAsyncAITimeoutMicros();}

    @Override
    public boolean isAiSpatialIndexEnabled() {return config.isAiSpatialIndexEnabled();}
    
    @Override
    public int getAiSpatialIndexGridSize() {return config.getAiSpatialIndexGridSize();}
    
    @Override
    public boolean isAiSpatialIndexAutoUpdate() {return config.isAiSpatialIndexAutoUpdate();}
    
    @Override
    public boolean isAiSpatialIndexPlayerIndexEnabled() {return config.isAiSpatialIndexPlayerIndexEnabled();}
    
    @Override
    public boolean isAiSpatialIndexPoiIndexEnabled() {return config.isAiSpatialIndexPoiIndexEnabled();}
    
    @Override
    public boolean isAiSpatialIndexStatisticsEnabled() {return config.isAiSpatialIndexStatisticsEnabled();}
    
    @Override
    public int getAiSpatialIndexLogIntervalSeconds() {return config.getAiSpatialIndexLogIntervalSeconds();}

    @Override
    public boolean isVillagerOptimizationEnabled() {return config.isVillagerOptimizationEnabled();}

    @Override
    public boolean isVillagerUsePOISnapshot() {return config.isVillagerUsePOISnapshot();}

    @Override
    public boolean isVillagerPoiCacheEnabled() {return config.isVillagerPoiCacheEnabled();}

    @Override
    public int getVillagerPoiCacheExpireTime() {return config.getVillagerPoiCacheExpireTime();}
    
    @Override
    public boolean isWanderingTraderOptimizationEnabled() {return config.isWanderingTraderOptimizationEnabled();}
    
    @Override
    public boolean isWardenOptimizationEnabled() {return config.isWardenOptimizationEnabled();}
    
    @Override
    public boolean isHoglinOptimizationEnabled() {return config.isHoglinOptimizationEnabled();}
    
    @Override
    public boolean isAllayOptimizationEnabled() {return config.isAllayOptimizationEnabled();}
    
    @Override
    public boolean isEndermanOptimizationEnabled() {return config.isEndermanOptimizationEnabled();}
    
    @Override
    public int getEndermanTickInterval() {return config.getEndermanTickInterval();}
    
    @Override
    public boolean isEndermanAllowPickupBlocks() {return config.isEndermanAllowPickupBlocks();}
    
    @Override
    public boolean isArmadilloOptimizationEnabled() {return config.isArmadilloOptimizationEnabled();}
    
    @Override
    public int getArmadilloTickInterval() {return config.getArmadilloTickInterval();}
    
    @Override
    public boolean isSnifferOptimizationEnabled() {return config.isSnifferOptimizationEnabled();}
    
    @Override
    public int getSnifferTickInterval() {return config.getSnifferTickInterval();}
    
    @Override
    public boolean isCamelOptimizationEnabled() {return config.isCamelOptimizationEnabled();}
    
    @Override
    public int getCamelTickInterval() {return config.getCamelTickInterval();}
    
    @Override
    public boolean isFrogOptimizationEnabled() {return config.isFrogOptimizationEnabled();}
    
    @Override
    public int getFrogTickInterval() {return config.getFrogTickInterval();}
    
    @Override
    public boolean isGoatOptimizationEnabled() {return config.isGoatOptimizationEnabled();}
    
    @Override
    public int getGoatTickInterval() {return config.getGoatTickInterval();}
    
    @Override
    public boolean isPandaOptimizationEnabled() {return config.isPandaOptimizationEnabled();}
    
    @Override
    public int getPandaTickInterval() {return config.getPandaTickInterval();}
    
    @Override
    public boolean isEndermanAllowPlaceBlocks() {return config.isEndermanAllowPlaceBlocks();}

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
    public boolean isDabEnabled() { return config.isDabEnabled(); }

    @Override
    public int getDabStartDistance() { return config.getDabStartDistance(); }

    @Override
    public int getDabActivationDistMod() { return config.getDabActivationDistMod(); }

    @Override
    public int getDabMaxTickInterval() { return config.getDabMaxTickInterval(); }
    
    @Override
    public boolean isBrainMemoryOptimizationEnabled() { return config.isBrainMemoryOptimizationEnabled(); }
    
    @Override
    public boolean isPoiSnapshotEnabled() { return config.isPoiSnapshotEnabled(); }

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
    public boolean shouldThrottleEntity(Object entity) {
        if (plugin.getThrottlingManager() == null) {
            return false;
        }

        org.bukkit.entity.Entity bukkitEntity = null;

        if (entity instanceof org.bukkit.entity.Entity) {
            bukkitEntity = (org.bukkit.entity.Entity) entity;
        } else if (entity instanceof net.minecraft.world.entity.Entity) {
            try {
                net.minecraft.world.entity.Entity mcEntity = (net.minecraft.world.entity.Entity) entity;
                bukkitEntity = mcEntity.getBukkitEntity();
            } catch (Exception e) {
                return false;
            }
        }

        if (bukkitEntity == null) {
            return false;
        }

        return plugin.getThrottlingManager().shouldThrottle(bukkitEntity);
    }

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
    public boolean isSimpleEntitiesOptimizationEnabled() {return config.isSimpleEntitiesOptimizationEnabled();}

    @Override
    public boolean isSimpleEntitiesUsePOISnapshot() {return config.isSimpleEntitiesUsePOISnapshot();}

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
    public boolean isEntityTrackerEnabled() {return config.isEntityTrackerEnabled();}

    @Override
    public int getEntityTrackerQueueSize() {return config.getMaxQueueSize();}

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
    public boolean isAsyncLightingEnabled() {return config.isAsyncLightingEnabled();}

    @Override
    public ExecutorService getLightingExecutor() {return lightingExecutor != null ? lightingExecutor : generalExecutor;}

    @Override
    public int getLightBatchThreshold() {return config.getLightBatchThreshold();}

    @Override
    public int getLightUpdateIntervalMs() {return config.getLightUpdateIntervalMs();}

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
    public boolean isUsePandaWireAlgorithm() {return config.isUsePandaWireAlgorithm();}

    @Override
    public boolean isRedstoneNetworkCacheEnabled() {return config.isRedstoneNetworkCacheEnabled();}

    @Override
    public int getRedstoneNetworkCacheExpireTicks() {return config.getRedstoneNetworkCacheExpireTicks();}

    @Override
    public boolean isTNTUseSakuraDensityCache() {return config.isTNTUseSakuraDensityCache();}

    @Override
    public boolean isTNTMergeEnabled() {return config.isTNTMergeEnabled();}

    @Override
    public double getTNTMergeRadius() {return config.getTNTMergeRadius();}

    @Override
    public int getTNTMaxFuseDifference() {return config.getTNTMaxFuseDifference();}

    @Override
    public float getTNTMergedPowerMultiplier() {return config.getTNTMergedPowerMultiplier();}

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
    public String getBlockId(net.minecraft.world.level.block.Block block) {
        try {
            net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            return key != null ? key.toString() : "unknown";
        } catch (Exception e) {
            return block.getClass().getSimpleName().toLowerCase();
        }
    }

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
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                net.minecraft.server.level.ServerLevel level = sourceStack.getLevel();
                net.minecraft.core.BlockPos startPos = net.minecraft.core.BlockPos.containing(sourceStack.getPosition());

                if (config.isStructureLocationDebugEnabled()) {
                    System.out.println("[AkiAsync] Starting async locate command from " + startPos);
                }

                com.mojang.datafixers.util.Pair<net.minecraft.core.BlockPos, net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure>> result;

                if (config.isStructureAlgorithmOptimizationEnabled()) {
                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Using optimized structure search algorithm");
                    }
                    result = org.virgil.akiasync.mixin.async.structure.OptimizedStructureLocator.findNearestStructureOptimized(
                        level, holderSet, startPos,
                        config.getLocateCommandSearchRadius(),
                        config.isLocateCommandSkipKnownStructures()
                    );
                } else {
                    result = level.getChunkSource().getGenerator().findNearestMapStructure(
                        level, holderSet, startPos,
                        config.getLocateCommandSearchRadius(),
                        config.isLocateCommandSkipKnownStructures()
                    );
                }

                return result != null ? result.getFirst() : null;
            } catch (Exception e) {
                System.err.println("[AkiAsync] Error in async locate command: " + e.getMessage());
                return null;
            }
        }, generalExecutor).whenComplete((foundStructure, asyncThrowable) -> {
            handleLocateCommandResult(sourceStack, foundStructure, asyncThrowable);
        });
    }

    @Override
    public void handleLocateCommandResult(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.core.BlockPos structurePos, Throwable throwable) {
        if (structurePos == null && throwable == null) {
            FoliaSchedulerAdapter.runTask(plugin, () -> {
                try {
                    sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                        "§a[AkiAsync] Structure location started asynchronously..."), false);

                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Locate command started asynchronously");
                    }

                    FoliaSchedulerAdapter.runTaskLater(plugin, () -> {
                        sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§b[AkiAsync] Async structure location completed (test mode)"), false);
                    }, 20L);

                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error starting async locate command: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            return;
        }

        FoliaSchedulerAdapter.runTask(plugin, () -> {
            try {
                if (throwable != null) {
                    System.err.println("[AkiAsync] Locate command failed: " + throwable.getMessage());
                    sourceStack.sendFailure(net.minecraft.network.chat.Component.literal("Structure location failed: " + throwable.getMessage()));
                    return;
                }

                if (structurePos != null) {
                    sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                        "The nearest structure is at " + structurePos.getX() + ", " + structurePos.getY() + ", " + structurePos.getZ()), false);

                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Locate command completed: structure found at " + structurePos);
                    }
                } else {
                    sourceStack.sendFailure(net.minecraft.network.chat.Component.literal("Could not find that structure nearby"));

                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Locate command completed: no structure found");
                    }
                }
            } catch (Exception e) {
                System.err.println("[AkiAsync] Error processing locate command result: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void handleDolphinTreasureResult(net.minecraft.world.entity.animal.Dolphin dolphin, net.minecraft.core.BlockPos treasurePos, Throwable throwable) {
        if (treasurePos == null && throwable == null) {
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) dolphin.level();
                    net.minecraft.core.BlockPos startPos = dolphin.blockPosition();

                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Starting async dolphin treasure hunt from " + startPos);
                    }

                    net.minecraft.core.BlockPos foundTreasure = level.findNearestMapStructure(
                        net.minecraft.tags.StructureTags.DOLPHIN_LOCATED,
                        startPos,
                        config.getDolphinTreasureSearchRadius(),
                        isDolphinTreasureSkipKnownStructures()
                    );

                    return foundTreasure;
                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error in async dolphin treasure hunt: " + e.getMessage());
                    return null;
                }
            }, generalExecutor).whenComplete((foundTreasure, asyncThrowable) -> {
                handleDolphinTreasureResult(dolphin, foundTreasure, asyncThrowable);
            });
            return;
        }

        FoliaEntityAdapter.safeEntityOperation(plugin, (org.bukkit.entity.Entity) dolphin.getBukkitEntity(), (entity) -> {
            try {
                if (throwable != null) {
                    System.err.println("[AkiAsync] Dolphin treasure hunt failed: " + throwable.getMessage());
                    return;
                }

                if (treasurePos != null) {
                    try {
                        java.lang.reflect.Field treasurePosField = dolphin.getClass().getDeclaredField("treasurePos");
                        treasurePosField.setAccessible(true);
                        treasurePosField.set(dolphin, treasurePos);

                        dolphin.level().addParticle(net.minecraft.core.particles.ParticleTypes.DOLPHIN,
                            dolphin.getX(), dolphin.getY(), dolphin.getZ(), 0.0D, 0.0D, 0.0D);

                        if (config.isStructureLocationDebugEnabled()) {
                            System.out.println("[AkiAsync] Dolphin treasure hunt completed: treasure found at " + treasurePos);
                        }
                    } catch (Exception e) {
                        System.err.println("[AkiAsync] Error setting dolphin treasure position: " + e.getMessage());
                    }
                } else {
                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Dolphin treasure hunt completed: no treasure found");
                    }
                }
            } catch (Exception e) {
                System.err.println("[AkiAsync] Error processing dolphin treasure result: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void handleChestExplorationMapAsyncStart(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> destination, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, int searchRadius, boolean skipKnownStructures, Object cir) {
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                net.minecraft.server.level.ServerLevel level = context.getLevel();
                net.minecraft.world.phys.Vec3 origin = context.getOptionalParameter(
                    net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
                net.minecraft.core.BlockPos startPos = origin != null ?
                    net.minecraft.core.BlockPos.containing(origin) : new net.minecraft.core.BlockPos(0, 64, 0);

                if (config.isStructureLocationDebugEnabled()) {
                    System.out.println("[AkiAsync] Starting async chest exploration map creation from " + startPos + " for " + destination.location());
                }

                net.minecraft.core.BlockPos foundStructure = level.findNearestMapStructure(
                    destination, startPos, searchRadius, skipKnownStructures);

                if (config.isStructureLocationDebugEnabled()) {
                    if (foundStructure != null) {
                        System.out.println("[AkiAsync] Chest exploration map: structure found at " + foundStructure);
                    } else {
                        System.out.println("[AkiAsync] Chest exploration map: no structure found");
                    }
                }

                return foundStructure;
            } catch (Exception e) {
                System.err.println("[AkiAsync] Error in async chest exploration map creation: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, generalExecutor).whenComplete((foundStructure, asyncThrowable) -> {
            handleChestExplorationMapResult(stack, context, foundStructure, mapDecoration, zoom, asyncThrowable, cir);
        });
    }

    @Override
    public void handleChestExplorationMapResult(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, Throwable throwable, Object cir) {

        FoliaSchedulerAdapter.runTask(plugin, () -> {
            try {
                if (throwable != null) {
                    System.err.println("[AkiAsync] Chest exploration map creation failed: " + throwable.getMessage());
                    setReturnValue(cir, stack);
                    return;
                }

                if (structurePos != null) {
                    net.minecraft.server.level.ServerLevel level = context.getLevel();
                    net.minecraft.world.item.ItemStack mapStack = net.minecraft.world.item.MapItem.create(
                        level, structurePos.getX(), structurePos.getZ(), zoom, true, true);
                    net.minecraft.world.item.MapItem.renderBiomePreviewMap(level, mapStack);
                    net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(
                        mapStack, structurePos, "+", mapDecoration);

                    setReturnValue(cir, mapStack);

                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Chest exploration map created for structure at " + structurePos);
                    }
                } else {
                    setReturnValue(cir, stack);

                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] No structure found for chest exploration map");
                    }
                }
            } catch (Exception e) {
                System.err.println("[AkiAsync] Error processing chest exploration map result: " + e.getMessage());
                e.printStackTrace();
                setReturnValue(cir, stack);
            }
        });
    }

    @Override
    public void handleVillagerTradeMapAsyncStart(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> destination, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Object cir) {
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) trader.level();
                net.minecraft.core.BlockPos startPos = trader.blockPosition();

                if (config.isStructureLocationDebugEnabled()) {
                    System.out.println("[AkiAsync] Starting async villager trade map creation from " + startPos + " for " + destination.location());
                }

                int searchRadius = config.getVillagerTradeMapsSearchRadius();
                boolean skipKnown = config.isVillagerTradeMapsSkipKnownStructures();

                net.minecraft.core.BlockPos foundStructure = level.findNearestMapStructure(
                    destination, startPos, searchRadius, skipKnown);

                if (config.isStructureLocationDebugEnabled()) {
                    if (foundStructure != null) {
                        System.out.println("[AkiAsync] Villager trade map: structure found at " + foundStructure);
                    } else {
                        System.out.println("[AkiAsync] Villager trade map: no structure found");
                    }
                }

                return foundStructure;
            } catch (Exception e) {
                System.err.println("[AkiAsync] Error in async villager trade map creation: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, generalExecutor).whenComplete((foundStructure, asyncThrowable) -> {
            handleVillagerTradeMapResult(offer, trader, foundStructure, destinationType, displayName, maxUses, villagerXp, asyncThrowable, cir);
        });
    }

    @Override
    public void handleVillagerTradeMapResult(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Throwable throwable, Object cir) {

        FoliaEntityAdapter.safeEntityOperation(plugin, (org.bukkit.entity.Entity) trader.getBukkitEntity(), (entity) -> {
            try {
                if (throwable != null) {
                    System.err.println("[AkiAsync] Villager trade map creation failed: " + throwable.getMessage());
                    setReturnValue(cir, null);
                    return;
                }

                if (structurePos != null) {
                    net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) trader.level();
                    net.minecraft.world.item.ItemStack mapStack = net.minecraft.world.item.MapItem.create(
                        level, structurePos.getX(), structurePos.getZ(), (byte)2, true, true);
                    net.minecraft.world.item.MapItem.renderBiomePreviewMap(level, mapStack);
                    net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(
                        mapStack, structurePos, "+", destinationType);
                    mapStack.set(net.minecraft.core.component.DataComponents.ITEM_NAME,
                        net.minecraft.network.chat.Component.translatable(displayName));

                    net.minecraft.world.item.trading.MerchantOffer newOffer =
                        new net.minecraft.world.item.trading.MerchantOffer(
                            new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD, offer.getCostA().getCount()),
                            java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.COMPASS, 1)),
                            mapStack, 0, maxUses, villagerXp, 0.2F);

                    setReturnValue(cir, newOffer);

                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Villager trade map created for structure at " + structurePos);
                    }
                } else {
                    setReturnValue(cir, null);

                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] No structure found for villager trade map");
                    }
                }
            } catch (Exception e) {
                System.err.println("[AkiAsync] Error processing villager trade map result: " + e.getMessage());
                e.printStackTrace();
                setReturnValue(cir, null);
            }
        });
    }

    @Override
    public int getVillagerTradeMapsSearchRadius() {return config.getVillagerTradeMapsSearchRadius();}

    @Override
    public boolean isVillagerTradeMapsSkipKnownStructures() {return config.isVillagerTradeMapsSkipKnownStructures();}

    @Override
    public boolean isDolphinTreasureSkipKnownStructures() {return config.isLocateCommandSkipKnownStructures();}

    private void setReturnValue(Object cir, Object value) {
        try {
            cir.getClass().getMethod("setReturnValue", Object.class).invoke(cir, value);
        } catch (Exception e) {
            System.err.println("[AkiAsync] Failed to set return value: " + e.getMessage());
        }
    }

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
    public void clearSakuraOptimizationCaches() {
        try {
            org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.clearAllCaches();
            org.virgil.akiasync.mixin.async.redstone.RedstoneWireHelper.clearAllCaches();
            org.virgil.akiasync.mixin.async.explosion.density.AsyncDensityCacheManager.shutdown();
            org.virgil.akiasync.mixin.async.redstone.RedstoneNetworkCache.clearAllCaches();
            org.virgil.akiasync.mixin.async.redstone.AsyncRedstoneNetworkManager.shutdown();
        } catch (Exception e) {
            errorLog("[AkiAsync] Error clearing Sakura caches: %s", e.getMessage());
        }
    }
    
    @Override
    public void clearEntityThrottleCache(int entityId) {
        try {
            org.virgil.akiasync.mixin.network.EntityDataThrottler.clearEntity(entityId);
            org.virgil.akiasync.mixin.network.EntityPacketThrottler.clearEntity(entityId);
        } catch (Exception e) {
            
        }
    }

    @Override
    public java.util.Map<String, Object> getSakuraCacheStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        try {

            java.util.Map<String, String> densityStats = new java.util.HashMap<>();
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                try {
                    net.minecraft.server.level.ServerLevel serverLevel = 
                        ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                    org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache cache = 
                        org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.getOrCreate(serverLevel);
                    densityStats.put(world.getName(), cache.getStats());
                } catch (Exception e) {
                    densityStats.put(world.getName(), "Error: " + e.getMessage());
                }
            }
            stats.put("density_cache", densityStats);
            
            java.util.Map<String, String> asyncStats = new java.util.HashMap<>();
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                try {
                    net.minecraft.server.level.ServerLevel serverLevel = 
                        ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                    org.virgil.akiasync.mixin.async.explosion.density.AsyncDensityCacheManager manager = 
                        org.virgil.akiasync.mixin.async.explosion.density.AsyncDensityCacheManager.getInstance(serverLevel);
                    asyncStats.put(world.getName(), manager.getStats());
                } catch (Exception e) {
                    asyncStats.put(world.getName(), "Error: " + e.getMessage());
                }
            }
            stats.put("async_density_cache", asyncStats);
            
            stats.put("pandawire_evaluators", 
                org.virgil.akiasync.mixin.async.redstone.RedstoneWireHelper.getEvaluatorCount());
            
            java.util.Map<String, String> networkStats = new java.util.HashMap<>();
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                try {
                    net.minecraft.server.level.ServerLevel serverLevel = 
                        ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                    org.virgil.akiasync.mixin.async.redstone.AsyncRedstoneNetworkManager manager = 
                        org.virgil.akiasync.mixin.async.redstone.AsyncRedstoneNetworkManager.getInstance(serverLevel);
                    networkStats.put(world.getName(), manager.getStats());
                } catch (Exception e) {
                    networkStats.put(world.getName(), "Error: " + e.getMessage());
                }
            }
            stats.put("network_cache", networkStats);
            
        } catch (Exception e) {
            errorLog("[AkiAsync] Error getting Sakura cache stats: %s", e.getMessage());
        }
        
        return stats;
    }

    @Override
    public void performSakuraCacheCleanup() {
        try {
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                try {
                    net.minecraft.server.level.ServerLevel serverLevel = 
                        ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                    
                    org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache cache = 
                        org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.getOrCreate(serverLevel);
                    cache.expire(serverLevel.getGameTime());
                    
                    org.virgil.akiasync.mixin.async.explosion.density.AsyncDensityCacheManager manager = 
                        org.virgil.akiasync.mixin.async.explosion.density.AsyncDensityCacheManager.getInstance(serverLevel);
                    manager.expire(serverLevel.getGameTime());
                    
                    org.virgil.akiasync.mixin.async.redstone.AsyncRedstoneNetworkManager networkManager = 
                        org.virgil.akiasync.mixin.async.redstone.AsyncRedstoneNetworkManager.getInstance(serverLevel);
                    networkManager.expire(serverLevel.getGameTime());
                    
                } catch (Exception e) {

                }
            }
        } catch (Exception e) {
            errorLog("[AkiAsync] Error performing Sakura cache cleanup: %s", e.getMessage());
        }
    }

    @Override
    public boolean isVirtualEntity(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        try {
            
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            return org.virgil.akiasync.util.VirtualEntityDetector.isVirtualEntity(bukkitEntity);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isSeedProtectionEnabled() {
        return config != null && config.isSeedProtectionEnabled();
    }

    @Override
    public boolean shouldReturnFakeSeed() {
        return config != null && config.shouldReturnFakeSeed();
    }

    @Override
    public long getFakeSeedValue() {
        return config != null ? config.getFakeSeedValue() : 0L;
    }
    
    @Override
    public boolean isQuantumSeedEnabled() {
        return config != null && config.isQuantumSeedEnabled();
    }
    
    @Override
    public byte[] getQuantumServerKey() {
        org.virgil.akiasync.crypto.QuantumSeedManager manager = plugin.getQuantumSeedManager();
        if (manager == null) {
            return null;
        }

        try {
            java.lang.reflect.Field keyManagerField = manager.getClass().getDeclaredField("keyManager");
            keyManagerField.setAccessible(true);
            org.virgil.akiasync.crypto.ServerKeyManager keyManager = 
                (org.virgil.akiasync.crypto.ServerKeyManager) keyManagerField.get(manager);
            return keyManager.getServerKey();
        } catch (Exception e) {
            plugin.getLogger().warning("[AkiAsync-Bridge] Failed to get server key: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public long getEncryptedSeed(long originalSeed, int chunkX, int chunkZ, String dimension, String generationType, long gameTime) {
        org.virgil.akiasync.crypto.QuantumSeedManager manager = plugin.getQuantumSeedManager();
        if (manager == null) {
            return originalSeed;
        }
        
        org.virgil.akiasync.mixin.crypto.quantum.GenerationType type;
        try {
            type = org.virgil.akiasync.mixin.crypto.quantum.GenerationType.valueOf(generationType.toUpperCase());
        } catch (Exception e) {
            type = org.virgil.akiasync.mixin.crypto.quantum.GenerationType.DECORATION;
        }
        
        return manager.getEncryptedSeed(originalSeed, chunkX, chunkZ, dimension, type, gameTime);
    }
    
    @Override
    public boolean isSecureSeedEnabled() {
        return config != null && config.isSeedEncryptionEnabled() && 
               "secure".equalsIgnoreCase(config.getSeedEncryptionScheme());
    }
    
    @Override
    public long[] getSecureSeedWorldSeed() {
        return org.virgil.akiasync.mixin.crypto.secureseed.crypto.Globals.worldSeed;
    }
    
    @Override
    public void initializeSecureSeed(long originalSeed) {
        if (!isSecureSeedEnabled()) {
            return;
        }
        
        int bits = getSecureSeedBits();
        plugin.getLogger().info("[AkiAsync-SecureSeed] Initializing with " + bits + " bits");
        
        org.virgil.akiasync.mixin.crypto.secureseed.crypto.Globals.initializeWorldSeed(
            originalSeed, 
            bits
        );
        
        plugin.getLogger().info("[AkiAsync-SecureSeed] Seed encryption initialized");
    }
    
    @Override
    public int getSecureSeedBits() {
        return config != null ? config.getSecureSeedBits() : 1024;
    }
    
    @Override
    public boolean isSeedEncryptionProtectStructures() {
        return config != null && config.isSeedEncryptionProtectStructures();
    }
    
    @Override
    public boolean isSeedEncryptionProtectOres() {
        return config != null && config.isSeedEncryptionProtectOres();
    }
    
    @Override
    public boolean isSeedEncryptionProtectSlimes() {
        return config != null && config.isSeedEncryptionProtectSlimes();
    }
    
    @Override
    public boolean isSeedEncryptionProtectBiomes() {
        return config != null && config.isSeedEncryptionProtectBiomes();
    }

    @Override
    public boolean isTNTLandProtectionEnabled() {
        return config != null && config.isTNTLandProtectionEnabled();
    }

    @Override
    public boolean canTNTExplodeAt(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (!isTNTLandProtectionEnabled()) {
            return true;
        }
        
        org.bukkit.World world = level.getWorld();
        return org.virgil.akiasync.util.LandProtectionIntegration.canTNTExplode(world, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Boolean checkChunkProtection(net.minecraft.server.level.ServerLevel level, int chunkX, int chunkZ) {
        if (!isTNTLandProtectionEnabled()) {
            return true; 
        }
        
        org.bukkit.World world = level.getWorld();
        return org.virgil.akiasync.util.LandProtectionIntegration.checkChunkProtection(world, chunkX, chunkZ);
    }
    
    @Override
    public boolean isBlockLockerProtectionEnabled() {
        return config != null && config.isBlockLockerProtectionEnabled();
    }
    
    @Override
    public boolean isBlockLockerProtected(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!isBlockLockerProtectionEnabled()) {
            return false;
        }
        
        org.bukkit.World world = level.getWorld();
        String blockType = getBlockId(state.getBlock());
        return org.virgil.akiasync.util.BlockLockerIntegration.isProtected(world, pos.getX(), pos.getY(), pos.getZ(), blockType);
    }

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
    public void submitChunkLoad(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, int priority, double speed) {
        if (plugin == null || player == null || chunkPos == null) {
            return;
        }
        
        org.virgil.akiasync.chunk.ChunkLoadPriorityScheduler scheduler = plugin.getChunkLoadScheduler();
        if (scheduler != null) {
            
            org.bukkit.entity.Player bukkitPlayer = player.getBukkitEntity();
            org.bukkit.World world = player.level().getWorld();
            scheduler.submitChunkLoad(bukkitPlayer, world, chunkPos.x, chunkPos.z, priority, speed);
        }
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
    public int getMapRenderingThreads() {
        return config != null ? config.getMapRenderingThreads() : 2;
    }
    
    @Override
    public void runOnMainThread(Runnable task) {
        if (task == null) return;
        
        try {
            
            org.virgil.akiasync.compat.FoliaSchedulerAdapter.runTask(plugin, task);
        } catch (Exception e) {
            plugin.getLogger().warning("[AkiAsync] Failed to run task on main thread: " + e.getMessage());
            
            try {
                task.run();
            } catch (Exception ex) {
                
            }
        }
    }
    
    @Override
    public double getCurrentTPS() {
        try {

            return plugin.getServer().getTPS()[0];
        } catch (Exception e) {
            return 20.0;
        }
    }
    
    @Override
    public double getCurrentMSPT() {
        try {

            long[] tickTimes = plugin.getServer().getTickTimes();
            if (tickTimes != null && tickTimes.length > 0) {

                long sum = 0;
                int count = Math.min(100, tickTimes.length);
                for (int i = 0; i < count; i++) {
                    sum += tickTimes[i];
                }
                return (sum / (double) count) / 1_000_000.0;
            }
        } catch (Exception e) {

        }
        return 50.0;
    }
    
    @Override
    public Object getBlockTickSmoothingScheduler() {
        return blockTickScheduler;
    }
    
    @Override
    public Object getEntityTickSmoothingScheduler() {
        return entityTickScheduler;
    }
    
    @Override
    public Object getBlockEntitySmoothingScheduler() {
        return blockEntityScheduler;
    }
    
    @Override
    public boolean submitSmoothTask(Object scheduler, Runnable task, int priority, String category) {
        if (scheduler == null || task == null) return false;
        
        try {
            if (scheduler instanceof TaskSmoothingScheduler smoothScheduler) {
                TaskSmoothingScheduler.Priority pri = switch (priority) {
                    case 0 -> TaskSmoothingScheduler.Priority.CRITICAL;
                    case 1 -> TaskSmoothingScheduler.Priority.HIGH;
                    case 2 -> TaskSmoothingScheduler.Priority.NORMAL;
                    case 3 -> TaskSmoothingScheduler.Priority.LOW;
                    default -> TaskSmoothingScheduler.Priority.NORMAL;
                };
                return smoothScheduler.submit(task, pri, category != null ? category : "Unknown");
            }
        } catch (Exception e) {

        }
        return false;
    }
    
    @Override
    public int submitSmoothTaskBatch(Object scheduler, java.util.List<Runnable> tasks, int priority, String category) {
        if (scheduler == null || tasks == null || tasks.isEmpty()) return 0;
        
        try {
            if (scheduler instanceof TaskSmoothingScheduler smoothScheduler) {
                TaskSmoothingScheduler.Priority pri = switch (priority) {
                    case 0 -> TaskSmoothingScheduler.Priority.CRITICAL;
                    case 1 -> TaskSmoothingScheduler.Priority.HIGH;
                    case 2 -> TaskSmoothingScheduler.Priority.NORMAL;
                    case 3 -> TaskSmoothingScheduler.Priority.LOW;
                    default -> TaskSmoothingScheduler.Priority.NORMAL;
                };
                
                int successCount = 0;
                String cat = category != null ? category : "Unknown";
                
                for (Runnable task : tasks) {
                    if (task != null && smoothScheduler.submit(task, pri, cat)) {
                        successCount++;
                    }
                }
                
                return successCount;
            }
        } catch (Exception e) {

        }
        return 0;
    }
    
    @Override
    public void notifySmoothSchedulerTick(Object scheduler) {
        if (scheduler == null) return;
        
        try {
            if (scheduler instanceof TaskSmoothingScheduler smoothScheduler) {
                smoothScheduler.onTick();
            }
        } catch (Exception e) {

        }
    }
    
    @Override
    public void updateSmoothSchedulerMetrics(Object scheduler, double tps, double mspt) {
        if (scheduler == null) return;
        
        try {
            if (scheduler instanceof TaskSmoothingScheduler smoothScheduler) {
                smoothScheduler.updatePerformanceMetrics(tps, mspt);
            }
        } catch (Exception e) {

        }
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
    public boolean isCollisionCacheEnabled() {
        return config != null ? config.isCollisionCacheEnabled() : true;
    }

    @Override
    public int getCollisionCacheLifetimeMs() {
        return config != null ? config.getCollisionCacheLifetimeMs() : 50;
    }

    @Override
    public double getCollisionCacheMovementThreshold() {
        return config != null ? config.getCollisionCacheMovementThreshold() : 0.01;
    }

    @Override
    public boolean isCollisionSpatialPartitionEnabled() {
        return config != null ? config.isCollisionSpatialPartitionEnabled() : true;
    }

    @Override
    public int getCollisionSpatialGridSize() {
        return config != null ? config.getCollisionSpatialGridSize() : 4;
    }

    @Override
    public int getCollisionSpatialDensityThreshold() {
        return config != null ? config.getCollisionSpatialDensityThreshold() : 50;
    }

    @Override
    public int getCollisionSpatialUpdateIntervalMs() {
        return config != null ? config.getCollisionSpatialUpdateIntervalMs() : 100;
    }

    @Override
    public double getCollisionSkipMinMovement() {
        return config != null ? config.getCollisionSkipMinMovement() : 0.001;
    }

    @Override
    public int getCollisionSkipCheckIntervalMs() {
        return config != null ? config.getCollisionSkipCheckIntervalMs() : 50;
    }
    
    @Override
    public boolean isPushOptimizationEnabled() {
        return config != null ? config.isPushOptimizationEnabled() : true;
    }
    
    @Override
    public double getPushMaxPushPerTick() {
        return config != null ? config.getPushMaxPushPerTick() : 0.5;
    }
    
    @Override
    public double getPushDampingFactor() {
        return config != null ? config.getPushDampingFactor() : 0.7;
    }
    
    @Override
    public int getPushHighDensityThreshold() {
        return config != null ? config.getPushHighDensityThreshold() : 10;
    }
    
    @Override
    public double getPushHighDensityMultiplier() {
        return config != null ? config.getPushHighDensityMultiplier() : 0.3;
    }
    
    @Override
    public boolean isAdvancedCollisionOptimizationEnabled() {
        return config != null ? config.isAdvancedCollisionOptimizationEnabled() : true;
    }
    
    @Override
    public int getCollisionThreshold() {
        return config != null ? config.getCollisionThreshold() : 8;
    }
    
    @Override
    public float getSuffocationDamage() {
        return config != null ? config.getSuffocationDamage() : 0.5f;
    }
    
    @Override
    public int getMaxPushIterations() {
        return config != null ? config.getMaxPushIterations() : 8;
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
    public boolean isNoiseOptimizationEnabled() {
        return config != null && config.isNoiseOptimizationEnabled();
    }

    @Override
    public boolean isJigsawOptimizationEnabled() {
        return config != null && config.isJigsawOptimizationEnabled();
    }

    @Override
    public void initializeJigsawOctree(net.minecraft.world.phys.AABB bounds) {
        if (config != null) {
            org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.set(
                new org.virgil.akiasync.mixin.util.worldgen.BoxOctree(bounds)
            );
        }
    }

    @Override
    public boolean hasJigsawOctree() {
        return org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.isSet();
    }

    @Override
    public void insertIntoJigsawOctree(net.minecraft.world.phys.AABB box) {
        org.virgil.akiasync.mixin.util.worldgen.BoxOctree octree = 
            org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.get();
        if (octree != null) {
            octree.insert(box);
        }
    }

    @Override
    public boolean jigsawOctreeIntersects(net.minecraft.world.phys.AABB box) {
        org.virgil.akiasync.mixin.util.worldgen.BoxOctree octree = 
            org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.get();
        return octree != null && octree.intersects(box);
    }

    @Override
    public void clearJigsawOctree() {
        org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.clear();
    }

    @Override
    public String getJigsawOctreeStats() {
        org.virgil.akiasync.mixin.util.worldgen.BoxOctree octree = 
            org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.get();
        if (octree != null) {
            return octree.getStats().toString();
        }
        return null;
    }
    
    @Override
    public boolean isEntityPacketThrottleEnabled() {
        return config != null && config.isEntityPacketThrottleEnabled();
    }
    
    @Override
    public boolean shouldSendEntityUpdate(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.entity.Entity entity) {
        if (!isEntityPacketThrottleEnabled() || player == null || entity == null) {
            return true;
        }
        
        try {
            if (!org.virgil.akiasync.mixin.network.EntityPacketThrottler.isInitialized()) {
                return true;
            }
            
            return org.virgil.akiasync.mixin.network.EntityPacketThrottler.shouldSendUpdateSimple(player, entity);
        } catch (Exception e) {
            
            return true;
        }
    }
    
    @Override
    public void tickEntityPacketThrottler() {
        if (!isEntityPacketThrottleEnabled()) {
            return;
        }
        
        try {
            if (org.virgil.akiasync.mixin.network.EntityPacketThrottler.isInitialized()) {
                org.virgil.akiasync.mixin.network.EntityPacketThrottler.tick();
            }
        } catch (Exception e) {
            
        }
    }
    
    @Override
    public boolean isEntityDataThrottleEnabled() {
        return config != null && config.isEntityDataThrottleEnabled();
    }
    
    @Override
    public boolean shouldSendMetadata(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.entity.Entity entity, int metadataHash) {
        if (!isEntityDataThrottleEnabled() || player == null || entity == null) {
            return true;
        }
        
        try {
            return org.virgil.akiasync.mixin.network.EntityDataThrottler.shouldSendMetadata(player, entity, metadataHash);
        } catch (Exception e) {
            
            return true;
        }
    }
    
    @Override
    public boolean shouldSendNBT(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.entity.Entity entity, boolean forceUpdate) {
        if (!isEntityDataThrottleEnabled() || player == null || entity == null) {
            return true;
        }
        
        try {
            return org.virgil.akiasync.mixin.network.EntityDataThrottler.shouldSendNBT(player, entity, forceUpdate);
        } catch (Exception e) {
            
            return true;
        }
    }
    
    @Override
    public void tickEntityDataThrottler() {
        if (!isEntityDataThrottleEnabled()) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.network.EntityDataThrottler.tick();
        } catch (Exception e) {
            
        }
    }
    
    @Override
    public boolean isChunkVisibilityFilterEnabled() {
        return config != null && config.isChunkVisibilityFilterEnabled();
    }
    
    @Override
    public boolean isChunkVisible(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, net.minecraft.server.level.ServerLevel level) {
        if (!isChunkVisibilityFilterEnabled() || player == null || chunkPos == null || level == null) {
            return true;
        }
        
        try {
            return org.virgil.akiasync.mixin.network.ChunkVisibilityFilter.isChunkVisible(player, chunkPos, level);
        } catch (Exception e) {
            
            return true;
        }
    }
    
    @Override
    public void tickChunkVisibilityFilter() {
        if (!isChunkVisibilityFilterEnabled()) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.network.ChunkVisibilityFilter.tick();
        } catch (Exception e) {
            
        }
    }
    
    @Override
    public void clearWorldCaches(String worldName) {
        try {
            
            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("[Bridge] World not found: " + worldName);
                return;
            }
            
            
            net.minecraft.server.level.ServerLevel level = 
                ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
            
            
            org.virgil.akiasync.mixin.brain.core.AiSpatialIndexManager.removeIndex(level);
            org.virgil.akiasync.mixin.poi.PoiSpatialIndexManager.removeIndex(level);
            org.virgil.akiasync.mixin.poi.BatchPoiManager.clearLevelCache(level);
            org.virgil.akiasync.mixin.util.EntitySliceGridManager.clearSliceGrid(level);
            org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.clearLevelCache(level);
            org.virgil.akiasync.mixin.async.redstone.RedstoneNetworkCache.clearLevelCache(level);
            org.virgil.akiasync.mixin.async.redstone.RedstoneWireHelper.clearLevelCache(level);
            org.virgil.akiasync.mixin.async.redstone.AsyncRedstoneNetworkManager.clearLevelCache(level);
            org.virgil.akiasync.mixin.async.redstone.PandaWireEvaluator.clearLevelCache(level);
            org.virgil.akiasync.mixin.async.explosion.TNTBatchCollector.clearLevelCache(level);
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to clear world caches: " + e.getMessage());
        }
    }
    
    @Override
    public void prewarmPlayerPaths(java.util.UUID playerId) {
        try {
            
            org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerId);
            if (player == null) {
                plugin.getLogger().warning("[Bridge] Player not found: " + playerId);
                return;
            }
            
            
            net.minecraft.server.level.ServerPlayer serverPlayer = 
                ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle();
            
            
            org.virgil.akiasync.mixin.pathfinding.EnhancedPathfindingSystem.prewarmPlayerPathsMainThread(serverPlayer);
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to prewarm player paths: " + e.getMessage());
        }
    }
    
    @Override
    public void cleanupPlayerPaths(java.util.UUID playerId) {
        try {
            
            org.virgil.akiasync.mixin.pathfinding.EnhancedPathfindingSystem.cleanupPlayer(playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to cleanup player paths: " + e.getMessage());
        }
    }
    
    @Override
    public void restartVillagerExecutor() {
        try {
            org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor.restartSmooth();
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to restart villager executor: " + e.getMessage());
        }
    }
    
    @Override
    public void restartTNTExecutor() {
        try {
            org.virgil.akiasync.mixin.async.TNTThreadPool.restartSmooth();
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to restart TNT executor: " + e.getMessage());
        }
    }
    
    @Override
    public void restartBrainExecutor() {
        try {
            org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor.restartSmooth();
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to restart brain executor: " + e.getMessage());
        }
    }
    
    @Override
    public void restartChunkExecutor(int threadCount) {
        try {
            org.virgil.akiasync.mixin.async.chunk.ChunkTickExecutor.setThreadCount(threadCount);
            org.virgil.akiasync.mixin.async.chunk.ChunkTickExecutor.restartSmooth();
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to restart chunk executor: " + e.getMessage());
        }
    }
    
    @Override
    public void clearVillagerBreedCache() {
        try {
            org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor.clearOldCache(Long.MAX_VALUE);
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to clear villager breed cache: " + e.getMessage());
        }
    }
    
    @Override
    public void resetBrainExecutorStatistics() {
        try {
            org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor.resetStatistics();
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to reset brain executor statistics: " + e.getMessage());
        }
    }
    
    @Override
    public void resetAsyncMetrics() {
        try {
            org.virgil.akiasync.mixin.metrics.AsyncMetrics.reset();
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to reset async metrics: " + e.getMessage());
        }
    }
}
