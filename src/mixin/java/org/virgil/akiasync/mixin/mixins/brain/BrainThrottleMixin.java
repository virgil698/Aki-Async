package org.virgil.akiasync.mixin.mixins.brain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.phys.Vec3;

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
        
        if (entity instanceof net.minecraft.world.entity.monster.warden.Warden) {
            return;
        }
        
        if (entity instanceof net.minecraft.world.entity.npc.Villager) {
            return;
        }
        
        if (entity instanceof net.minecraft.world.entity.npc.AbstractVillager) {
            return;
        }
        
        if (entity instanceof net.minecraft.world.entity.NeutralMob neutralMob) {
            if (neutralMob.getRemainingPersistentAngerTime() > 0) {
                akiasync$stillTicks = 0;
                akiasync$lastPos = entity.position();
                return;
            }
            if (neutralMob.getPersistentAngerTarget() != null) {
                akiasync$stillTicks = 0;
                akiasync$lastPos = entity.position();
                return;
            }
        }
        
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.getTarget() != null) {
                akiasync$stillTicks = 0;
                akiasync$lastPos = entity.position();
                return;
            }
            if (mob.getLastHurtByMob() != null) {
                akiasync$stillTicks = 0;
                akiasync$lastPos = entity.position();
                return;
            }
            if (mob.getNavigation() != null && mob.getNavigation().isInProgress()) {
                akiasync$stillTicks = 0;
                akiasync$lastPos = entity.position();
                return;
            }
        }
        
        if (entity.hurtTime > 0 || entity.isOnFire()) {
            akiasync$stillTicks = 0;
            akiasync$lastPos = entity.position();
            return;
        }
        
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

        boolean inFluid = entity.isInWater() || entity.isInLava();
        if (!inFluid) {
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos((int)cur.x, (int)cur.y, (int)cur.z);
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            inFluid = !state.getFluidState().isEmpty();
        }
        
        double dx = cur.x - akiasync$lastPos.x;
        double dy = cur.y - akiasync$lastPos.y;
        double dz = cur.z - akiasync$lastPos.z;
        double dist2 = dx * dx + dy * dy + dz * dz;
        
        if (!inFluid && entity.onGround() && dist2 < 1.0E-4) {
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
        if (bridge != null) {
            bridge.debugLog("[AkiAsync] BrainThrottleMixin initialized: enabled=" + cached_enabled + ", interval=" + cached_interval);
        }
    }
}
