package org.virgil.akiasync.mixin.brain.core;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
public class AsyncBrainExecutor {
    private static final AtomicInteger totalExecutions = new AtomicInteger(0);
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger timeoutCount = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    private static volatile ExecutorService executorService = null;
    public static void setExecutor(ExecutorService executor) {
        executorService = executor;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[AkiAsync-Brain] Executor set: " + (executor != null ? executor.getClass().getSimpleName() : "null"));
        }
    }
    private static boolean getDebugEnabled() {
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            return bridge != null && bridge.isDebugLoggingEnabled();
        } catch (Exception e) {
            return false;
        }
    }
    public static <T> CompletableFuture<T> runSync(Callable<T> task, long timeout, TimeUnit unit) {
        totalExecutions.incrementAndGet();
        long startNanos = org.virgil.akiasync.mixin.metrics.AsyncMetrics.recordAsyncStart();
        
        if (executorService == null || executorService.isShutdown()) {
            try {
                T result = task.call();
                successCount.incrementAndGet();
                org.virgil.akiasync.mixin.metrics.AsyncMetrics.recordAsyncEnd(startNanos, true, false);
                return CompletableFuture.completedFuture(result);
            } catch (Exception e) {
                errorCount.incrementAndGet();
                org.virgil.akiasync.mixin.metrics.AsyncMetrics.recordAsyncEnd(startNanos, false, false);
                return CompletableFuture.failedFuture(e);
            }
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                T result = task.call();
                successCount.incrementAndGet();
                org.virgil.akiasync.mixin.metrics.AsyncMetrics.recordAsyncEnd(startNanos, true, false);
                return result;
            } catch (Exception e) {
                errorCount.incrementAndGet();
                org.virgil.akiasync.mixin.metrics.AsyncMetrics.recordAsyncEnd(startNanos, false, false);
                throw new CompletionException(e);
            }
        }, executorService)
        .orTimeout(timeout, unit)
        .exceptionally(throwable -> {
            if (throwable instanceof TimeoutException || throwable.getCause() instanceof TimeoutException) {
                timeoutCount.incrementAndGet();
                org.virgil.akiasync.mixin.metrics.AsyncMetrics.recordAsyncEnd(startNanos, false, true);
            }
            return null;
        });
    }
    public static <T> T getWithTimeoutOrRunSync(CompletableFuture<T> future, long timeout, TimeUnit unit, Callable<T> fallbackTask) {
        try {
            T result = future.get(timeout, unit);
            successCount.incrementAndGet();
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            timeoutCount.incrementAndGet();
            try {
                return fallbackTask != null ? fallbackTask.call() : null;
            } catch (Exception ex) {
                errorCount.incrementAndGet();
                return null;
            }
        } catch (Exception e) {
            errorCount.incrementAndGet();
            return null;
        }
    }
    public static <T> T getWithTimeout(CompletableFuture<T> future, long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            return null;
        } catch (Exception e) {
            return null;
        }
    }
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
    public static void resetStatistics() {
        totalExecutions.set(0);
        successCount.set(0);
        timeoutCount.set(0);
        errorCount.set(0);
    }
    public static void restartSmooth() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[AkiAsync-Debug] AsyncBrainExecutor restart - resetting statistics");
        }

        resetStatistics();
        
        if (bridge != null) {
            executorService = bridge.getBrainExecutor();
            bridge.debugLog("[AkiAsync-Debug] AsyncBrainExecutor restart completed");
        }
    }
}
