package org.virgil.akiasync.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.virgil.akiasync.AkiAsyncPlugin;

public class ConfigManager {
    
    private final AkiAsyncPlugin plugin;
    private FileConfiguration config;
    private boolean entityTrackerEnabled;
    private int threadPoolSize;
    private int updateIntervalTicks;
    private int maxQueueSize;
    private boolean mobSpawningEnabled;
    private boolean spawnerOptimizationEnabled;
    private int maxEntitiesPerChunk;
    private int aiCooldownTicks;
    private boolean brainThrottle;
    private int brainThrottleInterval;
    private long asyncAITimeoutMicros;
    private boolean villagerOptimizationEnabled;
    private boolean villagerUsePOISnapshot;
    private boolean piglinOptimizationEnabled;
    private boolean piglinUsePOISnapshot;
    private int piglinLookDistance;
    private int piglinBarterDistance;
    private boolean pillagerFamilyOptimizationEnabled;
    private boolean pillagerFamilyUsePOISnapshot;
    private boolean evokerOptimizationEnabled;
    private boolean blazeOptimizationEnabled;
    private boolean guardianOptimizationEnabled;
    private boolean witchOptimizationEnabled;
    private boolean universalAiOptimizationEnabled;
    private java.util.Set<String> universalAiEntities;
    private boolean zeroDelayFactoryOptimizationEnabled;
    private java.util.Set<String> zeroDelayFactoryEntities;
    private boolean itemEntityOptimizationEnabled;
    private int itemEntityAgeInterval;
    private int itemEntityMinNearbyItems;
    private boolean simpleEntitiesOptimizationEnabled;
    private boolean simpleEntitiesUsePOISnapshot;
    private boolean entityTickParallel;
    private int entityTickThreads;
    private int minEntitiesForParallel;
    private int entityTickBatchSize;
    private boolean asyncLightingEnabled;
    private int lightingThreadPoolSize;
    private int lightBatchThreshold;
    private boolean useLayeredPropagationQueue;
    private int maxLightPropagationDistance;
    private boolean skylightCacheEnabled;
    private int skylightCacheDurationMs;
    private boolean lightDeduplicationEnabled;
    private boolean dynamicBatchAdjustmentEnabled;
    private boolean advancedLightingStatsEnabled;
    private boolean playerChunkLoadingOptimizationEnabled;
    private int maxConcurrentChunkLoadsPerPlayer;
    private boolean entityTrackingRangeOptimizationEnabled;
    private double entityTrackingRangeMultiplier;
    private boolean alternateCurrentEnabled;
    private boolean redstoneWireTurboEnabled;
    private boolean redstoneUpdateBatchingEnabled;
    private int redstoneUpdateBatchThreshold;
    private boolean redstoneCacheEnabled;
    private int redstoneCacheDurationMs;
    private boolean tntOptimizationEnabled;
    private java.util.Set<String> tntExplosionEntities;
    private int tntThreads;
    private int tntMaxBlocks;
    private long tntTimeoutMicros;
    private int tntBatchSize;
    private boolean tntDebugEnabled;
    private boolean asyncVillagerBreedEnabled;
    private boolean villagerAgeThrottleEnabled;
    private int villagerBreedThreads;
    private int villagerBreedCheckInterval;
    private boolean chunkTickAsyncEnabled;
    private int chunkTickThreads;
    private long chunkTickTimeoutMicros;
    private boolean enableDebugLogging;
    private boolean enablePerformanceMetrics;
    private int configVersion;
    
