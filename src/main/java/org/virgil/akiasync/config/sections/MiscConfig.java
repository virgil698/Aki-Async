package org.virgil.akiasync.config.sections;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;

public class MiscConfig {

    private String language;
    private int configVersion;

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

    private boolean zeroDelayFactoryOptimizationEnabled;
    private String zeroDelayFactoryEntitiesConfigFile;
    private Set<String> zeroDelayFactoryEntities;

    private boolean blockEntityParallelTickEnabled;
    private int blockEntityParallelMinBlockEntities;
    private int blockEntityParallelBatchSize;
    private boolean blockEntityParallelProtectContainers;
    private int blockEntityParallelTimeoutMs;

    private boolean hopperOptimizationEnabled;
    private int hopperCacheExpireTime;

    private boolean minecartOptimizationEnabled;
    private int minecartTickInterval;

    private boolean structureLocationAsyncEnabled;
    private int structureLocationThreads;
    private boolean locateCommandEnabled;
    private int locateCommandSearchRadius;
    private boolean locateCommandSkipKnownStructures;
    private boolean villagerTradeMapsEnabled;
    private int villagerTradeMapsSearchRadius;
    private boolean villagerTradeMapsSkipKnownStructures;
    private Set<String> villagerTradeMapTypes;
    private int villagerMapGenerationTimeoutSeconds;
    private boolean dolphinTreasureHuntEnabled;
    private int dolphinTreasureSearchRadius;
    private boolean chestExplorationMapsEnabled;
    private Set<String> chestExplorationLootTables;

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

    private boolean executeCommandInactiveSkipEnabled;
    private int executeCommandSkipLevel;
    private double executeCommandSimulationDistanceMultiplier;
    private long executeCommandCacheDurationMs;
    private Set<String> executeCommandWhitelistTypes;
    private boolean executeCommandDebugEnabled;

    private boolean commandDeduplicationEnabled;
    private boolean commandDeduplicationDebugEnabled;

    public void load(FileConfiguration config) {
        language = config.getString("language", "zh_CN");
        configVersion = config.getInt("version", 6);

        loadSeedEncryption(config);
        loadRecipeOptimization(config);
        loadBlockEntityOptimization(config);
        loadStructureLocation(config);
        loadDataPackOptimization(config);
        loadCommandOptimization(config);
    }

    private void loadSeedEncryption(FileConfiguration config) {
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
    }

    private void loadRecipeOptimization(FileConfiguration config) {
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
    }

    private void loadBlockEntityOptimization(FileConfiguration config) {
        zeroDelayFactoryOptimizationEnabled = config.getBoolean("block-entity-optimizations.zero-delay-factory-optimization.enabled", false);
        zeroDelayFactoryEntitiesConfigFile = config.getString("block-entity-optimizations.zero-delay-factory-optimization.entities-config-file", "entities.yml");

        blockEntityParallelTickEnabled = config.getBoolean("block-entity-optimizations.parallel-tick.enabled", true);
        blockEntityParallelMinBlockEntities = config.getInt("block-entity-optimizations.parallel-tick.min-block-entities", 50);
        blockEntityParallelBatchSize = config.getInt("block-entity-optimizations.parallel-tick.batch-size", 16);
        blockEntityParallelProtectContainers = config.getBoolean("block-entity-optimizations.parallel-tick.protect-containers", true);
        blockEntityParallelTimeoutMs = config.getInt("block-entity-optimizations.parallel-tick.timeout-ms", 50);

        hopperOptimizationEnabled = config.getBoolean("block-entity-optimizations.hopper-optimization.enabled", true);
        hopperCacheExpireTime = config.getInt("block-entity-optimizations.hopper-optimization.cache-expire-time", 100);

        minecartOptimizationEnabled = config.getBoolean("block-entity-optimizations.minecart-optimization.enabled", true);
        minecartTickInterval = config.getInt("block-entity-optimizations.minecart-optimization.tick-interval", 2);
    }

