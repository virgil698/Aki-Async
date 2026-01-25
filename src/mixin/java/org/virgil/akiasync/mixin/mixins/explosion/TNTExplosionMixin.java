package org.virgil.akiasync.mixin.mixins.explosion;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
@Mixin(value = PrimedTnt.class, priority = 1200)
public class TNTExplosionMixin {
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void aki$asyncExplosion(CallbackInfo ci) {
        PrimedTnt tnt = (PrimedTnt) (Object) this;
        Level level = tnt.level();

        if (!(level instanceof ServerLevel sl)) {
            return;
        }

        Bridge bridge = BridgeManager.getBridge();
        if (bridge == null) {
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Bridge is null!");
            return;
        }

        boolean tntEnabled = bridge.isTNTOptimizationEnabled();

        if (!tntEnabled) {
            if (bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] TNT optimization is disabled, using vanilla logic");
            }
            return;
        }

        if (bridge.isTNTDebugEnabled()) {
            BridgeConfigCache.debugLog("[AkiAsync-TNT] ===== TNT EXPLOSION START =====");
            BridgeConfigCache.debugLog("[AkiAsync-TNT] Entity: " + tnt.getEncodeId() + " at " + tnt.position());
        }

        Vec3 velocity = tnt.getDeltaMovement();
        if (velocity == null) {
            return;
        }
        double speed = velocity.lengthSqr();
        int fuse = tnt.getFuse();

        double horizontalSpeed = velocity.x * velocity.x + velocity.z * velocity.z;
        boolean isPistonPushed = speed > 0.1 || horizontalSpeed > 0.05;

        if (isPistonPushed) {
            if (bridge.isTNTDebugEnabled()) {
                BridgeConfigCache.debugLog("[AkiAsync-TNT] Skipping optimization for piston-pushed TNT: speed=" +
                    String.format("%.3f", Math.sqrt(speed)) + ", horizontal=" + String.format("%.3f", Math.sqrt(horizontalSpeed)) +
                    ", fuse=" + fuse);
            }
            return;
        }

        ci.cancel();

        Vec3 center = tnt.position();

        if (aki$isFoliaEnvironment()) {
            net.minecraft.core.BlockPos centerPos = net.minecraft.core.BlockPos.containing(center);

            if (!bridge.isOwnedByCurrentRegion(sl, centerPos)) {
                aki$scheduleFoliaExplosion(tnt, sl, center, bridge);
                tnt.discard();
                return;
            }

            aki$executeExplosionInRegion(tnt, sl, center, bridge);
            tnt.discard();
            return;
        }

        boolean inWater = tnt.isInWater() ||
                          tnt.isUnderWater() ||
                          !sl.getFluidState(BlockPos.containing(center)).isEmpty();

        if (inWater) {
            if (bridge.isTNTDebugEnabled()) {
                BridgeConfigCache.debugLog("[AkiAsync-TNT] TNT in water at " + center + ", using vanilla explosion (no block damage)");
            }

            ci.cancel();
            sl.explode(
                tnt,
                net.minecraft.world.level.Explosion.getDefaultDamageSource(sl, tnt),
                null,
                center.x,
                center.y,
                center.z,
                4.0F,
                false,
                net.minecraft.world.level.Level.ExplosionInteraction.TNT
            );
            tnt.discard();
            return;
        }

        aki$executeSafeExplosion(tnt, sl, center, false, bridge);

