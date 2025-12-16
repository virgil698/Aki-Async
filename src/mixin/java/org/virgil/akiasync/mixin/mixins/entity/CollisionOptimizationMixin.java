package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings({"unused", "ConstantConditions"})
@Mixin(Entity.class)
public abstract class CollisionOptimizationMixin {
    @Unique
    private static volatile boolean enabled;
    @Unique
    private static volatile double minMovementSqr = 0.001D * 0.001D; 
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile int blockCheckInterval = 5; 
    
    @Unique
    private long akiasync$lastBlockCheckTick = 0;
    @Unique
    private Vec3 akiasync$lastBlockCheckPos = Vec3.ZERO;
    
    @Inject(method = "checkInsideBlocks", at = @At("HEAD"), cancellable = true)
    private void akiasync$optimizeBlockCheck(CallbackInfo ci) {
        if (!initialized) { akiasync$initCollisionOptimization(); }
        if (!enabled) return;
        
        Entity self = (Entity) (Object) this;
        if (self == null) return;
        
        if (org.virgil.akiasync.mixin.util.VirtualEntityCheck.is(self)) return;
        
        if (akiasync$isExcludedEntity(self)) return;
        
        if (akiasync$canSafelyReduceCheckFrequency(self)) {
            long currentTick = self.level().getGameTime();
            Vec3 currentPos = self.position();
            
            boolean positionChanged = currentPos.distanceToSqr(akiasync$lastBlockCheckPos) > 0.01;
            boolean enoughTicksPassed = (currentTick - akiasync$lastBlockCheckTick) >= blockCheckInterval;
            
            if (!positionChanged && !enoughTicksPassed) {
                
                ci.cancel();
                return;
            }
            
            akiasync$lastBlockCheckTick = currentTick;
            akiasync$lastBlockCheckPos = currentPos;
        }
        
    }
    
    @Unique
    @SuppressWarnings("RedundantIfStatement")
    private boolean akiasync$canSafelyReduceCheckFrequency(Entity entity) {
        
        if (entity.getDeltaMovement().lengthSqr() >= minMovementSqr) {
            return false;
        }
        
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) {
            return !item.isInLava() && !item.isOnFire() && item.getRemainingFireTicks() <= 0;
        }
        
        if (entity instanceof net.minecraft.world.entity.item.FallingBlockEntity falling) {
            double verticalSpeed = Math.abs(falling.getDeltaMovement().y);
            return verticalSpeed < 0.01; 
        }
        
        return entity instanceof net.minecraft.world.entity.ExperienceOrb; 
    }
    
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void optimizeEntityPush(Entity other, CallbackInfo ci) {
        if (!initialized) { akiasync$initCollisionOptimization(); }
        if (!enabled) return;
        Entity self = (Entity) (Object) this;

        if (org.virgil.akiasync.mixin.util.VirtualEntityCheck.isAny(self, other)) return;
        
        if (akiasync$isExcludedEntity(self) || akiasync$isExcludedEntity(other)) return;
        
        double selfMovementSqr = self.getDeltaMovement().lengthSqr();
        double otherMovementSqr = other.getDeltaMovement().lengthSqr();
        
        if (selfMovementSqr < minMovementSqr && otherMovementSqr < minMovementSqr) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean akiasync$isExcludedEntity(Entity entity) {
        return org.virgil.akiasync.mixin.util.CollisionExclusionCache.isExcluded(entity);
    }

    @Unique
    private static synchronized void akiasync$initCollisionOptimization() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isCollisionOptimizationEnabled();
            bridge.debugLog("[AkiAsync] CollisionOptimizationMixin initialized: enabled=" + enabled);
            bridge.debugLog("[AkiAsync]   - checkInsideBlocks: Frequency reduction for static non-critical entities");
            bridge.debugLog("[AkiAsync]   - push: Skip for completely static entities");
            bridge.debugLog("[AkiAsync]   - TNT and players are excluded from optimizations");
        } else {
            enabled = true;
        }
        initialized = true;
    }
}
