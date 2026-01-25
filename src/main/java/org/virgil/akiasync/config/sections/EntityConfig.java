package org.virgil.akiasync.config.sections;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;

public class EntityConfig {

    private boolean multithreadedTrackerEnabled;
    private int multithreadedTrackerParallelism;
    private int multithreadedTrackerBatchSize;
    private int multithreadedTrackerAssistBatchSize;

    private boolean mobSpawningEnabled;
    private boolean spawnerOptimizationEnabled;
    private int mobSpawnInterval;
    private boolean densityControlEnabled;
    private int maxEntitiesPerChunk;
    private boolean brainThrottle;
    private int brainThrottleInterval;
    private boolean livingEntityTravelOptimizationEnabled;
    private int livingEntityTravelSkipInterval;
    private boolean mobDespawnOptimizationEnabled;
    private int mobDespawnCheckInterval;

    private boolean endermanBlockCarryLimiterEnabled;
    private int endermanMaxCarrying;
    private boolean endermanCountTowardsMobCap;
    private boolean endermanPreventPickup;

    private boolean entityThrottlingEnabled;
    private String entityThrottlingConfigFile;
    private int entityThrottlingCheckInterval;
    private int entityThrottlingThrottleInterval;
    private int entityThrottlingRemovalBatchSize;

    private boolean entityTickParallel;
    private int minEntitiesForParallel;
    private int entityTickBatchSize;

    private boolean entityTrackingRangeOptimizationEnabled;
    private double entityTrackingRangeMultiplier;

    private boolean mobSunBurnOptimizationEnabled;
    private boolean entitySpeedOptimizationEnabled;
    private boolean entityFallDamageOptimizationEnabled;
    private boolean entitySectionStorageOptimizationEnabled;

    private boolean fallingBlockParallelEnabled;
    private int minFallingBlocksForParallel;
    private int fallingBlockBatchSize;

    private boolean itemEntityMergeOptimizationEnabled;
    private boolean itemEntityCancelVanillaMerge;
    private int itemEntityMergeInterval;
    private int itemEntityMinNearbyItems;
    private double itemEntityMergeRange;
    private boolean itemEntityAgeOptimizationEnabled;
    private int itemEntityAgeInterval;
    private double itemEntityPlayerDetectionRange;
    private boolean itemEntityInactiveTickEnabled;
    private double itemEntityInactiveRange;
    private int itemEntityInactiveMergeInterval;

    private boolean experienceOrbInactiveTickEnabled;
    private double experienceOrbInactiveRange;
    private int experienceOrbInactiveMergeInterval;
    private boolean experienceOrbMergeEnabled;
    private int experienceOrbMergeInterval;

    private boolean projectileOptimizationEnabled;
    private int maxProjectileLoadsPerTick;
    private int maxProjectileLoadsPerProjectile;

    private boolean beeFixEnabled;
    private boolean endIslandDensityFixEnabled;
    private boolean leaderZombieHealthFixEnabled;
    private boolean portalSuffocationCheckDisabled;
    private boolean shulkerBulletSelfHitFixEnabled;

    private boolean hopperOptimizationEnabled;
    private int hopperCacheExpireTime;
    private boolean minecartOptimizationEnabled;
    private int minecartTickInterval;
    private boolean minecartCauldronDestructionEnabled;

    private boolean asyncVillagerBreedEnabled;
    private boolean villagerAgeThrottleEnabled;
    private int villagerBreedCheckInterval;

    private boolean suffocationOptimizationEnabled;
    private boolean fastRayTraceEnabled;

