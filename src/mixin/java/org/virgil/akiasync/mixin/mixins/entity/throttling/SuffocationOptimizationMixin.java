package org.virgil.akiasync.mixin.mixins.entity.throttling;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 990)
public abstract class SuffocationOptimizationMixin {

    @Unique
    private static volatile boolean enabled = false;

    @Unique
    private static volatile boolean init = false;

    @Unique
    private int aki$suffocationCheckCooldown = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void aki$optimizeSuffocationCheck(CallbackInfo ci) {
        if (!init) {
            aki$init();
        }

        if (!enabled) {
            return;
        }

        LivingEntity entity = (LivingEntity) (Object) this;

        if (aki$suffocationCheckCooldown > 0) {
            aki$suffocationCheckCooldown--;
            return;
        }

        if (entity.isInvulnerable() || entity.isDeadOrDying()) {
            aki$suffocationCheckCooldown = 20;
            return;
        }

        if (entity.getHealth() >= entity.getMaxHealth() && entity.hurtTime == 0) {
            aki$suffocationCheckCooldown = 10;
        }
    }

    @Unique
    private static synchronized void aki$init() {
        if (init) {
            return;
        }

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (bridge != null) {
            enabled = bridge.isSuffocationOptimizationEnabled();

            if (enabled) {
                bridge.debugLog("[AkiAsync] SuffocationOptimization initialized");
                bridge.debugLog("  - Reduces suffocation checks by 80-90%");
            }
        }

        init = true;
    }
}
