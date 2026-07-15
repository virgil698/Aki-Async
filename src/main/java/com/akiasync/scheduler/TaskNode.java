package com.akiasync.scheduler;

import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public final class TaskNode<T> {
    private final TaskTree.Seal seal;
    private final String name;
    private final SchedulerPriority priority;
    private final Computation<T> computation;
    private final List<TaskNode<?>> children = new ArrayList<>();
    private final AtomicReference<TaskState> state = new AtomicReference<>(TaskState.CREATED);
    private final CompletableFuture<T> result = new CompletableFuture<>();
    private volatile long taskId;
    private volatile T value;
    private volatile PrioritisedExecutor.PrioritisedTask queuedTask;

    TaskNode(
            TaskTree.Seal seal,
            String name,
            SchedulerPriority priority,
            Computation<T> computation
    ) {
        this.seal = seal;
        this.name = requireName(name);
        this.priority = Objects.requireNonNull(priority, "priority");
        this.computation = Objects.requireNonNull(computation, "computation");
    }

    public <R> TaskNode<R> then(
            String childName,
            SchedulerPriority childPriority,
            Continuation<? super T, ? extends R> continuation
    ) {
        Objects.requireNonNull(continuation, "continuation");
        synchronized (seal) {
            seal.requireOpen();
            TaskNode<R> child = new TaskNode<>(
                    seal,
                    childName,
                    childPriority,
                    context -> continuation.run(value, context)
            );
            children.add(child);
            return child;
        }
    }

    public String name() {
        return name;
    }

    public SchedulerPriority priority() {
        return priority;
    }

    public TaskState state() {
        return state.get();
    }

    public long taskId() {
        return taskId;
    }

    public CompletionStage<T> result() {
        return result.minimalCompletionStage();
    }

    List<TaskNode<?>> children() {
        return children;
    }

    T compute(TaskExecutionContext context) throws Exception {
        return computation.run(context);
    }

    AtomicReference<TaskState> stateReference() {
        return state;
    }

    void taskId(long nextTaskId) {
        taskId = nextTaskId;
    }

    @SuppressWarnings("unchecked")
    void value(Object nextValue) {
        value = (T) nextValue;
    }

    PrioritisedExecutor.PrioritisedTask queuedTask() {
        return queuedTask;
    }

    void queuedTask(PrioritisedExecutor.PrioritisedTask task) {
        queuedTask = task;
    }

    @SuppressWarnings("unchecked")
    void completeResult(Object nextValue) {
        result.complete((T) nextValue);
    }

    void failResult(Throwable failure) {
        result.completeExceptionally(failure);
    }

    void cancelResult() {
        result.cancel(false);
    }

    private static String requireName(String name) {
        String validated = Objects.requireNonNull(name, "name").trim();
        if (validated.isEmpty()) {
            throw new IllegalArgumentException("Task name must not be blank");
        }
        if (validated.length() > 96) {
            throw new IllegalArgumentException("Task name must not exceed 96 characters");
        }
        return validated;
    }

    @FunctionalInterface
    interface Computation<T> {
        T run(TaskExecutionContext context) throws Exception;
    }

    @FunctionalInterface
    public interface Continuation<P, R> {
        R run(P parentResult, TaskExecutionContext context) throws Exception;
    }
}
