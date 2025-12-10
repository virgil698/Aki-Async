package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.virgil.akiasync.mixin.util.PushAccumulator;

import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
@Mixin(Entity.class)
public abstract class EntityPushOptimizationMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile double maxPushPerTick = 0.5; 
    @Unique
    private static volatile double pushDampingFactor = 0.7; 
    @Unique
    private static volatile int highDensityThreshold = 10; 
    @Unique
    private static volatile double highDensityPushMultiplier = 0.3; 
    
    @Unique
    private static final ConcurrentHashMap<Integer, PushAccumulator> pushAccumulators = new ConcurrentHashMap<>();
    
    @Unique
    private static volatile long currentTick = 0;
    
    @Unique
    private static volatile long lastCleanupTick = 0;
    
    @Unique
    private static final int CLEANUP_INTERVAL = 5; 
    
    @Unique
    private static volatile double fixedPushStrength = 0.035; 
    
    @Inject(
        method = "push(Lnet/minecraft/world/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void optimizePush(Entity other, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initPushOptimization();
        }
        
        if (!enabled) {
            return;
        }
        
        Entity self = (Entity) (Object) this;
        
        if (akiasync$isExcludedEntity(self) || akiasync$isExcludedEntity(other)) {
            return;
        }
        
        if (self.isRemoved() || other.isRemoved()) {
            ci.cancel();
            return;
        }
        
        if (!self.isPushable() || !other.isPushable()) {
            return;
        }
        
        if (self.isInWater() || self.isInLava()) {
            return; 
        }
        
        if (akiasync$isBeingPushedByFluid(self)) {
            return; 
        }
        
        double dx = other.getX() - self.getX();
        double dz = other.getZ() - self.getZ();
        double distSqr = dx * dx + dz * dz;
        
        if (distSqr < 0.0001 || distSqr > 1.0) {
            return;
        }
        
        double invDistSqr = fixedPushStrength / distSqr;
        
        dx *= invDistSqr;
        dz *= invDistSqr;
        
        Vec3 push = new Vec3(dx, 0, dz);
        
        if (!akiasync$accumulatePush(self.getId(), push)) {
            
            ci.cancel();
            return;
        }
        
        Vec3 currentDelta = self.getDeltaMovement();
        self.setDeltaMovement(
            currentDelta.x + push.x,
            currentDelta.y,
            currentDelta.z + push.z
        );
        
        ci.cancel();
    }
    
    @Inject(
        method = "tick",
        at = @At("HEAD")
    )
    private void resetPushAccumulator(CallbackInfo ci) {
        if (!enabled) {
            return;
        }
        
        Entity self = (Entity) (Object) this;
        long tick = self.level().getGameTime();
        
        if (tick != currentTick) {
            currentTick = tick;
            
            if (tick - lastCleanupTick >= CLEANUP_INTERVAL) {
                pushAccumulators.clear();
                lastCleanupTick = tick;
            }
        }
    }
    
    @Unique
    private boolean akiasync$accumulatePush(int entityId, Vec3 push) {
        PushAccumulator accumulator = pushAccumulators.computeIfAbsent(
            entityId,
            k -> new PushAccumulator()
        );
        
        double newTotalX = accumulator.totalX + Math.abs(push.x);
        double newTotalZ = accumulator.totalZ + Math.abs(push.z);
        
        double newTotalSqr = newTotalX * newTotalX + newTotalZ * newTotalZ;
        double maxPushSqr = maxPushPerTick * maxPushPerTick;
        
        if (newTotalSqr > maxPushSqr) {
            return false;
        }
        
        accumulator.totalX = newTotalX;
        accumulator.totalZ = newTotalZ;
        accumulator.count++;
        
        return true;
    }
    
    @Unique
    private boolean akiasync$isBeingPushedByFluid(Entity entity) {
        try {
            
            if (!entity.isInWater()) {
                return false;
            }
            
            Vec3 delta = entity.getDeltaMovement();
            
            double horizontalSpeedSqr = delta.x * delta.x + delta.z * delta.z;
            
            return horizontalSpeedSqr > 0.0004; 
        } catch (Exception e) {
            
            return false;
        }
    }
    
    @Unique
    private static boolean akiasync$isExcludedEntity(Entity entity) {
        return org.virgil.akiasync.mixin.util.CollisionExclusionCache.isExcluded(entity);
    }

    @Unique
    private static synchronized void akiasync$initPushOptimization() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            
            enabled = bridge.isPushOptimizationEnabled();
            maxPushPerTick = bridge.getPushMaxPushPerTick();
            pushDampingFactor = bridge.getPushDampingFactor();
            highDensityThreshold = bridge.getPushHighDensityThreshold(); 
            highDensityPushMultiplier = bridge.getPushHighDensityMultiplier(); 
            
            fixedPushStrength = 0.05 * pushDampingFactor;
            
            bridge.debugLog("[AkiAsync] EntityPushOptimizationMixin initialized (optimized): enabled=" + enabled + 
                ", maxPushPerTick=" + maxPushPerTick + 
                ", fixedPushStrength=" + fixedPushStrength +
                " (density detection disabled for performance)");
            bridge.debugLog("[AkiAsync] TNT and TNT minecarts are excluded from push optimization for redstone machines");
        }
        
        initialized = true;
    }
}
