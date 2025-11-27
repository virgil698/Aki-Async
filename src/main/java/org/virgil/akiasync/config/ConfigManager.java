package org.virgil.akiasync.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.virgil.akiasync.AkiAsyncPlugin;

public class ConfigManager {

    private static final int CURRENT_CONFIG_VERSION = 12;

    private final AkiAsyncPlugin plugin;
    private FileConfiguration config;
    private boolean entityTrackerEnabled;
    private int threadPoolSize;
    private int updateIntervalTicks;
    private int maxQueueSize;
    private boolean mobSpawningEnabled;
    private boolean spawnerOptimizationEnabled;
    private boolean densityControlEnabled;
    private int maxEntitiesPerChunk;
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
    private String universalAiEntitiesConfigFile;
    private java.util.Set<String> universalAiEntities;
    private boolean dabEnabled;
    private int dabStartDistance;
    private int dabActivationDistMod;
    private int dabMaxTickInterval;
    private boolean asyncPathfindingEnabled;
    private int asyncPathfindingMaxThreads;
    private int asyncPathfindingKeepAliveSeconds;
    private int asyncPathfindingMaxQueueSize;
    private int asyncPathfindingTimeoutMs;
    private boolean entityThrottlingEnabled;
    private String entityThrottlingConfigFile;
    private int entityThrottlingCheckInterval;
    private int entityThrottlingThrottleInterval;
    private int entityThrottlingRemovalBatchSize;
    private boolean zeroDelayFactoryOptimizationEnabled;
    private String zeroDelayFactoryEntitiesConfigFile;
    private java.util.Set<String> zeroDelayFactoryEntities;
    private boolean blockEntityParallelTickEnabled;
    private int blockEntityParallelMinBlockEntities;
    private int blockEntityParallelBatchSize;
    private boolean blockEntityParallelProtectContainers;
    private int blockEntityParallelTimeoutMs;
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
    private boolean tntLandProtectionEnabled;
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

    private boolean furnaceRecipeCacheEnabled;
    private int furnaceRecipeCacheSize;
    private boolean furnaceCacheApplyToBlastFurnace;
    private boolean furnaceCacheApplyToSmoker;
    private boolean furnaceFixBurnTimeBug;

    private boolean craftingRecipeCacheEnabled;
    private int craftingRecipeCacheSize;
    private boolean craftingOptimizeBatchCrafting;
    private boolean craftingReduceNetworkTraffic;

    private boolean minecartCauldronDestructionEnabled;

    private boolean fallingBlockParallelEnabled;
    private int minFallingBlocksForParallel;
    private int fallingBlockBatchSize;

    private boolean networkOptimizationEnabled;
    private boolean packetPriorityEnabled;
    private boolean chunkRateControlEnabled;
    private boolean congestionDetectionEnabled;
    private int highPingThreshold;
    private int criticalPingThreshold;
    private long highBandwidthThreshold;
    private int baseChunkSendRate;
    private int maxChunkSendRate;
    private int minChunkSendRate;

    private int packetSendRateBase;
    private int packetSendRateMedium;
    private int packetSendRateHeavy;
    private int packetSendRateExtreme;

    private int queueLimitMaxTotal;
    private int queueLimitMaxCritical;
    private int queueLimitMaxHigh;
    private int queueLimitMaxNormal;

    private int accelerationThresholdMedium;
    private int accelerationThresholdHeavy;
    private int accelerationThresholdExtreme;

    private boolean cleanupEnabled;
    private int cleanupStaleThreshold;
    private int cleanupCriticalCleanup;
    private int cleanupNormalCleanup;

    private boolean itemEntityParallelEnabled;
    private int minItemEntitiesForParallel;
    private int itemEntityBatchSize;
    private boolean itemEntityMergeOptimizationEnabled;
    private int itemEntityMergeInterval;
    private int itemEntityMinNearbyItems;
    private double itemEntityMergeRange;
    private boolean itemEntityAgeOptimizationEnabled;
    private int itemEntityAgeInterval;
    private double itemEntityPlayerDetectionRange;

    private boolean fastMovementChunkLoadEnabled;
    private double fastMovementSpeedThreshold;
    private int fastMovementPreloadDistance;
    private int fastMovementMaxConcurrentLoads;
    private int fastMovementPredictionTicks;

    private boolean centerOffsetEnabled;
    private double minOffsetSpeed;
    private double maxOffsetSpeed;
    private double maxOffsetRatio;
    private int asyncLoadingBatchSize;
    private long asyncLoadingBatchDelayMs;

