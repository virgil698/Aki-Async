package com.akiasync.scheduler;

import ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue;
import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.executor.queue.PrioritisedTaskQueue;
import ca.spottedleaf.concurrentutil.util.Priority;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

final class RedisTaskScheduler implements AutoCloseable {
    private final SchedulerConfig config;
    private final Consumer<Throwable> errorHandler;
    private final Object lifecycleLock = new Object();
    private final AtomicReference<SchedulerState> state = new AtomicReference<>(SchedulerState.NEW);
    private final AtomicLong generation = new AtomicLong();
    private final AtomicLong treeIds = new AtomicLong();
    private final AtomicLong taskIds = new AtomicLong();
    private final AtomicInteger outstandingTasks = new AtomicInteger();
    private final AtomicInteger activeTrees = new AtomicInteger();
    private final AtomicInteger runningWorkers = new AtomicInteger();
    private final LongAdder submittedTrees = new LongAdder();
    private final LongAdder completedTrees = new LongAdder();
    private final LongAdder failedTrees = new LongAdder();
    private final LongAdder cancelledTrees = new LongAdder();
    private final LongAdder rejectedTrees = new LongAdder();
    private final MultiThreadedQueue<Runnable> coordinatorQueue = new MultiThreadedQueue<>();
    private final Semaphore coordinatorSignal = new Semaphore(0);
    private final Semaphore computeSignal = new Semaphore(0);
    private final PrioritisedTaskQueue computeQueue = new PrioritisedTaskQueue(
            new AtomicLong(),
            0L,
            ignored -> computeSignal.release()
    );
    private final Map<Long, TreeRun<?>> trees = new HashMap<>();
    private final Thread coordinatorThread;
    private final Thread[] workerThreads;
    private final CountDownLatch coordinatorStopped = new CountDownLatch(1);
    private final AtomicBoolean workersStopping = new AtomicBoolean();
    private boolean coordinatorStopping;

    RedisTaskScheduler(String threadPrefix, SchedulerConfig config, Consumer<Throwable> errorHandler) {
        Objects.requireNonNull(threadPrefix, "threadPrefix");
        this.config = Objects.requireNonNull(config, "config");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
        coordinatorThread = daemonThread(threadPrefix + "-Coordinator", this::coordinatorLoop);
        workerThreads = new Thread[config.workerThreads()];
        for (int index = 0; index < workerThreads.length; index++) {
            workerThreads[index] = daemonThread(threadPrefix + "-Worker-" + (index + 1), this::workerLoop);
        }
    }

    void start() {
        synchronized (lifecycleLock) {
            if (!state.compareAndSet(SchedulerState.NEW, SchedulerState.RUNNING)) {
                throw new IllegalStateException("Scheduler can only be started once");
            }
            generation.incrementAndGet();
            coordinatorThread.start();
            for (Thread worker : workerThreads) {
                worker.start();
            }
        }
    }

    <T> TaskTreeHandle<T> submit(TaskTree<T> tree) {
        Objects.requireNonNull(tree, "tree");
        TaskTree.PreparedTree<T> prepared = tree.prepare();
        int nodeCount = prepared.nodes().size();
        RejectedExecutionException rejection = null;
        TreeRun<T> run = null;

        synchronized (lifecycleLock) {
            if (state.get() != SchedulerState.RUNNING) {
                rejection = new RejectedExecutionException("Scheduler is not running");
            } else if (nodeCount > config.maxTreeNodes()) {
                rejection = new RejectedExecutionException(
                        "Task tree contains " + nodeCount + " nodes; maximum is " + config.maxTreeNodes()
                );
            } else if (!reserve(nodeCount)) {
                rejection = new RejectedExecutionException("Scheduler task capacity is exhausted");
            } else {
                long currentGeneration = generation.get();
                run = new TreeRun<>(
                        treeIds.incrementAndGet(),
                        currentGeneration,
                        prepared.root(),
                        prepared.nodes()
                );
                TreeRun<T> accepted = run;
                submittedTrees.increment();
                postControl(() -> register(accepted));
            }
        }

        if (rejection != null) {
            rejectedTrees.increment();
            reject(prepared.nodes(), rejection);
            throw rejection;
        }
        return run.handle();
    }

