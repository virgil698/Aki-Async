package org.virgil.akiasync.mixin.mixins.entity;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

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
    private static volatile int maxQueryResults = 20;
    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private int aki$mergeTickCounter = 0;
    @Unique
    private List<ItemEntity> aki$cachedNearbyItems = null;
    @Unique
    private int aki$lastCacheTick = 0;

    @Inject(method = "mergeWithNeighbours", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaMerge(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initMergeOptimization();
        }
        
        if (enabled && cancelVanillaMerge && aki$cachedNearbyItems != null) {
            ci.cancel(); 
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void optimizeMerge(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initMergeOptimization();
        }
        if (!enabled) return;

        
        aki$mergeTickCounter++;
        if (aki$mergeTickCounter % mergeInterval != 0) {
            return;
        }
        aki$mergeTickCounter = 0;

        ItemEntity self = (ItemEntity) (Object) this;

        
        if (akiasync$isVirtualEntity(self)) {
            return;
        }

        if (akiasync$isInDangerousEnvironment(self)) {
            return;
        }

        akiasync$smartMerge(self);
    }

    @Unique
    private void akiasync$smartMerge(ItemEntity self) {
        
        if (!akiasync$isTickThread(self)) {
            return;
        }

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
                
                if (!akiasync$isTickThread(other)) {
                    continue;
                }
                try {
                    this.tryToMerge(other);
                    mergedCount++;
                    
                    if (!highDensity && mergedCount >= 5) break;
                } catch (Throwable t) {
                    org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                        "ItemEntityMerge", "tryMerge",
                        t instanceof Exception ? (Exception) t : new RuntimeException(t));
                }
            }
        }
    }
    
    @Unique
    private boolean akiasync$isTickThread(ItemEntity entity) {
        
        
        return true;
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
                    
                    
                    List<ItemEntity> result = new ArrayList<>(Math.min(entities.size(), maxQueryResults));
                    for (net.minecraft.world.entity.Entity entity : entities) {
                        if (entity instanceof ItemEntity item && 
                            item != self && 
                            !item.isRemoved()) {
                            result.add(item);
                            
                            if (result.size() >= maxQueryResults) {
                                break;
                            }
                        }
                    }
                    return result;
                }
            }
        } catch (Throwable t) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ItemEntityMerge", "getNearbyItemsOptimized",
                t instanceof Exception ? (Exception) t : new RuntimeException(t));
        }
        
        AABB searchBox = boundingBox.inflate(mergeRange);
        if (searchBox == null) {
            return java.util.Collections.emptyList();
        }

        
        List<ItemEntity> result = self.level().getEntitiesOfClass(
            ItemEntity.class,
            searchBox,
            item -> item != self && !item.isRemoved()
        );
        
        
        if (result.size() > maxQueryResults) {
            return result.subList(0, maxQueryResults);
        }
        
        return result;
    }

    @Unique
    private boolean akiasync$isInDangerousEnvironment(ItemEntity item) {
        if (item.isInLava() || item.isOnFire() || item.getRemainingFireTicks() > 0) {
            return true;
        }

        if (akiasync$isInPortal(item)) {
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

        if (state.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock ||
            state.getBlock() instanceof net.minecraft.world.level.block.EndPortalBlock) {
            return true;
        }

        return false;
    }

    @Unique
    private boolean akiasync$isInPortal(ItemEntity item) {
        try {
            net.minecraft.core.BlockPos pos = item.blockPosition();
            if (pos == null) return false;

            net.minecraft.world.level.block.state.BlockState state = item.level().getBlockState(pos);
            if (state == null) return false;

            return state.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock ||
                   state.getBlock() instanceof net.minecraft.world.level.block.EndPortalBlock;
        } catch (Throwable t) {
            return false;
        }
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
