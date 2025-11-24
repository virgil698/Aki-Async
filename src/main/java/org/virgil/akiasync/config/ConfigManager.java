package org.virgil.akiasync.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.virgil.akiasync.AkiAsyncPlugin;

public class ConfigManager {
    
    private final AkiAsyncPlugin plugin;
    private FileConfiguration config;
    private boolean entityTrackerEnabled;
    private int threadPoolSize;
    private int updateIntervalTicks;
    private int maxQueueSize;
    private boolean mobSpawningEnabled;
    private boolean spawnerOptimizationEnabled;
    private int maxEntitiesPerChunk;
    private int aiCooldownTicks;
    private boolean brainThrottle;
    private int brainThrottleInterval;
    private long asyncAITimeoutMicros;
    private boolean villagerOptimizationEnabled;
    private boolean villagerUsePOISnapshot;
    private boolean piglinOptimizationEnabled;
    private boolean piglinUsePOISnapshot;
    private int piglinLookDistance;
    private int piglinBarterDistance;
    private boolean pillagerFamilyOptimizationEnabled;
    private boolean pillagerFamilyUsePOISnapshot;
    private boolean evokerOptimizationEnabled;
    private boolean blazeOptimizationEnabled;
    private boolean guardianOptimizationEnabled;
    private boolean witchOptimizationEnabled;
    private boolean universalAiOptimizationEnabled;
    private java.util.Set<String> universalAiEntities;
    private boolean zeroDelayFactoryOptimizationEnabled;
    private java.util.Set<String> zeroDelayFactoryEntities;
    private boolean blockEntityParallelTickEnabled;
    private int blockEntityParallelMinBlockEntities;
    private int blockEntityParallelBatchSize;
    private boolean blockEntityParallelProtectContainers;
    private int blockEntityParallelTimeoutMs;
    private boolean itemEntityOptimizationEnabled;
    private int itemEntityAgeInterval;
    private int itemEntityMinNearbyItems;
    private boolean simpleEntitiesOptimizationEnabled;
    private boolean simpleEntitiesUsePOISnapshot;
    private boolean entityTickParallel;
    private int entityTickThreads;
    private int minEntitiesForParallel;
    private int entityTickBatchSize;
    private boolean asyncLightingEnabled;
    private int lightingThreadPoolSize;
    private int lightBatchThreshold;
    private boolean useLayeredPropagationQueue;
    private int maxLightPropagationDistance;
    private boolean skylightCacheEnabled;
    private int skylightCacheDurationMs;
    private boolean lightDeduplicationEnabled;
    private boolean dynamicBatchAdjustmentEnabled;
    private boolean advancedLightingStatsEnabled;
    private boolean playerChunkLoadingOptimizationEnabled;
    private int maxConcurrentChunkLoadsPerPlayer;
    private boolean entityTrackingRangeOptimizationEnabled;
    private double entityTrackingRangeMultiplier;
    private boolean alternateCurrentEnabled;
    private boolean redstoneWireTurboEnabled;
    private boolean redstoneUpdateBatchingEnabled;
    private int redstoneUpdateBatchThreshold;
    private boolean redstoneCacheEnabled;
    private int redstoneCacheDurationMs;
    private boolean tntOptimizationEnabled;
    private java.util.Set<String> tntExplosionEntities;
    private int tntThreads;
    private int tntMaxBlocks;
    private long tntTimeoutMicros;
    private int tntBatchSize;
    private boolean tntVanillaCompatibilityEnabled;
    private boolean tntUseVanillaPower;
    private boolean tntUseVanillaFireLogic;
    private boolean tntUseVanillaDamageCalculation;
    private boolean beeFixEnabled;
    private boolean tntUseFullRaycast;
    private boolean tntUseVanillaBlockDestruction;
    private boolean tntUseVanillaDrops;
    private boolean asyncVillagerBreedEnabled;
    private boolean villagerAgeThrottleEnabled;
    private int villagerBreedThreads;
    private int villagerBreedCheckInterval;
    private boolean chunkTickAsyncEnabled;
    private int chunkTickThreads;
    private long chunkTickTimeoutMicros;
    private boolean enableDebugLogging;
    private boolean enablePerformanceMetrics;
    private int configVersion;
    
    private boolean structureLocationAsyncEnabled;
    private int structureLocationThreads;
    private boolean locateCommandEnabled;
    private int locateCommandSearchRadius;
    private boolean locateCommandSkipKnownStructures;
    private boolean villagerTradeMapsEnabled;
    private java.util.Set<String> villagerTradeMapTypes;
    private int villagerMapGenerationTimeoutSeconds;
    private boolean dolphinTreasureHuntEnabled;
    private int dolphinTreasureSearchRadius;
    private int dolphinTreasureHuntInterval;
    private boolean chestExplorationMapsEnabled;
    private java.util.Set<String> chestExplorationLootTables;
    private boolean chestMapPreserveProbability;
    
    private boolean structureAlgorithmOptimizationEnabled;
    private String structureSearchPattern;
    private boolean structureCachingEnabled;
    private boolean structurePrecomputationEnabled;
    private boolean biomeAwareSearchEnabled;
    private int structureCacheMaxSize;
    private long structureCacheExpirationMinutes;
    
    private boolean dataPackOptimizationEnabled;
    private int dataPackFileLoadThreads;
    private int dataPackZipProcessThreads;
    private int dataPackBatchSize;
    private long dataPackCacheExpirationMinutes;
    private boolean dataPackDebugEnabled;

    private boolean nitoriOptimizationsEnabled;
    private boolean virtualThreadEnabled;
    private boolean workStealingEnabled;
    private boolean blockPosCacheEnabled;
    private boolean optimizedCollectionsEnabled;
    
    private boolean secureSeedEnabled;
    private boolean secureSeedProtectStructures;
    private boolean secureSeedProtectOres;
    private boolean secureSeedProtectSlimes;
    private int secureSeedBits;
    
