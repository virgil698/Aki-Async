package org.virgil.akiasync.mixin.mixins.fluid;

import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Unique
    public int akiasync$getCachedAmount() {
        if (!akiasync$cacheInitialized) {
            return ((FluidState) (Object) this).getAmount();
        }
        return akiasync$cachedAmount;
    }

    @Unique
    public boolean akiasync$getCachedIsSource() {
        if (!akiasync$cacheInitialized) {
            return ((FluidState) (Object) this).isSource();
        }
        return akiasync$cachedIsSource;
    }

    @Unique
    public float akiasync$getCachedOwnHeight() {
        if (!akiasync$cacheInitialized) {
            return ((FluidState) (Object) this).getOwnHeight();
        }
        return akiasync$cachedOwnHeight;
    }

    @Unique
    public boolean akiasync$getCachedIsRandomlyTicking() {
        if (!akiasync$cacheInitialized) {
            return ((FluidState) (Object) this).isRandomlyTicking();
        }
        return akiasync$cachedIsRandomlyTicking;
    }
}
