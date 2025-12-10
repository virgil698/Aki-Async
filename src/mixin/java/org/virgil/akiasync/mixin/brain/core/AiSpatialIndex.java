package org.virgil.akiasync.mixin.brain.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * AI空间索引 - 单个世界的索引实例
 * 
 * 提供统一的空间查询接口，支持：
 * - 实体查询
 * - 玩家查询
 * - POI查询
 * 
 * 所有查询都是O(1)复杂度
 * 
 * @author AkiAsync
 */
public class AiSpatialIndex {
    
    private final ServerLevel level;
    private final AiSliceGrid grid;
    
    private volatile boolean enabled = true;
    
    private volatile long totalQueries = 0;
    private volatile long totalHits = 0;
    
    public AiSpatialIndex(ServerLevel level) {
        this.level = level;
        this.grid = new AiSliceGrid();
    }
    
    /**
     * 添加实体到索引
     */
    public void addEntity(LivingEntity entity) {
        if (!enabled || entity == null) return;
        grid.addEntity(entity);
    }
    
    /**
     * 从索引移除实体
     */
    public void removeEntity(LivingEntity entity) {
        if (!enabled || entity == null) return;
        grid.removeEntity(entity);
    }
    
    /**
     * 更新实体位置
     */
    public void updateEntity(LivingEntity entity, BlockPos oldPos, BlockPos newPos) {
        if (!enabled || entity == null) return;
        grid.updateEntity(entity, oldPos, newPos);
    }
    
    /**
     * 添加POI到索引
     */
    public void addPoi(PoiRecord poi) {
        if (!enabled || poi == null) return;
        grid.addPoi(poi);
    }
    
    /**
     * 从索引移除POI
     */
    public void removePoi(BlockPos pos) {
        if (!enabled || pos == null) return;
        grid.removePoi(pos);
    }
    
    /**
     * 查询范围内的实体 - O(1)
     */
    public List<LivingEntity> queryEntities(BlockPos center, int radius) {
        totalQueries++;
        
        if (!enabled) {
            return List.of();
        }
        
        List<LivingEntity> result = grid.queryEntities(center, radius);
        
        if (!result.isEmpty()) {
            totalHits++;
        }
        
        return result;
    }
    
    /**
     * 查询范围内的玩家 - O(1)
     */
    public List<Player> queryPlayers(BlockPos center, int radius) {
        totalQueries++;
        
        if (!enabled) {
            return List.of();
        }
        
        List<Player> result = grid.queryPlayers(center, radius);
        
        if (!result.isEmpty()) {
            totalHits++;
        }
        
        return result;
    }
    
    /**
     * 查询范围内的POI - O(1)
     */
    public List<PoiRecord> queryPoi(BlockPos center, int radius) {
        totalQueries++;
        
        if (!enabled) {
            return List.of();
        }
        
        List<PoiRecord> result = grid.queryPoi(center, radius);
        
        if (!result.isEmpty()) {
            totalHits++;
        }
        
        return result;
    }
    
    /**
     * 清空索引
     */
    public void clear() {
        grid.clear();
        totalQueries = 0;
        totalHits = 0;
    }
    
    /**
     * 检查索引是否为空
     */
    public boolean isEmpty() {
        return grid.isEmpty();
    }
    
    /**
     * 启用/禁用索引
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 检查索引是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 获取世界
     */
    public ServerLevel getLevel() {
        return level;
    }
    
    /**
     * 获取统计信息
     */
    public String getStatistics() {
        double hitRate = totalQueries > 0 ? (totalHits * 100.0 / totalQueries) : 0.0;
        return String.format(
            "Level: %s | %s | Total Queries: %d | Hit Rate: %.2f%%",
            level.dimension().location(),
            grid.getStatistics(),
            totalQueries,
            hitRate
        );
    }
    
    /**
     * 获取实体数量
     */
    public long getEntityCount() {
        return grid.getEntityCount();
    }
    
    /**
     * 获取玩家数量
     */
    public long getPlayerCount() {
        return grid.getPlayerCount();
    }
    
    /**
     * 获取POI数量
     */
    public long getPoiCount() {
        return grid.getPoiCount();
    }
}
