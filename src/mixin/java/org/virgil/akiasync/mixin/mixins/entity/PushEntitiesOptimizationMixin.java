package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;

/**
 * Optimize pushEntities (26.72% hotspot) - ServerCore inspired.
 * Skip pushing for stationary entities or apply interval-based throttle.
 */
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
        
        // Skip if entity is not moving (ServerCore pattern)
        if (self.getDeltaMovement().lengthSqr() < 1.0E-7) {
            ci.cancel();
            return;
        }
        
        // Throttle: only push every N ticks
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
        System.out.println("[AkiAsync] PushOptimizationMixin initialized: enabled=" + enabled);
    }
}

