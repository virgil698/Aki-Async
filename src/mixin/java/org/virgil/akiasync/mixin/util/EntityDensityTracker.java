package org.virgil.akiasync.mixin.util;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.world.level.ChunkPos;

/**
 * 实体密度追踪器
 * 
 * 根据区域实体密度动态调整优化策略：
 * - 低密度区域（<10实体）：使用vanilla逻辑（开销小）
 * - 中密度区域（10-50实体）：使用采样+缓存
 * - 高密度区域（>50实体）：使用空间索引+激进优化
 * 
 * @author AkiAsync
 */
public class EntityDensityTracker {
    
    public enum DensityLevel {
        LOW,      
        MEDIUM,   
        HIGH,     
        EXTREME   
    }
    
    private static final Long2IntOpenHashMap chunkDensity = new Long2IntOpenHashMap();
    
    private static volatile long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 1000; 
    
    private static final int LOW_DENSITY_THRESHOLD = 100;      
    private static final int MEDIUM_DENSITY_THRESHOLD = 512;   
    private static final int HIGH_DENSITY_THRESHOLD = 2048;    
    private static final int EXTREME_DENSITY_THRESHOLD = 4096; 
    
    /**
     * 更新区块密度
     * 
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @param entityCount 实体数量
     */
    public static void updateChunkDensity(int chunkX, int chunkZ, int entityCount) {
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        chunkDensity.put(chunkKey, entityCount);
    }
    
    /**
     * 获取区块密度等级
     * 
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @return 密度等级
     */
    public static DensityLevel getDensityLevel(int chunkX, int chunkZ) {
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        int entityCount = chunkDensity.getOrDefault(chunkKey, 0);
        
        if (entityCount < LOW_DENSITY_THRESHOLD) {
            return DensityLevel.LOW;
        } else if (entityCount < MEDIUM_DENSITY_THRESHOLD) {
            return DensityLevel.MEDIUM;
        } else if (entityCount < HIGH_DENSITY_THRESHOLD) {
            return DensityLevel.HIGH;
        } else {
            return DensityLevel.EXTREME;
        }
    }
    
    /**
     * 获取区域密度等级（考虑周围区块）
     * 
     * @param x 世界X坐标
     * @param z 世界Z坐标
     * @return 密度等级
     */
    public static DensityLevel getRegionDensityLevel(double x, double z) {
        int chunkX = ((int) Math.floor(x)) >> 4;
        int chunkZ = ((int) Math.floor(z)) >> 4;
        
        int totalEntities = 0;
        int checkedChunks = 0;
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long chunkKey = ChunkPos.asLong(chunkX + dx, chunkZ + dz);
                if (chunkDensity.containsKey(chunkKey)) {
                    totalEntities += chunkDensity.get(chunkKey);
                    checkedChunks++;
                }
            }
        }
        
        if (checkedChunks == 0) {
            return DensityLevel.LOW; 
        }
        
        int avgDensity = totalEntities / checkedChunks;
        
        if (avgDensity < LOW_DENSITY_THRESHOLD) {
            return DensityLevel.LOW;
        } else if (avgDensity < MEDIUM_DENSITY_THRESHOLD) {
            return DensityLevel.MEDIUM;
        } else if (avgDensity < HIGH_DENSITY_THRESHOLD) {
            return DensityLevel.HIGH;
        } else {
            return DensityLevel.EXTREME;
        }
    }
    
    /**
     * 是否应该使用激进优化
     * 
     * @param x 世界X坐标
     * @param z 世界Z坐标
     * @return true = 使用激进优化
     */
    public static boolean shouldUseAggressiveOptimization(double x, double z) {
        DensityLevel level = getRegionDensityLevel(x, z);
        return level == DensityLevel.HIGH || level == DensityLevel.EXTREME;
    }
    
    /**
     * 是否应该使用空间索引
     * 
     * @param x 世界X坐标
     * @param z 世界Z坐标
     * @return true = 使用空间索引
     */
    public static boolean shouldUseSpatialIndex(double x, double z) {
        DensityLevel level = getRegionDensityLevel(x, z);
        return level == DensityLevel.MEDIUM || level == DensityLevel.HIGH || level == DensityLevel.EXTREME;
    }
    
    /**
     * 是否应该跳过优化（使用vanilla逻辑）
     * 
     * @param x 世界X坐标
     * @param z 世界Z坐标
     * @return true = 跳过优化
     */
    public static boolean shouldSkipOptimization(double x, double z) {
        DensityLevel level = getRegionDensityLevel(x, z);
        return level == DensityLevel.LOW;
    }
    
    /**
     * 清理过期数据
     */
    public static void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastUpdateTime > UPDATE_INTERVAL_MS * 60) {
            
            chunkDensity.clear();
            lastUpdateTime = currentTime;
        }
    }
    
    /**
     * 获取统计信息
     */
    public static String getStats() {
        int lowCount = 0;
        int mediumCount = 0;
        int highCount = 0;
        int extremeCount = 0;
        
        for (int entityCount : chunkDensity.values()) {
            if (entityCount < LOW_DENSITY_THRESHOLD) {
                lowCount++;
            } else if (entityCount < MEDIUM_DENSITY_THRESHOLD) {
                mediumCount++;
            } else if (entityCount < HIGH_DENSITY_THRESHOLD) {
                highCount++;
            } else {
                extremeCount++;
            }
        }
        
        return String.format("Density: Low=%d, Medium=%d, High=%d, Extreme=%d", 
            lowCount, mediumCount, highCount, extremeCount);
    }
    
    /**
     * 清空所有数据
     */
    public static void clear() {
        chunkDensity.clear();
        lastUpdateTime = 0;
    }
}
