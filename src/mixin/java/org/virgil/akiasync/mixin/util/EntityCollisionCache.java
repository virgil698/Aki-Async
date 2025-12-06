package org.virgil.akiasync.mixin.util;

import net.minecraft.world.entity.Entity;
import java.util.List;

public final class EntityCollisionCache {
    private static volatile int cacheLifetimeMs = 50;
    
    public final List<Entity> entities;
    public final long timestamp;
    
    public EntityCollisionCache(List<Entity> entities, long timestamp) {
        this.entities = entities;
        this.timestamp = timestamp;
    }
    
    public boolean isExpired(long currentTime) {
        return (currentTime - timestamp) > cacheLifetimeMs;
    }
    
    public static void setCacheLifetime(int lifetimeMs) {
        cacheLifetimeMs = lifetimeMs;
    }
}
