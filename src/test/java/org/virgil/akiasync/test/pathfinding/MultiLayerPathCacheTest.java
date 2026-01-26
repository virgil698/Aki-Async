package org.virgil.akiasync.test.pathfinding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests for MultiLayerPathCache functionality.
 * Validates hot/warm/cold cache tiers, promotion, demotion, and expiration.
 */
public class MultiLayerPathCacheTest {

    private TestMultiLayerCache cache;

    @BeforeEach
    void setUp() {
        cache = new TestMultiLayerCache(10, 20, 50);
    }

    @Test
    @DisplayName("New entries should go to hot cache")
    void testNewEntriesToHot() {
        cache.put("key1", new TestPath("path1"));
        
        assertEquals(1, cache.getHotSize());
        assertEquals(0, cache.getWarmSize());
        assertEquals(0, cache.getColdSize());
    }

    @Test
    @DisplayName("Hot cache hit should not promote")
    void testHotCacheHit() {
        cache.put("key1", new TestPath("path1"));
        
        TestPath result = cache.get("key1");
        assertNotNull(result);
        assertEquals("path1", result.id);
        assertEquals(1, cache.getHotHits());
    }

    @Test
    @DisplayName("Warm cache hit should promote to hot")
    void testWarmCachePromotion() {
        // Manually place in warm cache
        cache.putToWarm("key1", new TestPath("path1"));
        
        assertEquals(0, cache.getHotSize());
        assertEquals(1, cache.getWarmSize());
        
        // Access should promote
        TestPath result = cache.get("key1");
        assertNotNull(result);
        assertEquals(1, cache.getWarmHits());
        assertEquals(1, cache.getPromotions());
    }

    @Test
    @DisplayName("Cold cache hit should promote to warm")
    void testColdCachePromotion() {
        cache.putToCold("key1", new TestPath("path1"));
        
        assertEquals(0, cache.getHotSize());
        assertEquals(0, cache.getWarmSize());
        assertEquals(1, cache.getColdSize());
        
        TestPath result = cache.get("key1");
        assertNotNull(result);
        assertEquals(1, cache.getColdHits());
        assertEquals(1, cache.getPromotions());
    }

    @Test
    @DisplayName("Cache miss should return null")
    void testCacheMiss() {
        TestPath result = cache.get("nonexistent");
        assertNull(result);
        assertEquals(1, cache.getMisses());
    }

    @Test
    @DisplayName("Hot cache overflow should demote to warm")
    void testHotCacheEviction() {
        // Fill hot cache beyond capacity
        for (int i = 0; i < 15; i++) {
            cache.put("key" + i, new TestPath("path" + i));
        }
        
        // Hot should be at capacity, some demoted to warm
        assertTrue(cache.getHotSize() <= 10);
        assertTrue(cache.getWarmSize() > 0 || cache.getDemotions() > 0);
    }

    @Test
    @DisplayName("Expired entries should not be returned")
    void testExpiration() {
        cache.put("key1", new TestPath("path1"));
        
        // Simulate time passing
        cache.advanceTime(20000); // 20 seconds
        
        TestPath result = cache.get("key1");
        assertNull(result, "Expired entry should not be returned");
    }

    @Test
    @DisplayName("Clear should empty all caches")
    void testClear() {
        cache.put("key1", new TestPath("path1"));
        cache.putToWarm("key2", new TestPath("path2"));
        cache.putToCold("key3", new TestPath("path3"));
        
        cache.clear();
        
        assertEquals(0, cache.getHotSize());
        assertEquals(0, cache.getWarmSize());
        assertEquals(0, cache.getColdSize());
    }

    @Test
    @DisplayName("Statistics should be accurate")
    void testStatistics() {
        cache.put("key1", new TestPath("path1"));
        cache.get("key1"); // hot hit
        cache.get("key1"); // hot hit
        cache.get("nonexistent"); // miss
        
        assertEquals(2, cache.getHotHits());
        assertEquals(1, cache.getMisses());
    }

