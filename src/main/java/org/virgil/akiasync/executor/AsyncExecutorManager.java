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

/**
 * Manages async executor service for entity tracker and other async operations
 * Provides thread-safe async task execution with monitoring capabilities
 * 
 * @author Virgil
 */
public class AsyncExecutorManager {
    
    private final AkiAsyncPlugin plugin;
    private final ThreadPoolExecutor executorService;
    private final ThreadPoolExecutor lightingExecutor;
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
        
        this.metricsExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AkiAsync-Metrics");
            thread.setDaemon(true);
            return thread;
        });
        
        plugin.getLogger().info("General executor initialized: " + threadPoolSize + " threads (prestarted: " + prestarted + ")");
        plugin.getLogger().info("Lighting executor initialized: " + lightingThreads + " threads (prestarted: " + lightingPrestarted + ")");
    }
    
    /**
     * Submit a task for async execution.
     * @param task The task to execute
     * @return Future representing the task
     */
    public Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }
    
    /**
     * Submit a callable task for async execution
     * @param task The task to execute
     * @return Future with the result
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }
    
    /**
     * Execute a task asynchronously without returning a Future
     * @param task The task to execute
     */
    public void execute(Runnable task) {
        executorService.execute(task);
    }
    
    /**
     * Shutdown the executor gracefully
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down async executors...");
        
        executorService.shutdown();
        lightingExecutor.shutdown();
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
            metricsExecutor.shutdownNow();
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            lightingExecutor.shutdownNow();
            metricsExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        plugin.getLogger().info("Async executors shut down successfully");
    }
    
    /**
     * Get the general executor service
     * @return ExecutorService instance
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }
    
    /**
     * Get the lighting executor service
     * @return Lighting ExecutorService instance
     */
    public ExecutorService getLightingExecutor() {
        return lightingExecutor;
    }
    
    /**
     * Get current executor statistics
     * @return Formatted statistics string
     */
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
    
    /**
     * Check if executor is shutdown
     * @return true if shutdown
     */
    public boolean isShutdown() {
        return executorService.isShutdown();
    }
}

