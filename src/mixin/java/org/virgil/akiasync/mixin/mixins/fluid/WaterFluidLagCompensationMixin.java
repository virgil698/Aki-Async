package org.virgil.akiasync.mixin.mixins.fluid;

import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.TPSTracker;

@Mixin(WaterFluid.class)
public class WaterFluidLagCompensationMixin {
    
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = false;
    @Unique
    private static volatile double tpsThreshold = 18.0;
    
    @Inject(method = "getTickDelay", at = @At("RETURN"), cancellable = true)
    private void modifyTickDelay(CallbackInfoReturnable<Integer> cir) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) return;
        
        try {
            TPSTracker tracker = TPSTracker.getInstance();
            double currentTPS = tracker.getMostAccurateTPS();
            
            if (currentTPS < tpsThreshold) {
                int original = cir.getReturnValue();
                int compensated = tracker.tt20(original, true);
                cir.setReturnValue(compensated);
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
            enabled = bridge.isSmartLagFluidCompensationEnabled() && 
                     bridge.isSmartLagFluidWaterEnabled();
            tpsThreshold = bridge.getSmartLagTPSThreshold();
            
            bridge.debugLog("[AkiAsync] WaterFluidLagCompensationMixin initialized: enabled=%s, threshold=%.1f",
                enabled, tpsThreshold);
        } else {
            enabled = false;
        }
        
        initialized = true;
    }
}
