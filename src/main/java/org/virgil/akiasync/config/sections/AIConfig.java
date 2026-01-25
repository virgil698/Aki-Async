package org.virgil.akiasync.config.sections;

import org.bukkit.configuration.file.FileConfiguration;

public class AIConfig {

    private boolean aiSensorOptimizationEnabled;
    private int aiSensorRefreshInterval;

    private boolean gameEventOptimizationEnabled;
    private boolean gameEventEarlyFilter;
    private boolean gameEventThrottleLowPriority;
    private long gameEventThrottleIntervalMs;
    private boolean gameEventDistanceFilter;
    private double gameEventMaxDetectionDistance;

    private boolean asyncPathfindingEnabled;
    private int asyncPathfindingMaxThreads;
    private int asyncPathfindingKeepAliveSeconds;
    private int asyncPathfindingMaxQueueSize;
    private int asyncPathfindingTimeoutMs;
    private boolean asyncPathfindingSyncFallbackEnabled;
    private boolean asyncPathfindingCacheEnabled;
    private int asyncPathfindingCacheMaxSize;
    private int asyncPathfindingCacheExpireSeconds;
    private int asyncPathfindingCacheReuseTolerance;
    private int asyncPathfindingCacheCleanupIntervalSeconds;

    private boolean enhancedPathfindingEnabled;
    private int enhancedPathfindingMaxConcurrentRequests;
    private int enhancedPathfindingMaxRequestsPerTick;
    private int enhancedPathfindingHighPriorityDistance;
    private int enhancedPathfindingMediumPriorityDistance;
    private boolean pathPrewarmEnabled;
    private int pathPrewarmRadius;
    private int pathPrewarmMaxMobsPerBatch;
    private int pathPrewarmMaxPoisPerMob;

    private boolean fastRayTraceEnabled;
    private boolean asyncVillagerBreedEnabled;
    private boolean villagerAgeThrottleEnabled;
    private int villagerBreedThreads;
    private int villagerBreedCheckInterval;

    public void load(FileConfiguration config) {
        aiSensorOptimizationEnabled = config.getBoolean("async-ai.sensor-optimization.enabled", true);
        aiSensorRefreshInterval = config.getInt("async-ai.sensor-optimization.sensing-refresh-interval", 5);

        gameEventOptimizationEnabled = config.getBoolean("async-ai.game-event-optimization.enabled", true);
        gameEventEarlyFilter = config.getBoolean("async-ai.game-event-optimization.early-filter", true);
        gameEventThrottleLowPriority = config.getBoolean("async-ai.game-event-optimization.throttle-low-priority", true);
        gameEventThrottleIntervalMs = config.getLong("async-ai.game-event-optimization.throttle-interval-ms", 50L);
        gameEventDistanceFilter = config.getBoolean("async-ai.game-event-optimization.distance-filter", true);
        gameEventMaxDetectionDistance = config.getDouble("async-ai.game-event-optimization.max-detection-distance", 64.0);

        asyncPathfindingEnabled = config.getBoolean("async-ai.async-pathfinding.enabled", true);
        asyncPathfindingMaxThreads = config.getInt("async-ai.async-pathfinding.max-threads", 6);
        asyncPathfindingKeepAliveSeconds = config.getInt("async-ai.async-pathfinding.keep-alive-seconds", 60);
        asyncPathfindingMaxQueueSize = config.getInt("async-ai.async-pathfinding.max-queue-size", 500);
        asyncPathfindingTimeoutMs = config.getInt("async-ai.async-pathfinding.timeout-ms", 100);
        asyncPathfindingSyncFallbackEnabled = config.getBoolean("async-ai.async-pathfinding.sync-fallback-enabled", true);

        asyncPathfindingCacheEnabled = config.getBoolean("async-ai.async-pathfinding.cache.enabled", true);
        asyncPathfindingCacheMaxSize = config.getInt("async-ai.async-pathfinding.cache.max-size", 1000);
        asyncPathfindingCacheExpireSeconds = config.getInt("async-ai.async-pathfinding.cache.expire-seconds", 30);
        asyncPathfindingCacheReuseTolerance = config.getInt("async-ai.async-pathfinding.cache.reuse-tolerance", 3);
        asyncPathfindingCacheCleanupIntervalSeconds = config.getInt("async-ai.async-pathfinding.cache.cleanup-interval-seconds", 5);

        enhancedPathfindingEnabled = config.getBoolean("async-ai.async-pathfinding.enhanced.enabled", true);
        enhancedPathfindingMaxConcurrentRequests = config.getInt("async-ai.async-pathfinding.enhanced.max-concurrent-requests", 30);
        enhancedPathfindingMaxRequestsPerTick = config.getInt("async-ai.async-pathfinding.enhanced.max-requests-per-tick", 15);
        enhancedPathfindingHighPriorityDistance = config.getInt("async-ai.async-pathfinding.enhanced.priority.high-distance", 16);
        enhancedPathfindingMediumPriorityDistance = config.getInt("async-ai.async-pathfinding.enhanced.priority.medium-distance", 48);
        pathPrewarmEnabled = config.getBoolean("async-ai.async-pathfinding.enhanced.prewarm.enabled", true);
        pathPrewarmRadius = config.getInt("async-ai.async-pathfinding.enhanced.prewarm.radius", 32);
        pathPrewarmMaxMobsPerBatch = config.getInt("async-ai.async-pathfinding.enhanced.prewarm.max-mobs-per-batch", 5);
        pathPrewarmMaxPoisPerMob = config.getInt("async-ai.async-pathfinding.enhanced.prewarm.max-pois-per-mob", 3);

        fastRayTraceEnabled = config.getBoolean("async-ai.villager-optimization.fast-raytrace.enabled", true);
        asyncVillagerBreedEnabled = config.getBoolean("async-ai.villager-optimization.breed-optimization.async-villager-breed", true);
        villagerAgeThrottleEnabled = config.getBoolean("async-ai.villager-optimization.breed-optimization.age-throttle", true);
        villagerBreedThreads = config.getInt("async-ai.villager-optimization.breed-optimization.threads", 4);
        villagerBreedCheckInterval = config.getInt("async-ai.villager-optimization.breed-optimization.check-interval", 5);
    }

