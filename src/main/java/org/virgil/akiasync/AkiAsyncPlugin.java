package org.virgil.akiasync;

import org.bukkit.plugin.java.JavaPlugin;
import org.virgil.akiasync.bridge.AkiAsyncBridge;
import org.virgil.akiasync.cache.CacheManager;
import org.virgil.akiasync.command.ReloadCommand;
import org.virgil.akiasync.config.ConfigManager;
import org.virgil.akiasync.executor.AsyncExecutorManager;
import org.virgil.akiasync.listener.ConfigReloadListener;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@SuppressWarnings("unused")
public final class AkiAsyncPlugin extends JavaPlugin {
    
    private static AkiAsyncPlugin instance;
    private ConfigManager configManager;
    private AsyncExecutorManager executorManager;
    private AkiAsyncBridge bridge;
    private CacheManager cacheManager;
    private java.util.concurrent.ScheduledExecutorService metricsScheduler;
    
    @Override
    public void onEnable() {
        instance = this;
        
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        cacheManager = new CacheManager(this);
        executorManager = new AsyncExecutorManager(this);
        
        bridge = new AkiAsyncBridge(this, executorManager.getExecutorService(), executorManager.getLightingExecutor());
        BridgeManager.setBridge(bridge);
        
        getLogger().info("[AkiAsync] Bridge registered successfully (Leaves template pattern)");
        
        if (configManager.isTNTOptimizationEnabled()) {
            org.virgil.akiasync.mixin.async.TNTThreadPool.init(configManager.getTNTThreads());
            getLogger().info("[AkiAsync] TNT explosion optimization enabled with " + configManager.getTNTThreads() + " threads");
        }
        
        if (configManager.isAsyncHopperChainEnabled()) {
            getLogger().info("[AkiAsync] Hopper chain async I/O enabled with " + configManager.getHopperChainThreads() + " threads");
        }
        if (configManager.isAsyncVillagerBreedEnabled()) {
            getLogger().info("[AkiAsync] Villager breed async check enabled with " + configManager.getVillagerBreedThreads() + " threads");
        }
        
        BridgeManager.validateAndDisplayConfigurations();
        
        getServer().getPluginManager().registerEvents(new ConfigReloadListener(this), this);
        
        registerCommand("aki-reload", new ReloadCommand());
        
        if (configManager.isPerformanceMetricsEnabled()) {
            startCombinedMetrics();
        }
        
        getLogger().info("========================================");
        getLogger().info("  AkiAsync - Async Optimization Plugin");
        getLogger().info("========================================");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("Features enabled:");
        getLogger().info("  - Async Entity Tracker: " + configManager.isEntityTrackerEnabled());
        getLogger().info("  - Async Mob Spawning: " + configManager.isMobSpawningEnabled());
        getLogger().info("Thread pool size: " + configManager.getThreadPoolSize());
        getLogger().info("Density max-per-chunk: " + configManager.getMaxEntitiesPerChunk());
        getLogger().info("Brain throttle: " + configManager.isBrainThrottleEnabled() + ", interval: " + configManager.getBrainThrottleInterval());
        getLogger().info("EntityTickList Parallel: " + configManager.isEntityTickParallel() + ", threads: " + configManager.getEntityTickThreads() + ", min: " + configManager.getMinEntitiesForParallel());
        getLogger().info("Async Lighting (Enhanced): " + configManager.isAsyncLightingEnabled() + ", threads: " + configManager.getLightingThreadPoolSize() + ", 16-layer queue + dedup + dynamic batch");
        getLogger().info("Update interval: " + configManager.getUpdateIntervalTicks() + " ticks");
        getLogger().info("ServerCore optimizations: enabled");
        getLogger().info("FerriteCore memory optimizations: enabled");
        getLogger().info("========================================");
        getLogger().info("Plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        BridgeManager.clearBridge();
        
        if (metricsScheduler != null) {
            metricsScheduler.shutdownNow();
        }
        
        org.virgil.akiasync.mixin.async.TNTThreadPool.shutdown();
        
        org.virgil.akiasync.mixin.async.hopper.HopperChainExecutor.shutdown();
        org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor.shutdown();
        
        if (executorManager != null) {
            executorManager.shutdown();
        }
        getLogger().info("AkiAsync disabled. All async tasks have been gracefully shut down.");
        instance = null;
    }
    
    private void startCombinedMetrics() {
        metricsScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AkiAsync-Combined-Metrics");
            t.setDaemon(true);
            return t;
        });
        
        final long[] lastGeneralCompleted = {0};
        final long[] lastGeneralTotal = {0};
        
        metricsScheduler.scheduleAtFixedRate(() -> {
            try {
                java.util.concurrent.ThreadPoolExecutor generalExecutor = 
                    (java.util.concurrent.ThreadPoolExecutor) executorManager.getExecutorService();
                
                long genCompleted = generalExecutor.getCompletedTaskCount();
                long genTotal = generalExecutor.getTaskCount();
                long genCompletedPeriod = genCompleted - lastGeneralCompleted[0];
                long genSubmittedPeriod = genTotal - lastGeneralTotal[0];
                lastGeneralCompleted[0] = genCompleted;
                lastGeneralTotal[0] = genTotal;
                
                double generalThroughput = genCompletedPeriod / 60.0;
                
                getLogger().info(String.format(
                    "============== AkiAsync Metrics (60s period) =============="
                ));
                getLogger().info(String.format(
                    "[General Pool] Submitted: %d | Completed: %d (%.2f/s) | Active: %d/%d | Queue: %d",
                    genSubmittedPeriod, genCompletedPeriod, generalThroughput,
                    generalExecutor.getActiveCount(), generalExecutor.getPoolSize(),
                    generalExecutor.getQueue().size()
                ));
                getLogger().info(String.format(
                    "[Lifetime]     Completed: %d/%d tasks",
                    genCompleted, genTotal
                ));
                getLogger().info("===========================================================");
                
            } catch (Exception e) {
                getLogger().warning("[Metrics] Error: " + e.getMessage());
            }
        }, 60, 60, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    public static AkiAsyncPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public AsyncExecutorManager getExecutorManager() {
        return executorManager;
    }
    
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    
    public AkiAsyncBridge getBridge() {
        return bridge;
    }
    
    public void restartMetricsScheduler() {
        stopMetricsScheduler();
        
        startCombinedMetrics();
    }
    
    public void stopMetricsScheduler() {
        if (metricsScheduler != null) {
            metricsScheduler.shutdownNow();
            metricsScheduler = null;
        }
    }
    
}

