package org.virgil.akiasync.mixin.mixins.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Chest hardcore optimization (delay markDirty + skip duplicate comparator)
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = ChestBlockEntity.class, priority = 989)
public class ChestOptimizationMixin {
    
    // Delay markDirty (reduce disk I/O)
    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void aki$delayChestMarkDirty(CallbackInfo ci) {
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        Level level = chest.getLevel();
        if (level == null) return;
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (serverLevel.getGameTime() % 40 != 0) {
                ci.cancel();  // Only save every 40 ticks
            }
        }
    }
}

