package org.virgil.akiasync.executor;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.compat.FoliaExecutorAdapter;
import org.virgil.akiasync.util.resource.ExecutorLifecycleManager;
import org.virgil.akiasync.util.resource.ResourceTracker;
public class AsyncExecutorManager {
    private final AkiAsyncPlugin plugin;
    private final ForkJoinPool executorService;  
    private final ForkJoinPool lightingExecutor;  
    private final ExecutorService tntExecutor;
    private final ExecutorService chunkTickExecutor;
    private final ExecutorService villagerBreedExecutor;
    private final ExecutorService brainExecutor;
    private final ScheduledExecutorService metricsExecutor;
    public AsyncExecutorManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        int threadPoolSize = plugin.getConfigManager().getThreadPoolSize();
        int lightingThreads = plugin.getConfigManager().getLightingThreadPoolSize();
        int tntThreads = plugin.getConfigManager().getTNTThreads();

        this.executorService = (ForkJoinPool) ResourceTracker.track(
            ExecutorLifecycleManager.createForkJoinPool("AkiAsync-Worker", threadPoolSize, true),
            "AkiAsync-Worker-ForkJoinPool");
        
        this.lightingExecutor = (ForkJoinPool) ResourceTracker.track(
            ExecutorLifecycleManager.createForkJoinPool("AkiAsync-Lighting", lightingThreads, true),
            "AkiAsync-Lighting-ForkJoinPool");

        this.tntExecutor = ResourceTracker.track(
            new FoliaExecutorAdapter(plugin, tntThreads, "AkiAsync-TNT"),
            "AkiAsync-TNT-Executor");
        this.chunkTickExecutor = ResourceTracker.track(
            new FoliaExecutorAdapter(plugin, 4, "AkiAsync-ChunkTick"),
            "AkiAsync-ChunkTick-Executor");
        this.villagerBreedExecutor = ResourceTracker.track(
            new FoliaExecutorAdapter(plugin, 4, "AkiAsync-VillagerBreed"),
            "AkiAsync-VillagerBreed-Executor");
        this.brainExecutor = ResourceTracker.track(
            new FoliaExecutorAdapter(plugin, threadPoolSize / 2, "AkiAsync-Brain"),
            "AkiAsync-Brain-Executor");

        this.metricsExecutor = ResourceTracker.track(
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "AkiAsync-Metrics");
                thread.setDaemon(true);
                return thread;
            }),
            "AkiAsync-Metrics-Executor");
        
        plugin.getLogger().info("General executor initialized: ForkJoinPool with parallelism=" + threadPoolSize + " (work-stealing enabled)");
        plugin.getLogger().info("Lighting executor initialized: ForkJoinPool with parallelism=" + lightingThreads + " (work-stealing enabled)");
        plugin.getLogger().info("TNT executor initialized: " + tntThreads + " threads (Folia-compatible)");
        plugin.getLogger().info("ChunkTick executor initialized: 4 threads (Folia-compatible)");
        plugin.getLogger().info("VillagerBreed executor initialized: 4 threads (Folia-compatible)");
        plugin.getLogger().info("Brain executor initialized: " + (threadPoolSize / 2) + " threads (Folia-compatible)");
        plugin.getLogger().info("All executors tracked by ResourceTracker for leak detection");
    }
    public Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }
    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }
    public void execute(Runnable task) {
        executorService.execute(task);
    }
    public void shutdown() {
        plugin.getLogger().info("Shutting down async executors...");
        
        boolean generalShutdown = ExecutorLifecycleManager.shutdownGracefully(executorService, 10, TimeUnit.SECONDS);
        if (!generalShutdown) {
            plugin.getLogger().warning("General executor did not terminate gracefully");
        }
        
        boolean lightingShutdown = ExecutorLifecycleManager.shutdownGracefully(lightingExecutor, 5, TimeUnit.SECONDS);
        if (!lightingShutdown) {
            plugin.getLogger().warning("Lighting executor did not terminate gracefully");
        }
        
        boolean tntShutdown = ExecutorLifecycleManager.shutdownGracefully(tntExecutor, 5, TimeUnit.SECONDS);
        if (!tntShutdown) {
            plugin.getLogger().warning("TNT executor did not terminate gracefully");
        }
        
        boolean chunkTickShutdown = ExecutorLifecycleManager.shutdownGracefully(chunkTickExecutor, 5, TimeUnit.SECONDS);
        if (!chunkTickShutdown) {
            plugin.getLogger().warning("ChunkTick executor did not terminate gracefully");
        }
        
        boolean villagerBreedShutdown = ExecutorLifecycleManager.shutdownGracefully(villagerBreedExecutor, 5, TimeUnit.SECONDS);
        if (!villagerBreedShutdown) {
            plugin.getLogger().warning("VillagerBreed executor did not terminate gracefully");
        }
        
        boolean brainShutdown = ExecutorLifecycleManager.shutdownGracefully(brainExecutor, 5, TimeUnit.SECONDS);
        if (!brainShutdown) {
            plugin.getLogger().warning("Brain executor did not terminate gracefully");
        }
        
        metricsExecutor.shutdownNow();
        
        java.util.List<String> unclosed = ResourceTracker.getUnclosedResources();
        if (!unclosed.isEmpty()) {
            plugin.getLogger().warning("Found " + unclosed.size() + " unclosed executor resources: " + unclosed);
            plugin.getLogger().warning("Forcing cleanup of unclosed resources...");
            ResourceTracker.closeAll();
        }
        
        plugin.getLogger().info("Async executors shut down successfully");
    }
    public ExecutorService getExecutorService() {
        return executorService;
    }
    public ExecutorService getLightingExecutor() {
        return lightingExecutor;
    }
    public ExecutorService getTNTExecutor() {
        return tntExecutor;
    }
    public ExecutorService getChunkTickExecutor() {
        return chunkTickExecutor;
    }
    public ExecutorService getVillagerBreedExecutor() {
        return villagerBreedExecutor;
    }
    public ExecutorService getBrainExecutor() {
        return brainExecutor;
    }
    public String getStatistics() {
        
        return String.format(
            "ForkJoinPool: Size=%d | Active=%d | Running=%d | Queued=%d | Steal=%d | Parallelism=%d",
            executorService.getPoolSize(),
            executorService.getActiveThreadCount(),
            executorService.getRunningThreadCount(),
            executorService.getQueuedSubmissionCount(),
            executorService.getStealCount(),
            executorService.getParallelism()
        );
    }
    public boolean isShutdown() {
        return executorService.isShutdown();
    }
    public void restartSmooth() {
        plugin.getLogger().info("[AkiAsync] Starting smooth restart of async executors...");
        
        int threadPoolSize = plugin.getConfigManager().getThreadPoolSize();
        int lightingThreads = plugin.getConfigManager().getLightingThreadPoolSize();
        
        plugin.getLogger().warning("[AkiAsync] Smooth restart requires plugin reload to take effect");
        plugin.getLogger().warning("  - Desired general executor threads: " + threadPoolSize);
        plugin.getLogger().warning("  - Desired lighting executor threads: " + lightingThreads);
        plugin.getLogger().warning("  - Please use /reload or restart the server to apply new thread pool sizes");
    }
}
