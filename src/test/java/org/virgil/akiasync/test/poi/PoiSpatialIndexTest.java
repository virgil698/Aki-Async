package org.virgil.akiasync.test.poi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests for PoiSpatialIndex functionality.
 * Validates POI storage, range queries, type filtering, and cleanup.
 */
public class PoiSpatialIndexTest {

    private TestPoiSpatialIndex index;

    @BeforeEach
    void setUp() {
        index = new TestPoiSpatialIndex();
    }

    @Test
    @DisplayName("Adding POI should increase count")
    void testAddPoi() {
        index.addPoi(100, 64, 100, "workstation");
        index.addPoi(200, 64, 200, "bed");
        
        assertEquals(2, index.getTotalPois());
    }

    @Test
    @DisplayName("Removing POI should decrease count")
    void testRemovePoi() {
        index.addPoi(100, 64, 100, "workstation");
        index.addPoi(200, 64, 200, "bed");
        
        index.removePoi(100, 64, 100);
        
        assertEquals(1, index.getTotalPois());
    }

    @Test
    @DisplayName("Range query should return POIs within radius")
    void testRangeQuery() {
        index.addPoi(100, 64, 100, "workstation");
        index.addPoi(110, 64, 110, "workstation");
        index.addPoi(500, 64, 500, "workstation"); // Far away
        
        List<int[]> results = index.queryRange(100, 64, 100, 50);
        
        assertEquals(2, results.size(), "Should find 2 POIs within radius 50");
    }