    public ConfigManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        entityTrackerEnabled = config.getBoolean("entity-tracker.enabled", true);
        threadPoolSize = config.getInt("entity-tracker.thread-pool-size", 4);
        updateIntervalTicks = config.getInt("entity-tracker.update-interval-ticks", 1);
        maxQueueSize = config.getInt("entity-tracker.max-queue-size", 1000);
        mobSpawningEnabled = config.getBoolean("mob-spawning.enabled", true);
        spawnerOptimizationEnabled = config.getBoolean("mob-spawning.spawner-optimization", true);
        maxEntitiesPerChunk = config.getInt("density.max-per-chunk", 80);
        aiCooldownTicks = config.getInt("density.ai-cooldown-ticks", 10);
        brainThrottle = config.getBoolean("brain.throttle", true);
        brainThrottleInterval = config.getInt("brain.throttle-interval", 10);
        asyncAITimeoutMicros = config.getLong("async-ai.timeout-microseconds", 500L);
        villagerOptimizationEnabled = config.getBoolean("async-ai.villager-optimization.enabled", false);
        villagerUsePOISnapshot = config.getBoolean("async-ai.villager-optimization.use-poi-snapshot", true);
        piglinOptimizationEnabled = config.getBoolean("async-ai.piglin-optimization.enabled", false);
        piglinUsePOISnapshot = config.getBoolean("async-ai.piglin-optimization.use-poi-snapshot", false);
        piglinLookDistance = config.getInt("async-ai.piglin-optimization.look-distance", 16);
        piglinBarterDistance = config.getInt("async-ai.piglin-optimization.barter-distance", 16);
        pillagerFamilyOptimizationEnabled = config.getBoolean("async-ai.pillager-family-optimization.enabled", false);
        pillagerFamilyUsePOISnapshot = config.getBoolean("async-ai.pillager-family-optimization.use-poi-snapshot", false);
        evokerOptimizationEnabled = config.getBoolean("async-ai.evoker-optimization.enabled", false);
        blazeOptimizationEnabled = config.getBoolean("async-ai.blaze-optimization.enabled", false);
        guardianOptimizationEnabled = config.getBoolean("async-ai.guardian-optimization.enabled", false);
        witchOptimizationEnabled = config.getBoolean("async-ai.witch-optimization.enabled", false);
        universalAiOptimizationEnabled = config.getBoolean("async-ai.universal-ai-optimization.enabled", false);
        universalAiEntities = new java.util.HashSet<>(config.getStringList("async-ai.universal-ai-optimization.entities"));
        zeroDelayFactoryOptimizationEnabled = config.getBoolean("block-entity-optimizations.zero-delay-factory-optimization.enabled", false);
        zeroDelayFactoryEntities = new java.util.HashSet<>(config.getStringList("block-entity-optimizations.zero-delay-factory-optimization.entities"));
        blockEntityParallelTickEnabled = config.getBoolean("block-entity-optimizations.parallel-tick.enabled", true);
        blockEntityParallelMinBlockEntities = config.getInt("block-entity-optimizations.parallel-tick.min-block-entities", 50);
        blockEntityParallelBatchSize = config.getInt("block-entity-optimizations.parallel-tick.batch-size", 16);
        blockEntityParallelProtectContainers = config.getBoolean("block-entity-optimizations.parallel-tick.protect-containers", true);
        blockEntityParallelTimeoutMs = config.getInt("block-entity-optimizations.parallel-tick.timeout-ms", 50);
        itemEntityOptimizationEnabled = config.getBoolean("item-entity-optimizations.enabled", true);
        itemEntityAgeInterval = config.getInt("item-entity-optimizations.age-increment-interval", 10);
        itemEntityMinNearbyItems = config.getInt("item-entity-optimizations.min-nearby-items", 3);
        simpleEntitiesOptimizationEnabled = config.getBoolean("async-ai.simple-entities.enabled", false);
        simpleEntitiesUsePOISnapshot = config.getBoolean("async-ai.simple-entities.use-poi-snapshot", false);
        entityTickParallel = config.getBoolean("entity-tick-parallel.enabled", true);
        entityTickThreads = config.getInt("entity-tick-parallel.threads", 4);
        minEntitiesForParallel = config.getInt("entity-tick-parallel.min-entities", 100);
        entityTickBatchSize = config.getInt("entity-tick-parallel.batch-size", 8);
        asyncLightingEnabled = config.getBoolean("lighting-optimizations.async-lighting.enabled", true);
        lightingThreadPoolSize = config.getInt("lighting-optimizations.async-lighting.thread-pool-size", 2);
        lightBatchThreshold = config.getInt("lighting-optimizations.async-lighting.batch-threshold", 16);
        useLayeredPropagationQueue = config.getBoolean("lighting-optimizations.propagation-queue.use-layered-queue", true);
        maxLightPropagationDistance = config.getInt("lighting-optimizations.propagation-queue.max-propagation-distance", 15);
        skylightCacheEnabled = config.getBoolean("lighting-optimizations.skylight-cache.enabled", true);
        skylightCacheDurationMs = config.getInt("lighting-optimizations.skylight-cache.cache-duration-ms", 100);
        lightDeduplicationEnabled = config.getBoolean("lighting-optimizations.advanced.enable-deduplication", true);
        dynamicBatchAdjustmentEnabled = config.getBoolean("lighting-optimizations.advanced.dynamic-batch-adjustment", true);
        advancedLightingStatsEnabled = config.getBoolean("lighting-optimizations.advanced.log-advanced-stats", false);
        playerChunkLoadingOptimizationEnabled = config.getBoolean("vmp-optimizations.chunk-loading.enabled", true);
        maxConcurrentChunkLoadsPerPlayer = config.getInt("vmp-optimizations.chunk-loading.max-concurrent-per-player", 5);
        entityTrackingRangeOptimizationEnabled = config.getBoolean("vmp-optimizations.entity-tracking.enabled", true);
        entityTrackingRangeMultiplier = config.getDouble("vmp-optimizations.entity-tracking.range-multiplier", 0.8);
        alternateCurrentEnabled = config.getBoolean("redstone-optimizations.alternate-current.enabled", true);
        redstoneWireTurboEnabled = config.getBoolean("redstone-optimizations.wire-turbo.enabled", true);
        redstoneUpdateBatchingEnabled = config.getBoolean("redstone-optimizations.update-batching.enabled", true);
        redstoneUpdateBatchThreshold = config.getInt("redstone-optimizations.update-batching.batch-threshold", 8);
        redstoneCacheEnabled = config.getBoolean("redstone-optimizations.cache.enabled", true);
        redstoneCacheDurationMs = config.getInt("redstone-optimizations.cache.duration-ms", 50);
        asyncVillagerBreedEnabled = config.getBoolean("villager-breed-optimization.async-villager-breed", true);
        villagerAgeThrottleEnabled = config.getBoolean("villager-breed-optimization.age-throttle", true);
        villagerBreedThreads = config.getInt("villager-breed-optimization.threads", 4);
        villagerBreedCheckInterval = config.getInt("villager-breed-optimization.check-interval", 5);
        chunkTickAsyncEnabled = config.getBoolean("chunk-tick-async.enabled", false);
        chunkTickThreads = config.getInt("chunk-tick-async.threads", 4);
        chunkTickTimeoutMicros = config.getLong("chunk-tick-async.timeout-us", 200L);
        tntOptimizationEnabled = config.getBoolean("tnt-explosion-optimization.enabled", true);
        tntExplosionEntities = new java.util.HashSet<>(config.getStringList("tnt-explosion-optimization.entities"));
        if (tntExplosionEntities.isEmpty()) {
            tntExplosionEntities.add("minecraft:tnt");
            tntExplosionEntities.add("minecraft:tnt_minecart");
            tntExplosionEntities.add("minecraft:wither_skull");
        }
        tntThreads = config.getInt("tnt-explosion-optimization.threads", 6);
        tntMaxBlocks = config.getInt("tnt-explosion-optimization.max-blocks", 4096);
        tntTimeoutMicros = config.getLong("tnt-explosion-optimization.timeout-us", 100L);
        tntBatchSize = config.getInt("tnt-explosion-optimization.batch-size", 64);
        tntVanillaCompatibilityEnabled = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.enabled", true);
        tntUseVanillaPower = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-vanilla-power", true);
        tntUseVanillaFireLogic = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-vanilla-fire-logic", true);
        tntUseVanillaDamageCalculation = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-vanilla-damage-calculation", true);
        tntUseFullRaycast = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-full-raycast", false);
        tntUseVanillaBlockDestruction = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-vanilla-block-destruction", true);
        tntUseVanillaDrops = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-vanilla-drops", true);
        beeFixEnabled = config.getBoolean("bee-fix.enabled", true);
        enableDebugLogging = config.getBoolean("performance.debug-logging", false);
        enablePerformanceMetrics = config.getBoolean("performance.enable-metrics", true);
        configVersion = config.getInt("version", 6);
        
