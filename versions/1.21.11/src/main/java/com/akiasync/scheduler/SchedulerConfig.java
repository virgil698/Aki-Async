package com.akiasync.scheduler;

public record SchedulerConfig(
        int workerThreads,
        int maxOutstandingTasks,
        int maxTreeNodes,
        int coordinatorBatchSize,
        int maxOwnerTasks,
        long shutdownWaitMillis
) {
    public SchedulerConfig {
        if (workerThreads < 1) {
            throw new IllegalArgumentException("workerThreads must be positive");
        }
        if (maxOutstandingTasks < 1) {
            throw new IllegalArgumentException("maxOutstandingTasks must be positive");
        }
        if (maxTreeNodes < 1 || maxTreeNodes > maxOutstandingTasks) {
            throw new IllegalArgumentException("maxTreeNodes must be between 1 and maxOutstandingTasks");
        }
        if (coordinatorBatchSize < 1) {
            throw new IllegalArgumentException("coordinatorBatchSize must be positive");
        }
        if (maxOwnerTasks < 1) {
            throw new IllegalArgumentException("maxOwnerTasks must be positive");
        }
        if (shutdownWaitMillis < 1) {
            throw new IllegalArgumentException("shutdownWaitMillis must be positive");
        }
    }

    public static SchedulerConfig defaults() {
        int processors = Runtime.getRuntime().availableProcessors();
        int workers = Math.max(1, Math.min(4, processors / 2));
        return new SchedulerConfig(workers, 8_192, 1_024, 64, 4_096, 2_000);
    }
}
