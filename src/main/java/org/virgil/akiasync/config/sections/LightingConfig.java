package org.virgil.akiasync.config.sections;

import org.bukkit.configuration.file.FileConfiguration;

public class LightingConfig {

    private boolean asyncLightingEnabled;
    private int lightingThreadPoolSize;
    private int lightBatchThreshold;
    private int lightUpdateIntervalMs;
    private boolean useLayeredPropagationQueue;
    private int maxLightPropagationDistance;
    private boolean skylightCacheEnabled;
    private int skylightCacheDurationMs;
    private boolean lightDeduplicationEnabled;
    private boolean dynamicBatchAdjustmentEnabled;
    private boolean advancedLightingStatsEnabled;
    private boolean lightingDebugEnabled;

    private String lightingThreadPoolMode;
    private String lightingThreadPoolCalculation;
    private int lightingMinThreads;
    private int lightingMaxThreads;
    private int lightingBatchThresholdMax;
    private boolean lightingAggressiveBatching;

    private boolean lightingPrioritySchedulingEnabled;
    private int lightingHighPriorityRadius;
    private int lightingMediumPriorityRadius;
    private int lightingLowPriorityRadius;
    private long lightingMaxLowPriorityDelay;

    private boolean lightingDebouncingEnabled;
    private long lightingDebounceDelay;
    private int lightingMaxUpdatesPerSecond;
    private long lightingResetOnStableMs;

    private boolean lightingMergingEnabled;
    private int lightingMergeRadius;
    private long lightingMergeDelay;
    private int lightingMaxMergedUpdates;

    private boolean lightingChunkBorderEnabled;
    private boolean lightingBatchBorderUpdates;
    private long lightingBorderUpdateDelay;
    private int lightingCrossChunkBatchSize;

    private boolean lightingAdaptiveEnabled;
    private int lightingMonitorInterval;
    private boolean lightingAutoAdjustThreads;
    private boolean lightingAutoAdjustBatchSize;
    private int lightingTargetQueueSize;
    private int lightingTargetLatency;

    private boolean lightingChunkUnloadEnabled;
    private boolean lightingAsyncCleanup;
    private int lightingCleanupBatchSize;
    private long lightingCleanupDelay;

