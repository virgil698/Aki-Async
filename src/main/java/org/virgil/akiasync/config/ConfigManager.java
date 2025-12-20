package org.virgil.akiasync.config;

import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.util.concurrency.ConfigReloader;

public class ConfigManager {

    private static final int CURRENT_CONFIG_VERSION = 18;

    private final AkiAsyncPlugin plugin;
    private FileConfiguration config;
    
    @Deprecated private boolean entityTrackerEnabled; 
    @Deprecated private int threadPoolSize; 
    @Deprecated private int updateIntervalTicks; 
    @Deprecated private int maxQueueSize; 
    
    private boolean multithreadedTrackerEnabled;
    private int multithreadedTrackerParallelism;
    private int multithreadedTrackerBatchSize;
    private int multithreadedTrackerAssistBatchSize;
    
    private boolean mobSpawningEnabled;
    private boolean spawnerOptimizationEnabled;
    private boolean densityControlEnabled;
    private int maxEntitiesPerChunk;
    private boolean brainThrottle;
    private int brainThrottleInterval;
    private boolean livingEntityTravelOptimizationEnabled;
    private int livingEntityTravelSkipInterval;
    private boolean behaviorThrottleEnabled;
    private int behaviorThrottleInterval;
    private boolean mobDespawnOptimizationEnabled;
    private int mobDespawnCheckInterval;
    private long asyncAITimeoutMicros;
    
    private boolean aiSpatialIndexEnabled;
    private int aiSpatialIndexGridSize;
    private boolean aiSpatialIndexAutoUpdate;
    private boolean aiSpatialIndexPlayerIndexEnabled;
    private boolean aiSpatialIndexPoiIndexEnabled;
    private boolean aiSpatialIndexStatisticsEnabled;
    private int aiSpatialIndexLogIntervalSeconds;
    
    private boolean villagerOptimizationEnabled;
    private boolean villagerUsePOISnapshot;
    private boolean villagerPoiCacheEnabled;
    private int villagerPoiCacheExpireTime;
    private boolean wanderingTraderOptimizationEnabled;
    private boolean wardenOptimizationEnabled;
    private boolean hoglinOptimizationEnabled;
    private boolean allayOptimizationEnabled;
    private boolean endermanOptimizationEnabled;
    private int endermanTickInterval;
    private boolean endermanAllowPickupBlocks;
    private boolean endermanAllowPlaceBlocks;
    
    private boolean endermanBlockCarryLimiterEnabled;
    private int endermanMaxCarrying;
    private boolean endermanCountTowardsMobCap;
    private boolean endermanPreventPickup;
    
    private boolean multithreadedEntityTrackerEnabled;
    
    private boolean armadilloOptimizationEnabled;
    private int armadilloTickInterval;
    private boolean snifferOptimizationEnabled;
    private int snifferTickInterval;
    private boolean camelOptimizationEnabled;
    private int camelTickInterval;
    private boolean frogOptimizationEnabled;
    private int frogTickInterval;
    private boolean goatOptimizationEnabled;
    private int goatTickInterval;
    private boolean pandaOptimizationEnabled;
    private int pandaTickInterval;
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
    
    private boolean brainMemoryOptimizationEnabled;
    private boolean poiSnapshotEnabled;
    private boolean asyncPathfindingEnabled;
    private int asyncPathfindingMaxThreads;
    private int asyncPathfindingKeepAliveSeconds;
    private int asyncPathfindingMaxQueueSize;
    private int asyncPathfindingTimeoutMs;
    private boolean asyncPathfindingSyncFallbackEnabled;
    private boolean asyncPathfindingCacheEnabled;
    private int asyncPathfindingCacheMaxSize;
    private int asyncPathfindingCacheExpireSeconds;
    private int asyncPathfindingCacheReuseTolerance;
    private int asyncPathfindingCacheCleanupIntervalSeconds;
    
    private boolean aiSensorOptimizationEnabled;
    private int aiSensorRefreshInterval;

    private boolean gameEventOptimizationEnabled;
    private boolean gameEventEarlyFilter;
    private boolean gameEventThrottleLowPriority;
    private long gameEventThrottleIntervalMs;
    private boolean gameEventDistanceFilter;
    private double gameEventMaxDetectionDistance;
    
    private boolean enhancedPathfindingEnabled;
    private int enhancedPathfindingMaxConcurrentRequests;
    private int enhancedPathfindingMaxRequestsPerTick;
    private int enhancedPathfindingHighPriorityDistance;
    private int enhancedPathfindingMediumPriorityDistance;
    private boolean pathPrewarmEnabled;
    private int pathPrewarmRadius;
    private int pathPrewarmMaxMobsPerBatch;
    private int pathPrewarmMaxPoisPerMob;
    private boolean collisionOptimizationEnabled;
    private boolean collisionAggressiveMode;
    private String collisionExclusionListFile;
    private java.util.Set<String> collisionExcludedEntities;
    private boolean collisionCacheEnabled;
    private int collisionCacheLifetimeMs;
    private double collisionCacheMovementThreshold;
    private boolean collisionSpatialPartitionEnabled;
    private int collisionSpatialGridSize;
    private int collisionSpatialDensityThreshold;
    private int collisionSpatialUpdateIntervalMs;
    private double collisionSkipMinMovement;
    private int collisionSkipCheckIntervalMs;
    private boolean pushOptimizationEnabled;
    private double pushMaxPushPerTick;
    private double pushDampingFactor;
    private int pushHighDensityThreshold;
    private double pushHighDensityMultiplier;
    private boolean advancedCollisionOptimizationEnabled;
    private int collisionThreshold;
    private float suffocationDamage;
    private int maxPushIterations;
    private boolean vectorizedCollisionEnabled;
    private int vectorizedCollisionThreshold;
    private boolean collisionBlockCacheEnabled;
    private int collisionBlockCacheSize;
    private int collisionBlockCacheExpireTicks;
    private boolean rayCollisionEnabled;
    private double rayCollisionMaxDistance;
    private boolean useSectionGrid;
    private boolean sectionGridWarmup;
    private boolean shapeOptimizationEnabled;
    private boolean shapePrecomputeArrays;
    private boolean shapeBlockShapeCache;
    private int shapeBlockShapeCacheSize;
    private boolean tntCacheEnabled;
    private boolean tntUseOptimizedCache;
    private int tntCacheExpiryTicks;
    private boolean tntCacheWarmupEnabled;
    private boolean tntPrecomputedShapeEnabled;
    private boolean tntUseOcclusionDetection;
    private double tntOcclusionThreshold;
    private boolean tntBatchCollisionEnabled;
    private int tntBatchUnrollFactor;
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
    private boolean hopperOptimizationEnabled;
    private int hopperCacheExpireTime;
    private boolean minecartOptimizationEnabled;
    private int minecartTickInterval;
    private boolean simpleEntitiesOptimizationEnabled;
    private boolean simpleEntitiesUsePOISnapshot;
    private boolean entityTickParallel;
    private int entityTickThreads;
    private int minEntitiesForParallel;
    private int entityTickBatchSize;
    private boolean asyncLightingEnabled;
    private int lightingThreadPoolSize;
    private int lightBatchThreshold;
    private int lightUpdateIntervalMs;
    private boolean useLayeredPropagationQueue;
    private int maxLightPropagationDistance;
    private boolean skylightCacheEnabled;
    private int skylightCacheDurationMs;
    private boolean lightDeduplicationEnabled;
    private boolean dynamicBatchAdjustmentEnabled;
    private boolean advancedLightingStatsEnabled;
    
    private String lightingThreadPoolMode;
    private String lightingThreadPoolCalculation;
    private int lightingMinThreads;
    private int lightingMaxThreads;
    private int lightingBatchThresholdMax;
    private boolean lightingAggressiveBatching;
    
    private boolean lightingPrioritySchedulingEnabled;
    private int lightingHighPriorityRadius;
    private int lightingMediumPriorityRadius;
    private int lightingLowPriorityRadius;
    private long lightingMaxLowPriorityDelay;
    
    private boolean lightingDebouncingEnabled;
    private long lightingDebounceDelay;
    private int lightingMaxUpdatesPerSecond;
    private long lightingResetOnStableMs;
    
    private boolean lightingMergingEnabled;
    private int lightingMergeRadius;
    private long lightingMergeDelay;
    private int lightingMaxMergedUpdates;
    
    private boolean lightingChunkBorderEnabled;
    private boolean lightingBatchBorderUpdates;
    private long lightingBorderUpdateDelay;
    private int lightingCrossChunkBatchSize;
    
    private boolean lightingAdaptiveEnabled;
    private int lightingMonitorInterval;
    private boolean lightingAutoAdjustThreads;
    private boolean lightingAutoAdjustBatchSize;
    private int lightingTargetQueueSize;
    private int lightingTargetLatency;
    
    private boolean lightingChunkUnloadEnabled;
    private boolean lightingAsyncCleanup;
    private int lightingCleanupBatchSize;
    private long lightingCleanupDelay;
    
    private boolean noiseOptimizationEnabled;
    private boolean jigsawOptimizationEnabled;
    
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
    
    private boolean usePandaWireAlgorithm;
    private boolean redstoneNetworkCacheEnabled;
    private int redstoneNetworkCacheExpireTicks;
    
    private boolean tntOptimizationEnabled;
    private boolean tntDebugEnabled;
    private boolean lightingDebugEnabled;
    private boolean landProtectionDebugEnabled;
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
    private boolean endIslandDensityFixEnabled;
    private boolean portalSuffocationCheckDisabled;
    private boolean shulkerBulletSelfHitFixEnabled;
    
    private boolean executeCommandInactiveSkipEnabled;
    private int executeCommandSkipLevel;
    private double executeCommandSimulationDistanceMultiplier;
    private long executeCommandCacheDurationMs;
    private Set<String> executeCommandWhitelistTypes;
    private boolean executeCommandDebugEnabled;
    
    private boolean commandDeduplicationEnabled;
    private boolean commandDeduplicationDebugEnabled;
    
    private boolean tntUseFullRaycast;
    private boolean tntUseVanillaBlockDestruction;
    private boolean tntUseVanillaDrops;
    private boolean tntLandProtectionEnabled;
    private boolean blockLockerProtectionEnabled;
    
    private boolean tntUseSakuraDensityCache;
    private boolean tntUseVectorizedAABB;
    private boolean tntMergeEnabled;
    private double tntMergeRadius;
    private int tntMaxFuseDifference;
    private float tntMergedPowerMultiplier;
    
    private boolean asyncVillagerBreedEnabled;
    private boolean villagerAgeThrottleEnabled;
    private int villagerBreedThreads;
    private int villagerBreedCheckInterval;
    private boolean chunkTickAsyncEnabled;
    private int chunkTickThreads;
    private long chunkTickTimeoutMicros;
    private int chunkTickAsyncBatchSize;
    
    private boolean enableDebugLogging;
    
    private boolean smartLagCompensationEnabled;
    private double smartLagTPSThreshold;
    private boolean smartLagItemPickupDelayEnabled;
    private boolean smartLagPotionEffectsEnabled;
    private boolean smartLagTimeAccelerationEnabled;
    private boolean smartLagDebugEnabled;
    private boolean smartLagLogMissedTicks;
    private boolean smartLagLogCompensation;
    
    private boolean experienceOrbInactiveTickEnabled;
    private double experienceOrbInactiveRange;
    private int experienceOrbInactiveMergeInterval;
    private boolean enablePerformanceMetrics;
    private int configVersion;

    private boolean structureLocationAsyncEnabled;
    private int structureLocationThreads;
    private boolean locateCommandEnabled;
    private int locateCommandSearchRadius;
    private boolean locateCommandSkipKnownStructures;
    private boolean villagerTradeMapsEnabled;
    private int villagerTradeMapsSearchRadius;
    private boolean villagerTradeMapsSkipKnownStructures;
    private java.util.Set<String> villagerTradeMapTypes;
    private int villagerMapGenerationTimeoutSeconds;
    private boolean dolphinTreasureHuntEnabled;
    private int dolphinTreasureSearchRadius;
    private boolean chestExplorationMapsEnabled;
    private java.util.Set<String> chestExplorationLootTables;

