package org.virgil.akiasync.mixin.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Async AI metrics collector.
 * 
 * @author Virgil
 */
public final class AsyncMetrics {
    
    private static final AtomicLong currentMspt = new AtomicLong(0);
    private static final AtomicLong lastTickTime = new AtomicLong(System.nanoTime());
    
    private static final AtomicLong totalExecutions = new AtomicLong(0);
    private static final AtomicLong successCount = new AtomicLong(0);
    private static final AtomicLong timeoutCount = new AtomicLong(0);
    private static final AtomicLong fallbackCount = new AtomicLong(0);
    
    private static final AtomicLong asyncCpuNanos = new AtomicLong(0);
    private static final AtomicLong totalCpuNanos = new AtomicLong(0);
    
    public static void recordTickMspt() {
        long now = System.nanoTime();
        long elapsed = now - lastTickTime.get();
        currentMspt.set(elapsed / 1_000_000);
        lastTickTime.set(now);
        totalCpuNanos.addAndGet(elapsed);
    }
    
    public static long recordAsyncStart() {
        totalExecutions.incrementAndGet();
        return System.nanoTime();
    }
    
    public static void recordAsyncEnd(long startNanos, boolean success, boolean timeout) {
        long elapsed = System.nanoTime() - startNanos;
        asyncCpuNanos.addAndGet(elapsed);
        
        if (success) {
            successCount.incrementAndGet();
        }
        if (timeout) {
            timeoutCount.incrementAndGet();
            fallbackCount.incrementAndGet();
        }
    }
    
    public static long getCurrentMspt() {
        return currentMspt.get();
    }
    
    public static double getAsyncCpuPercent() {
        long total = totalCpuNanos.get();
        if (total == 0) return 0.0;
        return (asyncCpuNanos.get() * 100.0) / total;
    }
    
    public static double getSuccessRate() {
        long total = totalExecutions.get();
        if (total == 0) return 100.0;
        return (successCount.get() * 100.0) / total;
    }
    
    public static long getFallbackCount() {
        return fallbackCount.get();
    }
    
    public static String getPrometheusMetrics() {
        return String.format(
            "# HELP akiasync_mspt_current Current tick MSPT in milliseconds\n" +
            "# TYPE akiasync_mspt_current gauge\n" +
            "akiasync_mspt_current %d\n" +
            "\n" +
            "# HELP akiasync_async_cpu_percent CompletableFuture CPU usage percentage\n" +
            "# TYPE akiasync_async_cpu_percent gauge\n" +
            "akiasync_async_cpu_percent %.2f\n" +
            "\n" +
            "# HELP akiasync_fallback_count Timeout fallback count\n" +
            "# TYPE akiasync_fallback_count counter\n" +
            "akiasync_fallback_count %d\n" +
            "\n" +
            "# HELP akiasync_success_rate Async success rate percentage\n" +
            "# TYPE akiasync_success_rate gauge\n" +
            "akiasync_success_rate %.2f\n",
            getCurrentMspt(),
            getAsyncCpuPercent(),
            getFallbackCount(),
            getSuccessRate()
        );
    }
    
    public static void reset() {
        totalExecutions.set(0);
        successCount.set(0);
        timeoutCount.set(0);
        fallbackCount.set(0);
        asyncCpuNanos.set(0);
        totalCpuNanos.set(0);
    }
}
