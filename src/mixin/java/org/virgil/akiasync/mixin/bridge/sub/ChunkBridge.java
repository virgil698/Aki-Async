package org.virgil.akiasync.mixin.bridge.sub;

public interface ChunkBridge {

    boolean isChunkPosOptimizationEnabled();
    boolean isChunkOptimizationEnabled();

    boolean isSpawnChunkRemovalEnabled();
    boolean isPlayerChunkLoadingOptimizationEnabled();
    int getMaxConcurrentChunkLoadsPerPlayer();

    boolean isChunkTickAsyncEnabled();
    int getChunkTickThreads();
    long getChunkTickTimeoutMicros();
    int getChunkTickAsyncBatchSize();

    int getMidTickChunkTasksIntervalMs();

    boolean isChunkVisibilityFilterEnabled();
    boolean isChunkVisible(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, net.minecraft.server.level.ServerLevel level);
    void tickChunkVisibilityFilter();

    boolean isFastMovementChunkLoadEnabled();
    double getFastMovementSpeedThreshold();
    int getFastMovementPreloadDistance();
    int getFastMovementMaxConcurrentLoads();
    int getFastMovementPredictionTicks();
    boolean isCenterOffsetEnabled();
    double getMinOffsetSpeed();
    double getMaxOffsetSpeed();
    double getMaxOffsetRatio();

    boolean isPlayerJoinWarmupEnabled();
    long getPlayerJoinWarmupDurationMs();
    double getPlayerJoinWarmupInitialRate();

    void submitChunkLoad(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, int priority, double speed);

    void clearWorldCaches(String worldName);
}
