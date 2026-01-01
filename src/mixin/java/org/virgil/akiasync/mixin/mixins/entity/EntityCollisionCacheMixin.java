package org.virgil.akiasync.mixin.mixins.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.EntityCollisionCache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("unused")
@Mixin(Level.class)
public abstract class EntityCollisionCacheMixin implements org.virgil.akiasync.mixin.util.EntityCollisionCache.EntityCollisionCacheAccess {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile int cacheLifetimeMs = 50;
    @Unique
    private static volatile double movementThreshold = 0.01;
    
    @Unique
    private final Long2ObjectOpenHashMap<EntityCollisionCache> collisionCache = new Long2ObjectOpenHashMap<>();
    @Unique
    private long lastCacheCleanup = 0;
    @Unique
    private static final long CACHE_CLEANUP_INTERVAL = 1000;
    
    @Inject(
        method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cacheEntityCollisions(
        Entity except,
        AABB box,
        Predicate<? super Entity> predicate,
        CallbackInfoReturnable<List<Entity>> cir
    ) {
        if (!initialized) {
            akiasync$initCollisionCache();
        }
        
        if (!enabled) {
            return;
        }
        
        if (predicate == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheCleanup > CACHE_CLEANUP_INTERVAL) {
            akiasync$cleanupCache(currentTime);
            lastCacheCleanup = currentTime;
        }
        
        if (except != null) {
            net.minecraft.world.phys.Vec3 movement = except.getDeltaMovement();
            if (movement == null) return;
            double movementSqr = movement.lengthSqr();
            if (movementSqr < movementThreshold * movementThreshold) {
                long cacheKey = akiasync$generateCacheKey(except, box);
                EntityCollisionCache cache = collisionCache.get(cacheKey);
                
                if (cache != null && !cache.isExpired(currentTime)) {
                    if (cache.entities == null || cache.entities.isEmpty()) {
                        collisionCache.remove(cacheKey);
                        return;
                    }
                    
                    List<Entity> validEntities = new ArrayList<>(cache.entities.size());
                    for (Entity entity : cache.entities) {
                        if (entity != null && !entity.isRemoved() && predicate.test(entity)) {
                            validEntities.add(entity);
                        }
                    }
                    
                    if (validEntities.size() == cache.entities.size()) {
                        cir.setReturnValue(validEntities);
                        return;
                    }
                }
            }
        }
    }
    
    @Inject(
        method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
        at = @At("RETURN")
    )
    private void updateCollisionCache(
        Entity except,
        AABB box,
        Predicate<? super Entity> predicate,
        CallbackInfoReturnable<List<Entity>> cir
    ) {
        if (!initialized) {
            akiasync$initCollisionCache();
        }
        
        if (!enabled || except == null) {
            return;
        }
        
        net.minecraft.world.phys.Vec3 movement = except.getDeltaMovement();
        if (movement == null) return;
        double movementSqr = movement.lengthSqr();
        if (movementSqr < movementThreshold * movementThreshold) {
            long cacheKey = akiasync$generateCacheKey(except, box);
            List<Entity> result = cir.getReturnValue();
            
            if (result != null && result.size() < 100) {
                collisionCache.put(cacheKey, new EntityCollisionCache(
                    new ArrayList<>(result),
                    System.currentTimeMillis()
                ));
            }
        }
    }
    
    @Unique
    private long akiasync$generateCacheKey(Entity entity, AABB box) {
        
        long key = entity.getId();
        
        int quantizedX = (int) (box.minX * 2);
        int quantizedY = (int) (box.minY * 2);
        int quantizedZ = (int) (box.minZ * 2);
        
        int sizeX = (int) ((box.maxX - box.minX) * 10);
        int sizeY = (int) ((box.maxY - box.minY) * 10);
        
        key = (key << 20) ^ quantizedX;
        key = (key << 20) ^ quantizedY;
        key = (key << 20) ^ quantizedZ;
        key = (key << 8) ^ sizeX;
        key = (key << 8) ^ sizeY;
        
        return key;
    }
    
    @Unique
    private void akiasync$cleanupCache(long currentTime) {
        collisionCache.values().removeIf(cache -> {
            if (cache == null) {
                return true;
            }
            return cache.isExpired(currentTime);
        });
    }
    
    @Unique
    private static synchronized void akiasync$initCollisionCache() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isCollisionOptimizationEnabled();
            EntityCollisionCache.setCacheLifetime(cacheLifetimeMs);
            bridge.debugLog("[AkiAsync] EntityCollisionCacheMixin initialized: enabled=" + enabled);
        
            initialized = true;
        }

    }
    
    @Override
    public void akiasync$clearEntityCache(Entity entity) {
        if (entity == null) return;
        
        int entityId = entity.getId();
        collisionCache.long2ObjectEntrySet().removeIf(entry -> {
            if (entry == null || entry.getValue() == null) {
                return true;
            }
            long key = entry.getLongKey();
            
            int cachedEntityId = (int) (key >>> 44);
            return cachedEntityId == entityId;
        });
    }
}
