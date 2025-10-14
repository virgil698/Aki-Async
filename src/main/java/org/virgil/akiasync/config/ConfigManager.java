package org.virgil.akiasync.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.virgil.akiasync.AkiAsyncPlugin;

/**
 * Configuration manager for AkiAsync plugin
 * Handles loading and accessing configuration values
 * 
 * @author Virgil
 */
public class ConfigManager {
    
    private final AkiAsyncPlugin plugin;
    private FileConfiguration config;
    
    // Entity Tracker settings
    private boolean entityTrackerEnabled;
    private int threadPoolSize;
    private int updateIntervalTicks;
    private int maxQueueSize;
    
    // Mob Spawning settings
    private boolean mobSpawningEnabled;
    private boolean spawnerOptimizationEnabled;
    
    // Density & pathfinding
    private int maxEntitiesPerChunk;
    private int aiCooldownTicks;
    private int pathfindingTickBudget;
    // Brain throttle
    private boolean brainThrottle;
    private int brainThrottleInterval;
    // Async AI - Zero Latency (分类优化)
    private long asyncAITimeoutMicros;
    private boolean villagerOptimizationEnabled;
    private boolean villagerUsePOISnapshot;
    private boolean piglinOptimizationEnabled;
    private boolean piglinUsePOISnapshot;
    private int piglinLookDistance;
    private int piglinBarterDistance;
    private boolean simpleEntitiesOptimizationEnabled;
    private boolean simpleEntitiesUsePOISnapshot;
    // Parallel EntityTickList
    private boolean entityTickParallel;
    private int entityTickThreads;
    private int minEntitiesForParallel;
    private int entityTickBatchSize;
    
    // Lighting optimizations (ScalableLux/Starlight inspired)
    private boolean asyncLightingEnabled;
    private int lightingThreadPoolSize;
    private int lightBatchThreshold;
    private boolean useLayeredPropagationQueue;
    private int maxLightPropagationDistance;
    private boolean skylightCacheEnabled;
    private int skylightCacheDurationMs;
    // Advanced lighting optimizations
    private boolean lightDeduplicationEnabled;
    private boolean dynamicBatchAdjustmentEnabled;
    private boolean advancedLightingStatsEnabled;
    
    // VMP (Very Many Players) optimizations
    private boolean playerChunkLoadingOptimizationEnabled;
    private int maxConcurrentChunkLoadsPerPlayer;
    private boolean entityTrackingRangeOptimizationEnabled;
    private double entityTrackingRangeMultiplier;
    
    // Redstone optimizations (Alternate Current + Carpet Turbo)
    private boolean alternateCurrentEnabled;
    private boolean redstoneWireTurboEnabled;
    private boolean redstoneUpdateBatchingEnabled;
    private int redstoneUpdateBatchThreshold;
    private boolean redstoneCacheEnabled;
    private int redstoneCacheDurationMs;
    
    // Performance settings
    private boolean enableDebugLogging;
    private boolean enablePerformanceMetrics;
    
