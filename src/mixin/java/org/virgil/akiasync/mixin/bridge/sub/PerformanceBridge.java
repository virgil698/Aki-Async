package org.virgil.akiasync.mixin.bridge.sub;

public interface PerformanceBridge {

    boolean isNoiseOptimizationEnabled();
    boolean isNbtOptimizationEnabled();
    boolean isBitSetPoolingEnabled();
    boolean isCompletableFutureOptimizationEnabled();

    boolean isMultithreadedTrackerEnabled();
    int getMultithreadedTrackerParallelism();
    int getMultithreadedTrackerBatchSize();
    int getMultithreadedTrackerAssistBatchSize();
    boolean isMultithreadedEntityTrackerEnabled();

    boolean isEntityTrackingRangeOptimizationEnabled();
    double getEntityTrackingRangeMultiplier();
    boolean isEntityTrackerDistanceCacheEnabled();

    boolean isSmartLagCompensationEnabled();
    double getSmartLagTPSThreshold();
    boolean isSmartLagItemPickupDelayEnabled();
    boolean isSmartLagPotionEffectsEnabled();
    boolean isSmartLagTimeAccelerationEnabled();
    boolean isSmartLagDebugEnabled();
    boolean isSmartLagLogMissedTicks();
    boolean isSmartLagLogCompensation();

    Object getBlockTickSmoothingScheduler();
    Object getEntityTickSmoothingScheduler();
    Object getBlockEntitySmoothingScheduler();
    boolean submitSmoothTask(Object scheduler, Runnable task, int priority, String category);
    int submitSmoothTaskBatch(Object scheduler, java.util.List<Runnable> tasks, int priority, String category);
    void notifySmoothSchedulerTick(Object scheduler);
    void updateSmoothSchedulerMetrics(Object scheduler, double tps, double mspt);

    void clearSakuraOptimizationCaches();
    java.util.Map<String, Object> getSakuraCacheStatistics();
    void performSakuraCacheCleanup();

    boolean isPalettedContainerLockRemovalEnabled();
    boolean isSpawnDensityArrayEnabled();
    boolean isTypeFilterableListOptimizationEnabled();
    boolean isEntityTrackerLinkedHashMapEnabled();
    boolean isBiomeAccessOptimizationEnabled();

    String getBlockId(net.minecraft.world.level.block.Block block);
}
