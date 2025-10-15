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
    
        // Delay markDirty (only safe method, like Witch v2.1)
    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void aki$delayMarkDirty(CallbackInfo ci) {
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        Level level = furnace.getLevel();
        if (level == null) return;
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (serverLevel.getGameTime() % 20 != 0) {
                ci.cancel();
            }
        }
    }
}

