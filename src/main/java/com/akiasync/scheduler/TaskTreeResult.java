package com.akiasync.scheduler;

public record TaskTreeResult(
        long treeId,
        long generation,
        int totalTasks,
        int succeededTasks,
        int failedTasks,
        int skippedTasks,
        int cancelledTasks,
        long durationNanos
) {
    public boolean successful() {
        return failedTasks == 0 && skippedTasks == 0 && cancelledTasks == 0;
    }
}