    private void loadStructureLocation(FileConfiguration config) {
        structureLocationAsyncEnabled = config.getBoolean("structure-location-async.enabled", true);
        structureLocationThreads = config.getInt("structure-location-async.threads", 3);
        locateCommandEnabled = config.getBoolean("structure-location-async.locate-command.enabled", true);
        locateCommandSearchRadius = config.getInt("structure-location-async.locate-command.search-radius", 100);
        locateCommandSkipKnownStructures = config.getBoolean("structure-location-async.locate-command.skip-known-structures", false);
        villagerTradeMapsEnabled = config.getBoolean("structure-location-async.villager-trade-maps.enabled", true);
        villagerTradeMapsSearchRadius = config.getInt("structure-location-async.villager-trade-maps.search-radius", 100);
        villagerTradeMapsSkipKnownStructures = config.getBoolean("structure-location-async.villager-trade-maps.skip-known-structures", false);
        villagerTradeMapTypes = new HashSet<>(config.getStringList("structure-location-async.villager-trade-maps.trade-types"));
        if (villagerTradeMapTypes.isEmpty()) {
            villagerTradeMapTypes.add("minecraft:ocean_monument_map");
            villagerTradeMapTypes.add("minecraft:woodland_mansion_map");
            villagerTradeMapTypes.add("minecraft:buried_treasure_map");
        }
        villagerMapGenerationTimeoutSeconds = config.getInt("structure-location-async.villager-trade-maps.generation-timeout-seconds", 30);
        dolphinTreasureHuntEnabled = config.getBoolean("structure-location-async.dolphin-treasure-hunt.enabled", true);
        dolphinTreasureSearchRadius = config.getInt("structure-location-async.dolphin-treasure-hunt.search-radius", 50);
        chestExplorationMapsEnabled = config.getBoolean("structure-location-async.chest-exploration-maps.enabled", true);
        chestExplorationLootTables = new HashSet<>(config.getStringList("structure-location-async.chest-exploration-maps.loot-tables"));
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
    }

    private void loadDataPackOptimization(FileConfiguration config) {
        dataPackOptimizationEnabled = config.getBoolean("datapack-optimization.enabled", true);
        dataPackFileLoadThreads = config.getInt("datapack-optimization.file-load-threads", 4);
        dataPackZipProcessThreads = config.getInt("datapack-optimization.zip-process-threads", 2);
        dataPackBatchSize = config.getInt("datapack-optimization.batch-size", 100);
        dataPackCacheExpirationMinutes = config.getLong("datapack-optimization.cache-expiration-minutes", 30L);
        dataPackMaxFileCacheSize = config.getInt("datapack-optimization.max-file-cache-size", 1000);
        dataPackMaxFileSystemCacheSize = config.getInt("datapack-optimization.max-filesystem-cache-size", 50);
    }

    private void loadCommandOptimization(FileConfiguration config) {
        executeCommandInactiveSkipEnabled = config.getBoolean("datapack-optimization.execute-inactive-skip.enabled", false);
        executeCommandSkipLevel = config.getInt("datapack-optimization.execute-inactive-skip.skip-level", 2);
        executeCommandSimulationDistanceMultiplier = config.getDouble("datapack-optimization.execute-inactive-skip.simulation-distance-multiplier", 1.0);
        executeCommandCacheDurationMs = config.getLong("datapack-optimization.execute-inactive-skip.cache-duration-ms", 1000L);
        executeCommandWhitelistTypes = new HashSet<>(config.getStringList("datapack-optimization.execute-inactive-skip.whitelist-types"));
        executeCommandDebugEnabled = config.getBoolean("performance.debug-logging.modules.execute-command", false);

        commandDeduplicationEnabled = config.getBoolean("datapack-optimization.command-deduplication.enabled", true);
        commandDeduplicationDebugEnabled = config.getBoolean("performance.debug-logging.modules.command-deduplication", false);
    }

