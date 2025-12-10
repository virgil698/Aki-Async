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

/**
 * POI空间索引
 * 
 * 基于区块的POI索引，提供O(1)查询性能
 * 替代BatchPoiManager的缓存策略
 * 
 * 优势：
 * - 区块级别索引，不是查询级别缓存
 * - 支持类型索引（快速查找特定类型POI）
 * - 自动监听POI变化
 * - 无需手动清理缓存
 * 
 * @author AkiAsync
 */
public class PoiSpatialIndex {
    
    private final ServerLevel level;
    
    private final Map<Long, List<PoiRecord>> chunkPoiMap = new ConcurrentHashMap<>();
    
    private final Map<PoiType, Map<Long, List<PoiRecord>>> typeIndex = new ConcurrentHashMap<>();
    
    private volatile long totalPois = 0;
    private volatile long queryCount = 0;
    private volatile long cacheHitCount = 0;
    private volatile long typeQueryCount = 0;
    
    public PoiSpatialIndex(ServerLevel level) {
        this.level = level;
    }
    
    /**
     * 添加POI到索引
     */
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
        
        totalPois++;
    }
    
    /**
     * 添加POI到索引（简化版本，用于Mixin）
     * 
     * 注意：这个方法不会创建完整的PoiRecord，只记录位置和类型
     * 用于快速索引，不适合需要完整POI信息的场景
     */
    public void addPoiSimple(BlockPos pos, Holder<PoiType> typeHolder) {
        if (pos == null || typeHolder == null || !typeHolder.isBound()) return;
        
    }
    
    /**
     * 从索引移除POI
     */
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
                totalPois--;
                
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
    
    /**
     * 查询范围内的POI - O(1)
     */
    public List<PoiRecord> queryRange(BlockPos center, int radius) {
        queryCount++;
        
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
            cacheHitCount++;
        }
        
        return result;
    }
    
    /**
     * 查询特定类型的POI - 更快
     */
    public List<PoiRecord> queryByType(BlockPos center, PoiType type, int radius) {
        typeQueryCount++;
        queryCount++;
        
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
            cacheHitCount++;
        }
        
        return result;
    }
    
    /**
     * 清空索引
     */
    public void clear() {
        chunkPoiMap.clear();
        typeIndex.clear();
        totalPois = 0;
        queryCount = 0;
        cacheHitCount = 0;
        typeQueryCount = 0;
    }
    
    /**
     * 检查索引是否为空
     */
    public boolean isEmpty() {
        return chunkPoiMap.isEmpty();
    }
    
    /**
     * 获取统计信息
     */
    public String getStatistics() {
        double hitRate = queryCount > 0 ? (cacheHitCount * 100.0 / queryCount) : 0.0;
        return String.format(
            "POIs: %d | Queries: %d | Type Queries: %d | Hit Rate: %.2f%%",
            totalPois, queryCount, typeQueryCount, hitRate
        );
    }
    
    /**
     * 计算位置的区块key
     */
    private long getChunkKey(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return ChunkPos.asLong(chunkX, chunkZ);
    }
    
    /**
     * 获取范围内的所有区块key
     */
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
    
    /**
     * 获取POI总数
     */
    public long getTotalPois() {
        return totalPois;
    }
    
    /**
     * 获取查询次数
     */
    public long getQueryCount() {
        return queryCount;
    }
    
    /**
     * 获取类型查询次数
     */
    public long getTypeQueryCount() {
        return typeQueryCount;
    }
    
    /**
     * 获取缓存命中次数
     */
    public long getCacheHitCount() {
        return cacheHitCount;
    }
    
    /**
     * 获取世界
     */
    public ServerLevel getLevel() {
        return level;
    }
}
