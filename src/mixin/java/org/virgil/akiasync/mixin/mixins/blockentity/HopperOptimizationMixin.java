package org.virgil.akiasync.mixin.mixins.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Hopper hardcore optimization (delay markDirty only, like Witch v2.1)
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = HopperBlockEntity.class, priority = 989)
public class HopperOptimizationMixin {
    
    // Skip empty hopper setChanged + delay markDirty
    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void aki$optimizeMarkDirty(CallbackInfo ci) {
        HopperBlockEntity hopper = (HopperBlockEntity) (Object) this;
        
        // Skip if empty (no items changed)
        if (hopper.isEmpty()) {
            ci.cancel();
            return;
        }
        
        // Delay markDirty (reduce disk I/O)
        Level level = hopper.getLevel();
        if (level == null) return;
        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameTime() % 20 != 0) {
                ci.cancel();  // Only save every 20 ticks
            }
        }
    }
}

