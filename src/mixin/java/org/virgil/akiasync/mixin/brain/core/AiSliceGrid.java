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
import java.util.concurrent.atomic.AtomicLong;

public class AiSliceGrid {
    
    private static final int GRID_SIZE = 16;
    
    private final ConcurrentHashMap<Long, List<LivingEntity>> entityGrid = new ConcurrentHashMap<>();
    
    private final ConcurrentHashMap<Long, List<Player>> playerGrid = new ConcurrentHashMap<>();
    
    private final ConcurrentHashMap<Long, List<PoiRecord>> poiGrid = new ConcurrentHashMap<>();
    
    private final AtomicLong entityCount = new AtomicLong(0);
    private final AtomicLong playerCount = new AtomicLong(0);
    private final AtomicLong poiCount = new AtomicLong(0);
    private final AtomicLong queryCount = new AtomicLong(0);
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    
    public void addEntity(LivingEntity entity) {
        if (entity == null || entity.isRemoved()) return;
        
        long gridKey = getGridKey(entity.blockPosition());
        
        entityGrid.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(entity);
        entityCount.incrementAndGet();
        
        if (entity instanceof Player player) {
            playerGrid.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(player);
            playerCount.incrementAndGet();
        }
    }
    
    public void removeEntity(LivingEntity entity) {
        if (entity == null) return;
        
        long gridKey = getGridKey(entity.blockPosition());
        
        List<LivingEntity> entities = entityGrid.get(gridKey);
        if (entities != null) {
            if (entities.remove(entity)) {
                entityCount.decrementAndGet();
                if (entities.isEmpty()) {
                    entityGrid.remove(gridKey);
                }
            }
        }
        
        if (entity instanceof Player player) {
            List<Player> players = playerGrid.get(gridKey);
            if (players != null) {
                if (players.remove(player)) {
                    playerCount.decrementAndGet();
                    if (players.isEmpty()) {
                        playerGrid.remove(gridKey);
                    }
                }
            }
        }
    }
    
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
    
    public void addPoi(PoiRecord poi) {
        if (poi == null) return;
        
        long gridKey = getGridKey(poi.getPos());
        poiGrid.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(poi);
        poiCount.incrementAndGet();
    }
    
    public void removePoi(BlockPos pos) {
        if (pos == null) return;
        
        long gridKey = getGridKey(pos);
        List<PoiRecord> pois = poiGrid.get(gridKey);
        if (pois != null) {
            pois.removeIf(poi -> poi.getPos().equals(pos));
            poiCount.decrementAndGet();
            if (pois.isEmpty()) {
                poiGrid.remove(gridKey);
            }
        }
    }
    
    public List<LivingEntity> queryEntities(BlockPos center, int radius) {
        queryCount.incrementAndGet();
        
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
            cacheHitCount.incrementAndGet();
        }
        
        return result;
    }
    
    public List<Player> queryPlayers(BlockPos center, int radius) {
        queryCount.incrementAndGet();
        
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
            cacheHitCount.incrementAndGet();
        }
        
        return result;
    }
    
    public List<PoiRecord> queryPoi(BlockPos center, int radius) {
        queryCount.incrementAndGet();
        
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
            cacheHitCount.incrementAndGet();
        }
        
        return result;
    }
    
    public void clear() {
        entityGrid.clear();
        playerGrid.clear();
        poiGrid.clear();
        entityCount.set(0);
        playerCount.set(0);
        poiCount.set(0);
    }
    
    public boolean isEmpty() {
        return entityGrid.isEmpty() && playerGrid.isEmpty() && poiGrid.isEmpty();
    }
    
    public String getStatistics() {
        long queries = queryCount.get();
        long hits = cacheHitCount.get();
        double hitRate = queries > 0 ? (hits * 100.0 / queries) : 0.0;
        return String.format(
            "Entities: %d | Players: %d | POIs: %d | Queries: %d | Hit Rate: %.2f%%",
            entityCount.get(), playerCount.get(), poiCount.get(), queries, hitRate
        );
    }
    
    private long getGridKey(BlockPos pos) {
        int gridX = Math.floorDiv(pos.getX(), GRID_SIZE);
        int gridZ = Math.floorDiv(pos.getZ(), GRID_SIZE);
        return ChunkPos.asLong(gridX, gridZ);
    }
    
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
    
    public long getEntityCount() {
        return entityCount.get();
    }
    
    public long getPlayerCount() {
        return playerCount.get();
    }
    
    public long getPoiCount() {
        return poiCount.get();
    }
    
    public long getQueryCount() {
        return queryCount.get();
    }
    
    public long getCacheHitCount() {
        return cacheHitCount.get();
    }
}
