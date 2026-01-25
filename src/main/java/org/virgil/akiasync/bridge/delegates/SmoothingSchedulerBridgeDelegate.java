package org.virgil.akiasync.bridge.delegates;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.ConfigManager;
import org.virgil.akiasync.executor.TaskSmoothingScheduler;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Delegate class handling task smoothing scheduler related bridge methods.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class SmoothingSchedulerBridgeDelegate {

    private final AkiAsyncPlugin plugin;
    private ConfigManager config;
    private final ExecutorService generalExecutor;

    private TaskSmoothingScheduler blockTickScheduler;
    private TaskSmoothingScheduler entityTickScheduler;
    private TaskSmoothingScheduler blockEntityScheduler;

    public SmoothingSchedulerBridgeDelegate(AkiAsyncPlugin plugin, ConfigManager config, ExecutorService generalExecutor) {
        this.plugin = plugin;
        this.config = config;
        this.generalExecutor = generalExecutor;
        initializeSmoothingSchedulers();
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ConfigManager is intentionally shared")
    public void updateConfig(ConfigManager newConfig) {
        this.config = newConfig;
    }

    private void initializeSmoothingSchedulers() {
        try {
            boolean isFolia = false;
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                // Not Folia
            }

            if (isFolia) {
                plugin.getLogger().info("[AkiAsync] Folia environment detected - TaskSmoothingScheduler disabled");
                return;
            }

            if (config != null && config.isChunkTickAsyncEnabled() && generalExecutor != null) {
                int batchSize = config.getChunkTickAsyncBatchSize();
                blockTickScheduler = new TaskSmoothingScheduler(
                    generalExecutor,
                    batchSize * 10,
                    batchSize * 2,
                    3
                );
                plugin.getLogger().info("[AkiAsync] BlockTick TaskSmoothingScheduler initialized");
            }

            if (config != null && config.isEntityTickParallel() && generalExecutor != null) {
                int batchSize = config.getEntityTickBatchSize();
                entityTickScheduler = new TaskSmoothingScheduler(
                    generalExecutor,
                    batchSize * 20,
                    batchSize * 3,
                    2
                );
                plugin.getLogger().info("[AkiAsync] EntityTick TaskSmoothingScheduler initialized");
            }

            if (config != null && config.isBlockEntityParallelTickEnabled() && generalExecutor != null) {
                int batchSize = config.getBlockEntityParallelBatchSize();
                blockEntityScheduler = new TaskSmoothingScheduler(
                    generalExecutor,
                    batchSize * 15,
                    batchSize * 2,
                    3
                );
                plugin.getLogger().info("[AkiAsync] BlockEntity TaskSmoothingScheduler initialized");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[AkiAsync] Failed to initialize TaskSmoothingSchedulers: " + e.getMessage());
        }
    }

    public Object getBlockTickSmoothingScheduler() {
        return blockTickScheduler;
    }

    public Object getEntityTickSmoothingScheduler() {
        return entityTickScheduler;
    }

    public Object getBlockEntitySmoothingScheduler() {
        return blockEntityScheduler;
    }

    public boolean submitSmoothTask(Object scheduler, Runnable task, int priority, String category) {
        if (scheduler == null || task == null) return false;

        try {
            if (scheduler instanceof TaskSmoothingScheduler smoothScheduler) {
                TaskSmoothingScheduler.Priority pri = switch (priority) {
                    case 0 -> TaskSmoothingScheduler.Priority.CRITICAL;
                    case 1 -> TaskSmoothingScheduler.Priority.HIGH;
                    case 2 -> TaskSmoothingScheduler.Priority.NORMAL;
                    case 3 -> TaskSmoothingScheduler.Priority.LOW;
                    default -> TaskSmoothingScheduler.Priority.NORMAL;
                };
                return smoothScheduler.submit(task, pri, category != null ? category : "Unknown");
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "SmoothingSchedulerBridgeDelegate", "submitSmoothTask", e);
        }
        return false;
    }

    public int submitSmoothTaskBatch(Object scheduler, List<Runnable> tasks, int priority, String category) {
        if (scheduler == null || tasks == null || tasks.isEmpty()) return 0;

        try {
            if (scheduler instanceof TaskSmoothingScheduler smoothScheduler) {
                TaskSmoothingScheduler.Priority pri = switch (priority) {
                    case 0 -> TaskSmoothingScheduler.Priority.CRITICAL;
                    case 1 -> TaskSmoothingScheduler.Priority.HIGH;
                    case 2 -> TaskSmoothingScheduler.Priority.NORMAL;
                    case 3 -> TaskSmoothingScheduler.Priority.LOW;
                    default -> TaskSmoothingScheduler.Priority.NORMAL;
                };

                int successCount = 0;
                String cat = category != null ? category : "Unknown";

                for (Runnable task : tasks) {
                    if (task != null && smoothScheduler.submit(task, pri, cat)) {
                        successCount++;
                    }
                }

                return successCount;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "SmoothingSchedulerBridgeDelegate", "submitSmoothTaskBatch", e);
        }
        return 0;
    }

    public void notifySmoothSchedulerTick(Object scheduler) {
        if (scheduler == null) return;

        try {
            if (scheduler instanceof TaskSmoothingScheduler smoothScheduler) {
                smoothScheduler.onTick();
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "SmoothingSchedulerBridgeDelegate", "notifySmoothSchedulerTick", e);
        }
    }

    public void updateSmoothSchedulerMetrics(Object scheduler, double tps, double mspt) {
        if (scheduler == null) return;

        try {
            if (scheduler instanceof TaskSmoothingScheduler smoothScheduler) {
                smoothScheduler.updatePerformanceMetrics(tps, mspt);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "SmoothingSchedulerBridgeDelegate", "updateSmoothSchedulerMetrics", e);
        }
    }
}
