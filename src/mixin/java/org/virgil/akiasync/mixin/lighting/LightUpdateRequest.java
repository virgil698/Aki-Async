package org.virgil.akiasync.mixin.lighting;

import net.minecraft.core.BlockPos;

public class LightUpdateRequest implements Comparable<LightUpdateRequest> {
    private final BlockPos pos;
    private final int lightLevel;
    private final LightUpdatePriority priority;
    private final long timestamp;
    private final boolean isChunkLoading;
    
    public LightUpdateRequest(BlockPos pos, int lightLevel, LightUpdatePriority priority, boolean isChunkLoading) {
        this.pos = pos.immutable();
        this.lightLevel = lightLevel;
        this.priority = priority;
        this.timestamp = System.currentTimeMillis();
        this.isChunkLoading = isChunkLoading;
    }
    
    public BlockPos getPos() {
        return pos;
    }
    
    public int getLightLevel() {
        return lightLevel;
    }
    
    public LightUpdatePriority getPriority() {
        return priority;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public boolean isChunkLoading() {
        return isChunkLoading;
    }
    
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }
    
    @Override
    public int compareTo(LightUpdateRequest other) {
        
        int priorityCompare = Integer.compare(other.priority.getValue(), this.priority.getValue());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        
        int levelCompare = Integer.compare(other.lightLevel, this.lightLevel);
        if (levelCompare != 0) {
            return levelCompare;
        }
        
        return Long.compare(this.timestamp, other.timestamp);
    }
}
