package org.virgil.akiasync.mixin.bridge.sub;

import java.util.concurrent.ExecutorService;

public interface CoreBridge {

    boolean isNitoriOptimizationsEnabled();
    boolean isVirtualThreadEnabled();
    boolean isWorkStealingEnabled();
    boolean isBlockPosCacheEnabled();
    boolean isOptimizedCollectionsEnabled();
    boolean isPredicateCacheEnabled();
    boolean isBlockPosPoolEnabled();
    boolean isListPreallocEnabled();
    int getListPreallocCapacity();

    ExecutorService getGeneralExecutor();
    ExecutorService getTNTExecutor();
    ExecutorService getChunkTickExecutor();
    ExecutorService getVillagerBreedExecutor();
    ExecutorService getBrainExecutor();
    ExecutorService getCollisionExecutor();
    ExecutorService getLightingExecutor();
    ExecutorService getWorldgenExecutor();

    boolean isFoliaEnvironment();
    boolean isOwnedByCurrentRegion(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos);
    void scheduleRegionTask(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, Runnable task);
    boolean canAccessEntityDirectly(net.minecraft.world.entity.Entity entity);
    boolean canAccessBlockPosDirectly(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos);
    void safeExecute(Runnable task, String context);
    String checkExecutorHealth(ExecutorService executor, String name);

    void runOnMainThread(Runnable task);
    double getCurrentTPS();
    double getCurrentMSPT();

    void debugLog(String message);
    void debugLog(String format, Object... args);
    void errorLog(String message);
    void errorLog(String format, Object... args);
    boolean isDebugLoggingEnabled();

    int getGeneralThreadPoolSize();

    void restartVillagerExecutor();
    void restartTNTExecutor();
    void restartBrainExecutor();
    void restartChunkExecutor(int threadCount);
    void clearVillagerBreedCache();
    void resetBrainExecutorStatistics();
    void resetAsyncMetrics();
}
