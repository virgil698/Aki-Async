package org.virgil.akiasync.test.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for async thread pool functionality.
 * Validates task execution, thread safety, and shutdown behavior.
 */
public class ThreadPoolTest {

    @Test
    @DisplayName("Thread pool should execute tasks")
    @Timeout(5)
    void testBasicExecution() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger counter = new AtomicInteger(0);

        Future<?> future = executor.submit(() -> counter.incrementAndGet());
        future.get();

        assertEquals(1, counter.get());
        executor.shutdown();
    }

    @Test
    @DisplayName("Thread pool should handle multiple concurrent tasks")
    @Timeout(10)
    void testConcurrentExecution() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        AtomicInteger counter = new AtomicInteger(0);
        int taskCount = 100;

        CountDownLatch latch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(taskCount, counter.get());
        executor.shutdown();
    }

    @Test
    @DisplayName("Thread pool should handle exceptions gracefully")
    @Timeout(5)
    void testExceptionHandling() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicInteger successCount = new AtomicInteger(0);

        // Task that throws
        Future<?> failingFuture = executor.submit(() -> {
            throw new RuntimeException("Test exception");
        });

        // Task that succeeds
        Future<?> successFuture = executor.submit(() -> successCount.incrementAndGet());

        assertThrows(ExecutionException.class, failingFuture::get);
        successFuture.get();
        assertEquals(1, successCount.get());

        executor.shutdown();
    }

    @Test
    @DisplayName("Scheduled executor should delay execution")
    @Timeout(5)
    void testScheduledExecution() throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger counter = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        ScheduledFuture<?> future = scheduler.schedule(
            () -> counter.incrementAndGet(),
            100, TimeUnit.MILLISECONDS
        );

        future.get();
        long elapsed = System.currentTimeMillis() - startTime;

        assertEquals(1, counter.get());
        assertTrue(elapsed >= 90, "Should have delayed at least 90ms");

        scheduler.shutdown();
    }

    @Test
    @DisplayName("CompletableFuture should chain correctly")
    @Timeout(5)
    void testCompletableFutureChaining() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CompletableFuture<Integer> future = CompletableFuture
            .supplyAsync(() -> 10, executor)
            .thenApplyAsync(x -> x * 2, executor)
            .thenApplyAsync(x -> x + 5, executor);

        assertEquals(25, future.get());
        executor.shutdown();
    }

    @Test
    @DisplayName("Thread pool should shutdown gracefully")
    @Timeout(5)
    void testGracefulShutdown() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger completed = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(50);
                    completed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        boolean terminated = executor.awaitTermination(2, TimeUnit.SECONDS);

        assertTrue(terminated, "Executor should terminate");
        assertEquals(5, completed.get(), "All tasks should complete");
    }

    @Test
    @DisplayName("Virtual threads should work correctly (Java 21+)")
    @Timeout(5)
    void testVirtualThreads() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        int taskCount = 1000;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch latch = new CountDownLatch(taskCount);
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                });
            }
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }

        assertEquals(taskCount, counter.get());
    }

    @Test
    @DisplayName("Work stealing pool should balance load")
    @Timeout(10)
    void testWorkStealingPool() throws Exception {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        AtomicInteger counter = new AtomicInteger(0);
        int taskCount = 100;

        CountDownLatch latch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            pool.submit(() -> {
                // Simulate varying work
                try {
                    Thread.sleep((long) (Math.random() * 10));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(taskCount, counter.get());
    }
}
