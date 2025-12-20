package org.virgil.akiasync.mixin.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class CollisionableCache {
    
    private CollisionableCache() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static void clearCacheForEntity(Entity entity) {
        if (entity instanceof LivingEntity living) {
            
            if (living instanceof CollisionableCacheAccess access) {
                access.akiasync$invalidateCache();
            }
        }
    }
    
    public interface CollisionableCacheAccess {
        void akiasync$invalidateCache();
    }
}
