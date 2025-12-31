package org.virgil.akiasync.mixin.util;

import net.minecraft.core.BlockPos;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class ChunkLightBatch {
    private final long creationTime;
    private final ConcurrentLinkedQueue<BlockPos> positions = new ConcurrentLinkedQueue<>();
    private volatile boolean urgent = false;
    private final AtomicInteger size = new AtomicInteger(0);
    
    public ChunkLightBatch(long creationTime) {
        this.creationTime = creationTime;
    }
    
    public void addPosition(BlockPos pos) {
        positions.offer(pos);
        size.incrementAndGet();
    }
    
    public int size() {
        return size.get();
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    public void markUrgent() {
        urgent = true;
    }
    
    public boolean isUrgent() {
        return urgent;
    }
    
    public ConcurrentLinkedQueue<BlockPos> getPositions() {
        return positions;
    }
}
