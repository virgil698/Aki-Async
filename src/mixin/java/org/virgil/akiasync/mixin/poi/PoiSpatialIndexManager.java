package org.virgil.akiasync.mixin.poi;

import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PoiSpatialIndexManager {
    
    private static final Map<ServerLevel, PoiSpatialIndex> indexes = new ConcurrentHashMap<>();
    
    private static volatile boolean globalEnabled = true;
    
    public static PoiSpatialIndex getIndex(ServerLevel level) {
        if (level == null || !globalEnabled) {
            return null;
        }
        
        return indexes.computeIfAbsent(level, PoiSpatialIndex::new);
    }
    
    public static void removeIndex(ServerLevel level) {
        if (level == null) return;
        
        PoiSpatialIndex index = indexes.remove(level);
        if (index != null) {
            index.clear();
        }
    }
    
    public static void clearAll() {
        for (PoiSpatialIndex index : indexes.values()) {
            index.clear();
        }
        indexes.clear();
    }
    
    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
    }
    
    public static boolean isGlobalEnabled() {
        return globalEnabled;
    }
    
    public static int getIndexCount() {
        return indexes.size();
    }
    
    public static long getTotalPoiCount() {
        return indexes.values().stream()
            .mapToLong(PoiSpatialIndex::getTotalPois)
            .sum();
    }
    
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
