package org.virgil.akiasync.mixin.mixins.ai.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@Mixin(Brain.class)
public class BrainThrottleMixin {

    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile boolean throttleEnabled = false;

    @Unique
    private static volatile int throttleInterval = 10;

    @Unique
    private int aki$tickCounter = 0;

    @Unique
    private double aki$lastX = 0;
    @Unique
    private double aki$lastY = 0;
    @Unique
    private double aki$lastZ = 0;

    @Unique
    private boolean aki$isStationary = false;

    @Unique
    private static final double MOVEMENT_THRESHOLD = 0.01;

    @Inject(
        method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void throttleBrainTick(ServerLevel level, LivingEntity entity, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initThrottle();
        }

        if (!throttleEnabled) {
            return;
        }

        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return;
        }

        double dx = entity.getX() - aki$lastX;
        double dy = entity.getY() - aki$lastY;
        double dz = entity.getZ() - aki$lastZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;

        aki$isStationary = distanceSq < MOVEMENT_THRESHOLD;

        aki$lastX = entity.getX();
        aki$lastY = entity.getY();
        aki$lastZ = entity.getZ();

        if (aki$isStationary) {
            aki$tickCounter++;

            if (aki$tickCounter % throttleInterval != 0) {

                ci.cancel();
                return;
            }
        } else {

            aki$tickCounter = 0;
        }

        if (entity instanceof Mob mob) {
            if (mob.getTarget() != null || mob.getLastHurtByMob() != null) {
                aki$tickCounter = 0;
                return;
            }
        }
    }

    @Unique
    private static synchronized void akiasync$initThrottle() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            throttleEnabled = bridge.isBrainThrottleEnabled();
            throttleInterval = bridge.getBrainThrottleInterval();

            BridgeConfigCache.debugLog("[AkiAsync-BrainThrottle] Brain throttle initialized");
            BridgeConfigCache.debugLog("[AkiAsync-BrainThrottle]   Enabled: " + throttleEnabled);
            BridgeConfigCache.debugLog("[AkiAsync-BrainThrottle]   Throttle interval: " + throttleInterval + " ticks");
            BridgeConfigCache.debugLog("[AkiAsync-BrainThrottle]   Strategy: Skip AI tick for stationary mobs");

            initialized = true;
        } else {
            throttleEnabled = false;
        }
    }
}
