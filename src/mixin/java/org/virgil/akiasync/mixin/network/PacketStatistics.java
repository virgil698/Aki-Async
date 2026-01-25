package org.virgil.akiasync.mixin.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PacketStatistics {

    private static final Map<String, PacketCounter> outgoingStats = new ConcurrentHashMap<>();
    private static final Map<String, PacketCounter> incomingStats = new ConcurrentHashMap<>();

    private static volatile boolean enabled = false;
    private static volatile long startTime = 0;

    private PacketStatistics() {
    }

    public static void setEnabled(boolean enable) {
        if (enable && !enabled) {
            reset();
            startTime = System.currentTimeMillis();
        }
        enabled = enable;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void recordOutgoing(String packetName, int estimatedSize) {
        if (!enabled) return;
        outgoingStats.computeIfAbsent(packetName, k -> new PacketCounter())
                     .record(estimatedSize);
    }

    public static void recordIncoming(String packetName, int estimatedSize) {
        if (!enabled) return;
        incomingStats.computeIfAbsent(packetName, k -> new PacketCounter())
                     .record(estimatedSize);
    }

    public static void reset() {
        outgoingStats.clear();
        incomingStats.clear();
        startTime = System.currentTimeMillis();
    }

    public static long getElapsedSeconds() {
        return Math.max(1, (System.currentTimeMillis() - startTime) / 1000);
    }

    public static List<PacketStat> getTopOutgoing(int limit) {
        return getTopStats(outgoingStats, limit);
    }

    public static List<PacketStat> getTopIncoming(int limit) {
        return getTopStats(incomingStats, limit);
    }

    private static List<PacketStat> getTopStats(Map<String, PacketCounter> stats, int limit) {
        long elapsed = getElapsedSeconds();
        List<PacketStat> result = new ArrayList<>();

        for (Map.Entry<String, PacketCounter> entry : stats.entrySet()) {
            PacketCounter counter = entry.getValue();
            result.add(new PacketStat(
                entry.getKey(),
                counter.getCount(),
                counter.getTotalBytes(),
                counter.getCount() / elapsed,
                counter.getTotalBytes() / elapsed
            ));
        }

        result.sort(Comparator.comparingLong(PacketStat::count).reversed());

        if (result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }

    public static long getTotalOutgoingCount() {
        return outgoingStats.values().stream()
                           .mapToLong(PacketCounter::getCount)
                           .sum();
    }

    public static long getTotalIncomingCount() {
        return incomingStats.values().stream()
                           .mapToLong(PacketCounter::getCount)
                           .sum();
    }

    public static long getTotalOutgoingBytes() {
        return outgoingStats.values().stream()
                           .mapToLong(PacketCounter::getTotalBytes)
                           .sum();
    }

    public static long getTotalIncomingBytes() {
        return incomingStats.values().stream()
                           .mapToLong(PacketCounter::getTotalBytes)
                           .sum();
    }

    public static class PacketCounter {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalBytes = new AtomicLong(0);

        public void record(int bytes) {
            count.incrementAndGet();
            totalBytes.addAndGet(bytes);
        }

        public long getCount() {
            return count.get();
        }

        public long getTotalBytes() {
            return totalBytes.get();
        }
    }

    public record PacketStat(
        String name,
        long count,
        long totalBytes,
        long countPerSecond,
        long bytesPerSecond
    ) {}
}
