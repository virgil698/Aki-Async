package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * ItemEntity zero-scan optimization v6.0
 * Throttles tick without breaking Paper/Spigot merge configs
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = ItemEntity.class, priority = 989)
public class ItemEntityOptimizationMixin {
    
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$throttleTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level() instanceof ServerLevel sl && sl.getGameTime() % 10 != 0) {
            ci.cancel();
        }
    }
    
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void aki$skipSelfMerge(CallbackInfo ci) {
        ci.cancel();
    }
}