        tnt.discard();
    }

    private static void applyExplosionResults(ServerLevel level,
                                            net.minecraft.world.level.ServerExplosion explosion,
                                            org.virgil.akiasync.mixin.async.explosion.ExplosionResult result,
                                            PrimedTnt tnt, Vec3 center, boolean inWater) {

        org.virgil.akiasync.mixin.async.explosion.ExplosionResultApplier.applyExplosionResults(
            level, explosion, result, tnt, center, inWater
        );
    }

    private static boolean aki$isFoliaEnvironment() {
        Bridge bridge = BridgeManager.getBridge();
        return bridge != null && bridge.isFoliaEnvironment();
    }

    private static void aki$scheduleFoliaExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                                   Bridge bridge) {
        Runnable explosionTask = () -> bridge.safeExecute(
            () -> aki$executeExplosionInRegion(tnt, sl, center, bridge),
            "Folia scheduled explosion"
        );

        net.minecraft.core.BlockPos centerPos = net.minecraft.core.BlockPos.containing(center);
        bridge.scheduleRegionTask(sl, centerPos, explosionTask);

        tnt.discard();
    }

    private static void aki$executeExplosionInRegion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                                     Bridge bridge) {
        boolean inWater = tnt.isInWater() || !sl.getFluidState(BlockPos.containing(center)).isEmpty();

        if (inWater) {
            sl.explode(
                tnt,
                net.minecraft.world.level.Explosion.getDefaultDamageSource(sl, tnt),
                null,
                center.x, center.y, center.z,
                4.0F, false,
                net.minecraft.world.level.Level.ExplosionInteraction.TNT
            );
            return;
        }

        try {
            org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot =
                new org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot(sl, center, 4.0F, false);

            aki$executeSyncExplosion(tnt, sl, center, snapshot, bridge);

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                BridgeConfigCache.debugLog("[AkiAsync-TNT] Folia region explosion completed at " + center);
            }

        } catch (Exception ex) {
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Error in Folia region explosion: " + ex.getMessage());
            aki$fallbackSyncExplosion(tnt, sl, center);
        }
    }

    private static void aki$fallbackSyncExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center) {
        try {

            sl.explode(
                tnt,
                net.minecraft.world.level.Explosion.getDefaultDamageSource(sl, tnt),
                null,
                center.x, center.y, center.z,
                4.0F,
                false,
                net.minecraft.world.level.Level.ExplosionInteraction.TNT
            );

            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Fallback explosion completed at " + center);
            }
        } catch (Exception fallbackEx) {
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Fallback explosion failed: " + fallbackEx.getMessage());
        }
    }

    private static void aki$executeSafeExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                                boolean inWater, Bridge bridge) {
        try {

            if (tnt == null) {
                BridgeConfigCache.errorLog("[AkiAsync-TNT] TNT entity is null, aborting");
                return;
            }
            if (sl == null) {
                BridgeConfigCache.errorLog("[AkiAsync-TNT] ServerLevel is null, aborting");
                return;
            }
            if (center == null) {
                BridgeConfigCache.errorLog("[AkiAsync-TNT] Explosion center is null, aborting");
                return;
            }
            if (tnt.isRemoved()) {
                if (bridge != null && bridge.isTNTDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-TNT] TNT entity already removed, skipping");
                }
                return;
            }

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Collecting entities in main thread before async calculation");
            }

            org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot =
                new org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot(sl, center, 4.0F, false);

            java.util.concurrent.ExecutorService executor = org.virgil.akiasync.mixin.async.TNTThreadPool.getExecutor();

            if (bridge != null) {
                String healthStatus = bridge.checkExecutorHealth(executor, "TNT");

                if (healthStatus != null && healthStatus.contains("unhealthy")) {
                    if (bridge.isTNTDebugEnabled()) {
                        BridgeConfigCache.debugLog("[AkiAsync-TNT] Executor unhealthy, using sync calculation: " + healthStatus);
                    }
                    aki$executeSyncExplosion(tnt, sl, center, snapshot, bridge);
                    return;
                }
            }

            executor.execute(() -> {
                try {
                    org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator calculator =
                        new org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator(snapshot);
                    org.virgil.akiasync.mixin.async.explosion.ExplosionResult result = calculator.calculate();

                    Runnable applyResultsTask = () -> {
                        try {
                            net.minecraft.world.level.ServerExplosion explosion = new net.minecraft.world.level.ServerExplosion(
                                sl, tnt, null, null, center, 4.0F, false,
                                net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
                            );

                            applyExplosionResults(sl, explosion, result, tnt, center, inWater);

                            org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator.releaseResult(result);

                            if (bridge != null && bridge.isTNTDebugEnabled()) {
                                BridgeConfigCache.debugLog("[AkiAsync-TNT] Safe async explosion completed at " + center);
                            }
                        } catch (Exception ex) {
                            BridgeConfigCache.errorLog("[AkiAsync-TNT] Error applying safe explosion results: " + ex.getMessage());
                            aki$fallbackSyncExplosion(tnt, sl, center);
                        }
                    };

                    if (aki$isFoliaEnvironment()) {
                        net.minecraft.core.BlockPos centerPos = net.minecraft.core.BlockPos.containing(center);
                        bridge.scheduleRegionTask(sl, centerPos, applyResultsTask);
                    } else {
                        sl.getServer().execute(applyResultsTask);
                    }
                } catch (Exception ex) {

                    BridgeConfigCache.errorLog("[AkiAsync-TNT] Error in safe async explosion calculation:");
                    BridgeConfigCache.errorLog("[AkiAsync-TNT] Exception type: " + ex.getClass().getName());
                    BridgeConfigCache.errorLog("[AkiAsync-TNT] Message: " + (ex.getMessage() != null ? ex.getMessage() : "null"));

                    StackTraceElement[] stackTrace = ex.getStackTrace();
                    if (stackTrace != null && stackTrace.length > 0) {
                        BridgeConfigCache.errorLog("[AkiAsync-TNT] Stack trace:");
                        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                            BridgeConfigCache.errorLog("[AkiAsync-TNT]   at " + stackTrace[i].toString());
                        }
                    }

                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        BridgeConfigCache.errorLog("[AkiAsync-TNT] Caused by: " + cause.getClass().getName() +
                            ": " + (cause.getMessage() != null ? cause.getMessage() : "null"));
                    }

                    Runnable fallbackTask = () -> aki$fallbackSyncExplosion(tnt, sl, center);
                    if (aki$isFoliaEnvironment()) {
                        net.minecraft.core.BlockPos centerPos = net.minecraft.core.BlockPos.containing(center);
                        bridge.scheduleRegionTask(sl, centerPos, fallbackTask);
                    } else {
                        sl.getServer().execute(fallbackTask);
                    }
                }
            });

        } catch (Exception e) {

            BridgeConfigCache.errorLog("[AkiAsync-TNT] Safe explosion setup failed:");
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Exception type: " + e.getClass().getName());
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Message: " + (e.getMessage() != null ? e.getMessage() : "null"));

            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                BridgeConfigCache.errorLog("[AkiAsync-TNT] Stack trace:");
                for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                    BridgeConfigCache.errorLog("[AkiAsync-TNT]   at " + stackTrace[i].toString());
                }
            }

            aki$fallbackSyncExplosion(tnt, sl, center);
        }
    }

    private static void aki$executeSyncExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                               org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot,
                                               Bridge bridge) {
        try {
            org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator calculator =
                new org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator(snapshot);
            org.virgil.akiasync.mixin.async.explosion.ExplosionResult result = calculator.calculate();

            net.minecraft.world.level.ServerExplosion explosion = new net.minecraft.world.level.ServerExplosion(
                sl, tnt, null, null, center, 4.0F, false,
                net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
            );

            applyExplosionResults(sl, explosion, result, tnt, center, false);

            org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator.releaseResult(result);

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                BridgeConfigCache.debugLog("[AkiAsync-TNT] Sync explosion completed at " + center);
            }
        } catch (Exception ex) {
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Sync explosion calculation failed: " + ex.getMessage());
            aki$fallbackSyncExplosion(tnt, sl, center);
        }
    }
}
