package org.virgil.akiasync.mixin.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntitySliceGrid {
    
    private final Int2ObjectOpenHashMap<Set<Entity>> entitySlices = new Int2ObjectOpenHashMap<>();
    
    public static int calculateIntXYZ(int x, int y, int z) {
        return (y << 16) | (z << 8) | x;
    }
    
    public static int calculateIntXYZ(Entity entity) {
        
        int x = ((int) Math.round(entity.getX())) & 15;
        int y = ((int) Math.round(entity.getY())) & 15;
        int z = ((int) Math.round(entity.getZ())) & 15;
        return calculateIntXYZ(x, y, z);
    }
    
    public void addEntity(Entity entity) {
        if (entity == null || entity.isRemoved()) {
            return;
        }
        
        int intXYZ = calculateIntXYZ(entity);
        Set<Entity> entities = entitySlices.computeIfAbsent(intXYZ, k -> new HashSet<>());
        entities.add(entity);
    }
    
    public void removeEntity(Entity entity, int intXYZ) {
        if (entity == null) {
            return;
        }
        
        Set<Entity> entities = entitySlices.get(intXYZ);
        if (entities != null) {
            entities.remove(entity);
            
            if (entities.isEmpty()) {
                entitySlices.remove(intXYZ);
            }
        }
    }
    
    public int updateEntitySlice(Entity entity, int oldIntXYZ) {
        if (entity == null || entity.isRemoved()) {
            
            removeEntity(entity, oldIntXYZ);
            return -1;
        }
        
        int newIntXYZ = calculateIntXYZ(entity);
        
        if (oldIntXYZ != newIntXYZ) {
            
            removeEntity(entity, oldIntXYZ);
            
            addEntity(entity);
        }
        
        return newIntXYZ;
    }
    
    public List<Entity> queryRange(AABB aabb) {
        List<Entity> result = new ArrayList<>();
        
        int chunkMinX = ((int) Math.floor(aabb.minX)) >> 4;
        int chunkMaxX = ((int) Math.floor(aabb.maxX)) >> 4;
        int chunkMinZ = ((int) Math.floor(aabb.minZ)) >> 4;
        int chunkMaxZ = ((int) Math.floor(aabb.maxZ)) >> 4;
        
        boolean crossesChunks = (chunkMinX != chunkMaxX) || (chunkMinZ != chunkMaxZ);
        
        if (crossesChunks) {
            
            return queryRangeCrossChunk(aabb);
        }
        
        int minX = ((int) Math.floor(aabb.minX)) & 15;
        int minY = ((int) Math.floor(aabb.minY)) & 15;
        int minZ = ((int) Math.floor(aabb.minZ)) & 15;
        int maxX = ((int) Math.floor(aabb.maxX)) & 15;
        int maxY = ((int) Math.floor(aabb.maxY)) & 15;
        int maxZ = ((int) Math.floor(aabb.maxZ)) & 15;
        
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int intXYZ = calculateIntXYZ(x, y, z);
                    Set<Entity> entities = entitySlices.get(intXYZ);
                    
                    if (entities != null && !entities.isEmpty()) {
                        result.addAll(entities);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 跨区块范围查询（优化版）
     * 
     * 策略：遍历所有slice，只检查AABB相交的实体
     * 虽然比单区块查询慢，但比完全放弃优化要好
     */
    private List<Entity> queryRangeCrossChunk(AABB aabb) {
        List<Entity> result = new ArrayList<>();
        Set<Entity> deduplicatedEntities = new HashSet<>();
        
        int minX = (int) Math.floor(aabb.minX);
        int minY = (int) Math.floor(aabb.minY);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxX = (int) Math.floor(aabb.maxX);
        int maxY = (int) Math.floor(aabb.maxY);
        int maxZ = (int) Math.floor(aabb.maxZ);
        
        int rangeX = maxX - minX;
        int rangeY = maxY - minY;
        int rangeZ = maxZ - minZ;
        
        if (rangeX > 32 || rangeY > 32 || rangeZ > 32) {
            return result; 
        }
        
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int localX = x & 15;
                    int localY = y & 15;
                    int localZ = z & 15;
                    int intXYZ = calculateIntXYZ(localX, localY, localZ);
                    
                    Set<Entity> entities = entitySlices.get(intXYZ);
                    if (entities != null && !entities.isEmpty()) {
                        
                        for (Entity entity : entities) {
                            if (entity != null && !entity.isRemoved() && 
                                deduplicatedEntities.add(entity)) {
                                result.add(entity);
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    public Set<Entity> getEntities(int intXYZ) {
        return entitySlices.get(intXYZ);
    }
    
    public void clear() {
        entitySlices.clear();
    }
    
    public int size() {
        return entitySlices.size();
    }
    
    public int getTotalEntityCount() {
        int count = 0;
        for (Set<Entity> entities : entitySlices.values()) {
            count += entities.size();
        }
        return count;
    }
    
    public boolean isEmpty() {
        return entitySlices.isEmpty();
    }
    
    public String getStats() {
        return String.format("Slices: %d, Total Entities: %d", 
            size(), getTotalEntityCount());
    }
}
