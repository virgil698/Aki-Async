package org.virgil.akiasync.mixin.mixins.math;

import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.math.CompactSineLUT;

@Mixin(value = Mth.class, priority = 900)
public class MthMixin {
    
    @Shadow
    @Final
    @Mutable
    public static float[] SIN;
    
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void onClassInit(CallbackInfo ci) {
        try {
            
            CompactSineLUT.init();
            
            MthMixin.SIN = null;
            
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync-Math] " + CompactSineLUT.getStats());
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.BridgeConfigCache.errorLog("[AkiAsync-Math] Failed to initialize CompactSineLUT: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Overwrite
    public static float sin(float f) {
        return CompactSineLUT.sin(f);
    }
    
    @Overwrite
    public static float cos(float f) {
        return CompactSineLUT.cos(f);
    }
}
