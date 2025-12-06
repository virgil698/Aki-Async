package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(LivingEntity.class)
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
        cancellable = true
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
        
        Vec3 currentPos = self.position();
        double movementSpeed = akiasync$calculateMovementSpeed(currentPos);
        
        int checkInterval = akiasync$getCheckInterval(movementSpeed);
        
        ticksSinceLastCollisionCheck++;
        
        if (ticksSinceLastCollisionCheck < checkInterval) {
            ci.cancel();
            return;
        }
        
        ticksSinceLastCollisionCheck = 0;
        lastPosition = currentPos;
    }
    
    @Unique
    private double akiasync$calculateMovementSpeed(Vec3 currentPos) {
        if (lastPosition == Vec3.ZERO) {
            lastPosition = currentPos;
            return 0;
        }
        
        double dx = currentPos.x - lastPosition.x;
        double dy = currentPos.y - lastPosition.y;
        double dz = currentPos.z - lastPosition.z;
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    @Unique
    private int akiasync$getCheckInterval(double movementSpeed) {
        if (movementSpeed < slowMovementThreshold) {
            
            return slowMovementInterval;
        } else if (movementSpeed > fastMovementThreshold) {
            
            return fastMovementInterval;
        } else {
            
            return 3 + (int) (movementSpeed * 4);
        }
    }
    
    @Inject(
        method = "tick",
        at = @At("HEAD")
    )
    private void resetCollisionCounter(CallbackInfo ci) {
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
            
            bridge.debugLog("[AkiAsync] BatchCollisionOptimizationMixin initialized: enabled=" + enabled + 
                ", slowThreshold=" + slowMovementThreshold +
                ", slowInterval=" + slowMovementInterval +
                ", fastThreshold=" + fastMovementThreshold);
        }
        
        initialized = true;
    }
}
