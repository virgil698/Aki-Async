package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * MinecartHopper hardcore optimization (v6.0 - Entity-level minecart)
 * 
 * "一个漏斗矿车 = 16个普通漏斗的卡顿" - ilmango quote
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = MinecartHopper.class, priority = 987)
public class MinecartHopperOptimizationMixin {
    
    // Skip empty minecart tick (safe method only)
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$skipEmptyMinecart(CallbackInfo ci) {
        MinecartHopper minecart = (MinecartHopper) (Object) this;
        
        if (minecart.isEmpty() && minecart.level() instanceof ServerLevel sl) {
            if (sl.getGameTime() % 20 != 0) {
                ci.cancel();  // Empty minecart, tick every 20 ticks
            }
        }
    }
}

