package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Mixin(ItemEntity.class)
public abstract class ItemEntityMergeMixin {

    @Shadow
    public abstract void tryToMerge(ItemEntity other);

    @Unique
    private static volatile boolean enabled;
    @Unique
    private static volatile boolean cancelVanillaMerge;
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
    private int aki$lastCacheTick = 0;

    /**
     * 取消原版的合并逻辑，避免与我们的优化重复
     */
    @Inject(method = "mergeWithNeighbours", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaMerge(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initMergeOptimization();
        }
        if (enabled && cancelVanillaMerge) {
            ci.cancel(); 
        }
    }

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

        int currentTick = self.tickCount;
        if (aki$cachedNearbyItems == null || currentTick - aki$lastCacheTick >= 3) {
            aki$cachedNearbyItems = akiasync$getNearbyItems(self);
            aki$lastCacheTick = currentTick;
        }

        boolean highDensity = aki$cachedNearbyItems.size() >= 10;
        
        if (!highDensity && aki$cachedNearbyItems.size() < minNearbyItems) {
            return;
        }

        int mergedCount = 0;
        for (ItemEntity other : aki$cachedNearbyItems) {
            if (other != self && !other.isRemoved()) {
                try {
                    this.tryToMerge(other);
                    mergedCount++;
                    
                    if (!highDensity && mergedCount >= 5) break;
                } catch (Throwable t) {
                }
            }
        }
    }

    @Unique
    private List<ItemEntity> akiasync$getNearbyItems(ItemEntity self) {
        AABB boundingBox = self.getBoundingBox();
        if (boundingBox == null) {
            return java.util.Collections.emptyList();
        }
        
        try {
            org.virgil.akiasync.mixin.util.EntitySliceGrid grid = 
                org.virgil.akiasync.mixin.util.EntitySliceGridManager.getSliceGrid(self.level());
            
            if (grid != null) {
                
                AABB searchBox = boundingBox.inflate(mergeRange);
                if (searchBox != null) {
                    List<net.minecraft.world.entity.Entity> entities = grid.queryRange(searchBox);
                    
                    List<ItemEntity> result = new ArrayList<>();
                    for (net.minecraft.world.entity.Entity entity : entities) {
                        if (entity instanceof ItemEntity item && 
                            item != self && 
                            !item.isRemoved()) {
                            result.add(item);
                        }
                    }
                    return result;
                }
            }
        } catch (Throwable t) {
            
        }
        
        AABB searchBox = boundingBox.inflate(mergeRange);
        if (searchBox == null) {
            return java.util.Collections.emptyList();
        }

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
        if (pos == null) {
            return false;
        }
        
        net.minecraft.world.level.block.state.BlockState state = item.level().getBlockState(pos);
        if (state == null) {
            return false;
        }
        
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
            cancelVanillaMerge = bridge.isItemEntityCancelVanillaMerge();
            mergeInterval = bridge.getItemEntityMergeInterval();
            minNearbyItems = bridge.getItemEntityMinNearbyItems();
            mergeRange = bridge.getItemEntityMergeRange();
        } else {
            enabled = true;
            cancelVanillaMerge = true;
            mergeInterval = 3; 
            minNearbyItems = 2; 
            mergeRange = 2.0; 
        }

        initialized = true;

        if (bridge != null) {
            bridge.debugLog("[AkiAsync] ItemEntityMergeMixin initialized: enabled=" + enabled +
                ", cancelVanillaMerge=" + cancelVanillaMerge +
                ", mergeInterval=" + mergeInterval + ", minNearbyItems=" + minNearbyItems +
                ", mergeRange=" + mergeRange);
        }
    }
}
