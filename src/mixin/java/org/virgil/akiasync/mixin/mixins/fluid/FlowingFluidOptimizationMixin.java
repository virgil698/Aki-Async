package org.virgil.akiasync.mixin.mixins.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.level.material.FlowingFluid")
public class FlowingFluidOptimizationMixin {

    @Redirect(
            method = "canHoldAnyFluid(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"),
            require = 0
    )
    private static boolean optimizeSignCheck(BlockState blockState, TagKey<Block> tagKey) {
        
        if (tagKey == BlockTags.SIGNS) {
            return blockState.getBlock() instanceof SignBlock;
        }
        
        return blockState.is(tagKey);
    }
}
