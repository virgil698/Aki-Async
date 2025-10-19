package org.virgil.akiasync;

import org.bukkit.plugin.java.JavaPlugin;
import org.virgil.akiasync.bridge.AkiAsyncBridge;
import org.virgil.akiasync.cache.CacheManager;
import org.virgil.akiasync.command.ReloadCommand;
import org.virgil.akiasync.command.DebugCommand;
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
        
        getLogger().info("§a[AkiAsync] Bridge registered successfully");
        
        if (configManager.isTNTOptimizationEnabled()) {
            org.virgil.akiasync.mixin.async.TNTThreadPool.init(configManager.getTNTThreads());
            getLogger().info("§e[AkiAsync] TNT explosion optimization enabled with " + configManager.getTNTThreads() + " threads");
        }
        
        if (configManager.isAsyncHopperChainEnabled()) {
            getLogger().info("§e[AkiAsync] Hopper chain async I/O enabled with " + configManager.getHopperChainThreads() + " threads");
        }
        if (configManager.isAsyncVillagerBreedEnabled()) {
            getLogger().info("§e[AkiAsync] Villager breed async check enabled with " + configManager.getVillagerBreedThreads() + " threads");
        }
        
        BridgeManager.validateAndDisplayConfigurations();
        
        getServer().getPluginManager().registerEvents(new ConfigReloadListener(this), this);
        
        registerCommand("aki-reload", new ReloadCommand());
        registerCommand("aki-debug", new DebugCommand(this));
        
        if (configManager.isPerformanceMetricsEnabled()) {
            startCombinedMetrics();
        }
        
        getLogger().info("§6========================================");
        getLogger().info("§6  §lAkiAsync §r§6- Async Optimization Plugin");
        getLogger().info("§6========================================");
        getLogger().info("§7Version: §f" + getDescription().getVersion());
        getLogger().info("§7Commands: §f/aki-reload §7| §f/aki-debug <true|false>");
        getLogger().info("");
        getLogger().info("§a• Core Features:");
        getLogger().info("§7  • Async Entity Tracker: §f" + (configManager.isEntityTrackerEnabled() ? "§aEnabled" : "§cDisabled"));
        getLogger().info("§7  • Async Mob Spawning: §f" + (configManager.isMobSpawningEnabled() ? "§aEnabled" : "§cDisabled"));
        getLogger().info("§7  • Entity Tick Parallel: §f" + (configManager.isEntityTickParallel() ? "§aEnabled" : "§cDisabled") + " §7(§f" + configManager.getEntityTickThreads() + " threads§7)");
        getLogger().info("§7  • Async Lighting: §f" + (configManager.isAsyncLightingEnabled() ? "§aEnabled" : "§cDisabled") + " §7(§f" + configManager.getLightingThreadPoolSize() + " threads§7)");
        getLogger().info("");
        getLogger().info("§e• Performance Settings:");
        getLogger().info("§7  • Thread Pool Size: §f" + configManager.getThreadPoolSize());
        getLogger().info("§7  • Max Entities/Chunk: §f" + configManager.getMaxEntitiesPerChunk());
        getLogger().info("§7  • Brain Throttle: §f" + (configManager.isBrainThrottleEnabled() ? "§aEnabled" : "§cDisabled") + " §7(§f" + configManager.getBrainThrottleInterval() + " ticks§7)");
        getLogger().info("§7  • Update Interval: §f" + configManager.getUpdateIntervalTicks() + " ticks");
        getLogger().info("");
        getLogger().info("§b• Optimizations:");
        getLogger().info("§7  • ServerCore optimizations: §aEnabled");
        getLogger().info("§7  • FerriteCore memory optimizations: §aEnabled");
        getLogger().info("§7  • 16-layer lighting queue: §aEnabled");
        getLogger().info("§6========================================");
        getLogger().info("§a§lPlugin enabled successfully! §7Use §f/aki-reload §7to reload config");
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
        getLogger().info("§c[AkiAsync] Plugin disabled. All async tasks have been gracefully shut down.");
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
                if (!configManager.isDebugLoggingEnabled()) {
                    return;
                }
                
                java.util.concurrent.ThreadPoolExecutor generalExecutor = 
                    (java.util.concurrent.ThreadPoolExecutor) executorManager.getExecutorService();
                
                long genCompleted = generalExecutor.getCompletedTaskCount();
                long genTotal = generalExecutor.getTaskCount();
                long genCompletedPeriod = genCompleted - lastGeneralCompleted[0];
                long genSubmittedPeriod = genTotal - lastGeneralTotal[0];
                lastGeneralCompleted[0] = genCompleted;
                lastGeneralTotal[0] = genTotal;
                
                double generalThroughput = genCompletedPeriod / 60.0;
                
                getLogger().info("§6============== AkiAsync Metrics (60s period) ==============");
                getLogger().info(String.format(
                    "§7[General Pool] §fSubmitted: %d §7| §fCompleted: %d §7(§f%.2f/s§7) | §fActive: %d/%d §7| §fQueue: %d",
                    genSubmittedPeriod, genCompletedPeriod, generalThroughput,
                    generalExecutor.getActiveCount(), generalExecutor.getPoolSize(),
                    generalExecutor.getQueue().size()
                ));
                getLogger().info(String.format(
                    "§7[Lifetime]     §fCompleted: %d/%d tasks",
                    genCompleted, genTotal
                ));
                getLogger().info("§6===========================================================");
                
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

