package org.virgil.akiasync.mixin.network;

import java.util.concurrent.atomic.AtomicLong;

public class NetworkTrafficTracker {

    private static final AtomicLong totalBytesIn = new AtomicLong(0);
    private static final AtomicLong totalBytesOut = new AtomicLong(0);
    private static final AtomicLong lastBytesIn = new AtomicLong(0);
    private static final AtomicLong lastBytesOut = new AtomicLong(0);

    private static volatile long currentInRate = 0;
    private static volatile long currentOutRate = 0;
    private static volatile long lastCalculateTime = System.currentTimeMillis();

    private NetworkTrafficTracker() {
    }

    public static void recordIncoming(long bytes) {
        totalBytesIn.addAndGet(bytes);
    }

    public static void recordOutgoing(long bytes) {
        totalBytesOut.addAndGet(bytes);
    }

    public static void calculateRates() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastCalculateTime;

        if (elapsed >= 1000) {
            long currentIn = totalBytesIn.get();
            long currentOut = totalBytesOut.get();

            currentInRate = (currentIn - lastBytesIn.get()) * 1000 / elapsed;
            currentOutRate = (currentOut - lastBytesOut.get()) * 1000 / elapsed;

            lastBytesIn.set(currentIn);
            lastBytesOut.set(currentOut);
            lastCalculateTime = now;
        }
    }

    public static long getCurrentInRate() {
        return currentInRate;
    }

    public static long getCurrentOutRate() {
        return currentOutRate;
    }

    public static long getTotalBytesIn() {
        return totalBytesIn.get();
    }

    public static long getTotalBytesOut() {
        return totalBytesOut.get();
    }
}
