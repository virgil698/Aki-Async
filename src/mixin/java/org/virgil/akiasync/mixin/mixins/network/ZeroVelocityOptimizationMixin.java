package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public class ZeroVelocityOptimizationMixin {
    
    @Shadow @Final private Entity entity;
    
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile double velocityThreshold = 0.003;
    
    @Unique
    private Vec3 lastPosition = Vec3.ZERO;
    @Unique
    private Vec3 lastVelocity = Vec3.ZERO;
    @Unique
    private int ticksSinceLastUpdate = 0;
    @Unique
    private static final int MAX_TICKS_WITHOUT_UPDATE = 20; 
    
    @Inject(
        method = "sendDirtyEntityData",
        at = @At("HEAD"),
        cancellable = true
    )
    private void skipZeroVelocityUpdate(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initConfig();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            Vec3 currentPos = entity.position();
            Vec3 currentVelocity = entity.getDeltaMovement();
            
            boolean velocityNearZero = Math.abs(currentVelocity.x) < velocityThreshold &&
                                      Math.abs(currentVelocity.y) < velocityThreshold &&
                                      Math.abs(currentVelocity.z) < velocityThreshold;
            
            boolean positionUnchanged = currentPos.equals(lastPosition);
            
            boolean velocityUnchanged = currentVelocity.equals(lastVelocity);
            
            ticksSinceLastUpdate++;
            
            if (velocityNearZero && positionUnchanged && velocityUnchanged && 
                ticksSinceLastUpdate < MAX_TICKS_WITHOUT_UPDATE) {
                ci.cancel();
                return;
            }
            
            if (entity.onGround() && velocityNearZero && positionUnchanged && 
                ticksSinceLastUpdate < MAX_TICKS_WITHOUT_UPDATE) {
                ci.cancel();
                return;
            }
            
            lastPosition = currentPos;
            lastVelocity = currentVelocity;
            ticksSinceLastUpdate = 0;
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ZeroVelocityOptimization", "skipZeroVelocityUpdate", e);
        }
    }
    
    @Unique
    private static synchronized void akiasync$initConfig() {
        if (initialized) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                
                enabled = true;
                
                bridge.debugLog("[ZeroVelocityOptimization] Initialized: enabled=%s, threshold=%.4f",
                    enabled, velocityThreshold);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ZeroVelocityOptimization", "initConfig", e);
        }
        
        initialized = true;
    }
}
