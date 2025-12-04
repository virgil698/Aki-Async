package org.virgil.akiasync.mixin.async.explosion.density;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class SakuraBlockDensityCache {
    public static final float UNKNOWN_DENSITY = -1.0f;
    

    private final Object2FloatOpenHashMap<BlockDensityCacheKey> exactCache = new Object2FloatOpenHashMap<>();
    

    private final Int2ObjectOpenHashMap<CachedBlockDensity> lenientCache = new Int2ObjectOpenHashMap<>();
    

    private static final Map<ServerLevel, SakuraBlockDensityCache> LEVEL_CACHES = new ConcurrentHashMap<>();
    
    private long lastExpireTime = 0;
    private static final int EXPIRE_INTERVAL = 600;

    public SakuraBlockDensityCache() {
        exactCache.defaultReturnValue(UNKNOWN_DENSITY);
    }

    public static SakuraBlockDensityCache getOrCreate(ServerLevel level) {
        return LEVEL_CACHES.computeIfAbsent(level, k -> new SakuraBlockDensityCache());
    }

    
    public float getBlockDensity(Vec3 explosionPos, Entity entity) {

        BlockDensityCacheKey exactKey = new BlockDensityCacheKey(explosionPos, entity.blockPosition());
        float exactDensity = exactCache.getFloat(exactKey);
        if (exactDensity != UNKNOWN_DENSITY) {
            return exactDensity;
        }


        int lenientKey = BlockDensityCacheKey.getLenientKey(explosionPos, entity.blockPosition());
        CachedBlockDensity cached = lenientCache.get(lenientKey);
        
        if (cached != null && cached.hasPosition(explosionPos, entity.getBoundingBox())) {
            return cached.blockDensity();
        }

        return UNKNOWN_DENSITY;
    }

    
    public void putBlockDensity(Vec3 explosionPos, Entity entity, float density) {

        BlockDensityCacheKey exactKey = new BlockDensityCacheKey(explosionPos, entity.blockPosition());
        exactCache.put(exactKey, density);


        int lenientKey = BlockDensityCacheKey.getLenientKey(explosionPos, entity.blockPosition());
        CachedBlockDensity existing = lenientCache.get(lenientKey);
        
        if (existing != null && existing.complete()) {

            existing.expand(explosionPos, entity);
        } else {

            lenientCache.put(lenientKey, new CachedBlockDensity(explosionPos, entity, density));
        }
    }

    
    public void expire(long currentTime) {
        if (currentTime - lastExpireTime < EXPIRE_INTERVAL) {
            return;
        }

        lastExpireTime = currentTime;


        if (exactCache.size() > 1000) {
            exactCache.clear();
        }
        
        if (lenientCache.size() > 500) {
            lenientCache.clear();
        }
    }

    
    public void clear() {
        exactCache.clear();
        lenientCache.clear();
    }

    
    public String getStats() {
        return String.format("Exact: %d, Lenient: %d", exactCache.size(), lenientCache.size());
    }
    
    
    public static void clearAllCaches() {
        for (SakuraBlockDensityCache cache : LEVEL_CACHES.values()) {
            cache.clear();
        }
        LEVEL_CACHES.clear();
    }
}