    public ConfigManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load configuration from config.yml
     */
    public void loadConfig() {
        // Save default config if not exists
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load entity tracker settings
        entityTrackerEnabled = config.getBoolean("entity-tracker.enabled", true);
        threadPoolSize = config.getInt("entity-tracker.thread-pool-size", 4);
        updateIntervalTicks = config.getInt("entity-tracker.update-interval-ticks", 1);
        maxQueueSize = config.getInt("entity-tracker.max-queue-size", 1000);
        
        // Load mob spawning settings
        mobSpawningEnabled = config.getBoolean("mob-spawning.enabled", true);
        spawnerOptimizationEnabled = config.getBoolean("mob-spawning.spawner-optimization", true);
        
        // Density & budget
        maxEntitiesPerChunk = config.getInt("density.max-per-chunk", 80);
        aiCooldownTicks = config.getInt("density.ai-cooldown-ticks", 10);
        pathfindingTickBudget = config.getInt("pathfinding.tick-budget", 0);
        brainThrottle = config.getBoolean("brain.throttle", true);
        brainThrottleInterval = config.getInt("brain.throttle-interval", 10);
        // Async AI settings (分类优化)
        asyncAITimeoutMicros = config.getLong("async-ai.timeout-microseconds", 500L);
        villagerOptimizationEnabled = config.getBoolean("async-ai.villager-optimization.enabled", false);
        villagerUsePOISnapshot = config.getBoolean("async-ai.villager-optimization.use-poi-snapshot", true);
        piglinOptimizationEnabled = config.getBoolean("async-ai.piglin-optimization.enabled", false);
        piglinUsePOISnapshot = config.getBoolean("async-ai.piglin-optimization.use-poi-snapshot", false);
        piglinLookDistance = config.getInt("async-ai.piglin-optimization.look-distance", 16);
        piglinBarterDistance = config.getInt("async-ai.piglin-optimization.barter-distance", 16);
        simpleEntitiesOptimizationEnabled = config.getBoolean("async-ai.simple-entities.enabled", false);
        simpleEntitiesUsePOISnapshot = config.getBoolean("async-ai.simple-entities.use-poi-snapshot", false);
        entityTickParallel = config.getBoolean("entity-tick-parallel.enabled", true);
        entityTickThreads = config.getInt("entity-tick-parallel.threads", 4);
        minEntitiesForParallel = config.getInt("entity-tick-parallel.min-entities", 100);
        entityTickBatchSize = config.getInt("entity-tick-parallel.batch-size", 8);
        
        // Load lighting optimization settings
        asyncLightingEnabled = config.getBoolean("lighting-optimizations.async-lighting.enabled", true);
        lightingThreadPoolSize = config.getInt("lighting-optimizations.async-lighting.thread-pool-size", 2);
        lightBatchThreshold = config.getInt("lighting-optimizations.async-lighting.batch-threshold", 16);
        useLayeredPropagationQueue = config.getBoolean("lighting-optimizations.propagation-queue.use-layered-queue", true);
        maxLightPropagationDistance = config.getInt("lighting-optimizations.propagation-queue.max-propagation-distance", 15);
        skylightCacheEnabled = config.getBoolean("lighting-optimizations.skylight-cache.enabled", true);
        skylightCacheDurationMs = config.getInt("lighting-optimizations.skylight-cache.cache-duration-ms", 100);
        // Advanced lighting optimizations
        lightDeduplicationEnabled = config.getBoolean("lighting-optimizations.advanced.enable-deduplication", true);
        dynamicBatchAdjustmentEnabled = config.getBoolean("lighting-optimizations.advanced.dynamic-batch-adjustment", true);
        advancedLightingStatsEnabled = config.getBoolean("lighting-optimizations.advanced.log-advanced-stats", false);
        
        // Load VMP (Very Many Players) optimization settings
        playerChunkLoadingOptimizationEnabled = config.getBoolean("vmp-optimizations.chunk-loading.enabled", true);
        maxConcurrentChunkLoadsPerPlayer = config.getInt("vmp-optimizations.chunk-loading.max-concurrent-per-player", 5);
        entityTrackingRangeOptimizationEnabled = config.getBoolean("vmp-optimizations.entity-tracking.enabled", true);
        entityTrackingRangeMultiplier = config.getDouble("vmp-optimizations.entity-tracking.range-multiplier", 0.8);
        
        // Load redstone optimization settings
        alternateCurrentEnabled = config.getBoolean("redstone-optimizations.alternate-current.enabled", true);
        redstoneWireTurboEnabled = config.getBoolean("redstone-optimizations.wire-turbo.enabled", true);
        redstoneUpdateBatchingEnabled = config.getBoolean("redstone-optimizations.update-batching.enabled", true);
        redstoneUpdateBatchThreshold = config.getInt("redstone-optimizations.update-batching.batch-threshold", 8);
        redstoneCacheEnabled = config.getBoolean("redstone-optimizations.cache.enabled", true);
        redstoneCacheDurationMs = config.getInt("redstone-optimizations.cache.duration-ms", 50);
        
        // Load performance settings
        enableDebugLogging = config.getBoolean("performance.debug-logging", false);
        enablePerformanceMetrics = config.getBoolean("performance.enable-metrics", true);
        
        // Validate configuration
        validateConfig();
    }
    
    /**
     * Validate configuration values
     */
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
        if (pathfindingTickBudget < 0) {
            pathfindingTickBudget = 0;
        }
        if (brainThrottleInterval < 0) {
            brainThrottleInterval = 0;
        }
        // Validate async AI settings
        if (asyncAITimeoutMicros < 100) {
            plugin.getLogger().warning("Async AI timeout too low, setting to 100μs");
            asyncAITimeoutMicros = 100;
        }
        if (asyncAITimeoutMicros > 5000) {
            plugin.getLogger().warning("Async AI timeout too high, setting to 5000μs (5ms)");
            asyncAITimeoutMicros = 5000;
        }
        if (entityTickThreads < 1) entityTickThreads = 1;
        if (entityTickThreads > 16) entityTickThreads = 16;
        if (minEntitiesForParallel < 10) minEntitiesForParallel = 10;
        
