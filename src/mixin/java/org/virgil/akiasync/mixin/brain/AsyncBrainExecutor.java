package org.virgil.akiasync.mixin.brain;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步Brain执行器
 * 提供带超时控制的异步执行能力，保证在指定时间内返回
 * 
 * 核心特性：
 * - 500μs超时保护（可配置）
 * - CompletableFuture异步模式
 * - 超时立即回落，不阻塞主线程
 * - 统计成功率和超时率
 * 
 * @author Virgil
 */
public class AsyncBrainExecutor {
    
    // 统计信息
    private static final AtomicInteger totalExecutions = new AtomicInteger(0);
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger timeoutCount = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    
    // 执行器（由Bridge注入）
    private static volatile ExecutorService executorService = null;
    
    /**
     * 设置执行器（由Bridge初始化时调用）
     */
    public static void setExecutor(ExecutorService executor) {
        executorService = executor;
    }
    
    /**
     * 异步执行并同步等待结果（0延迟模式）
     * 优化：200μs 超时 + 立即同步fallback
     * 
     * @param task 要执行的任务
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 执行结果的Future
     */
    public static <T> CompletableFuture<T> runSync(Callable<T> task, long timeout, TimeUnit unit) {
        totalExecutions.incrementAndGet();
        
        if (executorService == null || executorService.isShutdown()) {
            // Fallback：执行器不可用，直接同步执行
            return CompletableFuture.supplyAsync(() -> {
                try {
                    T result = task.call();
                    successCount.incrementAndGet();
                    return result;
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    throw new CompletionException(e);
                }
            });
        }
        
        // 异步执行 + 超时控制（更短的超时时间）
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executorService)
        .orTimeout(timeout, unit)
        .whenComplete((result, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof TimeoutException) {
                    timeoutCount.incrementAndGet();
                } else {
                    errorCount.incrementAndGet();
                }
            } else {
                successCount.incrementAndGet();
            }
        });
    }
    
    /**
     * 同步等待（优化版）：超时立即同步执行
     */
    public static <T> T getWithTimeoutOrRunSync(CompletableFuture<T> future, long timeout, TimeUnit unit, Callable<T> fallbackTask) {
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            // 超时：取消异步，立即同步执行
            future.cancel(true);
            timeoutCount.incrementAndGet();
            try {
                return fallbackTask != null ? fallbackTask.call() : null;
            } catch (Exception ex) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 同步等待并获取结果（主线程调用）
     * 
     * @param future Future对象
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 执行结果，超时返回null
     */
    public static <T> T getWithTimeout(CompletableFuture<T> future, long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            // 超时：取消任务，返回null
            future.cancel(true);
            return null;
        } catch (Exception e) {
            // 其他异常：返回null
            return null;
        }
    }
    
    /**
     * 获取统计信息
     */
    public static String getStatistics() {
        int total = totalExecutions.get();
        if (total == 0) return "AsyncBrain: No executions yet";
        
        int success = successCount.get();
        int timeout = timeoutCount.get();
        int error = errorCount.get();
        
        double successRate = (success * 100.0) / total;
        double timeoutRate = (timeout * 100.0) / total;
        
        return String.format(
            "AsyncBrain[Total: %d | Success: %d(%.1f%%) | Timeout: %d(%.1f%%) | Error: %d]",
            total, success, successRate, timeout, timeoutRate, error
        );
    }
    
    /**
     * 重置统计信息
     */
    public static void resetStatistics() {
        totalExecutions.set(0);
        successCount.set(0);
        timeoutCount.set(0);
        errorCount.set(0);
    }
}

