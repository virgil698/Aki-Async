package org.virgil.akiasync.mixin.bridge.sub;

public interface EntityBridge {

    boolean isMobSunBurnOptimizationEnabled();
    boolean isEntitySpeedOptimizationEnabled();
    boolean isEntityFallDamageOptimizationEnabled();
    boolean isEntitySectionStorageOptimizationEnabled();

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

    boolean shouldThrottleEntity(Object entity);
    boolean isEntityThrottlingEnabled();

    boolean isMobSpawningEnabled();
    boolean isDensityControlEnabled();
    int getMaxEntitiesPerChunk();
    int getMobSpawnInterval();
    boolean isSpawnerOptimizationEnabled();

    boolean isEntityLookupCacheEnabled();
    int getEntityLookupCacheDurationMs();

    boolean isVirtualEntity(net.minecraft.world.entity.Entity entity);

    boolean isAsyncVillagerBreedEnabled();
    boolean isVillagerAgeThrottleEnabled();
    int getVillagerBreedThreads();
    int getVillagerBreedCheckInterval();

    boolean isBeeFixEnabled();
    boolean isEndIslandDensityFixEnabled();
    boolean isLeaderZombieHealthFixEnabled();
    boolean isEquipmentHealthCapFixEnabled();

    boolean isEndermanBlockCarryLimiterEnabled();
    int getEndermanMaxCarrying();
    boolean isEndermanCountTowardsMobCap();
    boolean isEndermanPreventPickup();

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

    boolean isExperienceOrbInactiveTickEnabled();
    double getExperienceOrbInactiveRange();
    int getExperienceOrbInactiveMergeInterval();
    boolean isExperienceOrbMergeEnabled();
    int getExperienceOrbMergeInterval();

    boolean isProjectileOptimizationEnabled();
    int getMaxProjectileLoadsPerTick();
    int getMaxProjectileLoadsPerProjectile();

    boolean isEntityMoveZeroVelocityOptimizationEnabled();

    void clearEntityThrottleCache(int entityId);
}
