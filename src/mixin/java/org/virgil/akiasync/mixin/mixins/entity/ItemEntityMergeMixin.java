package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SuppressWarnings("unused")
@Mixin(ItemEntity.class)
public abstract class ItemEntityMergeMixin {

    @Shadow
    public abstract void tryToMerge(ItemEntity other);

    @Unique
    private static volatile boolean enabled;
    @Unique
    private static volatile int mergeInterval;
    @Unique
    private static volatile int minNearbyItems;
    @Unique
    private static volatile double mergeRange;
    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private int aki$mergeTickCounter = 0;
    @Unique
    private List<ItemEntity> aki$cachedNearbyItems = null;
    @Unique
    private long aki$lastCacheTime = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void optimizeMerge(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initMergeOptimization();
        }
        if (!enabled) return;

        ItemEntity self = (ItemEntity) (Object) this;

        if (akiasync$isVirtualEntity(self)) {
            return;
        }

        if (akiasync$isInDangerousEnvironment(self)) {
            return;
        }

        aki$mergeTickCounter++;

        if (aki$mergeTickCounter % mergeInterval != 0) {
            return;
        }

        aki$mergeTickCounter = 0;
        akiasync$smartMerge(self);
    }

    @Unique
    private void akiasync$smartMerge(ItemEntity self) {
        long currentTime = System.currentTimeMillis();
        if (aki$cachedNearbyItems == null || currentTime - aki$lastCacheTime > 50) {
            aki$cachedNearbyItems = akiasync$getNearbyItems(self);
            aki$lastCacheTime = currentTime;
        }

        if (aki$cachedNearbyItems.size() < minNearbyItems) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync-ItemMerge] Skipping merge: nearby items (%d) < threshold (%d)",
                    aki$cachedNearbyItems.size(), minNearbyItems);
            }
            return;
        }

        for (ItemEntity other : aki$cachedNearbyItems) {
            if (other != self && !other.isRemoved()) {
                try {
                    this.tryToMerge(other);
                } catch (Throwable t) {
                }
            }
        }
    }

    @Unique
    private List<ItemEntity> akiasync$getNearbyItems(ItemEntity self) {
        AABB searchBox = self.getBoundingBox().inflate(mergeRange);

        return self.level().getEntitiesOfClass(
            ItemEntity.class,
            searchBox,
            item -> item != self && !item.isRemoved()
        );
    }

    @Unique
    private boolean akiasync$isInDangerousEnvironment(ItemEntity item) {
        if (item.isInLava() || item.isOnFire() || item.getRemainingFireTicks() > 0) {
            return true;
        }

        net.minecraft.core.BlockPos pos = item.blockPosition();
        net.minecraft.world.level.block.state.BlockState state = item.level().getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.LayeredCauldronBlock ||
            state.getBlock() instanceof net.minecraft.world.level.block.LavaCauldronBlock) {
            return true;
        }

        return false;
    }

    @Unique
    private boolean akiasync$isVirtualEntity(ItemEntity entity) {
        if (entity == null) return false;

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                return bridge.isVirtualEntity(entity);
            }
        } catch (Throwable t) {
            return true;
        }

        return false;
    }

    @Unique
    private static synchronized void akiasync$initMergeOptimization() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (bridge != null) {
            enabled = bridge.isItemEntityMergeOptimizationEnabled();
            mergeInterval = bridge.getItemEntityMergeInterval();
            minNearbyItems = bridge.getItemEntityMinNearbyItems();
            mergeRange = bridge.getItemEntityMergeRange();
        } else {
            enabled = true;
            mergeInterval = 5;
            minNearbyItems = 3;
            mergeRange = 1.5;
        }

        initialized = true;

        if (bridge != null) {
            bridge.debugLog("[AkiAsync] ItemEntityMergeMixin initialized: enabled=" + enabled +
                ", mergeInterval=" + mergeInterval + ", minNearbyItems=" + minNearbyItems +
                ", mergeRange=" + mergeRange);
        }
    }
}
