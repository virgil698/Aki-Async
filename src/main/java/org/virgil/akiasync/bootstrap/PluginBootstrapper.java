package org.virgil.akiasync.bootstrap;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.bridge.AkiAsyncBridge;
import org.virgil.akiasync.cache.CacheManager;
import org.virgil.akiasync.chunk.ChunkLoadPriorityScheduler;
import org.virgil.akiasync.config.ConfigManager;
import org.virgil.akiasync.executor.AsyncExecutorManager;
import org.virgil.akiasync.language.LanguageManager;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.logging.Logger;

public class PluginBootstrapper {

    private final AkiAsyncPlugin plugin;
    private final Logger logger;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private CacheManager cacheManager;
    private AsyncExecutorManager executorManager;
    private AkiAsyncBridge bridge;
    private org.virgil.akiasync.throttling.EntityThrottlingManager throttlingManager;
    private ChunkLoadPriorityScheduler chunkLoadScheduler;
    private java.util.concurrent.ScheduledExecutorService metricsScheduler;
    private org.virgil.akiasync.compat.VirtualEntityCompatManager virtualEntityCompatManager;
    private org.virgil.akiasync.crypto.QuantumSeedManager quantumSeedManager;

    public PluginBootstrapper(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void bootstrap() {
        initializeMetrics();
        initializeConfig();
        initializeLanguage();
        initializeCoreServices();
        initializeBridge();
        initializeCompatibility();
        initializeNetworkOptimizations();
        initializeAsyncSystems();
        initializeSeedEncryption();
        initializeStructureLocation();
        initializeDataPack();
        registerListeners();
        registerCommands();
        initializeSchedulers();
        printStartupBanner();
    }

    private void initializeMetrics() {
        org.virgil.akiasync.util.DebugLogger.setLogger(plugin);

        int pluginId = 28019;
        new org.bstats.bukkit.Metrics(plugin, pluginId);
        logger.info("[AkiAsync] bStats metrics initialized (ID: " + pluginId + ")");
    }

    private void initializeConfig() {
        configManager = new ConfigManager(plugin);
        configManager.loadConfig();
    }

    private void initializeLanguage() {
        languageManager = new LanguageManager(plugin);
        languageManager.initialize();
    }

    private void initializeCoreServices() {
        cacheManager = new CacheManager(plugin);
        executorManager = new AsyncExecutorManager(plugin);
    }

    private void initializeBridge() {
        bridge = new AkiAsyncBridge(
            plugin,
            executorManager.getExecutorService(),
            executorManager.getLightingExecutor(),
            executorManager.getTNTExecutor(),
            executorManager.getChunkTickExecutor(),
            executorManager.getVillagerBreedExecutor(),
            executorManager.getBrainExecutor(),
            executorManager.getCollisionExecutor()
        );

        org.virgil.akiasync.mixin.util.AsyncCollisionProcessor.setExecutor(
            executorManager.getCollisionExecutor()
        );
        BridgeManager.setBridge(bridge);

        org.virgil.akiasync.util.VirtualEntityDetector.setLogger(logger, configManager.isDebugLoggingEnabled());

        logger.info("[AkiAsync] Bridge registered successfully");
    }

    private void initializeCompatibility() {
        org.virgil.akiasync.compat.ViaVersionCompat.initialize();

        virtualEntityCompatManager = new org.virgil.akiasync.compat.VirtualEntityCompatManager(plugin);
        virtualEntityCompatManager.initialize();

        if (virtualEntityCompatManager.isEnabled()) {
            org.virgil.akiasync.util.VirtualEntityDetector.setDetectorRegistry(
                virtualEntityCompatManager.getDetectorRegistry()
            );

            java.util.Map<String, Boolean> availability = virtualEntityCompatManager.getPluginAvailability();
            if (!availability.isEmpty()) {
                logger.info("[VirtualEntity] Plugin availability status:");
                for (java.util.Map.Entry<String, Boolean> entry : availability.entrySet()) {
                    logger.info("  - " + entry.getKey() + ": " +
                               (entry.getValue() ? "Available" : "Not found"));
                }
            }
        }
    }

    private void initializeNetworkOptimizations() {
        if (configManager.isAdvancedNetworkOptimizationEnabled()) {
            org.virgil.akiasync.network.NetworkOptimizationManager.initialize(
                true,
                configManager.isFastVarIntEnabled(),
                configManager.isEventLoopAffinityEnabled(),
                configManager.isByteBufOptimizerEnabled(),
                configManager.isStrictEventLoopChecking(),
                configManager.isPooledByteBufAllocator(),
                configManager.isDirectByteBufPreferred()
            );

            logger.info("[AkiAsync] Network optimization initialized:");
            logger.info("  - FastVarInt: " + (configManager.isFastVarIntEnabled() ? "Enabled" : "Disabled"));
            logger.info("  - EventLoop Affinity: " + (configManager.isEventLoopAffinityEnabled() ? "Enabled (Strict: " + configManager.isStrictEventLoopChecking() + ")" : "Disabled"));
            logger.info("  - ByteBuf Optimizer: " + (configManager.isByteBufOptimizerEnabled() ? "Enabled (Pooled: " + configManager.isPooledByteBufAllocator() + ", Direct: " + configManager.isDirectByteBufPreferred() + ")" : "Disabled"));
        }

        if (configManager.isMultiNettyEventLoopEnabled()) {
            org.virgil.akiasync.network.MultiNettyEventLoopManager.initialize(true);
            logger.info("[AkiAsync] VMP Multi-Netty Event Loop enabled");
        }
    }

    private void initializeAsyncSystems() {
        if (configManager.isAsyncPathfindingEnabled()) {
            org.virgil.akiasync.mixin.pathfinding.AsyncPathProcessor.initialize();
            logger.info("[AkiAsync] Async pathfinding enabled with " + configManager.getAsyncPathfindingMaxThreads() + " threads");
        }

        if (configManager.isEntityThrottlingEnabled()) {
            throttlingManager = new org.virgil.akiasync.throttling.EntityThrottlingManager(plugin);
            throttlingManager.initialize();
        }

        if (configManager.isTNTOptimizationEnabled()) {
            org.virgil.akiasync.mixin.async.TNTThreadPool.init(configManager.getTNTThreads());
            logger.info("[AkiAsync] TNT explosion optimization enabled with " + configManager.getTNTThreads() + " threads");
        }

        if (configManager.isAsyncVillagerBreedEnabled()) {
            logger.info("[AkiAsync] Villager breed async check enabled with " + configManager.getVillagerBreedThreads() + " threads");
        }
    }

    private void initializeSeedEncryption() {
        if (!configManager.isSeedEncryptionEnabled()) {
            return;
        }

        if (configManager.isQuantumSeedEnabled()) {
            quantumSeedManager = new org.virgil.akiasync.crypto.QuantumSeedManager(
                plugin,
                configManager.getQuantumSeedCacheSize(),
                configManager.isQuantumSeedEnableTimeDecay(),
                configManager.isQuantumSeedDebugLogging()
            );
            quantumSeedManager.initialize();
            logger.info("[AkiAsync] QuantumSeed encryption enabled (Level " + configManager.getQuantumSeedEncryptionLevel() + ")");
            logger.info("[AkiAsync] QuantumSeed features: IbRNG full integration + Regional chunk obfuscation + 6-layer encryption");
        } else if (configManager.isSecureSeedEnabled()) {
            long worldSeed = plugin.getServer().getWorlds().get(0).getSeed();
            bridge.initializeSecureSeed(worldSeed);
            logger.info("[AkiAsync] SecureSeed encryption enabled (" + configManager.getSecureSeedBits() + " bits)");
            logger.info("[AkiAsync] SecureSeed features: BLAKE2b哈希 + 1024位种子空间");
        }
    }

    private void initializeStructureLocation() {
        if (!configManager.isStructureLocationAsyncEnabled()) {
            return;
        }

        org.virgil.akiasync.mixin.async.StructureLocatorBridge.initialize();
        org.virgil.akiasync.mixin.async.structure.OptimizedStructureLocator.initialize(plugin);
        logger.info("[AkiAsync] Async structure location enabled with " + configManager.getStructureLocationThreads() + " threads");

        if (configManager.isStructureAlgorithmOptimizationEnabled()) {
            logger.info("[AkiAsync] Structure search algorithm optimization enabled (" + configManager.getStructureSearchPattern() + " pattern)");
        }
    }

    private void initializeDataPack() {
        if (!configManager.isDataPackOptimizationEnabled()) {
            return;
        }

        org.virgil.akiasync.async.datapack.DataPackLoadOptimizer.getInstance(plugin);
        logger.info("[AkiAsync] DataPack loading optimization enabled with " +
            configManager.getDataPackFileLoadThreads() + " file threads, " +
            configManager.getDataPackZipProcessThreads() + " zip threads");
    }

    private void registerListeners() {
        BridgeManager.validateAndDisplayConfigurations();

        plugin.getServer().getPluginManager().registerEvents(
            new org.virgil.akiasync.listener.ConfigReloadListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(
            new org.virgil.akiasync.listener.WorldUnloadListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(
            new org.virgil.akiasync.listener.PlayerPathPrewarmListener(plugin), plugin);

        if (configManager.isSeedCommandRestrictionEnabled()) {
            plugin.getServer().getPluginManager().registerEvents(
                new org.virgil.akiasync.listener.SeedCommandListener(plugin), plugin);
            logger.info("[AkiAsync] /seed command restriction enabled (OP only)");
        }
    }

    private void registerCommands() {
        registerBasicCommand("aki-reload", new org.virgil.akiasync.command.ReloadCommand(plugin));
        registerBasicCommand("aki-debug", new org.virgil.akiasync.command.DebugCommand(plugin));
        registerBasicCommand("aki-version", new org.virgil.akiasync.command.VersionCommand(plugin));
        registerBasicCommand("aki-network", new org.virgil.akiasync.command.NetworkCommand(plugin));
        registerBasicCommand("aki-packets", new org.virgil.akiasync.command.PacketStatsCommand(plugin));

        org.virgil.akiasync.network.NetworkTrafficMonitor.getInstance(plugin);
    }

    private void registerBasicCommand(String name, io.papermc.paper.command.brigadier.BasicCommand executor) {
        plugin.getLifecycleManager().registerEventHandler(
            io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
            event -> event.registrar().register(name, executor)
        );
    }

    private void initializeSchedulers() {
        if (configManager.isFastMovementChunkLoadEnabled()) {
            chunkLoadScheduler = new ChunkLoadPriorityScheduler(configManager);
            chunkLoadScheduler.start();
            logger.info("[AkiAsync] Chunk load priority scheduler enabled");
        }

        if (configManager.isPerformanceMetricsEnabled()) {
            startCombinedMetrics();
        }
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

                logger.info("============== AkiAsync Metrics (60s period) ==============");
                logger.info(String.format(
                    "[General Pool] Submitted: %d | Completed: %d (%.2f/s) | Active: %d/%d | Queue: %d",
                    genSubmittedPeriod, genCompletedPeriod, generalThroughput,
                    generalExecutor.getActiveCount(), generalExecutor.getPoolSize(),
                    generalExecutor.getQueue().size()
                ));
                logger.info(String.format(
                    "[Lifetime]     Completed: %d/%d tasks",
                    genCompleted, genTotal
                ));
                logger.info("===========================================================");

            } catch (Exception e) {
                logger.warning("[Metrics] Error: " + e.getMessage());
            }
        }, 60, 60, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void printStartupBanner() {
        logger.info("========================================");
        logger.info("  AkiAsync - Async Optimization Plugin");
        logger.info("========================================");
        logger.info("Version: " + plugin.getDescription().getVersion());
        logger.info("Commands: /aki-reload | /aki-debug | /aki-version | /aki-network | /aki-packets");
        logger.info("");
        logger.info("[+] Core Features:");
        logger.info("  [+] Multithreaded Entity Tracker: " + (configManager.isMultithreadedEntityTrackerEnabled() ? "Enabled" : "Disabled"));
        logger.info("  [+] Async Mob Spawning: " + (configManager.isMobSpawningEnabled() ? "Enabled" : "Disabled"));
        logger.info("  [+] Entity Tick Parallel: " + (configManager.isEntityTickParallel() ? "Enabled" : "Disabled") + " (using general pool: " + configManager.getThreadPoolSize() + " threads)");
        logger.info("  [+] Async Lighting: " + (configManager.isAsyncLightingEnabled() ? "Enabled" : "Disabled") + " (" + configManager.getLightingThreadPoolSize() + " threads)");
        logger.info("");
        logger.info("[*] Performance Settings:");
        logger.info("  [*] General Thread Pool Size: " + configManager.getThreadPoolSize() + " (CPU cores: " + Runtime.getRuntime().availableProcessors() + ")");
        logger.info("  [*] Max Entities/Chunk: " + configManager.getMaxEntitiesPerChunk());
        logger.info("  [*] Brain Throttle: " + (configManager.isBrainThrottleEnabled() ? "Enabled" : "Disabled") + " (" + configManager.getBrainThrottleInterval() + " ticks)");
        logger.info("");
        logger.info("[#] Optimizations:");
        logger.info("  [#] ServerCore optimizations: Enabled");
        logger.info("  [#] FerriteCore memory optimizations: Enabled");
        logger.info("  [#] 16-layer lighting queue: Enabled");
        logger.info("========================================");
        logger.info("Plugin enabled successfully! Use /aki-version for details");
    }

    public void shutdown() {
        logger.info("=== AkiAsync Shutdown Sequence Starting ===");
        long startTime = System.currentTimeMillis();

        logger.info("Phase 1: Disconnecting bridge and stopping new tasks...");
        BridgeManager.clearBridge();

        logger.info("Phase 2: Shutting down high-priority components...");

        org.virgil.akiasync.network.NetworkTrafficMonitor networkMonitor =
            org.virgil.akiasync.network.NetworkTrafficMonitor.getInstance();
        if (networkMonitor != null) {
            networkMonitor.shutdown();
        }

        if (metricsScheduler != null) {
            metricsScheduler.shutdownNow();
        }

        if (throttlingManager != null) {
            throttlingManager.shutdown();
        }

        if (virtualEntityCompatManager != null) {
            virtualEntityCompatManager.shutdown();
        }

        logger.info("Phase 3: Shutting down async processors...");

        org.virgil.akiasync.mixin.pathfinding.AsyncPathProcessor.shutdown();

        try {
            org.virgil.akiasync.mixin.pathfinding.EnhancedPathfindingInitializer.shutdown();
            logger.info("EnhancedPathfindingSystem shutdown completed");
        } catch (Exception e) {
            logger.warning("Failed to shutdown EnhancedPathfindingSystem: " + e.getMessage());
        }

        try {
            org.virgil.akiasync.mixin.crypto.quantum.AsyncSeedEncryptor.shutdown();
            logger.info("AsyncSeedEncryptor shutdown completed");
        } catch (Exception e) {
            logger.warning("Failed to shutdown AsyncSeedEncryptor: " + e.getMessage());
        }

        org.virgil.akiasync.mixin.async.TNTThreadPool.shutdown();

        try {
            org.virgil.akiasync.mixin.async.structure.OptimizedStructureLocator.shutdown();
            logger.info("OptimizedStructureLocator shutdown completed");
        } catch (Exception e) {
            logger.warning("Failed to shutdown OptimizedStructureLocator: " + e.getMessage());
        }

        org.virgil.akiasync.async.datapack.DataPackLoadOptimizer optimizer =
            org.virgil.akiasync.async.datapack.DataPackLoadOptimizer.getInstance();
        if (optimizer != null) {
            optimizer.shutdown();
        }

        try {
            org.virgil.akiasync.network.NetworkOptimizationManager.shutdown();
            logger.info("NetworkOptimizationManager shutdown completed");
        } catch (Exception e) {
            logger.warning("Failed to shutdown NetworkOptimizationManager: " + e.getMessage());
        }

        try {
            org.virgil.akiasync.network.MultiNettyEventLoopManager.shutdown();
            logger.info("MultiNettyEventLoopManager shutdown completed");
        } catch (Exception e) {
            logger.warning("Failed to shutdown MultiNettyEventLoopManager: " + e.getMessage());
        }

        if (chunkLoadScheduler != null) {
            chunkLoadScheduler.shutdown();
        }

        logger.info("Phase 4: Shutting down executor manager (waiting for tasks)...");
        if (executorManager != null) {
            executorManager.shutdown();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("=== AkiAsync Shutdown Completed in " + elapsed + "ms ===");
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

    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public CacheManager getCacheManager() { return cacheManager; }
    public AsyncExecutorManager getExecutorManager() { return executorManager; }
    public AkiAsyncBridge getBridge() { return bridge; }
    public org.virgil.akiasync.throttling.EntityThrottlingManager getThrottlingManager() { return throttlingManager; }
    public ChunkLoadPriorityScheduler getChunkLoadScheduler() { return chunkLoadScheduler; }
    public org.virgil.akiasync.compat.VirtualEntityCompatManager getVirtualEntityCompatManager() { return virtualEntityCompatManager; }
    public org.virgil.akiasync.crypto.QuantumSeedManager getQuantumSeedManager() { return quantumSeedManager; }
}
