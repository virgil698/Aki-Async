package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * ItemEntity zero-scan optimization v6.1
 * Throttles merge check only, preserves physics (no bounce bug)
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = ItemEntity.class, priority = 989)
public class ItemEntityOptimizationMixin {
    
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void aki$throttleMerge(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level() instanceof ServerLevel sl) {
            // Only check merge every 10 ticks (preserve physics every tick)
            if (sl.getGameTime() % 10 != 0) {
                ci.cancel();
            }
        }
    }
}

