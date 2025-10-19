package org.virgil.akiasync.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.virgil.akiasync.AkiAsyncPlugin;

/**
 * AI dedicated thread pool manager
 * 
 * Core optimizations:
 * 1. Independent pool: Separated from lighting, avoid mutual interference
 * 2. Bounded queue: LinkedBlockingQueue(256) â†?Prevent task accumulation
 * 3. CallerRunsPolicy: Execute sync immediately when full, no TPS blocking
 * 
 * @author Virgil
 */
public class AIExecutorManager {
    
    private final AkiAsyncPlugin plugin;
    private final ThreadPoolExecutor aiExecutor;
    
    public AIExecutorManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        
        int aiThreads = 4;
        ThreadFactory aiThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "AkiAI-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        };
        
        this.aiExecutor = new ThreadPoolExecutor(
            aiThreads,
            aiThreads,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(256),
            aiThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        int prestarted = aiExecutor.prestartAllCoreThreads();
        plugin.getLogger().info("AI executor initialized: " + aiThreads + " threads (prestarted: " + prestarted + ")");
    }
    
    /**
     * Submit AI task (with timeout)
     * 
     * @param task Task to execute
     * @param timeoutMicros Timeout in microseconds
     * @return Future
     */
    public <T> CompletableFuture<T> submitWithTimeout(Callable<T> task, long timeoutMicros) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, aiExecutor)
        .orTimeout(timeoutMicros, TimeUnit.MICROSECONDS);
    }
    
    /**
     * Sync wait for result (main thread invocation)
     * 
     * @param future Future object
     * @param timeoutMicros Timeout in microseconds
     * @param fallback Timeout callback (execute sync immediately)
     * @return Result, null on timeout with fallback executed
     */
    public <T> T getOrRunSync(CompletableFuture<T> future, long timeoutMicros, Runnable fallback) {
        try {
            return future.get(timeoutMicros, TimeUnit.MICROSECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            if (fallback != null) {
                fallback.run();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get executor
     */
    public ExecutorService getExecutor() {
        return aiExecutor;
    }
    
    /**
     * Shutdown
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down AI executor...");
        aiExecutor.shutdown();
        try {
            if (!aiExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                aiExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            aiExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        plugin.getLogger().info("AI executor shut down successfully");
    }
    
    /**
     * Get statistics
     */
    public String getStatistics() {
        return String.format(
            "AI Pool: %d/%d | Active: %d | Queue: %d | Completed: %d",
            aiExecutor.getPoolSize(),
            aiExecutor.getCorePoolSize(),
            aiExecutor.getActiveCount(),
            aiExecutor.getQueue().size(),
            aiExecutor.getCompletedTaskCount()
        );
    }
}

