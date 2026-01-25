package org.virgil.akiasync.mixin.metrics;

import java.util.concurrent.atomic.AtomicLong;

public final class AsyncMetrics {

    private static final AtomicLong totalExecutions = new AtomicLong(0);
    private static final AtomicLong successCount = new AtomicLong(0);
    private static final AtomicLong timeoutCount = new AtomicLong(0);

    public static long recordAsyncStart() {
        totalExecutions.incrementAndGet();
        return System.nanoTime();
    }

    public static void recordAsyncEnd(long startNanos, boolean success, boolean timeout) {
        if (success) {
            successCount.incrementAndGet();
        }
        if (timeout) {
            timeoutCount.incrementAndGet();
        }
    }

    public static void reset() {
        totalExecutions.set(0);
        successCount.set(0);
        timeoutCount.set(0);
    }
}
