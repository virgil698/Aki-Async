package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@Mixin(value = Entity.class, priority = 900)
public class EntityThrottlingMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$throttleEntityTick(CallbackInfo ci) {
        try {
            Entity self = (Entity) (Object) this;
            
            if (self.level().isClientSide) {
                return;
            }
            
            Bridge bridge = BridgeManager.getBridge();
            if (bridge == null) {
                return;
            }
            
            if (bridge.shouldThrottleEntity(self)) {
                ci.cancel();
            }
            
        } catch (Exception e) {
        }
    }
}
