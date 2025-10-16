package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.item.ItemEntity;

/**
 * ItemEntity hardcore optimization (v5.0 Full - Mojang namespace enabled)
 * 
 * v5.0 - Second performance bottleneck for redstone farms
 * With paperweight-mappings-namespace=mojang, all methods are now accessible
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = ItemEntity.class, priority = 988)
public class ItemEntityOptimizationMixin {
    
    @Unique private static volatile boolean enabled;
    @Unique private static volatile int tickInterval;
    @Unique private static volatile int minNearbyItems;
    @Unique private static volatile boolean init = false;
    @Unique private long aki$nextTick = 0;
    
    // 1. Skip empty region tick (no collision)
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$skipEmptyRegion(CallbackInfo ci) {
        if (!init) { aki$init(); }
        if (!enabled) return;
        
        ItemEntity item = (ItemEntity) (Object) this;
        
        // Skip tick if in air and no nearby entities
        if (item.isInWater() == false && item.onGround() == false) {
            // In air, check if empty region
            if (item.level().noCollision(item.getBoundingBox())) {
                // Throttle tick when falling in empty space
                if (item.level().getGameTime() < aki$nextTick) {
                    ci.cancel();
                    return;
                }
                aki$nextTick = item.level().getGameTime() + tickInterval;
            }
        }
    }
    
    // 2. Skip sparse merge (when nearby < 3 items)
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void aki$skipSparseMerge(ItemEntity other, CallbackInfo ci) {
        if (!enabled) return;
        ItemEntity item = (ItemEntity) (Object) this;
        
        // Count nearby ItemEntities
        int nearby = item.level().getEntitiesOfClass(ItemEntity.class, 
            item.getBoundingBox().inflate(0.5D)).size();
        
        if (nearby < minNearbyItems) {
            ci.cancel();  // Skip merge check if too sparse
        }
    }
    
    @Unique private static synchronized void aki$init() {
        if (init) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        enabled = bridge != null && bridge.isItemEntityOptimizationEnabled();
        tickInterval = bridge != null ? bridge.getItemEntityAgeInterval() : 10;
        minNearbyItems = bridge != null ? bridge.getItemEntityMinNearbyItems() : 3;
        init = true;
        System.out.println("[AkiAsync] ItemEntityOptimizationMixin (v5.0 Full) initialized: enabled=" + enabled + 
            ", tickInterval=" + tickInterval + ", minNearbyItems=" + minNearbyItems);
    }
}

