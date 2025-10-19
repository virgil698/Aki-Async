package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.MinecartHopper;

@SuppressWarnings("unused")
@Mixin(value = MinecartHopper.class, priority = 1200)
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
    
    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private void aki$throttlePush(CallbackInfoReturnable<Boolean> cir) {
        MinecartHopper minecart = (MinecartHopper) (Object) this;
        if (!(minecart.level() instanceof ServerLevel sl)) return;
        
        net.minecraft.core.BlockPos pos = minecart.blockPosition();
        net.minecraft.world.level.block.entity.BlockEntity be = sl.getBlockEntity(pos.above());
        
        if (be instanceof net.minecraft.world.Container) {
            return;
        }
        
        if (sl.getGameTime() % 10 != 0) {
            cir.setReturnValue(false);
        }
    }
}

