package org.virgil.akiasync.config.sections;

import org.bukkit.configuration.file.FileConfiguration;

public class PerformanceConfig {

    private int threadPoolSize;
    private boolean enableDebugLogging;
    private boolean enablePerformanceMetrics;

    private boolean smartLagCompensationEnabled;
    private double smartLagTPSThreshold;
    private boolean smartLagItemPickupDelayEnabled;
    private boolean smartLagPotionEffectsEnabled;
    private boolean smartLagTimeAccelerationEnabled;
    private boolean smartLagDebugEnabled;
    private boolean smartLagLogMissedTicks;
    private boolean smartLagLogCompensation;

    private boolean nitoriOptimizationsEnabled;
    private boolean virtualThreadEnabled;
    private boolean workStealingEnabled;
    private boolean blockPosCacheEnabled;
    private boolean optimizedCollectionsEnabled;
    private boolean bitSetPoolingEnabled;
    private boolean completableFutureOptimizationEnabled;
    private boolean nbtOptimizationEnabled;

    private boolean multiNettyEventLoopEnabled;
    private boolean palettedContainerLockRemovalEnabled;
    private boolean spawnDensityArrayEnabled;
    private boolean typeFilterableListOptimizationEnabled;
    private boolean entityTrackerLinkedHashMapEnabled;
    private boolean biomeAccessOptimizationEnabled;
    private boolean entityMoveZeroVelocityOptimizationEnabled;
    private boolean entityTrackerDistanceCacheEnabled;

    public void load(FileConfiguration config) {
        int configuredThreadPoolSize = config.getInt("general-thread-pool.size", 0);
        if (configuredThreadPoolSize <= 0) {
            int cpuCores = Runtime.getRuntime().availableProcessors();
            threadPoolSize = Math.max(2, cpuCores / 4);
        } else {
            threadPoolSize = configuredThreadPoolSize;
        }

        enableDebugLogging = config.getBoolean("performance.debug-logging", false);
        enablePerformanceMetrics = config.getBoolean("performance.enable-metrics", true);

        smartLagCompensationEnabled = config.getBoolean("smart-lag-compensation.enabled", true);
        smartLagTPSThreshold = config.getDouble("smart-lag-compensation.tps-threshold", 18.0);
        smartLagItemPickupDelayEnabled = config.getBoolean("smart-lag-compensation.item-pickup-delay.enabled", true);
        smartLagPotionEffectsEnabled = config.getBoolean("smart-lag-compensation.potion-effects.enabled", false);
        smartLagTimeAccelerationEnabled = config.getBoolean("smart-lag-compensation.time-acceleration.enabled", false);
        smartLagDebugEnabled = config.getBoolean("performance.debug-logging.smart-lag-compensation.enabled", false);
        smartLagLogMissedTicks = config.getBoolean("performance.debug-logging.smart-lag-compensation.log-missed-ticks", false);
        smartLagLogCompensation = config.getBoolean("performance.debug-logging.smart-lag-compensation.log-compensation", false);

        nitoriOptimizationsEnabled = config.getBoolean("advanced-optimizations.virtual-threads", true) ||
                                      config.getBoolean("advanced-optimizations.work-stealing", true);
        virtualThreadEnabled = config.getBoolean("advanced-optimizations.virtual-threads", true);
        workStealingEnabled = config.getBoolean("advanced-optimizations.work-stealing", true);

        blockPosCacheEnabled = config.getBoolean("math-optimization.c2me-optimizations.blockpos-cache", true);
        optimizedCollectionsEnabled = config.getBoolean("math-optimization.c2me-optimizations.optimized-collections", true);
        bitSetPoolingEnabled = config.getBoolean("math-optimization.c2me-optimizations.bitset-pooling.enabled", true);
        completableFutureOptimizationEnabled = config.getBoolean("math-optimization.c2me-optimizations.completable-future-optimization.enabled", true);
        nbtOptimizationEnabled = config.getBoolean("recipe-optimization.c2me-nbt-optimization.enabled", true);

        multiNettyEventLoopEnabled = config.getBoolean("advanced-optimizations.multi-netty-event-loop", true);
        palettedContainerLockRemovalEnabled = config.getBoolean("advanced-optimizations.paletted-container-lock-removal", true);
        spawnDensityArrayEnabled = config.getBoolean("advanced-optimizations.spawn-density-array", true);
        typeFilterableListOptimizationEnabled = config.getBoolean("advanced-optimizations.type-filterable-list", true);
        entityTrackerLinkedHashMapEnabled = config.getBoolean("advanced-optimizations.entity-tracker-linked-hashmap", true);
        biomeAccessOptimizationEnabled = config.getBoolean("advanced-optimizations.biome-access-optimization", true);
        entityMoveZeroVelocityOptimizationEnabled = config.getBoolean("advanced-optimizations.entity-move-zero-velocity", true);
        entityTrackerDistanceCacheEnabled = config.getBoolean("advanced-optimizations.entity-tracker-distance-cache", true);
    }

