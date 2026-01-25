package org.virgil.akiasync.mixin.mixins.ai.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MoveToBlockGoal.class)
public class MoveToBlockGoalChunkLoadMixin {

    @Shadow
    @Final
    protected PathfinderMob mob;

    @Inject(
        method = "findNearestBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/PathfinderMob;isWithinHome(Lnet/minecraft/core/BlockPos;)Z"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private void onFindNearestBlock(
        CallbackInfoReturnable<Boolean> cir,
        int searchRange,
        int verticalSearchRange,
        BlockPos mobPos,
        BlockPos.MutableBlockPos mutableBlockPos,
        int x,
        int y,
        int z,
        int i4
    ) {

        if (!this.mob.level().hasChunkAt(mutableBlockPos)) {

            cir.setReturnValue(false);
        }
    }
}
