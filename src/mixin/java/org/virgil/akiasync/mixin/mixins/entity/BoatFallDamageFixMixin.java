package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(AbstractBoat.class)
public abstract class BoatFallDamageFixMixin {
    
    @Inject(
        method = "checkFallDamage",
        at = @At("HEAD"),
        cancellable = true
    )
    private void preventBoatFallDamage(
        double y,
        boolean onGround,
        BlockState state,
        BlockPos pos,
        CallbackInfo ci
    ) {
        if (onGround) {
            ci.cancel();
        }
    }
}
