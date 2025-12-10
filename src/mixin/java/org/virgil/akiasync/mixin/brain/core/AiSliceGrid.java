package org.virgil.akiasync.mixin.brain.core;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI专用空间分区网格
 * 参考EntitySliceGrid的设计，为AI查询优化
 * 
 * 使用16x16区块级别的网格存储实体、POI和玩家
 * 提供O(1)的范围查询性能
 * 
 * @author AkiAsync
 */
public class AiSliceGrid {
    
    private static final int GRID_SIZE = 16;
    
    private final ConcurrentHashMap<Long, List<LivingEntity>> entityGrid = new ConcurrentHashMap<>();
    
    private final ConcurrentHashMap<Long, List<Player>> playerGrid = new ConcurrentHashMap<>();
    
    private final ConcurrentHashMap<Long, List<PoiRecord>> poiGrid = new ConcurrentHashMap<>();
    
    private volatile long entityCount = 0;
    private volatile long playerCount = 0;
    private volatile long poiCount = 0;
    private volatile long queryCount = 0;
    private volatile long cacheHitCount = 0;
    
    /**
     * 添加实体到网格
     */
    public void addEntity(LivingEntity entity) {
        if (entity == null || entity.isRemoved()) return;
        
        long gridKey = getGridKey(entity.blockPosition());
        
        entityGrid.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(entity);
        entityCount++;
        
        if (entity instanceof Player player) {
            playerGrid.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(player);
            playerCount++;
        }
    }
    
    /**
     * 从网格移除实体
     */
    public void removeEntity(LivingEntity entity) {
        if (entity == null) return;
        
        long gridKey = getGridKey(entity.blockPosition());
        
        List<LivingEntity> entities = entityGrid.get(gridKey);
        if (entities != null) {
            if (entities.remove(entity)) {
                entityCount--;
                if (entities.isEmpty()) {
                    entityGrid.remove(gridKey);
                }
            }
        }
        
        if (entity instanceof Player player) {
            List<Player> players = playerGrid.get(gridKey);
            if (players != null) {
                if (players.remove(player)) {
                    playerCount--;
                    if (players.isEmpty()) {
                        playerGrid.remove(gridKey);
                    }
                }
            }
        }
    }
    
    /**
     * 更新实体位置（从旧网格移到新网格）
     */
    public void updateEntity(LivingEntity entity, BlockPos oldPos, BlockPos newPos) {
        long oldKey = getGridKey(oldPos);
        long newKey = getGridKey(newPos);
        
        if (oldKey == newKey) {
            return; 
        }
        
        List<LivingEntity> oldEntities = entityGrid.get(oldKey);
        if (oldEntities != null) {
            oldEntities.remove(entity);
            if (oldEntities.isEmpty()) {
                entityGrid.remove(oldKey);
            }
        }
        
        entityGrid.computeIfAbsent(newKey, k -> new ArrayList<>()).add(entity);
        
        if (entity instanceof Player player) {
            List<Player> oldPlayers = playerGrid.get(oldKey);
            if (oldPlayers != null) {
                oldPlayers.remove(player);
                if (oldPlayers.isEmpty()) {
                    playerGrid.remove(oldKey);
                }
            }
            playerGrid.computeIfAbsent(newKey, k -> new ArrayList<>()).add(player);
        }
    }
    
    /**
     * 添加POI到网格
     */
    public void addPoi(PoiRecord poi) {
        if (poi == null) return;
        
        long gridKey = getGridKey(poi.getPos());
        poiGrid.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(poi);
        poiCount++;
    }
    
    /**
     * 从网格移除POI
     */
    public void removePoi(BlockPos pos) {
        if (pos == null) return;
        
        long gridKey = getGridKey(pos);
        List<PoiRecord> pois = poiGrid.get(gridKey);
        if (pois != null) {
            pois.removeIf(poi -> poi.getPos().equals(pos));
            poiCount--;
            if (pois.isEmpty()) {
                poiGrid.remove(gridKey);
            }
        }
    }
    
