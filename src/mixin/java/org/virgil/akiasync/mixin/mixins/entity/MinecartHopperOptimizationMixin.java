package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.MinecartHopper;

/**
 * MinecartHopper optimization v6.0
 * Throttle empty minecart ticks (ilmango: 1 minecart = 16 hoppers lag)
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = MinecartHopper.class, priority = 987)
public class MinecartHopperOptimizationMixin {
    
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$skipEmptyMinecart(CallbackInfo ci) {
        MinecartHopper minecart = (MinecartHopper) (Object) this;
        
        if (minecart.isEmpty() && minecart.level() instanceof ServerLevel sl) {
            if (sl.getGameTime() % 20 != 0) {
                ci.cancel();
            }
        }
    }
}

