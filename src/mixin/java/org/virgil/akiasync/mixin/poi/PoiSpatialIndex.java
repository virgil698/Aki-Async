package org.virgil.akiasync.mixin.poi;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PoiSpatialIndex {
    
    private final ServerLevel level;
    
    private final Map<Long, List<PoiRecord>> chunkPoiMap = new ConcurrentHashMap<>();
    
    private final Map<PoiType, Map<Long, List<PoiRecord>>> typeIndex = new ConcurrentHashMap<>();
    
    private final AtomicLong totalPois = new AtomicLong(0);
    private final AtomicLong queryCount = new AtomicLong(0);
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong typeQueryCount = new AtomicLong(0);
    
    public PoiSpatialIndex(ServerLevel level) {
        this.level = level;
    }
    
    public void addPoi(PoiRecord poi) {
        if (poi == null) return;
        
        BlockPos pos = poi.getPos();
        long chunkKey = getChunkKey(pos);
        
        chunkPoiMap.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(poi);
        
        net.minecraft.core.Holder<PoiType> typeHolder = poi.getPoiType();
        if (typeHolder != null && typeHolder.isBound()) {
            PoiType type = typeHolder.value();
            typeIndex.computeIfAbsent(type, t -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> new ArrayList<>())
                .add(poi);
        }
        
        totalPois.incrementAndGet();
    }
    
    public void addPoiSimple(BlockPos pos, Holder<PoiType> typeHolder) {
        if (pos == null || typeHolder == null || !typeHolder.isBound()) return;
        
    }
    
    public void removePoi(BlockPos pos) {
        if (pos == null) return;
        
        long chunkKey = getChunkKey(pos);
        
        List<PoiRecord> pois = chunkPoiMap.get(chunkKey);
        if (pois != null) {
            PoiRecord removed = null;
            for (PoiRecord poi : pois) {
                if (poi.getPos().equals(pos)) {
                    removed = poi;
                    break;
                }
            }
            
            if (removed != null) {
                pois.remove(removed);
                totalPois.decrementAndGet();
                
                if (pois.isEmpty()) {
                    chunkPoiMap.remove(chunkKey);
                }
                
                net.minecraft.core.Holder<PoiType> typeHolder = removed.getPoiType();
                if (typeHolder != null && typeHolder.isBound()) {
                    PoiType type = typeHolder.value();
                    Map<Long, List<PoiRecord>> typeMap = typeIndex.get(type);
                    if (typeMap != null) {
                        List<PoiRecord> typePois = typeMap.get(chunkKey);
                        if (typePois != null) {
                            typePois.remove(removed);
                            if (typePois.isEmpty()) {
                                typeMap.remove(chunkKey);
                            }
                        }
                    }
                }
            }
        }
    }
    
    public List<PoiRecord> queryRange(BlockPos center, int radius) {
        queryCount.incrementAndGet();
        
        if (radius <= 0) return Collections.emptyList();
        
        List<Long> chunkKeys = getChunkKeysInRange(center, radius);
        
        if (chunkKeys.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<PoiRecord> result = new ArrayList<>();
        double radiusSq = radius * radius;
        
        for (Long key : chunkKeys) {
            List<PoiRecord> pois = chunkPoiMap.get(key);
            if (pois != null) {
                for (PoiRecord poi : pois) {
                    
                    if (poi.getPos().distSqr(center) <= radiusSq) {
                        result.add(poi);
                    }
                }
            }
        }
        
        if (!result.isEmpty()) {
            cacheHitCount.incrementAndGet();
        }
        
        return result;
    }
    
    public List<PoiRecord> queryByType(BlockPos center, PoiType type, int radius) {
        typeQueryCount.incrementAndGet();
        queryCount.incrementAndGet();
        
        if (radius <= 0 || type == null) return Collections.emptyList();
        
        Map<Long, List<PoiRecord>> typeMap = typeIndex.get(type);
        if (typeMap == null) {
            return Collections.emptyList();
        }
        
        List<Long> chunkKeys = getChunkKeysInRange(center, radius);
        
        if (chunkKeys.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<PoiRecord> result = new ArrayList<>();
        double radiusSq = radius * radius;
        
        for (Long key : chunkKeys) {
            List<PoiRecord> pois = typeMap.get(key);
            if (pois != null) {
                for (PoiRecord poi : pois) {
                    if (poi.getPos().distSqr(center) <= radiusSq) {
                        result.add(poi);
                    }
                }
            }
        }
        
        if (!result.isEmpty()) {
            cacheHitCount.incrementAndGet();
        }
        
        return result;
    }
    
    public void clear() {
        chunkPoiMap.clear();
        typeIndex.clear();
        totalPois.set(0);
        queryCount.set(0);
        cacheHitCount.set(0);
        typeQueryCount.set(0);
    }
    
    public void cleanupChunk(long chunkKey) {
        List<PoiRecord> removed = chunkPoiMap.remove(chunkKey);
        if (removed != null) {
            totalPois.addAndGet(-removed.size());
            
            for (Map<Long, List<PoiRecord>> typeMap : typeIndex.values()) {
                List<PoiRecord> typeRemoved = typeMap.remove(chunkKey);
                if (typeRemoved != null) {
                }
            }
        }
    }
    
    public void cleanupChunk(int chunkX, int chunkZ) {
        long chunkKey = net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ);
        cleanupChunk(chunkKey);
    }
    
    public boolean isEmpty() {
        return chunkPoiMap.isEmpty();
    }
    
    public String getStatistics() {
        long queries = queryCount.get();
        double hitRate = queries > 0 ? (cacheHitCount.get() * 100.0 / queries) : 0.0;
        return String.format(
            "POIs: %d | Queries: %d | Type Queries: %d | Hit Rate: %.2f%%",
            totalPois.get(), queries, typeQueryCount.get(), hitRate
        );
    }
    
    private long getChunkKey(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return ChunkPos.asLong(chunkX, chunkZ);
    }
    
    private List<Long> getChunkKeysInRange(BlockPos center, int radius) {
        List<Long> keys = new ArrayList<>();
        
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                keys.add(ChunkPos.asLong(x, z));
            }
        }
        
        return keys;
    }
    
    public long getTotalPois() {
        return totalPois.get();
    }
    
    public long getQueryCount() {
        return queryCount.get();
    }
    
    public long getTypeQueryCount() {
        return typeQueryCount.get();
    }
    
    public long getCacheHitCount() {
        return cacheHitCount.get();
    }
    
    public ServerLevel getLevel() {
        return level;
    }
}