    public ConfigManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    private java.util.Set<String> loadEntitiesFromFile(String fileName, String key) {
        try {
            java.io.File entitiesFile = new java.io.File(plugin.getDataFolder(), fileName);

            if (!entitiesFile.exists()) {
                plugin.saveResource(fileName, false);
            }

            org.bukkit.configuration.file.FileConfiguration entitiesConfig =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(entitiesFile);

            java.util.List<String> entityList = entitiesConfig.getStringList(key);
            if (entityList.isEmpty()) {
                plugin.getLogger().warning("[Config] No entities found in " + fileName + " under key: " + key);
                return new java.util.HashSet<>();
            }

            plugin.getLogger().info("[Config] Loaded " + entityList.size() + " entities from " + fileName + " (" + key + ")");
            return new java.util.HashSet<>(entityList);

        } catch (Exception e) {
            plugin.getLogger().severe("[Config] Failed to load entities from " + fileName + ": " + e.getMessage());
            return new java.util.HashSet<>();
        }
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
        densityControlEnabled = config.getBoolean("density.enabled", true);
        maxEntitiesPerChunk = config.getInt("density.max-per-chunk", 80);
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
        universalAiEntitiesConfigFile = config.getString("async-ai.universal-ai-optimization.entities-config-file", "entities.yml");
        universalAiEntities = loadEntitiesFromFile(universalAiEntitiesConfigFile, "universal-ai-entities");
        dabEnabled = config.getBoolean("async-ai.universal-ai-optimization.dynamic-activation.enabled", true);
        dabStartDistance = config.getInt("async-ai.universal-ai-optimization.dynamic-activation.start-distance", 12);
        dabActivationDistMod = config.getInt("async-ai.universal-ai-optimization.dynamic-activation.activation-dist-mod", 8);
        dabMaxTickInterval = config.getInt("async-ai.universal-ai-optimization.dynamic-activation.max-tick-interval", 20);
        asyncPathfindingEnabled = config.getBoolean("async-ai.async-pathfinding.enabled", true);
        asyncPathfindingMaxThreads = config.getInt("async-ai.async-pathfinding.max-threads", 8);
        asyncPathfindingKeepAliveSeconds = config.getInt("async-ai.async-pathfinding.keep-alive-seconds", 60);
        asyncPathfindingMaxQueueSize = config.getInt("async-ai.async-pathfinding.max-queue-size", 500);
        asyncPathfindingTimeoutMs = config.getInt("async-ai.async-pathfinding.timeout-ms", 50);
        entityThrottlingEnabled = config.getBoolean("entity-throttling.enabled", true);
        entityThrottlingConfigFile = config.getString("entity-throttling.config-file", "throttling.yml");
        entityThrottlingCheckInterval = config.getInt("entity-throttling.check-interval", 100);
        entityThrottlingThrottleInterval = config.getInt("entity-throttling.throttle-interval", 3);
        entityThrottlingRemovalBatchSize = config.getInt("entity-throttling.removal-batch-size", 10);
        zeroDelayFactoryOptimizationEnabled = config.getBoolean("block-entity-optimizations.zero-delay-factory-optimization.enabled", false);
        zeroDelayFactoryEntitiesConfigFile = config.getString("block-entity-optimizations.zero-delay-factory-optimization.entities-config-file", "entities.yml");
        zeroDelayFactoryEntities = loadEntitiesFromFile(zeroDelayFactoryEntitiesConfigFile, "zero-delay-factory-entities");
        blockEntityParallelTickEnabled = config.getBoolean("block-entity-optimizations.parallel-tick.enabled", true);
        blockEntityParallelMinBlockEntities = config.getInt("block-entity-optimizations.parallel-tick.min-block-entities", 50);
        blockEntityParallelBatchSize = config.getInt("block-entity-optimizations.parallel-tick.batch-size", 16);
        blockEntityParallelProtectContainers = config.getBoolean("block-entity-optimizations.parallel-tick.protect-containers", true);
        blockEntityParallelTimeoutMs = config.getInt("block-entity-optimizations.parallel-tick.timeout-ms", 50);
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
        tntLandProtectionEnabled = config.getBoolean("tnt-explosion-optimization.land-protection.enabled", true);
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

        furnaceRecipeCacheEnabled = config.getBoolean("recipe-optimization.furnace-recipe-cache.enabled", true);
        furnaceRecipeCacheSize = config.getInt("recipe-optimization.furnace-recipe-cache.cache-size", 100);
        furnaceCacheApplyToBlastFurnace = config.getBoolean("recipe-optimization.furnace-recipe-cache.apply-to-blast-furnace", true);
        furnaceCacheApplyToSmoker = config.getBoolean("recipe-optimization.furnace-recipe-cache.apply-to-smoker", true);
        furnaceFixBurnTimeBug = config.getBoolean("recipe-optimization.furnace-recipe-cache.fix-burn-time-bug", true);

        craftingRecipeCacheEnabled = config.getBoolean("recipe-optimization.crafting-recipe-cache.enabled", false);
        craftingRecipeCacheSize = config.getInt("recipe-optimization.crafting-recipe-cache.cache-size", 200);
        craftingOptimizeBatchCrafting = config.getBoolean("recipe-optimization.crafting-recipe-cache.optimize-batch-crafting", true);
        craftingReduceNetworkTraffic = config.getBoolean("recipe-optimization.crafting-recipe-cache.reduce-network-traffic", true);

        minecartCauldronDestructionEnabled = config.getBoolean("servercore-optimizations.minecart-cauldron-destruction.enabled", true);

        fallingBlockParallelEnabled = config.getBoolean("falling-block-optimization.enabled", true);
        minFallingBlocksForParallel = config.getInt("falling-block-optimization.min-falling-blocks", 20);
        fallingBlockBatchSize = config.getInt("falling-block-optimization.batch-size", 10);

        itemEntityParallelEnabled = config.getBoolean("item-entity-optimizations.parallel-processing.enabled", true);
        minItemEntitiesForParallel = config.getInt("item-entity-optimizations.parallel-processing.min-item-entities", 50);
        itemEntityBatchSize = config.getInt("item-entity-optimizations.parallel-processing.batch-size", 20);
        itemEntityMergeOptimizationEnabled = config.getBoolean("item-entity-optimizations.smart-merge.enabled", true);
        itemEntityMergeInterval = config.getInt("item-entity-optimizations.smart-merge.merge-interval", 5);
        itemEntityMinNearbyItems = config.getInt("item-entity-optimizations.smart-merge.min-nearby-items", 3);
        itemEntityMergeRange = config.getDouble("item-entity-optimizations.smart-merge.merge-range", 1.5);
        itemEntityAgeOptimizationEnabled = config.getBoolean("item-entity-optimizations.age-optimization.enabled", true);
        itemEntityAgeInterval = config.getInt("item-entity-optimizations.age-optimization.age-interval", 10);
        itemEntityPlayerDetectionRange = config.getDouble("item-entity-optimizations.age-optimization.player-detection-range", 8.0);

        networkOptimizationEnabled = config.getBoolean("network-optimization.enabled", true);
        packetPriorityEnabled = config.getBoolean("network-optimization.packet-priority.enabled", true);
        chunkRateControlEnabled = config.getBoolean("network-optimization.chunk-rate-control.enabled", true);
        congestionDetectionEnabled = config.getBoolean("network-optimization.congestion-detection.enabled", true);
        highPingThreshold = config.getInt("network-optimization.congestion-detection.high-ping-threshold", 200);
        criticalPingThreshold = config.getInt("network-optimization.congestion-detection.critical-ping-threshold", 500);
        highBandwidthThreshold = config.getLong("network-optimization.congestion-detection.high-bandwidth-threshold", 1048576L);
        baseChunkSendRate = config.getInt("network-optimization.chunk-rate-control.base-rate", 10);
        maxChunkSendRate = config.getInt("network-optimization.chunk-rate-control.max-rate", 20);
        minChunkSendRate = config.getInt("network-optimization.chunk-rate-control.min-rate", 3);

        packetSendRateBase = config.getInt("network-optimization.packet-priority.send-rate.base", 50);
        packetSendRateMedium = config.getInt("network-optimization.packet-priority.send-rate.medium", 80);
        packetSendRateHeavy = config.getInt("network-optimization.packet-priority.send-rate.heavy", 110);
        packetSendRateExtreme = config.getInt("network-optimization.packet-priority.send-rate.extreme", 130);

        queueLimitMaxTotal = config.getInt("network-optimization.packet-priority.queue-limits.max-total", 2500);
        queueLimitMaxCritical = config.getInt("network-optimization.packet-priority.queue-limits.max-critical", 1200);
        queueLimitMaxHigh = config.getInt("network-optimization.packet-priority.queue-limits.max-high", 800);
        queueLimitMaxNormal = config.getInt("network-optimization.packet-priority.queue-limits.max-normal", 600);

        accelerationThresholdMedium = config.getInt("network-optimization.packet-priority.acceleration-thresholds.medium", 100);
        accelerationThresholdHeavy = config.getInt("network-optimization.packet-priority.acceleration-thresholds.heavy", 300);
        accelerationThresholdExtreme = config.getInt("network-optimization.packet-priority.acceleration-thresholds.extreme", 500);

        cleanupEnabled = config.getBoolean("network-optimization.packet-priority.cleanup.enabled", true);
        cleanupStaleThreshold = config.getInt("network-optimization.packet-priority.cleanup.stale-threshold", 5);
        cleanupCriticalCleanup = config.getInt("network-optimization.packet-priority.cleanup.critical-cleanup", 100);
        cleanupNormalCleanup = config.getInt("network-optimization.packet-priority.cleanup.normal-cleanup", 50);

        fastMovementChunkLoadEnabled = config.getBoolean("fast-movement-chunk-load.enabled", false);
        fastMovementSpeedThreshold = config.getDouble("fast-movement-chunk-load.speed-threshold", 0.5);
        fastMovementPreloadDistance = config.getInt("fast-movement-chunk-load.preload-distance", 8);
        fastMovementMaxConcurrentLoads = config.getInt("fast-movement-chunk-load.max-concurrent-loads", 4);
        fastMovementPredictionTicks = config.getInt("fast-movement-chunk-load.prediction-ticks", 40);

        centerOffsetEnabled = config.getBoolean("fast-movement-chunk-load.center-offset.enabled", true);
        minOffsetSpeed = config.getDouble("fast-movement-chunk-load.center-offset.min-speed", 3.0);
        maxOffsetSpeed = config.getDouble("fast-movement-chunk-load.center-offset.max-speed", 9.0);
        maxOffsetRatio = config.getDouble("fast-movement-chunk-load.center-offset.max-offset-ratio", 0.75);
        
        asyncLoadingBatchSize = config.getInt("fast-movement-chunk-load.async-loading.batch-size", 2);
        asyncLoadingBatchDelayMs = config.getLong("fast-movement-chunk-load.async-loading.batch-delay-ms", 20L);

        validateConfigVersion();
        validateConfig();
    }

