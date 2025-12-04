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
        
        Vec3 currentPos = entity.position();
        

        if (aki$lastPosition != null) {
            double movement = currentPos.distanceTo(aki$lastPosition);
            
            if (movement < cached_minMovementThreshold) {
                aki$staticTicks++;
                

                if (aki$staticTicks > 10 && travelVector.lengthSqr() < 0.0001) {
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
            cached_enabled = true;
            cached_minMovementThreshold = 0.003;
            
            bridge.debugLog("[AkiAsync] LivingEntityTravelOptimizeMixin initialized: enabled=" + 
                cached_enabled + " | threshold=" + cached_minMovementThreshold);
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
    }
}
