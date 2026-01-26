package org.virgil.akiasync.test.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;

/**
 * Tests for concurrency utilities and patterns.
 * Validates lock behavior, atomic operations, and concurrent data structures.
 */
public class ConcurrencyUtilsTest {

    @Test
    @DisplayName("AtomicInteger should be thread-safe")
    @Timeout(5)
    void testAtomicIntegerThreadSafety() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        int threadCount = 10;
        int incrementsPerThread = 10000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.incrementAndGet();
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(threadCount * incrementsPerThread, counter.get());
    }

    @Test
    @DisplayName("ConcurrentHashMap should handle concurrent access")
    @Timeout(5)
    void testConcurrentHashMap() throws Exception {
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
        int threadCount = 10;
        int operationsPerThread = 1000;

        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    int key = threadId * operationsPerThread + i;
                    map.put(key, key * 2);
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(threadCount * operationsPerThread, map.size());
        executor.shutdown();
    }

    @Test
    @DisplayName("ReentrantLock should prevent race conditions")
    @Timeout(5)
    void testReentrantLock() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        int[] counter = {0};
        int threadCount = 10;
        int incrementsPerThread = 1000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    lock.lock();
                    try {
                        counter[0]++;
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(threadCount * incrementsPerThread, counter[0]);
    }

    @Test
    @DisplayName("StampedLock optimistic read should work")
    @Timeout(5)
    void testStampedLockOptimisticRead() {
        StampedLock lock = new StampedLock();
        int[] value = {42};

        // Optimistic read
        long stamp = lock.tryOptimisticRead();
        int readValue = value[0];

        if (lock.validate(stamp)) {
            assertEquals(42, readValue);
        } else {
            // Fallback to pessimistic read
            stamp = lock.readLock();
            try {
                readValue = value[0];
            } finally {
                lock.unlockRead(stamp);
            }
            assertEquals(42, readValue);
        }
    }

    @Test
    @DisplayName("CompareAndSet should work atomically")
    void testCompareAndSet() {
        AtomicInteger value = new AtomicInteger(10);

        assertTrue(value.compareAndSet(10, 20));
        assertEquals(20, value.get());

        assertFalse(value.compareAndSet(10, 30));
        assertEquals(20, value.get());
    }

    @Test
    @DisplayName("LongAdder should be efficient for high contention")
    @Timeout(5)
    void testLongAdder() throws Exception {
        java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
        int threadCount = 10;
        int incrementsPerThread = 10000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    adder.increment();
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(threadCount * incrementsPerThread, adder.sum());
    }

    @Test
    @DisplayName("BlockingQueue should handle producer-consumer pattern")
    @Timeout(5)
    void testBlockingQueue() throws Exception {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);
        int itemCount = 100;

        Thread producer = new Thread(() -> {
            for (int i = 0; i < itemCount; i++) {
                try {
                    queue.put(i);
                    produced.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < itemCount; i++) {
                try {
                    queue.take();
                    consumed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        assertEquals(itemCount, produced.get());
        assertEquals(itemCount, consumed.get());
    }
}
