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
 * AI专用线程池管理器
 * 
 * 核心优化：
 * 1. 独立池：与光照分离，避免相互影响
 * 2. 有界队列：LinkedBlockingQueue(256) → 防止任务堆积
 * 3. CallerRunsPolicy：满了立即同步，不堵 TPS
 * 
 * @author Virgil
 */
public class AIExecutorManager {
    
    private final AkiAsyncPlugin plugin;
    private final ThreadPoolExecutor aiExecutor;
    
    public AIExecutorManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        
        // AI线程池：4线程 + 有界队列(256)
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
            new LinkedBlockingQueue<>(256),  // 有界队列，防止爆炸
            aiThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()  // 满了就同步，不堵TPS
        );
        
        // 预启动所有线程
        int prestarted = aiExecutor.prestartAllCoreThreads();
        plugin.getLogger().info("AI executor initialized: " + aiThreads + " threads (prestarted: " + prestarted + ")");
    }
    
    /**
     * 提交AI任务（带超时）
     * 
     * @param task 任务
     * @param timeoutMicros 超时时间（微秒）
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
     * 同步等待结果（主线程调用）
     * 
     * @param future Future对象
     * @param timeoutMicros 超时时间（微秒）
     * @param fallback 超时回调（立即同步执行）
     * @return 结果，超时返回null并执行fallback
     */
    public <T> T getOrRunSync(CompletableFuture<T> future, long timeoutMicros, Runnable fallback) {
        try {
            return future.get(timeoutMicros, TimeUnit.MICROSECONDS);
        } catch (TimeoutException e) {
            // 超时：立即同步执行，不堵TPS
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
     * 获取执行器
     */
    public ExecutorService getExecutor() {
        return aiExecutor;
    }
    
    /**
     * 关闭
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
     * 获取统计信息
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

