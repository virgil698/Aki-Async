package org.virgil.akiasync.mixin.mixins.math;

import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = Mth.class, priority = 1100)
public class MthIntrinsicMixin {
    
    /**
     * @author AkiAsync (from Nitori/Gale)
     * @reason 使用 JVM intrinsic 优化 floor 计算
     * 
     * JVM 会将 Math.floor() 编译为高效的机器指令（SSE4.1 roundsd）
     * 比原版的位操作实现快 2-3 倍
     */
    @Overwrite
    public static int floor(float value) {
        return (int) Math.floor(value);
    }
    
    /**
     * @author AkiAsync (from Nitori/Gale)
     * @reason 使用 JVM intrinsic 优化 floor 计算
     */
    @Overwrite
    public static int floor(double value) {
        return (int) Math.floor(value);
    }
    
    /**
     * @author AkiAsync (from Nitori/Gale)
     * @reason 使用 JVM intrinsic 优化 ceil 计算
     * 
     * JVM 会将 Math.ceil() 编译为高效的机器指令（SSE4.1 roundsd）
     * 比原版的位操作实现快 2-3 倍
     */
    @Overwrite
    public static int ceil(float value) {
        return (int) Math.ceil(value);
    }
    
    /**
     * @author AkiAsync (from Nitori/Gale)
     * @reason 使用 JVM intrinsic 优化 ceil 计算
     */
    @Overwrite
    public static int ceil(double value) {
        return (int) Math.ceil(value);
    }
    
    /**
     * @author AkiAsync (from Nitori/Gale)
     * @reason 简化 absMax 计算逻辑
     * 
     * 原版实现可能包含额外的分支判断
     * 直接使用 Math.max + Math.abs 更简洁高效
     */
    @Overwrite
    public static double absMax(double a, double b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }
}
