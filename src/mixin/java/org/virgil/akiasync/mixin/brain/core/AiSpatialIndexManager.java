package org.virgil.akiasync.mixin.brain.core;

import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI空间索引管理器
 * 
 * 管理所有世界的AI空间索引
 * 提供全局访问接口
 * 
 * @author AkiAsync
 */
public class AiSpatialIndexManager {
    
    private static final Map<ServerLevel, AiSpatialIndex> indexes = new ConcurrentHashMap<>();
    
    private static volatile boolean globalEnabled = true;
    
    private static volatile long totalIndexes = 0;
    private static volatile long totalQueries = 0;
    
    /**
     * 获取世界的空间索引
     * 如果不存在则自动创建
     */
    public static AiSpatialIndex getIndex(ServerLevel level) {
        if (level == null) {
            return null;
        }
        
        return indexes.computeIfAbsent(level, l -> {
            totalIndexes++;
            AiSpatialIndex index = new AiSpatialIndex(l);
            index.setEnabled(globalEnabled);
            return index;
        });
    }
    
    /**
     * 移除世界的空间索引
     */
    public static void removeIndex(ServerLevel level) {
        if (level == null) return;
        
        AiSpatialIndex index = indexes.remove(level);
        if (index != null) {
            index.clear();
            totalIndexes--;
        }
    }
    
    /**
     * 清空所有索引
     */
    public static void clearAll() {
        for (AiSpatialIndex index : indexes.values()) {
            index.clear();
        }
        indexes.clear();
        totalIndexes = 0;
        totalQueries = 0;
    }
    
    /**
     * 全局启用/禁用空间索引
     */
    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
        
        for (AiSpatialIndex index : indexes.values()) {
            index.setEnabled(enabled);
        }
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
     * 获取总实体数量
     */
    public static long getTotalEntityCount() {
        return indexes.values().stream()
            .mapToLong(AiSpatialIndex::getEntityCount)
            .sum();
    }
    
    /**
     * 获取总玩家数量
     */
    public static long getTotalPlayerCount() {
        return indexes.values().stream()
            .mapToLong(AiSpatialIndex::getPlayerCount)
            .sum();
    }
    
    /**
     * 获取总POI数量
     */
    public static long getTotalPoiCount() {
        return indexes.values().stream()
            .mapToLong(AiSpatialIndex::getPoiCount)
            .sum();
    }
    
    /**
     * 获取统计信息
     */
    public static String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AI Spatial Index Statistics ===\n");
        sb.append(String.format("Global Enabled: %s\n", globalEnabled));
        sb.append(String.format("Total Indexes: %d\n", indexes.size()));
        sb.append(String.format("Total Entities: %d\n", getTotalEntityCount()));
        sb.append(String.format("Total Players: %d\n", getTotalPlayerCount()));
        sb.append(String.format("Total POIs: %d\n", getTotalPoiCount()));
        sb.append("\n=== Per-Level Statistics ===\n");
        
        for (AiSpatialIndex index : indexes.values()) {
            sb.append(index.getStatistics()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 记录查询
     */
    public static void recordQuery() {
        totalQueries++;
    }
    
    /**
     * 获取总查询次数
     */
    public static long getTotalQueries() {
        return totalQueries;
    }
}
