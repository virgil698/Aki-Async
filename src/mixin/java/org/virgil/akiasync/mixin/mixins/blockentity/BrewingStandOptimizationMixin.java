package org.virgil.akiasync.mixin.mixins.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

@SuppressWarnings("unused")
@Mixin(value = BrewingStandBlockEntity.class, priority = 989)
public class BrewingStandOptimizationMixin {
    
    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void aki$delayDirty(CallbackInfo ci) {
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) (Object) this;
        if (stand.isEmpty()) {
            ci.cancel();
            return;
        }
        Level level = stand.getLevel();
        if (level == null) return;
        if (level instanceof net.minecraft.server.level.ServerLevel sl && sl.getGameTime() % 20 != 0) {
            ci.cancel();
        }
    }
}

