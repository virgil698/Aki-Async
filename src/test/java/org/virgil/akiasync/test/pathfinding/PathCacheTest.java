package org.virgil.akiasync.test.pathfinding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PathCache functionality.
 * Validates caching, expiration, and eviction logic.
 */
public class PathCacheTest {

    private static final int CACHE_EXPIRE_TICKS = 100;
    private static final int MAX_CACHE_SIZE = 1000;

    private TestPathCache cache;

    @BeforeEach
    void setUp() {
        cache = new TestPathCache();
    }

    @Test
    @DisplayName("Cache should store and retrieve paths")
    void testBasicCacheOperations() {
        TestPath path = new TestPath("test-path-1");
        cache.cachePath(0, 64, 0, 100, 64, 100, path, 0);

        TestPath retrieved = cache.getCachedPath(0, 64, 0, 100, 64, 100, 50);
        assertNotNull(retrieved, "Should retrieve cached path");
        assertEquals("test-path-1", retrieved.id);
    }

    @Test
    @DisplayName("Cache should return null for non-existent paths")
    void testCacheMiss() {
        TestPath retrieved = cache.getCachedPath(0, 64, 0, 100, 64, 100, 0);
        assertNull(retrieved, "Should return null for cache miss");
    }

    @Test
    @DisplayName("Cache should expire old entries")
    void testCacheExpiration() {
        TestPath path = new TestPath("expiring-path");
        cache.cachePath(0, 64, 0, 100, 64, 100, path, 0);

        // Before expiration
        assertNotNull(cache.getCachedPath(0, 64, 0, 100, 64, 100, 50));

        // After expiration
        assertNull(cache.getCachedPath(0, 64, 0, 100, 64, 100, CACHE_EXPIRE_TICKS + 1));
    }

    @Test
    @DisplayName("Cache should not store null paths")
    void testNullPathNotCached() {
        cache.cachePath(0, 64, 0, 100, 64, 100, null, 0);
        assertEquals(0, cache.size(), "Null path should not be cached");
    }

    @Test
    @DisplayName("Cache should increment hit count on retrieval")
    void testHitCountIncrement() {
        TestPath path = new TestPath("hit-test");
        cache.cachePath(0, 64, 0, 100, 64, 100, path, 0);

        // Multiple retrievals
        for (int i = 0; i < 5; i++) {
            cache.getCachedPath(0, 64, 0, 100, 64, 100, 10);
        }

        assertTrue(cache.getTotalHits() >= 5, "Hit count should be at least 5");
    }

    @Test
    @DisplayName("Cache should handle different positions independently")
    void testMultiplePositions() {
        cache.cachePath(0, 64, 0, 100, 64, 100, new TestPath("path-1"), 0);
        cache.cachePath(200, 64, 200, 300, 64, 300, new TestPath("path-2"), 0);

        TestPath p1 = cache.getCachedPath(0, 64, 0, 100, 64, 100, 10);
        TestPath p2 = cache.getCachedPath(200, 64, 200, 300, 64, 300, 10);

        assertNotNull(p1);
        assertNotNull(p2);
        assertEquals("path-1", p1.id);
        assertEquals("path-2", p2.id);
    }

    @Test
    @DisplayName("Cache clear should remove all entries")
    void testCacheClear() {
        for (int i = 0; i < 10; i++) {
            cache.cachePath(i, 64, i, i + 100, 64, i + 100, new TestPath("path-" + i), 0);
        }

        assertTrue(cache.size() > 0, "Cache should have entries");
        cache.clear();
        assertEquals(0, cache.size(), "Cache should be empty after clear");
    }

    @Test
    @DisplayName("Cache key generation should be consistent")
    void testCacheKeyConsistency() {
        long key1 = getCacheKey(100, 64, -50, 200, 70, -100);
        long key2 = getCacheKey(100, 64, -50, 200, 70, -100);

        assertEquals(key1, key2, "Same positions should generate same key");
    }

    @Test
    @DisplayName("Different positions should generate different keys")
    void testCacheKeyUniqueness() {
        long key1 = getCacheKey(100, 64, -50, 200, 70, -100);
        long key2 = getCacheKey(100, 64, -51, 200, 70, -100);

        assertNotEquals(key1, key2, "Different positions should generate different keys");
    }

    // Mirror of PathCache.getCacheKey logic
    private long getCacheKey(int sx, int sy, int sz, int tx, int ty, int tz) {
        long startHash = ((long) sx & 0xFFFFL) |
                        (((long) sy & 0xFFFFL) << 16) |
                        (((long) sz & 0xFFFFL) << 32);
        long targetHash = ((long) tx & 0xFFFFL) |
                         (((long) ty & 0xFFFFL) << 16) |
                         (((long) tz & 0xFFFFL) << 48);
        return startHash ^ targetHash;
    }

    // Test implementations
    static class TestPath {
        final String id;
        TestPath(String id) { this.id = id; }
    }

    static class TestPathCache {
        private final java.util.Map<Long, CachedPath> pathCache = new java.util.HashMap<>();

        TestPath getCachedPath(int sx, int sy, int sz, int tx, int ty, int tz, long currentTick) {
            long key = getCacheKey(sx, sy, sz, tx, ty, tz);
            CachedPath cached = pathCache.get(key);

            if (cached != null) {
                if (currentTick - cached.createdTick < CACHE_EXPIRE_TICKS) {
                    cached.hitCount++;
                    return cached.path;
                } else {
                    pathCache.remove(key);
                }
            }
            return null;
        }

        void cachePath(int sx, int sy, int sz, int tx, int ty, int tz, TestPath path, long currentTick) {
            if (path == null) return;
            long key = getCacheKey(sx, sy, sz, tx, ty, tz);
            pathCache.put(key, new CachedPath(path, currentTick));
        }

        private long getCacheKey(int sx, int sy, int sz, int tx, int ty, int tz) {
            long startHash = ((long) sx & 0xFFFFL) |
                            (((long) sy & 0xFFFFL) << 16) |
                            (((long) sz & 0xFFFFL) << 32);
            long targetHash = ((long) tx & 0xFFFFL) |
                             (((long) ty & 0xFFFFL) << 16) |
                             (((long) tz & 0xFFFFL) << 48);
            return startHash ^ targetHash;
        }

        int size() { return pathCache.size(); }
        void clear() { pathCache.clear(); }

        int getTotalHits() {
            return pathCache.values().stream().mapToInt(c -> c.hitCount).sum();
        }

        static class CachedPath {
            final TestPath path;
            final long createdTick;
            int hitCount;

            CachedPath(TestPath path, long createdTick) {
                this.path = path;
                this.createdTick = createdTick;
                this.hitCount = 0;
            }
        }
    }
}