    public void validate(java.util.logging.Logger logger) {
        if (structureLocationThreads < 1) {
            logger.warning("Structure location threads cannot be less than 1, setting to 1");
            structureLocationThreads = 1;
        }
        if (structureLocationThreads > 8) {
            logger.warning("Structure location threads cannot be more than 8, setting to 8");
            structureLocationThreads = 8;
        }
        if (locateCommandSearchRadius < 10) locateCommandSearchRadius = 10;
        if (locateCommandSearchRadius > 1000) locateCommandSearchRadius = 1000;
        if (villagerMapGenerationTimeoutSeconds < 5) villagerMapGenerationTimeoutSeconds = 5;
        if (villagerMapGenerationTimeoutSeconds > 300) villagerMapGenerationTimeoutSeconds = 300;
        if (dolphinTreasureSearchRadius < 10) dolphinTreasureSearchRadius = 10;
        if (dolphinTreasureSearchRadius > 200) dolphinTreasureSearchRadius = 200;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Set is intentionally stored by reference for configuration")
    public void setZeroDelayFactoryEntities(Set<String> entities) {
        this.zeroDelayFactoryEntities = entities;
    }

    public String getLanguage() { return language; }
    public int getConfigVersion() { return configVersion; }

    public String getSeedEncryptionScheme() { return seedEncryptionScheme; }
    public boolean isSeedEncryptionEnabled() { return seedEncryptionEnabled; }
    public boolean isSeedEncryptionProtectStructures() { return seedEncryptionProtectStructures; }
    public boolean isSeedEncryptionProtectOres() { return seedEncryptionProtectOres; }
    public boolean isSeedEncryptionProtectSlimes() { return seedEncryptionProtectSlimes; }
    public boolean isSeedEncryptionProtectBiomes() { return seedEncryptionProtectBiomes; }
    public int getSecureSeedBits() { return secureSeedBits; }
    public int getQuantumSeedEncryptionLevel() { return quantumSeedEncryptionLevel; }
    public String getQuantumSeedPrivateKeyFile() { return quantumSeedPrivateKeyFile; }
    public boolean isQuantumSeedEnableTimeDecay() { return quantumSeedEnableTimeDecay; }
    public int getQuantumSeedCacheSize() { return quantumSeedCacheSize; }
    public boolean isQuantumSeedEnabled() { return seedEncryptionEnabled && "quantum".equalsIgnoreCase(seedEncryptionScheme); }

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

    public boolean isZeroDelayFactoryOptimizationEnabled() { return zeroDelayFactoryOptimizationEnabled; }
    public String getZeroDelayFactoryEntitiesConfigFile() { return zeroDelayFactoryEntitiesConfigFile; }
    public Set<String> getZeroDelayFactoryEntities() { return zeroDelayFactoryEntities; }

    public boolean isBlockEntityParallelTickEnabled() { return blockEntityParallelTickEnabled; }
    public int getBlockEntityParallelMinBlockEntities() { return blockEntityParallelMinBlockEntities; }
    public int getBlockEntityParallelBatchSize() { return blockEntityParallelBatchSize; }
    public boolean isBlockEntityParallelProtectContainers() { return blockEntityParallelProtectContainers; }
    public int getBlockEntityParallelTimeoutMs() { return blockEntityParallelTimeoutMs; }

    public boolean isHopperOptimizationEnabled() { return hopperOptimizationEnabled; }
    public int getHopperCacheExpireTime() { return hopperCacheExpireTime; }

    public boolean isMinecartOptimizationEnabled() { return minecartOptimizationEnabled; }
    public int getMinecartTickInterval() { return minecartTickInterval; }

    public boolean isStructureLocationAsyncEnabled() { return structureLocationAsyncEnabled; }
    public int getStructureLocationThreads() { return structureLocationThreads; }
    public boolean isLocateCommandEnabled() { return locateCommandEnabled; }
    public int getLocateCommandSearchRadius() { return locateCommandSearchRadius; }
    public boolean isLocateCommandSkipKnownStructures() { return locateCommandSkipKnownStructures; }
    public boolean isVillagerTradeMapsEnabled() { return villagerTradeMapsEnabled; }
    public int getVillagerTradeMapsSearchRadius() { return villagerTradeMapsSearchRadius; }
    public boolean isVillagerTradeMapsSkipKnownStructures() { return villagerTradeMapsSkipKnownStructures; }
    public Set<String> getVillagerTradeMapTypes() { return villagerTradeMapTypes; }
    public int getVillagerMapGenerationTimeoutSeconds() { return villagerMapGenerationTimeoutSeconds; }
    public boolean isDolphinTreasureHuntEnabled() { return dolphinTreasureHuntEnabled; }
    public int getDolphinTreasureSearchRadius() { return dolphinTreasureSearchRadius; }
    public boolean isChestExplorationMapsEnabled() { return chestExplorationMapsEnabled; }
    public Set<String> getChestExplorationLootTables() { return chestExplorationLootTables; }

    public boolean isStructureAlgorithmOptimizationEnabled() { return structureAlgorithmOptimizationEnabled; }
    public String getStructureSearchPattern() { return structureSearchPattern; }
    public boolean isStructureCachingEnabled() { return structureCachingEnabled; }
    public boolean isBiomeAwareSearchEnabled() { return biomeAwareSearchEnabled; }
    public int getStructureCacheMaxSize() { return structureCacheMaxSize; }
    public long getStructureCacheExpirationMinutes() { return structureCacheExpirationMinutes; }

    public boolean isDataPackOptimizationEnabled() { return dataPackOptimizationEnabled; }
    public int getDataPackFileLoadThreads() { return dataPackFileLoadThreads; }
    public int getDataPackZipProcessThreads() { return dataPackZipProcessThreads; }
    public int getDataPackBatchSize() { return dataPackBatchSize; }
    public long getDataPackCacheExpirationMinutes() { return dataPackCacheExpirationMinutes; }
    public int getDataPackMaxFileCacheSize() { return dataPackMaxFileCacheSize; }
    public int getDataPackMaxFileSystemCacheSize() { return dataPackMaxFileSystemCacheSize; }

    public boolean isExecuteCommandInactiveSkipEnabled() { return executeCommandInactiveSkipEnabled; }
    public int getExecuteCommandSkipLevel() { return executeCommandSkipLevel; }
    public double getExecuteCommandSimulationDistanceMultiplier() { return executeCommandSimulationDistanceMultiplier; }
    public long getExecuteCommandCacheDurationMs() { return executeCommandCacheDurationMs; }
    public Set<String> getExecuteCommandWhitelistTypes() { return executeCommandWhitelistTypes; }
    public boolean isExecuteCommandDebugEnabled() { return executeCommandDebugEnabled; }

    public boolean isCommandDeduplicationEnabled() { return commandDeduplicationEnabled; }
    public boolean isCommandDeduplicationDebugEnabled() { return commandDeduplicationDebugEnabled; }
}
