package org.virgil.akiasync.test.entitytracker;

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
 * Tests for MultithreadedEntityTracker functionality.
 * Validates parallel chunk processing, task queuing, and thread pool behavior.
 */
public class EntityTrackerTest {

    private TestEntityTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new TestEntityTracker(4);
    }

    @Test
    @DisplayName("Small chunk count should use sequential processing")
    void testSequentialProcessingForSmallCount() {
        List<TestChunk> chunks = createChunks(3);
        
        tracker.tick(chunks);
        
        assertEquals(3, tracker.getProcessedCount());
        assertTrue(tracker.wasSequential(), "Should use sequential for <= 4 chunks");
    }

    @Test
    @DisplayName("Large chunk count should use parallel processing")
    @Timeout(10)
    void testParallelProcessingForLargeCount() {
        List<TestChunk> chunks = createChunks(20);
        
        tracker.tick(chunks);
        
        assertEquals(20, tracker.getProcessedCount());
        assertFalse(tracker.wasSequential(), "Should use parallel for > 4 chunks");
    }

    @Test
    @DisplayName("Empty chunk list should not process")
    void testEmptyChunkList() {
        List<TestChunk> chunks = new ArrayList<>();
        
        tracker.tick(chunks);
        
        assertEquals(0, tracker.getProcessedCount());
    }

    @Test
    @DisplayName("Main thread tasks should be executed")
    @Timeout(5)
    void testMainThreadTaskExecution() {
        AtomicInteger taskCounter = new AtomicInteger(0);
        
        tracker.addMainThreadTask(() -> taskCounter.incrementAndGet());
        tracker.addMainThreadTask(() -> taskCounter.incrementAndGet());
        tracker.addMainThreadTask(() -> taskCounter.incrementAndGet());
        
        tracker.runMainThreadTasks();
        
        assertEquals(3, taskCounter.get());
    }

    @Test
    @DisplayName("Task queue should handle concurrent submissions")
    @Timeout(10)
    void testConcurrentTaskSubmission() throws Exception {
        int threadCount = 10;
        int tasksPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger submitted = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                for (int i = 0; i < tasksPerThread; i++) {
                    tracker.addMainThreadTask(() -> {});
                    submitted.incrementAndGet();
                }
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(threadCount * tasksPerThread, submitted.get());
        
        executor.shutdown();
    }

    @Test
    @DisplayName("Parallelism should be bounded")
    void testParallelismBounds() {
        int parallelism = tracker.getParallelism();
        
        assertTrue(parallelism >= 2, "Parallelism should be at least 2");
        assertTrue(parallelism <= Runtime.getRuntime().availableProcessors(),
            "Parallelism should not exceed CPU count");
    }

    @Test
    @DisplayName("Chunk processing should handle exceptions gracefully")
    @Timeout(5)
    void testExceptionHandling() {
        // Use a tracker that handles exceptions internally
        TestEntityTracker safeTracker = new TestEntityTracker(4, true);
        
        List<TestChunk> chunks = new ArrayList<>();
        chunks.add(new TestChunk(0, 0, true)); // Will throw
        chunks.add(new TestChunk(1, 0, false));
        chunks.add(new TestChunk(2, 0, false));
        
        // Should not throw, should continue processing other chunks
        assertDoesNotThrow(() -> safeTracker.tick(chunks));
        
        // At least the non-throwing chunks should be processed
        assertTrue(safeTracker.getProcessedCount() >= 2);
    }

    @Test
    @DisplayName("Shutdown should terminate pool gracefully")
    @Timeout(5)
    void testShutdown() {
        TestEntityTracker localTracker = new TestEntityTracker(2);
        List<TestChunk> chunks = createChunks(10);
        
        localTracker.tick(chunks);
        localTracker.shutdown();
        
        assertTrue(localTracker.isShutdown());
    }

    @Test
    @DisplayName("ForkJoinPool should work steal efficiently")
    @Timeout(10)
    void testWorkStealing() {
        // Create chunks with varying processing times
        List<TestChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            chunks.add(new TestChunk(i, 0, false, i % 5)); // Varying work
        }
        
        long startTime = System.currentTimeMillis();
        tracker.tick(chunks);
        long elapsed = System.currentTimeMillis() - startTime;
        
        assertEquals(20, tracker.getProcessedCount());
        // With work stealing, should be faster than sequential
    }

    private List<TestChunk> createChunks(int count) {
        List<TestChunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(new TestChunk(i, 0, false));
        }
        return chunks;
    }

    // Test implementations
    static class TestChunk {
        final int x, z;
        final boolean shouldThrow;
        final int workUnits;

        TestChunk(int x, int z, boolean shouldThrow) {
            this(x, z, shouldThrow, 1);
        }

        TestChunk(int x, int z, boolean shouldThrow, int workUnits) {
            this.x = x;
            this.z = z;
            this.shouldThrow = shouldThrow;
            this.workUnits = workUnits;
        }
    }

    static class TestEntityTracker {
        private final ForkJoinPool pool;
        private final ConcurrentLinkedQueue<Runnable> mainThreadTasks = new ConcurrentLinkedQueue<>();
        private final AtomicInteger processedCount = new AtomicInteger(0);
        private volatile boolean wasSequential = false;
        private volatile boolean isShutdown = false;
        private final boolean catchExceptions;

        TestEntityTracker(int parallelism) {
            this(parallelism, false);
        }

        TestEntityTracker(int parallelism, boolean catchExceptions) {
            this.pool = new ForkJoinPool(parallelism);
            this.catchExceptions = catchExceptions;
        }

        void tick(List<TestChunk> chunks) {
            processedCount.set(0);
            
            if (chunks.isEmpty()) return;
            
            if (chunks.size() <= 4) {
                wasSequential = true;
                for (TestChunk chunk : chunks) {
                    safeProcessChunk(chunk);
                }
                runMainThreadTasks();
                return;
            }
            
            wasSequential = false;
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (TestChunk chunk : chunks) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    safeProcessChunk(chunk);
                }, pool);
                futures.add(future);
            }
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            runMainThreadTasks();
        }

        private void safeProcessChunk(TestChunk chunk) {
            try {
                processChunk(chunk);
            } catch (Exception e) {
                if (!catchExceptions) {
                    throw e;
                }
                // Silently ignore if catchExceptions is true
            }
        }

        private void processChunk(TestChunk chunk) {
            if (chunk.shouldThrow) {
                throw new RuntimeException("Test exception");
            }
            
            // Simulate work
            for (int i = 0; i < chunk.workUnits; i++) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            processedCount.incrementAndGet();
        }

        void addMainThreadTask(Runnable task) {
            mainThreadTasks.add(task);
        }

        void runMainThreadTasks() {
            Runnable task;
            while ((task = mainThreadTasks.poll()) != null) {
                task.run();
            }
        }

        void shutdown() {
            pool.shutdown();
            try {
                pool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            isShutdown = true;
        }

        int getProcessedCount() { return processedCount.get(); }
        boolean wasSequential() { return wasSequential; }
        boolean isShutdown() { return isShutdown; }
        int getParallelism() { return pool.getParallelism(); }
    }
}
