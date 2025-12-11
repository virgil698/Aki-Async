package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.TPSTracker;

@Mixin(ItemEntity.class)
public class ItemEntityPickupDelayCompensationMixin {
    
    @Shadow
    private int pickupDelay;
    
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = false;
    @Unique
    private static volatile double tpsThreshold = 18.0;
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void compensatePickupDelay(CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) return;
        if (pickupDelay == 0) return;
        
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide) return;
        
        try {
            TPSTracker tracker = TPSTracker.getInstance();
            double currentTPS = tracker.getMostAccurateTPS();
            
            if (currentTPS < tpsThreshold) {
                int missedTicks = tracker.getApplicableMissedTicks();
                if (missedTicks > 0) {
                    
                    pickupDelay = Math.max(0, pickupDelay - missedTicks);
                }
            }
        } catch (Throwable t) {
            
        }
    }
    
    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isSmartLagItemPickupDelayEnabled();
            tpsThreshold = bridge.getSmartLagTPSThreshold();
            
            bridge.debugLog("[AkiAsync] ItemEntityPickupDelayCompensationMixin initialized: enabled=%s, threshold=%.1f",
                enabled, tpsThreshold);
        } else {
            enabled = false;
        }
        
        initialized = true;
    }
}
