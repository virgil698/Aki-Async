package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.item.ItemEntity;

/**
 * ItemEntity hardcore optimization (delay tick frequency, safe method only)
 * 
 * v5.0 - Second performance bottleneck for redstone farms
 * Note: tryMerge() obfuscated by Paper, only tick() is safe to inject
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = ItemEntity.class, priority = 988)
public class ItemEntityOptimizationMixin {
    
    @Unique private static volatile boolean enabled;
    @Unique private static volatile int tickInterval;
    @Unique private static volatile boolean init = false;
    @Unique private long aki$nextTick = 0;
    
    // Delay tick frequency (reduce overall ItemEntity processing)
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$delayTick(CallbackInfo ci) {
        if (!init) { aki$init(); }
        if (!enabled) return;
        
        ItemEntity item = (ItemEntity) (Object) this;
        
        // Throttle ticking (every N ticks)
        if (item.level().getGameTime() < aki$nextTick) {
            ci.cancel();
            return;
        }
        aki$nextTick = item.level().getGameTime() + tickInterval;
    }
    
    @Unique private static synchronized void aki$init() {
        if (init) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        enabled = bridge != null && bridge.isItemEntityOptimizationEnabled();
        tickInterval = bridge != null ? bridge.getItemEntityAgeInterval() : 10;
        init = true;
        System.out.println("[AkiAsync] ItemEntityOptimizationMixin initialized: enabled=" + enabled + 
            ", tickInterval=" + tickInterval);
    }
}

