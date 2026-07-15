package com.akiasync.scheduler;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

public final class TaskExecutionContext {
    private final long treeId;
    private final long taskId;
    private final long generation;
    private final BooleanSupplier cancellationRequested;

    TaskExecutionContext(long treeId, long taskId, long generation, BooleanSupplier cancellationRequested) {
        this.treeId = treeId;
        this.taskId = taskId;
        this.generation = generation;
        this.cancellationRequested = cancellationRequested;
    }

    public long treeId() {
        return treeId;
    }

    public long taskId() {
        return taskId;
    }

    public long generation() {
        return generation;
    }

    public boolean isCancellationRequested() {
        return cancellationRequested.getAsBoolean();
    }

    public void checkCancellation() {
        if (isCancellationRequested()) {
            throw new CancellationException("Task tree was cancelled or invalidated");
        }
    }
}
