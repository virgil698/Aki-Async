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
        
        try {
            plugin.getConfigManager().reload();
            plugin.getCacheManager().invalidateAll();
            
            org.virgil.akiasync.mixin.metrics.AsyncMetrics.reset();
            plugin.getLogger().info("[AkiAsync] Configuration reloaded from file");
            
            plugin.getExecutorManager().restartSmooth();
            
            if (plugin.getConfigManager().isTNTOptimizationEnabled()) {
                org.virgil.akiasync.mixin.async.TNTThreadPool.restartSmooth();
            }
            
            if (plugin.getConfigManager().isAsyncVillagerBreedEnabled()) {
                org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor.restartSmooth();
            }
            
            org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor.restartSmooth();
            
            plugin.getBridge().updateConfiguration(plugin.getConfigManager());
            
            if (plugin.getConfigManager().isPerformanceMetricsEnabled()) {
                plugin.restartMetricsScheduler();
                plugin.getLogger().info("[AkiAsync] Metrics scheduler restarted");
            } else {
                plugin.stopMetricsScheduler();
                plugin.getLogger().info("[AkiAsync] Metrics scheduler stopped");
            }
            
            org.virgil.akiasync.mixin.bridge.BridgeManager.validateAndDisplayConfigurations();
            
            plugin.getLogger().info("[AkiAsync] Config hot-reloaded, all caches invalidated.");
            plugin.getLogger().info("[AkiAsync] Thread pools smoothly restarted with new configuration.");
            plugin.getLogger().info("[AkiAsync] Hot-reload completed successfully!");
            
        } catch (Exception e) {
            plugin.getLogger().severe("[AkiAsync] Error during hot-reload: " + e.getMessage());
            e.printStackTrace();
        }
    }
}