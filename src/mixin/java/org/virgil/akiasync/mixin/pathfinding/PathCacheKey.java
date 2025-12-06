package org.virgil.akiasync.mixin.pathfinding;

import net.minecraft.core.BlockPos;

public class PathCacheKey {
    private final long startHash;
    private final long endHash;
    private final int hashCode;
    
    public PathCacheKey(BlockPos start, BlockPos end) {
        this.startHash = encodePosition(start);
        this.endHash = encodePosition(end);
        this.hashCode = computeHashCode();
    }
    
    private static long encodePosition(BlockPos pos) {
        long x = (long) pos.getX() & 0x1FFFFF;  
        long y = (long) pos.getY() & 0x1FFFFF;  
        long z = (long) pos.getZ() & 0x3FFFFF;  
        
        return (x << 43) | (y << 22) | z;
    }
    
    private int computeHashCode() {
        int result = (int) (startHash ^ (startHash >>> 32));
        result = 31 * result + (int) (endHash ^ (endHash >>> 32));
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PathCacheKey)) return false;
        
        PathCacheKey other = (PathCacheKey) obj;
        return this.startHash == other.startHash && this.endHash == other.endHash;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    public boolean isSimilar(PathCacheKey other, int tolerance) {
        BlockPos thisStart = decodePosition(this.startHash);
        BlockPos thisEnd = decodePosition(this.endHash);
        BlockPos otherStart = decodePosition(other.startHash);
        BlockPos otherEnd = decodePosition(other.endHash);
        
        return thisStart.closerThan(otherStart, tolerance) && 
               thisEnd.closerThan(otherEnd, tolerance);
    }
    
    private static BlockPos decodePosition(long encoded) {
        int x = (int) ((encoded >> 43) & 0x1FFFFF);
        int y = (int) ((encoded >> 22) & 0x1FFFFF);
        int z = (int) (encoded & 0x3FFFFF);
        
        if ((x & 0x100000) != 0) x |= 0xFFE00000;
        if ((y & 0x100000) != 0) y |= 0xFFE00000;
        if ((z & 0x200000) != 0) z |= 0xFFC00000;
        
        return new BlockPos(x, y, z);
    }
    
    public BlockPos getStart() {
        return decodePosition(startHash);
    }
    
    public BlockPos getEnd() {
        return decodePosition(endHash);
    }
}
