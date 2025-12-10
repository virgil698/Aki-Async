package org.virgil.akiasync.mixin.mixins.fluid;

import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 流体状态缓存优化
 * 
 * 参考 Leaf 的 Moonrise 流体状态缓存机制：
 * 通过预计算和缓存流体状态属性，显著减少了运行时计算开销
 * 
 * 缓存的属性：
 * - amount: 流体数量
 * - isSource: 是否为源方块
 * - ownHeight: 自身高度
 * - isRandomlyTicking: 是否随机tick
 */
@Mixin(FluidState.class)
public abstract class FluidStateCacheMixin {

    @Unique
    private int akiasync$cachedAmount = -1;
    
    @Unique
    private boolean akiasync$cachedIsSource = false;
    
    @Unique
    private float akiasync$cachedOwnHeight = -1.0f;
    
    @Unique
    private boolean akiasync$cachedIsRandomlyTicking = false;
    
    @Unique
    private boolean akiasync$cacheInitialized = false;

    /**
     * 在FluidState创建时初始化缓存
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void initCache(CallbackInfo ci) {
        try {
            FluidState self = (FluidState) (Object) this;
            
            this.akiasync$cachedAmount = self.getAmount();
            this.akiasync$cachedIsSource = self.isSource();
            this.akiasync$cachedOwnHeight = self.getOwnHeight();
            this.akiasync$cachedIsRandomlyTicking = self.isRandomlyTicking();
            
            this.akiasync$cacheInitialized = true;
        } catch (Throwable t) {
            
            this.akiasync$cacheInitialized = false;
        }
    }

    /**
     * 获取缓存的amount值
     */
    @Unique
    public int akiasync$getCachedAmount() {
        if (!akiasync$cacheInitialized) {
            return ((FluidState) (Object) this).getAmount();
        }
        return akiasync$cachedAmount;
    }

    /**
     * 获取缓存的isSource值
     */
    @Unique
    public boolean akiasync$getCachedIsSource() {
        if (!akiasync$cacheInitialized) {
            return ((FluidState) (Object) this).isSource();
        }
        return akiasync$cachedIsSource;
    }

    /**
     * 获取缓存的ownHeight值
     */
    @Unique
    public float akiasync$getCachedOwnHeight() {
        if (!akiasync$cacheInitialized) {
            return ((FluidState) (Object) this).getOwnHeight();
        }
        return akiasync$cachedOwnHeight;
    }

    /**
     * 获取缓存的isRandomlyTicking值
     */
    @Unique
    public boolean akiasync$getCachedIsRandomlyTicking() {
        if (!akiasync$cacheInitialized) {
            return ((FluidState) (Object) this).isRandomlyTicking();
        }
        return akiasync$cachedIsRandomlyTicking;
    }
}
