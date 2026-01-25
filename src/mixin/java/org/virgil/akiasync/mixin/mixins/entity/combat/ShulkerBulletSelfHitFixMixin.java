package org.virgil.akiasync.mixin.mixins.entity.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBullet.class)
public class ShulkerBulletSelfHitFixMixin {

    @Unique
    private static volatile boolean enabled = true;

    @Unique
    private static volatile boolean init = false;

    @Inject(
        method = "canHitEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void preventSelfHit(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!init) {
            aki$init();
        }

        if (!enabled) {
            return;
        }

        ShulkerBullet bullet = (ShulkerBullet) (Object) this;
        Entity owner = bullet.getOwner();

        if (owner != null && target == owner) {
            cir.setReturnValue(false);
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
            enabled = bridge.isShulkerBulletSelfHitFixEnabled();

            if (enabled && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync] ShulkerBulletSelfHitFix enabled");
                bridge.debugLog("  - Fixes vanilla bug where shulker bullets can hit their shooter");
            }
        }

        init = true;
    }
}
