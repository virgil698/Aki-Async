package org.virgil.akiasync.mixin.mixins.shapes;

import net.minecraft.world.phys.shapes.CubePointRange;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CubePointRange.class)
public class CubePointRangeMixin {
    
    @Shadow
    @Final
    private int parts;

    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private double scale;

    @Inject(method = "<init>(I)V", at = @At("RETURN"))
    public void initScale(int sectionCount, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initConfig();
        }
        
        if (enabled) {
            this.scale = 1.0D / this.parts;
        }
    }

    /**
     * @author AkiAsync
     * @reason 使用预计算的缩放因子，将除法转换为乘法
     */
    @Overwrite
    public double getDouble(int position) {
        if (enabled) {
            return position * this.scale;
        }
        
        return (double) position / (double) this.parts;
    }
    
    @Unique
    private static synchronized void akiasync$initConfig() {
        if (initialized) return;
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isShapeOptimizationEnabled() && 
                         bridge.isShapePrecomputeArrays();
                
                bridge.debugLog("[AkiAsync] CubePointRangeMixin initialized: enabled=" + enabled);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "CubePointRange", "init", e);
        }
        
        initialized = true;
    }
}