    boolean isGenerationCurrent(long expectedGeneration) {
        return state.get() == SchedulerState.RUNNING && generation.get() == expectedGeneration;
    }

    long generation() {
        return generation.get();
    }

    SchedulerSnapshot snapshot(int pendingOwnerTasks) {
        int busy = runningWorkers.get();
        long queued = Math.max(
                0L,
                computeQueue.getTotalTasksScheduled() - computeQueue.getTotalTasksExecuted() - busy
        );
        return new SchedulerSnapshot(
                state.get(),
                generation.get(),
                workerThreads.length,
                busy,
                activeTrees.get(),
                outstandingTasks.get(),
                config.maxOutstandingTasks(),
                queued,
                coordinatorQueue.size(),
                pendingOwnerTasks,
                submittedTrees.sum(),
                completedTrees.sum(),
                failedTrees.sum(),
                cancelledTrees.sum(),
                rejectedTrees.sum()
        );
    }

    @Override
    public void close() {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(config.shutdownWaitMillis());
        synchronized (lifecycleLock) {
            SchedulerState current = state.get();
            if (current == SchedulerState.STOPPED || current == SchedulerState.STOPPING) {
                return;
            }
            if (current == SchedulerState.NEW) {
                generation.incrementAndGet();
                state.set(SchedulerState.STOPPED);
                return;
            }
            state.set(SchedulerState.STOPPING);
            generation.incrementAndGet();
            postControl(this::beginCoordinatorShutdown);
        }

        awaitCoordinator(deadline);
        computeQueue.shutdown();
        workersStopping.set(true);
        computeSignal.release(workerThreads.length);
        joinWorkers(deadline);
        coordinatorQueue.clear();
        state.set(SchedulerState.STOPPED);
    }

    private void register(TreeRun<?> tree) {
        trees.put(tree.id, tree);
        activeTrees.incrementAndGet();
        for (TaskNode<?> node : tree.nodes) {
            node.stateReference().set(TaskState.WAITING);
        }
        if (state.get() != SchedulerState.RUNNING
                || tree.generation != generation.get()
                || tree.cancellationRequested.get()) {
            cancelTree(tree);
            return;
        }
        dispatch(tree, tree.root);
    }

    private void dispatch(TreeRun<?> tree, TaskNode<?> node) {
        if (state.get() != SchedulerState.RUNNING || tree.cancellationRequested.get()) {
            cancelTree(tree);
            return;
        }
        if (!node.stateReference().compareAndSet(TaskState.WAITING, TaskState.QUEUED)) {
            return;
        }

        node.taskId(taskIds.incrementAndGet());
        try {
            PrioritisedExecutor.PrioritisedTask task = computeQueue.createTask(
                    () -> execute(tree, node),
                    node.priority().delegate()
            );
            node.queuedTask(task);
            task.queue();
        } catch (RuntimeException | Error failure) {
            completeFailure(tree, node, failure);
        }
    }

    private void execute(TreeRun<?> tree, TaskNode<?> node) {
        if (!node.stateReference().compareAndSet(TaskState.QUEUED, TaskState.RUNNING)) {
            return;
        }
        runningWorkers.incrementAndGet();
        TaskExecutionContext context = new TaskExecutionContext(
                tree.id,
                node.taskId(),
                tree.generation,
                () -> tree.cancellationRequested.get() || !isGenerationCurrent(tree.generation)
        );
        try {
            context.checkCancellation();
            Object value = node.compute(context);
            context.checkCancellation();
            postControl(() -> completeSuccess(tree, node, value));
        } catch (CancellationException cancellation) {
            postControl(() -> completeCancellation(tree, node));
        } catch (Throwable failure) {
            postControl(() -> completeFailure(tree, node, failure));
        } finally {
            runningWorkers.decrementAndGet();
        }
    }

    private void completeSuccess(TreeRun<?> tree, TaskNode<?> node, Object value) {
        if (node.state() != TaskState.RUNNING) {
            return;
        }
        if (tree.cancellationRequested.get() || tree.generation != generation.get()) {
            completeCancellation(tree, node);
            return;
        }

        node.value(value);
        node.stateReference().set(TaskState.SUCCEEDED);
        publishCompletion(() -> node.completeResult(value));
        finishNode(tree, TaskState.SUCCEEDED);
        for (TaskNode<?> child : node.children()) {
            dispatch(tree, child);
        }
    }

