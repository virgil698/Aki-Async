package org.virgil.akiasync.config;

import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.configuration.file.FileConfiguration;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.sections.*;
import org.virgil.akiasync.util.concurrency.ConfigReloader;

public class ConfigManager {

    private static final int CURRENT_CONFIG_VERSION = 20;

    private final AkiAsyncPlugin plugin;
    private FileConfiguration config;

    private final NetworkConfig networkConfig = new NetworkConfig();
    private final EntityConfig entityConfig = new EntityConfig();
    private final AIConfig aiConfig = new AIConfig();
    private final LightingConfig lightingConfig = new LightingConfig();
    private final ChunkConfig chunkConfig = new ChunkConfig();
    private final TNTConfig tntConfig = new TNTConfig();
    private final CollisionConfig collisionConfig = new CollisionConfig();
    private final PerformanceConfig performanceConfig = new PerformanceConfig();
    private final CompatibilityConfig compatibilityConfig = new CompatibilityConfig();
    private final MiscConfig miscConfig = new MiscConfig();

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
        int cpuCores = Runtime.getRuntime().availableProcessors();

        networkConfig.load(config);
        entityConfig.load(config, cpuCores);
        aiConfig.load(config);
        lightingConfig.load(config);
        chunkConfig.load(config);
        tntConfig.load(config);
        collisionConfig.load(config);
        performanceConfig.load(config);
        compatibilityConfig.load(config);
        miscConfig.load(config);

        collisionConfig.setCollisionExcludedEntities(
            loadEntitiesFromFile(collisionConfig.getCollisionExclusionListFile(), "collision-optimization-excluded-entities")
        );
        miscConfig.setZeroDelayFactoryEntities(
            loadEntitiesFromFile(miscConfig.getZeroDelayFactoryEntitiesConfigFile(), "zero-delay-factory-entities")
        );