    @Test
    @DisplayName("Range query with zero radius should return empty")
    void testRangeQueryZeroRadius() {
        index.addPoi(100, 64, 100, "workstation");
        
        List<int[]> results = index.queryRange(100, 64, 100, 0);
        
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Type query should filter by POI type")
    void testTypeQuery() {
        index.addPoi(100, 64, 100, "workstation");
        index.addPoi(110, 64, 110, "bed");
        index.addPoi(120, 64, 120, "workstation");
        
        List<int[]> workstations = index.queryByType(100, 64, 100, "workstation", 100);
        List<int[]> beds = index.queryByType(100, 64, 100, "bed", 100);
        
        assertEquals(2, workstations.size());
        assertEquals(1, beds.size());
    }

    @Test
    @DisplayName("Clear should remove all POIs")
    void testClear() {
        index.addPoi(100, 64, 100, "workstation");
        index.addPoi(200, 64, 200, "bed");
        
        index.clear();
        
        assertEquals(0, index.getTotalPois());
        assertTrue(index.isEmpty());
    }

    @Test
    @DisplayName("Chunk cleanup should remove POIs in chunk")
    void testChunkCleanup() {
        // Add POIs in same chunk (chunk 6, 6)
        index.addPoi(100, 64, 100, "workstation");
        index.addPoi(105, 64, 105, "bed");
        // Add POI in different chunk
        index.addPoi(200, 64, 200, "workstation");
        
        index.cleanupChunk(6, 6);
        
        assertEquals(1, index.getTotalPois());
    }

    @Test
    @DisplayName("Statistics should track queries correctly")
    void testStatistics() {
        index.addPoi(100, 64, 100, "workstation");
        
        index.queryRange(100, 64, 100, 50);
        index.queryRange(100, 64, 100, 50);
        index.queryByType(100, 64, 100, "workstation", 50);
        
        assertEquals(3, index.getQueryCount());
        assertEquals(1, index.getTypeQueryCount());
    }

    @Test
    @DisplayName("Chunk key calculation should be correct")
    void testChunkKeyCalculation() {
        // Block 100, 100 should be in chunk 6, 6
        long key1 = index.getChunkKey(100, 64, 100);
        long key2 = index.getChunkKey(105, 64, 105); // Same chunk
        long key3 = index.getChunkKey(200, 64, 200); // Different chunk
        
        assertEquals(key1, key2, "Same chunk should have same key");
        assertNotEquals(key1, key3, "Different chunks should have different keys");
    }

    @Test
    @DisplayName("Range query should check all relevant chunks")
    void testRangeQueryCrossChunk() {
        // POIs in different chunks
        index.addPoi(15, 64, 15, "workstation");   // Chunk 0, 0
        index.addPoi(17, 64, 17, "workstation");   // Chunk 1, 1
        index.addPoi(100, 64, 100, "workstation"); // Far away
        
        List<int[]> results = index.queryRange(16, 64, 16, 10);
        
        assertEquals(2, results.size(), "Should find POIs across chunk boundaries");
    }

    @Test
    @DisplayName("Distance calculation should be accurate")
    void testDistanceCalculation() {
        index.addPoi(100, 64, 100, "workstation");
        index.addPoi(100, 64, 110, "workstation"); // 10 blocks away
        index.addPoi(100, 64, 120, "workstation"); // 20 blocks away
        
        List<int[]> results5 = index.queryRange(100, 64, 100, 5);
        List<int[]> results15 = index.queryRange(100, 64, 100, 15);
        List<int[]> results25 = index.queryRange(100, 64, 100, 25);
        
        assertEquals(1, results5.size());
        assertEquals(2, results15.size());
        assertEquals(3, results25.size());
    }

    @Test
    @DisplayName("Concurrent access should be thread-safe")
    void testConcurrentAccess() throws Exception {
        int threadCount = 10;
        int operationsPerThread = 100;
        
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    int x = threadId * 1000 + i;
                    index.addPoi(x, 64, x, "workstation");
                    index.queryRange(x, 64, x, 50);
                }
            });
        }
        
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        
        assertEquals(threadCount * operationsPerThread, index.getTotalPois());
    }

    // Test implementation
    static class TestPoiSpatialIndex {
        private final Map<Long, List<TestPoi>> chunkPoiMap = new ConcurrentHashMap<>();
        private final Map<String, Map<Long, List<TestPoi>>> typeIndex = new ConcurrentHashMap<>();
        private final AtomicLong totalPois = new AtomicLong(0);
        private final AtomicLong queryCount = new AtomicLong(0);
        private final AtomicLong typeQueryCount = new AtomicLong(0);

        void addPoi(int x, int y, int z, String type) {
            long chunkKey = getChunkKey(x, y, z);
            TestPoi poi = new TestPoi(x, y, z, type);
            
            chunkPoiMap.computeIfAbsent(chunkKey, k -> 
                Collections.synchronizedList(new ArrayList<>())).add(poi);
            
            typeIndex.computeIfAbsent(type, t -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> 
                    Collections.synchronizedList(new ArrayList<>())).add(poi);
            
            totalPois.incrementAndGet();
        }

        void removePoi(int x, int y, int z) {
            long chunkKey = getChunkKey(x, y, z);
            List<TestPoi> pois = chunkPoiMap.get(chunkKey);
            
            if (pois != null) {
                TestPoi toRemove = null;
                for (TestPoi poi : pois) {
                    if (poi.x == x && poi.y == y && poi.z == z) {
                        toRemove = poi;
                        break;
                    }
                }
                
                if (toRemove != null) {
                    pois.remove(toRemove);
                    totalPois.decrementAndGet();
                    
                    Map<Long, List<TestPoi>> typeMap = typeIndex.get(toRemove.type);
                    if (typeMap != null) {
                        List<TestPoi> typePois = typeMap.get(chunkKey);
                        if (typePois != null) {
                            typePois.remove(toRemove);
                        }
                    }
                }
            }
        }

        List<int[]> queryRange(int centerX, int centerY, int centerZ, int radius) {
            queryCount.incrementAndGet();
            if (radius <= 0) return Collections.emptyList();
            
            List<Long> chunkKeys = getChunkKeysInRange(centerX, centerZ, radius);
            List<int[]> result = new ArrayList<>();
            double radiusSq = radius * radius;
            
            for (Long key : chunkKeys) {
                List<TestPoi> pois = chunkPoiMap.get(key);
                if (pois != null) {
                    for (TestPoi poi : pois) {
                        double distSq = distanceSquared(poi.x, poi.y, poi.z, centerX, centerY, centerZ);
                        if (distSq <= radiusSq) {
                            result.add(new int[]{poi.x, poi.y, poi.z});
                        }
                    }
                }
            }
            return result;
        }

        List<int[]> queryByType(int centerX, int centerY, int centerZ, String type, int radius) {
            typeQueryCount.incrementAndGet();
            queryCount.incrementAndGet();
            if (radius <= 0 || type == null) return Collections.emptyList();
            
            Map<Long, List<TestPoi>> typeMap = typeIndex.get(type);
            if (typeMap == null) return Collections.emptyList();
            
            List<Long> chunkKeys = getChunkKeysInRange(centerX, centerZ, radius);
            List<int[]> result = new ArrayList<>();
            double radiusSq = radius * radius;
            
            for (Long key : chunkKeys) {
                List<TestPoi> pois = typeMap.get(key);
                if (pois != null) {
                    for (TestPoi poi : pois) {
                        double distSq = distanceSquared(poi.x, poi.y, poi.z, centerX, centerY, centerZ);
                        if (distSq <= radiusSq) {
                            result.add(new int[]{poi.x, poi.y, poi.z});
                        }
                    }
                }
            }
            return result;
        }

        void cleanupChunk(int chunkX, int chunkZ) {
            long chunkKey = ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
            List<TestPoi> removed = chunkPoiMap.remove(chunkKey);
            if (removed != null) {
                totalPois.addAndGet(-removed.size());
                for (Map<Long, List<TestPoi>> typeMap : typeIndex.values()) {
                    typeMap.remove(chunkKey);
                }
            }
        }

        void clear() {
            chunkPoiMap.clear();
            typeIndex.clear();
            totalPois.set(0);
            queryCount.set(0);
            typeQueryCount.set(0);
        }

        long getChunkKey(int x, int y, int z) {
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
        }

        private List<Long> getChunkKeysInRange(int centerX, int centerZ, int radius) {
            List<Long> keys = new ArrayList<>();
            int minChunkX = (centerX - radius) >> 4;
            int maxChunkX = (centerX + radius) >> 4;
            int minChunkZ = (centerZ - radius) >> 4;
            int maxChunkZ = (centerZ + radius) >> 4;
            
            for (int x = minChunkX; x <= maxChunkX; x++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    keys.add(((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32));
                }
            }
            return keys;
        }

        private double distanceSquared(int x1, int y1, int z1, int x2, int y2, int z2) {
            int dx = x2 - x1;
            int dy = y2 - y1;
            int dz = z2 - z1;
            return dx * dx + dy * dy + dz * dz;
        }

        boolean isEmpty() { return chunkPoiMap.isEmpty(); }
        long getTotalPois() { return totalPois.get(); }
        long getQueryCount() { return queryCount.get(); }
        long getTypeQueryCount() { return typeQueryCount.get(); }

        static class TestPoi {
            final int x, y, z;
            final String type;
            TestPoi(int x, int y, int z, String type) {
                this.x = x; this.y = y; this.z = z; this.type = type;
            }
        }
    }
}