    private boolean structureAlgorithmOptimizationEnabled;
    private String structureSearchPattern;
    private boolean structureCachingEnabled;
    private boolean biomeAwareSearchEnabled;
    private int structureCacheMaxSize;
    private long structureCacheExpirationMinutes;

    private boolean dataPackOptimizationEnabled;
    private int dataPackFileLoadThreads;
    private int dataPackZipProcessThreads;
    private int dataPackBatchSize;
    private long dataPackCacheExpirationMinutes;
    private int dataPackMaxFileCacheSize;
    private int dataPackMaxFileSystemCacheSize;

    private boolean nitoriOptimizationsEnabled;
    private boolean virtualThreadEnabled;
    private boolean workStealingEnabled;
    private boolean blockPosCacheEnabled;
    private boolean optimizedCollectionsEnabled;

    private String seedEncryptionScheme;
    private boolean seedEncryptionEnabled;
    private boolean seedEncryptionProtectStructures;
    private boolean seedEncryptionProtectOres;
    private boolean seedEncryptionProtectSlimes;
    private boolean seedEncryptionProtectBiomes;
    
    private int secureSeedBits;
    
    private int quantumSeedEncryptionLevel;
    private String quantumSeedPrivateKeyFile;
    private boolean quantumSeedEnableTimeDecay;
    private int quantumSeedCacheSize;

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

    private boolean entityPacketThrottleEnabled;
    private boolean entityDataThrottleEnabled;
    
    private boolean chunkVisibilityFilterEnabled;
    
    private boolean suffocationOptimizationEnabled;
    private boolean fastRayTraceEnabled;
    private boolean mapRenderingOptimizationEnabled;
    private int mapRenderingThreads;

    private boolean itemEntityMergeOptimizationEnabled;
    private boolean itemEntityCancelVanillaMerge;
    private int itemEntityMergeInterval;
    private int itemEntityMinNearbyItems;
    private double itemEntityMergeRange;
    private boolean itemEntityAgeOptimizationEnabled;
    private int itemEntityAgeInterval;
    private double itemEntityPlayerDetectionRange;
    private boolean itemEntityInactiveTickEnabled;
    private double itemEntityInactiveRange;
    private int itemEntityInactiveMergeInterval;

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
    
    private boolean playerJoinWarmupEnabled;
    private long playerJoinWarmupDurationMs;
    private double playerJoinWarmupInitialRate;

    private boolean virtualEntityCompatibilityEnabled;
    private boolean virtualEntityBypassPacketQueue;
    private boolean virtualEntityExcludeFromThrottling;
    private boolean fancynpcsCompatEnabled;
    private boolean fancynpcsUseAPI;
    private int fancynpcsPriority;
    private boolean znpcsplusCompatEnabled;
    private boolean znpcsplusUseAPI;
    private int znpcsplusPriority;
    private java.util.List<String> virtualEntityDetectionOrder;

