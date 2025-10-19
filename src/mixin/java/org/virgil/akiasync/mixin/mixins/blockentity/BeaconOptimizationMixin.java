package org.virgil.akiasync.mixin.mixins.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/**
 * Beacon optimization.
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = BeaconBlockEntity.class, priority = 989)
public class BeaconOptimizationMixin {
    
    @Inject(method = "updateBase", at = @At("HEAD"), cancellable = true)
    private static void aki$delayBaseScan(net.minecraft.world.level.Level level, int x, int y, int z, 
                                          CallbackInfoReturnable<Integer> cir) {
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            if (sl.getGameTime() % 20 != 0) {
                cir.setReturnValue(0);
            }
        }
    }
}

