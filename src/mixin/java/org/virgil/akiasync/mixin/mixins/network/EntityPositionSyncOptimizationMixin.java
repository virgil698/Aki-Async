package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(ServerEntity.class)
public class EntityPositionSyncOptimizationMixin {
    
    @Shadow @Final private Entity entity;
    @Shadow private int tickCount;
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile double minPositionChange = 0.01;
    @Unique
    private static volatile double minVelocityChange = 0.005;
    
    @Unique
    private Vec3 lastBroadcastPosition = Vec3.ZERO;
    @Unique
    private Vec3 lastBroadcastVelocity = Vec3.ZERO;
    @Unique
    private int ticksSinceLastBroadcast = 0;
    
    @Unique
    private static volatile long totalPackets = 0;
    @Unique
    private static volatile long skippedPackets = 0;
    
    @Inject(
        method = "sendChanges",
        at = @At("HEAD"),
        cancellable = true
    )
    private void optimizeBroadcast(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initOptimization();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            if (entity instanceof ServerPlayer) {
                return;
            }
            
            Vec3 currentPosition = entity.position();
            Vec3 currentVelocity = entity.getDeltaMovement();
            
            boolean shouldBroadcast = false;
            
            if (entity.hasImpulse || entity.getEntityData().isDirty() || tickCount % 60 == 0) {
                shouldBroadcast = true;
            } else {
                if (lastBroadcastPosition != null) {
                    double positionChange = currentPosition.distanceTo(lastBroadcastPosition);
                    if (positionChange >= minPositionChange) {
                        shouldBroadcast = true;
                    }
                }
                
                if (lastBroadcastVelocity != null && !shouldBroadcast) {
                    double velocityChange = currentVelocity.distanceTo(lastBroadcastVelocity);
                    if (velocityChange >= minVelocityChange) {
                        shouldBroadcast = true;
                    }
                }
            }
            
            if (!shouldBroadcast) {
                ticksSinceLastBroadcast++;
                skippedPackets++;
                ci.cancel();
                return;
            }
            
            lastBroadcastPosition = currentPosition;
            lastBroadcastVelocity = currentVelocity;
            ticksSinceLastBroadcast = 0;
            totalPackets++;
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityPositionSyncOptimization", "sendChanges", e);
        }
    }
    
    @Unique
    private static void akiasync$initOptimization() {
        if (initialized) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isVelocityCompressionEnabled();
                
                bridge.debugLog("[EntityPositionSyncOptimization] Initialized: enabled=%s", enabled);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityPositionSyncOptimization", "init", e);
        }
        
        initialized = true;
    }
}