    public void load(FileConfiguration config) {
        asyncLightingEnabled = config.getBoolean("lighting-optimizations.enabled", true);
        lightingThreadPoolSize = config.getInt("lighting-optimizations.async-lighting.thread-pool-size", 2);
        lightBatchThreshold = config.getInt("lighting-optimizations.async-lighting.batch-threshold", 16);
        lightUpdateIntervalMs = config.getInt("lighting-optimizations.update-interval-ms", 10);
        useLayeredPropagationQueue = config.getBoolean("lighting-optimizations.propagation-queue.use-layered-queue", true);
        maxLightPropagationDistance = config.getInt("lighting-optimizations.propagation-queue.max-propagation-distance", 15);
        skylightCacheEnabled = config.getBoolean("lighting-optimizations.skylight-cache.enabled", true);
        skylightCacheDurationMs = config.getInt("lighting-optimizations.skylight-cache.cache-duration-ms", 100);
        lightDeduplicationEnabled = config.getBoolean("lighting-optimizations.advanced.enable-deduplication", true);
        dynamicBatchAdjustmentEnabled = config.getBoolean("lighting-optimizations.advanced.dynamic-batch-adjustment", true);
        advancedLightingStatsEnabled = config.getBoolean("lighting-optimizations.advanced.log-advanced-stats", false);
        lightingDebugEnabled = config.getBoolean("performance.debug-logging.modules.lighting", false);

        lightingThreadPoolMode = config.getString("lighting-optimizations.async-lighting.thread-pool-mode", "auto");
        lightingThreadPoolCalculation = config.getString("lighting-optimizations.async-lighting.thread-pool-calculation", "cores/3");
        lightingMinThreads = config.getInt("lighting-optimizations.async-lighting.min-threads", 1);
        lightingMaxThreads = config.getInt("lighting-optimizations.async-lighting.max-threads", 8);
        lightingBatchThresholdMax = config.getInt("lighting-optimizations.async-lighting.batch-threshold-max", 64);
        lightingAggressiveBatching = config.getBoolean("lighting-optimizations.async-lighting.aggressive-batching", false);

        lightingPrioritySchedulingEnabled = config.getBoolean("lighting-optimizations.async-lighting.priority-scheduling.enabled", true);
        lightingHighPriorityRadius = config.getInt("lighting-optimizations.async-lighting.priority-scheduling.player-radius-high-priority", 32);
        lightingMediumPriorityRadius = config.getInt("lighting-optimizations.async-lighting.priority-scheduling.player-radius-medium-priority", 64);
        lightingLowPriorityRadius = config.getInt("lighting-optimizations.async-lighting.priority-scheduling.player-radius-low-priority", 128);
        lightingMaxLowPriorityDelay = config.getLong("lighting-optimizations.async-lighting.priority-scheduling.max-low-priority-delay-ms", 500);

        lightingDebouncingEnabled = config.getBoolean("lighting-optimizations.async-lighting.debouncing.enabled", true);
        lightingDebounceDelay = config.getLong("lighting-optimizations.async-lighting.debouncing.debounce-delay-ms", 50);
        lightingMaxUpdatesPerSecond = config.getInt("lighting-optimizations.async-lighting.debouncing.max-updates-per-second", 20);
        lightingResetOnStableMs = config.getLong("lighting-optimizations.async-lighting.debouncing.reset-on-stable-ms", 200);

        lightingMergingEnabled = config.getBoolean("lighting-optimizations.async-lighting.update-merging.enabled", true);
        lightingMergeRadius = config.getInt("lighting-optimizations.async-lighting.update-merging.merge-radius", 2);
        lightingMergeDelay = config.getLong("lighting-optimizations.async-lighting.update-merging.merge-delay-ms", 10);
        lightingMaxMergedUpdates = config.getInt("lighting-optimizations.async-lighting.update-merging.max-merged-updates", 64);

        lightingChunkBorderEnabled = config.getBoolean("lighting-optimizations.async-lighting.chunk-border.enabled", true);
        lightingBatchBorderUpdates = config.getBoolean("lighting-optimizations.async-lighting.chunk-border.batch-border-updates", true);
        lightingBorderUpdateDelay = config.getLong("lighting-optimizations.async-lighting.chunk-border.border-update-delay-ms", 20);
        lightingCrossChunkBatchSize = config.getInt("lighting-optimizations.async-lighting.chunk-border.cross-chunk-batch-size", 32);

        lightingAdaptiveEnabled = config.getBoolean("lighting-optimizations.async-lighting.adaptive.enabled", true);
        lightingMonitorInterval = config.getInt("lighting-optimizations.async-lighting.adaptive.monitor-interval-seconds", 10);
        lightingAutoAdjustThreads = config.getBoolean("lighting-optimizations.async-lighting.adaptive.auto-adjust-threads", true);
        lightingAutoAdjustBatchSize = config.getBoolean("lighting-optimizations.async-lighting.adaptive.auto-adjust-batch-size", true);
        lightingTargetQueueSize = config.getInt("lighting-optimizations.async-lighting.adaptive.target-queue-size", 100);
        lightingTargetLatency = config.getInt("lighting-optimizations.async-lighting.adaptive.target-latency-ms", 50);

        lightingChunkUnloadEnabled = config.getBoolean("lighting-optimizations.async-lighting.chunk-unload.enabled", true);
        lightingAsyncCleanup = config.getBoolean("lighting-optimizations.async-lighting.chunk-unload.async-cleanup", true);
        lightingCleanupBatchSize = config.getInt("lighting-optimizations.async-lighting.chunk-unload.cleanup-batch-size", 16);
        lightingCleanupDelay = config.getLong("lighting-optimizations.async-lighting.chunk-unload.cleanup-delay-ms", 100);
    }

    public void validate(java.util.logging.Logger logger) {
        if (lightingThreadPoolSize < 1) lightingThreadPoolSize = 1;
        if (lightingThreadPoolSize > 8) lightingThreadPoolSize = 8;
        if (lightBatchThreshold < 1) lightBatchThreshold = 1;
        if (lightBatchThreshold > 100) lightBatchThreshold = 100;
        if (maxLightPropagationDistance < 1) maxLightPropagationDistance = 1;
        if (maxLightPropagationDistance > 32) maxLightPropagationDistance = 32;
        if (skylightCacheDurationMs < 0) skylightCacheDurationMs = 0;
    }

