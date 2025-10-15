package org.virgil.akiasync.mixin.mixins.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Furnace hardcore optimization (delay markDirty only, like Witch v2.1)
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 989)
public class FurnaceOptimizationMixin {
    
    // Skip empty furnace setChanged + delay markDirty
    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void aki$optimizeMarkDirty(CallbackInfo ci) {
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        
        // Skip if both fuel and input are empty
        if (furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty()) {
            ci.cancel();
            return;
        }
        
        // Delay markDirty (reduce disk I/O)
        Level level = furnace.getLevel();
        if (level == null) return;
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (serverLevel.getGameTime() % 20 != 0) {
                ci.cancel();
            }
        }
    }
}

