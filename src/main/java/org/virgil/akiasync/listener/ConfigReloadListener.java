package org.virgil.akiasync.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.event.ConfigReloadEvent;

public class ConfigReloadListener implements Listener {

    private final AkiAsyncPlugin plugin;

    public ConfigReloadListener(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConfigReload(ConfigReloadEvent event) {
        plugin.getLogger().info("[AkiAsync] Configuration reload event received, starting hot-reload...");
        plugin.getLogger().info("[AkiAsync] Using optimized reload strategy (C3H6N6O6-inspired)");

        plugin.getExecutorManager().getExecutorService().execute(() -> {
            performReload();
        });
    }

    private void performReload() {
        long startTime = System.currentTimeMillis();

        try {
            plugin.getLogger().info("[AkiAsync] Phase 1: Reloading configuration and clearing caches...");
            plugin.getConfigManager().reload();
            plugin.getCacheManager().invalidateAll();
            org.virgil.akiasync.mixin.metrics.AsyncMetrics.reset();

            plugin.getLogger().info("[AkiAsync] Phase 2: Resetting Mixin states...");
            org.virgil.akiasync.manager.MixinStateManager.resetAllMixinStates();

            plugin.getLogger().info("[AkiAsync] Phase 3: Restarting executors in controlled batches...");
            if (plugin.getConfigManager().isAsyncVillagerBreedEnabled()) {
                plugin.getLogger().info("[AkiAsync]   -> Restarting villager executor...");
                org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor.restartSmooth();
                Thread.sleep(150);
            }
            plugin.getLogger().info("[AkiAsync]   -> Restarting main executor manager...");
            plugin.getExecutorManager().restartSmooth();
            Thread.sleep(150);

            if (plugin.getConfigManager().isTNTOptimizationEnabled()) {
                plugin.getLogger().info("[AkiAsync]   -> Restarting TNT executor...");
                org.virgil.akiasync.mixin.async.TNTThreadPool.restartSmooth();
                Thread.sleep(100);
            }

            plugin.getLogger().info("[AkiAsync]   -> Restarting brain executor...");
            org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor.restartSmooth();
            Thread.sleep(150);

            if (plugin.getConfigManager().isChunkTickAsyncEnabled()) {
                plugin.getLogger().info("[AkiAsync]   -> Restarting chunk executor...");
                org.virgil.akiasync.mixin.async.chunk.ChunkTickExecutor.setThreadCount(
                    plugin.getConfigManager().getChunkTickThreads()
                );
                org.virgil.akiasync.mixin.async.chunk.ChunkTickExecutor.restartSmooth();
                Thread.sleep(150);
            }

            plugin.getLogger().info("[AkiAsync] Phase 4: Updating configuration and metrics...");
            plugin.getBridge().updateConfiguration(plugin.getConfigManager());

            Thread.sleep(100);
            if (plugin.getConfigManager().isPerformanceMetricsEnabled()) {
                plugin.restartMetricsScheduler();
                plugin.getLogger().info("[AkiAsync]   -> Metrics scheduler restarted");
            } else {
                plugin.stopMetricsScheduler();
                plugin.getLogger().info("[AkiAsync]   -> Metrics scheduler stopped");
            }

            Thread.sleep(100);
            plugin.getLogger().info("[AkiAsync] Phase 5: Validating configuration...");

            try {
                org.virgil.akiasync.mixin.bridge.BridgeManager.validateAndDisplayConfigurations();
            } catch (Exception e) {
                plugin.getLogger().warning("Configuration validation failed: " + e.getMessage());
            }

            long reloadTime = System.currentTimeMillis() - startTime;

            plugin.getLogger().info("========================================");
            plugin.getLogger().info("[AkiAsync] Hot-reload completed successfully!");
            plugin.getLogger().info("  - Total time: " + reloadTime + "ms");
            plugin.getLogger().info("  - Configuration reloaded from file");
            plugin.getLogger().info("  - All caches invalidated");
            plugin.getLogger().info("  - Thread pools smoothly restarted");
            plugin.getLogger().info("  - Mixin states reset");
            plugin.getLogger().info("  - MSPT impact: Minimized (controlled execution)");
            plugin.getLogger().info("========================================");

        } catch (Exception e) {
            plugin.getLogger().severe("[AkiAsync] Error during hot-reload: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
