package com.akiasync.scheduler.config;

public record SchedulerSnapshot(
        SchedulerState state,
        long generation,
        int workerThreads,
        int busyWorkers,
        int activeTrees,
        int outstandingTasks,
        int taskCapacity,
        long queuedComputeTasks,
        int coordinatorBacklog,
        int pendingOwnerTasks,
        long submittedTrees,
        long completedTrees,
        long failedTrees,
        long cancelledTrees,
        long rejectedTrees
) {
}
