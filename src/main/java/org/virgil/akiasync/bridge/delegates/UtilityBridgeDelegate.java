package org.virgil.akiasync.bridge.delegates;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.ConfigManager;

/**
 * Delegate class handling utility bridge methods like TPS, MSPT, main thread tasks.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class UtilityBridgeDelegate {

    private final AkiAsyncPlugin plugin;
    private ConfigManager config;

    public UtilityBridgeDelegate(AkiAsyncPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ConfigManager is intentionally shared")
    public void updateConfig(ConfigManager newConfig) {
        this.config = newConfig;
    }

    public void runOnMainThread(Runnable task) {
        if (task == null) return;

        try {
            org.virgil.akiasync.compat.FoliaSchedulerAdapter.runTask(plugin, task);
        } catch (Exception e) {
            plugin.getLogger().warning("[AkiAsync] Failed to run task on main thread: " + e.getMessage());
            try {
                task.run();
            } catch (Exception ex) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "UtilityBridgeDelegate", "runTaskFallback", ex);
            }
        }
    }

    public double getCurrentTPS() {
        try {
            return plugin.getServer().getTPS()[0];
        } catch (Exception e) {
            return 20.0;
        }
    }

    public double getCurrentMSPT() {
        try {
            long[] tickTimes = plugin.getServer().getTickTimes();
            if (tickTimes.length > 0) {
                long sum = 0;
                int count = Math.min(100, tickTimes.length);
                for (int i = 0; i < count; i++) {
                    sum += tickTimes[i];
                }
                return (sum / (double) count) / 1_000_000.0;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "UtilityBridgeDelegate", "getCurrentMSPT", e);
        }
        return 50.0;
    }

    public int getLightingParallelism() {
        int poolSize = config.getThreadPoolSize();
        if (poolSize <= 0) {
            return Math.max(1, Runtime.getRuntime().availableProcessors() / 3);
        }
        return poolSize;
    }

    public String getBlockId(net.minecraft.world.level.block.Block block) {
        try {
            net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            return key.toString();
        } catch (Exception e) {
            return block.getClass().getSimpleName().toLowerCase();
        }
    }
}
