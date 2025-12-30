package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(LivingEntity.class)
public class LivingEntityTravelOptimizeMixin {
    
    @Unique
    private static volatile boolean cached_enabled = false;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile double cached_minMovementThreshold = 0.003;
    
    @Unique
    private static volatile int cached_skipInterval = 2;
    
    @Unique
    private Vec3 aki$lastPosition = Vec3.ZERO;
    
    @Unique
    private int aki$staticTicks = 0;
    
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true, require = 0)
    private void optimizeTravel(Vec3 travelVector, CallbackInfo ci) {
        if (!initialized) {
            aki$initTravelOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        LivingEntity entity = (LivingEntity) (Object) this;
        
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return;
        }
        
        if (entity.hasImpulse) {
            aki$staticTicks = 0;
            aki$lastPosition = entity.position();
            return;
        }
        
        if (entity.isOnPortalCooldown()) {
            aki$staticTicks = 0;
            aki$lastPosition = entity.position();
            return;
        }
        
        if (entity.isInWater() || entity.isInLava()) {
            aki$staticTicks = 0;
            aki$lastPosition = entity.position();
            return;
        }

        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() > 0.001) {
            aki$staticTicks = 0;
            aki$lastPosition = entity.position();
            return;
        }
        
        Vec3 currentPos = entity.position();
        
        if (aki$lastPosition != null) {
            double movement = currentPos.distanceTo(aki$lastPosition);
            
            if (movement < cached_minMovementThreshold) {
                aki$staticTicks++;
                
                if (aki$staticTicks > cached_skipInterval && 
                    travelVector.lengthSqr() < 0.0001 &&
                    entity.onGround() &&
                    !entity.isPassenger()) {
                    ci.cancel();
                    return;
                }
            } else {
                aki$staticTicks = 0;
            }
        }
        
        aki$lastPosition = currentPos;
    }
    
    @Unique
    private static void aki$initTravelOptimization() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = bridge.isLivingEntityTravelOptimizationEnabled();
            cached_skipInterval = bridge.getLivingEntityTravelSkipInterval();
            cached_minMovementThreshold = 0.003;
            
            bridge.debugLog("[AkiAsync] LivingEntityTravelOptimizeMixin initialized: enabled=" + 
                cached_enabled + " | skipInterval=" + cached_skipInterval + " | threshold=" + cached_minMovementThreshold);
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
    }
}