    public void load(FileConfiguration config, int cpuCores) {
        multithreadedTrackerEnabled = config.getBoolean("entity-tracker.enabled", true);
        multithreadedTrackerParallelism = config.getInt("entity-tracker.parallelism", Math.max(4, cpuCores));
        multithreadedTrackerBatchSize = config.getInt("entity-tracker.batch-size", 10);
        multithreadedTrackerAssistBatchSize = config.getInt("entity-tracker.assist-batch-size", 5);

        mobSpawningEnabled = config.getBoolean("mob-spawning.enabled", true);
        spawnerOptimizationEnabled = config.getBoolean("mob-spawning.spawner-optimization", true);
        mobSpawnInterval = config.getInt("mob-spawning.spawn-interval", 1);
        densityControlEnabled = config.getBoolean("density.enabled", true);
        maxEntitiesPerChunk = config.getInt("density.max-per-chunk", 80);
        brainThrottle = config.getBoolean("brain.enabled", true);
        brainThrottleInterval = config.getInt("brain.throttle-interval", 10);
        livingEntityTravelOptimizationEnabled = config.getBoolean("living-entity-travel-optimization.enabled", true);
        livingEntityTravelSkipInterval = config.getInt("living-entity-travel-optimization.skip-interval", 2);
        mobDespawnOptimizationEnabled = config.getBoolean("mob-despawn-optimization.enabled", true);
        mobDespawnCheckInterval = config.getInt("mob-despawn-optimization.check-interval", 20);

        endermanBlockCarryLimiterEnabled = config.getBoolean("enderman-block-carry-limiter.enabled", true);
        endermanMaxCarrying = config.getInt("enderman-block-carry-limiter.max-carrying-endermen", 50);
        endermanCountTowardsMobCap = config.getBoolean("enderman-block-carry-limiter.count-towards-mob-cap", true);
        endermanPreventPickup = config.getBoolean("enderman-block-carry-limiter.prevent-pickup-when-limit-reached", true);

        entityThrottlingEnabled = config.getBoolean("entity-throttling.enabled", true);
        entityThrottlingConfigFile = config.getString("entity-throttling.config-file", "throttling.yml");
        entityThrottlingCheckInterval = config.getInt("entity-throttling.check-interval", 100);
        entityThrottlingThrottleInterval = config.getInt("entity-throttling.throttle-interval", 3);
        entityThrottlingRemovalBatchSize = config.getInt("entity-throttling.removal-batch-size", 10);

        entityTickParallel = config.getBoolean("entity-tick-parallel.enabled", true);
        minEntitiesForParallel = config.getInt("entity-tick-parallel.min-entities", 50);
        entityTickBatchSize = config.getInt("entity-tick-parallel.batch-size", 8);

        entityTrackingRangeOptimizationEnabled = config.getBoolean("vmp-optimizations.entity-tracking.enabled", true);
        entityTrackingRangeMultiplier = config.getDouble("vmp-optimizations.entity-tracking.range-multiplier", 0.8);

        mobSunBurnOptimizationEnabled = config.getBoolean("collision-optimization.nitori-entity-optimizations.mob-sunburn-optimization.enabled", true);
        entitySpeedOptimizationEnabled = config.getBoolean("collision-optimization.nitori-entity-optimizations.entity-speed-optimization.enabled", true);
        entityFallDamageOptimizationEnabled = config.getBoolean("collision-optimization.nitori-entity-optimizations.entity-fall-damage-optimization.enabled", true);
        entitySectionStorageOptimizationEnabled = config.getBoolean("collision-optimization.nitori-entity-optimizations.entity-section-storage-optimization.enabled", true);

        fallingBlockParallelEnabled = config.getBoolean("falling-block-optimization.enabled", true);
        minFallingBlocksForParallel = config.getInt("falling-block-optimization.min-falling-blocks", 20);
        fallingBlockBatchSize = config.getInt("falling-block-optimization.batch-size", 10);

        itemEntityMergeOptimizationEnabled = config.getBoolean("item-entity-optimizations.smart-merge.enabled", true);
        itemEntityCancelVanillaMerge = config.getBoolean("item-entity-optimizations.smart-merge.cancel-vanilla-merge", true);
        itemEntityMergeInterval = config.getInt("item-entity-optimizations.smart-merge.merge-interval", 5);
        itemEntityMinNearbyItems = config.getInt("item-entity-optimizations.smart-merge.min-nearby-items", 3);
        itemEntityMergeRange = config.getDouble("item-entity-optimizations.smart-merge.merge-range", 1.5);
        itemEntityAgeOptimizationEnabled = config.getBoolean("item-entity-optimizations.age-optimization.enabled", true);
        itemEntityAgeInterval = config.getInt("item-entity-optimizations.age-optimization.age-interval", 10);
        itemEntityPlayerDetectionRange = config.getDouble("item-entity-optimizations.age-optimization.player-detection-range", 8.0);
        itemEntityInactiveTickEnabled = config.getBoolean("item-entity-optimizations.inactive-tick.enabled", true);
        itemEntityInactiveRange = config.getDouble("item-entity-optimizations.inactive-tick.inactive-range", 32.0);
        itemEntityInactiveMergeInterval = config.getInt("item-entity-optimizations.inactive-tick.merge-interval", 100);

        experienceOrbInactiveTickEnabled = config.getBoolean("experience-orb-optimizations.inactive-tick.enabled", true);
        experienceOrbInactiveRange = config.getDouble("experience-orb-optimizations.inactive-tick.inactive-range", 32.0);
        experienceOrbInactiveMergeInterval = config.getInt("experience-orb-optimizations.inactive-tick.merge-interval", 100);
        experienceOrbMergeEnabled = config.getBoolean("experience-orb-optimizations.merge.enabled", false);
        experienceOrbMergeInterval = config.getInt("experience-orb-optimizations.merge.interval", 20);

        projectileOptimizationEnabled = config.getBoolean("projectile.enabled", true);
        maxProjectileLoadsPerTick = config.getInt("projectile.max-loads-per-tick", 10);
        maxProjectileLoadsPerProjectile = config.getInt("projectile.max-loads-per-projectile", 10);

        beeFixEnabled = config.getBoolean("bee-fix.enabled", true);
        endIslandDensityFixEnabled = config.getBoolean("end-island-density-fix.enabled", true);
        leaderZombieHealthFixEnabled = config.getBoolean("leader-zombie-health-fix.enabled", true);
        portalSuffocationCheckDisabled = config.getBoolean("portal-suffocation-check.disabled", false);
        shulkerBulletSelfHitFixEnabled = config.getBoolean("shulker-bullet-self-hit-fix.enabled", true);

        hopperOptimizationEnabled = config.getBoolean("hopper-optimization.enabled", true);
        hopperCacheExpireTime = config.getInt("hopper-optimization.cache-expire-time", 20);
        minecartOptimizationEnabled = config.getBoolean("minecart-optimization.enabled", true);
        minecartTickInterval = config.getInt("minecart-optimization.tick-interval", 2);
        minecartCauldronDestructionEnabled = config.getBoolean("minecart-optimization.cauldron-destruction", true);

        asyncVillagerBreedEnabled = config.getBoolean("villager-optimization.async-breed.enabled", true);
        villagerAgeThrottleEnabled = config.getBoolean("villager-optimization.age-throttle.enabled", true);
        villagerBreedCheckInterval = config.getInt("villager-optimization.breed-check-interval", 100);

        suffocationOptimizationEnabled = config.getBoolean("suffocation-optimization.enabled", true);
        fastRayTraceEnabled = config.getBoolean("suffocation-optimization.fast-ray-trace", true);
    }