    private void validateConfigVersion() {
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
        densityControlEnabled = config.getBoolean("density.enabled", true);
        maxEntitiesPerChunk = config.getInt("density.max-per-chunk", 80);
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
        universalAiEntitiesConfigFile = config.getString("async-ai.universal-ai-optimization.entities-config-file", "entities.yml");
        universalAiEntities = loadEntitiesFromFile(universalAiEntitiesConfigFile, "universal-ai-entities");
        dabEnabled = config.getBoolean("async-ai.universal-ai-optimization.dynamic-activation.enabled", true);
        dabStartDistance = config.getInt("async-ai.universal-ai-optimization.dynamic-activation.start-distance", 12);
        dabActivationDistMod = config.getInt("async-ai.universal-ai-optimization.dynamic-activation.activation-dist-mod", 8);
        dabMaxTickInterval = config.getInt("async-ai.universal-ai-optimization.dynamic-activation.max-tick-interval", 20);
        asyncPathfindingEnabled = config.getBoolean("async-ai.async-pathfinding.enabled", true);
        asyncPathfindingMaxThreads = config.getInt("async-ai.async-pathfinding.max-threads", 8);
        asyncPathfindingKeepAliveSeconds = config.getInt("async-ai.async-pathfinding.keep-alive-seconds", 60);
        asyncPathfindingMaxQueueSize = config.getInt("async-ai.async-pathfinding.max-queue-size", 500);
        asyncPathfindingTimeoutMs = config.getInt("async-ai.async-pathfinding.timeout-ms", 50);
        entityThrottlingEnabled = config.getBoolean("entity-throttling.enabled", true);
        entityThrottlingConfigFile = config.getString("entity-throttling.config-file", "throttling.yml");
        entityThrottlingCheckInterval = config.getInt("entity-throttling.check-interval", 100);
        entityThrottlingThrottleInterval = config.getInt("entity-throttling.throttle-interval", 3);
        entityThrottlingRemovalBatchSize = config.getInt("entity-throttling.removal-batch-size", 10);
        zeroDelayFactoryOptimizationEnabled = config.getBoolean("block-entity-optimizations.zero-delay-factory-optimization.enabled", false);
        zeroDelayFactoryEntitiesConfigFile = config.getString("block-entity-optimizations.zero-delay-factory-optimization.entities-config-file", "entities.yml");
        zeroDelayFactoryEntities = loadEntitiesFromFile(zeroDelayFactoryEntitiesConfigFile, "zero-delay-factory-entities");
        blockEntityParallelTickEnabled = config.getBoolean("block-entity-optimizations.parallel-tick.enabled", true);
        blockEntityParallelMinBlockEntities = config.getInt("block-entity-optimizations.parallel-tick.min-block-entities", 50);
        blockEntityParallelBatchSize = config.getInt("block-entity-optimizations.parallel-tick.batch-size", 16);
        blockEntityParallelProtectContainers = config.getBoolean("block-entity-optimizations.parallel-tick.protect-containers", true);
        blockEntityParallelTimeoutMs = config.getInt("block-entity-optimizations.parallel-tick.timeout-ms", 50);
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
        if (brainThrottleInterval < 0) {
            brainThrottleInterval = 0;
        }
        if (asyncAITimeoutMicros < 100) {
            plugin.getLogger().warning("Async AI timeout too low, setting to 100");
            asyncAITimeoutMicros = 100;
        }
        if (asyncAITimeoutMicros > 5000) {
            plugin.getLogger().warning("Async AI timeout too high, setting to 5000");
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

    public boolean isDensityControlEnabled() {
        return densityControlEnabled;
    }

    public int getMaxEntitiesPerChunk() {
        return maxEntitiesPerChunk;
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
    public boolean isDabEnabled() { return dabEnabled; }
    public int getDabStartDistance() { return dabStartDistance; }
    public int getDabActivationDistMod() { return dabActivationDistMod; }
    public int getDabMaxTickInterval() { return dabMaxTickInterval; }
    public boolean isAsyncPathfindingEnabled() { return asyncPathfindingEnabled; }
    public int getAsyncPathfindingMaxThreads() { return asyncPathfindingMaxThreads; }
    public int getAsyncPathfindingKeepAliveSeconds() { return asyncPathfindingKeepAliveSeconds; }
    public int getAsyncPathfindingMaxQueueSize() { return asyncPathfindingMaxQueueSize; }
    public int getAsyncPathfindingTimeoutMs() { return asyncPathfindingTimeoutMs; }
    public boolean isEntityThrottlingEnabled() { return entityThrottlingEnabled; }
    public String getEntityThrottlingConfigFile() { return entityThrottlingConfigFile; }
    public int getEntityThrottlingCheckInterval() { return entityThrottlingCheckInterval; }
    public int getEntityThrottlingThrottleInterval() { return entityThrottlingThrottleInterval; }
    public int getEntityThrottlingRemovalBatchSize() { return entityThrottlingRemovalBatchSize; }
    public boolean isZeroDelayFactoryOptimizationEnabled() { return zeroDelayFactoryOptimizationEnabled; }
    public java.util.Set<String> getZeroDelayFactoryEntities() { return zeroDelayFactoryEntities; }
    public boolean isBlockEntityParallelTickEnabled() { return blockEntityParallelTickEnabled; }
    public int getBlockEntityParallelMinBlockEntities() { return blockEntityParallelMinBlockEntities; }
    public int getBlockEntityParallelBatchSize() { return blockEntityParallelBatchSize; }
    public boolean isBlockEntityParallelProtectContainers() { return blockEntityParallelProtectContainers; }
    public int getBlockEntityParallelTimeoutMs() { return blockEntityParallelTimeoutMs; }
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
    public boolean isTNTLandProtectionEnabled() { return tntLandProtectionEnabled; }
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

    public boolean isFurnaceRecipeCacheEnabled() { return furnaceRecipeCacheEnabled; }
    public int getFurnaceRecipeCacheSize() { return furnaceRecipeCacheSize; }
    public boolean isFurnaceCacheApplyToBlastFurnace() { return furnaceCacheApplyToBlastFurnace; }
    public boolean isFurnaceCacheApplyToSmoker() { return furnaceCacheApplyToSmoker; }
    public boolean isFurnaceFixBurnTimeBug() { return furnaceFixBurnTimeBug; }

    public boolean isCraftingRecipeCacheEnabled() { return craftingRecipeCacheEnabled; }
    public int getCraftingRecipeCacheSize() { return craftingRecipeCacheSize; }
    public boolean isCraftingOptimizeBatchCrafting() { return craftingOptimizeBatchCrafting; }
    public boolean isCraftingReduceNetworkTraffic() { return craftingReduceNetworkTraffic; }

    public boolean isMinecartCauldronDestructionEnabled() { return minecartCauldronDestructionEnabled; }

    public int getCurrentConfigVersion() { return CURRENT_CONFIG_VERSION; }

    public boolean isFallingBlockParallelEnabled() { return fallingBlockParallelEnabled; }
    public int getMinFallingBlocksForParallel() { return minFallingBlocksForParallel; }
    public int getFallingBlockBatchSize() { return fallingBlockBatchSize; }

    public boolean isItemEntityParallelEnabled() { return itemEntityParallelEnabled; }
    public int getMinItemEntitiesForParallel() { return minItemEntitiesForParallel; }
    public int getItemEntityBatchSize() { return itemEntityBatchSize; }
    public boolean isItemEntityMergeOptimizationEnabled() { return itemEntityMergeOptimizationEnabled; }
    public int getItemEntityMergeInterval() { return itemEntityMergeInterval; }
    public int getItemEntityMinNearbyItems() { return itemEntityMinNearbyItems; }
    public double getItemEntityMergeRange() { return itemEntityMergeRange; }
    public boolean isItemEntityAgeOptimizationEnabled() { return itemEntityAgeOptimizationEnabled; }
    public int getItemEntityAgeInterval() { return itemEntityAgeInterval; }
    public double getItemEntityPlayerDetectionRange() { return itemEntityPlayerDetectionRange; }

    public boolean isNetworkOptimizationEnabled() { return networkOptimizationEnabled; }
    public boolean isPacketPriorityEnabled() { return packetPriorityEnabled; }
    public boolean isChunkRateControlEnabled() { return chunkRateControlEnabled; }
    public boolean isCongestionDetectionEnabled() { return congestionDetectionEnabled; }
    public int getHighPingThreshold() { return highPingThreshold; }
    public int getCriticalPingThreshold() { return criticalPingThreshold; }
    public long getHighBandwidthThreshold() { return highBandwidthThreshold; }
    public int getBaseChunkSendRate() { return baseChunkSendRate; }
    public int getMaxChunkSendRate() { return maxChunkSendRate; }
    public int getMinChunkSendRate() { return minChunkSendRate; }

    public int getPacketSendRateBase() { return packetSendRateBase; }
    public int getPacketSendRateMedium() { return packetSendRateMedium; }
    public int getPacketSendRateHeavy() { return packetSendRateHeavy; }
    public int getPacketSendRateExtreme() { return packetSendRateExtreme; }

    public int getQueueLimitMaxTotal() { return queueLimitMaxTotal; }
    public int getQueueLimitMaxCritical() { return queueLimitMaxCritical; }
    public int getQueueLimitMaxHigh() { return queueLimitMaxHigh; }
    public int getQueueLimitMaxNormal() { return queueLimitMaxNormal; }

    public int getAccelerationThresholdMedium() { return accelerationThresholdMedium; }
    public int getAccelerationThresholdHeavy() { return accelerationThresholdHeavy; }
    public int getAccelerationThresholdExtreme() { return accelerationThresholdExtreme; }

    public boolean isCleanupEnabled() { return cleanupEnabled; }
    public int getCleanupStaleThreshold() { return cleanupStaleThreshold; }
    public int getCleanupCriticalCleanup() { return cleanupCriticalCleanup; }
    public int getCleanupNormalCleanup() { return cleanupNormalCleanup; }

    public boolean isFastMovementChunkLoadEnabled() { return fastMovementChunkLoadEnabled; }
    public double getFastMovementSpeedThreshold() { return fastMovementSpeedThreshold; }
    public int getFastMovementPreloadDistance() { return fastMovementPreloadDistance; }
    public int getFastMovementMaxConcurrentLoads() { return fastMovementMaxConcurrentLoads; }
    public int getFastMovementPredictionTicks() { return fastMovementPredictionTicks; }

    public boolean isCenterOffsetEnabled() { return centerOffsetEnabled; }
    public double getMinOffsetSpeed() { return minOffsetSpeed; }
    public double getMaxOffsetSpeed() { return maxOffsetSpeed; }
    public double getMaxOffsetRatio() { return maxOffsetRatio; }
    public int getAsyncLoadingBatchSize() { return asyncLoadingBatchSize; }
    public long getAsyncLoadingBatchDelayMs() { return asyncLoadingBatchDelayMs; }

    public boolean getBoolean(String path, boolean defaultValue) {
        return config != null ? config.getBoolean(path, defaultValue) : defaultValue;
    }
}
