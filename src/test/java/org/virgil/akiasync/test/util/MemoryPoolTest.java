package org.virgil.akiasync.test.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for memory pooling utilities.
 * Validates object pooling, recycling, and thread-safe access.
 */
public class MemoryPoolTest {

    private TestObjectPool<TestPooledObject> pool;

    @BeforeEach
    void setUp() {
        pool = new TestObjectPool<>(TestPooledObject::new, 10);
    }

    @Test
    @DisplayName("Pool should provide objects")
    void testPoolProvidesObjects() {
        TestPooledObject obj = pool.acquire();
        assertNotNull(obj);
    }

    @Test
    @DisplayName("Released objects should be recycled")
    void testObjectRecycling() {
        TestPooledObject obj1 = pool.acquire();
        pool.release(obj1);

        TestPooledObject obj2 = pool.acquire();
        assertSame(obj1, obj2, "Should return recycled object");
    }

    @Test
    @DisplayName("Pool should create new objects when empty")
    void testPoolCreatesNewObjects() {
        List<TestPooledObject> acquired = new ArrayList<>();

        // Acquire more than pool size
        for (int i = 0; i < 15; i++) {
            acquired.add(pool.acquire());
        }

        assertEquals(15, acquired.size());
        assertTrue(pool.getCreatedCount() >= 15);
    }

    @Test
    @DisplayName("Pool should respect max size")
    void testPoolMaxSize() {
        List<TestPooledObject> acquired = new ArrayList<>();

        // Acquire all
        for (int i = 0; i < 15; i++) {
            acquired.add(pool.acquire());
        }

        // Release all
        for (TestPooledObject obj : acquired) {
            pool.release(obj);
        }

        // Pool should only keep up to max size
        assertTrue(pool.getPooledCount() <= 10);
    }

    @Test
    @DisplayName("Pooled objects should be reset on release")
    void testObjectReset() {
        TestPooledObject obj = pool.acquire();
        obj.value = 42;

        pool.release(obj);

        TestPooledObject recycled = pool.acquire();
        assertEquals(0, recycled.value, "Object should be reset");
    }

    @Test
    @DisplayName("Pool should be thread-safe")
    @Timeout(10)
    void testPoolThreadSafety() throws Exception {
        int threadCount = 10;
        int operationsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        TestPooledObject obj = pool.acquire();
                        if (obj == null) {
                            errors.incrementAndGet();
                            continue;
                        }
                        obj.value = i;
                        Thread.yield();
                        pool.release(obj);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(0, errors.get(), "Should have no errors");
        executor.shutdown();
    }

    @Test
    @DisplayName("Clear should empty the pool")
    void testPoolClear() {
        // Acquire and release some objects
        for (int i = 0; i < 5; i++) {
            pool.release(pool.acquire());
        }

        assertTrue(pool.getPooledCount() > 0);

        pool.clear();

        assertEquals(0, pool.getPooledCount());
    }

    @Test
    @DisplayName("Pool stats should be accurate")
    void testPoolStats() {
        pool.acquire();
        pool.acquire();
        TestPooledObject obj = pool.acquire();
        pool.release(obj);

        assertEquals(3, pool.getAcquireCount());
        assertEquals(1, pool.getReleaseCount());
    }

    // Test implementations
    static class TestPooledObject {
        int value = 0;

        void reset() {
            value = 0;
        }
    }

    static class TestObjectPool<T extends TestPooledObject> {
        private final java.util.function.Supplier<T> factory;
        private final int maxSize;
        private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();
        private final AtomicInteger createdCount = new AtomicInteger(0);
        private final AtomicInteger acquireCount = new AtomicInteger(0);
        private final AtomicInteger releaseCount = new AtomicInteger(0);

        TestObjectPool(java.util.function.Supplier<T> factory, int maxSize) {
            this.factory = factory;
            this.maxSize = maxSize;
        }

        T acquire() {
            acquireCount.incrementAndGet();
            T obj = pool.poll();
            if (obj == null) {
                createdCount.incrementAndGet();
                return factory.get();
            }
            return obj;
        }

        void release(T obj) {
            if (obj == null) return;
            releaseCount.incrementAndGet();
            obj.reset();
            if (pool.size() < maxSize) {
                pool.offer(obj);
            }
        }

        void clear() {
            pool.clear();
        }

        int getPooledCount() { return pool.size(); }
        int getCreatedCount() { return createdCount.get(); }
        int getAcquireCount() { return acquireCount.get(); }
        int getReleaseCount() { return releaseCount.get(); }
    }
}
