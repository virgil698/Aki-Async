package org.virgil.akiasync.test.pathfinding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests for PathfindingRequestMerger functionality.
 * Validates request deduplication, merging, and batch processing.
 */
public class PathfindingRequestMergerTest {

    private TestRequestMerger merger;

    @BeforeEach
    void setUp() {
        merger = new TestRequestMerger(3); // tolerance of 3 blocks
    }

    @Test
    @DisplayName("Identical requests should be merged")
    void testIdenticalRequestsMerged() {
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity1");
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity2");
        
        assertEquals(1, merger.getUniqueRequestCount());
        assertEquals(2, merger.getTotalRequestCount());
    }

    @Test
    @DisplayName("Similar requests within tolerance should be merged")
    void testSimilarRequestsMerged() {
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity1");
        merger.addRequest(1, 64, 1, 101, 64, 101, "entity2"); // Within 3 block tolerance
        
        assertEquals(1, merger.getUniqueRequestCount());
    }

    @Test
    @DisplayName("Different requests should not be merged")
    void testDifferentRequestsNotMerged() {
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity1");
        merger.addRequest(50, 64, 50, 150, 64, 150, "entity2"); // Far apart
        
        assertEquals(2, merger.getUniqueRequestCount());
    }

    @Test
    @DisplayName("Merged request should notify all requesters")
    void testMergedRequestNotifiesAll() {
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity1");
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity2");
        merger.addRequest(1, 64, 1, 101, 64, 101, "entity3");
        
        List<String> requesters = merger.getRequestersForPosition(0, 64, 0, 100, 64, 100);
        assertTrue(requesters.size() >= 2, "Should have multiple requesters");
    }

    @Test
    @DisplayName("Clear should remove all requests")
    void testClear() {
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity1");
        merger.addRequest(50, 64, 50, 150, 64, 150, "entity2");
        
        merger.clear();
        
        assertEquals(0, merger.getUniqueRequestCount());
        assertEquals(0, merger.getTotalRequestCount());
    }

    @Test
    @DisplayName("Batch processing should return all unique requests")
    void testBatchProcessing() {
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity1");
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity2");
        merger.addRequest(200, 64, 200, 300, 64, 300, "entity3");
        
        List<int[]> batch = merger.getBatchRequests();
        
        assertEquals(2, batch.size(), "Should have 2 unique requests");
    }

    @Test
    @DisplayName("Request priority should be respected")
    void testRequestPriority() {
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity1", 1); // Low priority
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity2", 10); // High priority
        
        int priority = merger.getRequestPriority(0, 64, 0, 100, 64, 100);
        assertEquals(10, priority, "Should use highest priority");
    }

    @Test
    @DisplayName("Expired requests should be removed")
    void testExpiredRequestsRemoved() {
        merger.addRequest(0, 64, 0, 100, 64, 100, "entity1");
        
        // Simulate time passing
        merger.advanceTime(10000);
        merger.cleanupExpired(5000);
        
        assertEquals(0, merger.getUniqueRequestCount());
    }

    @Test
    @DisplayName("Tolerance calculation should be correct")
    void testToleranceCalculation() {
        // Within tolerance (3 blocks)
        assertTrue(merger.isWithinTolerance(0, 0, 0, 2, 2, 2));
        
        // Outside tolerance
        assertFalse(merger.isWithinTolerance(0, 0, 0, 10, 10, 10));
    }

    // Test implementation
    static class TestRequestMerger {
        private final int tolerance;
        private final Map<String, MergedRequest> requests = new ConcurrentHashMap<>();
        private long currentTime = System.currentTimeMillis();

        TestRequestMerger(int tolerance) {
            this.tolerance = tolerance;
        }

        void addRequest(int sx, int sy, int sz, int tx, int ty, int tz, String requester) {
            addRequest(sx, sy, sz, tx, ty, tz, requester, 1);
        }

        void addRequest(int sx, int sy, int sz, int tx, int ty, int tz, String requester, int priority) {
            String key = findSimilarKey(sx, sy, sz, tx, ty, tz);
            
            if (key == null) {
                key = sx + "," + sy + "," + sz + "->" + tx + "," + ty + "," + tz;
                requests.put(key, new MergedRequest(sx, sy, sz, tx, ty, tz, currentTime));
            }
            
            MergedRequest merged = requests.get(key);
            merged.addRequester(requester);
            merged.updatePriority(priority);
        }

        private String findSimilarKey(int sx, int sy, int sz, int tx, int ty, int tz) {
            for (Map.Entry<String, MergedRequest> entry : requests.entrySet()) {
                MergedRequest req = entry.getValue();
                if (isWithinTolerance(req.sx, req.sy, req.sz, sx, sy, sz) &&
                    isWithinTolerance(req.tx, req.ty, req.tz, tx, ty, tz)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        boolean isWithinTolerance(int x1, int y1, int z1, int x2, int y2, int z2) {
            return Math.abs(x1 - x2) <= tolerance &&
                   Math.abs(y1 - y2) <= tolerance &&
                   Math.abs(z1 - z2) <= tolerance;
        }

        List<String> getRequestersForPosition(int sx, int sy, int sz, int tx, int ty, int tz) {
            String key = findSimilarKey(sx, sy, sz, tx, ty, tz);
            if (key != null) {
                return new ArrayList<>(requests.get(key).requesters);
            }
            return Collections.emptyList();
        }

        int getRequestPriority(int sx, int sy, int sz, int tx, int ty, int tz) {
            String key = findSimilarKey(sx, sy, sz, tx, ty, tz);
            if (key != null) {
                return requests.get(key).priority;
            }
            return 0;
        }

        List<int[]> getBatchRequests() {
            List<int[]> batch = new ArrayList<>();
            for (MergedRequest req : requests.values()) {
                batch.add(new int[]{req.sx, req.sy, req.sz, req.tx, req.ty, req.tz});
            }
            return batch;
        }

        void cleanupExpired(long maxAge) {
            requests.entrySet().removeIf(e -> currentTime - e.getValue().timestamp > maxAge);
        }

        void advanceTime(long ms) { currentTime += ms; }
        void clear() { requests.clear(); }
        int getUniqueRequestCount() { return requests.size(); }
        int getTotalRequestCount() {
            return requests.values().stream().mapToInt(r -> r.requesters.size()).sum();
        }

        static class MergedRequest {
            final int sx, sy, sz, tx, ty, tz;
            final long timestamp;
            final List<String> requesters = new ArrayList<>();
            int priority = 0;

            MergedRequest(int sx, int sy, int sz, int tx, int ty, int tz, long timestamp) {
                this.sx = sx; this.sy = sy; this.sz = sz;
                this.tx = tx; this.ty = ty; this.tz = tz;
                this.timestamp = timestamp;
            }

            void addRequester(String id) { requesters.add(id); }
            void updatePriority(int p) { priority = Math.max(priority, p); }
        }
    }
}
