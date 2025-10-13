package org.virgil.akiasync.mixin.mixins.brain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.phys.Vec3;

/**
 * Brain throttle for stationary entities.
 * Config loaded from BridgeManager (Leaves template pattern).
 * Lower priority to execute AFTER ExpensiveAIMixin.
 */
@SuppressWarnings("unused")
@Mixin(value = Brain.class, priority = 1100)
public abstract class BrainThrottleMixin<E extends LivingEntity> {

    private static volatile boolean cached_enabled;
    private static volatile int cached_interval;
    private static volatile boolean initialized = false;

    private long akiasync$skipUntil;
    private Vec3 akiasync$lastPos;
    private int akiasync$stillTicks;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void akiasync$tickThrottle(net.minecraft.server.level.ServerLevel level, E entity, CallbackInfo ci) {
        if (!initialized) { akiasync$initBrainThrottle(); }
        if (!cached_enabled) return;

        long gameTime = level.getGameTime();
        if (gameTime < this.akiasync$skipUntil) {
            ci.cancel();
            return;
        }
        
        Vec3 cur = entity.position();
        if (akiasync$lastPos == null) {
            akiasync$lastPos = cur;
            akiasync$stillTicks = 0;
            return;
        }
        
        double dx = cur.x - akiasync$lastPos.x;
        double dy = cur.y - akiasync$lastPos.y;
        double dz = cur.z - akiasync$lastPos.z;
        double dist2 = dx * dx + dy * dy + dz * dz;

        if (!entity.isInWater() && entity.onGround() && dist2 < 1.0E-4) {
            akiasync$stillTicks++;
            if (akiasync$stillTicks >= cached_interval) {
                this.akiasync$skipUntil = gameTime + cached_interval;
                ci.cancel();
            }
        } else {
            akiasync$stillTicks = 0;
            akiasync$lastPos = cur;
        }
    }
    
    private static synchronized void akiasync$initBrainThrottle() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isBrainThrottleEnabled();
            cached_interval = bridge.getBrainThrottleInterval();
        } else {
            cached_enabled = false;
            cached_interval = 10;
        }
        initialized = true;
        System.out.println("[AkiAsync] BrainThrottleMixin initialized: enabled=" + cached_enabled + ", interval=" + cached_interval);
    }
}

