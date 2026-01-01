package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@SuppressWarnings("unused")
@Mixin(value = LivingEntity.class, priority = 850)
public abstract class BatchCollisionOptimizationMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile double slowMovementThreshold = 0.01; 
    @Unique
    private static volatile int slowMovementInterval = 10; 
    @Unique
    private static volatile double fastMovementThreshold = 0.5; 
    @Unique
    private static volatile int fastMovementInterval = 1; 
    
    @Unique
    private int collisionCheckCounter = 0;
    @Unique
    private Vec3 lastPosition = Vec3.ZERO;
    @Unique
    private int ticksSinceLastCollisionCheck = 0;
    
    @Inject(
        method = "pushEntities",
        at = @At("HEAD"),
        cancellable = true,
        require = 0  
    )
    private void optimizePushEntitiesFrequency(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initBatchOptimization();
        }
        
        if (!enabled) {
            return;
        }
        
        LivingEntity self = (LivingEntity) (Object) this;
        
        if (self.isRemoved() || !self.isAlive()) {
            ci.cancel();
            return;
        }
        
        if (self.isVehicle() && self.tickCount % 2 != 0) {
            ci.cancel();
            return;
        }
        
        ticksSinceLastCollisionCheck++;
        
        if (ticksSinceLastCollisionCheck < slowMovementInterval) {
            
            if (ticksSinceLastCollisionCheck < fastMovementInterval) {
                return; 
            }
            
            Vec3 currentPos = self.position();
            double movementSpeedSqr = akiasync$calculateMovementSpeedSqr(currentPos);
            int checkInterval = akiasync$getCheckInterval(movementSpeedSqr);
            
            if (ticksSinceLastCollisionCheck < checkInterval) {
                ci.cancel();
                return;
            }
            
            ticksSinceLastCollisionCheck = 0;
            lastPosition = currentPos;
        } else {
            ticksSinceLastCollisionCheck = 0;
            lastPosition = self.position();
        }
    }
    
    @Unique
    private double akiasync$calculateMovementSpeedSqr(Vec3 currentPos) {
        if (lastPosition == Vec3.ZERO) {
            lastPosition = currentPos;
            return 0;
        }
        
        double dx = currentPos.x - lastPosition.x;
        double dy = currentPos.y - lastPosition.y;
        double dz = currentPos.z - lastPosition.z;
        
        return dx * dx + dy * dy + dz * dz;
    }
    
    @Unique
    private int akiasync$getCheckInterval(double movementSpeedSqr) {
        
        double slowThresholdSqr = slowMovementThreshold * slowMovementThreshold; 
        double fastThresholdSqr = fastMovementThreshold * fastMovementThreshold; 
        
        if (movementSpeedSqr < slowThresholdSqr) {
            
            return slowMovementInterval;
        } else if (movementSpeedSqr > fastThresholdSqr) {
            
            return fastMovementInterval;
        } else {
            
            double movementSpeed = Math.sqrt(movementSpeedSqr);
            return 3 + (int) (movementSpeed * 4);
        }
    }
    
    @Inject(
        method = "tick",
        at = @At("HEAD")
    )
    private void resetCollisionCounter(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initBatchOptimization();
        }
        
        if (!enabled) {
            return;
        }
        
        collisionCheckCounter++;
        
        if (collisionCheckCounter >= 100) {
            collisionCheckCounter = 0;
            LivingEntity self = (LivingEntity) (Object) this;
            lastPosition = self.position();
        }
    }
    
    @Unique
    private static synchronized void akiasync$initBatchOptimization() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isCollisionOptimizationEnabled();
            slowMovementThreshold = bridge.getCollisionSkipMinMovement();
            
            BridgeConfigCache.debugLog("[AkiAsync] BatchCollisionOptimizationMixin initialized: enabled=" + enabled + 
                ", slowThreshold=" + slowMovementThreshold +
                ", slowInterval=" + slowMovementInterval +
                ", fastThreshold=" + fastMovementThreshold);
            
            initialized = true;
        }
    }
}
