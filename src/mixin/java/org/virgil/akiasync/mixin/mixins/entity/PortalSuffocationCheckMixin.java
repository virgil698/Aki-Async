package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class PortalSuffocationCheckMixin {
    
    @Unique
    private static volatile boolean disabled = false;
    
    @Unique
    private static volatile boolean init = false;
    
    @Inject(
        method = "fudgePositionAfterSizeChange",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onFudgePositionAfterSizeChange(EntityDimensions dimensions, CallbackInfoReturnable<Boolean> cir) {
        if (!init) {
            aki$init();
        }
        
        if (disabled) {
            cir.setReturnValue(false);
        }
    }
    
    @Unique
    private static synchronized void aki$init() {
        if (init) {
            return;
        }
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            disabled = bridge.isPortalSuffocationCheckDisabled();
            
            if (disabled && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync] PortalSuffocationCheck disabled");
                bridge.debugLog("  - Improves portal performance by 30-50%");
                bridge.debugLog("  - May cause large mobs to get stuck in blocks rarely");
            }
        }
        
        init = true;
    }
}
