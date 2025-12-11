package org.virgil.akiasync.mixin.brain.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class AiSpatialIndex {
    
    private final ServerLevel level;
    private final AiSliceGrid grid;
    
    private volatile boolean enabled = true;
    
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong totalHits = new AtomicLong(0);
    
    public AiSpatialIndex(ServerLevel level) {
        this.level = level;
        this.grid = new AiSliceGrid();
    }
    
    public void addEntity(LivingEntity entity) {
        if (!enabled || entity == null) return;
        grid.addEntity(entity);
    }
    
    public void removeEntity(LivingEntity entity) {
        if (!enabled || entity == null) return;
        grid.removeEntity(entity);
    }
    
    public void updateEntity(LivingEntity entity, BlockPos oldPos, BlockPos newPos) {
        if (!enabled || entity == null) return;
        grid.updateEntity(entity, oldPos, newPos);
    }
    
    public void addPoi(PoiRecord poi) {
        if (!enabled || poi == null) return;
        grid.addPoi(poi);
    }
    
    public void removePoi(BlockPos pos) {
        if (!enabled || pos == null) return;
        grid.removePoi(pos);
    }
    
    public List<LivingEntity> queryEntities(BlockPos center, int radius) {
        totalQueries.incrementAndGet();
        
        if (!enabled) {
            return List.of();
        }
        
        List<LivingEntity> result = grid.queryEntities(center, radius);
        
        if (!result.isEmpty()) {
            totalHits.incrementAndGet();
        }
        
        return result;
    }
    
    public List<Player> queryPlayers(BlockPos center, int radius) {
        totalQueries.incrementAndGet();
        
        if (!enabled) {
            return List.of();
        }
        
        List<Player> result = grid.queryPlayers(center, radius);
        
        if (!result.isEmpty()) {
            totalHits.incrementAndGet();
        }
        
        return result;
    }
    
    public List<PoiRecord> queryPoi(BlockPos center, int radius) {
        totalQueries.incrementAndGet();
        
        if (!enabled) {
            return List.of();
        }
        
        List<PoiRecord> result = grid.queryPoi(center, radius);
        
        if (!result.isEmpty()) {
            totalHits.incrementAndGet();
        }
        
        return result;
    }
    
    public void clear() {
        grid.clear();
        totalQueries.set(0);
        totalHits.set(0);
    }
    
    public boolean isEmpty() {
        return grid.isEmpty();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public ServerLevel getLevel() {
        return level;
    }
    
    public String getStatistics() {
        long queries = totalQueries.get();
        long hits = totalHits.get();
        double hitRate = queries > 0 ? (hits * 100.0 / queries) : 0.0;
        return String.format(
            "Level: %s | %s | Total Queries: %d | Hit Rate: %.2f%%",
            level.dimension().location(),
            grid.getStatistics(),
            queries,
            hitRate
        );
    }
    
    public long getEntityCount() {
        return grid.getEntityCount();
    }
    
    public long getPlayerCount() {
        return grid.getPlayerCount();
    }
    
    public long getPoiCount() {
        return grid.getPoiCount();
    }
}