    public ConfigManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        entityTrackerEnabled = config.getBoolean("entity-tracker.enabled", true);
        threadPoolSize = config.getInt("entity-tracker.thread-pool-size", 4);
        updateIntervalTicks = config.getInt("entity-tracker.update-interval-ticks", 1);
        maxQueueSize = config.getInt("entity-tracker.max-queue-size", 1000);
        mobSpawningEnabled = config.getBoolean("mob-spawning.enabled", true);
        spawnerOptimizationEnabled = config.getBoolean("mob-spawning.spawner-optimization", true);
        maxEntitiesPerChunk = config.getInt("density.max-per-chunk", 80);
        aiCooldownTicks = config.getInt("density.ai-cooldown-ticks", 10);
        brainThrottle = config.getBoolean("brain.throttle", true);
        brainThrottleInterval = config.getInt("brain.throttle-interval", 10);
        asyncAITimeoutMicros = config.getLong("async-ai.timeout-microseconds", 500L);
        villagerOptimizationEnabled = config.getBoolean("async-ai.villager-optimization.enabled", false);
        villagerUsePOISnapshot = config.getBoolean("async-ai.villager-optimization.use-poi-snapshot", true);
        piglinOptimizationEnabled = config.getBoolean("async-ai.piglin-optimization.enabled", false);
        piglinUsePOISnapshot = config.getBoolean("async-ai.piglin-optimization.use-poi-snapshot", false);
        piglinLookDistance = config.getInt("async-ai.piglin-optimization.look-distance", 16);
        piglinBarterDistance = config.getInt("async-ai.piglin-optimization.barter-distance", 16);
        pillagerFamilyOptimizationEnabled = config.getBoolean("async-ai.pillager-family-optimization.enabled", false);
        pillagerFamilyUsePOISnapshot = config.getBoolean("async-ai.pillager-family-optimization.use-poi-snapshot", false);
        evokerOptimizationEnabled = config.getBoolean("async-ai.evoker-optimization.enabled", false);
        blazeOptimizationEnabled = config.getBoolean("async-ai.blaze-optimization.enabled", false);
        guardianOptimizationEnabled = config.getBoolean("async-ai.guardian-optimization.enabled", false);
        witchOptimizationEnabled = config.getBoolean("async-ai.witch-optimization.enabled", false);
        universalAiOptimizationEnabled = config.getBoolean("async-ai.universal-ai-optimization.enabled", false);
        universalAiEntities = new java.util.HashSet<>(config.getStringList("async-ai.universal-ai-optimization.entities"));
        zeroDelayFactoryOptimizationEnabled = config.getBoolean("block-entity-optimizations.zero-delay-factory-optimization.enabled", false);
        zeroDelayFactoryEntities = new java.util.HashSet<>(config.getStringList("block-entity-optimizations.zero-delay-factory-optimization.entities"));
        itemEntityOptimizationEnabled = config.getBoolean("item-entity-optimizations.enabled", true);
        itemEntityAgeInterval = config.getInt("item-entity-optimizations.age-increment-interval", 10);
        itemEntityMinNearbyItems = config.getInt("item-entity-optimizations.min-nearby-items", 3);
        simpleEntitiesOptimizationEnabled = config.getBoolean("async-ai.simple-entities.enabled", false);
        simpleEntitiesUsePOISnapshot = config.getBoolean("async-ai.simple-entities.use-poi-snapshot", false);
        entityTickParallel = config.getBoolean("entity-tick-parallel.enabled", true);
        entityTickThreads = config.getInt("entity-tick-parallel.threads", 4);
        minEntitiesForParallel = config.getInt("entity-tick-parallel.min-entities", 100);
        entityTickBatchSize = config.getInt("entity-tick-parallel.batch-size", 8);
        asyncLightingEnabled = config.getBoolean("lighting-optimizations.async-lighting.enabled", true);
        lightingThreadPoolSize = config.getInt("lighting-optimizations.async-lighting.thread-pool-size", 2);
        lightBatchThreshold = config.getInt("lighting-optimizations.async-lighting.batch-threshold", 16);
        useLayeredPropagationQueue = config.getBoolean("lighting-optimizations.propagation-queue.use-layered-queue", true);
        maxLightPropagationDistance = config.getInt("lighting-optimizations.propagation-queue.max-propagation-distance", 15);
        skylightCacheEnabled = config.getBoolean("lighting-optimizations.skylight-cache.enabled", true);
        skylightCacheDurationMs = config.getInt("lighting-optimizations.skylight-cache.cache-duration-ms", 100);
        lightDeduplicationEnabled = config.getBoolean("lighting-optimizations.advanced.enable-deduplication", true);
        dynamicBatchAdjustmentEnabled = config.getBoolean("lighting-optimizations.advanced.dynamic-batch-adjustment", true);
        advancedLightingStatsEnabled = config.getBoolean("lighting-optimizations.advanced.log-advanced-stats", false);
        playerChunkLoadingOptimizationEnabled = config.getBoolean("vmp-optimizations.chunk-loading.enabled", true);
        maxConcurrentChunkLoadsPerPlayer = config.getInt("vmp-optimizations.chunk-loading.max-concurrent-per-player", 5);
        entityTrackingRangeOptimizationEnabled = config.getBoolean("vmp-optimizations.entity-tracking.enabled", true);
        entityTrackingRangeMultiplier = config.getDouble("vmp-optimizations.entity-tracking.range-multiplier", 0.8);
        alternateCurrentEnabled = config.getBoolean("redstone-optimizations.alternate-current.enabled", true);
        redstoneWireTurboEnabled = config.getBoolean("redstone-optimizations.wire-turbo.enabled", true);
        redstoneUpdateBatchingEnabled = config.getBoolean("redstone-optimizations.update-batching.enabled", true);
        redstoneUpdateBatchThreshold = config.getInt("redstone-optimizations.update-batching.batch-threshold", 8);
        redstoneCacheEnabled = config.getBoolean("redstone-optimizations.cache.enabled", true);
        redstoneCacheDurationMs = config.getInt("redstone-optimizations.cache.duration-ms", 50);
        asyncVillagerBreedEnabled = config.getBoolean("villager-breed-optimization.async-villager-breed", true);
        villagerAgeThrottleEnabled = config.getBoolean("villager-breed-optimization.age-throttle", true);
        villagerBreedThreads = config.getInt("villager-breed-optimization.threads", 4);
        villagerBreedCheckInterval = config.getInt("villager-breed-optimization.check-interval", 5);
        chunkTickAsyncEnabled = config.getBoolean("chunk-tick-async.enabled", false);
        chunkTickThreads = config.getInt("chunk-tick-async.threads", 4);
        chunkTickTimeoutMicros = config.getLong("chunk-tick-async.timeout-us", 200L);
        tntOptimizationEnabled = config.getBoolean("tnt-explosion-optimization.enabled", true);
        tntExplosionEntities = new java.util.HashSet<>(config.getStringList("tnt-explosion-optimization.entities"));
        if (tntExplosionEntities.isEmpty()) {
            tntExplosionEntities.add("minecraft:tnt");
            tntExplosionEntities.add("minecraft:tnt_minecart");
            tntExplosionEntities.add("minecraft:wither_skull");
        }
        tntThreads = config.getInt("tnt-explosion-optimization.threads", 6);
        tntMaxBlocks = config.getInt("tnt-explosion-optimization.max-blocks", 4096);
        tntTimeoutMicros = config.getLong("tnt-explosion-optimization.timeout-us", 100L);
        tntBatchSize = config.getInt("tnt-explosion-optimization.batch-size", 64);
        tntDebugEnabled = config.getBoolean("tnt-explosion-optimization.debug", false);
        enableDebugLogging = config.getBoolean("performance.debug-logging", false);
        enablePerformanceMetrics = config.getBoolean("performance.enable-metrics", true);
        configVersion = config.getInt("version", 1);
        
