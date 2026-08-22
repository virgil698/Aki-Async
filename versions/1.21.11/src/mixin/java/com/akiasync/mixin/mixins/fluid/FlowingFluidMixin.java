package com.akiasync.mixin.mixins.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumMap;
import java.util.Map;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @Shadow
    private boolean canMaybePassThrough(BlockGetter level, BlockPos pos, BlockState state,
                                        Direction direction, BlockPos spreadPos,
                                        BlockState spreadState, FluidState fluidState) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state);

    @Shadow
    private static boolean canHoldFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean canHoldSpecificFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        throw new AssertionError();
    }

    @Inject(method = "getSpread", at = @At("HEAD"), cancellable = true)
    private void akiAsync$singleDirectionSpread(ServerLevel level, BlockPos pos, BlockState state,
                                                CallbackInfoReturnable<Map<Direction, FluidState>> cir) {
        Direction onlyDirection = null;
        BlockPos onlyPos = null;
        BlockState onlyState = null;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = pos.relative(direction);
            BlockState targetState = level.getBlockStateIfLoaded(targetPos);
            if (targetState == null || !canHoldFluid(level, targetPos, targetState,
                    ((FlowingFluid) (Object) this).getSource())) {
                continue;
            }
            if (onlyDirection != null) {
                return;
            }
            onlyDirection = direction;
            onlyPos = targetPos;
            onlyState = targetState;
        }

        if (onlyDirection == null) {
            cir.setReturnValue(new EnumMap<>(Direction.class));
            return;
        }

        FluidState targetFluid = onlyState.getFluidState();
        if (!canMaybePassThrough(level, pos, state, onlyDirection, onlyPos, onlyState, targetFluid)) {
            cir.setReturnValue(new EnumMap<>(Direction.class));
            return;
        }

        FluidState newLiquid = getNewLiquid(level, onlyPos, onlyState);
        if (canHoldSpecificFluid(level, onlyPos, onlyState, newLiquid.getType())
            && targetFluid.canBeReplacedWith(level, onlyPos, newLiquid.getType(), onlyDirection)) {
            Map<Direction, FluidState> result = new EnumMap<>(Direction.class);
            result.put(onlyDirection, newLiquid);
            cir.setReturnValue(result);
        } else {
            cir.setReturnValue(new EnumMap<>(Direction.class));
        }
    }

    @Redirect(
        method = "canHoldAnyFluid(Lnet/minecraft/world/level/block/state/BlockState;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"
        )
    )
    private static boolean akiAsync$useSignTypeCheck(BlockState state, TagKey<Block> tag) {
        if (tag == BlockTags.SIGNS) {
            return state.getBlock() instanceof SignBlock;
        }
        return state.is(tag);
    }
}
