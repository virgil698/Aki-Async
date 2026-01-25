package org.virgil.akiasync.mixin.bridge.sub;

public interface PathfindingBridge {

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

    void prewarmPlayerPaths(java.util.UUID playerId);
    void cleanupPlayerPaths(java.util.UUID playerId);
}