    private void completeFailure(TreeRun<?> tree, TaskNode<?> node, Throwable failure) {
        TaskState current = node.state();
        if (current.isTerminal()) {
            return;
        }
        PrioritisedExecutor.PrioritisedTask queued = node.queuedTask();
        if (current == TaskState.QUEUED && queued != null) {
            queued.cancel();
        }
        node.stateReference().set(TaskState.FAILED);
        publishCompletion(() -> node.failResult(failure));
        finishNode(tree, TaskState.FAILED);
        skipDescendants(tree, node, failure);
    }

    private void completeCancellation(TreeRun<?> tree, TaskNode<?> node) {
        if (node.state().isTerminal()) {
            return;
        }
        node.stateReference().set(TaskState.CANCELLED);
        publishCompletion(node::cancelResult);
        finishNode(tree, TaskState.CANCELLED);
    }

    private void skipDescendants(TreeRun<?> tree, TaskNode<?> failedNode, Throwable failure) {
        ArrayDeque<TaskNode<?>> pending = new ArrayDeque<>(failedNode.children());
        while (!pending.isEmpty()) {
            TaskNode<?> node = pending.removeFirst();
            if (!node.state().isTerminal()) {
                node.stateReference().set(TaskState.SKIPPED);
                IllegalStateException skipped = new IllegalStateException(
                        "Task '" + node.name() + "' was skipped because an ancestor failed",
                        failure
                );
                publishCompletion(() -> node.failResult(skipped));
                finishNode(tree, TaskState.SKIPPED);
            }
            pending.addAll(node.children());
        }
    }

    private void cancelTree(TreeRun<?> tree) {
        tree.cancellationRequested.set(true);
        for (TaskNode<?> node : tree.nodes) {
            TaskState current = node.state();
            if (current.isTerminal()) {
                continue;
            }
            PrioritisedExecutor.PrioritisedTask queued = node.queuedTask();
            if (queued != null) {
                queued.cancel();
            }
            node.stateReference().set(TaskState.CANCELLED);
            publishCompletion(node::cancelResult);
            finishNode(tree, TaskState.CANCELLED);
        }
    }

    private void finishNode(TreeRun<?> tree, TaskState terminalState) {
        outstandingTasks.decrementAndGet();
        switch (terminalState) {
            case SUCCEEDED -> tree.succeeded++;
            case FAILED -> tree.failed++;
            case SKIPPED -> tree.skipped++;
            case CANCELLED -> tree.cancelled++;
            default -> throw new IllegalArgumentException("Not a terminal state: " + terminalState);
        }
        tree.remaining--;
        if (tree.remaining == 0) {
            finishTree(tree);
        }
    }

    private void finishTree(TreeRun<?> tree) {
        tree.finished.set(true);
        if (trees.remove(tree.id) != null) {
            activeTrees.decrementAndGet();
        }
        completedTrees.increment();
        if (tree.failed > 0 || tree.skipped > 0) {
            failedTrees.increment();
        }
        if (tree.cancelled > 0) {
            cancelledTrees.increment();
        }
        TaskTreeResult result = new TaskTreeResult(
                tree.id,
                tree.generation,
                tree.nodes.size(),
                tree.succeeded,
                tree.failed,
                tree.skipped,
                tree.cancelled,
                System.nanoTime() - tree.startedNanos
        );
        publishCompletion(() -> tree.completion.complete(result));
    }

    private void beginCoordinatorShutdown() {
        coordinatorStopping = true;
        for (TreeRun<?> tree : new ArrayList<>(trees.values())) {
            cancelTree(tree);
        }
    }

