package org.virgil.akiasync.mixin.async.explosion.density;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class SakuraBlockDensityCache {
    public static final float UNKNOWN_DENSITY = -1.0f;
    
    private final Object2FloatOpenHashMap<BlockDensityCacheKey> exactCache = new Object2FloatOpenHashMap<>();
    
    private final Int2ObjectOpenHashMap<CachedBlockDensity> lenientCache = new Int2ObjectOpenHashMap<>();
    
    private final Object2ObjectOpenHashMap<IdBlockPos, Float> spatialIndex = new Object2ObjectOpenHashMap<>();
    
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
        spatialIndex.clear();
    }

    public String getStats() {
        return String.format("Exact: %d, Lenient: %d, SpatialIndex: %d", 
            exactCache.size(), lenientCache.size(), spatialIndex.size());
    }
    
    public static void clearAllCaches() {
        for (SakuraBlockDensityCache cache : LEVEL_CACHES.values()) {
            cache.clear();
        }
        LEVEL_CACHES.clear();
    }
    
    public static void clearLevelCache(ServerLevel level) {
        SakuraBlockDensityCache cache = LEVEL_CACHES.remove(level);
        if (cache != null) {
            cache.clear();
        }
    }
    
    public void putSpatialIndex(Vec3 explosionPos, Vec3 entityPos, UUID entityId, float density) {
        
        int x = ((int) Math.floor(entityPos.x)) & 15;
        int y = ((int) Math.floor(entityPos.y)) & 15;
        int z = ((int) Math.floor(entityPos.z)) & 15;
        
        IdBlockPos idPos = new IdBlockPos(x, y, z, entityId, density);
        spatialIndex.put(idPos, density);
    }
    
    public List<IdBlockPos> querySpatialIndex(AABB explosionAABB) {
        List<IdBlockPos> result = new ArrayList<>();
        
        int minX = Math.max(0, ((int) Math.floor(explosionAABB.minX)) & 15);
        int minY = Math.max(0, ((int) Math.floor(explosionAABB.minY)) & 15);
        int minZ = Math.max(0, ((int) Math.floor(explosionAABB.minZ)) & 15);
        int maxX = Math.min(15, ((int) Math.ceil(explosionAABB.maxX)) & 15);
        int maxY = Math.min(15, ((int) Math.ceil(explosionAABB.maxY)) & 15);
        int maxZ = Math.min(15, ((int) Math.ceil(explosionAABB.maxZ)) & 15);
        
        int startKey = minY * 1000000 + minZ * 1000 + minX;
        int endKey = maxY * 1000000 + maxZ * 1000 + maxX;
        
        for (Map.Entry<IdBlockPos, Float> entry : spatialIndex.entrySet()) {
            IdBlockPos idPos = entry.getKey();
            int linearKey = idPos.getLinearKey();
            
            if (linearKey >= startKey && linearKey <= endKey) {
                
                if (idPos.getX() >= minX && idPos.getX() <= maxX &&
                    idPos.getY() >= minY && idPos.getY() <= maxY &&
                    idPos.getZ() >= minZ && idPos.getZ() <= maxZ) {
                    result.add(idPos);
                }
            }
        }
        
        return result;
    }
    
    public float getDensityByPosition(int x, int y, int z, UUID entityId) {
        if (entityId == null) return UNKNOWN_DENSITY;
        
        IdBlockPos searchKey = new IdBlockPos(x, y, z, entityId, 0.0f);
        
        for (Map.Entry<IdBlockPos, Float> entry : spatialIndex.entrySet()) {
            IdBlockPos idPos = entry.getKey();
            if (searchKey.strictEquals(idPos)) {
                return entry.getValue();
            }
        }
        
        return UNKNOWN_DENSITY;
    }
    
    public float getDensityByUUID(UUID entityId) {
        if (entityId == null) return UNKNOWN_DENSITY;
        
        for (Map.Entry<IdBlockPos, Float> entry : spatialIndex.entrySet()) {
            IdBlockPos idPos = entry.getKey();
            if (entityId.equals(idPos.getEntityId())) {
                return entry.getValue();
            }
        }
        
        return UNKNOWN_DENSITY;
    }
    
    public boolean containsEntity(UUID entityId) {
        if (entityId == null) return false;
        
        for (IdBlockPos idPos : spatialIndex.keySet()) {
            if (entityId.equals(idPos.getEntityId())) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean containsEntityAtPosition(int x, int y, int z, UUID entityId) {
        if (entityId == null) return false;
        
        IdBlockPos searchKey = new IdBlockPos(x, y, z, entityId, 0.0f);
        
        for (IdBlockPos idPos : spatialIndex.keySet()) {
            if (searchKey.strictEquals(idPos)) {
                return true;
            }
        }
        
        return false;
    }
}
