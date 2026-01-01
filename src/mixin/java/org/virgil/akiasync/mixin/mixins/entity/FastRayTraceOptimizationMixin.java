package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.collision.RayCollisionDetector;
import org.virgil.akiasync.mixin.util.collision.RayCollisionDetectorManager;

@Mixin(value = Mob.class, priority = 950)
public abstract class FastRayTraceOptimizationMixin extends LivingEntity {
    
    protected FastRayTraceOptimizationMixin(net.minecraft.world.entity.EntityType<? extends LivingEntity> entityType, net.minecraft.world.level.Level level) {
        super(entityType, level);
    }
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile double maxDistance = 64.0;
    
    @Unique
    private long akiasync$lastRayCheck = 0;
    
    @Unique
    private boolean akiasync$cachedLineOfSight = false;
    
    @Unique
    private Vec3 akiasync$lastTargetPos = Vec3.ZERO;
    
    @Unique
    private static synchronized void akiasync$initRayTrace() {
        if (initialized) return;
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isRayCollisionEnabled();
                maxDistance = bridge.getRayCollisionMaxDistance();
                
                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "FastRayTraceOptimization", "initConfig", e);
        }
    }
    
    @Inject(
        method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void optimizeLineOfSight(net.minecraft.world.entity.Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!initialized) {
            akiasync$initRayTrace();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            
            double distance = this.distanceToSqr(target);
            if (distance > maxDistance * maxDistance) {
                cir.setReturnValue(false);
                return;
            }
            
            Mob self = (Mob) (Object) this;
            if (self.getTarget() == target || self.getLastHurtByMob() == target) {
                return;
            }
            
            if (self instanceof net.minecraft.world.entity.monster.Monster && distance < 256) {
                return;
            }
            
            long currentTime = this.level().getGameTime();
            Vec3 targetPos = target.position();
            
            if (currentTime - akiasync$lastRayCheck < 5 && 
                targetPos.distanceToSqr(akiasync$lastTargetPos) < 0.01) {
                cir.setReturnValue(akiasync$cachedLineOfSight);
                return;
            }
            
            RayCollisionDetector detector = RayCollisionDetectorManager.getDetector(this.level());
            boolean hasLineOfSight = detector.hasLineOfSight(this, target);
            
            akiasync$lastRayCheck = currentTime;
            akiasync$lastTargetPos = targetPos;
            akiasync$cachedLineOfSight = hasLineOfSight;
            
            cir.setReturnValue(hasLineOfSight);
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "FastRayTraceOptimization", "lineOfSight", e);
        }
    }
}
