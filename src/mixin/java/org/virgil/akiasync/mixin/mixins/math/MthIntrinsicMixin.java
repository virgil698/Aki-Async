package org.virgil.akiasync.mixin.mixins.math;

import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = Mth.class, priority = 1100)
public class MthIntrinsicMixin {
    
    @Overwrite
    public static int floor(float value) {
        return (int) Math.floor(value);
    }
    
    @Overwrite
    public static int floor(double value) {
        return (int) Math.floor(value);
    }
    
    @Overwrite
    public static int ceil(float value) {
        return (int) Math.ceil(value);
    }
    
    @Overwrite
    public static int ceil(double value) {
        return (int) Math.ceil(value);
    }
    
    @Overwrite
    public static double absMax(double a, double b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }
}
