package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
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
    private static volatile double minPositionChange = 0.03;
    @Unique
    private static volatile double minVelocityChange = 0.01;
    
    @Unique
    private Vec3 lastBroadcastPosition = null;
    @Unique
    private Vec3 lastBroadcastVelocity = null;
    @Unique
    private int ticksSinceLastBroadcast = 0;
    
    @Unique
    private static final int FORCE_SYNC_INTERVAL = 20;
    
    @Unique
    private static final double DIMENSION_CHANGE_THRESHOLD = 100.0;
    
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
                totalPackets++;
                return;
            }
            
            if (entity instanceof EnderDragon || 
                entity instanceof EnderDragonPart ||
                entity instanceof WitherBoss) {
                totalPackets++;
                return;
            }
            
            if (entity.getEntityData().isDirty()) {
                totalPackets++;
                akiasync$updateLastBroadcast();
                return;
            }
            
            if (entity.hasImpulse) {
                totalPackets++;
                akiasync$updateLastBroadcast();
                return;
            }
            
            if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                if (livingEntity.hurtTime > 0 || livingEntity.deathTime > 0) {
                    totalPackets++;
                    akiasync$updateLastBroadcast();
                    return;
                }
            }
            
            ticksSinceLastBroadcast++;
            if (ticksSinceLastBroadcast >= FORCE_SYNC_INTERVAL) {
                totalPackets++;
                akiasync$updateLastBroadcast();
                ticksSinceLastBroadcast = 0;
                return;
            }
            
            
            Vec3 currentPosition = entity.position();
            Vec3 currentVelocity = entity.getDeltaMovement();
            
            if (lastBroadcastPosition == null) {
                totalPackets++;
                akiasync$updateLastBroadcast();
                return;
            }
            
            double positionChange = currentPosition.distanceTo(lastBroadcastPosition);
            if (positionChange >= DIMENSION_CHANGE_THRESHOLD) {
                totalPackets++;
                akiasync$updateLastBroadcast();
                ticksSinceLastBroadcast = 0;
                return;
            }
            
            if (positionChange >= minPositionChange) {
                totalPackets++;
                akiasync$updateLastBroadcast();
                ticksSinceLastBroadcast = 0;
                return;
            }
            
            if (lastBroadcastVelocity != null) {
                double velocityChange = currentVelocity.distanceTo(lastBroadcastVelocity);
                if (velocityChange >= minVelocityChange) {
                    totalPackets++;
                    akiasync$updateLastBroadcast();
                    ticksSinceLastBroadcast = 0;
                    return;
                }
            }
            
            skippedPackets++;
            ci.cancel();
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityPositionSyncOptimization", "sendChanges", e);
        }
    }
    
    @Unique
    private void akiasync$updateLastBroadcast() {
        lastBroadcastPosition = entity.position();
        lastBroadcastVelocity = entity.getDeltaMovement();
    }
    
    @Unique
    private static synchronized void akiasync$initOptimization() {
        if (initialized) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isVelocityCompressionEnabled();
                
                bridge.debugLog("[EntityPositionSyncOptimization] Initialized: enabled=%s", enabled);
                bridge.debugLog("[EntityPositionSyncOptimization] Boss entities excluded, force sync every %d ticks", FORCE_SYNC_INTERVAL);
                
                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityPositionSyncOptimization", "init", e);
        }
    }
}