    public void validate(java.util.logging.Logger logger) {
        if (threadPoolSize < 1) {
            logger.warning("Thread pool size cannot be less than 1, setting to 1");
            threadPoolSize = 1;
        }
        if (threadPoolSize > 32) {
            logger.warning("Thread pool size cannot be more than 32, setting to 32");
            threadPoolSize = 32;
        }
    }

    public void validateNitori(java.util.logging.Logger logger) {
        if (virtualThreadEnabled) {
            int javaVersion = getJavaMajorVersion();
            if (javaVersion < 19) {
                logger.warning("==========================================");
                logger.warning("  NITORI VIRTUAL THREAD WARNING");
                logger.warning("==========================================");
                logger.warning("Virtual Thread is enabled but your Java version (" + javaVersion + ") doesn't support it.");
                logger.warning("Virtual Thread requires Java 19+ (preview) or Java 21+ (stable).");
                logger.warning("The plugin will automatically fall back to regular threads.");
                logger.warning("Consider upgrading to Java 21+ for better performance.");
                logger.warning("==========================================");
            } else if (javaVersion >= 19 && javaVersion < 21) {
                logger.info("Virtual Thread enabled with Java " + javaVersion + " (preview feature)");
            } else {
                logger.info("Virtual Thread enabled with Java " + javaVersion + " (stable feature)");
            }
        }

        if (!nitoriOptimizationsEnabled) {
            logger.info("Nitori-style optimizations are disabled. You may miss some performance improvements.");
        } else {
            int enabledOptimizations = 0;
            if (virtualThreadEnabled) enabledOptimizations++;
            if (workStealingEnabled) enabledOptimizations++;
            if (blockPosCacheEnabled) enabledOptimizations++;
            if (optimizedCollectionsEnabled) enabledOptimizations++;

            logger.info("Nitori-style optimizations enabled: " + enabledOptimizations + "/4 features active");
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

    public int getThreadPoolSize() { return threadPoolSize; }
    public boolean isDebugLoggingEnabled() { return enableDebugLogging; }
    public boolean isPerformanceMetricsEnabled() { return enablePerformanceMetrics; }

    public void setDebugLoggingEnabled(boolean enabled) {
        this.enableDebugLogging = enabled;
    }

    public boolean isSmartLagCompensationEnabled() { return smartLagCompensationEnabled; }
    public double getSmartLagTPSThreshold() { return smartLagTPSThreshold; }
    public boolean isSmartLagItemPickupDelayEnabled() { return smartLagItemPickupDelayEnabled; }
    public boolean isSmartLagPotionEffectsEnabled() { return smartLagPotionEffectsEnabled; }
    public boolean isSmartLagTimeAccelerationEnabled() { return smartLagTimeAccelerationEnabled; }
    public boolean isSmartLagDebugEnabled() { return smartLagDebugEnabled || enableDebugLogging; }
    public boolean isSmartLagLogMissedTicks() { return smartLagLogMissedTicks && isSmartLagDebugEnabled(); }
    public boolean isSmartLagLogCompensation() { return smartLagLogCompensation && isSmartLagDebugEnabled(); }

    public boolean isNitoriOptimizationsEnabled() { return nitoriOptimizationsEnabled; }
    public boolean isVirtualThreadEnabled() { return virtualThreadEnabled; }
    public boolean isWorkStealingEnabled() { return workStealingEnabled; }
    public boolean isBlockPosCacheEnabled() { return blockPosCacheEnabled; }
    public boolean isOptimizedCollectionsEnabled() { return optimizedCollectionsEnabled; }
    public boolean isBitSetPoolingEnabled() { return bitSetPoolingEnabled; }
    public boolean isCompletableFutureOptimizationEnabled() { return completableFutureOptimizationEnabled; }
    public boolean isNbtOptimizationEnabled() { return nbtOptimizationEnabled; }

    public boolean isMultiNettyEventLoopEnabled() { return multiNettyEventLoopEnabled; }
    public boolean isPalettedContainerLockRemovalEnabled() { return palettedContainerLockRemovalEnabled; }
    public boolean isSpawnDensityArrayEnabled() { return spawnDensityArrayEnabled; }
    public boolean isTypeFilterableListOptimizationEnabled() { return typeFilterableListOptimizationEnabled; }
    public boolean isEntityTrackerLinkedHashMapEnabled() { return entityTrackerLinkedHashMapEnabled; }
    public boolean isBiomeAccessOptimizationEnabled() { return biomeAccessOptimizationEnabled; }
    public boolean isEntityMoveZeroVelocityOptimizationEnabled() { return entityMoveZeroVelocityOptimizationEnabled; }
    public boolean isEntityTrackerDistanceCacheEnabled() { return entityTrackerDistanceCacheEnabled; }
}
