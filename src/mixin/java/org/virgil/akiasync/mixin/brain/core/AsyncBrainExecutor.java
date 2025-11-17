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
        final boolean debugEnabled = getDebugEnabled();
        if (executorService == null || executorService.isShutdown()) {
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
            if (debugEnabled) {
                long duration = System.nanoTime() - startNanos;
            }
            org.virgil.akiasync.mixin.metrics.AsyncMetrics.recordAsyncEnd(startNanos, success, isTimeout);
        });
    }
    public static <T> T getWithTimeoutOrRunSync(CompletableFuture<T> future, long timeout, TimeUnit unit, Callable<T> fallbackTask) {
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
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
            bridge.debugLog("[AkiAsync-Debug] Starting AsyncBrainExecutor smooth restart...");
        }
        
        if (executorService != null) {
            ExecutorService oldExecutor = executorService;
            
            executorService = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "AkiAsync-Brain-Smooth");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });
            
            oldExecutor.shutdown();
            try {
                if (!oldExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    org.virgil.akiasync.mixin.bridge.Bridge brainForceShutdownBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    if (brainForceShutdownBridge != null) {
                        brainForceShutdownBridge.debugLog("[AkiAsync-Debug] AsyncBrainExecutor force shutdown");
                    }
                    oldExecutor.shutdownNow();
                } else {
                    org.virgil.akiasync.mixin.bridge.Bridge brainGracefulShutdownBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    if (brainGracefulShutdownBridge != null) {
                        brainGracefulShutdownBridge.debugLog("[AkiAsync-Debug] AsyncBrainExecutor gracefully shutdown");
                    }
                }
            } catch (InterruptedException e) {
                oldExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            resetStatistics();
            org.virgil.akiasync.mixin.bridge.Bridge bridge2 = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge2 != null) {
            bridge2.debugLog("[AkiAsync-Debug] AsyncBrainExecutor restart completed");
        }
        }
    }
}