        validateConfigVersion();
        validateConfig();
    }

    private void validateConfigVersion() {
        if (miscConfig.getConfigVersion() != CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("==========================================");
            plugin.getLogger().warning("  CONFIG VERSION MISMATCH DETECTED");
            plugin.getLogger().warning("==========================================");
            plugin.getLogger().warning("Current supported version: " + CURRENT_CONFIG_VERSION);
            plugin.getLogger().warning("Your config version: " + miscConfig.getConfigVersion());
            plugin.getLogger().warning("");

            if (miscConfig.getConfigVersion() < CURRENT_CONFIG_VERSION) {
                plugin.getLogger().warning("Your config.yml is outdated!");
                plugin.getLogger().info("Attempting smart migration to preserve your settings...");
            } else {
                plugin.getLogger().warning("Your config.yml is from a newer version!");
                plugin.getLogger().info("Attempting smart migration...");
            }

            ConfigMigrator migrator = new ConfigMigrator(plugin.getDataFolder(), plugin.getLogger());
            migrator.registerVersionMigrations();

            if (migrator.migrate(miscConfig.getConfigVersion(), CURRENT_CONFIG_VERSION)) {
                plugin.getLogger().info("==========================================");
                plugin.getLogger().info("  CONFIG MIGRATION SUCCESSFUL!");
                plugin.getLogger().info("==========================================");
                plugin.getLogger().info("Your settings have been preserved and migrated.");
                plugin.getLogger().info("Check migration-report-*.txt for details.");
                plugin.getLogger().info("==========================================");

                reloadConfigWithoutValidation();
            } else {

                plugin.getLogger().warning("Smart migration failed, using fallback method...");
                if (backupAndRegenerateConfig()) {
                    plugin.getLogger().info("Config backup and regeneration completed!");
                    plugin.getLogger().info("Old config saved as: config.yml.bak");
                    plugin.getLogger().warning("Please manually review and adjust settings.");
                    plugin.getLogger().warning("==========================================");

                    reloadConfigWithoutValidation();
                } else {
                    plugin.getLogger().severe("Failed to backup and regenerate config!");
                    plugin.getLogger().severe("Please manually update your config.yml");
                    plugin.getLogger().warning("==========================================");
                }
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

        int cpuCores = Runtime.getRuntime().availableProcessors();
        networkConfig.load(config);
        entityConfig.load(config, cpuCores);
        aiConfig.load(config);
        lightingConfig.load(config);
        chunkConfig.load(config);
        tntConfig.load(config);
        collisionConfig.load(config);
        performanceConfig.load(config);
        compatibilityConfig.load(config);
        miscConfig.load(config);

        collisionConfig.setCollisionExcludedEntities(
            loadEntitiesFromFile(collisionConfig.getCollisionExclusionListFile(), "collision-optimization-excluded-entities")
        );
        miscConfig.setZeroDelayFactoryEntities(
            loadEntitiesFromFile(miscConfig.getZeroDelayFactoryEntitiesConfigFile(), "zero-delay-factory-entities")
        );

        validateConfig();
    }

    private void validateConfig() {
        performanceConfig.validate(plugin.getLogger());
        entityConfig.validate(plugin.getLogger());
        lightingConfig.validate(plugin.getLogger());
        chunkConfig.validate(plugin.getLogger());
        tntConfig.validate(plugin.getLogger());
        miscConfig.validate(plugin.getLogger());
        performanceConfig.validateNitori(plugin.getLogger());
    }

    public void reload() {
        loadConfig();
        plugin.getLogger().info("Configuration reloaded successfully!");

        ConfigReloader.notifyReload(this);
        plugin.getLogger().info("Notified " + ConfigReloader.getListenerCount() + " config reload listeners");
    }

    public int getThreadPoolSize() { return performanceConfig.getThreadPoolSize(); }
    public boolean isDebugLoggingEnabled() { return performanceConfig.isDebugLoggingEnabled(); }
    public boolean isSmartLagCompensationEnabled() { return performanceConfig.isSmartLagCompensationEnabled(); }
    public double getSmartLagTPSThreshold() { return performanceConfig.getSmartLagTPSThreshold(); }
    public boolean isSmartLagItemPickupDelayEnabled() { return performanceConfig.isSmartLagItemPickupDelayEnabled(); }
    public boolean isSmartLagPotionEffectsEnabled() { return performanceConfig.isSmartLagPotionEffectsEnabled(); }
    public boolean isSmartLagTimeAccelerationEnabled() { return performanceConfig.isSmartLagTimeAccelerationEnabled(); }
    public boolean isSmartLagDebugEnabled() { return performanceConfig.isSmartLagDebugEnabled(); }
    public boolean isSmartLagLogMissedTicks() { return performanceConfig.isSmartLagLogMissedTicks(); }
    public boolean isSmartLagLogCompensation() { return performanceConfig.isSmartLagLogCompensation(); }

    public boolean isExperienceOrbInactiveTickEnabled() { return entityConfig.isExperienceOrbInactiveTickEnabled(); }
    public double getExperienceOrbInactiveRange() { return entityConfig.getExperienceOrbInactiveRange(); }
    public int getExperienceOrbInactiveMergeInterval() { return entityConfig.getExperienceOrbInactiveMergeInterval(); }
    public boolean isExperienceOrbMergeEnabled() { return entityConfig.isExperienceOrbMergeEnabled(); }
    public int getExperienceOrbMergeInterval() { return entityConfig.getExperienceOrbMergeInterval(); }

    public void setDebugLoggingEnabled(boolean enabled) {
        performanceConfig.setDebugLoggingEnabled(enabled);
        config.set("performance.debug-logging", enabled);
        plugin.saveConfig();
    }

    public boolean isPerformanceMetricsEnabled() { return performanceConfig.isPerformanceMetricsEnabled(); }
    public boolean isMobSpawningEnabled() { return entityConfig.isMobSpawningEnabled(); }
    public boolean isSpawnerOptimizationEnabled() { return entityConfig.isSpawnerOptimizationEnabled(); }
    public boolean isDensityControlEnabled() { return entityConfig.isDensityControlEnabled(); }
    public int getMaxEntitiesPerChunk() { return entityConfig.getMaxEntitiesPerChunk(); }
    public int getMobSpawnInterval() { return entityConfig.getMobSpawnInterval(); }

    public boolean isBrainThrottleEnabled() { return entityConfig.isBrainThrottleEnabled(); }
    public int getBrainThrottleInterval() { return entityConfig.getBrainThrottleInterval(); }
    public boolean isLivingEntityTravelOptimizationEnabled() { return entityConfig.isLivingEntityTravelOptimizationEnabled(); }
    public int getLivingEntityTravelSkipInterval() { return entityConfig.getLivingEntityTravelSkipInterval(); }
    public boolean isMobDespawnOptimizationEnabled() { return entityConfig.isMobDespawnOptimizationEnabled(); }
    public int getMobDespawnCheckInterval() { return entityConfig.getMobDespawnCheckInterval(); }

    public boolean isEndermanBlockCarryLimiterEnabled() { return entityConfig.isEndermanBlockCarryLimiterEnabled(); }
    public int getEndermanMaxCarrying() { return entityConfig.getEndermanMaxCarrying(); }
    public boolean isEndermanCountTowardsMobCap() { return entityConfig.isEndermanCountTowardsMobCap(); }
    public boolean isEndermanPreventPickup() { return entityConfig.isEndermanPreventPickup(); }

    public boolean isMultithreadedEntityTrackerEnabled() { return entityConfig.isMultithreadedTrackerEnabled(); }

    public boolean isAdvancedNetworkOptimizationEnabled() { return networkConfig.isAdvancedNetworkOptimizationEnabled(); }
    public boolean isFastVarIntEnabled() { return networkConfig.isFastVarIntEnabled(); }
    public boolean isEventLoopAffinityEnabled() { return networkConfig.isEventLoopAffinityEnabled(); }
    public boolean isByteBufOptimizerEnabled() { return networkConfig.isByteBufOptimizerEnabled(); }
    public boolean isStrictEventLoopChecking() { return networkConfig.isStrictEventLoopChecking(); }
    public boolean isPooledByteBufAllocator() { return networkConfig.isPooledByteBufAllocator(); }
    public boolean isDirectByteBufPreferred() { return networkConfig.isDirectByteBufPreferred(); }
    public boolean isSkipZeroMovementPacketsEnabled() { return networkConfig.isSkipZeroMovementPacketsEnabled(); }
    public boolean isSkipZeroMovementPacketsStrictMode() { return networkConfig.isSkipZeroMovementPacketsStrictMode(); }

    public boolean isMtuAwareBatchingEnabled() { return networkConfig.isMtuAwareBatchingEnabled(); }
    public int getMtuLimit() { return networkConfig.getMtuLimit(); }
    public int getMtuHardCapPackets() { return networkConfig.getMtuHardCapPackets(); }

    public boolean isFlushConsolidationEnabled() { return networkConfig.isFlushConsolidationEnabled(); }
    public int getFlushConsolidationExplicitFlushAfterFlushes() { return networkConfig.getFlushConsolidationExplicitFlushAfterFlushes(); }
    public boolean isFlushConsolidationConsolidateWhenNoReadInProgress() { return networkConfig.isFlushConsolidationConsolidateWhenNoReadInProgress(); }

    public boolean isNativeCompressionEnabled() { return networkConfig.isNativeCompressionEnabled(); }
    public int getNativeCompressionLevel() { return networkConfig.getNativeCompressionLevel(); }

    public boolean isNativeEncryptionEnabled() { return networkConfig.isNativeEncryptionEnabled(); }

    public boolean isExplosionBlockUpdateOptimizationEnabled() { return networkConfig.isExplosionBlockUpdateOptimizationEnabled(); }
    public int getExplosionBlockChangeThreshold() { return networkConfig.getExplosionBlockChangeThreshold(); }

    public int getHighLatencyThreshold() { return networkConfig.getHighLatencyThreshold(); }
    public int getHighLatencyMinViewDistance() { return networkConfig.getHighLatencyMinViewDistance(); }
    public long getHighLatencyDurationMs() { return networkConfig.getHighLatencyDurationMs(); }

    public boolean isAfkPacketThrottleEnabled() { return networkConfig.isAfkPacketThrottleEnabled(); }
    public long getAfkDurationMs() { return networkConfig.getAfkDurationMs(); }
    public double getAfkParticleMaxDistance() { return networkConfig.getAfkParticleMaxDistance(); }
    public double getAfkSoundMaxDistance() { return networkConfig.getAfkSoundMaxDistance(); }

    public boolean isDynamicChunkSendRateEnabled() { return networkConfig.isDynamicChunkSendRateEnabled(); }
    public long getDynamicChunkLimitBandwidth() { return networkConfig.getDynamicChunkLimitBandwidth(); }
    public long getDynamicChunkGuaranteedBandwidth() { return networkConfig.getDynamicChunkGuaranteedBandwidth(); }

    public boolean isPacketCompressionOptimizationEnabled() { return networkConfig.isPacketCompressionOptimizationEnabled(); }
    public boolean isAdaptiveCompressionThresholdEnabled() { return networkConfig.isAdaptiveCompressionThresholdEnabled(); }
    public boolean isSkipSmallPacketsEnabled() { return networkConfig.isSkipSmallPacketsEnabled(); }
    public int getSkipSmallPacketsThreshold() { return networkConfig.getSkipSmallPacketsThreshold(); }

    public boolean isChunkBatchOptimizationEnabled() { return networkConfig.isChunkBatchOptimizationEnabled(); }
    public float getChunkBatchMinChunks() { return networkConfig.getChunkBatchMinChunks(); }
    public float getChunkBatchMaxChunks() { return networkConfig.getChunkBatchMaxChunks(); }
    public int getChunkBatchMaxUnacked() { return networkConfig.getChunkBatchMaxUnacked(); }

    public boolean isPacketPriorityQueueEnabled() { return networkConfig.isPacketPriorityQueueEnabled(); }
    public boolean isPrioritizePlayerPacketsEnabled() { return networkConfig.isPrioritizePlayerPacketsEnabled(); }
    public boolean isPrioritizeChunkPacketsEnabled() { return networkConfig.isPrioritizeChunkPacketsEnabled(); }
    public boolean isDeprioritizeParticlesEnabled() { return networkConfig.isDeprioritizeParticlesEnabled(); }
    public boolean isDeprioritizeSoundsEnabled() { return networkConfig.isDeprioritizeSoundsEnabled(); }

    public boolean isMultithreadedTrackerEnabled() { return entityConfig.isMultithreadedTrackerEnabled(); }
    public int getMultithreadedTrackerParallelism() { return entityConfig.getMultithreadedTrackerParallelism(); }
    public int getMultithreadedTrackerBatchSize() { return entityConfig.getMultithreadedTrackerBatchSize(); }
    public int getMultithreadedTrackerAssistBatchSize() { return entityConfig.getMultithreadedTrackerAssistBatchSize(); }

    public boolean isAiSensorOptimizationEnabled() { return aiConfig.isAiSensorOptimizationEnabled(); }
    public int getAiSensorRefreshInterval() { return aiConfig.getAiSensorRefreshInterval(); }

    public boolean isGameEventOptimizationEnabled() { return aiConfig.isGameEventOptimizationEnabled(); }
    public boolean isGameEventEarlyFilter() { return aiConfig.isGameEventEarlyFilter(); }
    public boolean isGameEventThrottleLowPriority() { return aiConfig.isGameEventThrottleLowPriority(); }
    public long getGameEventThrottleIntervalMs() { return aiConfig.getGameEventThrottleIntervalMs(); }
    public boolean isGameEventDistanceFilter() { return aiConfig.isGameEventDistanceFilter(); }
    public double getGameEventMaxDetectionDistance() { return aiConfig.getGameEventMaxDetectionDistance(); }

    public boolean isAsyncPathfindingEnabled() { return aiConfig.isAsyncPathfindingEnabled(); }
    public int getAsyncPathfindingMaxThreads() { return performanceConfig.getThreadPoolSize(); }
    public int getAsyncPathfindingKeepAliveSeconds() { return aiConfig.getAsyncPathfindingKeepAliveSeconds(); }
    public int getAsyncPathfindingMaxQueueSize() { return aiConfig.getAsyncPathfindingMaxQueueSize(); }
    public int getAsyncPathfindingTimeoutMs() { return aiConfig.getAsyncPathfindingTimeoutMs(); }
    public boolean isAsyncPathfindingSyncFallbackEnabled() { return aiConfig.isAsyncPathfindingSyncFallbackEnabled(); }

    public boolean isEnhancedPathfindingEnabled() { return aiConfig.isEnhancedPathfindingEnabled(); }
    public int getEnhancedPathfindingMaxConcurrentRequests() { return aiConfig.getEnhancedPathfindingMaxConcurrentRequests(); }
    public int getEnhancedPathfindingMaxRequestsPerTick() { return aiConfig.getEnhancedPathfindingMaxRequestsPerTick(); }
    public int getEnhancedPathfindingHighPriorityDistance() { return aiConfig.getEnhancedPathfindingHighPriorityDistance(); }
    public int getEnhancedPathfindingMediumPriorityDistance() { return aiConfig.getEnhancedPathfindingMediumPriorityDistance(); }
    public boolean isPathPrewarmEnabled() { return aiConfig.isPathPrewarmEnabled(); }
    public int getPathPrewarmRadius() { return aiConfig.getPathPrewarmRadius(); }
    public int getPathPrewarmMaxMobsPerBatch() { return aiConfig.getPathPrewarmMaxMobsPerBatch(); }
    public int getPathPrewarmMaxPoisPerMob() { return aiConfig.getPathPrewarmMaxPoisPerMob(); }
    public boolean isAsyncPathfindingCacheEnabled() { return aiConfig.isAsyncPathfindingCacheEnabled(); }
    public int getAsyncPathfindingCacheMaxSize() { return aiConfig.getAsyncPathfindingCacheMaxSize(); }
    public int getAsyncPathfindingCacheExpireSeconds() { return aiConfig.getAsyncPathfindingCacheExpireSeconds(); }
    public int getAsyncPathfindingCacheReuseTolerance() { return aiConfig.getAsyncPathfindingCacheReuseTolerance(); }
    public int getAsyncPathfindingCacheCleanupIntervalSeconds() { return aiConfig.getAsyncPathfindingCacheCleanupIntervalSeconds(); }
    public boolean isCollisionOptimizationEnabled() { return collisionConfig.isCollisionOptimizationEnabled(); }
    public boolean isCollisionAggressiveMode() { return collisionConfig.isCollisionAggressiveMode(); }
    public java.util.Set<String> getCollisionExcludedEntities() { return collisionConfig.getCollisionExcludedEntities(); }

    public boolean isNativeCollisionsEnabled() { return collisionConfig.isNativeCollisionsEnabled(); }
    public boolean isNativeCollisionsFallbackEnabled() { return collisionConfig.isNativeCollisionsFallbackEnabled(); }
    public boolean isCollisionBlockCacheEnabled() { return collisionConfig.isCollisionBlockCacheEnabled(); }
    public int getCollisionBlockCacheSize() { return collisionConfig.getCollisionBlockCacheSize(); }
    public int getCollisionBlockCacheExpireTicks() { return collisionConfig.getCollisionBlockCacheExpireTicks(); }
    public boolean isRayCollisionEnabled() { return collisionConfig.isRayCollisionEnabled(); }
    public double getRayCollisionMaxDistance() { return collisionConfig.getRayCollisionMaxDistance(); }
    public boolean isShapeOptimizationEnabled() { return collisionConfig.isShapeOptimizationEnabled(); }
    public boolean isShapePrecomputeArrays() { return collisionConfig.isShapePrecomputeArrays(); }
    public boolean isShapeBlockShapeCache() { return collisionConfig.isShapeBlockShapeCache(); }
    public int getShapeBlockShapeCacheSize() { return collisionConfig.getShapeBlockShapeCacheSize(); }
    public boolean isTntCacheEnabled() { return tntConfig.isTntCacheEnabled(); }
    public boolean isTntUseOptimizedCache() { return tntConfig.isTntUseOptimizedCache(); }
    public int getTntCacheExpiryTicks() { return tntConfig.getTntCacheExpiryTicks(); }
    public boolean isTntCacheWarmupEnabled() { return tntConfig.isTntCacheWarmupEnabled(); }
    public boolean isTntPrecomputedShapeEnabled() { return tntConfig.isTntPrecomputedShapeEnabled(); }
    public boolean isTntUseOcclusionDetection() { return tntConfig.isTntUseOcclusionDetection(); }
    public double getTntOcclusionThreshold() { return tntConfig.getTntOcclusionThreshold(); }
    public boolean isTntBatchCollisionEnabled() { return tntConfig.isTntBatchCollisionEnabled(); }
    public int getTntBatchUnrollFactor() { return tntConfig.getTntBatchUnrollFactor(); }
    public boolean isEntityThrottlingEnabled() { return entityConfig.isEntityThrottlingEnabled(); }
    public String getEntityThrottlingConfigFile() { return entityConfig.getEntityThrottlingConfigFile(); }
    public int getEntityThrottlingCheckInterval() { return entityConfig.getEntityThrottlingCheckInterval(); }
    public int getEntityThrottlingThrottleInterval() { return entityConfig.getEntityThrottlingThrottleInterval(); }
    public int getEntityThrottlingRemovalBatchSize() { return entityConfig.getEntityThrottlingRemovalBatchSize(); }
    public boolean isZeroDelayFactoryOptimizationEnabled() { return miscConfig.isZeroDelayFactoryOptimizationEnabled(); }
    public java.util.Set<String> getZeroDelayFactoryEntities() { return miscConfig.getZeroDelayFactoryEntities(); }
    public boolean isBlockEntityParallelTickEnabled() { return miscConfig.isBlockEntityParallelTickEnabled(); }
    public int getBlockEntityParallelMinBlockEntities() { return miscConfig.getBlockEntityParallelMinBlockEntities(); }
    public int getBlockEntityParallelBatchSize() { return miscConfig.getBlockEntityParallelBatchSize(); }
    public boolean isBlockEntityParallelProtectContainers() { return miscConfig.isBlockEntityParallelProtectContainers(); }
    public int getBlockEntityParallelTimeoutMs() { return miscConfig.getBlockEntityParallelTimeoutMs(); }
    public boolean isHopperOptimizationEnabled() { return entityConfig.isHopperOptimizationEnabled(); }
    public int getHopperCacheExpireTime() { return entityConfig.getHopperCacheExpireTime(); }
    public boolean isMinecartOptimizationEnabled() { return entityConfig.isMinecartOptimizationEnabled(); }
    public int getMinecartTickInterval() { return entityConfig.getMinecartTickInterval(); }
    public boolean isEntityTickParallel() { return entityConfig.isEntityTickParallel(); }
    public int getMinEntitiesForParallel() { return entityConfig.getMinEntitiesForParallel(); }
    public int getEntityTickBatchSize() { return entityConfig.getEntityTickBatchSize(); }
    public boolean isAsyncLightingEnabled() { return lightingConfig.isAsyncLightingEnabled(); }
    public int getLightBatchThreshold() { return lightingConfig.getLightBatchThreshold(); }
    public int getLightUpdateIntervalMs() { return lightingConfig.getLightUpdateIntervalMs(); }
    public boolean useLayeredPropagationQueue() { return lightingConfig.useLayeredPropagationQueue(); }
    public int getMaxLightPropagationDistance() { return lightingConfig.getMaxLightPropagationDistance(); }
    public boolean isSkylightCacheEnabled() { return lightingConfig.isSkylightCacheEnabled(); }
    public int getSkylightCacheDurationMs() { return lightingConfig.getSkylightCacheDurationMs(); }
    public boolean isLightDeduplicationEnabled() { return lightingConfig.isLightDeduplicationEnabled(); }
    public boolean isDynamicBatchAdjustmentEnabled() { return lightingConfig.isDynamicBatchAdjustmentEnabled(); }
    public boolean isAdvancedLightingStatsEnabled() { return lightingConfig.isAdvancedLightingStatsEnabled(); }
    public boolean isLightingDebugEnabled() { return lightingConfig.isLightingDebugEnabled(); }
    public boolean isSpawnChunkRemovalEnabled() { return chunkConfig.isSpawnChunkRemovalEnabled(); }
    public boolean isPlayerChunkLoadingOptimizationEnabled() { return chunkConfig.isPlayerChunkLoadingOptimizationEnabled(); }
    public int getMaxConcurrentChunkLoadsPerPlayer() { return chunkConfig.getMaxConcurrentChunkLoadsPerPlayer(); }
    public boolean isEntityTrackingRangeOptimizationEnabled() { return entityConfig.isEntityTrackingRangeOptimizationEnabled(); }
    public double getEntityTrackingRangeMultiplier() { return entityConfig.getEntityTrackingRangeMultiplier(); }

    public boolean isTNTUseSakuraDensityCache() { return tntConfig.isTntUseSakuraDensityCache(); }
    public boolean isTNTUseVectorizedAABB() { return tntConfig.isTntUseVectorizedAABB(); }
    public boolean isTNTUseUnifiedEngine() { return tntConfig.isTntUseUnifiedEngine(); }
    public boolean isTNTMergeEnabled() { return tntConfig.isTntMergeEnabled(); }
    public double getTNTMergeRadius() { return tntConfig.getTntMergeRadius(); }
    public int getTNTMaxFuseDifference() { return tntConfig.getTntMaxFuseDifference(); }
    public float getTNTMergedPowerMultiplier() { return tntConfig.getTntMergedPowerMultiplier(); }
    public float getTNTMaxPower() { return tntConfig.getTntMaxPower(); }

    public boolean isAsyncVillagerBreedEnabled() { return entityConfig.isAsyncVillagerBreedEnabled(); }
    public boolean isVillagerAgeThrottleEnabled() { return entityConfig.isVillagerAgeThrottleEnabled(); }
    public int getVillagerBreedThreads() { return performanceConfig.getThreadPoolSize(); }
    public int getVillagerBreedCheckInterval() { return entityConfig.getVillagerBreedCheckInterval(); }
    public boolean isTNTOptimizationEnabled() { return tntConfig.isTntOptimizationEnabled(); }
    public int getTNTThreads() { return performanceConfig.getThreadPoolSize(); }
    public int getTNTMaxBlocks() { return tntConfig.getTntMaxBlocks(); }
    public long getTNTTimeoutMicros() { return tntConfig.getTntTimeoutMicros(); }
    public int getTNTBatchSize() { return tntConfig.getTntBatchSize(); }
    public boolean isTNTDebugEnabled() { return tntConfig.isTntDebugEnabled(); }
    public boolean isTNTVanillaCompatibilityEnabled() { return tntConfig.isTntVanillaCompatibilityEnabled(); }
    public boolean isTNTUseVanillaPower() { return tntConfig.isTntUseVanillaPower(); }
    public boolean isTNTUseVanillaFireLogic() { return tntConfig.isTntUseVanillaFireLogic(); }
    public boolean isTNTUseVanillaDamageCalculation() { return tntConfig.isTntUseVanillaDamageCalculation(); }
    public boolean isTNTUseFullRaycast() { return tntConfig.isTntUseFullRaycast(); }
    public boolean isBeeFixEnabled() { return entityConfig.isBeeFixEnabled(); }
    public boolean isEndIslandDensityFixEnabled() { return entityConfig.isEndIslandDensityFixEnabled(); }
    public boolean isLeaderZombieHealthFixEnabled() { return entityConfig.isLeaderZombieHealthFixEnabled(); }
    public boolean isEquipmentHealthCapFixEnabled() { return entityConfig.isEquipmentHealthCapFixEnabled(); }
    public boolean isPortalSuffocationCheckDisabled() { return entityConfig.isPortalSuffocationCheckDisabled(); }
    public boolean isShulkerBulletSelfHitFixEnabled() { return entityConfig.isShulkerBulletSelfHitFixEnabled(); }

    public boolean isExecuteCommandInactiveSkipEnabled() { return miscConfig.isExecuteCommandInactiveSkipEnabled(); }
    public int getExecuteCommandSkipLevel() { return miscConfig.getExecuteCommandSkipLevel(); }
    public double getExecuteCommandSimulationDistanceMultiplier() { return miscConfig.getExecuteCommandSimulationDistanceMultiplier(); }
    public long getExecuteCommandCacheDurationMs() { return miscConfig.getExecuteCommandCacheDurationMs(); }
    public Set<String> getExecuteCommandWhitelistTypes() { return miscConfig.getExecuteCommandWhitelistTypes(); }
    public boolean isExecuteCommandDebugEnabled() { return miscConfig.isExecuteCommandDebugEnabled(); }

    public boolean isCommandDeduplicationEnabled() { return miscConfig.isCommandDeduplicationEnabled(); }
    public boolean isCommandDeduplicationDebugEnabled() { return miscConfig.isCommandDeduplicationDebugEnabled(); }

    public boolean isTNTUseVanillaBlockDestruction() { return tntConfig.isTntUseVanillaBlockDestruction(); }
    public boolean isTNTUseVanillaDrops() { return tntConfig.isTntUseVanillaDrops(); }
    public boolean isChunkTickAsyncEnabled() { return chunkConfig.isChunkTickAsyncEnabled(); }
    public int getChunkTickThreads() { return performanceConfig.getThreadPoolSize(); }
    public long getChunkTickTimeoutMicros() { return chunkConfig.getChunkTickTimeoutMicros(); }
    public int getChunkTickAsyncBatchSize() { return chunkConfig.getChunkTickAsyncBatchSize(); }

    public int getConfigVersion() { return miscConfig.getConfigVersion(); }

    public boolean isStructureLocationAsyncEnabled() { return miscConfig.isStructureLocationAsyncEnabled(); }
    public int getStructureLocationThreads() { return performanceConfig.getThreadPoolSize(); }
    public boolean isLocateCommandEnabled() { return miscConfig.isLocateCommandEnabled(); }
    public int getLocateCommandSearchRadius() { return miscConfig.getLocateCommandSearchRadius(); }
    public boolean isLocateCommandSkipKnownStructures() { return miscConfig.isLocateCommandSkipKnownStructures(); }
    public boolean isVillagerTradeMapsEnabled() { return miscConfig.isVillagerTradeMapsEnabled(); }
    public int getVillagerTradeMapsSearchRadius() { return miscConfig.getVillagerTradeMapsSearchRadius(); }
    public boolean isVillagerTradeMapsSkipKnownStructures() { return miscConfig.isVillagerTradeMapsSkipKnownStructures(); }
    public java.util.Set<String> getVillagerTradeMapTypes() { return miscConfig.getVillagerTradeMapTypes(); }
    public int getVillagerMapGenerationTimeoutSeconds() { return miscConfig.getVillagerMapGenerationTimeoutSeconds(); }
    public boolean isDolphinTreasureHuntEnabled() { return miscConfig.isDolphinTreasureHuntEnabled(); }
    public int getDolphinTreasureSearchRadius() { return miscConfig.getDolphinTreasureSearchRadius(); }
    public boolean isChestExplorationMapsEnabled() { return miscConfig.isChestExplorationMapsEnabled(); }
    public java.util.Set<String> getChestExplorationLootTables() { return miscConfig.getChestExplorationLootTables(); }
    public boolean isStructureLocationDebugEnabled() { return performanceConfig.isDebugLoggingEnabled(); }

    public boolean isStructureAlgorithmOptimizationEnabled() { return miscConfig.isStructureAlgorithmOptimizationEnabled(); }
    public String getStructureSearchPattern() { return miscConfig.getStructureSearchPattern(); }
    public boolean isStructureCachingEnabled() { return miscConfig.isStructureCachingEnabled(); }
    public boolean isBiomeAwareSearchEnabled() { return miscConfig.isBiomeAwareSearchEnabled(); }
    public int getStructureCacheMaxSize() { return miscConfig.getStructureCacheMaxSize(); }
    public long getStructureCacheExpirationMinutes() { return miscConfig.getStructureCacheExpirationMinutes(); }

    public boolean isDataPackOptimizationEnabled() { return miscConfig.isDataPackOptimizationEnabled(); }
    public int getDataPackFileLoadThreads() { return performanceConfig.getThreadPoolSize(); }
    public int getDataPackZipProcessThreads() { return performanceConfig.getThreadPoolSize(); }
    public int getDataPackBatchSize() { return miscConfig.getDataPackBatchSize(); }
    public long getDataPackCacheExpirationMinutes() { return miscConfig.getDataPackCacheExpirationMinutes(); }
    public int getDataPackMaxFileCacheSize() { return miscConfig.getDataPackMaxFileCacheSize(); }
    public int getDataPackMaxFileSystemCacheSize() { return miscConfig.getDataPackMaxFileSystemCacheSize(); }
    public boolean isDataPackDebugEnabled() { return performanceConfig.isDebugLoggingEnabled(); }

    public boolean isNitoriOptimizationsEnabled() { return performanceConfig.isNitoriOptimizationsEnabled(); }
    public boolean isBlockPosCacheEnabled() { return performanceConfig.isBlockPosCacheEnabled(); }
    public boolean isOptimizedCollectionsEnabled() { return performanceConfig.isOptimizedCollectionsEnabled(); }

    public boolean isMobSunBurnOptimizationEnabled() { return entityConfig.isMobSunBurnOptimizationEnabled(); }
    public boolean isEntitySpeedOptimizationEnabled() { return entityConfig.isEntitySpeedOptimizationEnabled(); }
    public boolean isEntityFallDamageOptimizationEnabled() { return entityConfig.isEntityFallDamageOptimizationEnabled(); }
    public boolean isEntitySectionStorageOptimizationEnabled() { return entityConfig.isEntitySectionStorageOptimizationEnabled(); }

    public boolean isChunkPosOptimizationEnabled() { return chunkConfig.isChunkPosOptimizationEnabled(); }
    public boolean isNoiseOptimizationEnabled() { return chunkConfig.isNoiseOptimizationEnabled(); }
    public boolean isSimdOptimizationEnabled() { return chunkConfig.isSimdOptimizationEnabled(); }
    public boolean isBatchRandomEnabled() { return chunkConfig.isBatchRandomEnabled(); }
    public int getBatchRandomPoolSize() { return chunkConfig.getBatchRandomPoolSize(); }
    public boolean isWorldgenThreadsEnabled() { return chunkConfig.isWorldgenThreadsEnabled(); }
    public boolean isNbtOptimizationEnabled() { return performanceConfig.isNbtOptimizationEnabled(); }
    public boolean isBitSetPoolingEnabled() { return performanceConfig.isBitSetPoolingEnabled(); }
    public boolean isCompletableFutureOptimizationEnabled() { return performanceConfig.isCompletableFutureOptimizationEnabled(); }
    public boolean isChunkOptimizationEnabled() { return chunkConfig.isChunkOptimizationEnabled(); }

    public int getMidTickChunkTasksIntervalMs() {

        long intervalNs = config.getLong("chunk.loading.mid-tick-tasks.interval-ns", 1000000);
        return Math.max(1, (int)(intervalNs / 1000000));
    }

    public String getSeedEncryptionScheme() { return miscConfig.getSeedEncryptionScheme(); }
    public boolean isSeedEncryptionEnabled() { return miscConfig.isSeedEncryptionEnabled(); }
    public boolean isSeedEncryptionProtectStructures() { return miscConfig.isSeedEncryptionProtectStructures(); }
    public boolean isSeedEncryptionProtectOres() { return miscConfig.isSeedEncryptionProtectOres(); }
    public boolean isSeedEncryptionProtectSlimes() { return miscConfig.isSeedEncryptionProtectSlimes(); }
    public boolean isSeedEncryptionProtectBiomes() { return miscConfig.isSeedEncryptionProtectBiomes(); }

    public boolean isSecureSeedEnabled() { return miscConfig.isSeedEncryptionEnabled(); }
    public boolean isSecureSeedProtectStructures() { return miscConfig.isSeedEncryptionProtectStructures(); }
    public boolean isSecureSeedProtectOres() { return miscConfig.isSeedEncryptionProtectOres(); }
    public boolean isSecureSeedProtectSlimes() { return miscConfig.isSeedEncryptionProtectSlimes(); }
    public int getSecureSeedBits() { return miscConfig.getSecureSeedBits(); }
    public boolean isSecureSeedDebugLogging() { return performanceConfig.isDebugLoggingEnabled(); }

    public boolean isQuantumSeedEnabled() { return miscConfig.isQuantumSeedEnabled(); }
    public int getQuantumSeedEncryptionLevel() { return miscConfig.getQuantumSeedEncryptionLevel(); }
    public String getQuantumSeedPrivateKeyFile() { return miscConfig.getQuantumSeedPrivateKeyFile(); }
    public boolean isQuantumSeedEnableTimeDecay() { return miscConfig.isQuantumSeedEnableTimeDecay(); }
    public int getQuantumSeedCacheSize() { return miscConfig.getQuantumSeedCacheSize(); }
    public boolean isQuantumSeedDebugLogging() { return performanceConfig.isDebugLoggingEnabled(); }

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

    public boolean isFurnaceRecipeCacheEnabled() { return miscConfig.isFurnaceRecipeCacheEnabled(); }
    public int getFurnaceRecipeCacheSize() { return miscConfig.getFurnaceRecipeCacheSize(); }
    public boolean isFurnaceCacheApplyToBlastFurnace() { return miscConfig.isFurnaceCacheApplyToBlastFurnace(); }
    public boolean isFurnaceCacheApplyToSmoker() { return miscConfig.isFurnaceCacheApplyToSmoker(); }
    public boolean isFurnaceFixBurnTimeBug() { return miscConfig.isFurnaceFixBurnTimeBug(); }

    public boolean isCraftingRecipeCacheEnabled() { return miscConfig.isCraftingRecipeCacheEnabled(); }
    public int getCraftingRecipeCacheSize() { return miscConfig.getCraftingRecipeCacheSize(); }
    public boolean isCraftingOptimizeBatchCrafting() { return miscConfig.isCraftingOptimizeBatchCrafting(); }
    public boolean isCraftingReduceNetworkTraffic() { return miscConfig.isCraftingReduceNetworkTraffic(); }

    public boolean isMinecartCauldronDestructionEnabled() { return entityConfig.isMinecartCauldronDestructionEnabled(); }

    public int getCurrentConfigVersion() { return CURRENT_CONFIG_VERSION; }

    public boolean isFallingBlockParallelEnabled() { return entityConfig.isFallingBlockParallelEnabled(); }
    public int getMinFallingBlocksForParallel() { return entityConfig.getMinFallingBlocksForParallel(); }
    public int getFallingBlockBatchSize() { return entityConfig.getFallingBlockBatchSize(); }

    public boolean isItemEntityMergeOptimizationEnabled() { return entityConfig.isItemEntityMergeOptimizationEnabled(); }
    public boolean isItemEntityCancelVanillaMerge() { return entityConfig.isItemEntityCancelVanillaMerge(); }
    public int getItemEntityMergeInterval() { return entityConfig.getItemEntityMergeInterval(); }
    public int getItemEntityMinNearbyItems() { return entityConfig.getItemEntityMinNearbyItems(); }
    public double getItemEntityMergeRange() { return entityConfig.getItemEntityMergeRange(); }
    public boolean isItemEntityAgeOptimizationEnabled() { return entityConfig.isItemEntityAgeOptimizationEnabled(); }
    public int getItemEntityAgeInterval() { return entityConfig.getItemEntityAgeInterval(); }
    public double getItemEntityPlayerDetectionRange() { return entityConfig.getItemEntityPlayerDetectionRange(); }
    public boolean isItemEntityInactiveTickEnabled() { return entityConfig.isItemEntityInactiveTickEnabled(); }
    public double getItemEntityInactiveRange() { return entityConfig.getItemEntityInactiveRange(); }
    public int getItemEntityInactiveMergeInterval() { return entityConfig.getItemEntityInactiveMergeInterval(); }

    public boolean isChunkVisibilityFilterEnabled() { return networkConfig.isChunkVisibilityFilterEnabled(); }

    public boolean isSuffocationOptimizationEnabled() { return entityConfig.isSuffocationOptimizationEnabled(); }
    public boolean isFastRayTraceEnabled() { return entityConfig.isFastRayTraceEnabled(); }
    public boolean isMapRenderingOptimizationEnabled() { return networkConfig.isMapRenderingOptimizationEnabled(); }
    public int getMapRenderingThreads() { return performanceConfig.getThreadPoolSize(); }

    public boolean isProjectileOptimizationEnabled() { return entityConfig.isProjectileOptimizationEnabled(); }
    public int getMaxProjectileLoadsPerTick() { return entityConfig.getMaxProjectileLoadsPerTick(); }
    public int getMaxProjectileLoadsPerProjectile() { return entityConfig.getMaxProjectileLoadsPerProjectile(); }

    public boolean isFastMovementChunkLoadEnabled() { return networkConfig.isFastMovementChunkLoadEnabled(); }
    public double getFastMovementSpeedThreshold() { return networkConfig.getFastMovementSpeedThreshold(); }
    public int getFastMovementPreloadDistance() { return networkConfig.getFastMovementPreloadDistance(); }
    public int getFastMovementMaxConcurrentLoads() { return networkConfig.getFastMovementMaxConcurrentLoads(); }
    public int getFastMovementPredictionTicks() { return networkConfig.getFastMovementPredictionTicks(); }

    public boolean isCenterOffsetEnabled() { return networkConfig.isCenterOffsetEnabled(); }
    public double getMinOffsetSpeed() { return networkConfig.getMinOffsetSpeed(); }
    public double getMaxOffsetSpeed() { return networkConfig.getMaxOffsetSpeed(); }
    public double getMaxOffsetRatio() { return networkConfig.getMaxOffsetRatio(); }

    public boolean isPlayerJoinWarmupEnabled() { return networkConfig.isPlayerJoinWarmupEnabled(); }
    public long getPlayerJoinWarmupDurationMs() { return networkConfig.getPlayerJoinWarmupDurationMs(); }
    public double getPlayerJoinWarmupInitialRate() { return networkConfig.getPlayerJoinWarmupInitialRate(); }
    public int getAsyncLoadingBatchSize() { return networkConfig.getAsyncLoadingBatchSize(); }
    public long getAsyncLoadingBatchDelayMs() { return networkConfig.getAsyncLoadingBatchDelayMs(); }

    public boolean getBoolean(String path, boolean defaultValue) {
        return config != null ? config.getBoolean(path, defaultValue) : defaultValue;
    }

    public boolean isVirtualEntityCompatibilityEnabled() { return compatibilityConfig.isVirtualEntityCompatibilityEnabled(); }
    public boolean isVirtualEntityBypassPacketQueue() { return compatibilityConfig.isVirtualEntityBypassPacketQueue(); }
    public boolean isVirtualEntityExcludeFromThrottling() { return compatibilityConfig.isVirtualEntityExcludeFromThrottling(); }
    public boolean isVirtualEntityDebugEnabled() { return performanceConfig.isDebugLoggingEnabled(); }

    public boolean isFancynpcsCompatEnabled() { return compatibilityConfig.isFancynpcsCompatEnabled(); }
    public boolean isFancynpcsUseAPI() { return compatibilityConfig.isFancynpcsUseAPI(); }
    public int getFancynpcsPriority() { return compatibilityConfig.getFancynpcsPriority(); }

    public boolean isZnpcsplusCompatEnabled() { return compatibilityConfig.isZnpcsplusCompatEnabled(); }
    public boolean isZnpcsplusUseAPI() { return compatibilityConfig.isZnpcsplusUseAPI(); }
    public int getZnpcsplusPriority() { return compatibilityConfig.getZnpcsplusPriority(); }

    public java.util.List<String> getVirtualEntityDetectionOrder() { return compatibilityConfig.getVirtualEntityDetectionOrder(); }

    public int getLightingThreadPoolSize() { return lightingConfig.getLightingThreadPoolSize(); }
    public String getLightingThreadPoolMode() { return lightingConfig.getLightingThreadPoolMode(); }
    public String getLightingThreadPoolCalculation() { return lightingConfig.getLightingThreadPoolCalculation(); }
    public int getLightingMinThreads() { return lightingConfig.getLightingMinThreads(); }
    public int getLightingMaxThreads() { return lightingConfig.getLightingMaxThreads(); }
    public int getLightingBatchThresholdMax() { return lightingConfig.getLightingBatchThresholdMax(); }
    public boolean isLightingAggressiveBatching() { return lightingConfig.isLightingAggressiveBatching(); }

    public boolean isLightingPrioritySchedulingEnabled() { return lightingConfig.isLightingPrioritySchedulingEnabled(); }
    public int getLightingHighPriorityRadius() { return lightingConfig.getLightingHighPriorityRadius(); }
    public int getLightingMediumPriorityRadius() { return lightingConfig.getLightingMediumPriorityRadius(); }
    public int getLightingLowPriorityRadius() { return lightingConfig.getLightingLowPriorityRadius(); }
    public long getLightingMaxLowPriorityDelay() { return lightingConfig.getLightingMaxLowPriorityDelay(); }

    public boolean isLightingDebouncingEnabled() { return lightingConfig.isLightingDebouncingEnabled(); }
    public long getLightingDebounceDelay() { return lightingConfig.getLightingDebounceDelay(); }
    public int getLightingMaxUpdatesPerSecond() { return lightingConfig.getLightingMaxUpdatesPerSecond(); }
    public long getLightingResetOnStableMs() { return lightingConfig.getLightingResetOnStableMs(); }

    public boolean isLightingMergingEnabled() { return lightingConfig.isLightingMergingEnabled(); }
    public int getLightingMergeRadius() { return lightingConfig.getLightingMergeRadius(); }
    public long getLightingMergeDelay() { return lightingConfig.getLightingMergeDelay(); }
    public int getLightingMaxMergedUpdates() { return lightingConfig.getLightingMaxMergedUpdates(); }

    public boolean isLightingChunkBorderEnabled() { return lightingConfig.isLightingChunkBorderEnabled(); }
    public boolean isLightingBatchBorderUpdates() { return lightingConfig.isLightingBatchBorderUpdates(); }
    public long getLightingBorderUpdateDelay() { return lightingConfig.getLightingBorderUpdateDelay(); }
    public int getLightingCrossChunkBatchSize() { return lightingConfig.getLightingCrossChunkBatchSize(); }

    public boolean isLightingAdaptiveEnabled() { return lightingConfig.isLightingAdaptiveEnabled(); }
    public int getLightingMonitorInterval() { return lightingConfig.getLightingMonitorInterval(); }
    public boolean isLightingAutoAdjustThreads() { return lightingConfig.isLightingAutoAdjustThreads(); }
    public boolean isLightingAutoAdjustBatchSize() { return lightingConfig.isLightingAutoAdjustBatchSize(); }
    public int getLightingTargetQueueSize() { return lightingConfig.getLightingTargetQueueSize(); }
    public int getLightingTargetLatency() { return lightingConfig.getLightingTargetLatency(); }

    public boolean isLightingChunkUnloadEnabled() { return lightingConfig.isLightingChunkUnloadEnabled(); }
    public boolean isLightingAsyncCleanup() { return lightingConfig.isLightingAsyncCleanup(); }
    public int getLightingCleanupBatchSize() { return lightingConfig.getLightingCleanupBatchSize(); }
    public long getLightingCleanupDelay() { return lightingConfig.getLightingCleanupDelay(); }

    public boolean isJigsawOptimizationEnabled() { return chunkConfig.isJigsawOptimizationEnabled(); }

    public boolean isMultiNettyEventLoopEnabled() { return performanceConfig.isMultiNettyEventLoopEnabled(); }
    public boolean isPalettedContainerLockRemovalEnabled() { return performanceConfig.isPalettedContainerLockRemovalEnabled(); }
    public boolean isSpawnDensityArrayEnabled() { return performanceConfig.isSpawnDensityArrayEnabled(); }
    public boolean isTypeFilterableListOptimizationEnabled() { return performanceConfig.isTypeFilterableListOptimizationEnabled(); }
    public boolean isEntityTrackerLinkedHashMapEnabled() { return performanceConfig.isEntityTrackerLinkedHashMapEnabled(); }
    public boolean isBiomeAccessOptimizationEnabled() { return performanceConfig.isBiomeAccessOptimizationEnabled(); }
    public boolean isEntityMoveZeroVelocityOptimizationEnabled() { return performanceConfig.isEntityMoveZeroVelocityOptimizationEnabled(); }
    public boolean isEntityTrackerDistanceCacheEnabled() { return performanceConfig.isEntityTrackerDistanceCacheEnabled(); }
    public boolean isMixinPrewarmEnabled() { return performanceConfig.isMixinPrewarmEnabled(); }
    public boolean isMixinPrewarmAsync() { return performanceConfig.isMixinPrewarmAsync(); }
    public boolean isVirtualThreadEnabled() { return performanceConfig.isVirtualThreadEnabled(); }
    public boolean isWorkStealingEnabled() { return performanceConfig.isWorkStealingEnabled(); }

    public String getLanguage() { return miscConfig.getLanguage(); }

    public NetworkConfig network() { return networkConfig; }
    public EntityConfig entity() { return entityConfig; }
    public AIConfig ai() { return aiConfig; }
    public LightingConfig lighting() { return lightingConfig; }
    public ChunkConfig chunk() { return chunkConfig; }
    public TNTConfig tnt() { return tntConfig; }
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Config sections are intentionally shared and immutable after load")
    public CollisionConfig collision() { return collisionConfig; }
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Config sections are intentionally shared and immutable after load")
    public PerformanceConfig performance() { return performanceConfig; }
    public CompatibilityConfig compatibility() { return compatibilityConfig; }
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Config sections are intentionally shared and immutable after load")
    public MiscConfig misc() { return miscConfig; }
}
