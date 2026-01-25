package org.virgil.akiasync.mixin.bridge.sub;

public interface CollisionBridge {

    boolean isCollisionOptimizationEnabled();
    boolean isCollisionAggressiveMode();
    java.util.Set<String> getCollisionExcludedEntities();

    boolean isNativeCollisionsEnabled();
    boolean isNativeCollisionsFallbackEnabled();

    boolean isCollisionBlockCacheEnabled();
    int getCollisionBlockCacheSize();
    int getCollisionBlockCacheExpireTicks();

    boolean isRayCollisionEnabled();
    double getRayCollisionMaxDistance();

    boolean isShapeOptimizationEnabled();
    boolean isShapePrecomputeArrays();
    boolean isShapeBlockShapeCache();
    int getShapeBlockShapeCacheSize();

    boolean isSuffocationOptimizationEnabled();
    boolean isFastRayTraceEnabled();

    boolean isPortalSuffocationCheckDisabled();
    boolean isShulkerBulletSelfHitFixEnabled();
}
