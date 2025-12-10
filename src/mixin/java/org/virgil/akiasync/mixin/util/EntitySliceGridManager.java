package org.virgil.akiasync.mixin.util;

import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntitySliceGridManager {
    
    private static final Map<Level, EntitySliceGrid> SLICE_GRIDS = new ConcurrentHashMap<>();
    
    public static EntitySliceGrid getSliceGrid(Level level) {
        return SLICE_GRIDS.get(level);
    }
    
    public static EntitySliceGrid getOrCreateSliceGrid(Level level) {
        return SLICE_GRIDS.computeIfAbsent(level, k -> new EntitySliceGrid());
    }
    
    public static void clearSliceGrid(Level level) {
        EntitySliceGrid sliceGrid = SLICE_GRIDS.remove(level);
        if (sliceGrid != null) {
            sliceGrid.clear();
        }
    }
    
    public static void removeSliceGrid(Level level) {
        clearSliceGrid(level);
    }
    
    public static void clearAllSliceGrids() {
        for (EntitySliceGrid sliceGrid : SLICE_GRIDS.values()) {
            sliceGrid.clear();
        }
        SLICE_GRIDS.clear();
    }
    
    public static String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("Entity Slice Grid Stats:\n");
        
        for (Map.Entry<Level, EntitySliceGrid> entry : SLICE_GRIDS.entrySet()) {
            Level level = entry.getKey();
            EntitySliceGrid sliceGrid = entry.getValue();
            
            sb.append("  Level: ").append(level.dimension().location())
              .append(", ").append(sliceGrid.getStats())
              .append("\n");
        }
        
        return sb.toString();
    }
}
