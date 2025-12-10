package org.virgil.akiasync.mixin.poi;

import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * POI空间索引管理器
 * 
 * 管理所有世界的POI空间索引
 * 
 * @author AkiAsync
 */
public class PoiSpatialIndexManager {
    
    private static final Map<ServerLevel, PoiSpatialIndex> indexes = new ConcurrentHashMap<>();
    
    private static volatile boolean globalEnabled = true;
    
    /**
     * 获取世界的POI空间索引
     */
    public static PoiSpatialIndex getIndex(ServerLevel level) {
        if (level == null || !globalEnabled) {
            return null;
        }
        
        return indexes.computeIfAbsent(level, PoiSpatialIndex::new);
    }
    
    /**
     * 移除世界的POI空间索引
     */
    public static void removeIndex(ServerLevel level) {
        if (level == null) return;
        
        PoiSpatialIndex index = indexes.remove(level);
        if (index != null) {
            index.clear();
        }
    }
    
    /**
     * 清空所有索引
     */
    public static void clearAll() {
        for (PoiSpatialIndex index : indexes.values()) {
            index.clear();
        }
        indexes.clear();
    }
    
    /**
     * 全局启用/禁用POI空间索引
     */
    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
    }
    
    /**
     * 检查是否全局启用
     */
    public static boolean isGlobalEnabled() {
        return globalEnabled;
    }
    
    /**
     * 获取索引数量
     */
    public static int getIndexCount() {
        return indexes.size();
    }
    
    /**
     * 获取总POI数量
     */
    public static long getTotalPoiCount() {
        return indexes.values().stream()
            .mapToLong(PoiSpatialIndex::getTotalPois)
            .sum();
    }
    
    /**
     * 获取统计信息
     */
    public static String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== POI Spatial Index Statistics ===\n");
        sb.append(String.format("Global Enabled: %s\n", globalEnabled));
        sb.append(String.format("Total Indexes: %d\n", indexes.size()));
        sb.append(String.format("Total POIs: %d\n", getTotalPoiCount()));
        sb.append("\n=== Per-Level Statistics ===\n");
        
        for (PoiSpatialIndex index : indexes.values()) {
            sb.append(String.format("Level: %s | %s\n",
                index.getLevel().dimension().location(),
                index.getStatistics()));
        }
        
        return sb.toString();
    }
}
