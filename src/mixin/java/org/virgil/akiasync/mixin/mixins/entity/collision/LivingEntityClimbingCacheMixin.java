package org.virgil.akiasync.mixin.mixins.entity.collision;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityClimbingCacheMixin {

    @Unique
    private boolean akiasync$cachedOnClimbable = false;

    @Unique
    private BlockPos akiasync$lastClimbingPosition = null;

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void onClimbable(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        BlockPos currentPos = self.blockPosition();

        if (!currentPos.equals(this.akiasync$lastClimbingPosition)) {
            this.akiasync$lastClimbingPosition = currentPos.immutable();
            return;
        }

        cir.setReturnValue(this.akiasync$cachedOnClimbable);
    }

    @Inject(method = "onClimbable", at = @At("RETURN"))
    private void onClimbableReturn(CallbackInfoReturnable<Boolean> cir) {
        this.akiasync$cachedOnClimbable = cir.getReturnValue();
    }
}
