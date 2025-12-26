package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.SectionEntityGrid;
import org.virgil.akiasync.mixin.util.collision.VectorizedCollisionDetector;
import org.virgil.akiasync.mixin.util.collision.CollisionBlockCache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = LivingEntity.class, priority = 1000)
public abstract class AdvancedCollisionOptimizationMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    
    @Unique
    private static volatile int collisionThreshold = 8;
    
    @Unique
    private static volatile double suffocationDamage = 0.5;
    
    @Unique
    private static volatile int maxPushIterations = 8;
    
    @Unique
    private static volatile boolean vectorizedEnabled = true;
    
    @Unique
    private static volatile int vectorizedThreshold = 64;
    
    @Unique
    private static volatile boolean blockCacheEnabled = true;
    
    @Unique
    private static final Map<Integer, SectionEntityGrid> LEVEL_GRIDS = new ConcurrentHashMap<>();
    
    @Unique
    private int akiasync$pushIterationCount = 0;
    
    @Unique
    private long akiasync$lastCollisionCheck = 0;
    
    @Unique
    private int akiasync$cachedCollisionCount = 0;
    
    @Unique
    private long akiasync$lastBlockCacheCleanup = 0;
    
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
                vectorizedEnabled = bridge.isVectorizedCollisionEnabled();
                vectorizedThreshold = bridge.getVectorizedCollisionThreshold();
                blockCacheEnabled = bridge.isCollisionBlockCacheEnabled();
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
        
        if (blockCacheEnabled) {
            long currentTime = self.level().getGameTime();
            if (currentTime - akiasync$lastBlockCacheCleanup >= 600) { 
                CollisionBlockCache cache = CollisionBlockCache.getOrCreate(self.level());
                cache.cleanup(currentTime);
                akiasync$lastBlockCacheCleanup = currentTime;
            }
        }
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
            akiasync$applySuffocationDamage(self, akiasync$cachedCollisionCount);
            akiasync$pushIterationCount = 0;
            return;
        }
        
        if (vectorizedEnabled && akiasync$cachedCollisionCount >= vectorizedThreshold) {
            akiasync$handleVectorizedPush(self);
            ci.cancel();
            return;
        }
        
        akiasync$pushIterationCount++;
        if (akiasync$pushIterationCount > maxPushIterations) {
            akiasync$pushIterationCount = 0;
            ci.cancel();
            
        }
    }
    
    @Unique
    private void akiasync$applySuffocationDamage(LivingEntity entity, int collisionCount) {
        if (collisionCount < collisionThreshold * 1.5) {
            return;
        }
        
        try {
            double damageMultiplier = (collisionCount - collisionThreshold) / (double) collisionThreshold;
            double damage = suffocationDamage * damageMultiplier;
            
            if (damage > 0 && entity.level().getGameTime() % 20 == 0) {
                entity.hurt(entity.damageSources().inWall(), (float) damage);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "AdvancedCollisionOptimization", "applySuffocationDamage", e);
        }
    }
    
    @Unique
    private void akiasync$handleVectorizedPush(LivingEntity self) {
        try {
            AABB searchBox = self.getBoundingBox().inflate(0.2);
            
            List<Entity> nearbyEntities = self.level().getEntities(
                self,
                searchBox,
                org.virgil.akiasync.mixin.util.EntitySelectorCache.PUSHABLE
            );
            
            if (nearbyEntities.isEmpty()) {
                return;
            }
            
            List<Entity> collidingEntities = VectorizedCollisionDetector.filterCollisions(
                searchBox,
                nearbyEntities
            );
            
            if (blockCacheEnabled) {
                CollisionBlockCache blockCache = CollisionBlockCache.getOrCreate(self.level());
                BlockPos feetPos = self.blockPosition();
                
                if (!blockCache.isCollidable(feetPos.below())) {
                    
                    for (Entity entity : collidingEntities) {
                        if (entity instanceof LivingEntity) {
                            akiasync$applyPushForce(self, entity, 0.5);
                        }
                    }
                    return;
                }
            }
            
            for (Entity entity : collidingEntities) {
                if (entity instanceof LivingEntity) {
                    akiasync$applyPushForce(self, entity, 1.0);
                }
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "AdvancedCollisionOptimization", "vectorizedPush", e);
        }
    }
    
    @Unique
    private void akiasync$applyPushForce(LivingEntity self, Entity other, double multiplier) {
        double dx = other.getX() - self.getX();
        double dz = other.getZ() - self.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        if (distance < 0.01) {
            return;
        }
        
        dx /= distance;
        dz /= distance;
        
        double pushFactor = 1.0 / distance;
        if (pushFactor > 1.0) {
            pushFactor = 1.0;
        }
        
        double force = pushFactor * 0.05 * multiplier;
        other.push(dx * force, 0, dz * force);
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
