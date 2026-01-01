package org.virgil.akiasync.mixin.mixins.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction")
public class EndIslandDensityFixMixin {
    
    @Shadow
    @Final
    private SimplexNoise islandNoise;
    
    @Unique
    private static volatile boolean akiasync$initialized = false;
    
    @Unique
    private static volatile boolean akiasync$fixEnabled = true;
    
    @Unique
    private static void akiasync$initConfig() {
        if (akiasync$initialized) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                akiasync$fixEnabled = bridge.isEndIslandDensityFixEnabled();
            
                akiasync$initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EndIslandDensityFix", "initConfig", e);
        }
    }
    
    @Inject(
        method = "getHeightValue",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void fixIntegerOverflow(
            SimplexNoise noise, 
            int x, 
            int z, 
            CallbackInfoReturnable<Float> cir) {
        
        if (!akiasync$initialized) {
            akiasync$initConfig();
        }
        
        if (!akiasync$fixEnabled) {
            return; 
        }
        
        int i = x / 2;
        int i1 = z / 2;
        int i2 = x % 2;
        int i3 = z % 2;
        
        float f = 100.0F - Mth.sqrt((long)x * (long)x + (long)z * (long)z) * 8.0F;
        f = Mth.clamp(f, -100.0F, 80.0F);
        
        for (int i4 = -12; i4 <= 12; i4++) {
            for (int i5 = -12; i5 <= 12; i5++) {
                long l = (long)i + (long)i4;
                long l1 = (long)i1 + (long)i5;
                
                if (l * l + l1 * l1 > 4096L && noise.getValue((double)l, (double)l1) < -0.9F) {
                    float f1 = (Mth.abs((float)l) * 3439.0F + Mth.abs((float)l1) * 147.0F) % 13.0F + 9.0F;
                    float f2 = (float)i2 - (float)i4 * 2.0F;
                    float f3 = (float)i3 - (float)i5 * 2.0F;
                    float f4 = 100.0F - Mth.sqrt(f2 * f2 + f3 * f3) * f1;
                    f4 = Mth.clamp(f4, -100.0F, 80.0F);
                    f = Math.max(f, f4);
                }
            }
        }
        
        cir.setReturnValue(f);
    }
}