    public ConfigManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    private java.util.Set<String> loadEntitiesFromFile(String fileName, String key) {
        try {
            
            java.io.File entitiesFile = new java.io.File(plugin.getDataFolder(), fileName);
            
            if (!entitiesFile.exists()) {
                
                plugin.saveResource(fileName, false);
                plugin.getLogger().info("[Config] Created " + fileName);
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
        
        loadConfigValues();
    }
    
    private void loadConfigValues() {
        
        int configuredThreadPoolSize = config.getInt("general-thread-pool.size", 0);
        if (configuredThreadPoolSize <= 0) {
            int cpuCores = Runtime.getRuntime().availableProcessors();
            threadPoolSize = Math.max(2, cpuCores / 4);  
        } else {
            threadPoolSize = configuredThreadPoolSize;
        }
        
        entityTrackerEnabled = true; 
        updateIntervalTicks = 1; 
        maxQueueSize = 10000; 
        
        multithreadedEntityTrackerEnabled = config.getBoolean("entity-tracker.enabled", true);
        
        multithreadedTrackerEnabled = config.getBoolean("entity-tracker.enabled", true);
        multithreadedTrackerParallelism = config.getInt("entity-tracker.parallelism", 
            Math.max(4, Runtime.getRuntime().availableProcessors()));
        multithreadedTrackerBatchSize = config.getInt("entity-tracker.batch-size", 10);
        multithreadedTrackerAssistBatchSize = config.getInt("entity-tracker.assist-batch-size", 5);
        
        mobSpawningEnabled = config.getBoolean("mob-spawning.enabled", true);
        spawnerOptimizationEnabled = config.getBoolean("mob-spawning.spawner-optimization", true);
        mobSpawnInterval = config.getInt("mob-spawning.spawn-interval", 1);
        densityControlEnabled = config.getBoolean("density.enabled", true);
        maxEntitiesPerChunk = config.getInt("density.max-per-chunk", 80);
        brainThrottle = config.getBoolean("brain.enabled", true);
        brainThrottleInterval = config.getInt("brain.throttle-interval", 10);
        livingEntityTravelOptimizationEnabled = config.getBoolean("living-entity-travel-optimization.enabled", true);
        livingEntityTravelSkipInterval = config.getInt("living-entity-travel-optimization.skip-interval", 2);
        behaviorThrottleEnabled = config.getBoolean("behavior-throttle.enabled", false);
        behaviorThrottleInterval = config.getInt("behavior-throttle.throttle-interval", 3);
        mobDespawnOptimizationEnabled = config.getBoolean("mob-despawn-optimization.enabled", true);
        mobDespawnCheckInterval = config.getInt("mob-despawn-optimization.check-interval", 20);
        asyncAITimeoutMicros = config.getLong("async-ai.timeout-microseconds", 500L);
        
        aiSpatialIndexEnabled = config.getBoolean("async-ai.spatial-index.enabled", true);
        aiSpatialIndexGridSize = config.getInt("async-ai.spatial-index.grid-size", 16);
        aiSpatialIndexAutoUpdate = config.getBoolean("async-ai.spatial-index.auto-update", true);
        aiSpatialIndexPlayerIndexEnabled = config.getBoolean("async-ai.spatial-index.index-players", true);
        aiSpatialIndexPoiIndexEnabled = config.getBoolean("async-ai.spatial-index.index-poi", true);
        aiSpatialIndexStatisticsEnabled = config.getBoolean("async-ai.spatial-index.statistics.enabled", true);
        aiSpatialIndexLogIntervalSeconds = config.getInt("async-ai.spatial-index.statistics.log-interval-seconds", 300);
        
        villagerOptimizationEnabled = config.getBoolean("async-ai.villager-optimization.enabled", false);
        villagerUsePOISnapshot = config.getBoolean("async-ai.villager-optimization.use-poi-snapshot", true);
        villagerPoiCacheEnabled = config.getBoolean("async-ai.villager-optimization.poi-cache.enabled", true);
        villagerPoiCacheExpireTime = config.getInt("async-ai.villager-optimization.poi-cache.cache-expire-time", 100);
        wanderingTraderOptimizationEnabled = config.getBoolean("async-ai.wandering-trader-optimization.enabled", false);
        wardenOptimizationEnabled = config.getBoolean("async-ai.warden-optimization.enabled", false);
        hoglinOptimizationEnabled = config.getBoolean("async-ai.hoglin-optimization.enabled", false);
        allayOptimizationEnabled = config.getBoolean("async-ai.allay-optimization.enabled", false);
        endermanOptimizationEnabled = config.getBoolean("async-ai.enderman-optimization.enabled", true);
        endermanTickInterval = config.getInt("async-ai.enderman-optimization.tick-interval", 3);
        endermanAllowPickupBlocks = config.getBoolean("async-ai.enderman-optimization.allow-pickup-blocks", true);
        endermanAllowPlaceBlocks = config.getBoolean("async-ai.enderman-optimization.allow-place-blocks", true);
        endermanBlockCarryLimiterEnabled = config.getBoolean("enderman-block-carry-limiter.enabled", true);
        endermanMaxCarrying = config.getInt("enderman-block-carry-limiter.max-carrying-endermen", 50);
        endermanCountTowardsMobCap = config.getBoolean("enderman-block-carry-limiter.count-towards-mob-cap", true);
        endermanPreventPickup = config.getBoolean("enderman-block-carry-limiter.prevent-pickup-when-limit-reached", true);
        
        armadilloOptimizationEnabled = config.getBoolean("async-ai.armadillo-optimization.enabled", true);
        armadilloTickInterval = config.getInt("async-ai.armadillo-optimization.tick-interval", 3);
        snifferOptimizationEnabled = config.getBoolean("async-ai.sniffer-optimization.enabled", true);
        snifferTickInterval = config.getInt("async-ai.sniffer-optimization.tick-interval", 3);
        camelOptimizationEnabled = config.getBoolean("async-ai.camel-optimization.enabled", true);
        camelTickInterval = config.getInt("async-ai.camel-optimization.tick-interval", 3);
        frogOptimizationEnabled = config.getBoolean("async-ai.frog-optimization.enabled", true);
        frogTickInterval = config.getInt("async-ai.frog-optimization.tick-interval", 3);
        goatOptimizationEnabled = config.getBoolean("async-ai.goat-optimization.enabled", true);
        goatTickInterval = config.getInt("async-ai.goat-optimization.tick-interval", 3);
        pandaOptimizationEnabled = config.getBoolean("async-ai.panda-optimization.enabled", true);
        pandaTickInterval = config.getInt("async-ai.panda-optimization.tick-interval", 3);
        
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
        
        brainMemoryOptimizationEnabled = config.getBoolean("async-ai.brain-memory-optimization.enabled", false);
        poiSnapshotEnabled = config.getBoolean("async-ai.poi-snapshot.enabled", false);

        aiSensorOptimizationEnabled = config.getBoolean("async-ai.sensor-optimization.enabled", true);
        aiSensorRefreshInterval = config.getInt("async-ai.sensor-optimization.sensing-refresh-interval", 10);

        gameEventOptimizationEnabled = config.getBoolean("async-ai.game-event-optimization.enabled", true);
        gameEventEarlyFilter = config.getBoolean("async-ai.game-event-optimization.early-filter", true);
        gameEventThrottleLowPriority = config.getBoolean("async-ai.game-event-optimization.throttle-low-priority", true);
        gameEventThrottleIntervalMs = config.getLong("async-ai.game-event-optimization.throttle-interval-ms", 50L);
        gameEventDistanceFilter = config.getBoolean("async-ai.game-event-optimization.distance-filter", true);
        gameEventMaxDetectionDistance = config.getDouble("async-ai.game-event-optimization.max-detection-distance", 64.0);
        
        asyncPathfindingEnabled = config.getBoolean("async-ai.async-pathfinding.enabled", true);
        asyncPathfindingMaxThreads = config.getInt("async-ai.async-pathfinding.max-threads", 6);
        asyncPathfindingKeepAliveSeconds = config.getInt("async-ai.async-pathfinding.keep-alive-seconds", 60);
        asyncPathfindingMaxQueueSize = config.getInt("async-ai.async-pathfinding.max-queue-size", 500);
        asyncPathfindingTimeoutMs = config.getInt("async-ai.async-pathfinding.timeout-ms", 100);
        asyncPathfindingSyncFallbackEnabled = config.getBoolean("async-ai.async-pathfinding.sync-fallback-enabled", true);
        
        enhancedPathfindingEnabled = config.getBoolean("async-ai.async-pathfinding.enhanced.enabled", true);
        enhancedPathfindingMaxConcurrentRequests = config.getInt("async-ai.async-pathfinding.enhanced.max-concurrent-requests", 30);
        enhancedPathfindingMaxRequestsPerTick = config.getInt("async-ai.async-pathfinding.enhanced.max-requests-per-tick", 15);
        enhancedPathfindingHighPriorityDistance = config.getInt("async-ai.async-pathfinding.enhanced.priority.high-distance", 16);
        enhancedPathfindingMediumPriorityDistance = config.getInt("async-ai.async-pathfinding.enhanced.priority.medium-distance", 48);
        pathPrewarmEnabled = config.getBoolean("async-ai.async-pathfinding.enhanced.prewarm.enabled", true);
        pathPrewarmRadius = config.getInt("async-ai.async-pathfinding.enhanced.prewarm.radius", 32);
        pathPrewarmMaxMobsPerBatch = config.getInt("async-ai.async-pathfinding.enhanced.prewarm.max-mobs-per-batch", 5);
        pathPrewarmMaxPoisPerMob = config.getInt("async-ai.async-pathfinding.enhanced.prewarm.max-pois-per-mob", 3);
        
        loadPathfindingAndCollisionConfigs();
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
        hopperOptimizationEnabled = config.getBoolean("block-entity-optimizations.hopper-optimization.enabled", true);
        hopperCacheExpireTime = config.getInt("block-entity-optimizations.hopper-optimization.cache-expire-time", 100);
        minecartOptimizationEnabled = config.getBoolean("block-entity-optimizations.minecart-optimization.enabled", true);
        minecartTickInterval = config.getInt("block-entity-optimizations.minecart-optimization.tick-interval", 2);
        simpleEntitiesOptimizationEnabled = config.getBoolean("async-ai.simple-entities.enabled", false);
        simpleEntitiesUsePOISnapshot = config.getBoolean("async-ai.simple-entities.use-poi-snapshot", false);
        entityTickParallel = config.getBoolean("entity-tick-parallel.enabled", true);
        entityTickThreads = config.getInt("entity-tick-parallel.threads", 4);
        minEntitiesForParallel = config.getInt("entity-tick-parallel.min-entities", 100);
        entityTickBatchSize = config.getInt("entity-tick-parallel.batch-size", 8);
        asyncLightingEnabled = config.getBoolean("lighting-optimizations.enabled", true);
        lightingThreadPoolSize = config.getInt("lighting-optimizations.async-lighting.thread-pool-size", 2);
        lightBatchThreshold = config.getInt("lighting-optimizations.async-lighting.batch-threshold", 16);
        lightUpdateIntervalMs = config.getInt("lighting-optimizations.update-interval-ms", 10);
        useLayeredPropagationQueue = config.getBoolean("lighting-optimizations.propagation-queue.use-layered-queue", true);
        maxLightPropagationDistance = config.getInt("lighting-optimizations.propagation-queue.max-propagation-distance", 15);
        skylightCacheEnabled = config.getBoolean("lighting-optimizations.skylight-cache.enabled", true);
        skylightCacheDurationMs = config.getInt("lighting-optimizations.skylight-cache.cache-duration-ms", 100);
        lightDeduplicationEnabled = config.getBoolean("lighting-optimizations.advanced.enable-deduplication", true);
        dynamicBatchAdjustmentEnabled = config.getBoolean("lighting-optimizations.advanced.dynamic-batch-adjustment", true);
        advancedLightingStatsEnabled = config.getBoolean("lighting-optimizations.advanced.log-advanced-stats", false);
        
        lightingThreadPoolMode = config.getString("lighting-optimizations.async-lighting.thread-pool-mode", "auto");
        lightingThreadPoolCalculation = config.getString("lighting-optimizations.async-lighting.thread-pool-calculation", "cores/3");
        lightingMinThreads = config.getInt("lighting-optimizations.async-lighting.min-threads", 1);
        lightingMaxThreads = config.getInt("lighting-optimizations.async-lighting.max-threads", 8);
        lightingBatchThresholdMax = config.getInt("lighting-optimizations.async-lighting.batch-threshold-max", 64);
        lightingAggressiveBatching = config.getBoolean("lighting-optimizations.async-lighting.aggressive-batching", false);
        
        lightingPrioritySchedulingEnabled = config.getBoolean("lighting-optimizations.async-lighting.priority-scheduling.enabled", true);
        lightingHighPriorityRadius = config.getInt("lighting-optimizations.async-lighting.priority-scheduling.player-radius-high-priority", 32);
        lightingMediumPriorityRadius = config.getInt("lighting-optimizations.async-lighting.priority-scheduling.player-radius-medium-priority", 64);
        lightingLowPriorityRadius = config.getInt("lighting-optimizations.async-lighting.priority-scheduling.player-radius-low-priority", 128);
        lightingMaxLowPriorityDelay = config.getLong("lighting-optimizations.async-lighting.priority-scheduling.max-low-priority-delay-ms", 500);
        
        lightingDebouncingEnabled = config.getBoolean("lighting-optimizations.async-lighting.debouncing.enabled", true);
        lightingDebounceDelay = config.getLong("lighting-optimizations.async-lighting.debouncing.debounce-delay-ms", 50);
        lightingMaxUpdatesPerSecond = config.getInt("lighting-optimizations.async-lighting.debouncing.max-updates-per-second", 20);
        lightingResetOnStableMs = config.getLong("lighting-optimizations.async-lighting.debouncing.reset-on-stable-ms", 200);
        
        lightingMergingEnabled = config.getBoolean("lighting-optimizations.async-lighting.update-merging.enabled", true);
        lightingMergeRadius = config.getInt("lighting-optimizations.async-lighting.update-merging.merge-radius", 2);
        lightingMergeDelay = config.getLong("lighting-optimizations.async-lighting.update-merging.merge-delay-ms", 10);
        lightingMaxMergedUpdates = config.getInt("lighting-optimizations.async-lighting.update-merging.max-merged-updates", 64);
        
        lightingChunkBorderEnabled = config.getBoolean("lighting-optimizations.async-lighting.chunk-border.enabled", true);
        lightingBatchBorderUpdates = config.getBoolean("lighting-optimizations.async-lighting.chunk-border.batch-border-updates", true);
        lightingBorderUpdateDelay = config.getLong("lighting-optimizations.async-lighting.chunk-border.border-update-delay-ms", 20);
        lightingCrossChunkBatchSize = config.getInt("lighting-optimizations.async-lighting.chunk-border.cross-chunk-batch-size", 32);
        
        lightingAdaptiveEnabled = config.getBoolean("lighting-optimizations.async-lighting.adaptive.enabled", true);
        lightingMonitorInterval = config.getInt("lighting-optimizations.async-lighting.adaptive.monitor-interval-seconds", 10);
        lightingAutoAdjustThreads = config.getBoolean("lighting-optimizations.async-lighting.adaptive.auto-adjust-threads", true);
        lightingAutoAdjustBatchSize = config.getBoolean("lighting-optimizations.async-lighting.adaptive.auto-adjust-batch-size", true);
        lightingTargetQueueSize = config.getInt("lighting-optimizations.async-lighting.adaptive.target-queue-size", 100);
        lightingTargetLatency = config.getInt("lighting-optimizations.async-lighting.adaptive.target-latency-ms", 50);
        
        lightingChunkUnloadEnabled = config.getBoolean("lighting-optimizations.async-lighting.chunk-unload.enabled", true);
        lightingAsyncCleanup = config.getBoolean("lighting-optimizations.async-lighting.chunk-unload.async-cleanup", true);
        lightingCleanupBatchSize = config.getInt("lighting-optimizations.async-lighting.chunk-unload.cleanup-batch-size", 16);
        lightingCleanupDelay = config.getLong("lighting-optimizations.async-lighting.chunk-unload.cleanup-delay-ms", 100);
        
        noiseOptimizationEnabled = config.getBoolean("chunk-generation.noise-optimization.enabled", true);
        jigsawOptimizationEnabled = config.getBoolean("chunk-generation.jigsaw-optimization.enabled", true);
        
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
        
        usePandaWireAlgorithm = config.getBoolean("redstone-optimizations.pandawire.enabled", false);
        redstoneNetworkCacheEnabled = config.getBoolean("redstone-optimizations.network-cache.enabled", false);
        redstoneNetworkCacheExpireTicks = config.getInt("redstone-optimizations.network-cache.expire-ticks", 600);
        
        chunkTickAsyncEnabled = config.getBoolean("chunk-tick-async.enabled", false);
        chunkTickThreads = config.getInt("chunk-tick-async.threads", 4);
        chunkTickTimeoutMicros = config.getLong("chunk-tick-async.timeout-us", 200L);
        tntOptimizationEnabled = config.getBoolean("tnt-explosion-optimization.enabled", true);
        tntDebugEnabled = config.getBoolean("performance.debug-logging.modules.tnt", false);
        lightingDebugEnabled = config.getBoolean("performance.debug-logging.modules.lighting", false);
        landProtectionDebugEnabled = config.getBoolean("performance.debug-logging.modules.land-protection", false);
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
        blockLockerProtectionEnabled = config.getBoolean("tnt-explosion-optimization.blocklocker-protection.enabled", true);
        
        tntUseSakuraDensityCache = config.getBoolean("tnt-explosion-optimization.density-cache.enabled", true);
        tntUseVectorizedAABB = config.getBoolean("tnt-explosion-optimization.vectorized-aabb.enabled", true);
        tntMergeEnabled = config.getBoolean("tnt-explosion-optimization.entity-merge.enabled", false);
        tntMergeRadius = config.getDouble("tnt-explosion-optimization.entity-merge.radius", 1.5);
        tntMaxFuseDifference = config.getInt("tnt-explosion-optimization.entity-merge.max-fuse-difference", 5);
        tntMergedPowerMultiplier = (float) config.getDouble("tnt-explosion-optimization.entity-merge.power-multiplier", 0.5);
        
        beeFixEnabled = config.getBoolean("bee-fix.enabled", true);
        endIslandDensityFixEnabled = config.getBoolean("end-island-density-fix.enabled", true);
        
        enableDebugLogging = config.getBoolean("performance.debug-logging", false);
        
        smartLagCompensationEnabled = config.getBoolean("smart-lag-compensation.enabled", true);
        smartLagTPSThreshold = config.getDouble("smart-lag-compensation.tps-threshold", 18.0);
        smartLagItemPickupDelayEnabled = config.getBoolean("smart-lag-compensation.item-pickup-delay.enabled", true);
        smartLagPotionEffectsEnabled = config.getBoolean("smart-lag-compensation.potion-effects.enabled", false);
        smartLagTimeAccelerationEnabled = config.getBoolean("smart-lag-compensation.time-acceleration.enabled", false);
        smartLagDebugEnabled = config.getBoolean("performance.debug-logging.smart-lag-compensation.enabled", false);
        smartLagLogMissedTicks = config.getBoolean("performance.debug-logging.smart-lag-compensation.log-missed-ticks", false);
        smartLagLogCompensation = config.getBoolean("performance.debug-logging.smart-lag-compensation.log-compensation", false);
        
        experienceOrbInactiveTickEnabled = config.getBoolean("experience-orb-optimizations.inactive-tick.enabled", true);
        experienceOrbInactiveRange = config.getDouble("experience-orb-optimizations.inactive-tick.inactive-range", 32.0);
        experienceOrbInactiveMergeInterval = config.getInt("experience-orb-optimizations.inactive-tick.merge-interval", 100);
        enablePerformanceMetrics = config.getBoolean("performance.enable-metrics", true);
        configVersion = config.getInt("version", 6);

        structureLocationAsyncEnabled = config.getBoolean("structure-location-async.enabled", true);
        structureLocationThreads = config.getInt("structure-location-async.threads", 3);
        locateCommandEnabled = config.getBoolean("structure-location-async.locate-command.enabled", true);
        locateCommandSearchRadius = config.getInt("structure-location-async.locate-command.search-radius", 100);
        locateCommandSkipKnownStructures = config.getBoolean("structure-location-async.locate-command.skip-known-structures", false);
        villagerTradeMapsEnabled = config.getBoolean("structure-location-async.villager-trade-maps.enabled", true);
        villagerTradeMapsSearchRadius = config.getInt("structure-location-async.villager-trade-maps.search-radius", 100);
        villagerTradeMapsSkipKnownStructures = config.getBoolean("structure-location-async.villager-trade-maps.skip-known-structures", false);
        villagerTradeMapTypes = new java.util.HashSet<>(config.getStringList("structure-location-async.villager-trade-maps.trade-types"));
        if (villagerTradeMapTypes.isEmpty()) {
            villagerTradeMapTypes.add("minecraft:ocean_monument_map");
            villagerTradeMapTypes.add("minecraft:woodland_mansion_map");
            villagerTradeMapTypes.add("minecraft:buried_treasure_map");
        }
        villagerMapGenerationTimeoutSeconds = config.getInt("structure-location-async.villager-trade-maps.generation-timeout-seconds", 30);
        dolphinTreasureHuntEnabled = config.getBoolean("structure-location-async.dolphin-treasure-hunt.enabled", true);
        dolphinTreasureSearchRadius = config.getInt("structure-location-async.dolphin-treasure-hunt.search-radius", 50);
        chestExplorationMapsEnabled = config.getBoolean("structure-location-async.chest-exploration-maps.enabled", true);
        chestExplorationLootTables = new java.util.HashSet<>(config.getStringList("structure-location-async.chest-exploration-maps.loot-tables"));
        if (chestExplorationLootTables.isEmpty()) {
            chestExplorationLootTables.add("minecraft:chests/shipwreck_map");
            chestExplorationLootTables.add("minecraft:chests/underwater_ruin_big");
            chestExplorationLootTables.add("minecraft:chests/underwater_ruin_small");
        }

        structureAlgorithmOptimizationEnabled = config.getBoolean("structure-location-async.algorithm-optimization.enabled", true);
        structureSearchPattern = config.getString("structure-location-async.algorithm-optimization.search-pattern", "hybrid");
        structureCachingEnabled = config.getBoolean("structure-location-async.algorithm-optimization.caching.enabled", true);
        structureCacheMaxSize = config.getInt("structure-location-async.algorithm-optimization.caching.max-size", 1000);
        structureCacheExpirationMinutes = config.getLong("structure-location-async.algorithm-optimization.caching.expiration-minutes", 30L);
        biomeAwareSearchEnabled = config.getBoolean("structure-location-async.algorithm-optimization.biome-aware-search.enabled", true);

        dataPackOptimizationEnabled = config.getBoolean("datapack-optimization.enabled", true);
        dataPackFileLoadThreads = config.getInt("datapack-optimization.file-load-threads", 4);
        dataPackZipProcessThreads = config.getInt("datapack-optimization.zip-process-threads", 2);
        dataPackBatchSize = config.getInt("datapack-optimization.batch-size", 100);
        dataPackCacheExpirationMinutes = config.getLong("datapack-optimization.cache-expiration-minutes", 30L);
        dataPackMaxFileCacheSize = config.getInt("datapack-optimization.max-file-cache-size", 1000);
        dataPackMaxFileSystemCacheSize = config.getInt("datapack-optimization.max-filesystem-cache-size", 50);

        nitoriOptimizationsEnabled = config.getBoolean("nitori.enabled", true);
        virtualThreadEnabled = config.getBoolean("nitori.virtual-threads", true);
        workStealingEnabled = config.getBoolean("nitori.work-stealing", true);
        blockPosCacheEnabled = config.getBoolean("nitori.blockpos-cache", true);
        optimizedCollectionsEnabled = config.getBoolean("nitori.optimized-collections", true);

        if (config.contains("seed-encryption.scheme")) {

            seedEncryptionScheme = config.getString("seed-encryption.scheme", "quantum");
            seedEncryptionEnabled = config.getBoolean("seed-encryption.enabled", false);
            seedEncryptionProtectStructures = config.getBoolean("seed-encryption.protect-structures", true);
            seedEncryptionProtectOres = config.getBoolean("seed-encryption.protect-ores", true);
            seedEncryptionProtectSlimes = config.getBoolean("seed-encryption.protect-slimes", true);
            seedEncryptionProtectBiomes = config.getBoolean("seed-encryption.protect-biomes", true);
            
            secureSeedBits = config.getInt("seed-encryption.legacy.seed-bits", 1024);
            
            quantumSeedEncryptionLevel = config.getInt("seed-encryption.quantum.encryption-level", 2);
            quantumSeedPrivateKeyFile = config.getString("seed-encryption.quantum.private-key-file", "quantum-seed.key");
            quantumSeedEnableTimeDecay = config.getBoolean("seed-encryption.quantum.enable-time-decay", false);
            quantumSeedCacheSize = config.getInt("seed-encryption.quantum.cache-size", 10000);
        } else {

            seedEncryptionScheme = "legacy";
            seedEncryptionEnabled = config.getBoolean("secure-seed.enabled", false);
            seedEncryptionProtectStructures = config.getBoolean("secure-seed.protect-structures", true);
            seedEncryptionProtectOres = config.getBoolean("secure-seed.protect-ores", true);
            seedEncryptionProtectSlimes = config.getBoolean("secure-seed.protect-slimes", true);
            seedEncryptionProtectBiomes = false;
            secureSeedBits = config.getInt("secure-seed.seed-bits", 1024);
            
            quantumSeedEncryptionLevel = 2;
            quantumSeedPrivateKeyFile = "quantum-seed.key";
            quantumSeedEnableTimeDecay = false;
            quantumSeedCacheSize = 10000;
        }

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

        itemEntityMergeOptimizationEnabled = config.getBoolean("item-entity-optimizations.smart-merge.enabled", true);
        itemEntityCancelVanillaMerge = config.getBoolean("item-entity-optimizations.smart-merge.cancel-vanilla-merge", true);
        itemEntityMergeInterval = config.getInt("item-entity-optimizations.smart-merge.merge-interval", 5);
        itemEntityMinNearbyItems = config.getInt("item-entity-optimizations.smart-merge.min-nearby-items", 3);
        itemEntityMergeRange = config.getDouble("item-entity-optimizations.smart-merge.merge-range", 1.5);
        itemEntityAgeOptimizationEnabled = config.getBoolean("item-entity-optimizations.age-optimization.enabled", true);
        itemEntityAgeInterval = config.getInt("item-entity-optimizations.age-optimization.age-interval", 10);
        itemEntityPlayerDetectionRange = config.getDouble("item-entity-optimizations.age-optimization.player-detection-range", 8.0);
        itemEntityInactiveTickEnabled = config.getBoolean("item-entity-optimizations.inactive-tick.enabled", true);
        itemEntityInactiveRange = config.getDouble("item-entity-optimizations.inactive-tick.inactive-range", 32.0);
        itemEntityInactiveMergeInterval = config.getInt("item-entity-optimizations.inactive-tick.merge-interval", 100);

        entityPacketThrottleEnabled = config.getBoolean("entity-packet-throttle.enabled", true);
        
        entityDataThrottleEnabled = config.getBoolean("entity-packet-throttle.data-throttle.enabled", true);
        
        chunkVisibilityFilterEnabled = config.getBoolean("chunk-visibility-filter.enabled", true);

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
        
        playerJoinWarmupEnabled = config.getBoolean("fast-movement-chunk-load.player-join-warmup.enabled", true);
        playerJoinWarmupDurationMs = config.getLong("fast-movement-chunk-load.player-join-warmup.warmup-duration-ms", 3000L);
        playerJoinWarmupInitialRate = config.getDouble("fast-movement-chunk-load.player-join-warmup.initial-rate", 0.5);

        suffocationOptimizationEnabled = config.getBoolean("suffocation-optimization.enabled", true);
        
        fastRayTraceEnabled = config.getBoolean("async-ai.villager-optimization.fast-raytrace.enabled", true);
        asyncVillagerBreedEnabled = config.getBoolean("async-ai.villager-optimization.breed-optimization.async-villager-breed", true);
        villagerAgeThrottleEnabled = config.getBoolean("async-ai.villager-optimization.breed-optimization.age-throttle", true);
        villagerBreedThreads = config.getInt("async-ai.villager-optimization.breed-optimization.threads", 4);
        villagerBreedCheckInterval = config.getInt("async-ai.villager-optimization.breed-optimization.check-interval", 5);
        
        mapRenderingOptimizationEnabled = config.getBoolean("fast-movement-chunk-load.map-rendering.enabled", false);
        mapRenderingThreads = config.getInt("fast-movement-chunk-load.map-rendering.threads", 2);

        virtualEntityCompatibilityEnabled = config.getBoolean("virtual-entity-compatibility.enabled", true);
        virtualEntityBypassPacketQueue = config.getBoolean("virtual-entity-compatibility.bypass-packet-queue", true);
        virtualEntityExcludeFromThrottling = config.getBoolean("virtual-entity-compatibility.exclude-from-throttling", true);
        
        fancynpcsCompatEnabled = config.getBoolean("virtual-entity-compatibility.plugins.fancynpcs.enabled", true);
        fancynpcsUseAPI = config.getBoolean("virtual-entity-compatibility.plugins.fancynpcs.use-api", true);
        fancynpcsPriority = config.getInt("virtual-entity-compatibility.plugins.fancynpcs.priority", 90);
        
        znpcsplusCompatEnabled = config.getBoolean("virtual-entity-compatibility.plugins.znpcsplus.enabled", true);
        znpcsplusUseAPI = config.getBoolean("virtual-entity-compatibility.plugins.znpcsplus.use-api", false);
        znpcsplusPriority = config.getInt("virtual-entity-compatibility.plugins.znpcsplus.priority", 90);
        
        virtualEntityDetectionOrder = config.getStringList("virtual-entity-compatibility.detection-order");

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
            if (plugin.getConfig().getBoolean("performance.debug-logging", false)) {
                plugin.getLogger().severe("Stack trace: " + getStackTraceAsString(e));
            }
            return false;
        }
    }
    
    private String getStackTraceAsString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    private void reloadConfigWithoutValidation() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        int configuredThreadPoolSize = config.getInt("general-thread-pool.size", 0);
        if (configuredThreadPoolSize <= 0) {
            int cpuCores = Runtime.getRuntime().availableProcessors();
            threadPoolSize = Math.max(2, cpuCores / 4);
        } else {
            threadPoolSize = configuredThreadPoolSize;
        }
        
        entityTrackerEnabled = true;
        updateIntervalTicks = 1;
        maxQueueSize = 10000;
        
        multithreadedEntityTrackerEnabled = config.getBoolean("entity-tracker.enabled", true);
        
        mobSpawningEnabled = config.getBoolean("mob-spawning.enabled", true);
        spawnerOptimizationEnabled = config.getBoolean("mob-spawning.spawner-optimization", true);
        mobSpawnInterval = config.getInt("mob-spawning.spawn-interval", 1);
        densityControlEnabled = config.getBoolean("density.enabled", true);
        maxEntitiesPerChunk = config.getInt("density.max-per-chunk", 80);
        brainThrottle = config.getBoolean("brain.enabled", true);
        brainThrottleInterval = config.getInt("brain.throttle-interval", 10);
        asyncAITimeoutMicros = config.getLong("async-ai.timeout-microseconds", 500L);
        villagerOptimizationEnabled = config.getBoolean("async-ai.villager-optimization.enabled", false);
        villagerUsePOISnapshot = config.getBoolean("async-ai.villager-optimization.use-poi-snapshot", true);
        wanderingTraderOptimizationEnabled = config.getBoolean("async-ai.wandering-trader-optimization.enabled", false);
        wardenOptimizationEnabled = config.getBoolean("async-ai.warden-optimization.enabled", false);
        hoglinOptimizationEnabled = config.getBoolean("async-ai.hoglin-optimization.enabled", false);
        allayOptimizationEnabled = config.getBoolean("async-ai.allay-optimization.enabled", false);
        endermanOptimizationEnabled = config.getBoolean("async-ai.enderman-optimization.enabled", true);
        endermanTickInterval = config.getInt("async-ai.enderman-optimization.tick-interval", 3);
        endermanAllowPickupBlocks = config.getBoolean("async-ai.enderman-optimization.allow-pickup-blocks", true);
        endermanAllowPlaceBlocks = config.getBoolean("async-ai.enderman-optimization.allow-place-blocks", true);
        endermanBlockCarryLimiterEnabled = config.getBoolean("enderman-block-carry-limiter.enabled", true);
        endermanMaxCarrying = config.getInt("enderman-block-carry-limiter.max-carrying-endermen", 50);
        endermanCountTowardsMobCap = config.getBoolean("enderman-block-carry-limiter.count-towards-mob-cap", true);
        endermanPreventPickup = config.getBoolean("enderman-block-carry-limiter.prevent-pickup-when-limit-reached", true);
        
        armadilloOptimizationEnabled = config.getBoolean("async-ai.armadillo-optimization.enabled", true);
        armadilloTickInterval = config.getInt("async-ai.armadillo-optimization.tick-interval", 3);
        snifferOptimizationEnabled = config.getBoolean("async-ai.sniffer-optimization.enabled", true);
        snifferTickInterval = config.getInt("async-ai.sniffer-optimization.tick-interval", 3);
        camelOptimizationEnabled = config.getBoolean("async-ai.camel-optimization.enabled", true);
        camelTickInterval = config.getInt("async-ai.camel-optimization.tick-interval", 3);
        frogOptimizationEnabled = config.getBoolean("async-ai.frog-optimization.enabled", true);
        frogTickInterval = config.getInt("async-ai.frog-optimization.tick-interval", 3);
        goatOptimizationEnabled = config.getBoolean("async-ai.goat-optimization.enabled", true);
        goatTickInterval = config.getInt("async-ai.goat-optimization.tick-interval", 3);
        pandaOptimizationEnabled = config.getBoolean("async-ai.panda-optimization.enabled", true);
        pandaTickInterval = config.getInt("async-ai.panda-optimization.tick-interval", 3);
        
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
        
        brainMemoryOptimizationEnabled = config.getBoolean("async-ai.brain-memory-optimization.enabled", false);
        poiSnapshotEnabled = config.getBoolean("async-ai.poi-snapshot.enabled", false);

        aiSensorOptimizationEnabled = config.getBoolean("async-ai.sensor-optimization.enabled", true);
        aiSensorRefreshInterval = config.getInt("async-ai.sensor-optimization.sensing-refresh-interval", 10);

        gameEventOptimizationEnabled = config.getBoolean("async-ai.game-event-optimization.enabled", true);
        gameEventEarlyFilter = config.getBoolean("async-ai.game-event-optimization.early-filter", true);
        gameEventThrottleLowPriority = config.getBoolean("async-ai.game-event-optimization.throttle-low-priority", true);
        gameEventThrottleIntervalMs = config.getLong("async-ai.game-event-optimization.throttle-interval-ms", 50L);
        gameEventDistanceFilter = config.getBoolean("async-ai.game-event-optimization.distance-filter", true);
        gameEventMaxDetectionDistance = config.getDouble("async-ai.game-event-optimization.max-detection-distance", 64.0);
        
        asyncPathfindingEnabled = config.getBoolean("async-ai.async-pathfinding.enabled", true);
        asyncPathfindingMaxThreads = config.getInt("async-ai.async-pathfinding.max-threads", 6);
        asyncPathfindingKeepAliveSeconds = config.getInt("async-ai.async-pathfinding.keep-alive-seconds", 60);
        asyncPathfindingMaxQueueSize = config.getInt("async-ai.async-pathfinding.max-queue-size", 500);
        asyncPathfindingTimeoutMs = config.getInt("async-ai.async-pathfinding.timeout-ms", 100);
        asyncPathfindingSyncFallbackEnabled = config.getBoolean("async-ai.async-pathfinding.sync-fallback-enabled", true);
        
        enhancedPathfindingEnabled = config.getBoolean("async-ai.async-pathfinding.enhanced.enabled", true);
        enhancedPathfindingMaxConcurrentRequests = config.getInt("async-ai.async-pathfinding.enhanced.max-concurrent-requests", 30);
        enhancedPathfindingMaxRequestsPerTick = config.getInt("async-ai.async-pathfinding.enhanced.max-requests-per-tick", 15);
        enhancedPathfindingHighPriorityDistance = config.getInt("async-ai.async-pathfinding.enhanced.priority.high-distance", 16);
        enhancedPathfindingMediumPriorityDistance = config.getInt("async-ai.async-pathfinding.enhanced.priority.medium-distance", 48);
        pathPrewarmEnabled = config.getBoolean("async-ai.async-pathfinding.enhanced.prewarm.enabled", true);
        pathPrewarmRadius = config.getInt("async-ai.async-pathfinding.enhanced.prewarm.radius", 32);
        pathPrewarmMaxMobsPerBatch = config.getInt("async-ai.async-pathfinding.enhanced.prewarm.max-mobs-per-batch", 5);
        pathPrewarmMaxPoisPerMob = config.getInt("async-ai.async-pathfinding.enhanced.prewarm.max-pois-per-mob", 3);
        
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
        hopperOptimizationEnabled = config.getBoolean("block-entity-optimizations.hopper-optimization.enabled", true);
        hopperCacheExpireTime = config.getInt("block-entity-optimizations.hopper-optimization.cache-expire-time", 100);
        minecartOptimizationEnabled = config.getBoolean("block-entity-optimizations.minecart-optimization.enabled", true);
        minecartTickInterval = config.getInt("block-entity-optimizations.minecart-optimization.tick-interval", 2);
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

        asyncLightingEnabled = config.getBoolean("lighting-optimizations.enabled", true);
        lightingThreadPoolSize = config.getInt("lighting-optimizations.async-lighting.thread-pool-size", 2);
        lightBatchThreshold = config.getInt("lighting-optimizations.async-lighting.batch-threshold", 16);
        lightUpdateIntervalMs = config.getInt("lighting-optimizations.update-interval-ms", 10);
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
        tntDebugEnabled = config.getBoolean("performance.debug-logging.modules.tnt", false);
        lightingDebugEnabled = config.getBoolean("performance.debug-logging.modules.lighting", false);
        landProtectionDebugEnabled = config.getBoolean("performance.debug-logging.modules.land-protection", false);
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
        endIslandDensityFixEnabled = config.getBoolean("end-island-density-fix.enabled", true);
        portalSuffocationCheckDisabled = config.getBoolean("portal-suffocation-check.disabled", false);
        shulkerBulletSelfHitFixEnabled = config.getBoolean("shulker-bullet-self-hit-fix.enabled", true);
        
        executeCommandInactiveSkipEnabled = config.getBoolean("datapack-optimization.execute-inactive-skip.enabled", false);
        executeCommandSkipLevel = config.getInt("datapack-optimization.execute-inactive-skip.skip-level", 2);
        executeCommandSimulationDistanceMultiplier = config.getDouble("datapack-optimization.execute-inactive-skip.simulation-distance-multiplier", 1.0);
        executeCommandCacheDurationMs = config.getLong("datapack-optimization.execute-inactive-skip.cache-duration-ms", 1000L);
        executeCommandWhitelistTypes = new java.util.HashSet<>(config.getStringList("datapack-optimization.execute-inactive-skip.whitelist-types"));
        executeCommandDebugEnabled = config.getBoolean("performance.debug-logging.modules.execute-command", false);
        
        commandDeduplicationEnabled = config.getBoolean("datapack-optimization.command-deduplication.enabled", true);
        commandDeduplicationDebugEnabled = config.getBoolean("performance.debug-logging.modules.command-deduplication", false);
        
        chunkTickAsyncEnabled = config.getBoolean("chunk-tick-async.enabled", false);
        chunkTickThreads = config.getInt("chunk-tick-async.threads", 4);
        chunkTickTimeoutMicros = config.getLong("chunk-tick-async.timeout-us", 200L);
        chunkTickAsyncBatchSize = config.getInt("chunk-tick-async.batch-size", 16);

        structureLocationAsyncEnabled = config.getBoolean("structure-location-async.enabled", true);
        structureLocationThreads = config.getInt("structure-location-async.threads", 3);
        locateCommandEnabled = config.getBoolean("structure-location-async.locate-command.enabled", true);
        locateCommandSearchRadius = config.getInt("structure-location-async.locate-command.search-radius", 100);
        locateCommandSkipKnownStructures = config.getBoolean("structure-location-async.locate-command.skip-known-structures", false);
        villagerTradeMapsEnabled = config.getBoolean("structure-location-async.villager-trade-maps.enabled", true);
        villagerTradeMapsSearchRadius = config.getInt("structure-location-async.villager-trade-maps.search-radius", 100);
        villagerTradeMapsSkipKnownStructures = config.getBoolean("structure-location-async.villager-trade-maps.skip-known-structures", false);
        villagerTradeMapTypes = new java.util.HashSet<>(config.getStringList("structure-location-async.villager-trade-maps.trade-types"));
        if (villagerTradeMapTypes.isEmpty()) {
            villagerTradeMapTypes.add("minecraft:ocean_monument_map");
            villagerTradeMapTypes.add("minecraft:woodland_mansion_map");
            villagerTradeMapTypes.add("minecraft:buried_treasure_map");
        }
        villagerMapGenerationTimeoutSeconds = config.getInt("structure-location-async.villager-trade-maps.generation-timeout-seconds", 30);
        dolphinTreasureHuntEnabled = config.getBoolean("structure-location-async.dolphin-treasure-hunt.enabled", true);
        dolphinTreasureSearchRadius = config.getInt("structure-location-async.dolphin-treasure-hunt.search-radius", 50);
        chestExplorationMapsEnabled = config.getBoolean("structure-location-async.chest-exploration-maps.enabled", true);
        chestExplorationLootTables = new java.util.HashSet<>(config.getStringList("structure-location-async.chest-exploration-maps.loot-tables"));
        if (chestExplorationLootTables.isEmpty()) {
            chestExplorationLootTables.add("minecraft:chests/shipwreck_map");
            chestExplorationLootTables.add("minecraft:chests/underwater_ruin_big");
            chestExplorationLootTables.add("minecraft:chests/underwater_ruin_small");
        }

        structureAlgorithmOptimizationEnabled = config.getBoolean("structure-location-async.algorithm-optimization.enabled", true);
        structureSearchPattern = config.getString("structure-location-async.algorithm-optimization.search-pattern", "hybrid");
        structureCachingEnabled = config.getBoolean("structure-location-async.algorithm-optimization.caching.enabled", true);
        structureCacheMaxSize = config.getInt("structure-location-async.algorithm-optimization.caching.max-size", 1000);
        structureCacheExpirationMinutes = config.getLong("structure-location-async.algorithm-optimization.caching.expiration-minutes", 30L);
        biomeAwareSearchEnabled = config.getBoolean("structure-location-async.algorithm-optimization.biome-aware-search.enabled", true);

        dataPackOptimizationEnabled = config.getBoolean("datapack-optimization.enabled", true);
        dataPackFileLoadThreads = config.getInt("datapack-optimization.file-load-threads", 4);
        dataPackZipProcessThreads = config.getInt("datapack-optimization.zip-process-threads", 2);
        dataPackBatchSize = config.getInt("datapack-optimization.batch-size", 100);
        dataPackCacheExpirationMinutes = config.getLong("datapack-optimization.cache-expiration-minutes", 30L);

        enableDebugLogging = config.getBoolean("performance.debug-logging", false);
        enablePerformanceMetrics = config.getBoolean("performance.enable-metrics", true);
        configVersion = config.getInt("version", 6);

        virtualEntityCompatibilityEnabled = config.getBoolean("virtual-entity-compatibility.enabled", true);
        virtualEntityBypassPacketQueue = config.getBoolean("virtual-entity-compatibility.bypass-packet-queue", true);
        virtualEntityExcludeFromThrottling = config.getBoolean("virtual-entity-compatibility.exclude-from-throttling", true);
        
        fancynpcsCompatEnabled = config.getBoolean("virtual-entity-compatibility.plugins.fancynpcs.enabled", true);
        fancynpcsUseAPI = config.getBoolean("virtual-entity-compatibility.plugins.fancynpcs.use-api", true);
        fancynpcsPriority = config.getInt("virtual-entity-compatibility.plugins.fancynpcs.priority", 90);
        
        znpcsplusCompatEnabled = config.getBoolean("virtual-entity-compatibility.plugins.znpcsplus.enabled", true);
        znpcsplusUseAPI = config.getBoolean("virtual-entity-compatibility.plugins.znpcsplus.use-api", false);
        znpcsplusPriority = config.getInt("virtual-entity-compatibility.plugins.znpcsplus.priority", 90);
        
        virtualEntityDetectionOrder = config.getStringList("virtual-entity-compatibility.detection-order");

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
        
        ConfigReloader.notifyReload(this);
        plugin.getLogger().info("Notified " + ConfigReloader.getListenerCount() + " config reload listeners");
    }