        // Validate lighting settings
        if (lightingThreadPoolSize < 1) lightingThreadPoolSize = 1;
        if (lightingThreadPoolSize > 8) lightingThreadPoolSize = 8;
        if (lightBatchThreshold < 1) lightBatchThreshold = 1;
        if (lightBatchThreshold > 100) lightBatchThreshold = 100;
        if (maxLightPropagationDistance < 1) maxLightPropagationDistance = 1;
        if (maxLightPropagationDistance > 32) maxLightPropagationDistance = 32;
        if (skylightCacheDurationMs < 0) skylightCacheDurationMs = 0;
        
        // Validate VMP settings
        if (maxConcurrentChunkLoadsPerPlayer < 1) maxConcurrentChunkLoadsPerPlayer = 1;
        if (maxConcurrentChunkLoadsPerPlayer > 20) maxConcurrentChunkLoadsPerPlayer = 20;
        if (entityTrackingRangeMultiplier < 0.1) entityTrackingRangeMultiplier = 0.1;
        if (entityTrackingRangeMultiplier > 2.0) entityTrackingRangeMultiplier = 2.0;
        
        // Validate redstone settings
        if (redstoneUpdateBatchThreshold < 1) redstoneUpdateBatchThreshold = 1;
        if (redstoneUpdateBatchThreshold > 50) redstoneUpdateBatchThreshold = 50;
        if (redstoneCacheDurationMs < 0) redstoneCacheDurationMs = 0;
        if (redstoneCacheDurationMs > 1000) redstoneCacheDurationMs = 1000;
    }
    
    /**
     * Reload configuration
     */
    public void reload() {
        loadConfig();
        plugin.getLogger().info("Configuration reloaded successfully!");
    }
    
    // Getters
    
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
    
    public int getPathfindingTickBudget() {
        return pathfindingTickBudget;
    }
    
    public boolean isBrainThrottleEnabled() { return brainThrottle; }
    public int getBrainThrottleInterval() { return brainThrottleInterval; }
    // Async AI getters (分类优化)
    public long getAsyncAITimeoutMicros() { return asyncAITimeoutMicros; }
    public boolean isVillagerOptimizationEnabled() { return villagerOptimizationEnabled; }
    public boolean isVillagerUsePOISnapshot() { return villagerUsePOISnapshot; }
    public boolean isPiglinOptimizationEnabled() { return piglinOptimizationEnabled; }
    public boolean isPiglinUsePOISnapshot() { return piglinUsePOISnapshot; }
    public int getPiglinLookDistance() { return piglinLookDistance; }
    public int getPiglinBarterDistance() { return piglinBarterDistance; }
    public boolean isSimpleEntitiesOptimizationEnabled() { return simpleEntitiesOptimizationEnabled; }
    public boolean isSimpleEntitiesUsePOISnapshot() { return simpleEntitiesUsePOISnapshot; }
    public boolean isEntityTickParallel() { return entityTickParallel; }
    public int getEntityTickThreads() { return entityTickThreads; }
    public int getMinEntitiesForParallel() { return minEntitiesForParallel; }
    public int getEntityTickBatchSize() { return entityTickBatchSize; }
    
    // Lighting optimization getters
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
    
    // VMP optimization getters
    public boolean isPlayerChunkLoadingOptimizationEnabled() { return playerChunkLoadingOptimizationEnabled; }
    public int getMaxConcurrentChunkLoadsPerPlayer() { return maxConcurrentChunkLoadsPerPlayer; }
    public boolean isEntityTrackingRangeOptimizationEnabled() { return entityTrackingRangeOptimizationEnabled; }
    public double getEntityTrackingRangeMultiplier() { return entityTrackingRangeMultiplier; }
    
    // Redstone optimization getters
    public boolean isAlternateCurrentEnabled() { return alternateCurrentEnabled; }
    public boolean isRedstoneWireTurboEnabled() { return redstoneWireTurboEnabled; }
    public boolean isRedstoneUpdateBatchingEnabled() { return redstoneUpdateBatchingEnabled; }
    public int getRedstoneUpdateBatchThreshold() { return redstoneUpdateBatchThreshold; }
    public boolean isRedstoneCacheEnabled() { return redstoneCacheEnabled; }
    public int getRedstoneCacheDurationMs() { return redstoneCacheDurationMs; }
}