    /**
     * 查询范围内的实体 - O(1)
     */
    public List<LivingEntity> queryEntities(BlockPos center, int radius) {
        queryCount++;
        
        if (radius <= 0) return Collections.emptyList();
        
        List<Long> gridKeys = getGridKeysInRange(center, radius);
        
        if (gridKeys.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<LivingEntity> result = new ArrayList<>();
        double radiusSq = radius * radius;
        
        for (Long key : gridKeys) {
            List<LivingEntity> entities = entityGrid.get(key);
            if (entities != null) {
                for (LivingEntity entity : entities) {
                    if (entity.isRemoved()) continue;
                    
                    if (entity.blockPosition().distSqr(center) <= radiusSq) {
                        result.add(entity);
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
     * 查询范围内的玩家 - O(1)
     */
    public List<Player> queryPlayers(BlockPos center, int radius) {
        queryCount++;
        
        if (radius <= 0) return Collections.emptyList();
        
        List<Long> gridKeys = getGridKeysInRange(center, radius);
        
        if (gridKeys.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Player> result = new ArrayList<>();
        double radiusSq = radius * radius;
        
        for (Long key : gridKeys) {
            List<Player> players = playerGrid.get(key);
            if (players != null) {
                for (Player player : players) {
                    if (player.isRemoved()) continue;
                    
                    if (player.blockPosition().distSqr(center) <= radiusSq) {
                        result.add(player);
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
     * 查询范围内的POI - O(1)
     */
    public List<PoiRecord> queryPoi(BlockPos center, int radius) {
        queryCount++;
        
        if (radius <= 0) return Collections.emptyList();
        
        List<Long> gridKeys = getGridKeysInRange(center, radius);
        
        if (gridKeys.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<PoiRecord> result = new ArrayList<>();
        double radiusSq = radius * radius;
        
        for (Long key : gridKeys) {
            List<PoiRecord> pois = poiGrid.get(key);
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
     * 清空网格
     */
    public void clear() {
        entityGrid.clear();
        playerGrid.clear();
        poiGrid.clear();
        entityCount = 0;
        playerCount = 0;
        poiCount = 0;
    }
    
    /**
     * 检查网格是否为空
     */
    public boolean isEmpty() {
        return entityGrid.isEmpty() && playerGrid.isEmpty() && poiGrid.isEmpty();
    }
    
    /**
     * 获取统计信息
     */
    public String getStatistics() {
        double hitRate = queryCount > 0 ? (cacheHitCount * 100.0 / queryCount) : 0.0;
        return String.format(
            "Entities: %d | Players: %d | POIs: %d | Queries: %d | Hit Rate: %.2f%%",
            entityCount, playerCount, poiCount, queryCount, hitRate
        );
    }
    
    /**
     * 计算位置的网格key
     */
    private long getGridKey(BlockPos pos) {
        int gridX = Math.floorDiv(pos.getX(), GRID_SIZE);
        int gridZ = Math.floorDiv(pos.getZ(), GRID_SIZE);
        return ChunkPos.asLong(gridX, gridZ);
    }
    
    /**
     * 获取范围内的所有网格key
     */
    private List<Long> getGridKeysInRange(BlockPos center, int radius) {
        List<Long> keys = new ArrayList<>();
        
        int minX = Math.floorDiv(center.getX() - radius, GRID_SIZE);
        int maxX = Math.floorDiv(center.getX() + radius, GRID_SIZE);
        int minZ = Math.floorDiv(center.getZ() - radius, GRID_SIZE);
        int maxZ = Math.floorDiv(center.getZ() + radius, GRID_SIZE);
        
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                keys.add(ChunkPos.asLong(x, z));
            }
        }
        
        return keys;
    }
    
    /**
     * 获取实体数量
     */
    public long getEntityCount() {
        return entityCount;
    }
    
    /**
     * 获取玩家数量
     */
    public long getPlayerCount() {
        return playerCount;
    }
    
    /**
     * 获取POI数量
     */
    public long getPoiCount() {
        return poiCount;
    }
    
    /**
     * 获取查询次数
     */
    public long getQueryCount() {
        return queryCount;
    }
    
    /**
     * 获取缓存命中次数
     */
    public long getCacheHitCount() {
        return cacheHitCount;
    }
}
