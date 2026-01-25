package org.virgil.akiasync.mixin.bridge.sub;

public interface TNTBridge {

    boolean isTNTOptimizationEnabled();
    int getTNTThreads();
    int getTNTMaxBlocks();
    long getTNTTimeoutMicros();
    int getTNTBatchSize();
    boolean isTNTDebugEnabled();

    boolean isTNTVanillaCompatibilityEnabled();
    boolean isTNTUseVanillaPower();
    boolean isTNTUseVanillaFireLogic();
    boolean isTNTUseVanillaDamageCalculation();
    boolean isTNTUseFullRaycast();
    boolean isTNTUseVanillaBlockDestruction();
    boolean isTNTUseVanillaDrops();

    boolean isTNTUseSakuraDensityCache();
    boolean isTNTUseVectorizedAABB();
    boolean isTNTUseUnifiedEngine();
    boolean isTNTMergeEnabled();
    double getTNTMergeRadius();
    int getTNTMaxFuseDifference();
    float getTNTMergedPowerMultiplier();
    float getTNTMaxPower();
    boolean isTNTCacheEnabled();
    int getTNTCacheExpiryTicks();

    boolean isExplosionBlockUpdateOptimizationEnabled();
    int getExplosionBlockChangeThreshold();

    java.util.List<net.minecraft.core.BlockPos> fireEntityExplodeEvent(
        net.minecraft.server.level.ServerLevel level,
        net.minecraft.world.entity.Entity entity,
        net.minecraft.world.phys.Vec3 center,
        java.util.List<net.minecraft.core.BlockPos> blocks,
        float yield
    );
}