    @Deprecated
    public boolean isEntityTrackerEnabled() {
        return entityTrackerEnabled;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    @Deprecated
    public int getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }

    @Deprecated
    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public boolean isDebugLoggingEnabled() {
        return enableDebugLogging;
    }
    
    public boolean isSmartLagCompensationEnabled() {
        return smartLagCompensationEnabled;
    }
    
    public double getSmartLagTPSThreshold() {
        return smartLagTPSThreshold;
    }
    
    public boolean isSmartLagItemPickupDelayEnabled() {
        return smartLagItemPickupDelayEnabled;
    }
    
    public boolean isSmartLagPotionEffectsEnabled() {
        return smartLagPotionEffectsEnabled;
    }
    
    public boolean isSmartLagTimeAccelerationEnabled() {
        return smartLagTimeAccelerationEnabled;
    }
    
    public boolean isSmartLagDebugEnabled() {
        return smartLagDebugEnabled || enableDebugLogging;
    }
    
    public boolean isSmartLagLogMissedTicks() {
        return smartLagLogMissedTicks && isSmartLagDebugEnabled();
    }
    
    public boolean isSmartLagLogCompensation() {
        return smartLagLogCompensation && isSmartLagDebugEnabled();
    }
    
    public boolean isExperienceOrbInactiveTickEnabled() {
        return experienceOrbInactiveTickEnabled;
    }
    
