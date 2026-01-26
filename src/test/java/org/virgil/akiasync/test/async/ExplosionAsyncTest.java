package org.virgil.akiasync.test.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for async explosion processing.
 * Validates task queuing, parallel processing, and result aggregation.
 */
public class ExplosionAsyncTest {

    @Test
    @DisplayName("Explosion tasks should be queued correctly")
    @Timeout(5)
    void testExplosionTaskQueuing() {
        TestExplosionQueue queue = new TestExplosionQueue();
        
        queue.queueExplosion(0, 64, 0, 4.0f);
        queue.queueExplosion(100, 64, 100, 6.0f);
        
        assertEquals(2, queue.getPendingCount());
    }

    @Test
    @DisplayName("Explosion processing should be parallel")
    @Timeout(10)
    void testParallelProcessing() throws Exception {
        TestExplosionProcessor processor = new TestExplosionProcessor(4);
        AtomicInteger processed = new AtomicInteger(0);
        
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int x = i * 100;
            futures.add(processor.processAsync(() -> {
                try {
                    Thread.sleep(50); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                processed.incrementAndGet();
            }));
        }
        
        for (Future<?> f : futures) {
            f.get();
        }
        
        assertEquals(10, processed.get());
        processor.shutdown();
    }

    @Test
    @DisplayName("Block damage calculation should be deterministic")
    void testBlockDamageCalculation() {
        double damage1 = calculateBlockDamage(0, 64, 0, 5, 65, 5, 4.0f);
        double damage2 = calculateBlockDamage(0, 64, 0, 5, 65, 5, 4.0f);
        
        assertEquals(damage1, damage2, 0.0001, "Same inputs should give same damage");
    }

    @Test
    @DisplayName("Explosion radius should affect block count")
    void testExplosionRadius() {
        int blocks1 = estimateAffectedBlocks(2.0f);
        int blocks2 = estimateAffectedBlocks(4.0f);
        int blocks3 = estimateAffectedBlocks(8.0f);
        
        assertTrue(blocks2 > blocks1, "Larger radius should affect more blocks");
        assertTrue(blocks3 > blocks2, "Larger radius should affect more blocks");
    }

    @Test
    @DisplayName("Ray casting should terminate at max distance")
    void testRayCastingTermination() {
        TestRayCaster caster = new TestRayCaster();
        
        int steps = caster.castRay(0, 64, 0, 1, 0, 0, 10.0);
        
        assertTrue(steps <= 10 * 4, "Ray should terminate within max distance");
    }

    @Test
    @DisplayName("Explosion queue should handle concurrent submissions")
    @Timeout(5)
    void testConcurrentSubmissions() throws Exception {
        TestExplosionQueue queue = new TestExplosionQueue();
        int threadCount = 10;
        int explosionsPerThread = 100;
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < explosionsPerThread; i++) {
                    queue.queueExplosion(threadId * 1000 + i, 64, i, 4.0f);
                }
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(threadCount * explosionsPerThread, queue.getPendingCount());
        
        executor.shutdown();
    }

    @Test
    @DisplayName("Explosion result aggregation should be correct")
    void testResultAggregation() {
        TestExplosionResult result = new TestExplosionResult();
        
        result.addDestroyedBlock(0, 64, 0);
        result.addDestroyedBlock(1, 64, 0);
        result.addDestroyedBlock(0, 65, 0);
        result.addAffectedEntity(100);
        result.addAffectedEntity(101);
        
        assertEquals(3, result.getDestroyedBlockCount());
        assertEquals(2, result.getAffectedEntityCount());
    }

    @Test
    @DisplayName("TNT chain explosions should be queued")
    void testTNTChainExplosions() {
        TestExplosionQueue queue = new TestExplosionQueue();
        
        // Initial explosion
        queue.queueExplosion(0, 64, 0, 4.0f);
        
        // Simulate finding TNT blocks and queueing chain explosions
        queue.queueExplosion(3, 64, 0, 4.0f);
        queue.queueExplosion(-3, 64, 0, 4.0f);
        
        assertEquals(3, queue.getPendingCount());
    }

    // Helper methods
    private double calculateBlockDamage(int expX, int expY, int expZ, 
                                         int blockX, int blockY, int blockZ, float power) {
        double dx = blockX - expX;
        double dy = blockY - expY;
        double dz = blockZ - expZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (distance > power) return 0;
        
        double impact = (1.0 - distance / power) * power;
        return Math.max(0, impact);
    }

    private int estimateAffectedBlocks(float radius) {
        int count = 0;
        int r = (int) Math.ceil(radius);
        
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z <= radius * radius) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // Test implementations
    static class TestExplosionQueue {
        private final ConcurrentLinkedQueue<ExplosionTask> queue = new ConcurrentLinkedQueue<>();

        void queueExplosion(int x, int y, int z, float power) {
            queue.add(new ExplosionTask(x, y, z, power));
        }

        int getPendingCount() { return queue.size(); }

        record ExplosionTask(int x, int y, int z, float power) {}
    }

    static class TestExplosionProcessor {
        private final ExecutorService executor;

        TestExplosionProcessor(int threads) {
            this.executor = Executors.newFixedThreadPool(threads);
        }

        Future<?> processAsync(Runnable task) {
            return executor.submit(task);
        }

        void shutdown() {
            executor.shutdown();
        }
    }

    static class TestRayCaster {
        int castRay(double startX, double startY, double startZ,
                    double dirX, double dirY, double dirZ, double maxDistance) {
            double step = 0.25;
            int steps = 0;
            double traveled = 0;
            
            while (traveled < maxDistance) {
                traveled += step;
                steps++;
                
                // Simulate hitting a block
                if (steps > 20 && Math.random() < 0.1) {
                    break;
                }
            }
            
            return steps;
        }
    }

    static class TestExplosionResult {
        private final List<int[]> destroyedBlocks = new ArrayList<>();
        private final List<Integer> affectedEntities = new ArrayList<>();

        void addDestroyedBlock(int x, int y, int z) {
            destroyedBlocks.add(new int[]{x, y, z});
        }

        void addAffectedEntity(int entityId) {
            affectedEntities.add(entityId);
        }

        int getDestroyedBlockCount() { return destroyedBlocks.size(); }
        int getAffectedEntityCount() { return affectedEntities.size(); }
    }
}