        validateConfigVersion();
        validateConfig();
    }
    
    private void validateConfigVersion() {
        final int CURRENT_CONFIG_VERSION = 2;
        
        if (configVersion < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("==========================================");
            plugin.getLogger().warning("  CONFIG VERSION WARNING");
            plugin.getLogger().warning("==========================================");
            plugin.getLogger().warning("Your config.yml is outdated!");
            plugin.getLogger().warning("Current version: " + CURRENT_CONFIG_VERSION);
            plugin.getLogger().warning("Your version: " + configVersion);
            plugin.getLogger().warning("");
            plugin.getLogger().warning("Please update your config.yml to avoid issues.");
            plugin.getLogger().warning("The plugin will continue with default values for new options.");
            plugin.getLogger().warning("==========================================");
        } else if (configVersion > CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("==========================================");
            plugin.getLogger().warning("  CONFIG VERSION WARNING");
            plugin.getLogger().warning("==========================================");
            plugin.getLogger().warning("Your config.yml is from a newer version!");
            plugin.getLogger().warning("Current supported version: " + CURRENT_CONFIG_VERSION);
            plugin.getLogger().warning("Your version: " + configVersion);
            plugin.getLogger().warning("");
            plugin.getLogger().warning("Please update the plugin or downgrade your config.");
            plugin.getLogger().warning("==========================================");
        }
    }
    
    private void validateConfig() {
        if (threadPoolSize < 1) {
            plugin.getLogger().warning("Thread pool size cannot be less than 1, setting to 1");
            threadPoolSize = 1;
        }
        if (threadPoolSize > 32) {
            plugin.getLogger().warning("Thread pool size cannot be more than 32, setting to 32");
            threadPoolSize = 32;
        }
        
        if (updateIntervalTicks < 1) {
            plugin.getLogger().warning("Update interval cannot be less than 1 tick, setting to 1");
            updateIntervalTicks = 1;
        }
        
        if (maxQueueSize < 100) {
            plugin.getLogger().warning("Max queue size cannot be less than 100, setting to 100");
            maxQueueSize = 100;
        }

        if (maxEntitiesPerChunk < 20) {
            maxEntitiesPerChunk = 20;
        }
        if (aiCooldownTicks < 0) {
            aiCooldownTicks = 0;
        }
        if (brainThrottleInterval < 0) {
            brainThrottleInterval = 0;
        }
        if (asyncAITimeoutMicros < 100) {
            plugin.getLogger().warning("Async AI timeout too low, setting to 100娓璼");
            asyncAITimeoutMicros = 100;
        }
        if (asyncAITimeoutMicros > 5000) {
            plugin.getLogger().warning("Async AI timeout too high, setting to 5000娓璼 (5ms)");
            asyncAITimeoutMicros = 5000;
        }
        if (entityTickThreads < 1) entityTickThreads = 1;
        if (entityTickThreads > 16) entityTickThreads = 16;
        if (minEntitiesForParallel < 10) minEntitiesForParallel = 10;
        if (lightingThreadPoolSize < 1) lightingThreadPoolSize = 1;
        if (lightingThreadPoolSize > 8) lightingThreadPoolSize = 8;
        if (lightBatchThreshold < 1) lightBatchThreshold = 1;
        if (lightBatchThreshold > 100) lightBatchThreshold = 100;
        if (maxLightPropagationDistance < 1) maxLightPropagationDistance = 1;
        if (maxLightPropagationDistance > 32) maxLightPropagationDistance = 32;
        if (skylightCacheDurationMs < 0) skylightCacheDurationMs = 0;
        if (maxConcurrentChunkLoadsPerPlayer < 1) maxConcurrentChunkLoadsPerPlayer = 1;
        if (maxConcurrentChunkLoadsPerPlayer > 20) maxConcurrentChunkLoadsPerPlayer = 20;
        if (entityTrackingRangeMultiplier < 0.1) entityTrackingRangeMultiplier = 0.1;
        if (entityTrackingRangeMultiplier > 2.0) entityTrackingRangeMultiplier = 2.0;
        if (redstoneUpdateBatchThreshold < 1) redstoneUpdateBatchThreshold = 1;
        if (redstoneUpdateBatchThreshold > 50) redstoneUpdateBatchThreshold = 50;
        if (redstoneCacheDurationMs < 0) redstoneCacheDurationMs = 0;
        if (redstoneCacheDurationMs > 1000) redstoneCacheDurationMs = 1000;
        if (tntThreads < 1) {
            plugin.getLogger().warning("TNT threads cannot be less than 1, setting to 1");
            tntThreads = 1;
        }
        if (tntThreads > 32) {
            plugin.getLogger().warning("TNT threads cannot be more than 32, setting to 32");
            tntThreads = 32;
        }
        if (tntMaxBlocks < 256) tntMaxBlocks = 256;
        if (tntMaxBlocks > 16384) tntMaxBlocks = 16384;
        if (tntTimeoutMicros < 10) tntTimeoutMicros = 10;
        if (tntTimeoutMicros > 10000) tntTimeoutMicros = 10000;
        if (tntBatchSize < 8) tntBatchSize = 8;
        if (tntBatchSize > 256) tntBatchSize = 256;
    }
    
    public void reload() {
        loadConfig();
        plugin.getLogger().info("Configuration reloaded successfully!");
    }
    
    public boolean isEntityTrackerEnabled() {
        return entityTrackerEnabled;
    }
    
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    public int getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }
    
    public int getMaxQueueSize() {
        return maxQueueSize;
    }
    
    public boolean isDebugLoggingEnabled() {
        return enableDebugLogging;
    }
    
    public void setDebugLoggingEnabled(boolean enabled) {
        this.enableDebugLogging = enabled;
        config.set("performance.debug-logging", enabled);
        plugin.saveConfig();
    }
    
    public boolean isPerformanceMetricsEnabled() {
        return enablePerformanceMetrics;
    }
    
    public boolean isMobSpawningEnabled() {
        return mobSpawningEnabled;
    }
    
    public boolean isSpawnerOptimizationEnabled() {
        return spawnerOptimizationEnabled;
    }
    
    public int getMaxEntitiesPerChunk() {
        return maxEntitiesPerChunk;
    }
    
    public int getAiCooldownTicks() {
        return aiCooldownTicks;
    }
    
    public boolean isBrainThrottleEnabled() { return brainThrottle; }
    public int getBrainThrottleInterval() { return brainThrottleInterval; }
    public long getAsyncAITimeoutMicros() { return asyncAITimeoutMicros; }
    public boolean isVillagerOptimizationEnabled() { return villagerOptimizationEnabled; }
    public boolean isVillagerUsePOISnapshot() { return villagerUsePOISnapshot; }
    public boolean isPiglinOptimizationEnabled() { return piglinOptimizationEnabled; }
    public boolean isPiglinUsePOISnapshot() { return piglinUsePOISnapshot; }
    public int getPiglinLookDistance() { return piglinLookDistance; }
    public int getPiglinBarterDistance() { return piglinBarterDistance; }
    public boolean isPillagerFamilyOptimizationEnabled() { return pillagerFamilyOptimizationEnabled; }
    public boolean isPillagerFamilyUsePOISnapshot() { return pillagerFamilyUsePOISnapshot; }
    public boolean isEvokerOptimizationEnabled() { return evokerOptimizationEnabled; }
    public boolean isBlazeOptimizationEnabled() { return blazeOptimizationEnabled; }
    public boolean isGuardianOptimizationEnabled() { return guardianOptimizationEnabled; }
    public boolean isWitchOptimizationEnabled() { return witchOptimizationEnabled; }
    public boolean isUniversalAiOptimizationEnabled() { return universalAiOptimizationEnabled; }
    public java.util.Set<String> getUniversalAiEntities() { return universalAiEntities; }
    public boolean isZeroDelayFactoryOptimizationEnabled() { return zeroDelayFactoryOptimizationEnabled; }
    public java.util.Set<String> getZeroDelayFactoryEntities() { return zeroDelayFactoryEntities; }
    public boolean isItemEntityOptimizationEnabled() { return itemEntityOptimizationEnabled; }
    public int getItemEntityAgeInterval() { return itemEntityAgeInterval; }
    public int getItemEntityMinNearbyItems() { return itemEntityMinNearbyItems; }
    public boolean isSimpleEntitiesOptimizationEnabled() { return simpleEntitiesOptimizationEnabled; }
    public boolean isSimpleEntitiesUsePOISnapshot() { return simpleEntitiesUsePOISnapshot; }
    public boolean isEntityTickParallel() { return entityTickParallel; }
    public int getEntityTickThreads() { return entityTickThreads; }
    public int getMinEntitiesForParallel() { return minEntitiesForParallel; }
    public int getEntityTickBatchSize() { return entityTickBatchSize; }
    public boolean isAsyncLightingEnabled() { return asyncLightingEnabled; }
    public int getLightingThreadPoolSize() { return lightingThreadPoolSize; }
    public int getLightBatchThreshold() { return lightBatchThreshold; }
    public boolean useLayeredPropagationQueue() { return useLayeredPropagationQueue; }
    public int getMaxLightPropagationDistance() { return maxLightPropagationDistance; }
    public boolean isSkylightCacheEnabled() { return skylightCacheEnabled; }
    public int getSkylightCacheDurationMs() { return skylightCacheDurationMs; }
    public boolean isLightDeduplicationEnabled() { return lightDeduplicationEnabled; }
    public boolean isDynamicBatchAdjustmentEnabled() { return dynamicBatchAdjustmentEnabled; }
    public boolean isAdvancedLightingStatsEnabled() { return advancedLightingStatsEnabled; }
    public boolean isPlayerChunkLoadingOptimizationEnabled() { return playerChunkLoadingOptimizationEnabled; }
    public int getMaxConcurrentChunkLoadsPerPlayer() { return maxConcurrentChunkLoadsPerPlayer; }
    public boolean isEntityTrackingRangeOptimizationEnabled() { return entityTrackingRangeOptimizationEnabled; }
    public double getEntityTrackingRangeMultiplier() { return entityTrackingRangeMultiplier; }
    public boolean isAlternateCurrentEnabled() { return alternateCurrentEnabled; }
    public boolean isRedstoneWireTurboEnabled() { return redstoneWireTurboEnabled; }
    public boolean isRedstoneUpdateBatchingEnabled() { return redstoneUpdateBatchingEnabled; }
    public int getRedstoneUpdateBatchThreshold() { return redstoneUpdateBatchThreshold; }
    public boolean isRedstoneCacheEnabled() { return redstoneCacheEnabled; }
    public int getRedstoneCacheDurationMs() { return redstoneCacheDurationMs; }
    public boolean isAsyncVillagerBreedEnabled() { return asyncVillagerBreedEnabled; }
    public boolean isVillagerAgeThrottleEnabled() { return villagerAgeThrottleEnabled; }
    public int getVillagerBreedThreads() { return villagerBreedThreads; }
    public int getVillagerBreedCheckInterval() { return villagerBreedCheckInterval; }
    public boolean isTNTOptimizationEnabled() { return tntOptimizationEnabled; }
    public java.util.Set<String> getTNTExplosionEntities() { return tntExplosionEntities; }
    public int getTNTThreads() { return tntThreads; }
    public int getTNTMaxBlocks() { return tntMaxBlocks; }
    public long getTNTTimeoutMicros() { return tntTimeoutMicros; }
    public int getTNTBatchSize() { return tntBatchSize; }
    public boolean isTNTDebugEnabled() { return tntDebugEnabled; }
    public boolean isChunkTickAsyncEnabled() { return chunkTickAsyncEnabled; }
    public int getChunkTickThreads() { return chunkTickThreads; }
    public long getChunkTickTimeoutMicros() { return chunkTickTimeoutMicros; }
    
    public int getConfigVersion() {
        return configVersion;
    }
}