    public void validate(java.util.logging.Logger logger) {
        if (maxEntitiesPerChunk < 20) {
            maxEntitiesPerChunk = 20;
        }
        if (brainThrottleInterval < 0) {
            brainThrottleInterval = 0;
        }
        if (minEntitiesForParallel < 10) {
            minEntitiesForParallel = 10;
        }
        if (entityTrackingRangeMultiplier < 0.1) {
            entityTrackingRangeMultiplier = 0.1;
        }
        if (entityTrackingRangeMultiplier > 2.0) {
            entityTrackingRangeMultiplier = 2.0;
        }
    }

    public boolean isMultithreadedTrackerEnabled() { return multithreadedTrackerEnabled; }
    public int getMultithreadedTrackerParallelism() { return multithreadedTrackerParallelism; }
    public int getMultithreadedTrackerBatchSize() { return multithreadedTrackerBatchSize; }
    public int getMultithreadedTrackerAssistBatchSize() { return multithreadedTrackerAssistBatchSize; }

    public boolean isMobSpawningEnabled() { return mobSpawningEnabled; }
    public boolean isSpawnerOptimizationEnabled() { return spawnerOptimizationEnabled; }
    public int getMobSpawnInterval() { return mobSpawnInterval; }
    public boolean isDensityControlEnabled() { return densityControlEnabled; }
    public int getMaxEntitiesPerChunk() { return maxEntitiesPerChunk; }
    public boolean isBrainThrottleEnabled() { return brainThrottle; }
    public int getBrainThrottleInterval() { return brainThrottleInterval; }
    public boolean isLivingEntityTravelOptimizationEnabled() { return livingEntityTravelOptimizationEnabled; }
    public int getLivingEntityTravelSkipInterval() { return livingEntityTravelSkipInterval; }
    public boolean isMobDespawnOptimizationEnabled() { return mobDespawnOptimizationEnabled; }
    public int getMobDespawnCheckInterval() { return mobDespawnCheckInterval; }

    public boolean isEndermanBlockCarryLimiterEnabled() { return endermanBlockCarryLimiterEnabled; }
    public int getEndermanMaxCarrying() { return endermanMaxCarrying; }
    public boolean isEndermanCountTowardsMobCap() { return endermanCountTowardsMobCap; }
    public boolean isEndermanPreventPickup() { return endermanPreventPickup; }

    public boolean isEntityThrottlingEnabled() { return entityThrottlingEnabled; }
    public String getEntityThrottlingConfigFile() { return entityThrottlingConfigFile; }
    public int getEntityThrottlingCheckInterval() { return entityThrottlingCheckInterval; }
    public int getEntityThrottlingThrottleInterval() { return entityThrottlingThrottleInterval; }
    public int getEntityThrottlingRemovalBatchSize() { return entityThrottlingRemovalBatchSize; }

