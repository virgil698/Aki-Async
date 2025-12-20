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
                thread.setPriority(Thread.NORM_PRIORITY - 2);
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
    public ExecutorService getExecutor() {
        return aiExecutor;
    }
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
