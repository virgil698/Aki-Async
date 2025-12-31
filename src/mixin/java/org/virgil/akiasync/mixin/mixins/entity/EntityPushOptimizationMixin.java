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
        
        if (self.isOnPortalCooldown() || other.isOnPortalCooldown()) {
            return;
        }
        
        if (akiasync$isInPortal(self) || akiasync$isInPortal(other)) {
            return;
        }
        
        if (self.isRemoved() || other.isRemoved()) {
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
        
        if (akiasync$isSuffocating(self)) {
            return; 
        }
        
        double dx = self.getX() - other.getX();
        double dz = self.getZ() - other.getZ();
        double distSqr = dx * dx + dz * dz;
        
        if (distSqr < 0.0001 || distSqr > 1.0) {
            return;
        }
        
        if (!akiasync$canAccumulatePush(self.getId())) {
            return; 
        }
        
        double distance = Math.sqrt(distSqr);
        dx /= distance;
        dz /= distance;
        
        double pushFactor = 1.0 / distance;
        if (pushFactor > 1.0) {
            pushFactor = 1.0;
        }
        
        dx *= pushFactor * fixedPushStrength;
        dz *= pushFactor * fixedPushStrength;
        
        Vec3 push = new Vec3(dx, 0, dz);
        akiasync$recordPush(self.getId(), push);
        
        Vec3 currentDelta = self.getDeltaMovement();
        if (currentDelta == null) return;
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
    private boolean akiasync$canAccumulatePush(int entityId) {
        PushAccumulator accumulator = pushAccumulators.get(entityId);
        if (accumulator == null) {
            return true; 
        }
        
        double totalSqr = accumulator.totalX * accumulator.totalX + accumulator.totalZ * accumulator.totalZ;
        double maxPushSqr = maxPushPerTick * maxPushPerTick;
        
        return totalSqr <= maxPushSqr;
    }
    
    @Unique
    private void akiasync$recordPush(int entityId, Vec3 push) {
        PushAccumulator accumulator = pushAccumulators.computeIfAbsent(
            entityId,
            k -> new PushAccumulator()
        );
        
        accumulator.totalX += Math.abs(push.x);
        accumulator.totalZ += Math.abs(push.z);
        accumulator.count++;
    }
    
    @Unique
    private boolean akiasync$isBeingPushedByFluid(Entity entity) {
        try {
            
            if (!entity.isInWater()) {
                return false;
            }
            
            Vec3 delta = entity.getDeltaMovement();
            if (delta == null) return false;
            
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
    private boolean akiasync$isInPortal(Entity entity) {
        try {
            net.minecraft.core.BlockPos pos = entity.blockPosition();
            net.minecraft.world.level.block.state.BlockState state = entity.level().getBlockState(pos);
            
            if (state == null) return false;
            
            return state.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock ||
                   state.getBlock() instanceof net.minecraft.world.level.block.EndPortalBlock;
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityPushOptimizationMixin", "isInPortal", e);
            return false;
        }
    }
    
    @Unique
    private boolean akiasync$isSuffocating(Entity entity) {
        try {
            net.minecraft.core.BlockPos pos = entity.blockPosition();
            net.minecraft.world.level.block.state.BlockState state = entity.level().getBlockState(pos);
            
            if (state == null || state.isAir()) {
                return false;
            }
            
            return state.isSuffocating(entity.level(), pos);
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityPushOptimizationMixin", "isSuffocating", e);
            return false;
        }
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
