package org.virgil.akiasync.mixin.optimization.scheduler;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

public class WorkStealingTaskScheduler {

    private final int parallelism;
    private final ExecutorService executor;
    private final AtomicInteger taskIndex = new AtomicInteger();
    private final CountDownLatch finishedLatch;
    private final BlockingQueue<Runnable> mainThreadTasks = new LinkedBlockingQueue<>(2000);

    private static final WorkStealingTaskScheduler INSTANCE = new WorkStealingTaskScheduler();

    public static WorkStealingTaskScheduler getInstance() {
        return INSTANCE;
    }

    private WorkStealingTaskScheduler() {
        this.parallelism = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.finishedLatch = new CountDownLatch(parallelism);

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            this.executor = bridge.getGeneralExecutor();
            if (bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync] WorkStealingTaskScheduler using shared General Executor");
            }
        } else {

            this.executor = Executors.newFixedThreadPool(parallelism, r -> {
                Thread t = new Thread(r, "AkiAsync-WorkStealing-Fallback");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });
        }
    }

    public <T> void processBatch(T[] items, Consumer<T> processor, int batchSize) {
        if (items == null || items.length == 0) {
            return;
        }

        taskIndex.set(0);
        CountDownLatch latch = new CountDownLatch(parallelism);

        for (int i = 0; i < parallelism; i++) {
            executor.execute(() -> {
                try {
                    processWorkStealingTasks(items, processor, batchSize);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            while (!latch.await(1, TimeUnit.MILLISECONDS)) {
                runMainThreadTasks();
                if (taskIndex.get() < items.length) {
                    handleBatchTasks(items, processor, Math.min(batchSize, 5));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.errorLog("[AkiAsync-WorkStealing] Batch processing interrupted");
            }
        }

        runMainThreadTasks();
    }

    private <T> void processWorkStealingTasks(T[] items, Consumer<T> processor, int batchSize) {
        while (handleBatchTasks(items, processor, batchSize)) {

        }
    }

    private <T> boolean handleBatchTasks(T[] items, Consumer<T> processor, int batchSize) {
        int startIndex = taskIndex.getAndAdd(batchSize);

        if (startIndex >= items.length) {
            return false;
        }

        int endIndex = Math.min(startIndex + batchSize, items.length);

        for (int i = startIndex; i < endIndex; i++) {
            if (items[i] != null) {
                try {
                    processor.accept(items[i]);
                } catch (Exception e) {
                    Bridge bridge = BridgeManager.getBridge();
                    if (bridge != null && bridge.isDebugLoggingEnabled()) {
                        bridge.debugLog("[AkiAsync-WorkStealing] Task processing error: " + e.getMessage());
                    }
                }
            }
        }

        return true;
    }

    public void scheduleMainThreadTask(Runnable task) {
        if (!mainThreadTasks.offer(task)) {
            try {
                task.run();
            } catch (Exception e) {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null && bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync-WorkStealing] Task execution error (queue full): " + e.getMessage());
                }
            }
        }
    }

    private void runMainThreadTasks() {
        Runnable task;
        int processed = 0;

        while ((task = mainThreadTasks.poll()) != null && processed < 10) {
            try {
                task.run();
                processed++;
            } catch (Exception e) {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null && bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync-WorkStealing] Main thread task error: " + e.getMessage());
                }
            }
        }
    }

    public <T> void processAdaptiveBatch(T[] items, Consumer<T> processor) {
        if (items == null || items.length == 0) {
            return;
        }

        int optimalBatchSize = Math.max(1, items.length / (parallelism * 4));
        optimalBatchSize = Math.min(optimalBatchSize, 50);

        processBatch(items, processor, optimalBatchSize);
    }

    public <T> void parallelForEach(T[] items, Consumer<T> processor) {
        processAdaptiveBatch(items, processor);
    }

    public SchedulerStats getStats() {
        return new SchedulerStats(
            parallelism,
            taskIndex.get(),
            (int) finishedLatch.getCount(),
            mainThreadTasks.size(),
            !executor.isShutdown()
        );
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class SchedulerStats {
        public final int parallelism;
        public final int currentTaskIndex;
        public final int finishedTasks;
        public final int pendingMainThreadTasks;
        public final boolean isActive;

        public SchedulerStats(int parallelism, int currentTaskIndex, int finishedTasks,
                            int pendingMainThreadTasks, boolean isActive) {
            this.parallelism = parallelism;
            this.currentTaskIndex = currentTaskIndex;
            this.finishedTasks = finishedTasks;
            this.pendingMainThreadTasks = pendingMainThreadTasks;
            this.isActive = isActive;
        }

        @Override
        public String toString() {
            return String.format("SchedulerStats{parallelism=%d, taskIndex=%d, finished=%d, pending=%d, active=%s}",
                parallelism, currentTaskIndex, finishedTasks, pendingMainThreadTasks, isActive);
        }
    }
}
