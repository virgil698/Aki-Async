package org.virgil.akiasync.mixin.mixins.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Furnace hardcore optimization.
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 989)
public class FurnaceOptimizationMixin {
    
    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void aki$optimizeMarkDirty(CallbackInfo ci) {
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        
        if (furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty()) {
            ci.cancel();
            return;
        }
        
        Level level = furnace.getLevel();
        if (level == null) return;
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (serverLevel.getGameTime() % 20 != 0) {
                ci.cancel();
            }
        }
    }
}

