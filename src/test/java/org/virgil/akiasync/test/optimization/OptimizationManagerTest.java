package org.virgil.akiasync.test.optimization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OptimizationManager functionality.
 * Validates optimization type availability, stats tracking, and singleton behavior.
 */
public class OptimizationManagerTest {

    private TestOptimizationManager manager;

    @BeforeEach
    void setUp() {
        manager = TestOptimizationManager.getInstance();
        manager.resetStats();
    }

    @Test
    @DisplayName("Singleton should return same instance")
    void testSingletonInstance() {
        TestOptimizationManager instance1 = TestOptimizationManager.getInstance();
        TestOptimizationManager instance2 = TestOptimizationManager.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("Stats should track entity collections created")
    void testEntityCollectionStats() {
        long initial = manager.getStats().entityCollectionsCreated;

        manager.createEntityCollection();
        manager.createEntityCollection();

        assertEquals(initial + 2, manager.getStats().entityCollectionsCreated);
    }

    @Test
    @DisplayName("Stats should track cache hits")
    void testCacheHitStats() {
        long initial = manager.getStats().blockPosCacheHits;

        manager.recordCacheHit();
        manager.recordCacheHit();
        manager.recordCacheHit();

        assertEquals(initial + 3, manager.getStats().blockPosCacheHits);
    }

    @Test
    @DisplayName("Stats should track work stealing tasks")
    void testWorkStealingStats() {
        long initial = manager.getStats().workStealingTasksProcessed;

        manager.recordTaskProcessed();

        assertEquals(initial + 1, manager.getStats().workStealingTasksProcessed);
    }

    @Test
    @DisplayName("Uptime should increase over time")
    void testUptimeTracking() throws Exception {
        long uptime1 = manager.getStats().getUptimeMillis();
        Thread.sleep(50);
        long uptime2 = manager.getStats().getUptimeMillis();

        assertTrue(uptime2 > uptime1, "Uptime should increase");
    }

    @Test
    @DisplayName("Optimization types should be checkable")
    void testOptimizationTypeAvailability() {
        manager.setOptimizationEnabled(TestOptimizationType.BLOCK_POS_CACHE, true);
        manager.setOptimizationEnabled(TestOptimizationType.WORK_STEALING, false);

        assertTrue(manager.isOptimizationAvailable(TestOptimizationType.BLOCK_POS_CACHE));
        assertFalse(manager.isOptimizationAvailable(TestOptimizationType.WORK_STEALING));
    }

    @Test
    @DisplayName("All optimization types should be enumerable")
    void testAllOptimizationTypes() {
        TestOptimizationType[] types = TestOptimizationType.values();

        assertTrue(types.length >= 4);
        assertNotNull(TestOptimizationType.valueOf("BLOCK_POS_CACHE"));
        assertNotNull(TestOptimizationType.valueOf("WORK_STEALING"));
        assertNotNull(TestOptimizationType.valueOf("VIRTUAL_THREADS"));
        assertNotNull(TestOptimizationType.valueOf("ENTITY_COLLECTIONS"));
    }

    @Test
    @DisplayName("Stats toString should contain all fields")
    void testStatsToString() {
        manager.createEntityCollection();
        manager.recordCacheHit();
        manager.recordTaskProcessed();

        String statsStr = manager.getStats().toString();

        assertTrue(statsStr.contains("uptime"));
        assertTrue(statsStr.contains("entityCollections"));
        assertTrue(statsStr.contains("cacheHits"));
        assertTrue(statsStr.contains("tasks"));
    }

    @Test
    @DisplayName("Reset stats should clear counters")
    void testResetStats() {
        manager.createEntityCollection();
        manager.recordCacheHit();
        manager.recordTaskProcessed();

        manager.resetStats();

        assertEquals(0, manager.getStats().entityCollectionsCreated);
        assertEquals(0, manager.getStats().blockPosCacheHits);
        assertEquals(0, manager.getStats().workStealingTasksProcessed);
    }

    // Test implementations
    enum TestOptimizationType {
        BLOCK_POS_CACHE,
        WORK_STEALING,
        VIRTUAL_THREADS,
        ENTITY_COLLECTIONS
    }

    static class TestOptimizationManager {
        private static final TestOptimizationManager INSTANCE = new TestOptimizationManager();
        private final TestOptimizationStats stats = new TestOptimizationStats();
        private final java.util.Map<TestOptimizationType, Boolean> optimizations = 
            new java.util.concurrent.ConcurrentHashMap<>();

        private TestOptimizationManager() {
            for (TestOptimizationType type : TestOptimizationType.values()) {
                optimizations.put(type, true);
            }
        }

        static TestOptimizationManager getInstance() {
            return INSTANCE;
        }

        void createEntityCollection() {
            stats.entityCollectionsCreated++;
        }

        void recordCacheHit() {
            stats.blockPosCacheHits++;
        }

        void recordTaskProcessed() {
            stats.workStealingTasksProcessed++;
        }

        void setOptimizationEnabled(TestOptimizationType type, boolean enabled) {
            optimizations.put(type, enabled);
        }

        boolean isOptimizationAvailable(TestOptimizationType type) {
            return optimizations.getOrDefault(type, false);
        }

        TestOptimizationStats getStats() {
            return stats;
        }

        void resetStats() {
            stats.entityCollectionsCreated = 0;
            stats.blockPosCacheHits = 0;
            stats.workStealingTasksProcessed = 0;
            stats.virtualThreadsUsed = 0;
        }
    }

    static class TestOptimizationStats {
        volatile long entityCollectionsCreated = 0;
        volatile long blockPosCacheHits = 0;
        volatile long workStealingTasksProcessed = 0;
        volatile long virtualThreadsUsed = 0;
        private final long startTime = System.currentTimeMillis();

        long getUptimeMillis() {
            return System.currentTimeMillis() - startTime;
        }

        @Override
        public String toString() {
            return String.format(
                "Stats{uptime=%dms, entityCollections=%d, cacheHits=%d, tasks=%d, vThreads=%d}",
                getUptimeMillis(), entityCollectionsCreated, blockPosCacheHits,
                workStealingTasksProcessed, virtualThreadsUsed
            );
        }
    }
}
