package org.virgil.akiasync.config.sections;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;

public class CollisionConfig {

    private boolean collisionOptimizationEnabled;
    private boolean collisionAggressiveMode;
    private String collisionExclusionListFile;
    private Set<String> collisionExcludedEntities;

    private boolean nativeCollisionsEnabled;
    private boolean nativeCollisionsFallbackEnabled;
    private boolean collisionBlockCacheEnabled;
    private int collisionBlockCacheSize;
    private int collisionBlockCacheExpireTicks;
    private boolean rayCollisionEnabled;
    private double rayCollisionMaxDistance;
    private boolean shapeOptimizationEnabled;
    private boolean shapePrecomputeArrays;
    private boolean shapeBlockShapeCache;
    private int shapeBlockShapeCacheSize;

    private boolean suffocationOptimizationEnabled;

    public void load(FileConfiguration config) {
        collisionOptimizationEnabled = config.getBoolean("collision-optimization.enabled", true);
        collisionAggressiveMode = config.getBoolean("collision-optimization.aggressive-mode", true);
        collisionExclusionListFile = config.getString("collision-optimization.exclusion-list-file", "entities.yml");

        nativeCollisionsEnabled = config.getBoolean("collision-optimization.native.enabled", true);
        nativeCollisionsFallbackEnabled = config.getBoolean("collision-optimization.native.fallback-enabled", true);

        collisionBlockCacheEnabled = config.getBoolean("collision-optimization.block-cache.enabled", true);
        collisionBlockCacheSize = config.getInt("collision-optimization.block-cache.cache-size", 512);
        collisionBlockCacheExpireTicks = config.getInt("collision-optimization.block-cache.expire-ticks", 600);

        rayCollisionEnabled = config.getBoolean("collision-optimization.ray-collision.enabled", true);
        rayCollisionMaxDistance = config.getDouble("collision-optimization.ray-collision.max-distance", 64.0);
        shapeOptimizationEnabled = config.getBoolean("collision-optimization.shape-optimization.enabled", true);
        shapePrecomputeArrays = config.getBoolean("collision-optimization.shape-optimization.precompute-arrays", true);
        shapeBlockShapeCache = config.getBoolean("collision-optimization.shape-optimization.block-shape-cache", true);
        shapeBlockShapeCacheSize = config.getInt("collision-optimization.shape-optimization.cache-size", 512);

        suffocationOptimizationEnabled = config.getBoolean("suffocation-optimization.enabled", true);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Set is intentionally stored by reference for configuration")
    public void setCollisionExcludedEntities(Set<String> entities) {
        this.collisionExcludedEntities = entities;
    }

    public boolean isCollisionOptimizationEnabled() { return collisionOptimizationEnabled; }
    public boolean isCollisionAggressiveMode() { return collisionAggressiveMode; }
    public String getCollisionExclusionListFile() { return collisionExclusionListFile; }
    public Set<String> getCollisionExcludedEntities() { return collisionExcludedEntities; }

    public boolean isNativeCollisionsEnabled() { return nativeCollisionsEnabled; }
    public boolean isNativeCollisionsFallbackEnabled() { return nativeCollisionsFallbackEnabled; }
    public boolean isCollisionBlockCacheEnabled() { return collisionBlockCacheEnabled; }
    public int getCollisionBlockCacheSize() { return collisionBlockCacheSize; }
    public int getCollisionBlockCacheExpireTicks() { return collisionBlockCacheExpireTicks; }
    public boolean isRayCollisionEnabled() { return rayCollisionEnabled; }
    public double getRayCollisionMaxDistance() { return rayCollisionMaxDistance; }
    public boolean isShapeOptimizationEnabled() { return shapeOptimizationEnabled; }
    public boolean isShapePrecomputeArrays() { return shapePrecomputeArrays; }
    public boolean isShapeBlockShapeCache() { return shapeBlockShapeCache; }
    public int getShapeBlockShapeCacheSize() { return shapeBlockShapeCacheSize; }

    public boolean isSuffocationOptimizationEnabled() { return suffocationOptimizationEnabled; }
}