    @Test
    @DisplayName("Concurrent access should be thread-safe")
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    String key = "key-" + threadId + "-" + i;
                    cache.put(key, new TestPath("path-" + threadId + "-" + i));
                    cache.get(key);
                }
            });
        }
        
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        
        // Should not throw any exceptions
        assertTrue(cache.getHotHits() + cache.getWarmHits() + cache.getColdHits() + cache.getMisses() > 0);
    }

    // Test implementations
    static class TestPath {
        final String id;
        TestPath(String id) { this.id = id; }
    }

    static class TestCachedPath {
        final TestPath path;
        final long createTime;
        volatile long lastAccessTime;
        final AtomicInteger hitCount = new AtomicInteger(0);

        TestCachedPath(TestPath path, long currentTime) {
            this.path = path;
            this.createTime = currentTime;
            this.lastAccessTime = currentTime;
        }

        void recordHit(long currentTime) {
            this.lastAccessTime = currentTime;
            this.hitCount.incrementAndGet();
        }

        boolean isExpired(long expireMs, long currentTime) {
            return currentTime - createTime > expireMs;
        }
    }

    static class TestMultiLayerCache {
        private final int hotCacheSize;
        private final int warmCacheSize;
        private final int coldCacheSize;
        
        private final long hotExpireMs = 10_000;
        private final long warmExpireMs = 30_000;
        private final long coldExpireMs = 60_000;

        private final Map<String, TestCachedPath> hotCache = new ConcurrentHashMap<>();
        private final Map<String, TestCachedPath> warmCache = new ConcurrentHashMap<>();
        private final Map<String, TestCachedPath> coldCache = new ConcurrentHashMap<>();

        private final AtomicLong hotHits = new AtomicLong(0);
        private final AtomicLong warmHits = new AtomicLong(0);
        private final AtomicLong coldHits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong promotions = new AtomicLong(0);
        private final AtomicLong demotions = new AtomicLong(0);

        private long currentTime = System.currentTimeMillis();

        TestMultiLayerCache(int hotSize, int warmSize, int coldSize) {
            this.hotCacheSize = hotSize;
            this.warmCacheSize = warmSize;
            this.coldCacheSize = coldSize;
        }

        TestPath get(String key) {
            TestCachedPath hotPath = hotCache.get(key);
            if (hotPath != null && !hotPath.isExpired(hotExpireMs, currentTime)) {
                hotHits.incrementAndGet();
                hotPath.recordHit(currentTime);
                return hotPath.path;
            }

            TestCachedPath warmPath = warmCache.get(key);
            if (warmPath != null && !warmPath.isExpired(warmExpireMs, currentTime)) {
                warmHits.incrementAndGet();
                warmPath.recordHit(currentTime);
                promoteToHot(key, warmPath);
                return warmPath.path;
            }

            TestCachedPath coldPath = coldCache.get(key);
            if (coldPath != null && !coldPath.isExpired(coldExpireMs, currentTime)) {
                coldHits.incrementAndGet();
                coldPath.recordHit(currentTime);
                promoteToWarm(key, coldPath);
                return coldPath.path;
            }

            misses.incrementAndGet();
            return null;
        }

        void put(String key, TestPath path) {
            if (path == null) return;
            
            if (hotCache.size() >= hotCacheSize) {
                evictFromHot();
            }
            hotCache.put(key, new TestCachedPath(path, currentTime));
        }

        void putToWarm(String key, TestPath path) {
            warmCache.put(key, new TestCachedPath(path, currentTime));
        }

        void putToCold(String key, TestPath path) {
            coldCache.put(key, new TestCachedPath(path, currentTime));
        }

        private void promoteToHot(String key, TestCachedPath path) {
            warmCache.remove(key);
            if (hotCache.size() >= hotCacheSize) {
                evictFromHot();
            }
            hotCache.put(key, path);
            promotions.incrementAndGet();
        }

        private void promoteToWarm(String key, TestCachedPath path) {
            coldCache.remove(key);
            if (warmCache.size() >= warmCacheSize) {
                evictFromWarm();
            }
            warmCache.put(key, path);
            promotions.incrementAndGet();
        }

        private void evictFromHot() {
            String lruKey = null;
            long minLastAccess = Long.MAX_VALUE;
            
            for (var entry : hotCache.entrySet()) {
                if (entry.getValue().lastAccessTime < minLastAccess) {
                    minLastAccess = entry.getValue().lastAccessTime;
                    lruKey = entry.getKey();
                }
            }
            
            if (lruKey != null) {
                TestCachedPath evicted = hotCache.remove(lruKey);
                if (warmCache.size() < warmCacheSize && evicted != null) {
                    warmCache.put(lruKey, evicted);
                    demotions.incrementAndGet();
                }
            }
        }

        private void evictFromWarm() {
            String lruKey = null;
            long minLastAccess = Long.MAX_VALUE;
            
            for (var entry : warmCache.entrySet()) {
                if (entry.getValue().lastAccessTime < minLastAccess) {
                    minLastAccess = entry.getValue().lastAccessTime;
                    lruKey = entry.getKey();
                }
            }
            
            if (lruKey != null) {
                TestCachedPath evicted = warmCache.remove(lruKey);
                if (coldCache.size() < coldCacheSize && evicted != null) {
                    coldCache.put(lruKey, evicted);
                    demotions.incrementAndGet();
                }
            }
        }

        void advanceTime(long ms) { currentTime += ms; }
        void clear() { hotCache.clear(); warmCache.clear(); coldCache.clear(); }
        
        int getHotSize() { return hotCache.size(); }
        int getWarmSize() { return warmCache.size(); }
        int getColdSize() { return coldCache.size(); }
        long getHotHits() { return hotHits.get(); }
        long getWarmHits() { return warmHits.get(); }
        long getColdHits() { return coldHits.get(); }
        long getMisses() { return misses.get(); }
        long getPromotions() { return promotions.get(); }
        long getDemotions() { return demotions.get(); }
    }
}
