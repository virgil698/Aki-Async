package org.virgil.akiasync;

import org.bukkit.plugin.java.JavaPlugin;
import org.virgil.akiasync.bridge.AkiAsyncBridge;
import org.virgil.akiasync.cache.CacheManager;
import org.virgil.akiasync.command.DebugCommand;
import org.virgil.akiasync.command.ReloadCommand;
import org.virgil.akiasync.command.VersionCommand;
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
    private org.virgil.akiasync.throttling.EntityThrottlingManager throttlingManager;
    private java.util.concurrent.ScheduledExecutorService metricsScheduler;
    
    @Override
    public void onEnable() {
        instance = this;
        
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        cacheManager = new CacheManager(this);
        executorManager = new AsyncExecutorManager(this);
        
        bridge = new AkiAsyncBridge(
            this, 
            executorManager.getExecutorService(), 
            executorManager.getLightingExecutor(), 
            executorManager.getTNTExecutor(),
            executorManager.getChunkTickExecutor(),
            executorManager.getVillagerBreedExecutor(),
            executorManager.getBrainExecutor()
        );
        BridgeManager.setBridge(bridge);
        
        org.virgil.akiasync.util.VirtualEntityDetector.setLogger(getLogger(), configManager.isDebugLoggingEnabled());
        
        getLogger().info("[AkiAsync] Bridge registered successfully");
        
        if (configManager.isAsyncPathfindingEnabled()) {
            org.virgil.akiasync.mixin.pathfinding.AsyncPathProcessor.initialize();
            getLogger().info("[AkiAsync] Async pathfinding enabled with " + configManager.getAsyncPathfindingMaxThreads() + " threads");
        }
        
        if (configManager.isEntityThrottlingEnabled()) {
            throttlingManager = new org.virgil.akiasync.throttling.EntityThrottlingManager(this);
            throttlingManager.initialize();
        }
        
        if (configManager.isTNTOptimizationEnabled()) {
            org.virgil.akiasync.mixin.async.TNTThreadPool.init(configManager.getTNTThreads());
            getLogger().info("[AkiAsync] TNT explosion optimization enabled with " + configManager.getTNTThreads() + " threads");
        }
        
        if (configManager.isAsyncVillagerBreedEnabled()) {
            getLogger().info("[AkiAsync] Villager breed async check enabled with " + configManager.getVillagerBreedThreads() + " threads");
        }
        
        if (configManager.isStructureLocationAsyncEnabled()) {
            org.virgil.akiasync.mixin.async.StructureLocatorBridge.initialize();
            org.virgil.akiasync.async.structure.OptimizedStructureLocator.initialize(this);
            getLogger().info("[AkiAsync] Async structure location enabled with " + configManager.getStructureLocationThreads() + " threads");
            if (configManager.isStructureAlgorithmOptimizationEnabled()) {
                getLogger().info("[AkiAsync] Structure search algorithm optimization enabled (" + configManager.getStructureSearchPattern() + " pattern)");
            }
        }
        
        if (configManager.isDataPackOptimizationEnabled()) {
            org.virgil.akiasync.async.datapack.DataPackLoadOptimizer.getInstance(this);
            getLogger().info("[AkiAsync] DataPack loading optimization enabled with " + 
                configManager.getDataPackFileLoadThreads() + " file threads, " + 
                configManager.getDataPackZipProcessThreads() + " zip threads");
        }
        
        BridgeManager.validateAndDisplayConfigurations();
        
        getServer().getPluginManager().registerEvents(new ConfigReloadListener(this), this);
        
        registerCommand("aki-reload", new ReloadCommand(this));
        registerCommand("aki-debug", new DebugCommand(this));
        registerCommand("aki-version", new VersionCommand(this));
        
        if (configManager.isPerformanceMetricsEnabled()) {
            startCombinedMetrics();
        }
        
        getLogger().info("========================================");
        getLogger().info("  AkiAsync - Async Optimization Plugin");
        getLogger().info("========================================");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("Commands: /aki-reload | /aki-debug | /aki-version");
        getLogger().info("");
        getLogger().info("[+] Core Features:");
        getLogger().info("  [+] Async Entity Tracker: " + (configManager.isEntityTrackerEnabled() ? "Enabled" : "Disabled"));
        getLogger().info("  [+] Async Mob Spawning: " + (configManager.isMobSpawningEnabled() ? "Enabled" : "Disabled"));
        getLogger().info("  [+] Entity Tick Parallel: " + (configManager.isEntityTickParallel() ? "Enabled" : "Disabled") + " (" + configManager.getEntityTickThreads() + " threads)");
        getLogger().info("  [+] Async Lighting: " + (configManager.isAsyncLightingEnabled() ? "Enabled" : "Disabled") + " (" + configManager.getLightingThreadPoolSize() + " threads)");
        getLogger().info("");
        getLogger().info("[*] Performance Settings:");
        getLogger().info("  [*] Thread Pool Size: " + configManager.getThreadPoolSize());
        getLogger().info("  [*] Max Entities/Chunk: " + configManager.getMaxEntitiesPerChunk());
        getLogger().info("  [*] Brain Throttle: " + (configManager.isBrainThrottleEnabled() ? "Enabled" : "Disabled") + " (" + configManager.getBrainThrottleInterval() + " ticks)");
        getLogger().info("  [*] Update Interval: " + configManager.getUpdateIntervalTicks() + " ticks");
        getLogger().info("");
        getLogger().info("[#] Optimizations:");
        getLogger().info("  [#] ServerCore optimizations: Enabled");
        getLogger().info("  [#] FerriteCore memory optimizations: Enabled");
        getLogger().info("  [#] 16-layer lighting queue: Enabled");
        getLogger().info("========================================");
        getLogger().info("Plugin enabled successfully! Use /aki-version for details");
    }
    
    @Override
    public void onDisable() {
        BridgeManager.clearBridge();
        
        if (metricsScheduler != null) {
            metricsScheduler.shutdownNow();
        }
        
        org.virgil.akiasync.mixin.pathfinding.AsyncPathProcessor.shutdown();
        
        if (throttlingManager != null) {
            throttlingManager.shutdown();
        }
        
        org.virgil.akiasync.mixin.async.TNTThreadPool.shutdown();
        
        org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor.shutdown();

        org.virgil.akiasync.mixin.async.StructureLocatorBridge.shutdown();
        
        org.virgil.akiasync.async.structure.OptimizedStructureLocator.shutdown();
        
        org.virgil.akiasync.async.datapack.DataPackLoadOptimizer optimizer = 
            org.virgil.akiasync.async.datapack.DataPackLoadOptimizer.getInstance();
        if (optimizer != null) {
            optimizer.shutdown();
        }
        
        if (executorManager != null) {
            executorManager.shutdown();
        }
        getLogger().info("[AkiAsync] Plugin disabled. All async tasks have been gracefully shut down.");
        instance = null;
    }
    
    private void startCombinedMetrics() {
        metricsScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AkiAsync-Combined-Metrics");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
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
                
                getLogger().info("============== AkiAsync Metrics (60s period) ==============");
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
    
    public org.virgil.akiasync.throttling.EntityThrottlingManager getThrottlingManager() {
        return throttlingManager;
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
    
    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        org.bukkit.command.PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        }
    }
    
}