    public boolean isAsyncLightingEnabled() { return asyncLightingEnabled; }
    public int getLightingThreadPoolSize() { return lightingThreadPoolSize; }
    public int getLightBatchThreshold() { return lightBatchThreshold; }
    public int getLightUpdateIntervalMs() { return lightUpdateIntervalMs; }
    public boolean useLayeredPropagationQueue() { return useLayeredPropagationQueue; }
    public int getMaxLightPropagationDistance() { return maxLightPropagationDistance; }
    public boolean isSkylightCacheEnabled() { return skylightCacheEnabled; }
    public int getSkylightCacheDurationMs() { return skylightCacheDurationMs; }
    public boolean isLightDeduplicationEnabled() { return lightDeduplicationEnabled; }
    public boolean isDynamicBatchAdjustmentEnabled() { return dynamicBatchAdjustmentEnabled; }
    public boolean isAdvancedLightingStatsEnabled() { return advancedLightingStatsEnabled; }
    public boolean isLightingDebugEnabled() { return lightingDebugEnabled; }

    public String getLightingThreadPoolMode() { return lightingThreadPoolMode; }
    public String getLightingThreadPoolCalculation() { return lightingThreadPoolCalculation; }
    public int getLightingMinThreads() { return lightingMinThreads; }
    public int getLightingMaxThreads() { return lightingMaxThreads; }
    public int getLightingBatchThresholdMax() { return lightingBatchThresholdMax; }
    public boolean isLightingAggressiveBatching() { return lightingAggressiveBatching; }

    public boolean isLightingPrioritySchedulingEnabled() { return lightingPrioritySchedulingEnabled; }
    public int getLightingHighPriorityRadius() { return lightingHighPriorityRadius; }
    public int getLightingMediumPriorityRadius() { return lightingMediumPriorityRadius; }
    public int getLightingLowPriorityRadius() { return lightingLowPriorityRadius; }
    public long getLightingMaxLowPriorityDelay() { return lightingMaxLowPriorityDelay; }

    public boolean isLightingDebouncingEnabled() { return lightingDebouncingEnabled; }
    public long getLightingDebounceDelay() { return lightingDebounceDelay; }
    public int getLightingMaxUpdatesPerSecond() { return lightingMaxUpdatesPerSecond; }
    public long getLightingResetOnStableMs() { return lightingResetOnStableMs; }

    public boolean isLightingMergingEnabled() { return lightingMergingEnabled; }
    public int getLightingMergeRadius() { return lightingMergeRadius; }
    public long getLightingMergeDelay() { return lightingMergeDelay; }
    public int getLightingMaxMergedUpdates() { return lightingMaxMergedUpdates; }

    public boolean isLightingChunkBorderEnabled() { return lightingChunkBorderEnabled; }
    public boolean isLightingBatchBorderUpdates() { return lightingBatchBorderUpdates; }
    public long getLightingBorderUpdateDelay() { return lightingBorderUpdateDelay; }
    public int getLightingCrossChunkBatchSize() { return lightingCrossChunkBatchSize; }

    public boolean isLightingAdaptiveEnabled() { return lightingAdaptiveEnabled; }
    public int getLightingMonitorInterval() { return lightingMonitorInterval; }
    public boolean isLightingAutoAdjustThreads() { return lightingAutoAdjustThreads; }
    public boolean isLightingAutoAdjustBatchSize() { return lightingAutoAdjustBatchSize; }
    public int getLightingTargetQueueSize() { return lightingTargetQueueSize; }
    public int getLightingTargetLatency() { return lightingTargetLatency; }

    public boolean isLightingChunkUnloadEnabled() { return lightingChunkUnloadEnabled; }
    public boolean isLightingAsyncCleanup() { return lightingAsyncCleanup; }
    public int getLightingCleanupBatchSize() { return lightingCleanupBatchSize; }
    public long getLightingCleanupDelay() { return lightingCleanupDelay; }
}
