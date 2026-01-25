package org.virgil.akiasync.config.sections;

import org.bukkit.configuration.file.FileConfiguration;

public class TNTConfig {

    private boolean tntOptimizationEnabled;
    private boolean tntDebugEnabled;
    private int tntThreads;
    private int tntMaxBlocks;
    private long tntTimeoutMicros;
    private int tntBatchSize;

    private boolean tntVanillaCompatibilityEnabled;
    private boolean tntUseVanillaPower;
    private boolean tntUseVanillaFireLogic;
    private boolean tntUseVanillaDamageCalculation;
    private boolean tntUseFullRaycast;
    private boolean tntUseVanillaBlockDestruction;
    private boolean tntUseVanillaDrops;

    private boolean tntUseSakuraDensityCache;
    private boolean tntUseVectorizedAABB;
    private boolean tntUseUnifiedEngine;

    private boolean tntMergeEnabled;
    private double tntMergeRadius;
    private int tntMaxFuseDifference;
    private float tntMergedPowerMultiplier;
    private float tntMaxPower;

    private boolean tntCacheEnabled;
    private boolean tntUseOptimizedCache;
    private int tntCacheExpiryTicks;
    private boolean tntCacheWarmupEnabled;
    private boolean tntPrecomputedShapeEnabled;
    private boolean tntUseOcclusionDetection;
    private double tntOcclusionThreshold;
    private boolean tntBatchCollisionEnabled;
    private int tntBatchUnrollFactor;

    public void load(FileConfiguration config) {
        tntOptimizationEnabled = config.getBoolean("tnt-explosion-optimization.enabled", true);
        tntDebugEnabled = config.getBoolean("performance.debug-logging.modules.tnt", false);
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

        tntUseSakuraDensityCache = config.getBoolean("tnt-explosion-optimization.density-cache.enabled", true);
        tntUseVectorizedAABB = config.getBoolean("tnt-explosion-optimization.vectorized-aabb.enabled", true);
        tntUseUnifiedEngine = config.getBoolean("tnt-explosion-optimization.unified-engine.enabled", true);

        tntMergeEnabled = config.getBoolean("tnt-explosion-optimization.entity-merge.enabled", false);
        tntMergeRadius = config.getDouble("tnt-explosion-optimization.entity-merge.radius", 1.5);
        tntMaxFuseDifference = config.getInt("tnt-explosion-optimization.entity-merge.max-fuse-difference", 5);
        tntMergedPowerMultiplier = (float) config.getDouble("tnt-explosion-optimization.entity-merge.power-multiplier", 0.5);
        tntMaxPower = (float) config.getDouble("tnt-explosion-optimization.entity-merge.max-power", 12.0);

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

    public void validate(java.util.logging.Logger logger) {
        if (tntThreads < 1) {
            logger.warning("TNT threads cannot be less than 1, setting to 1");
            tntThreads = 1;
        }
        if (tntThreads > 32) {
            logger.warning("TNT threads cannot be more than 32, setting to 32");
            tntThreads = 32;
        }
        if (tntMaxBlocks < 256) tntMaxBlocks = 256;
        if (tntMaxBlocks > 16384) tntMaxBlocks = 16384;
        if (tntTimeoutMicros < 10) tntTimeoutMicros = 10;
        if (tntTimeoutMicros > 10000) tntTimeoutMicros = 10000;
        if (tntBatchSize < 8) tntBatchSize = 8;
        if (tntBatchSize > 256) tntBatchSize = 256;
    }

    public boolean isTntOptimizationEnabled() { return tntOptimizationEnabled; }
    public boolean isTntDebugEnabled() { return tntDebugEnabled; }
    public int getTntThreads() { return tntThreads; }
    public int getTntMaxBlocks() { return tntMaxBlocks; }
    public long getTntTimeoutMicros() { return tntTimeoutMicros; }
    public int getTntBatchSize() { return tntBatchSize; }

    public boolean isTntVanillaCompatibilityEnabled() { return tntVanillaCompatibilityEnabled; }
    public boolean isTntUseVanillaPower() { return tntUseVanillaPower; }
    public boolean isTntUseVanillaFireLogic() { return tntUseVanillaFireLogic; }
    public boolean isTntUseVanillaDamageCalculation() { return tntUseVanillaDamageCalculation; }
    public boolean isTntUseFullRaycast() { return tntUseFullRaycast; }
    public boolean isTntUseVanillaBlockDestruction() { return tntUseVanillaBlockDestruction; }
    public boolean isTntUseVanillaDrops() { return tntUseVanillaDrops; }

    public boolean isTntUseSakuraDensityCache() { return tntUseSakuraDensityCache; }
    public boolean isTntUseVectorizedAABB() { return tntUseVectorizedAABB; }
    public boolean isTntUseUnifiedEngine() { return tntUseUnifiedEngine; }

    public boolean isTntMergeEnabled() { return tntMergeEnabled; }
    public double getTntMergeRadius() { return tntMergeRadius; }
    public int getTntMaxFuseDifference() { return tntMaxFuseDifference; }
    public float getTntMergedPowerMultiplier() { return tntMergedPowerMultiplier; }
    public float getTntMaxPower() { return tntMaxPower; }

    public boolean isTntCacheEnabled() { return tntCacheEnabled; }
    public boolean isTntUseOptimizedCache() { return tntUseOptimizedCache; }
    public int getTntCacheExpiryTicks() { return tntCacheExpiryTicks; }
    public boolean isTntCacheWarmupEnabled() { return tntCacheWarmupEnabled; }
    public boolean isTntPrecomputedShapeEnabled() { return tntPrecomputedShapeEnabled; }
    public boolean isTntUseOcclusionDetection() { return tntUseOcclusionDetection; }
    public double getTntOcclusionThreshold() { return tntOcclusionThreshold; }
    public boolean isTntBatchCollisionEnabled() { return tntBatchCollisionEnabled; }
    public int getTntBatchUnrollFactor() { return tntBatchUnrollFactor; }
}
