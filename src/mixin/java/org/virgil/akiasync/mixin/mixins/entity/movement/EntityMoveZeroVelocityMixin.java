package org.virgil.akiasync.mixin.mixins.entity.movement;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@Mixin(Entity.class)
public abstract class EntityMoveZeroVelocityMixin {

    @Shadow
    private AABB bb;

    @Unique
    private boolean akiasync$boundingBoxChanged = false;

    @Unique
    private static volatile boolean akiasync$initialized = false;

    @Unique
    private static volatile boolean akiasync$enabled = true;

    @Unique
    private static volatile long akiasync$skippedMoves = 0;

    @Unique
    private static volatile long akiasync$totalMoves = 0;

    @Inject(method = "move", at = @At("HEAD"), cancellable = true, require = 0)
    private void akiasync$onMove(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        if (!akiasync$initialized) {
            akiasync$initConfig();
        }

        if (!akiasync$enabled) {
            return;
        }

        akiasync$totalMoves++;

        if (!akiasync$boundingBoxChanged && isZeroMovement(movement)) {
            akiasync$skippedMoves++;
            ci.cancel();
            akiasync$boundingBoxChanged = false;
            return;
        }

        akiasync$boundingBoxChanged = false;
    }

    @Inject(method = "setBoundingBox", at = @At("HEAD"), require = 0)
    private void akiasync$onBoundingBoxChanged(AABB boundingBox, CallbackInfo ci) {
        if (this.bb != null && !this.bb.equals(boundingBox)) {
            akiasync$boundingBoxChanged = true;
        }
    }

    @Unique
    private static boolean isZeroMovement(Vec3 movement) {
        return movement.x == 0.0 && movement.y == 0.0 && movement.z == 0.0;
    }

    @Unique
    private static synchronized void akiasync$initConfig() {
        if (akiasync$initialized) {
            return;
        }

        try {
            var bridge = BridgeConfigCache.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isEntityMoveZeroVelocityOptimizationEnabled();
                BridgeConfigCache.debugLog("[AkiAsync-MoveZeroVelocity] VMP-style optimization enabled: %s", akiasync$enabled);
                akiasync$initialized = true;
            }
        } catch (Exception e) {
            akiasync$enabled = true;
        }
    }

    @Unique
    private static String akiasync$getStatistics() {
        if (akiasync$totalMoves == 0) {
            return "EntityMoveZeroVelocity: No moves processed yet";
        }
        double skipRate = (double) akiasync$skippedMoves / akiasync$totalMoves * 100.0;
        return String.format(
            "EntityMoveZeroVelocity: total=%d, skipped=%d (%.2f%%)",
            akiasync$totalMoves, akiasync$skippedMoves, skipRate
        );
    }
}
