package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
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
    private static volatile double velocityThreshold = 0.001;
    @Unique
    private static volatile double velocityDeltaThreshold = 0.0005;
    @Unique
    private static volatile boolean velocityCompressionEnabled = true;
    
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
            
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                return;
            }
            
            if (entity instanceof EnderDragon || 
                entity instanceof EnderDragonPart ||
                entity instanceof WitherBoss) {
                return;
            }
            
            if (entity.hasImpulse) {
                ticksSinceLastUpdate = 0;
                lastPosition = entity.position();
                lastVelocity = entity.getDeltaMovement();
                return;
            }
            
            if (entity instanceof LivingEntity living) {
                if (living.hurtTime > 0 || living.deathTime > 0) {
                    ticksSinceLastUpdate = 0;
                    lastPosition = entity.position();
                    lastVelocity = entity.getDeltaMovement();
                    return;
                }
            }
            
            if (entity instanceof net.minecraft.world.entity.monster.RangedAttackMob) {
                return;
            }
            
            if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null) {
                return;
            }
            
            SynchedEntityData entityData = entity.getEntityData();
            if (entityData.isDirty()) {
                ticksSinceLastUpdate = 0;
                lastPosition = entity.position();
                lastVelocity = entity.getDeltaMovement();
                return;
            }
            
            if (entity instanceof LivingEntity living) {
                Set<AttributeInstance> attributesToSync = living.getAttributes().getAttributesToSync();
                if (!attributesToSync.isEmpty()) {
                    ticksSinceLastUpdate = 0;
                    lastPosition = entity.position();
                    lastVelocity = entity.getDeltaMovement();
                    return;
                }
            }
            
            ticksSinceLastUpdate++;
            if (ticksSinceLastUpdate >= MAX_TICKS_WITHOUT_UPDATE) {
                ticksSinceLastUpdate = 0;
                lastPosition = entity.position();
                lastVelocity = entity.getDeltaMovement();
                return;
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
                return;
            }
            
            if (velocityCompressionEnabled && lastVelocity != null) {
                double velocityDeltaX = Math.abs(currentVelocity.x - lastVelocity.x);
                double velocityDeltaY = Math.abs(currentVelocity.y - lastVelocity.y);
                double velocityDeltaZ = Math.abs(currentVelocity.z - lastVelocity.z);
                
                boolean velocityBarelyChanged = 
                    velocityDeltaX < velocityDeltaThreshold &&
                    velocityDeltaY < velocityDeltaThreshold &&
                    velocityDeltaZ < velocityDeltaThreshold;
                
                if (velocityBarelyChanged && positionDelta < POSITION_THRESHOLD * 0.5) {
                    skippedCalls++;
                    ci.cancel();
                    return;
                }
            }
            
            boolean velocityNearZero = Math.abs(currentVelocity.x) < velocityThreshold &&
                                      Math.abs(currentVelocity.y) < velocityThreshold &&
                                      Math.abs(currentVelocity.z) < velocityThreshold;
            
            boolean velocityUnchanged = currentVelocity.equals(lastVelocity);
            
            if (velocityNearZero && velocityUnchanged) {
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
                velocityCompressionEnabled = bridge.isVelocityCompressionEnabled();
                
                bridge.debugLog("[ZeroVelocityOptimization] Initialized: enabled=%s, velocityCompression=%s",
                    enabled, velocityCompressionEnabled);
                bridge.debugLog("[ZeroVelocityOptimization] Boss entities excluded, force sync every %d ticks", MAX_TICKS_WITHOUT_UPDATE);
                
                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ZeroVelocityOptimization", "initConfig", e);
        }
    }
}
