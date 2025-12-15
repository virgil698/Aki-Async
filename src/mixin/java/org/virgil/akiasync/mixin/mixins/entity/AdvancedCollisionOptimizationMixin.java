package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.SectionEntityGrid;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = LivingEntity.class, priority = 1000)
public abstract class AdvancedCollisionOptimizationMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    
    @Unique
    private static volatile int collisionThreshold = 8;
    
    @Unique
    private static volatile float suffocationDamage = 0.5f;
    
    @Unique
    private static volatile int maxPushIterations = 8;
    
    @Unique
    private static final Map<Integer, SectionEntityGrid> LEVEL_GRIDS = new ConcurrentHashMap<>();
    
    @Unique
    private int akiasync$pushIterationCount = 0;
    
    @Unique
    private long akiasync$lastCollisionCheck = 0;
    
    @Unique
    private int akiasync$cachedCollisionCount = 0;
    
    static {
        akiasync$initAdvancedCollision();
    }
    
    @Unique
    private static void akiasync$initAdvancedCollision() {
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isAdvancedCollisionOptimizationEnabled();
                collisionThreshold = bridge.getCollisionThreshold();
                suffocationDamage = bridge.getSuffocationDamage();
                maxPushIterations = bridge.getMaxPushIterations();
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "AdvancedCollisionOptimization", "initConfig", e);
        }
    }
    
    @Unique
    private SectionEntityGrid akiasync$getGrid() {
        LivingEntity self = (LivingEntity) (Object) this;
        int levelId = System.identityHashCode(self.level());
        return LEVEL_GRIDS.computeIfAbsent(levelId, k -> new SectionEntityGrid());
    }
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void updateSectionGrid(CallbackInfo ci) {
        
        if (!enabled) {
            return;
        }
        
        LivingEntity self = (LivingEntity) (Object) this;
        
        if (!self.isAlive()) {
            return;
        }
        
        SectionEntityGrid grid = akiasync$getGrid();
        grid.updateEntity(self);
    }
    
    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true, require = 0)
    private void optimizePushEntities(CallbackInfo ci) {
        
        if (!enabled) {
            return;
        }
        
        LivingEntity self = (LivingEntity) (Object) this;
        long currentTime = self.level().getGameTime();
        
        if (currentTime - akiasync$lastCollisionCheck >= 5) {
            SectionEntityGrid grid = akiasync$getGrid();
            akiasync$cachedCollisionCount = grid.countCollisions(self, 2.0);
            akiasync$lastCollisionCheck = currentTime;
        }
        
        if (akiasync$cachedCollisionCount >= collisionThreshold) {
            
            if (currentTime % 20 == 0) { 
                DamageSources damageSources = self.level().damageSources();
                self.hurt(damageSources.inWall(), suffocationDamage);
            }
            
            akiasync$pushIterationCount = 0;
            ci.cancel();
            return;
        }
        
        akiasync$pushIterationCount++;
        if (akiasync$pushIterationCount > maxPushIterations) {
            akiasync$pushIterationCount = 0;
            ci.cancel();
            
        }
    }
    
    @Inject(method = "remove", at = @At("HEAD"))
    private void cleanupSectionGrid(CallbackInfo ci) {
        if (!enabled) {
            return;
        }
        
        LivingEntity self = (LivingEntity) (Object) this;
        SectionEntityGrid grid = akiasync$getGrid();
        grid.removeEntity(self);
    }
}
