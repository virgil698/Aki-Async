package org.virgil.akiasync.mixin.brain.core;

import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class AiSpatialIndexManager {
    
    private static final Map<ServerLevel, AiSpatialIndex> indexes = new ConcurrentHashMap<>();
    
    private static volatile boolean globalEnabled = true;
    
    private static final AtomicLong totalIndexes = new AtomicLong(0);
    private static final AtomicLong totalQueries = new AtomicLong(0);
    
    public static AiSpatialIndex getIndex(ServerLevel level) {
        if (level == null) {
            return null;
        }
        
        return indexes.computeIfAbsent(level, l -> {
            totalIndexes.incrementAndGet();
            AiSpatialIndex index = new AiSpatialIndex(l);
            index.setEnabled(globalEnabled);
            return index;
        });
    }
    
    public static void removeIndex(ServerLevel level) {
        if (level == null) return;
        
        AiSpatialIndex index = indexes.remove(level);
        if (index != null) {
            index.clear();
            totalIndexes.decrementAndGet();
        }
    }
    
    public static void clearAll() {
        for (AiSpatialIndex index : indexes.values()) {
            index.clear();
        }
        indexes.clear();
        totalIndexes.set(0);
        totalQueries.set(0);
    }
    
    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
        
        for (AiSpatialIndex index : indexes.values()) {
            index.setEnabled(enabled);
        }
    }
    
    public static boolean isGlobalEnabled() {
        return globalEnabled;
    }
    
    public static int getIndexCount() {
        return indexes.size();
    }
    
    public static long getTotalEntityCount() {
        return indexes.values().stream()
            .mapToLong(AiSpatialIndex::getEntityCount)
            .sum();
    }
    
    public static long getTotalPlayerCount() {
        return indexes.values().stream()
            .mapToLong(AiSpatialIndex::getPlayerCount)
            .sum();
    }
    
    public static long getTotalPoiCount() {
        return indexes.values().stream()
            .mapToLong(AiSpatialIndex::getPoiCount)
            .sum();
    }
    
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
    
    public static void recordQuery() {
        totalQueries.incrementAndGet();
    }
    
    public static long getTotalQueries() {
        return totalQueries.get();
    }
}
