package org.virgil.akiasync.mixin.bridge.sub;

public interface LightingBridge {

    boolean isAsyncLightingEnabled();
    int getLightBatchThreshold();
    int getLightUpdateIntervalMs();
    int getLightingParallelism();
    boolean useLayeredPropagationQueue();
    int getMaxLightPropagationDistance();

    boolean isSkylightCacheEnabled();
    int getSkylightCacheDurationMs();
    boolean isLightDeduplicationEnabled();
    boolean isDynamicBatchAdjustmentEnabled();
    boolean isAdvancedLightingStatsEnabled();
    boolean isLightingDebugEnabled();

    boolean isLightingPrioritySchedulingEnabled();
    int getLightingHighPriorityRadius();
    int getLightingMediumPriorityRadius();
    int getLightingLowPriorityRadius();
    long getLightingMaxLowPriorityDelay();

    boolean isLightingDebouncingEnabled();
    long getLightingDebounceDelay();
    int getLightingMaxUpdatesPerSecond();
    long getLightingResetOnStableMs();

    boolean isLightingMergingEnabled();
    int getLightingMergeRadius();
    long getLightingMergeDelay();
    int getLightingMaxMergedUpdates();

    boolean isLightingChunkBorderEnabled();
    boolean isLightingBatchBorderUpdates();
    long getLightingBorderUpdateDelay();
    int getLightingCrossChunkBatchSize();

    boolean isLightingAdaptiveEnabled();
    int getLightingMonitorInterval();
    boolean isLightingAutoAdjustThreads();
    boolean isLightingAutoAdjustBatchSize();
    int getLightingTargetQueueSize();
    int getLightingTargetLatency();

    boolean isLightingChunkUnloadEnabled();
    boolean isLightingAsyncCleanup();
    int getLightingCleanupBatchSize();
    long getLightingCleanupDelay();

    String getLightingThreadPoolMode();
    String getLightingThreadPoolCalculation();
    int getLightingMinThreads();
    int getLightingMaxThreads();
    int getLightingBatchThresholdMax();
    boolean isLightingAggressiveBatching();
}