    private void coordinatorLoop() {
        try {
            while (true) {
                try {
                    coordinatorSignal.acquire();
                } catch (InterruptedException interrupted) {
                    if (state.get() == SchedulerState.STOPPING) {
                        break;
                    }
                    Thread.currentThread().interrupt();
                    break;
                }

                int processed = 0;
                Runnable command;
                while (processed < config.coordinatorBatchSize()
                        && (command = coordinatorQueue.poll()) != null) {
                    try {
                        command.run();
                    } catch (Throwable failure) {
                        report(failure);
                    }
                    processed++;
                }

                if (!coordinatorQueue.isEmpty()) {
                    coordinatorSignal.release();
                }
                if (coordinatorStopping && trees.isEmpty() && coordinatorQueue.isEmpty()) {
                    break;
                }
            }
        } finally {
            coordinatorStopped.countDown();
        }
    }

    private void workerLoop() {
        while (true) {
            try {
                computeSignal.acquire();
            } catch (InterruptedException interrupted) {
                if (workersStopping.get()) {
                    return;
                }
                continue;
            }
            if (workersStopping.get() && computeQueue.hasNoScheduledTasks()) {
                return;
            }
            try {
                computeQueue.executeTask();
            } catch (Throwable failure) {
                report(failure);
            }
            if (workersStopping.get() && computeQueue.hasNoScheduledTasks()) {
                computeSignal.release(workerThreads.length);
                return;
            }
        }
    }

    private boolean reserve(int nodeCount) {
        while (true) {
            int current = outstandingTasks.get();
            if (nodeCount > config.maxOutstandingTasks() - current) {
                return false;
            }
            if (outstandingTasks.compareAndSet(current, current + nodeCount)) {
                return true;
            }
        }
    }

    private void reject(List<TaskNode<?>> nodes, RejectedExecutionException rejection) {
        for (TaskNode<?> node : nodes) {
            node.stateReference().set(TaskState.FAILED);
            node.failResult(rejection);
        }
    }

    private void postControl(Runnable command) {
        coordinatorQueue.offer(command);
        coordinatorSignal.release();
    }

    private void publishCompletion(Runnable completion) {
        try {
            computeQueue.queueTask(completion, Priority.HIGHER);
        } catch (RuntimeException | Error failure) {
            completion.run();
            report(failure);
        }
    }

    private void awaitCoordinator(long deadline) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0L) {
            coordinatorThread.interrupt();
            return;
        }
        try {
            if (!coordinatorStopped.await(remaining, TimeUnit.NANOSECONDS)) {
                coordinatorThread.interrupt();
            }
        } catch (InterruptedException interrupted) {
            coordinatorThread.interrupt();
            Thread.currentThread().interrupt();
        }
    }

    private void joinWorkers(long deadline) {
        for (Thread worker : workerThreads) {
            if (worker == Thread.currentThread()) {
                continue;
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0L) {
                worker.interrupt();
                continue;
            }
            try {
                worker.join(Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remaining)));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            if (worker.isAlive()) {
                worker.interrupt();
            }
        }
    }

    private Thread daemonThread(String name, Runnable run) {
        Thread thread = new Thread(run, name);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((ignored, failure) -> report(failure));
        return thread;
    }

    private void report(Throwable failure) {
        try {
            errorHandler.accept(failure);
        } catch (Throwable ignored) {
            // Error reporting must not terminate the coordinator or a worker.
        }
    }

    private final class TreeRun<T> {
        private final long id;
        private final long generation;
        private final TaskNode<T> root;
        private final List<TaskNode<?>> nodes;
        private final long startedNanos = System.nanoTime();
        private final AtomicBoolean cancellationRequested = new AtomicBoolean();
        private final AtomicBoolean finished = new AtomicBoolean();
        private final CompletableFuture<TaskTreeResult> completion = new CompletableFuture<>();
        private int remaining;
        private int succeeded;
        private int failed;
        private int skipped;
        private int cancelled;

        private TreeRun(long id, long generation, TaskNode<T> root, List<TaskNode<?>> nodes) {
            this.id = id;
            this.generation = generation;
            this.root = root;
            this.nodes = nodes;
            remaining = nodes.size();
        }

        private TaskTreeHandle<T> handle() {
            return new TaskTreeHandle<>(
                    id,
                    generation,
                    root,
                    completion.minimalCompletionStage(),
                    () -> {
                        if (finished.get() || !cancellationRequested.compareAndSet(false, true)) {
                            return false;
                        }
                        postControl(() -> cancelTree(this));
                        return true;
                    }
            );
        }
    }
}
