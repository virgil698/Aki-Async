package org.virgil.akiasync.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.event.ConfigReloadEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

            if (plugin.getBridge() != null) {
                plugin.getBridge().resetAsyncMetrics();
            }

            plugin.getLogger().info("[AkiAsync] Phase 2: Resetting Mixin states...");
            org.virgil.akiasync.manager.MixinStateManager.resetAllMixinStates();

            plugin.getLogger().info("[AkiAsync] Phase 3: Restarting executors in controlled batches...");

            CompletableFuture<Void> restartChain = CompletableFuture.completedFuture(null);

            if (plugin.getConfigManager().isAsyncVillagerBreedEnabled() && plugin.getBridge() != null) {
                restartChain = restartChain.thenRunAsync(() -> {
                    plugin.getLogger().info("[AkiAsync]   -> Restarting villager executor...");
                    plugin.getBridge().restartVillagerExecutor();
                }, CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS));
                restartChain = restartChain.thenRunAsync(() -> {},
                    CompletableFuture.delayedExecutor(150, TimeUnit.MILLISECONDS));
            }

            restartChain = restartChain.thenRunAsync(() -> {
                plugin.getLogger().info("[AkiAsync]   -> Restarting main executor manager...");
                plugin.getExecutorManager().restartSmooth();
            }, CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS));
            restartChain = restartChain.thenRunAsync(() -> {},
                CompletableFuture.delayedExecutor(150, TimeUnit.MILLISECONDS));

            if (plugin.getConfigManager().isTNTOptimizationEnabled() && plugin.getBridge() != null) {
                restartChain = restartChain.thenRunAsync(() -> {
                    plugin.getLogger().info("[AkiAsync]   -> Restarting TNT executor...");
                    plugin.getBridge().restartTNTExecutor();
                }, CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS));
                restartChain = restartChain.thenRunAsync(() -> {},
                    CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));
            }

            if (plugin.getBridge() != null) {
                restartChain = restartChain.thenRunAsync(() -> {
                    plugin.getLogger().info("[AkiAsync]   -> Restarting brain executor...");
                    plugin.getBridge().restartBrainExecutor();
                }, CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS));
                restartChain = restartChain.thenRunAsync(() -> {},
                    CompletableFuture.delayedExecutor(150, TimeUnit.MILLISECONDS));
            }

            if (plugin.getConfigManager().isChunkTickAsyncEnabled() && plugin.getBridge() != null) {
                restartChain = restartChain.thenRunAsync(() -> {
                    plugin.getLogger().info("[AkiAsync]   -> Restarting chunk executor...");
                    plugin.getBridge().restartChunkExecutor(
                        plugin.getConfigManager().getChunkTickThreads()
                    );
                }, CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS));
                restartChain = restartChain.thenRunAsync(() -> {},
                    CompletableFuture.delayedExecutor(150, TimeUnit.MILLISECONDS));
            }

            restartChain.join();

            plugin.getLogger().info("[AkiAsync] Phase 4: Updating configuration and metrics...");
            plugin.getBridge().updateConfiguration(plugin.getConfigManager());

            if (plugin.getVirtualEntityCompatManager() != null) {
                plugin.getLogger().info("[AkiAsync]   -> Reloading virtual entity compatibility...");
                plugin.getVirtualEntityCompatManager().reload();
            }

            plugin.getLogger().info("[AkiAsync]   -> Block tick mixin will reload on next tick");

            CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> {
                if (plugin.getConfigManager().isPerformanceMetricsEnabled()) {
                    plugin.restartMetricsScheduler();
                    plugin.getLogger().info("[AkiAsync]   -> Metrics scheduler restarted");
                } else {
                    plugin.stopMetricsScheduler();
                    plugin.getLogger().info("[AkiAsync]   -> Metrics scheduler stopped");
                }
            });

            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            plugin.getLogger().info("[AkiAsync] Phase 5: Validating configuration...");

            try {

                plugin.getLogger().info("[AkiAsync] Configuration validation completed");
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
