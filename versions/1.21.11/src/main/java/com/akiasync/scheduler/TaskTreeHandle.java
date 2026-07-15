package com.akiasync.scheduler;

import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;

public final class TaskTreeHandle<T> {
    private final long treeId;
    private final long generation;
    private final TaskNode<T> root;
    private final CompletionStage<TaskTreeResult> completion;
    private final BooleanSupplier cancellation;

    TaskTreeHandle(
            long treeId,
            long generation,
            TaskNode<T> root,
            CompletionStage<TaskTreeResult> completion,
            BooleanSupplier cancellation
    ) {
        this.treeId = treeId;
        this.generation = generation;
        this.root = root;
        this.completion = completion;
        this.cancellation = cancellation;
    }

    public long treeId() {
        return treeId;
    }

    public long generation() {
        return generation;
    }

    public TaskNode<T> root() {
        return root;
    }

    public CompletionStage<TaskTreeResult> completion() {
        return completion;
    }

    public boolean cancel() {
        return cancellation.getAsBoolean();
    }
}
