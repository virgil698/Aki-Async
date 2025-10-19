package org.virgil.akiasync.mixin.mixins.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hopper optimization.
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = HopperBlockEntity.class, priority = 989)
public class HopperOptimizationMixin {
    
    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void aki$skipWhenCovered(Level level, net.minecraft.world.level.block.entity.Hopper hopper, 
                                           CallbackInfoReturnable<Boolean> cir) {
        BlockPos above = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
        BlockState aboveState = level.getBlockState(above);
        
        if (level.getBlockEntity(above) != null || 
            aboveState.is(net.minecraft.world.level.block.Blocks.SHULKER_BOX) ||
            aboveState.is(net.minecraft.world.level.block.Blocks.BEEHIVE)) {
            cir.setReturnValue(false);
        }
    }
    
    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void aki$optimizeMarkDirty(CallbackInfo ci) {
        HopperBlockEntity hopper = (HopperBlockEntity) (Object) this;
        
        if (hopper.isEmpty()) {
            ci.cancel();
            return;
        }
        
        Level level = hopper.getLevel();
        if (level == null) return;
        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameTime() % 20 != 0) {
                ci.cancel();
            }
        }
    }
}