    public boolean isEntityTickParallel() { return entityTickParallel; }
    public int getMinEntitiesForParallel() { return minEntitiesForParallel; }
    public int getEntityTickBatchSize() { return entityTickBatchSize; }

    public boolean isEntityTrackingRangeOptimizationEnabled() { return entityTrackingRangeOptimizationEnabled; }
    public double getEntityTrackingRangeMultiplier() { return entityTrackingRangeMultiplier; }

    public boolean isMobSunBurnOptimizationEnabled() { return mobSunBurnOptimizationEnabled; }
    public boolean isEntitySpeedOptimizationEnabled() { return entitySpeedOptimizationEnabled; }
    public boolean isEntityFallDamageOptimizationEnabled() { return entityFallDamageOptimizationEnabled; }
    public boolean isEntitySectionStorageOptimizationEnabled() { return entitySectionStorageOptimizationEnabled; }

    public boolean isFallingBlockParallelEnabled() { return fallingBlockParallelEnabled; }
    public int getMinFallingBlocksForParallel() { return minFallingBlocksForParallel; }
    public int getFallingBlockBatchSize() { return fallingBlockBatchSize; }

    public boolean isItemEntityMergeOptimizationEnabled() { return itemEntityMergeOptimizationEnabled; }
    public boolean isItemEntityCancelVanillaMerge() { return itemEntityCancelVanillaMerge; }
    public int getItemEntityMergeInterval() { return itemEntityMergeInterval; }
    public int getItemEntityMinNearbyItems() { return itemEntityMinNearbyItems; }
    public double getItemEntityMergeRange() { return itemEntityMergeRange; }
    public boolean isItemEntityAgeOptimizationEnabled() { return itemEntityAgeOptimizationEnabled; }
    public int getItemEntityAgeInterval() { return itemEntityAgeInterval; }
    public double getItemEntityPlayerDetectionRange() { return itemEntityPlayerDetectionRange; }
    public boolean isItemEntityInactiveTickEnabled() { return itemEntityInactiveTickEnabled; }
    public double getItemEntityInactiveRange() { return itemEntityInactiveRange; }
    public int getItemEntityInactiveMergeInterval() { return itemEntityInactiveMergeInterval; }

    public boolean isExperienceOrbInactiveTickEnabled() { return experienceOrbInactiveTickEnabled; }
    public double getExperienceOrbInactiveRange() { return experienceOrbInactiveRange; }
    public int getExperienceOrbInactiveMergeInterval() { return experienceOrbInactiveMergeInterval; }
    public boolean isExperienceOrbMergeEnabled() { return experienceOrbMergeEnabled; }
    public int getExperienceOrbMergeInterval() { return experienceOrbMergeInterval; }

    public boolean isProjectileOptimizationEnabled() { return projectileOptimizationEnabled; }
    public int getMaxProjectileLoadsPerTick() { return maxProjectileLoadsPerTick; }
    public int getMaxProjectileLoadsPerProjectile() { return maxProjectileLoadsPerProjectile; }

    public boolean isBeeFixEnabled() { return beeFixEnabled; }
    public boolean isEndIslandDensityFixEnabled() { return endIslandDensityFixEnabled; }
    public boolean isLeaderZombieHealthFixEnabled() { return leaderZombieHealthFixEnabled; }
    public boolean isPortalSuffocationCheckDisabled() { return portalSuffocationCheckDisabled; }
    public boolean isShulkerBulletSelfHitFixEnabled() { return shulkerBulletSelfHitFixEnabled; }

    public boolean isHopperOptimizationEnabled() { return hopperOptimizationEnabled; }
    public int getHopperCacheExpireTime() { return hopperCacheExpireTime; }
    public boolean isMinecartOptimizationEnabled() { return minecartOptimizationEnabled; }
    public int getMinecartTickInterval() { return minecartTickInterval; }
    public boolean isMinecartCauldronDestructionEnabled() { return minecartCauldronDestructionEnabled; }

    public boolean isAsyncVillagerBreedEnabled() { return asyncVillagerBreedEnabled; }
    public boolean isVillagerAgeThrottleEnabled() { return villagerAgeThrottleEnabled; }
    public int getVillagerBreedCheckInterval() { return villagerBreedCheckInterval; }

    public boolean isSuffocationOptimizationEnabled() { return suffocationOptimizationEnabled; }
    public boolean isFastRayTraceEnabled() { return fastRayTraceEnabled; }
}
