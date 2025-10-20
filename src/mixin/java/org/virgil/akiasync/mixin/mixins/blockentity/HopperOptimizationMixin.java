package org.virgil.akiasync.mixin.mixins.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

@SuppressWarnings("unused")
@Mixin(value = HopperBlockEntity.class, priority = 989)
public class HopperOptimizationMixin {
    
    
    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void aki$optimizeMarkDirty(CallbackInfo ci) {
        HopperBlockEntity hopper = (HopperBlockEntity) (Object) this;
        
        if (hopper.isEmpty()) {
            ci.cancel();
            return;
        }
        
        Level level = hopper.getLevel();
        if (level == null) return;
        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameTime() % 20 != 0) {
                ci.cancel();
            }
        }
    }
}

