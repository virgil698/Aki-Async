package org.virgil.akiasync.mixin.poi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import com.google.common.collect.ImmutableMap;

public class BatchPoiManager {
    
    private static final Map<ServerLevel, PoiCache> levelCaches = new ConcurrentHashMap<>();
    
    private static class PoiRequest {
        final BlockPos center;
        final int radius;
        final long requestTime;
        
        PoiRequest(BlockPos center, int radius) {
            this.center = center;
            this.radius = radius;
            this.requestTime = System.currentTimeMillis();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PoiRequest)) return false;
            PoiRequest that = (PoiRequest) o;
            return radius == that.radius && center.equals(that.center);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(center, radius);
        }
    }
    
    private static class PoiCache {
        private final Map<PoiRequest, Map<BlockPos, PoiRecord>> cache = new ConcurrentHashMap<>();
        private long lastClearTime = System.currentTimeMillis();
        
        Map<BlockPos, PoiRecord> get(PoiRequest request) {
            return cache.get(request);
        }
        
        void put(PoiRequest request, Map<BlockPos, PoiRecord> result) {
            cache.put(request, result);
        }
        
        void clearIfNeeded() {
            long now = System.currentTimeMillis();

            if (now - lastClearTime > 50) {
                cache.clear();
                lastClearTime = now;
            }
        }
    }
    
    public static Map<BlockPos, PoiRecord> getPoiInRange(
            ServerLevel level,
            BlockPos center,
            int radius) {
        
        if (level == null || center == null) {
            return Collections.emptyMap();
        }
        
        PoiCache cache = levelCaches.computeIfAbsent(level, k -> new PoiCache());
        cache.clearIfNeeded();
        
        PoiRequest request = new PoiRequest(center, radius);
        
        Map<BlockPos, PoiRecord> cached = cache.get(request);
        if (cached != null) {
            return cached;
        }
        
        try {
            PoiManager poiManager = level.getPoiManager();
            Map<BlockPos, PoiRecord> result = poiManager.getInRange(
                type -> true,
                center,
                radius,
                PoiManager.Occupancy.ANY
            ).collect(ImmutableMap.toImmutableMap(
                PoiRecord::getPos,
                record -> record
            ));
            
            cache.put(request, result);
            return result;
            
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
    
    public static void clearCache(ServerLevel level) {
        if (level != null) {
            levelCaches.remove(level);
        }
    }
    
    public static void clearLevelCache(ServerLevel level) {
        clearCache(level);
    }
    
    public static void clearAllCaches() {
        levelCaches.clear();
    }
    
    public static String getStatistics() {
        int totalCaches = levelCaches.size();
        int totalEntries = levelCaches.values().stream()
            .mapToInt(cache -> cache.cache.size())
            .sum();
        
        return String.format("POI Cache: Levels=%d | Entries=%d", totalCaches, totalEntries);
    }
}