        structureLocationAsyncEnabled = config.getBoolean("structure-location-async.enabled", true);
        structureLocationThreads = config.getInt("structure-location-async.threads", 3);
        locateCommandEnabled = config.getBoolean("structure-location-async.locate-command.enabled", true);
        locateCommandSearchRadius = config.getInt("structure-location-async.locate-command.search-radius", 100);
        locateCommandSkipKnownStructures = config.getBoolean("structure-location-async.locate-command.skip-known-structures", false);
        villagerTradeMapsEnabled = config.getBoolean("structure-location-async.villager-trade-maps.enabled", true);
        villagerTradeMapTypes = new java.util.HashSet<>(config.getStringList("structure-location-async.villager-trade-maps.trade-types"));
        if (villagerTradeMapTypes.isEmpty()) {
            villagerTradeMapTypes.add("minecraft:ocean_monument_map");
            villagerTradeMapTypes.add("minecraft:woodland_mansion_map");
            villagerTradeMapTypes.add("minecraft:buried_treasure_map");
        }
        villagerMapGenerationTimeoutSeconds = config.getInt("structure-location-async.villager-trade-maps.generation-timeout-seconds", 30);
        dolphinTreasureHuntEnabled = config.getBoolean("structure-location-async.dolphin-treasure-hunt.enabled", true);
        dolphinTreasureSearchRadius = config.getInt("structure-location-async.dolphin-treasure-hunt.search-radius", 50);
        dolphinTreasureHuntInterval = config.getInt("structure-location-async.dolphin-treasure-hunt.hunt-interval", 100);
        chestExplorationMapsEnabled = config.getBoolean("structure-location-async.chest-exploration-maps.enabled", true);
        chestExplorationLootTables = new java.util.HashSet<>(config.getStringList("structure-location-async.chest-exploration-maps.loot-tables"));
        if (chestExplorationLootTables.isEmpty()) {
            chestExplorationLootTables.add("minecraft:chests/shipwreck_map");
            chestExplorationLootTables.add("minecraft:chests/underwater_ruin_big");
            chestExplorationLootTables.add("minecraft:chests/underwater_ruin_small");
        }
        chestMapPreserveProbability = config.getBoolean("structure-location-async.chest-exploration-maps.preserve-probability", true);
        
        structureAlgorithmOptimizationEnabled = config.getBoolean("structure-location-async.algorithm-optimization.enabled", true);
        structureSearchPattern = config.getString("structure-location-async.algorithm-optimization.search-pattern", "hybrid");
        structureCachingEnabled = config.getBoolean("structure-location-async.algorithm-optimization.caching.enabled", true);
        structureCacheMaxSize = config.getInt("structure-location-async.algorithm-optimization.caching.max-size", 1000);
        structureCacheExpirationMinutes = config.getLong("structure-location-async.algorithm-optimization.caching.expiration-minutes", 30L);
        structurePrecomputationEnabled = config.getBoolean("structure-location-async.algorithm-optimization.precomputation.enabled", true);
        biomeAwareSearchEnabled = config.getBoolean("structure-location-async.algorithm-optimization.biome-aware-search.enabled", true);
        
        dataPackOptimizationEnabled = config.getBoolean("datapack-optimization.enabled", true);
        dataPackFileLoadThreads = config.getInt("datapack-optimization.file-load-threads", 4);
        dataPackZipProcessThreads = config.getInt("datapack-optimization.zip-process-threads", 2);
        dataPackBatchSize = config.getInt("datapack-optimization.batch-size", 100);
        dataPackCacheExpirationMinutes = config.getLong("datapack-optimization.cache-expiration-minutes", 30L);
        dataPackDebugEnabled = config.getBoolean("datapack-optimization.debug-enabled", false);
        
        nitoriOptimizationsEnabled = config.getBoolean("nitori.enabled", true);
        virtualThreadEnabled = config.getBoolean("nitori.virtual-threads", true);
        workStealingEnabled = config.getBoolean("nitori.work-stealing", true);
        blockPosCacheEnabled = config.getBoolean("nitori.blockpos-cache", true);
        optimizedCollectionsEnabled = config.getBoolean("nitori.optimized-collections", true);
        
        secureSeedEnabled = config.getBoolean("secure-seed.enabled", true);
        secureSeedProtectStructures = config.getBoolean("secure-seed.protect-structures", true);
        secureSeedProtectOres = config.getBoolean("secure-seed.protect-ores", true);
        secureSeedProtectSlimes = config.getBoolean("secure-seed.protect-slimes", true);
        secureSeedBits = config.getInt("secure-seed.seed-bits", 1024);
        
