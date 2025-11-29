package org.virgil.akiasync.mixin.mixins.entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.LivingEntity;
@SuppressWarnings("unused")
@Mixin(LivingEntity.class)
public abstract class PushEntitiesOptimizationMixin {
    private static volatile boolean enabled;
    private static volatile int interval = 2;
    private static volatile boolean initialized = false;
    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void optimizePush(CallbackInfo ci) {
        if (!initialized) { akiasync$initPushOptimization(); }
        if (!enabled) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getDeltaMovement().lengthSqr() < 1.0E-7) {
            ci.cancel();
            return;
        }
        if (interval > 1 && self.tickCount % interval != 0) {
            ci.cancel();
        }
    }
    private static synchronized void akiasync$initPushOptimization() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isPushOptimizationEnabled();
        } else {
            enabled = true;
        }
        initialized = true;
        if (bridge != null) {
            bridge.debugLog("[AkiAsync] PushOptimizationMixin initialized: enabled=" + enabled);
        }
    }
}
