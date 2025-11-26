package org.virgil.akiasync.executor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.compat.FoliaExecutorAdapter;
public class AsyncExecutorManager {
    private final AkiAsyncPlugin plugin;
    private final ThreadPoolExecutor executorService;
    private final ThreadPoolExecutor lightingExecutor;
    private final ExecutorService tntExecutor;
    private final ExecutorService chunkTickExecutor;
    private final ExecutorService villagerBreedExecutor;
    private final ExecutorService brainExecutor;
    private final ScheduledExecutorService metricsExecutor;
    public AsyncExecutorManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        int threadPoolSize = plugin.getConfigManager().getThreadPoolSize();
        int maxQueueSize = plugin.getConfigManager().getMaxQueueSize();
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "AkiAsync-Worker-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        };
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(maxQueueSize);
        this.executorService = new ThreadPoolExecutor(
            threadPoolSize,
            threadPoolSize,
            60L, TimeUnit.SECONDS,
            workQueue,
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        int prestarted = executorService.prestartAllCoreThreads();
        int lightingThreads = plugin.getConfigManager().getLightingThreadPoolSize();
        ThreadFactory lightingThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "AkiAsync-Lighting-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        };
        this.lightingExecutor = new ThreadPoolExecutor(
            lightingThreads,
            lightingThreads,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            lightingThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        int lightingPrestarted = lightingExecutor.prestartAllCoreThreads();

        int tntThreads = plugin.getConfigManager().getTNTThreads();
        this.tntExecutor = new FoliaExecutorAdapter(plugin, tntThreads, "AkiAsync-TNT");

        this.chunkTickExecutor = new FoliaExecutorAdapter(plugin, 4, "AkiAsync-ChunkTick");
        this.villagerBreedExecutor = new FoliaExecutorAdapter(plugin, 4, "AkiAsync-VillagerBreed");
        this.brainExecutor = new FoliaExecutorAdapter(plugin, threadPoolSize / 2, "AkiAsync-Brain");

        this.metricsExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AkiAsync-Metrics");
            thread.setDaemon(true);
            return thread;
        });
        plugin.getLogger().info("General executor initialized: " + threadPoolSize + " threads (prestarted: " + prestarted + ")");
        plugin.getLogger().info("Lighting executor initialized: " + lightingThreads + " threads (prestarted: " + lightingPrestarted + ")");
        plugin.getLogger().info("TNT executor initialized: " + tntThreads + " threads (Folia-compatible)");
        plugin.getLogger().info("ChunkTick executor initialized: 4 threads (Folia-compatible)");
        plugin.getLogger().info("VillagerBreed executor initialized: 4 threads (Folia-compatible)");
        plugin.getLogger().info("Brain executor initialized: " + (threadPoolSize / 2) + " threads (Folia-compatible)");
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
        executorService.shutdown();
        lightingExecutor.shutdown();
        tntExecutor.shutdown();
        chunkTickExecutor.shutdown();
        villagerBreedExecutor.shutdown();
        brainExecutor.shutdown();
        metricsExecutor.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("General executor did not terminate in time, forcing shutdown...");
                executorService.shutdownNow();
            }
            if (!lightingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Lighting executor did not terminate in time, forcing shutdown...");
                lightingExecutor.shutdownNow();
            }
            if (!tntExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("TNT executor did not terminate in time, forcing shutdown...");
                tntExecutor.shutdownNow();
            }
            if (!chunkTickExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("ChunkTick executor did not terminate in time, forcing shutdown...");
                chunkTickExecutor.shutdownNow();
            }
            if (!villagerBreedExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("VillagerBreed executor did not terminate in time, forcing shutdown...");
                villagerBreedExecutor.shutdownNow();
            }
            if (!brainExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Brain executor did not terminate in time, forcing shutdown...");
                brainExecutor.shutdownNow();
            }
            metricsExecutor.shutdownNow();
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            lightingExecutor.shutdownNow();
            tntExecutor.shutdownNow();
            chunkTickExecutor.shutdownNow();
            villagerBreedExecutor.shutdownNow();
            brainExecutor.shutdownNow();
            metricsExecutor.shutdownNow();
            Thread.currentThread().interrupt();
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
            "Pool: %d/%d | Active: %d | Queue: %d | Completed: %d | Total: %d",
            executorService.getPoolSize(),
            executorService.getCorePoolSize(),
            executorService.getActiveCount(),
            executorService.getQueue().size(),
            executorService.getCompletedTaskCount(),
            executorService.getTaskCount()
        );
    }
    public boolean isShutdown() {
        return executorService.isShutdown();
    }
    public void restartSmooth() {
        plugin.getLogger().info("[AkiAsync] Starting smooth restart of async executors...");
        int threadPoolSize = plugin.getConfigManager().getThreadPoolSize();
        int maxQueueSize = plugin.getConfigManager().getMaxQueueSize();
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "AkiAsync-Smooth-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        };
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(maxQueueSize);
        ThreadPoolExecutor newExecutor = new ThreadPoolExecutor(
            threadPoolSize,
            threadPoolSize,
            60L, TimeUnit.SECONDS,
            workQueue,
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        int lightingThreads = plugin.getConfigManager().getLightingThreadPoolSize();
        ThreadFactory lightingThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "AkiAsync-Lighting-Smooth-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        };
        ThreadPoolExecutor newLightingExecutor = new ThreadPoolExecutor(
            lightingThreads,
            lightingThreads,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            lightingThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        ThreadPoolExecutor oldExecutor = executorService;
        ThreadPoolExecutor oldLightingExecutor = lightingExecutor;
        oldExecutor.shutdown();
        oldLightingExecutor.shutdown();
        try {
            if (!oldExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                plugin.getLogger().warning("General executor did not terminate in time, forcing shutdown...");
                oldExecutor.shutdownNow();
            }
            if (!oldLightingExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                plugin.getLogger().warning("Lighting executor did not terminate in time, forcing shutdown...");
                oldLightingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            oldExecutor.shutdownNow();
            oldLightingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            java.lang.reflect.Field executorField = AsyncExecutorManager.class.getDeclaredField("executorService");
            executorField.setAccessible(true);
            executorField.set(this, newExecutor);
            java.lang.reflect.Field lightingField = AsyncExecutorManager.class.getDeclaredField("lightingExecutor");
            lightingField.setAccessible(true);
            lightingField.set(this, newLightingExecutor);
            plugin.getLogger().info("[AkiAsync] Executors smoothly restarted with new configuration");
            plugin.getLogger().info("  - General executor: " + threadPoolSize + " threads");
            plugin.getLogger().info("  - Lighting executor: " + lightingThreads + " threads");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to replace executor references: " + e.getMessage());
            newExecutor.shutdownNow();
            newLightingExecutor.shutdownNow();
        }
    }
}