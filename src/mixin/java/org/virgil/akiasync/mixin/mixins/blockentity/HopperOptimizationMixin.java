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
 * Hopper hardcore optimization (v7.0 - Real method names from decompile)
 * 
 * Real methods: suckInItems, ejectItems (NOT pushItems/pickUpItem)
 * Based on HopperBlockEntity.class decompile analysis
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = HopperBlockEntity.class, priority = 989)
public class HopperOptimizationMixin {
    
    // 1. Skip suckInItems when covered by container (Discord建议 - 游戏机制级优化)
    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void aki$skipWhenCovered(Level level, net.minecraft.world.level.block.entity.Hopper hopper, 
                                           CallbackInfoReturnable<Boolean> cir) {
        // Check if above block has BlockEntity (container)
        BlockPos above = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
        BlockState aboveState = level.getBlockState(above);
        
        // If above has container, skip item entity scan
        if (level.getBlockEntity(above) != null || 
            aboveState.is(net.minecraft.world.level.block.Blocks.SHULKER_BOX) ||
            aboveState.is(net.minecraft.world.level.block.Blocks.BEEHIVE)) {
            cir.setReturnValue(false);  // Skip掉落物扫描，container transfer handles it
        }
    }
    
    // 2. Skip empty + delay markDirty
    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void aki$optimizeMarkDirty(CallbackInfo ci) {
        HopperBlockEntity hopper = (HopperBlockEntity) (Object) this;
        
        // Skip if empty
        if (hopper.isEmpty()) {
            ci.cancel();
            return;
        }
        
        // Delay markDirty (reduce disk I/O)
        Level level = hopper.getLevel();
        if (level == null) return;
        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameTime() % 20 != 0) {
                ci.cancel();
            }
        }
    }
}

