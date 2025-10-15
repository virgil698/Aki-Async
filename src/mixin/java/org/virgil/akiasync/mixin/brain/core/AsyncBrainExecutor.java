package org.virgil.akiasync.mixin.brain.core;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.virgil.akiasync.mixin.metrics.AsyncMetrics;

/**
 * Async Brain executor
 * Provides timeout-controlled async execution with guaranteed return within specified time
 * 
 * Core features:
 * - 100μs timeout protection (configurable)
 * - CompletableFuture async pattern
 * - Immediate fallback on timeout, no main thread blocking
 * - Statistics tracking for success and timeout rates
 * 
 * @author Virgil
 */
public class AsyncBrainExecutor {
    
    // Statistics
    private static final AtomicInteger totalExecutions = new AtomicInteger(0);
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger timeoutCount = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    
    // Executor (injected by Bridge)
    private static volatile ExecutorService executorService = null;
    
    /**
     * Set executor (called during Bridge initialization)
     */
    public static void setExecutor(ExecutorService executor) {
        executorService = executor;
    }
    
    /**
     * Async execution with sync wait for result (0-latency mode)
     * Optimization: 100μs timeout + immediate sync fallback
     * 
     * @param task Task to execute
     * @param timeout Timeout duration
     * @param unit Time unit
     * @return Future with execution result
     */
    public static <T> CompletableFuture<T> runSync(Callable<T> task, long timeout, TimeUnit unit) {
        totalExecutions.incrementAndGet();
        long startNanos = org.virgil.akiasync.mixin.metrics.AsyncMetrics.recordAsyncStart();
        
        if (executorService == null || executorService.isShutdown()) {
            // Fallback: executor unavailable, execute synchronously
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
        
        // Async execution + timeout control (shorter timeout)
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executorService)
        .orTimeout(timeout, unit)
        .whenComplete((result, throwable) -> {
            boolean success = throwable == null;
            boolean isTimeout = throwable instanceof TimeoutException;
            
            if (isTimeout) {
                timeoutCount.incrementAndGet();
            } else if (!success) {
                errorCount.incrementAndGet();
            } else {
                successCount.incrementAndGet();
            }
            
            // Record metrics
            org.virgil.akiasync.mixin.metrics.AsyncMetrics.recordAsyncEnd(startNanos, success, isTimeout);
        });
    }
    
    /**
     * Sync wait (optimized version): immediate sync execution on timeout
     */
    public static <T> T getWithTimeoutOrRunSync(CompletableFuture<T> future, long timeout, TimeUnit unit, Callable<T> fallbackTask) {
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            // Timeout: cancel async, execute sync immediately
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
     * Sync wait and get result (main thread invocation)
     * 
     * @param future Future object
     * @param timeout Timeout duration
     * @param unit Time unit
     * @return Execution result, null on timeout
     */
    public static <T> T getWithTimeout(CompletableFuture<T> future, long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            // Timeout: cancel task, return null
            future.cancel(true);
            return null;
        } catch (Exception e) {
            // Other exceptions: return null
            return null;
        }
    }
    
    /**
     * Get statistics
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
     * Reset statistics
     */
    public static void resetStatistics() {
        totalExecutions.set(0);
        successCount.set(0);
        timeoutCount.set(0);
        errorCount.set(0);
    }
}

