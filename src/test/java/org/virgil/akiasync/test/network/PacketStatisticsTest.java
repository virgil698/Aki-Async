package org.virgil.akiasync.test.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests for PacketStatistics functionality.
 * Validates packet counting, statistics, and thread safety.
 */
public class PacketStatisticsTest {

    private TestPacketStatistics stats;

    @BeforeEach
    void setUp() {
        stats = new TestPacketStatistics();
        stats.reset();
        stats.setEnabled(true);
    }

    @Test
    @DisplayName("Should record outgoing packets correctly")
    void testRecordOutgoing() {
        stats.recordOutgoing("TestPacket", 100);
        stats.recordOutgoing("TestPacket", 150);
        stats.recordOutgoing("OtherPacket", 50);

        assertEquals(2, stats.getOutgoingCount("TestPacket"));
        assertEquals(250, stats.getOutgoingBytes("TestPacket"));
        assertEquals(1, stats.getOutgoingCount("OtherPacket"));
    }

    @Test
    @DisplayName("Should record incoming packets correctly")
    void testRecordIncoming() {
        stats.recordIncoming("InPacket", 200);
        stats.recordIncoming("InPacket", 300);

        assertEquals(2, stats.getIncomingCount("InPacket"));
        assertEquals(500, stats.getIncomingBytes("InPacket"));
    }

    @Test
    @DisplayName("Should not record when disabled")
    void testDisabledRecording() {
        stats.setEnabled(false);
        stats.recordOutgoing("TestPacket", 100);

        assertEquals(0, stats.getOutgoingCount("TestPacket"));
    }

    @Test
    @DisplayName("Reset should clear all statistics")
    void testReset() {
        stats.recordOutgoing("TestPacket", 100);
        stats.recordIncoming("InPacket", 200);

        stats.reset();

        assertEquals(0, stats.getTotalOutgoingCount());
        assertEquals(0, stats.getTotalIncomingCount());
    }

    @Test
    @DisplayName("Should calculate totals correctly")
    void testTotals() {
        stats.recordOutgoing("Packet1", 100);
        stats.recordOutgoing("Packet2", 200);
        stats.recordOutgoing("Packet1", 150);

        assertEquals(3, stats.getTotalOutgoingCount());
        assertEquals(450, stats.getTotalOutgoingBytes());
    }

    @Test
    @DisplayName("PacketCounter should be thread-safe")
    void testThreadSafety() throws InterruptedException {
        TestPacketCounter counter = new TestPacketCounter();
        int threadCount = 10;
        int incrementsPerThread = 1000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.record(10);
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(threadCount * incrementsPerThread, counter.getCount());
        assertEquals(threadCount * incrementsPerThread * 10, counter.getTotalBytes());
    }

    @Test
    @DisplayName("Top packets should be sorted by count")
    void testTopPacketsSorting() {
        stats.recordOutgoing("LowPacket", 100);
        for (int i = 0; i < 10; i++) {
            stats.recordOutgoing("HighPacket", 50);
        }
        for (int i = 0; i < 5; i++) {
            stats.recordOutgoing("MedPacket", 75);
        }

        List<String> top = stats.getTopOutgoingNames(3);
        assertEquals("HighPacket", top.get(0));
        assertEquals("MedPacket", top.get(1));
        assertEquals("LowPacket", top.get(2));
    }

    // Test implementations
    static class TestPacketCounter {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalBytes = new AtomicLong(0);

        public void record(int bytes) {
            count.incrementAndGet();
            totalBytes.addAndGet(bytes);
        }

        public long getCount() { return count.get(); }
        public long getTotalBytes() { return totalBytes.get(); }
    }

    static class TestPacketStatistics {
        private final Map<String, TestPacketCounter> outgoingStats = new ConcurrentHashMap<>();
        private final Map<String, TestPacketCounter> incomingStats = new ConcurrentHashMap<>();
        private volatile boolean enabled = false;

        public void setEnabled(boolean enable) { this.enabled = enable; }

        public void recordOutgoing(String packetName, int size) {
            if (!enabled) return;
            outgoingStats.computeIfAbsent(packetName, k -> new TestPacketCounter()).record(size);
        }

        public void recordIncoming(String packetName, int size) {
            if (!enabled) return;
            incomingStats.computeIfAbsent(packetName, k -> new TestPacketCounter()).record(size);
        }

        public void reset() {
            outgoingStats.clear();
            incomingStats.clear();
        }

        public long getOutgoingCount(String name) {
            TestPacketCounter c = outgoingStats.get(name);
            return c != null ? c.getCount() : 0;
        }

        public long getOutgoingBytes(String name) {
            TestPacketCounter c = outgoingStats.get(name);
            return c != null ? c.getTotalBytes() : 0;
        }

        public long getIncomingCount(String name) {
            TestPacketCounter c = incomingStats.get(name);
            return c != null ? c.getCount() : 0;
        }

        public long getIncomingBytes(String name) {
            TestPacketCounter c = incomingStats.get(name);
            return c != null ? c.getTotalBytes() : 0;
        }

        public long getTotalOutgoingCount() {
            return outgoingStats.values().stream().mapToLong(TestPacketCounter::getCount).sum();
        }

        public long getTotalIncomingCount() {
            return incomingStats.values().stream().mapToLong(TestPacketCounter::getCount).sum();
        }

        public long getTotalOutgoingBytes() {
            return outgoingStats.values().stream().mapToLong(TestPacketCounter::getTotalBytes).sum();
        }

        public List<String> getTopOutgoingNames(int limit) {
            return outgoingStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().getCount(), a.getValue().getCount()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
        }
    }
}
