package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = LivingEntity.class, priority = 900)
public abstract class SmartCollisionSamplingMixin extends Entity {
    
    public SmartCollisionSamplingMixin(net.minecraft.world.entity.EntityType<?> entityType, net.minecraft.world.level.Level level) {
        super(entityType, level);
    }
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile double samplingThreshold = 0.5; 
    
    @Unique
    private long lastFullCheck = 0;
    @Unique
    private static final long FULL_CHECK_INTERVAL = 100; 
    
    @Inject(
        method = "pushEntities",
        at = @At("HEAD"),
        cancellable = true
    )
    private void optimizePushEntitiesWithSampling(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initSmartSampling();
        }
        
        if (!enabled) {
            return;
        }
        
        LivingEntity self = (LivingEntity) (Object) this;
        
        if (org.virgil.akiasync.mixin.util.VirtualEntityCheck.is(self)) {
            return;
        }
        
        if (akiasync$isExcludedEntity(self)) {
            return;
        }
        
        Vec3 movement = getDeltaMovement();
        if (movement == null || movement.lengthSqr() < 0.0001) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFullCheck > FULL_CHECK_INTERVAL) {
            lastFullCheck = currentTime;
            return; 
        }
        
        if (!akiasync$quickSamplingCheck(self)) {
            ci.cancel();
            
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync-Collision] Sampling check: no collision detected, skipping pushEntities");
            }
        }
    }
    
    @Unique
    private boolean akiasync$quickSamplingCheck(Entity entity) {
        AABB box = entity.getBoundingBox();
        if (box == null) return true;
        
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;
        
        Vec3 movement = entity.getDeltaMovement();
        if (movement == null) return true;
        double expandX = Math.abs(movement.x) + samplingThreshold;
        double expandY = Math.abs(movement.y) + samplingThreshold;
        double expandZ = Math.abs(movement.z) + samplingThreshold;
        
        AABB expandedBox = new AABB(
            minX - expandX, minY - expandY, minZ - expandZ,
            maxX + expandX, maxY + expandY, maxZ + expandZ
        );
        
        List<Entity> nearbyEntities = entity.level().getEntities(
            entity,
            expandedBox,
            e -> e != null && !e.isRemoved() && e.isPickable()
        );
        
        if (nearbyEntities.isEmpty()) {
            return false;
        }
        
        if (nearbyEntities.size() <= 3) {
            return akiasync$eightPointSampling(entity, nearbyEntities, box);
        }
        
        return true;
    }
    
    @Unique
    private boolean akiasync$eightPointSampling(Entity entity, List<Entity> nearbyEntities, AABB box) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;
        
        double boxSize = (maxX - minX) + (maxY - minY) + (maxZ - minZ);
        boolean useFullSampling = boxSize > 2.0; 
        
        for (Entity nearby : nearbyEntities) {
            if (nearby == null || nearby.isRemoved()) {
                continue;
            }
            
            AABB nearbyBox = nearby.getBoundingBox();
            
            if (akiasync$pointInBox(nearbyBox, maxX, maxY, maxZ) ||  
                akiasync$pointInBox(nearbyBox, minX, minY, minZ) ||  
                akiasync$pointInBox(nearbyBox, maxX, minY, maxZ) ||  
                akiasync$pointInBox(nearbyBox, minX, maxY, minZ)) {  
                return true;
            }
            
            if (useFullSampling) {
                if (akiasync$pointInBox(nearbyBox, minX, maxY, maxZ) ||  
                    akiasync$pointInBox(nearbyBox, minX, minY, maxZ) ||  
                    akiasync$pointInBox(nearbyBox, maxX, maxY, minZ) ||  
                    akiasync$pointInBox(nearbyBox, maxX, minY, minZ)) {  
                    return true;
                }
            }
            
            if (akiasync$boxIntersectsBox(box, nearbyBox)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Unique
    private boolean akiasync$pointInBox(AABB box, double x, double y, double z) {
        return x >= box.minX && x <= box.maxX &&
               y >= box.minY && y <= box.maxY &&
               z >= box.minZ && z <= box.maxZ;
    }
    
    @Unique
    private boolean akiasync$boxIntersectsBox(AABB box1, AABB box2) {
        return box1.minX < box2.maxX && box1.maxX > box2.minX &&
               box1.minY < box2.maxY && box1.maxY > box2.minY &&
               box1.minZ < box2.maxZ && box1.maxZ > box2.minZ;
    }
    
    @Unique
    private static boolean akiasync$isExcludedEntity(Entity entity) {
        return org.virgil.akiasync.mixin.util.CollisionExclusionCache.isExcluded(entity);
    }
    
    @Unique
    private static synchronized void akiasync$initSmartSampling() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            
            enabled = bridge.isCollisionOptimizationEnabled() && 
                     bridge.isCollisionAggressiveMode();
            
            samplingThreshold = bridge.getCollisionSkipMinMovement() * 10;
            
            bridge.debugLog("[AkiAsync] SmartCollisionSamplingMixin initialized: enabled=" + enabled + 
                ", samplingThreshold=" + samplingThreshold);
        
            initialized = true;
        }

    }
}
