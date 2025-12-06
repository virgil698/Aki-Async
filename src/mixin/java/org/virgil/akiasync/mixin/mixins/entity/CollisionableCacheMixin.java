package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unused")
@Mixin(LivingEntity.class)
public abstract class CollisionableCacheMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile int cacheLifetimeMs = 50;
    
    @Unique
    private boolean cachedCollidable = true;
    @Unique
    private long lastCollidableCheck = 0;
    @Unique
    private int lastHealthState = 0; 
    @Unique
    private boolean lastIgnoreClimbing = false; 
    
    @Inject(
        method = "isCollidable(Z)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void cacheCollidable(boolean ignoreClimbing, CallbackInfoReturnable<Boolean> cir) {
        if (!initialized) {
            akiasync$initCollidableCache();
        }
        
        if (!enabled) {
            return;
        }
        
        LivingEntity self = (LivingEntity) (Object) this;
        long now = System.currentTimeMillis();
        
        int currentHealthState = akiasync$getHealthState(self);
        boolean stateChanged = currentHealthState != lastHealthState || ignoreClimbing != lastIgnoreClimbing;
        
        if (!stateChanged && (now - lastCollidableCheck) < cacheLifetimeMs) {
            cir.setReturnValue(cachedCollidable);
            return;
        }
        
        lastCollidableCheck = now;
        lastHealthState = currentHealthState;
        lastIgnoreClimbing = ignoreClimbing;
    }
    
    @Inject(
        method = "isCollidable(Z)Z",
        at = @At("RETURN"),
        remap = false
    )
    private void updateCollidableCache(boolean ignoreClimbing, CallbackInfoReturnable<Boolean> cir) {
        if (!enabled) {
            return;
        }
        
        cachedCollidable = cir.getReturnValue();
    }
    
    @Unique
    private int akiasync$getHealthState(LivingEntity entity) {
        int hash = 0;
        hash = hash * 31 + (entity.isRemoved() ? 1 : 0);
        hash = hash * 31 + (entity.isAlive() ? 1 : 0);
        hash = hash * 31 + (entity.isDeadOrDying() ? 1 : 0);
        hash = hash * 31 + (int) entity.getHealth();
        return hash;
    }
    
    @Unique
    private static synchronized void akiasync$initCollidableCache() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isCollisionOptimizationEnabled();
            cacheLifetimeMs = bridge.getCollisionCacheLifetimeMs();
            bridge.debugLog("[AkiAsync] CollisionableCacheMixin initialized: enabled=" + enabled + 
                ", cacheLifetime=" + cacheLifetimeMs + "ms (caching isCollidable)");
        }
        
        initialized = true;
    }
}
