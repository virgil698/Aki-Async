package com.akiasync.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisTaskSchedulerTest {
    private RedisTaskScheduler scheduler;

    @AfterEach
    void stopScheduler() {
        if (scheduler != null) {
            scheduler.close();
        }
    }

    @Test
    void siblingBranchesExecuteConcurrently() throws Exception {
        scheduler = start(config(2, 16, 16));
        CyclicBarrier barrier = new CyclicBarrier(2);
        Set<String> workerNames = ConcurrentHashMap.newKeySet();
        TaskTree<Integer> tree = TaskTree.root("root", SchedulerPriority.NORMAL, context -> 5);
        TaskNode<Integer> doubled = tree.root().then("double", SchedulerPriority.HIGH, (value, context) -> {
            workerNames.add(Thread.currentThread().getName());
            barrier.await(2, TimeUnit.SECONDS);
            return value * 2;
        });
        TaskNode<Integer> tripled = tree.root().then("triple", SchedulerPriority.NORMAL, (value, context) -> {
            workerNames.add(Thread.currentThread().getName());
            barrier.await(2, TimeUnit.SECONDS);
            return value * 3;
        });

        TaskTreeResult result = scheduler.submit(tree).completion().toCompletableFuture().get(3, TimeUnit.SECONDS);

        assertTrue(result.successful());
        assertEquals(3, result.succeededTasks());
        assertEquals(10, doubled.result().toCompletableFuture().get(1, TimeUnit.SECONDS));
        assertEquals(15, tripled.result().toCompletableFuture().get(1, TimeUnit.SECONDS));
        assertEquals(2, workerNames.size());
        assertEquals(0, scheduler.snapshot(0).outstandingTasks());
    }

    @Test
    void failureSkipsOnlyItsDescendants() throws Exception {
        scheduler = start(config(2, 16, 16));
        TaskTree<Integer> tree = TaskTree.root("root", SchedulerPriority.NORMAL, context -> 4);
        TaskNode<Integer> failed = tree.root().then("failed", SchedulerPriority.HIGH, (value, context) -> {
            throw new IllegalStateException("expected failure");
        });
        TaskNode<Integer> skipped = failed.then("skipped", SchedulerPriority.NORMAL, (value, context) -> value + 1);
        TaskNode<Integer> sibling = tree.root().then("sibling", SchedulerPriority.NORMAL, (value, context) -> value * 3);

        TaskTreeResult result = scheduler.submit(tree).completion().toCompletableFuture().get(3, TimeUnit.SECONDS);

        assertFalse(result.successful());
        assertEquals(2, result.succeededTasks());
        assertEquals(1, result.failedTasks());
        assertEquals(1, result.skippedTasks());
        assertEquals(TaskState.FAILED, failed.state());
        assertEquals(TaskState.SKIPPED, skipped.state());
        assertEquals(12, sibling.result().toCompletableFuture().get(1, TimeUnit.SECONDS));
        assertThrows(Exception.class, () -> skipped.result().toCompletableFuture().get(1, TimeUnit.SECONDS));
    }

    @Test
    void wholeTreeReservationAppliesBackpressure() throws Exception {
        scheduler = start(config(2, 3, 3));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TaskTree<Integer> occupying = TaskTree.root("occupying", SchedulerPriority.NORMAL, context -> {
            started.countDown();
            assertTrue(release.await(2, TimeUnit.SECONDS));
            return 1;
        });
        occupying.root().then("child-a", SchedulerPriority.NORMAL, (value, context) -> value + 1);
        occupying.root().then("child-b", SchedulerPriority.NORMAL, (value, context) -> value + 2);
        TaskTreeHandle<Integer> accepted = scheduler.submit(occupying);
        assertTrue(started.await(1, TimeUnit.SECONDS));

        TaskTree<Integer> rejected = TaskTree.root("rejected", SchedulerPriority.NORMAL, context -> 2);
        assertThrows(RejectedExecutionException.class, () -> scheduler.submit(rejected));
        assertEquals(TaskState.FAILED, rejected.root().state());

        release.countDown();
        assertTrue(accepted.completion().toCompletableFuture().get(3, TimeUnit.SECONDS).successful());
        SchedulerSnapshot snapshot = scheduler.snapshot(0);
        assertEquals(1, snapshot.rejectedTrees());
        assertEquals(0, snapshot.outstandingTasks());
    }

    @Test
    void cancellationPropagatesAcrossTheTree() throws Exception {
        scheduler = start(config(2, 8, 8));
        CountDownLatch started = new CountDownLatch(1);
        TaskTree<Integer> tree = TaskTree.root("root", SchedulerPriority.NORMAL, context -> {
            started.countDown();
            while (!context.isCancellationRequested()) {
                LockSupport.parkNanos(100_000L);
            }
            context.checkCancellation();
            return 1;
        });
        TaskNode<Integer> child = tree.root().then("child", SchedulerPriority.NORMAL, (value, context) -> value + 1);
        TaskTreeHandle<Integer> handle = scheduler.submit(tree);
        assertTrue(started.await(1, TimeUnit.SECONDS));

        assertTrue(handle.cancel());
        assertFalse(handle.cancel());
        TaskTreeResult result = handle.completion().toCompletableFuture().get(3, TimeUnit.SECONDS);

        assertEquals(2, result.cancelledTasks());
        assertEquals(TaskState.CANCELLED, tree.root().state());
        assertEquals(TaskState.CANCELLED, child.state());
        assertEquals(0, scheduler.snapshot(0).outstandingTasks());
    }

    @Test
    void shutdownInvalidatesGenerationAndCancelsActiveTrees() throws Exception {
        scheduler = start(config(1, 4, 4));
        CountDownLatch started = new CountDownLatch(1);
        TaskTree<Integer> tree = TaskTree.root("root", SchedulerPriority.NORMAL, context -> {
            started.countDown();
            while (!context.isCancellationRequested()) {
                LockSupport.parkNanos(100_000L);
            }
            context.checkCancellation();
            return 1;
        });
        TaskTreeHandle<Integer> handle = scheduler.submit(tree);
        assertTrue(started.await(1, TimeUnit.SECONDS));
        long generation = handle.generation();

        scheduler.close();

        assertFalse(scheduler.isGenerationCurrent(generation));
        assertEquals(SchedulerState.STOPPED, scheduler.snapshot(0).state());
        assertEquals(1, handle.completion().toCompletableFuture().get(1, TimeUnit.SECONDS).cancelledTasks());
    }

    @Test
    void concurrentProducersDoNotLoseOrDuplicateTrees() throws Exception {
        scheduler = start(config(4, 512, 1));
        int producerCount = 8;
        int treesPerProducer = 50;
        int expectedTrees = producerCount * treesPerProducer;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger executions = new AtomicInteger();
        List<CompletableFuture<TaskTreeResult>> completions = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> submissions = new ArrayList<>();
        ExecutorService producers = Executors.newFixedThreadPool(producerCount);
        try {
            for (int producer = 0; producer < producerCount; producer++) {
                submissions.add(producers.submit(() -> {
                    assertTrue(start.await(1, TimeUnit.SECONDS));
                    for (int index = 0; index < treesPerProducer; index++) {
                        TaskTree<Integer> tree = TaskTree.root(
                                "tree-" + index,
                                SchedulerPriority.NORMAL,
                                context -> executions.incrementAndGet()
                        );
                        completions.add(scheduler.submit(tree).completion().toCompletableFuture());
                    }
                    return null;
                }));
            }
            start.countDown();
            producers.shutdown();
            assertTrue(producers.awaitTermination(5, TimeUnit.SECONDS));
            for (Future<?> submission : submissions) {
                submission.get(1, TimeUnit.SECONDS);
            }
        } finally {
            producers.shutdownNow();
        }

        for (CompletableFuture<TaskTreeResult> completion : List.copyOf(completions)) {
            assertTrue(completion.get(5, TimeUnit.SECONDS).successful());
        }
        assertEquals(expectedTrees, completions.size());
        assertEquals(expectedTrees, executions.get());
        SchedulerSnapshot snapshot = scheduler.snapshot(0);
        assertEquals(expectedTrees, snapshot.submittedTrees());
        assertEquals(expectedTrees, snapshot.completedTrees());
        assertEquals(0, snapshot.outstandingTasks());
    }

    @Test
    void resultCallbacksNeverRunOnTheCoordinator() throws Exception {
        scheduler = start(config(2, 8, 8));
        TaskTree<Integer> tree = TaskTree.root("root", SchedulerPriority.NORMAL, context -> 7);
        CountDownLatch callbackRan = new CountDownLatch(1);
        AtomicReference<String> callbackThread = new AtomicReference<>();
        tree.root().result().thenRun(() -> {
            callbackThread.set(Thread.currentThread().getName());
            callbackRan.countDown();
        });

        assertTrue(scheduler.submit(tree).completion().toCompletableFuture().get(3, TimeUnit.SECONDS).successful());
        assertTrue(callbackRan.await(1, TimeUnit.SECONDS));
        assertTrue(callbackThread.get().startsWith("Aki-Test-Worker-"));
    }

    private static RedisTaskScheduler start(SchedulerConfig config) {
        RedisTaskScheduler scheduler = new RedisTaskScheduler("Aki-Test", config, failure -> {
            throw new AssertionError("Unexpected scheduler failure", failure);
        });
        scheduler.start();
        return scheduler;
    }

    private static SchedulerConfig config(int workers, int capacity, int maxTreeNodes) {
        return new SchedulerConfig(workers, capacity, maxTreeNodes, 8, 8, 2_000);
    }
}
