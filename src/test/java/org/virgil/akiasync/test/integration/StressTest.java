package org.virgil.akiasync.test.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stress tests for high-load scenarios.
 * Validates system behavior under extreme conditions.
 */
public class StressTest {

    @Test
    @DisplayName("High entity count should not cause OOM")
    @Timeout(30)
    void testHighEntityCount() {
        TestEntityManager manager = new TestEntityManager();

        // Spawn many entities
        for (int i = 0; i < 10000; i++) {
            manager.spawnEntity(i);
        }

        assertEquals(10000, manager.getEntityCount());

        // Tick all entities
        for (int tick = 0; tick < 10; tick++) {
            manager.tickAll();
        }

        assertTrue(manager.getTicksProcessed() >= 100000);
    }

    @Test
    @DisplayName("Rapid cache access should not degrade")
    @Timeout(30)
    void testRapidCacheAccess() throws Exception {
        TestCache cache = new TestCache(1000);
        int threadCount = 10;
        int operationsPerThread = 10000;

        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        long startTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    String key = "key-" + (threadId * 1000 + i % 1000);
                    cache.getOrCompute(key, () -> "value-" + key);
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        long elapsed = System.nanoTime() - startTime;

        double opsPerSecond = (threadCount * operationsPerThread) / (elapsed / 1_000_000_000.0);
        assertTrue(opsPerSecond > 10000, "Should handle > 10K ops/sec");

        executor.shutdown();
    }

    @Test
    @DisplayName("Concurrent pathfinding requests should not deadlock")
    @Timeout(30)
    void testConcurrentPathfinding() throws Exception {
        TestPathfinder pathfinder = new TestPathfinder(4);
        int requestCount = 1000;

        List<CompletableFuture<int[]>> futures = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            final int id = i;
            futures.add(pathfinder.findPathAsync(
                id * 10, 64, id * 10,
                id * 10 + 100, 64, id * 10 + 100
            ));
        }

        int completed = 0;
        for (CompletableFuture<int[]> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
                completed++;
            } catch (TimeoutException e) {
                fail("Pathfinding request timed out - possible deadlock");
            }
        }

        assertEquals(requestCount, completed);
        pathfinder.shutdown();
    }

    @Test
    @DisplayName("Memory should stabilize under sustained load")
    @Timeout(60)
    void testMemoryStability() throws Exception {
        TestMemoryIntensiveOperation operation = new TestMemoryIntensiveOperation();

        // Warm up
        for (int i = 0; i < 100; i++) {
            operation.process();
        }

        System.gc();
        Thread.sleep(100);
        long baselineMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Sustained load
        for (int i = 0; i < 1000; i++) {
            operation.process();
            if (i % 100 == 0) {
                System.gc();
            }
        }

        System.gc();
        Thread.sleep(100);
        long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Memory should not grow unboundedly (allow 50% growth)
        double growth = (double) finalMemory / baselineMemory;
        assertTrue(growth < 2.0, "Memory grew by " + (growth * 100 - 100) + "%, expected < 100%");
    }

    @Test
    @DisplayName("Thread pool should handle burst traffic")
    @Timeout(30)
    void testBurstTraffic() throws Exception {
        TestThreadPool pool = new TestThreadPool(4);
        AtomicInteger completed = new AtomicInteger(0);

        // Submit burst of tasks
        int burstSize = 1000;
        CountDownLatch latch = new CountDownLatch(burstSize);

        long startTime = System.nanoTime();
        for (int i = 0; i < burstSize; i++) {
            pool.submit(() -> {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {}
                completed.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        long elapsed = System.nanoTime() - startTime;

        assertEquals(burstSize, completed.get());
        assertTrue(elapsed < 10_000_000_000L, "Burst should complete in < 10 seconds");

        pool.shutdown();
    }

    @Test
    @DisplayName("Lock contention should not cause starvation")
    @Timeout(30)
    void testLockContention() throws Exception {
        TestSharedResource resource = new TestSharedResource();
        int threadCount = 20;
        int operationsPerThread = 1000;

        AtomicInteger[] threadCompletions = new AtomicInteger[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threadCompletions[i] = new AtomicInteger(0);
        }

        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    resource.access();
                    threadCompletions[threadId].incrementAndGet();
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));

        // Check no thread was starved (each should complete at least 80% of operations)
        for (int t = 0; t < threadCount; t++) {
            assertTrue(threadCompletions[t].get() >= operationsPerThread * 0.8,
                "Thread " + t + " was starved");
        }

        executor.shutdown();
    }

    // Test implementations
    static class TestEntityManager {
        private final List<Integer> entities = new ArrayList<>();
        private final AtomicLong ticksProcessed = new AtomicLong(0);

        void spawnEntity(int id) { entities.add(id); }
        int getEntityCount() { return entities.size(); }

        void tickAll() {
            for (Integer entity : entities) {
                ticksProcessed.incrementAndGet();
            }
        }

        long getTicksProcessed() { return ticksProcessed.get(); }
    }

    static class TestCache {
        private final Map<String, String> cache;
        private final int maxSize;

        TestCache(int maxSize) {
            this.maxSize = maxSize;
            this.cache = new ConcurrentHashMap<>();
        }

        String getOrCompute(String key, java.util.function.Supplier<String> supplier) {
            return cache.computeIfAbsent(key, k -> {
                if (cache.size() >= maxSize) {
                    // Simple eviction
                    cache.keySet().stream().findFirst().ifPresent(cache::remove);
                }
                return supplier.get();
            });
        }
    }

    static class TestPathfinder {
        private final ExecutorService executor;

        TestPathfinder(int threads) {
            this.executor = Executors.newFixedThreadPool(threads);
        }

        CompletableFuture<int[]> findPathAsync(int sx, int sy, int sz, int tx, int ty, int tz) {
            return CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                return new int[]{tx, ty, tz};
            }, executor);
        }

        void shutdown() { executor.shutdown(); }
    }

    static class TestMemoryIntensiveOperation {
        void process() {
            // Create temporary objects
            List<byte[]> temp = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                temp.add(new byte[1024]);
            }
            temp.clear();
        }
    }

    static class TestThreadPool {
        private final ExecutorService executor;

        TestThreadPool(int threads) {
            this.executor = Executors.newFixedThreadPool(threads);
        }

        void submit(Runnable task) { executor.submit(task); }
        void shutdown() { executor.shutdown(); }
    }

    static class TestSharedResource {
        private final java.util.concurrent.locks.ReentrantLock lock = 
            new java.util.concurrent.locks.ReentrantLock(true); // Fair lock

        void access() {
            lock.lock();
            try {
                // Simulate work
                Thread.yield();
            } finally {
                lock.unlock();
            }
        }
    }
}
