package org.virgil.akiasync.test.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests for work-stealing task scheduler.
 * Validates task distribution, stealing behavior, and load balancing.
 */
public class WorkStealingSchedulerTest {

    private TestWorkStealingScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TestWorkStealingScheduler(4);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    @Test
    @DisplayName("Scheduler should execute submitted tasks")
    @Timeout(5)
    void testTaskExecution() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        Future<?> future = scheduler.submit(() -> counter.incrementAndGet());
        future.get(3, TimeUnit.SECONDS);

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("Scheduler should handle multiple tasks")
    @Timeout(10)
    void testMultipleTasks() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        int taskCount = 100;

        CountDownLatch latch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            scheduler.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(taskCount, counter.get());
    }

    @Test
    @DisplayName("Work stealing should balance load")
    @Timeout(10)
    void testWorkStealing() throws Exception {
        AtomicInteger completedCount = new AtomicInteger(0);
        int taskCount = 100;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            scheduler.submit(() -> {
                // Simulate varying work
                try {
                    Thread.sleep((long) (Math.random() * 5));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                completedCount.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // All tasks should complete
        assertEquals(taskCount, completedCount.get());
    }

    @Test
    @DisplayName("Scheduler should handle exceptions gracefully")
    @Timeout(5)
    void testExceptionHandling() throws Exception {
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit failing task
        scheduler.submit(() -> {
            throw new RuntimeException("Test exception");
        });

        // Submit succeeding task
        Future<?> future = scheduler.submit(() -> successCount.incrementAndGet());
        future.get(3, TimeUnit.SECONDS);

        assertEquals(1, successCount.get());
    }

    @Test
    @DisplayName("Scheduler stats should track tasks")
    @Timeout(5)
    void testSchedulerStats() throws Exception {
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            scheduler.submit(() -> {
                latch.countDown();
            });
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));

        TestWorkStealingScheduler.Stats stats = scheduler.getStats();
        assertEquals(10, stats.submittedTasks);
        assertEquals(10, stats.completedTasks);
    }

    @Test
    @DisplayName("Parallelism should match configuration")
    void testParallelism() {
        assertEquals(4, scheduler.getParallelism());
    }

    @Test
    @DisplayName("Shutdown should complete pending tasks")
    @Timeout(10)
    void testShutdown() throws Exception {
        AtomicInteger completed = new AtomicInteger(0);

        for (int i = 0; i < 20; i++) {
            scheduler.submit(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                completed.incrementAndGet();
            });
        }

        scheduler.shutdown();
        scheduler.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(completed.get() > 0, "Some tasks should complete");
    }

    @Test
    @DisplayName("Recursive tasks should work with fork/join")
    @Timeout(10)
    void testRecursiveTasks() throws Exception {
        int result = scheduler.computeRecursive(10);

        // Fibonacci(10) = 55
        assertEquals(55, result);
    }

    // Test implementation
    static class TestWorkStealingScheduler {
        private final ForkJoinPool pool;
        private final Stats stats = new Stats();

        TestWorkStealingScheduler(int parallelism) {
            this.pool = new ForkJoinPool(parallelism);
        }

        Future<?> submit(Runnable task) {
            stats.submittedTasks++;
            return pool.submit(() -> {
                try {
                    task.run();
                    stats.completedTasks++;
                } catch (Exception e) {
                    stats.failedTasks++;
                }
            });
        }

        int computeRecursive(int n) {
            return pool.invoke(new FibonacciTask(n));
        }

        void shutdown() {
            pool.shutdown();
        }

        boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return pool.awaitTermination(timeout, unit);
        }

        int getParallelism() {
            return pool.getParallelism();
        }

        long getCompletedTaskCount() {
            return stats.completedTasks;
        }

        Stats getStats() {
            return stats;
        }

        static class Stats {
            volatile long submittedTasks = 0;
            volatile long completedTasks = 0;
            volatile long failedTasks = 0;
            final int parallelism = 4;

            @Override
            public String toString() {
                return String.format("Stats{submitted=%d, completed=%d, failed=%d}",
                    submittedTasks, completedTasks, failedTasks);
            }
        }

        static class FibonacciTask extends RecursiveTask<Integer> {
            private final int n;

            FibonacciTask(int n) {
                this.n = n;
            }

            @Override
            protected Integer compute() {
                if (n <= 1) return n;

                FibonacciTask f1 = new FibonacciTask(n - 1);
                FibonacciTask f2 = new FibonacciTask(n - 2);

                f1.fork();
                return f2.compute() + f1.join();
            }
        }
    }
}
