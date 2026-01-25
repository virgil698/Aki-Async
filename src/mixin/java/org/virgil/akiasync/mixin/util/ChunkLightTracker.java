package org.virgil.akiasync.mixin.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ChunkLightTracker {
    private final AtomicLong lastUpdateTime = new AtomicLong(0);
    private final AtomicInteger recentUpdateCount = new AtomicInteger(0);
    private volatile long windowStartTime = 0;

    public void recordUpdate(long currentTime) {
        lastUpdateTime.set(currentTime);

        if (currentTime - windowStartTime > 100_000_000L) {
            windowStartTime = currentTime;
            recentUpdateCount.set(1);
        } else {
            recentUpdateCount.incrementAndGet();
        }
    }

    public boolean isInBurstMode(long currentTime, long windowNanos, int threshold) {
        if (currentTime - windowStartTime > windowNanos) {
            return false;
        }
        return recentUpdateCount.get() > threshold;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime.get();
    }
}