    public double getExperienceOrbInactiveRange() {
        return experienceOrbInactiveRange;
    }
    
    public int getExperienceOrbInactiveMergeInterval() {
        return experienceOrbInactiveMergeInterval;
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
    
    private int mobSpawnInterval = 1;
    
    public int getMobSpawnInterval() {
        return mobSpawnInterval;
    }

    public boolean isBrainThrottleEnabled() { return brainThrottle; }
    public int getBrainThrottleInterval() { return brainThrottleInterval; }
    public boolean isLivingEntityTravelOptimizationEnabled() { return livingEntityTravelOptimizationEnabled; }
    public int getLivingEntityTravelSkipInterval() { return livingEntityTravelSkipInterval; }
    public boolean isBehaviorThrottleEnabled() { return behaviorThrottleEnabled; }
    public int getBehaviorThrottleInterval() { return behaviorThrottleInterval; }
    public boolean isMobDespawnOptimizationEnabled() { return mobDespawnOptimizationEnabled; }
    public int getMobDespawnCheckInterval() { return mobDespawnCheckInterval; }
    public long getAsyncAITimeoutMicros() { return asyncAITimeoutMicros; }
    
    public boolean isAiSpatialIndexEnabled() { return aiSpatialIndexEnabled; }
    public int getAiSpatialIndexGridSize() { return aiSpatialIndexGridSize; }
    public boolean isAiSpatialIndexAutoUpdate() { return aiSpatialIndexAutoUpdate; }
    public boolean isAiSpatialIndexPlayerIndexEnabled() { return aiSpatialIndexPlayerIndexEnabled; }
    public boolean isAiSpatialIndexPoiIndexEnabled() { return aiSpatialIndexPoiIndexEnabled; }
    public boolean isAiSpatialIndexStatisticsEnabled() { return aiSpatialIndexStatisticsEnabled; }
    public int getAiSpatialIndexLogIntervalSeconds() { return aiSpatialIndexLogIntervalSeconds; }
    
    public boolean isVillagerOptimizationEnabled() { return villagerOptimizationEnabled; }
    public boolean isVillagerUsePOISnapshot() { return villagerUsePOISnapshot; }
    public boolean isVillagerPoiCacheEnabled() { return villagerPoiCacheEnabled; }
    public int getVillagerPoiCacheExpireTime() { return villagerPoiCacheExpireTime; }
    public boolean isWanderingTraderOptimizationEnabled() { return wanderingTraderOptimizationEnabled; }
    public boolean isWardenOptimizationEnabled() { return wardenOptimizationEnabled; }
    public boolean isHoglinOptimizationEnabled() { return hoglinOptimizationEnabled; }
    public boolean isAllayOptimizationEnabled() { return allayOptimizationEnabled; }
    public boolean isEndermanOptimizationEnabled() { return endermanOptimizationEnabled; }
    public int getEndermanTickInterval() { return endermanTickInterval; }
    public boolean isEndermanAllowPickupBlocks() { return endermanAllowPickupBlocks; }
    public boolean isEndermanAllowPlaceBlocks() { return endermanAllowPlaceBlocks; }
    
    public boolean isEndermanBlockCarryLimiterEnabled() { return endermanBlockCarryLimiterEnabled; }
    public int getEndermanMaxCarrying() { return endermanMaxCarrying; }
    public boolean isEndermanCountTowardsMobCap() { return endermanCountTowardsMobCap; }
    public boolean isEndermanPreventPickup() { return endermanPreventPickup; }
    
    public boolean isMultithreadedEntityTrackerEnabled() { return multithreadedEntityTrackerEnabled; }
    
    public boolean isMultithreadedTrackerEnabled() { return multithreadedTrackerEnabled; }
    public int getMultithreadedTrackerParallelism() { return multithreadedTrackerParallelism; }
    public int getMultithreadedTrackerBatchSize() { return multithreadedTrackerBatchSize; }
    public int getMultithreadedTrackerAssistBatchSize() { return multithreadedTrackerAssistBatchSize; }
    
    public boolean isArmadilloOptimizationEnabled() { return armadilloOptimizationEnabled; }
    public int getArmadilloTickInterval() { return armadilloTickInterval; }
    public boolean isSnifferOptimizationEnabled() { return snifferOptimizationEnabled; }
    public int getSnifferTickInterval() { return snifferTickInterval; }
    public boolean isCamelOptimizationEnabled() { return camelOptimizationEnabled; }
    public int getCamelTickInterval() { return camelTickInterval; }
    public boolean isFrogOptimizationEnabled() { return frogOptimizationEnabled; }
    public int getFrogTickInterval() { return frogTickInterval; }
    public boolean isGoatOptimizationEnabled() { return goatOptimizationEnabled; }
    public int getGoatTickInterval() { return goatTickInterval; }
    public boolean isPandaOptimizationEnabled() { return pandaOptimizationEnabled; }
    public int getPandaTickInterval() { return pandaTickInterval; }
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
    
    public boolean isBrainMemoryOptimizationEnabled() { return brainMemoryOptimizationEnabled; }
    public boolean isPoiSnapshotEnabled() { return poiSnapshotEnabled; }
    
    public boolean isAiSensorOptimizationEnabled() { return aiSensorOptimizationEnabled; }
    public int getAiSensorRefreshInterval() { return aiSensorRefreshInterval; }

    public boolean isGameEventOptimizationEnabled() { return gameEventOptimizationEnabled; }
    public boolean isGameEventEarlyFilter() { return gameEventEarlyFilter; }
    public boolean isGameEventThrottleLowPriority() { return gameEventThrottleLowPriority; }
    public long getGameEventThrottleIntervalMs() { return gameEventThrottleIntervalMs; }
    public boolean isGameEventDistanceFilter() { return gameEventDistanceFilter; }
    public double getGameEventMaxDetectionDistance() { return gameEventMaxDetectionDistance; }
    
    public boolean isAsyncPathfindingEnabled() { return asyncPathfindingEnabled; }
    public int getAsyncPathfindingMaxThreads() { return threadPoolSize; }  
    public int getAsyncPathfindingKeepAliveSeconds() { return asyncPathfindingKeepAliveSeconds; }
    public int getAsyncPathfindingMaxQueueSize() { return asyncPathfindingMaxQueueSize; }
    public int getAsyncPathfindingTimeoutMs() { return asyncPathfindingTimeoutMs; }
    public boolean isAsyncPathfindingSyncFallbackEnabled() { return asyncPathfindingSyncFallbackEnabled; }
    
    public boolean isEnhancedPathfindingEnabled() { return enhancedPathfindingEnabled; }
    public int getEnhancedPathfindingMaxConcurrentRequests() { return enhancedPathfindingMaxConcurrentRequests; }
    public int getEnhancedPathfindingMaxRequestsPerTick() { return enhancedPathfindingMaxRequestsPerTick; }
    public int getEnhancedPathfindingHighPriorityDistance() { return enhancedPathfindingHighPriorityDistance; }
    public int getEnhancedPathfindingMediumPriorityDistance() { return enhancedPathfindingMediumPriorityDistance; }
    public boolean isPathPrewarmEnabled() { return pathPrewarmEnabled; }
    public int getPathPrewarmRadius() { return pathPrewarmRadius; }
    public int getPathPrewarmMaxMobsPerBatch() { return pathPrewarmMaxMobsPerBatch; }
    public int getPathPrewarmMaxPoisPerMob() { return pathPrewarmMaxPoisPerMob; }
    public boolean isAsyncPathfindingCacheEnabled() { return asyncPathfindingCacheEnabled; }
    public int getAsyncPathfindingCacheMaxSize() { return asyncPathfindingCacheMaxSize; }
    public int getAsyncPathfindingCacheExpireSeconds() { return asyncPathfindingCacheExpireSeconds; }
    public int getAsyncPathfindingCacheReuseTolerance() { return asyncPathfindingCacheReuseTolerance; }
    public int getAsyncPathfindingCacheCleanupIntervalSeconds() { return asyncPathfindingCacheCleanupIntervalSeconds; }
    public boolean isCollisionOptimizationEnabled() { return collisionOptimizationEnabled; }
    public boolean isCollisionAggressiveMode() { return collisionAggressiveMode; }
    public java.util.Set<String> getCollisionExcludedEntities() { return collisionExcludedEntities; }
    public boolean isCollisionCacheEnabled() { return collisionCacheEnabled; }
    public int getCollisionCacheLifetimeMs() { return collisionCacheLifetimeMs; }
    public double getCollisionCacheMovementThreshold() { return collisionCacheMovementThreshold; }
    public boolean isCollisionSpatialPartitionEnabled() { return collisionSpatialPartitionEnabled; }
    public int getCollisionSpatialGridSize() { return collisionSpatialGridSize; }
    public int getCollisionSpatialDensityThreshold() { return collisionSpatialDensityThreshold; }
    public int getCollisionSpatialUpdateIntervalMs() { return collisionSpatialUpdateIntervalMs; }
    public double getCollisionSkipMinMovement() { return collisionSkipMinMovement; }
    public int getCollisionSkipCheckIntervalMs() { return collisionSkipCheckIntervalMs; }
    public boolean isPushOptimizationEnabled() { return pushOptimizationEnabled; }
    public double getPushMaxPushPerTick() { return pushMaxPushPerTick; }
    public double getPushDampingFactor() { return pushDampingFactor; }
    public int getPushHighDensityThreshold() { return pushHighDensityThreshold; }
    public double getPushHighDensityMultiplier() { return pushHighDensityMultiplier; }
    public boolean isAdvancedCollisionOptimizationEnabled() { return advancedCollisionOptimizationEnabled; }
    public int getCollisionThreshold() { return collisionThreshold; }
    public float getSuffocationDamage() { return suffocationDamage; }
    public int getMaxPushIterations() { return maxPushIterations; }
    public boolean isVectorizedCollisionEnabled() { return vectorizedCollisionEnabled; }
    public int getVectorizedCollisionThreshold() { return vectorizedCollisionThreshold; }
    public boolean isCollisionBlockCacheEnabled() { return collisionBlockCacheEnabled; }
    public int getCollisionBlockCacheSize() { return collisionBlockCacheSize; }
    public int getCollisionBlockCacheExpireTicks() { return collisionBlockCacheExpireTicks; }
    public boolean isRayCollisionEnabled() { return rayCollisionEnabled; }
    public double getRayCollisionMaxDistance() { return rayCollisionMaxDistance; }
    public boolean isUseSectionGrid() { return useSectionGrid; }
    public boolean isSectionGridWarmup() { return sectionGridWarmup; }
    public boolean isShapeOptimizationEnabled() { return shapeOptimizationEnabled; }
    public boolean isShapePrecomputeArrays() { return shapePrecomputeArrays; }
    public boolean isShapeBlockShapeCache() { return shapeBlockShapeCache; }
    public int getShapeBlockShapeCacheSize() { return shapeBlockShapeCacheSize; }
    public boolean isTntCacheEnabled() { return tntCacheEnabled; }
    public boolean isTntUseOptimizedCache() { return tntUseOptimizedCache; }
    public int getTntCacheExpiryTicks() { return tntCacheExpiryTicks; }
    public boolean isTntCacheWarmupEnabled() { return tntCacheWarmupEnabled; }
    public boolean isTntPrecomputedShapeEnabled() { return tntPrecomputedShapeEnabled; }
    public boolean isTntUseOcclusionDetection() { return tntUseOcclusionDetection; }
    public double getTntOcclusionThreshold() { return tntOcclusionThreshold; }
    public boolean isTntBatchCollisionEnabled() { return tntBatchCollisionEnabled; }
    public int getTntBatchUnrollFactor() { return tntBatchUnrollFactor; }
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
    public boolean isHopperOptimizationEnabled() { return hopperOptimizationEnabled; }
    public int getHopperCacheExpireTime() { return hopperCacheExpireTime; }
    public boolean isMinecartOptimizationEnabled() { return minecartOptimizationEnabled; }
    public int getMinecartTickInterval() { return minecartTickInterval; }
    public boolean isSimpleEntitiesOptimizationEnabled() { return simpleEntitiesOptimizationEnabled; }
    public boolean isSimpleEntitiesUsePOISnapshot() { return simpleEntitiesUsePOISnapshot; }
    public boolean isEntityTickParallel() { return entityTickParallel; }
    public int getEntityTickThreads() { return threadPoolSize; }  
    public int getMinEntitiesForParallel() { return minEntitiesForParallel; }
    public int getEntityTickBatchSize() { return entityTickBatchSize; }
    public boolean isAsyncLightingEnabled() { return asyncLightingEnabled; }
    public int getLightingThreadPoolSize() { return lightingThreadPoolSize; }
    public int getLightBatchThreshold() { return lightBatchThreshold; }
    public int getLightUpdateIntervalMs() { return lightUpdateIntervalMs; }
    public boolean useLayeredPropagationQueue() { return useLayeredPropagationQueue; }
    public int getMaxLightPropagationDistance() { return maxLightPropagationDistance; }
    public boolean isSkylightCacheEnabled() { return skylightCacheEnabled; }
    public int getSkylightCacheDurationMs() { return skylightCacheDurationMs; }
    public boolean isLightDeduplicationEnabled() { return lightDeduplicationEnabled; }
    public boolean isDynamicBatchAdjustmentEnabled() { return dynamicBatchAdjustmentEnabled; }
    public boolean isAdvancedLightingStatsEnabled() { return advancedLightingStatsEnabled; }
    public boolean isLightingDebugEnabled() { return enableDebugLogging && lightingDebugEnabled; }
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
    
    public boolean isUsePandaWireAlgorithm() { return usePandaWireAlgorithm; }
    public boolean isRedstoneNetworkCacheEnabled() { return redstoneNetworkCacheEnabled; }
    public int getRedstoneNetworkCacheExpireTicks() { return redstoneNetworkCacheExpireTicks; }
    public boolean isTNTUseSakuraDensityCache() { return tntUseSakuraDensityCache; }
    public boolean isTNTUseVectorizedAABB() { return tntUseVectorizedAABB; }
    public boolean isTNTMergeEnabled() { return tntMergeEnabled; }
    public double getTNTMergeRadius() { return tntMergeRadius; }
    public int getTNTMaxFuseDifference() { return tntMaxFuseDifference; }
    public float getTNTMergedPowerMultiplier() { return tntMergedPowerMultiplier; }
    
    public boolean isAsyncVillagerBreedEnabled() { return asyncVillagerBreedEnabled; }
    public boolean isVillagerAgeThrottleEnabled() { return villagerAgeThrottleEnabled; }
    public int getVillagerBreedThreads() { return threadPoolSize; }  
    public int getVillagerBreedCheckInterval() { return villagerBreedCheckInterval; }
    public boolean isTNTOptimizationEnabled() { return tntOptimizationEnabled; }
    public java.util.Set<String> getTNTExplosionEntities() { return tntExplosionEntities; }
    public int getTNTThreads() { return threadPoolSize; }  
    public int getTNTMaxBlocks() { return tntMaxBlocks; }
    public long getTNTTimeoutMicros() { return tntTimeoutMicros; }
    public int getTNTBatchSize() { return tntBatchSize; }
    public boolean isTNTDebugEnabled() { return tntDebugEnabled || enableDebugLogging; }
    public boolean isLandProtectionDebugEnabled() { return landProtectionDebugEnabled || enableDebugLogging; }
    public boolean isTNTVanillaCompatibilityEnabled() { return tntVanillaCompatibilityEnabled; }
    public boolean isTNTUseVanillaPower() { return tntUseVanillaPower; }
    public boolean isTNTUseVanillaFireLogic() { return tntUseVanillaFireLogic; }
    public boolean isTNTUseVanillaDamageCalculation() { return tntUseVanillaDamageCalculation; }
    public boolean isTNTUseFullRaycast() { return tntUseFullRaycast; }
    public boolean isBeeFixEnabled() { return beeFixEnabled; }
    public boolean isEndIslandDensityFixEnabled() { return endIslandDensityFixEnabled; }
    public boolean isPortalSuffocationCheckDisabled() { return portalSuffocationCheckDisabled; }
    public boolean isShulkerBulletSelfHitFixEnabled() { return shulkerBulletSelfHitFixEnabled; }
    
    public boolean isExecuteCommandInactiveSkipEnabled() { return executeCommandInactiveSkipEnabled; }
    public int getExecuteCommandSkipLevel() { return executeCommandSkipLevel; }
    public double getExecuteCommandSimulationDistanceMultiplier() { return executeCommandSimulationDistanceMultiplier; }
    public long getExecuteCommandCacheDurationMs() { return executeCommandCacheDurationMs; }
    public Set<String> getExecuteCommandWhitelistTypes() { return executeCommandWhitelistTypes; }
    public boolean isExecuteCommandDebugEnabled() { return executeCommandDebugEnabled; }
    
    public boolean isCommandDeduplicationEnabled() { return commandDeduplicationEnabled; }
    public boolean isCommandDeduplicationDebugEnabled() { return commandDeduplicationDebugEnabled; }
    
    public boolean isTNTUseVanillaBlockDestruction() { return tntUseVanillaBlockDestruction; }
    public boolean isTNTUseVanillaDrops() { return tntUseVanillaDrops; }
    public boolean isTNTLandProtectionEnabled() { return tntLandProtectionEnabled; }
    public boolean isBlockLockerProtectionEnabled() { return blockLockerProtectionEnabled; }
    public boolean isChunkTickAsyncEnabled() { return chunkTickAsyncEnabled; }
    public int getChunkTickThreads() { return threadPoolSize; }  
    public long getChunkTickTimeoutMicros() { return chunkTickTimeoutMicros; }
    public int getChunkTickAsyncBatchSize() { return chunkTickAsyncBatchSize; }

    public int getConfigVersion() {
        return configVersion;
    }

    public boolean isStructureLocationAsyncEnabled() { return structureLocationAsyncEnabled; }
    public int getStructureLocationThreads() { return threadPoolSize; }  
    public boolean isLocateCommandEnabled() { return locateCommandEnabled; }
    public int getLocateCommandSearchRadius() { return locateCommandSearchRadius; }
    public boolean isLocateCommandSkipKnownStructures() { return locateCommandSkipKnownStructures; }
    public boolean isVillagerTradeMapsEnabled() { return villagerTradeMapsEnabled; }
    public int getVillagerTradeMapsSearchRadius() { return villagerTradeMapsSearchRadius; }
    public boolean isVillagerTradeMapsSkipKnownStructures() { return villagerTradeMapsSkipKnownStructures; }
    public java.util.Set<String> getVillagerTradeMapTypes() { return villagerTradeMapTypes; }
    public int getVillagerMapGenerationTimeoutSeconds() { return villagerMapGenerationTimeoutSeconds; }
    public boolean isDolphinTreasureHuntEnabled() { return dolphinTreasureHuntEnabled; }
    public int getDolphinTreasureSearchRadius() { return dolphinTreasureSearchRadius; }
    public boolean isChestExplorationMapsEnabled() { return chestExplorationMapsEnabled; }
    public java.util.Set<String> getChestExplorationLootTables() { return chestExplorationLootTables; }
    public boolean isStructureLocationDebugEnabled() { return enableDebugLogging; }

    public boolean isStructureAlgorithmOptimizationEnabled() { return structureAlgorithmOptimizationEnabled; }
    public String getStructureSearchPattern() { return structureSearchPattern; }
    public boolean isStructureCachingEnabled() { return structureCachingEnabled; }
    public boolean isBiomeAwareSearchEnabled() { return biomeAwareSearchEnabled; }
    public int getStructureCacheMaxSize() { return structureCacheMaxSize; }
    public long getStructureCacheExpirationMinutes() { return structureCacheExpirationMinutes; }

    public boolean isDataPackOptimizationEnabled() { return dataPackOptimizationEnabled; }
    public int getDataPackFileLoadThreads() { return threadPoolSize; }  
    public int getDataPackZipProcessThreads() { return threadPoolSize; }  
    public int getDataPackBatchSize() { return dataPackBatchSize; }
    public long getDataPackCacheExpirationMinutes() { return dataPackCacheExpirationMinutes; }
    public int getDataPackMaxFileCacheSize() { return dataPackMaxFileCacheSize; }
    public int getDataPackMaxFileSystemCacheSize() { return dataPackMaxFileSystemCacheSize; }
    public boolean isDataPackDebugEnabled() { return enableDebugLogging; }

    public boolean isNitoriOptimizationsEnabled() { return nitoriOptimizationsEnabled; }
    public boolean isVirtualThreadEnabled() { return virtualThreadEnabled; }
    public boolean isWorkStealingEnabled() { return workStealingEnabled; }
    public boolean isBlockPosCacheEnabled() { return blockPosCacheEnabled; }
    public boolean isOptimizedCollectionsEnabled() { return optimizedCollectionsEnabled; }

    public String getSeedEncryptionScheme() { return seedEncryptionScheme; }
    public boolean isSeedEncryptionEnabled() { return seedEncryptionEnabled; }
    public boolean isSeedEncryptionProtectStructures() { return seedEncryptionProtectStructures; }
    public boolean isSeedEncryptionProtectOres() { return seedEncryptionProtectOres; }
    public boolean isSeedEncryptionProtectSlimes() { return seedEncryptionProtectSlimes; }
    public boolean isSeedEncryptionProtectBiomes() { return seedEncryptionProtectBiomes; }
    
    public boolean isSecureSeedEnabled() { return seedEncryptionEnabled; }
    public boolean isSecureSeedProtectStructures() { return seedEncryptionProtectStructures; }
    public boolean isSecureSeedProtectOres() { return seedEncryptionProtectOres; }
    public boolean isSecureSeedProtectSlimes() { return seedEncryptionProtectSlimes; }
    public int getSecureSeedBits() { return secureSeedBits; }
    public boolean isSecureSeedDebugLogging() { return enableDebugLogging; }
    
    public boolean isQuantumSeedEnabled() { return seedEncryptionEnabled && "quantum".equalsIgnoreCase(seedEncryptionScheme); }
    public int getQuantumSeedEncryptionLevel() { return quantumSeedEncryptionLevel; }
    public String getQuantumSeedPrivateKeyFile() { return quantumSeedPrivateKeyFile; }
    public boolean isQuantumSeedEnableTimeDecay() { return quantumSeedEnableTimeDecay; }
    public int getQuantumSeedCacheSize() { return quantumSeedCacheSize; }
    public boolean isQuantumSeedDebugLogging() { return enableDebugLogging; }
    
    public boolean isSeedCommandRestrictionEnabled() {
        return config.getBoolean("seed-encryption.restrict-seed-command", true);
    }
    
    public boolean isSeedProtectionEnabled() {
        return config.getBoolean("seed-encryption.anti-plugin-theft.enabled", true);
    }
    
    public boolean shouldReturnFakeSeed() {
        return config.getBoolean("seed-encryption.anti-plugin-theft.return-fake-seed", true);
    }
    
    public long getFakeSeedValue() {
        return config.getLong("seed-encryption.anti-plugin-theft.fake-seed-value", 0L);
    }

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

    public boolean isItemEntityMergeOptimizationEnabled() { return itemEntityMergeOptimizationEnabled; }
    public boolean isItemEntityCancelVanillaMerge() { return itemEntityCancelVanillaMerge; }
    public int getItemEntityMergeInterval() { return itemEntityMergeInterval; }
    public int getItemEntityMinNearbyItems() { return itemEntityMinNearbyItems; }
    public double getItemEntityMergeRange() { return itemEntityMergeRange; }
    public boolean isItemEntityAgeOptimizationEnabled() { return itemEntityAgeOptimizationEnabled; }
    public int getItemEntityAgeInterval() { return itemEntityAgeInterval; }
    public double getItemEntityPlayerDetectionRange() { return itemEntityPlayerDetectionRange; }
    public boolean isItemEntityInactiveTickEnabled() { return itemEntityInactiveTickEnabled; }
    public double getItemEntityInactiveRange() { return itemEntityInactiveRange; }
    public int getItemEntityInactiveMergeInterval() { return itemEntityInactiveMergeInterval; }

    public boolean isEntityPacketThrottleEnabled() { return entityPacketThrottleEnabled; }
    public boolean isEntityDataThrottleEnabled() { return entityDataThrottleEnabled; }
    public boolean isChunkVisibilityFilterEnabled() { return chunkVisibilityFilterEnabled; }
    
    public boolean isSuffocationOptimizationEnabled() { return suffocationOptimizationEnabled; }
    public boolean isFastRayTraceEnabled() { return fastRayTraceEnabled; }
    public boolean isMapRenderingOptimizationEnabled() { return mapRenderingOptimizationEnabled; }
    public int getMapRenderingThreads() { return threadPoolSize; }  

    public boolean isFastMovementChunkLoadEnabled() { return fastMovementChunkLoadEnabled; }
    public double getFastMovementSpeedThreshold() { return fastMovementSpeedThreshold; }
    public int getFastMovementPreloadDistance() { return fastMovementPreloadDistance; }
    public int getFastMovementMaxConcurrentLoads() { return fastMovementMaxConcurrentLoads; }
    public int getFastMovementPredictionTicks() { return fastMovementPredictionTicks; }

    public boolean isCenterOffsetEnabled() { return centerOffsetEnabled; }
    public double getMinOffsetSpeed() { return minOffsetSpeed; }
    public double getMaxOffsetSpeed() { return maxOffsetSpeed; }
    public double getMaxOffsetRatio() { return maxOffsetRatio; }
    
    public boolean isPlayerJoinWarmupEnabled() { return playerJoinWarmupEnabled; }
    public long getPlayerJoinWarmupDurationMs() { return playerJoinWarmupDurationMs; }
    public double getPlayerJoinWarmupInitialRate() { return playerJoinWarmupInitialRate; }
    public int getAsyncLoadingBatchSize() { return asyncLoadingBatchSize; }
    public long getAsyncLoadingBatchDelayMs() { return asyncLoadingBatchDelayMs; }

    public boolean getBoolean(String path, boolean defaultValue) {
        return config != null ? config.getBoolean(path, defaultValue) : defaultValue;
    }

    public boolean isVirtualEntityCompatibilityEnabled() { return virtualEntityCompatibilityEnabled; }
    public boolean isVirtualEntityBypassPacketQueue() { return virtualEntityBypassPacketQueue; }
    public boolean isVirtualEntityExcludeFromThrottling() { return virtualEntityExcludeFromThrottling; }
    public boolean isVirtualEntityDebugEnabled() { return enableDebugLogging; }
    
    public boolean isFancynpcsCompatEnabled() { return fancynpcsCompatEnabled; }
    public boolean isFancynpcsUseAPI() { return fancynpcsUseAPI; }
    public int getFancynpcsPriority() { return fancynpcsPriority; }
    
    public boolean isZnpcsplusCompatEnabled() { return znpcsplusCompatEnabled; }
    public boolean isZnpcsplusUseAPI() { return znpcsplusUseAPI; }
    public int getZnpcsplusPriority() { return znpcsplusPriority; }
    
    public java.util.List<String> getVirtualEntityDetectionOrder() { return virtualEntityDetectionOrder; }

    private void loadPathfindingAndCollisionConfigs() {
        asyncPathfindingCacheEnabled = config.getBoolean("async-ai.async-pathfinding.cache.enabled", true);
        asyncPathfindingCacheMaxSize = config.getInt("async-ai.async-pathfinding.cache.max-size", 1000);
        asyncPathfindingCacheExpireSeconds = config.getInt("async-ai.async-pathfinding.cache.expire-seconds", 30);
        asyncPathfindingCacheReuseTolerance = config.getInt("async-ai.async-pathfinding.cache.reuse-tolerance", 3);
        asyncPathfindingCacheCleanupIntervalSeconds = config.getInt("async-ai.async-pathfinding.cache.cleanup-interval-seconds", 5);
        collisionOptimizationEnabled = config.getBoolean("collision-optimization.enabled", true);
        collisionAggressiveMode = config.getBoolean("collision-optimization.aggressive-mode", true);
        collisionExclusionListFile = config.getString("collision-optimization.exclusion-list-file", "entities.yml");
        collisionExcludedEntities = loadEntitiesFromFile(collisionExclusionListFile, "collision-optimization-excluded-entities");
        collisionCacheEnabled = config.getBoolean("collision-optimization.cache.enabled", true);
        collisionCacheLifetimeMs = config.getInt("collision-optimization.cache.lifetime-ms", 50);
        collisionCacheMovementThreshold = config.getDouble("collision-optimization.cache.movement-threshold", 0.01);
        collisionSpatialPartitionEnabled = config.getBoolean("collision-optimization.spatial-partition.enabled", true);
        collisionSpatialGridSize = config.getInt("collision-optimization.spatial-partition.grid-size", 4);
        collisionSpatialDensityThreshold = config.getInt("collision-optimization.spatial-partition.density-threshold", 50);
        collisionSpatialUpdateIntervalMs = config.getInt("collision-optimization.spatial-partition.update-interval-ms", 100);
        collisionSkipMinMovement = config.getDouble("collision-optimization.skip-checks.min-movement", 0.001);
        collisionSkipCheckIntervalMs = config.getInt("collision-optimization.skip-checks.check-interval-ms", 50);
        pushOptimizationEnabled = config.getBoolean("collision-optimization.push-optimization.enabled", true);
        pushMaxPushPerTick = config.getDouble("collision-optimization.push-optimization.max-push-per-tick", 0.5);
        pushDampingFactor = config.getDouble("collision-optimization.push-optimization.damping-factor", 0.7);
        pushHighDensityThreshold = config.getInt("collision-optimization.push-optimization.high-density-threshold", 10);
        pushHighDensityMultiplier = config.getDouble("collision-optimization.push-optimization.high-density-multiplier", 0.3);
        advancedCollisionOptimizationEnabled = config.getBoolean("collision-optimization.advanced.enabled", true);
        collisionThreshold = config.getInt("collision-optimization.advanced.collision-threshold", 8);
        suffocationDamage = (float) config.getDouble("collision-optimization.advanced.suffocation-damage", 0.5);
        maxPushIterations = config.getInt("collision-optimization.advanced.max-push-iterations", 8);
        vectorizedCollisionEnabled = config.getBoolean("collision-optimization.vectorized.enabled", true);
        vectorizedCollisionThreshold = config.getInt("collision-optimization.vectorized.threshold", 64);
        collisionBlockCacheEnabled = config.getBoolean("collision-optimization.block-cache.enabled", true);
        collisionBlockCacheSize = config.getInt("collision-optimization.block-cache.cache-size", 512);
        collisionBlockCacheExpireTicks = config.getInt("collision-optimization.block-cache.expire-ticks", 600);
        rayCollisionEnabled = config.getBoolean("collision-optimization.ray-collision.enabled", true);
        rayCollisionMaxDistance = config.getDouble("collision-optimization.ray-collision.max-distance", 64.0);
        useSectionGrid = config.getBoolean("collision-optimization.advanced.use-section-grid", true);
        sectionGridWarmup = config.getBoolean("collision-optimization.advanced.section-grid-warmup", false);
        shapeOptimizationEnabled = config.getBoolean("collision-optimization.shape-optimization.enabled", true);
        shapePrecomputeArrays = config.getBoolean("collision-optimization.shape-optimization.precompute-arrays", true);
        shapeBlockShapeCache = config.getBoolean("collision-optimization.shape-optimization.block-shape-cache", true);
        shapeBlockShapeCacheSize = config.getInt("collision-optimization.shape-optimization.cache-size", 512);
        tntCacheEnabled = config.getBoolean("tnt-explosion-optimization.cache.enabled", true);
        tntUseOptimizedCache = config.getBoolean("tnt-explosion-optimization.cache.use-optimized-cache", true);
        tntCacheExpiryTicks = config.getInt("tnt-explosion-optimization.cache.cache-expiry-ticks", 600);
        tntCacheWarmupEnabled = config.getBoolean("tnt-explosion-optimization.cache.warmup-enabled", false);
        tntPrecomputedShapeEnabled = config.getBoolean("tnt-explosion-optimization.precomputed-shape.enabled", true);
        tntUseOcclusionDetection = config.getBoolean("tnt-explosion-optimization.precomputed-shape.use-occlusion-detection", true);
        tntOcclusionThreshold = config.getDouble("tnt-explosion-optimization.precomputed-shape.occlusion-threshold", 0.5);
        tntBatchCollisionEnabled = config.getBoolean("tnt-explosion-optimization.batch-collision.enabled", true);
        tntBatchUnrollFactor = config.getInt("tnt-explosion-optimization.batch-collision.unroll-factor", 4);
    }
    
    public String getLightingThreadPoolMode() { return lightingThreadPoolMode; }
    public String getLightingThreadPoolCalculation() { return lightingThreadPoolCalculation; }
    public int getLightingMinThreads() { return lightingMinThreads; }
    public int getLightingMaxThreads() { return lightingMaxThreads; }
    public int getLightingBatchThresholdMax() { return lightingBatchThresholdMax; }
    public boolean isLightingAggressiveBatching() { return lightingAggressiveBatching; }
    
    public boolean isLightingPrioritySchedulingEnabled() { return lightingPrioritySchedulingEnabled; }
    public int getLightingHighPriorityRadius() { return lightingHighPriorityRadius; }
    public int getLightingMediumPriorityRadius() { return lightingMediumPriorityRadius; }
    public int getLightingLowPriorityRadius() { return lightingLowPriorityRadius; }
    public long getLightingMaxLowPriorityDelay() { return lightingMaxLowPriorityDelay; }
    
    public boolean isLightingDebouncingEnabled() { return lightingDebouncingEnabled; }
    public long getLightingDebounceDelay() { return lightingDebounceDelay; }
    public int getLightingMaxUpdatesPerSecond() { return lightingMaxUpdatesPerSecond; }
    public long getLightingResetOnStableMs() { return lightingResetOnStableMs; }
    
    public boolean isLightingMergingEnabled() { return lightingMergingEnabled; }
    public int getLightingMergeRadius() { return lightingMergeRadius; }
    public long getLightingMergeDelay() { return lightingMergeDelay; }
    public int getLightingMaxMergedUpdates() { return lightingMaxMergedUpdates; }
    
    public boolean isLightingChunkBorderEnabled() { return lightingChunkBorderEnabled; }
    public boolean isLightingBatchBorderUpdates() { return lightingBatchBorderUpdates; }
    public long getLightingBorderUpdateDelay() { return lightingBorderUpdateDelay; }
    public int getLightingCrossChunkBatchSize() { return lightingCrossChunkBatchSize; }
    
    public boolean isLightingAdaptiveEnabled() { return lightingAdaptiveEnabled; }
    public int getLightingMonitorInterval() { return lightingMonitorInterval; }
    public boolean isLightingAutoAdjustThreads() { return lightingAutoAdjustThreads; }
    public boolean isLightingAutoAdjustBatchSize() { return lightingAutoAdjustBatchSize; }
    public int getLightingTargetQueueSize() { return lightingTargetQueueSize; }
    public int getLightingTargetLatency() { return lightingTargetLatency; }
    
    public boolean isLightingChunkUnloadEnabled() { return lightingChunkUnloadEnabled; }
    public boolean isLightingAsyncCleanup() { return lightingAsyncCleanup; }
    public int getLightingCleanupBatchSize() { return lightingCleanupBatchSize; }
    public long getLightingCleanupDelay() { return lightingCleanupDelay; }
    
    public boolean isNoiseOptimizationEnabled() { return noiseOptimizationEnabled; }
    public boolean isJigsawOptimizationEnabled() { return jigsawOptimizationEnabled; }
}
