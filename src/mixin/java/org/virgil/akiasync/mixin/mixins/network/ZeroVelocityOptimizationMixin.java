package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

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
    
    @Unique
    private boolean lastFallFlying = false;
    @Unique
    private boolean lastSprinting = false;
    @Unique
    private boolean lastOnGround = false;
    @Unique
    private boolean lastInWater = false;
    
    @Unique
    private static volatile long totalCalls = 0;
    @Unique
    private static volatile long skippedCalls = 0;
    @Unique
    private static volatile long dataSyncCalls = 0;
    @Unique
    private static volatile long stateChangeCalls = 0; 
    
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
        
        totalCalls++;
        
        try {
            if (entity.hasImpulse) {
                ticksSinceLastUpdate = 0;
                lastPosition = entity.position();
                lastVelocity = entity.getDeltaMovement();
                return;
            }
            
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                return;
            }
            
            if (entity instanceof net.minecraft.world.entity.monster.RangedAttackMob) {
                return;
            }
            
            if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null) {
                return;
            }
            
            if (entity instanceof net.minecraft.world.entity.Mob mob && mob.isSunBurnTick()) {
                return;
            }
            
            SynchedEntityData entityData = entity.getEntityData();
            if (entityData.isDirty()) {
                ticksSinceLastUpdate = 0;
                lastPosition = entity.position();
                lastVelocity = entity.getDeltaMovement();
                dataSyncCalls++;
                return;
            }
            
            if (entity instanceof LivingEntity living) {
                Set<AttributeInstance> attributesToSync = living.getAttributes().getAttributesToSync();
                if (!attributesToSync.isEmpty()) {
                    ticksSinceLastUpdate = 0;
                    lastPosition = entity.position();
                    lastVelocity = entity.getDeltaMovement();
                    dataSyncCalls++;
                    return;
                }
            }
            
            Vec3 currentPos = entity.position();
            Vec3 currentVelocity = entity.getDeltaMovement();
            
            double positionDelta = currentPos.distanceTo(lastPosition);
            double POSITION_THRESHOLD = 0.01;
            
            if (positionDelta > POSITION_THRESHOLD) {
                ticksSinceLastUpdate = 0;
                lastPosition = currentPos;
                lastVelocity = currentVelocity;
                return;
            }
            
            boolean stateChanged = false;
            if (entity instanceof LivingEntity living) {
                boolean currentFallFlying = living.isFallFlying();
                boolean currentSprinting = living.isSprinting();
                
                if (currentFallFlying != lastFallFlying || currentSprinting != lastSprinting) {
                    stateChanged = true;
                    lastFallFlying = currentFallFlying;
                    lastSprinting = currentSprinting;
                }
            }
            
            boolean currentOnGround = entity.onGround();
            boolean currentInWater = entity.isInWater();
            
            if (currentOnGround != lastOnGround || currentInWater != lastInWater) {
                stateChanged = true;
                lastOnGround = currentOnGround;
                lastInWater = currentInWater;
            }
            
            if (stateChanged) {
                ticksSinceLastUpdate = 0;
                lastPosition = currentPos;
                lastVelocity = currentVelocity;
                stateChangeCalls++;
                return;
            }
            
            boolean velocityNearZero = Math.abs(currentVelocity.x) < velocityThreshold &&
                                      Math.abs(currentVelocity.y) < velocityThreshold &&
                                      Math.abs(currentVelocity.z) < velocityThreshold;
            
            boolean velocityUnchanged = currentVelocity.equals(lastVelocity);
            
            ticksSinceLastUpdate++;
            
            if (velocityNearZero && velocityUnchanged && 
                ticksSinceLastUpdate < MAX_TICKS_WITHOUT_UPDATE) {
                skippedCalls++;
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
    
    @Unique
    private static String akiasync$getStats() {
        if (totalCalls == 0) {
            return "ZeroVelocityOptimization: No data";
        }
        
        double skipRate = (double) skippedCalls / totalCalls * 100.0;
        double dataSyncRate = (double) dataSyncCalls / totalCalls * 100.0;
        double stateChangeRate = (double) stateChangeCalls / totalCalls * 100.0;
        
        return String.format(
            "ZeroVelocityOptimization: total=%d, skipped=%d (%.1f%%), dataSync=%d (%.1f%%), stateChange=%d (%.1f%%)",
            totalCalls, skippedCalls, skipRate, dataSyncCalls, dataSyncRate, stateChangeCalls, stateChangeRate
        );
    }
    
    @Unique
    private static void akiasync$resetStats() {
        totalCalls = 0;
        skippedCalls = 0;
        dataSyncCalls = 0;
        stateChangeCalls = 0;
    }
}