    public boolean isAiSensorOptimizationEnabled() { return aiSensorOptimizationEnabled; }
    public int getAiSensorRefreshInterval() { return aiSensorRefreshInterval; }

    public boolean isGameEventOptimizationEnabled() { return gameEventOptimizationEnabled; }
    public boolean isGameEventEarlyFilter() { return gameEventEarlyFilter; }
    public boolean isGameEventThrottleLowPriority() { return gameEventThrottleLowPriority; }
    public long getGameEventThrottleIntervalMs() { return gameEventThrottleIntervalMs; }
    public boolean isGameEventDistanceFilter() { return gameEventDistanceFilter; }
    public double getGameEventMaxDetectionDistance() { return gameEventMaxDetectionDistance; }

    public boolean isAsyncPathfindingEnabled() { return asyncPathfindingEnabled; }
    public int getAsyncPathfindingMaxThreads() { return asyncPathfindingMaxThreads; }
    public int getAsyncPathfindingKeepAliveSeconds() { return asyncPathfindingKeepAliveSeconds; }
    public int getAsyncPathfindingMaxQueueSize() { return asyncPathfindingMaxQueueSize; }
    public int getAsyncPathfindingTimeoutMs() { return asyncPathfindingTimeoutMs; }
    public boolean isAsyncPathfindingSyncFallbackEnabled() { return asyncPathfindingSyncFallbackEnabled; }
    public boolean isAsyncPathfindingCacheEnabled() { return asyncPathfindingCacheEnabled; }
    public int getAsyncPathfindingCacheMaxSize() { return asyncPathfindingCacheMaxSize; }
    public int getAsyncPathfindingCacheExpireSeconds() { return asyncPathfindingCacheExpireSeconds; }
    public int getAsyncPathfindingCacheReuseTolerance() { return asyncPathfindingCacheReuseTolerance; }
    public int getAsyncPathfindingCacheCleanupIntervalSeconds() { return asyncPathfindingCacheCleanupIntervalSeconds; }

    public boolean isEnhancedPathfindingEnabled() { return enhancedPathfindingEnabled; }
    public int getEnhancedPathfindingMaxConcurrentRequests() { return enhancedPathfindingMaxConcurrentRequests; }
    public int getEnhancedPathfindingMaxRequestsPerTick() { return enhancedPathfindingMaxRequestsPerTick; }
    public int getEnhancedPathfindingHighPriorityDistance() { return enhancedPathfindingHighPriorityDistance; }
    public int getEnhancedPathfindingMediumPriorityDistance() { return enhancedPathfindingMediumPriorityDistance; }
    public boolean isPathPrewarmEnabled() { return pathPrewarmEnabled; }
    public int getPathPrewarmRadius() { return pathPrewarmRadius; }
    public int getPathPrewarmMaxMobsPerBatch() { return pathPrewarmMaxMobsPerBatch; }
    public int getPathPrewarmMaxPoisPerMob() { return pathPrewarmMaxPoisPerMob; }

    public boolean isFastRayTraceEnabled() { return fastRayTraceEnabled; }
    public boolean isAsyncVillagerBreedEnabled() { return asyncVillagerBreedEnabled; }
    public boolean isVillagerAgeThrottleEnabled() { return villagerAgeThrottleEnabled; }
    public int getVillagerBreedThreads() { return villagerBreedThreads; }
    public int getVillagerBreedCheckInterval() { return villagerBreedCheckInterval; }
}