        validateConfigVersion();
        validateConfig();
    }
    
    private void validateConfigVersion() {
        final int CURRENT_CONFIG_VERSION = 7;
        
        if (configVersion != CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("==========================================");
            plugin.getLogger().warning("  CONFIG VERSION MISMATCH DETECTED");
            plugin.getLogger().warning("==========================================");
            plugin.getLogger().warning("Current supported version: " + CURRENT_CONFIG_VERSION);
            plugin.getLogger().warning("Your config version: " + configVersion);
            plugin.getLogger().warning("");
            
            if (configVersion < CURRENT_CONFIG_VERSION) {
                plugin.getLogger().warning("Your config.yml is outdated!");
                plugin.getLogger().warning("Automatically backing up and regenerating config...");
            } else {
                plugin.getLogger().warning("Your config.yml is from a newer version!");
                plugin.getLogger().warning("Automatically backing up and regenerating config...");
            }
            
            if (backupAndRegenerateConfig()) {
                plugin.getLogger().info("Config backup and regeneration completed successfully!");
                plugin.getLogger().info("Old config saved as: config.yml.bak");
                plugin.getLogger().info("New config generated with version " + CURRENT_CONFIG_VERSION);
                plugin.getLogger().warning("Please review the new config.yml and adjust settings as needed.");
                plugin.getLogger().warning("==========================================");
                
                reloadConfigWithoutValidation();
            } else {
                plugin.getLogger().severe("Failed to backup and regenerate config!");
                plugin.getLogger().severe("Please manually update your config.yml");
                plugin.getLogger().warning("==========================================");
            }
        }
    }
    
    private boolean backupAndRegenerateConfig() {
        try {
            java.io.File configFile = new java.io.File(plugin.getDataFolder(), "config.yml");
            java.io.File backupFile = new java.io.File(plugin.getDataFolder(), "config.yml.bak");
            
            if (!configFile.exists()) {
                plugin.getLogger().warning("Config file does not exist, creating new one...");
                plugin.saveDefaultConfig();
                return true;
            }
            
            if (backupFile.exists()) {
                if (!backupFile.delete()) {
                    plugin.getLogger().warning("Failed to delete existing backup file");
                }
            }
            
            if (!configFile.renameTo(backupFile)) {
                plugin.getLogger().severe("Failed to backup config file to config.yml.bak");
                return false;
            }
            
            plugin.getLogger().info("Config file backed up to: config.yml.bak");
            
            plugin.saveDefaultConfig();
            plugin.getLogger().info("New config.yml generated with latest version");
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error during config backup and regeneration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void reloadConfigWithoutValidation() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        entityTrackerEnabled = config.getBoolean("entity-tracker.enabled", true);
        threadPoolSize = config.getInt("entity-tracker.thread-pool-size", 4);
        updateIntervalTicks = config.getInt("entity-tracker.update-interval-ticks", 1);
        maxQueueSize = config.getInt("entity-tracker.max-queue-size", 1000);
        mobSpawningEnabled = config.getBoolean("mob-spawning.enabled", true);
        spawnerOptimizationEnabled = config.getBoolean("mob-spawning.spawner-optimization", true);
        maxEntitiesPerChunk = config.getInt("density.max-per-chunk", 80);
        aiCooldownTicks = config.getInt("density.ai-cooldown-ticks", 10);
        brainThrottle = config.getBoolean("brain.throttle", true);
        brainThrottleInterval = config.getInt("brain.throttle-interval", 10);
        asyncAITimeoutMicros = config.getLong("async-ai.timeout-microseconds", 500L);
        villagerOptimizationEnabled = config.getBoolean("async-ai.villager-optimization.enabled", false);
        villagerUsePOISnapshot = config.getBoolean("async-ai.villager-optimization.use-poi-snapshot", true);
        piglinOptimizationEnabled = config.getBoolean("async-ai.piglin-optimization.enabled", false);
        piglinUsePOISnapshot = config.getBoolean("async-ai.piglin-optimization.use-poi-snapshot", false);
        piglinLookDistance = config.getInt("async-ai.piglin-optimization.look-distance", 16);
        piglinBarterDistance = config.getInt("async-ai.piglin-optimization.barter-distance", 16);
        pillagerFamilyOptimizationEnabled = config.getBoolean("async-ai.pillager-family-optimization.enabled", false);
        pillagerFamilyUsePOISnapshot = config.getBoolean("async-ai.pillager-family-optimization.use-poi-snapshot", false);
        evokerOptimizationEnabled = config.getBoolean("async-ai.evoker-optimization.enabled", false);
        blazeOptimizationEnabled = config.getBoolean("async-ai.blaze-optimization.enabled", false);
        guardianOptimizationEnabled = config.getBoolean("async-ai.guardian-optimization.enabled", false);
        witchOptimizationEnabled = config.getBoolean("async-ai.witch-optimization.enabled", false);
        universalAiOptimizationEnabled = config.getBoolean("async-ai.universal-ai-optimization.enabled", false);
        universalAiEntities = new java.util.HashSet<>(config.getStringList("async-ai.universal-ai-optimization.entities"));
        zeroDelayFactoryOptimizationEnabled = config.getBoolean("block-entity-optimizations.zero-delay-factory-optimization.enabled", false);
        zeroDelayFactoryEntities = new java.util.HashSet<>(config.getStringList("block-entity-optimizations.zero-delay-factory-optimization.entities"));
        blockEntityParallelTickEnabled = config.getBoolean("block-entity-optimizations.parallel-tick.enabled", true);
        blockEntityParallelMinBlockEntities = config.getInt("block-entity-optimizations.parallel-tick.min-block-entities", 50);
        blockEntityParallelBatchSize = config.getInt("block-entity-optimizations.parallel-tick.batch-size", 16);
        blockEntityParallelProtectContainers = config.getBoolean("block-entity-optimizations.parallel-tick.protect-containers", true);
        blockEntityParallelTimeoutMs = config.getInt("block-entity-optimizations.parallel-tick.timeout-ms", 50);
        itemEntityOptimizationEnabled = config.getBoolean("item-entity-optimizations.enabled", true);
        itemEntityAgeInterval = config.getInt("item-entity-optimizations.age-increment-interval", 10);
        itemEntityMinNearbyItems = config.getInt("item-entity-optimizations.min-nearby-items", 3);
        simpleEntitiesOptimizationEnabled = config.getBoolean("async-ai.simple-entities.enabled", false);
        simpleEntitiesUsePOISnapshot = config.getBoolean("async-ai.simple-entities.use-poi-snapshot", false);
        entityTickParallel = config.getBoolean("entity-tick-parallel.enabled", true);
        entityTickThreads = config.getInt("entity-tick-parallel.threads", 4);
        minEntitiesForParallel = config.getInt("entity-tick-parallel.min-entities", 100);
        entityTickBatchSize = config.getInt("entity-tick-parallel.batch-size", 50);
        
        nitoriOptimizationsEnabled = config.getBoolean("nitori.enabled", true);
        virtualThreadEnabled = config.getBoolean("nitori.virtual-threads", true);
        workStealingEnabled = config.getBoolean("nitori.work-stealing", true);
        blockPosCacheEnabled = config.getBoolean("nitori.blockpos-cache", true);
        optimizedCollectionsEnabled = config.getBoolean("nitori.optimized-collections", true);
        
        asyncLightingEnabled = config.getBoolean("lighting-optimizations.async-lighting.enabled", true);
        lightingThreadPoolSize = config.getInt("lighting-optimizations.async-lighting.thread-pool-size", 2);
        lightBatchThreshold = config.getInt("lighting-optimizations.async-lighting.batch-threshold", 16);
        useLayeredPropagationQueue = config.getBoolean("lighting-optimizations.propagation-queue.use-layered-queue", true);
        maxLightPropagationDistance = config.getInt("lighting-optimizations.propagation-queue.max-propagation-distance", 15);
        skylightCacheEnabled = config.getBoolean("lighting-optimizations.skylight-cache.enabled", true);
        skylightCacheDurationMs = config.getInt("lighting-optimizations.skylight-cache.cache-duration-ms", 100);
        lightDeduplicationEnabled = config.getBoolean("lighting-optimizations.advanced.enable-deduplication", true);
        dynamicBatchAdjustmentEnabled = config.getBoolean("lighting-optimizations.advanced.dynamic-batch-adjustment", true);
        advancedLightingStatsEnabled = config.getBoolean("lighting-optimizations.advanced.log-advanced-stats", false);
        
        playerChunkLoadingOptimizationEnabled = config.getBoolean("vmp-optimizations.chunk-loading.enabled", true);
        maxConcurrentChunkLoadsPerPlayer = config.getInt("vmp-optimizations.chunk-loading.max-concurrent-per-player", 5);
        entityTrackingRangeOptimizationEnabled = config.getBoolean("vmp-optimizations.entity-tracking.enabled", true);
        entityTrackingRangeMultiplier = config.getDouble("vmp-optimizations.entity-tracking.range-multiplier", 0.8);
        
        alternateCurrentEnabled = config.getBoolean("redstone-optimizations.alternate-current.enabled", true);
        redstoneWireTurboEnabled = config.getBoolean("redstone-optimizations.wire-turbo.enabled", true);
        redstoneUpdateBatchingEnabled = config.getBoolean("redstone-optimizations.update-batching.enabled", true);
        redstoneUpdateBatchThreshold = config.getInt("redstone-optimizations.update-batching.batch-threshold", 8);
        redstoneCacheEnabled = config.getBoolean("redstone-optimizations.cache.enabled", true);
        redstoneCacheDurationMs = config.getInt("redstone-optimizations.cache.duration-ms", 50);
        
        tntOptimizationEnabled = config.getBoolean("tnt-explosion-optimization.enabled", true);
        tntExplosionEntities = new java.util.HashSet<>(config.getStringList("tnt-explosion-optimization.entities"));
        if (tntExplosionEntities.isEmpty()) {
            tntExplosionEntities.add("minecraft:tnt");
            tntExplosionEntities.add("minecraft:tnt_minecart");
            tntExplosionEntities.add("minecraft:wither_skull");
        }
        tntThreads = config.getInt("tnt-explosion-optimization.threads", 6);
        tntMaxBlocks = config.getInt("tnt-explosion-optimization.max-blocks", 4096);
        tntTimeoutMicros = config.getLong("tnt-explosion-optimization.timeout-us", 100L);
        tntBatchSize = config.getInt("tnt-explosion-optimization.batch-size", 64);
        tntVanillaCompatibilityEnabled = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.enabled", true);
        tntUseVanillaPower = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-vanilla-power", true);
        tntUseVanillaFireLogic = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-vanilla-fire-logic", true);
        tntUseVanillaDamageCalculation = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-vanilla-damage-calculation", true);
        tntUseFullRaycast = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-full-raycast", false);
        tntUseVanillaBlockDestruction = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-vanilla-block-destruction", true);
        tntUseVanillaDrops = config.getBoolean("tnt-explosion-optimization.vanilla-compatibility.use-vanilla-drops", true);
        
        beeFixEnabled = config.getBoolean("bee-fix.enabled", true);
        
        asyncVillagerBreedEnabled = config.getBoolean("villager-breed-optimization.async-villager-breed", true);
        villagerAgeThrottleEnabled = config.getBoolean("villager-breed-optimization.age-throttle", true);
        villagerBreedThreads = config.getInt("villager-breed-optimization.threads", 4);
        villagerBreedCheckInterval = config.getInt("villager-breed-optimization.check-interval", 5);
        
        chunkTickAsyncEnabled = config.getBoolean("chunk-tick-async.enabled", false);
        chunkTickThreads = config.getInt("chunk-tick-async.threads", 4);
        chunkTickTimeoutMicros = config.getLong("chunk-tick-async.timeout-us", 200L);
        
        structureLocationAsyncEnabled = config.getBoolean("structure-location-async.enabled", true);
        structureLocationThreads = config.getInt("structure-location-async.threads", 3);
        locateCommandEnabled = config.getBoolean("structure-location-async.locate-command.enabled", true);
        locateCommandSearchRadius = config.getInt("structure-location-async.locate-command.search-radius", 100);
        locateCommandSkipKnownStructures = config.getBoolean("structure-location-async.locate-command.skip-known-structures", false);
        villagerTradeMapsEnabled = config.getBoolean("structure-location-async.villager-trade-maps.enabled", true);
        villagerTradeMapTypes = new java.util.HashSet<>(config.getStringList("structure-location-async.villager-trade-maps.trade-types"));
        if (villagerTradeMapTypes.isEmpty()) {
            villagerTradeMapTypes.add("minecraft:ocean_monument_map");
            villagerTradeMapTypes.add("minecraft:woodland_mansion_map");
            villagerTradeMapTypes.add("minecraft:buried_treasure_map");
        }
        villagerMapGenerationTimeoutSeconds = config.getInt("structure-location-async.villager-trade-maps.generation-timeout-seconds", 30);
        dolphinTreasureHuntEnabled = config.getBoolean("structure-location-async.dolphin-treasure-hunt.enabled", true);
        dolphinTreasureSearchRadius = config.getInt("structure-location-async.dolphin-treasure-hunt.search-radius", 50);
        dolphinTreasureHuntInterval = config.getInt("structure-location-async.dolphin-treasure-hunt.hunt-interval", 100);
        chestExplorationMapsEnabled = config.getBoolean("structure-location-async.chest-exploration-maps.enabled", true);
        chestExplorationLootTables = new java.util.HashSet<>(config.getStringList("structure-location-async.chest-exploration-maps.loot-tables"));
        if (chestExplorationLootTables.isEmpty()) {
            chestExplorationLootTables.add("minecraft:chests/shipwreck_map");
            chestExplorationLootTables.add("minecraft:chests/underwater_ruin_big");
            chestExplorationLootTables.add("minecraft:chests/underwater_ruin_small");
        }
        chestMapPreserveProbability = config.getBoolean("structure-location-async.chest-exploration-maps.preserve-probability", true);
        
        structureAlgorithmOptimizationEnabled = config.getBoolean("structure-location-async.algorithm-optimization.enabled", true);
        structureSearchPattern = config.getString("structure-location-async.algorithm-optimization.search-pattern", "hybrid");
        structureCachingEnabled = config.getBoolean("structure-location-async.algorithm-optimization.caching.enabled", true);
        structureCacheMaxSize = config.getInt("structure-location-async.algorithm-optimization.caching.max-size", 1000);
        structureCacheExpirationMinutes = config.getLong("structure-location-async.algorithm-optimization.caching.expiration-minutes", 30L);
        structurePrecomputationEnabled = config.getBoolean("structure-location-async.algorithm-optimization.precomputation.enabled", true);
        biomeAwareSearchEnabled = config.getBoolean("structure-location-async.algorithm-optimization.biome-aware-search.enabled", true);
        
        dataPackOptimizationEnabled = config.getBoolean("datapack-optimization.enabled", true);
        dataPackFileLoadThreads = config.getInt("datapack-optimization.file-load-threads", 4);
        dataPackZipProcessThreads = config.getInt("datapack-optimization.zip-process-threads", 2);
        dataPackBatchSize = config.getInt("datapack-optimization.batch-size", 100);
        dataPackCacheExpirationMinutes = config.getLong("datapack-optimization.cache-expiration-minutes", 30L);
        dataPackDebugEnabled = config.getBoolean("datapack-optimization.debug-enabled", false);
        
        enableDebugLogging = config.getBoolean("performance.debug-logging", false);
        enablePerformanceMetrics = config.getBoolean("performance.enable-metrics", true);
        configVersion = config.getInt("version", 6);
        
        validateConfig();
    }
    
    private void validateConfig() {
        if (threadPoolSize < 1) {
            plugin.getLogger().warning("Thread pool size cannot be less than 1, setting to 1");
            threadPoolSize = 1;
        }
        if (threadPoolSize > 32) {
            plugin.getLogger().warning("Thread pool size cannot be more than 32, setting to 32");
            threadPoolSize = 32;
        }
        
        if (updateIntervalTicks < 1) {
            plugin.getLogger().warning("Update interval cannot be less than 1 tick, setting to 1");
            updateIntervalTicks = 1;
        }
        
        if (maxQueueSize < 100) {
            plugin.getLogger().warning("Max queue size cannot be less than 100, setting to 100");
            maxQueueSize = 100;
        }

        if (maxEntitiesPerChunk < 20) {
            maxEntitiesPerChunk = 20;
        }
        if (aiCooldownTicks < 0) {
            aiCooldownTicks = 0;
        }
        if (brainThrottleInterval < 0) {
            brainThrottleInterval = 0;
        }
        if (asyncAITimeoutMicros < 100) {
            plugin.getLogger().warning("Async AI timeout too low, setting to 100娓璼");
            asyncAITimeoutMicros = 100;
        }
        if (asyncAITimeoutMicros > 5000) {
            plugin.getLogger().warning("Async AI timeout too high, setting to 5000娓璼 (5ms)");
            asyncAITimeoutMicros = 5000;
        }
        if (entityTickThreads < 1) entityTickThreads = 1;
        if (entityTickThreads > 16) entityTickThreads = 16;
        if (minEntitiesForParallel < 10) minEntitiesForParallel = 10;
        if (lightingThreadPoolSize < 1) lightingThreadPoolSize = 1;
        if (lightingThreadPoolSize > 8) lightingThreadPoolSize = 8;
        if (lightBatchThreshold < 1) lightBatchThreshold = 1;
        if (lightBatchThreshold > 100) lightBatchThreshold = 100;
        if (maxLightPropagationDistance < 1) maxLightPropagationDistance = 1;
        if (maxLightPropagationDistance > 32) maxLightPropagationDistance = 32;
        if (skylightCacheDurationMs < 0) skylightCacheDurationMs = 0;
        if (maxConcurrentChunkLoadsPerPlayer < 1) maxConcurrentChunkLoadsPerPlayer = 1;
        if (maxConcurrentChunkLoadsPerPlayer > 20) maxConcurrentChunkLoadsPerPlayer = 20;
        if (entityTrackingRangeMultiplier < 0.1) entityTrackingRangeMultiplier = 0.1;
        if (entityTrackingRangeMultiplier > 2.0) entityTrackingRangeMultiplier = 2.0;
        if (redstoneUpdateBatchThreshold < 1) redstoneUpdateBatchThreshold = 1;
        if (redstoneUpdateBatchThreshold > 50) redstoneUpdateBatchThreshold = 50;
        if (redstoneCacheDurationMs < 0) redstoneCacheDurationMs = 0;
        if (redstoneCacheDurationMs > 1000) redstoneCacheDurationMs = 1000;
        if (tntThreads < 1) {
            plugin.getLogger().warning("TNT threads cannot be less than 1, setting to 1");
            tntThreads = 1;
        }
        if (tntThreads > 32) {
            plugin.getLogger().warning("TNT threads cannot be more than 32, setting to 32");
            tntThreads = 32;
        }
        if (tntMaxBlocks < 256) tntMaxBlocks = 256;
        if (tntMaxBlocks > 16384) tntMaxBlocks = 16384;
        if (tntTimeoutMicros < 10) tntTimeoutMicros = 10;
        if (tntTimeoutMicros > 10000) tntTimeoutMicros = 10000;
        if (tntBatchSize < 8) tntBatchSize = 8;
        if (tntBatchSize > 256) tntBatchSize = 256;
        
        if (structureLocationThreads < 1) {
            plugin.getLogger().warning("Structure location threads cannot be less than 1, setting to 1");
            structureLocationThreads = 1;
        }
        if (structureLocationThreads > 8) {
            plugin.getLogger().warning("Structure location threads cannot be more than 8, setting to 8");
            structureLocationThreads = 8;
        }
        
        validateNitoriConfig();
        if (locateCommandSearchRadius < 10) locateCommandSearchRadius = 10;
        if (locateCommandSearchRadius > 1000) locateCommandSearchRadius = 1000;
        if (villagerMapGenerationTimeoutSeconds < 5) villagerMapGenerationTimeoutSeconds = 5;
        if (villagerMapGenerationTimeoutSeconds > 300) villagerMapGenerationTimeoutSeconds = 300;
        if (dolphinTreasureSearchRadius < 10) dolphinTreasureSearchRadius = 10;
        if (dolphinTreasureSearchRadius > 200) dolphinTreasureSearchRadius = 200;
        if (dolphinTreasureHuntInterval < 20) dolphinTreasureHuntInterval = 20;
        if (dolphinTreasureHuntInterval > 1200) dolphinTreasureHuntInterval = 1200;
    }
    
    private void validateNitoriConfig() {
        if (virtualThreadEnabled) {
            int javaVersion = getJavaMajorVersion();
            if (javaVersion < 19) {
                plugin.getLogger().warning("==========================================");
                plugin.getLogger().warning("  NITORI VIRTUAL THREAD WARNING");
                plugin.getLogger().warning("==========================================");
                plugin.getLogger().warning("Virtual Thread is enabled but your Java version (" + javaVersion + ") doesn't support it.");
                plugin.getLogger().warning("Virtual Thread requires Java 19+ (preview) or Java 21+ (stable).");
                plugin.getLogger().warning("The plugin will automatically fall back to regular threads.");
                plugin.getLogger().warning("Consider upgrading to Java 21+ for better performance.");
                plugin.getLogger().warning("==========================================");
            } else if (javaVersion >= 19 && javaVersion < 21) {
                plugin.getLogger().info("Virtual Thread enabled with Java " + javaVersion + " (preview feature)");
            } else {
                plugin.getLogger().info("Virtual Thread enabled with Java " + javaVersion + " (stable feature)");
            }
        }
        
        if (!nitoriOptimizationsEnabled) {
            plugin.getLogger().info("Nitori-style optimizations are disabled. You may miss some performance improvements.");
        } else {
            int enabledOptimizations = 0;
            if (virtualThreadEnabled) enabledOptimizations++;
            if (workStealingEnabled) enabledOptimizations++;
            if (blockPosCacheEnabled) enabledOptimizations++;
            if (optimizedCollectionsEnabled) enabledOptimizations++;
            
            plugin.getLogger().info("Nitori-style optimizations enabled: " + enabledOptimizations + "/4 features active");
        }
    }
    
    private int getJavaMajorVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            return version.charAt(2) - '0';
        }
        int dotIndex = version.indexOf(".");
        return Integer.parseInt(dotIndex == -1 ? version : version.substring(0, dotIndex));
    }
    
    public void reload() {
        loadConfig();
        plugin.getLogger().info("Configuration reloaded successfully!");
    }
    
    public boolean isEntityTrackerEnabled() {
        return entityTrackerEnabled;
    }
    
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    public int getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }
    
    public int getMaxQueueSize() {
        return maxQueueSize;
    }
    
    public boolean isDebugLoggingEnabled() {
        return enableDebugLogging;
    }
    
    public void setDebugLoggingEnabled(boolean enabled) {
        this.enableDebugLogging = enabled;
        config.set("performance.debug-logging", enabled);
        plugin.saveConfig();
    }
    
    public boolean isPerformanceMetricsEnabled() {
        return enablePerformanceMetrics;
    }
    
    public boolean isMobSpawningEnabled() {
        return mobSpawningEnabled;
    }
    
    public boolean isSpawnerOptimizationEnabled() {
        return spawnerOptimizationEnabled;
    }
    
    public int getMaxEntitiesPerChunk() {
        return maxEntitiesPerChunk;
    }
    
    public int getAiCooldownTicks() {
        return aiCooldownTicks;
    }
    
    public boolean isBrainThrottleEnabled() { return brainThrottle; }
    public int getBrainThrottleInterval() { return brainThrottleInterval; }
    public long getAsyncAITimeoutMicros() { return asyncAITimeoutMicros; }
    public boolean isVillagerOptimizationEnabled() { return villagerOptimizationEnabled; }
    public boolean isVillagerUsePOISnapshot() { return villagerUsePOISnapshot; }
    public boolean isPiglinOptimizationEnabled() { return piglinOptimizationEnabled; }
    public boolean isPiglinUsePOISnapshot() { return piglinUsePOISnapshot; }
    public int getPiglinLookDistance() { return piglinLookDistance; }
    public int getPiglinBarterDistance() { return piglinBarterDistance; }
    public boolean isPillagerFamilyOptimizationEnabled() { return pillagerFamilyOptimizationEnabled; }
    public boolean isPillagerFamilyUsePOISnapshot() { return pillagerFamilyUsePOISnapshot; }
    public boolean isEvokerOptimizationEnabled() { return evokerOptimizationEnabled; }
    public boolean isBlazeOptimizationEnabled() { return blazeOptimizationEnabled; }
    public boolean isGuardianOptimizationEnabled() { return guardianOptimizationEnabled; }
    public boolean isWitchOptimizationEnabled() { return witchOptimizationEnabled; }
    public boolean isUniversalAiOptimizationEnabled() { return universalAiOptimizationEnabled; }
    public java.util.Set<String> getUniversalAiEntities() { return universalAiEntities; }
    public boolean isZeroDelayFactoryOptimizationEnabled() { return zeroDelayFactoryOptimizationEnabled; }
    public java.util.Set<String> getZeroDelayFactoryEntities() { return zeroDelayFactoryEntities; }
    public boolean isBlockEntityParallelTickEnabled() { return blockEntityParallelTickEnabled; }
    public int getBlockEntityParallelMinBlockEntities() { return blockEntityParallelMinBlockEntities; }
    public int getBlockEntityParallelBatchSize() { return blockEntityParallelBatchSize; }
    public boolean isBlockEntityParallelProtectContainers() { return blockEntityParallelProtectContainers; }
    public int getBlockEntityParallelTimeoutMs() { return blockEntityParallelTimeoutMs; }
    public boolean isItemEntityOptimizationEnabled() { return itemEntityOptimizationEnabled; }
    public int getItemEntityAgeInterval() { return itemEntityAgeInterval; }
    public int getItemEntityMinNearbyItems() { return itemEntityMinNearbyItems; }
    public boolean isSimpleEntitiesOptimizationEnabled() { return simpleEntitiesOptimizationEnabled; }
    public boolean isSimpleEntitiesUsePOISnapshot() { return simpleEntitiesUsePOISnapshot; }
    public boolean isEntityTickParallel() { return entityTickParallel; }
    public int getEntityTickThreads() { return entityTickThreads; }
    public int getMinEntitiesForParallel() { return minEntitiesForParallel; }
    public int getEntityTickBatchSize() { return entityTickBatchSize; }
    public boolean isAsyncLightingEnabled() { return asyncLightingEnabled; }
    public int getLightingThreadPoolSize() { return lightingThreadPoolSize; }
    public int getLightBatchThreshold() { return lightBatchThreshold; }
    public boolean useLayeredPropagationQueue() { return useLayeredPropagationQueue; }
    public int getMaxLightPropagationDistance() { return maxLightPropagationDistance; }
    public boolean isSkylightCacheEnabled() { return skylightCacheEnabled; }
    public int getSkylightCacheDurationMs() { return skylightCacheDurationMs; }
    public boolean isLightDeduplicationEnabled() { return lightDeduplicationEnabled; }
    public boolean isDynamicBatchAdjustmentEnabled() { return dynamicBatchAdjustmentEnabled; }
    public boolean isAdvancedLightingStatsEnabled() { return advancedLightingStatsEnabled; }
    public boolean isPlayerChunkLoadingOptimizationEnabled() { return playerChunkLoadingOptimizationEnabled; }
    public int getMaxConcurrentChunkLoadsPerPlayer() { return maxConcurrentChunkLoadsPerPlayer; }
    public boolean isEntityTrackingRangeOptimizationEnabled() { return entityTrackingRangeOptimizationEnabled; }
    public double getEntityTrackingRangeMultiplier() { return entityTrackingRangeMultiplier; }
    public boolean isAlternateCurrentEnabled() { return alternateCurrentEnabled; }
    public boolean isRedstoneWireTurboEnabled() { return redstoneWireTurboEnabled; }
    public boolean isRedstoneUpdateBatchingEnabled() { return redstoneUpdateBatchingEnabled; }
    public int getRedstoneUpdateBatchThreshold() { return redstoneUpdateBatchThreshold; }
    public boolean isRedstoneCacheEnabled() { return redstoneCacheEnabled; }
    public int getRedstoneCacheDurationMs() { return redstoneCacheDurationMs; }
    public boolean isAsyncVillagerBreedEnabled() { return asyncVillagerBreedEnabled; }
    public boolean isVillagerAgeThrottleEnabled() { return villagerAgeThrottleEnabled; }
    public int getVillagerBreedThreads() { return villagerBreedThreads; }
    public int getVillagerBreedCheckInterval() { return villagerBreedCheckInterval; }
    public boolean isTNTOptimizationEnabled() { return tntOptimizationEnabled; }
    public java.util.Set<String> getTNTExplosionEntities() { return tntExplosionEntities; }
    public int getTNTThreads() { return tntThreads; }
    public int getTNTMaxBlocks() { return tntMaxBlocks; }
    public long getTNTTimeoutMicros() { return tntTimeoutMicros; }
    public int getTNTBatchSize() { return tntBatchSize; }
    public boolean isTNTDebugEnabled() { return enableDebugLogging; }
    public boolean isTNTVanillaCompatibilityEnabled() { return tntVanillaCompatibilityEnabled; }
    public boolean isTNTUseVanillaPower() { return tntUseVanillaPower; }
    public boolean isTNTUseVanillaFireLogic() { return tntUseVanillaFireLogic; }
    public boolean isTNTUseVanillaDamageCalculation() { return tntUseVanillaDamageCalculation; }
    public boolean isTNTUseFullRaycast() { return tntUseFullRaycast; }
    public boolean isBeeFixEnabled() { return beeFixEnabled; }
    public boolean isTNTUseVanillaBlockDestruction() { return tntUseVanillaBlockDestruction; }
    public boolean isTNTUseVanillaDrops() { return tntUseVanillaDrops; }
    public boolean isChunkTickAsyncEnabled() { return chunkTickAsyncEnabled; }
    public int getChunkTickThreads() { return chunkTickThreads; }
    public long getChunkTickTimeoutMicros() { return chunkTickTimeoutMicros; }
    
    public int getConfigVersion() {
        return configVersion;
    }
    
    public boolean isStructureLocationAsyncEnabled() { return structureLocationAsyncEnabled; }
    public int getStructureLocationThreads() { return structureLocationThreads; }
    public boolean isLocateCommandEnabled() { return locateCommandEnabled; }
    public int getLocateCommandSearchRadius() { return locateCommandSearchRadius; }
    public boolean isLocateCommandSkipKnownStructures() { return locateCommandSkipKnownStructures; }
    public boolean isVillagerTradeMapsEnabled() { return villagerTradeMapsEnabled; }
    public java.util.Set<String> getVillagerTradeMapTypes() { return villagerTradeMapTypes; }
    public int getVillagerMapGenerationTimeoutSeconds() { return villagerMapGenerationTimeoutSeconds; }
    public boolean isDolphinTreasureHuntEnabled() { return dolphinTreasureHuntEnabled; }
    public int getDolphinTreasureSearchRadius() { return dolphinTreasureSearchRadius; }
    public int getDolphinTreasureHuntInterval() { return dolphinTreasureHuntInterval; }
    public boolean isChestExplorationMapsEnabled() { return chestExplorationMapsEnabled; }
    public java.util.Set<String> getChestExplorationLootTables() { return chestExplorationLootTables; }
    public boolean isChestMapPreserveProbability() { return chestMapPreserveProbability; }
    public boolean isStructureLocationDebugEnabled() { return enableDebugLogging; }
    
    public boolean isStructureAlgorithmOptimizationEnabled() { return structureAlgorithmOptimizationEnabled; }
    public String getStructureSearchPattern() { return structureSearchPattern; }
    public boolean isStructureCachingEnabled() { return structureCachingEnabled; }
    public boolean isStructurePrecomputationEnabled() { return structurePrecomputationEnabled; }
    public boolean isBiomeAwareSearchEnabled() { return biomeAwareSearchEnabled; }
    public int getStructureCacheMaxSize() { return structureCacheMaxSize; }
    public long getStructureCacheExpirationMinutes() { return structureCacheExpirationMinutes; }
    
    public boolean isDataPackOptimizationEnabled() { return dataPackOptimizationEnabled; }
    public int getDataPackFileLoadThreads() { return dataPackFileLoadThreads; }
    public int getDataPackZipProcessThreads() { return dataPackZipProcessThreads; }
    public int getDataPackBatchSize() { return dataPackBatchSize; }
    public long getDataPackCacheExpirationMinutes() { return dataPackCacheExpirationMinutes; }
    public boolean isDataPackDebugEnabled() { return dataPackDebugEnabled; }
    
    public boolean isNitoriOptimizationsEnabled() { return nitoriOptimizationsEnabled; }
    public boolean isVirtualThreadEnabled() { return virtualThreadEnabled; }
    public boolean isWorkStealingEnabled() { return workStealingEnabled; }
    public boolean isBlockPosCacheEnabled() { return blockPosCacheEnabled; }
    public boolean isOptimizedCollectionsEnabled() { return optimizedCollectionsEnabled; }
    
    public boolean isSecureSeedEnabled() { return secureSeedEnabled; }
    public boolean isSecureSeedProtectStructures() { return secureSeedProtectStructures; }
    public boolean isSecureSeedProtectOres() { return secureSeedProtectOres; }
    public boolean isSecureSeedProtectSlimes() { return secureSeedProtectSlimes; }
    public int getSecureSeedBits() { return secureSeedBits; }
    public boolean isSecureSeedDebugLogging() { return enableDebugLogging; }
    
    public boolean getBoolean(String path, boolean defaultValue) {
        return config != null ? config.getBoolean(path, defaultValue) : defaultValue;
    }
}