package org.virgil.akiasync.mixin.bridge.sub;

public interface BlockEntityBridge {

    boolean isZeroDelayFactoryOptimizationEnabled();
    java.util.Set<String> getZeroDelayFactoryEntities();

    boolean isBlockEntityParallelTickEnabled();
    int getBlockEntityParallelMinBlockEntities();
    int getBlockEntityParallelBatchSize();
    boolean isBlockEntityParallelProtectContainers();
    int getBlockEntityParallelTimeoutMs();

    boolean isHopperOptimizationEnabled();
    int getHopperCacheExpireTime();

    boolean isMinecartOptimizationEnabled();
    int getMinecartTickInterval();

    boolean isFurnaceRecipeCacheEnabled();
    int getFurnaceRecipeCacheSize();
    boolean isFurnaceCacheApplyToBlastFurnace();
    boolean isFurnaceCacheApplyToSmoker();
    boolean isFurnaceFixBurnTimeBug();

    boolean isCraftingRecipeCacheEnabled();
    int getCraftingRecipeCacheSize();
    boolean isCraftingOptimizeBatchCrafting();
    boolean isCraftingReduceNetworkTraffic();

    boolean isMinecartCauldronDestructionEnabled();
}
