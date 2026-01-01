package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings({"unused", "ConstantConditions", "NullableProblems"})
@Mixin(value = LivingEntity.class, priority = 800)
public abstract class PushEntitiesOptimizationMixin {
    @Unique
    private static volatile boolean enabled = false;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile int interval = 2;
    
    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"ConstantConditions", "ConstantValue"}) 
    private void optimizePush(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initPushOptimization();
        }
        
        if (!enabled) {
            return;
        }
        
        LivingEntity self = (LivingEntity) (Object) this;
        
        if (!self.isPushable()) {
            ci.cancel();
            return;
        }
        
        net.minecraft.world.scores.Team team = self.getTeam();
        if (team != null && team.getCollisionRule() == net.minecraft.world.scores.Team.CollisionRule.NEVER) {
            ci.cancel();
            return;
        }
        
        try {
            if (self.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                if (serverLevel.paperConfig().collisions.onlyPlayersCollide) {
                    if (!(self instanceof net.minecraft.server.level.ServerPlayer)) {
                        ci.cancel();
                        return;
                    }
                }
            }
        } catch (Exception e) {
            
        }
        
        if (self.isOnPortalCooldown()) {
            return;
        }
        
        if (self instanceof net.minecraft.world.entity.monster.Shulker) {
            ci.cancel();
            return;
        }
        
        net.minecraft.world.phys.Vec3 deltaMovement = self.getDeltaMovement();
        if (deltaMovement == null) {
            return;
        }
        double movementSqr = deltaMovement.lengthSqr();
        
        if (movementSqr > 0.0001) { 
            return;
        }
        
        if (self.tickCount % 5 == 0) {
            try {
                net.minecraft.world.phys.AABB checkBox = self.getBoundingBox().inflate(0.5);
                java.util.List<net.minecraft.world.entity.Entity> nearbyEntities = 
                    self.level().getEntities(self, checkBox);
                
                if (nearbyEntities.size() >= 3) {
                    if (self.tickCount % 2 != 0) {
                        ci.cancel();
                    }
                    return;
                }
            } catch (Exception e) {
                
                return;
            }
        }
        
        if (movementSqr > 1.0E-10) { 
            if (self.tickCount % 2 != 0) {
                ci.cancel();
            }
            return;
        }
        
        if (self.onGround() && !self.isInWater() && !self.isInLava()) {
            if (self.tickCount % 10 != 0) {
                ci.cancel();
            }
        } else {
            if (self.tickCount % 2 != 0) {
                ci.cancel();
            }
        }
    }
    
    @Unique
    private static synchronized void akiasync$initPushOptimization() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeConfigCache.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isPushOptimizationEnabled();
            BridgeConfigCache.debugLog("[AkiAsync] PushOptimizationMixin initialized: enabled=" + enabled);
            initialized = true;
        }
    }
}
