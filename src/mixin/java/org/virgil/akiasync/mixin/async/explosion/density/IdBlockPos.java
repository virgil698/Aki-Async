package org.virgil.akiasync.mixin.async.explosion.density;

import java.util.UUID;

public class IdBlockPos {
    private final int x;
    private final int y;
    private final int z;
    private final UUID entityId;
    private final float density;
    
    private static final int CONSTANT_HASH = 0;
    
    public IdBlockPos(int x, int y, int z, UUID entityId, float density) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.entityId = entityId;
        this.density = density;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getZ() {
        return z;
    }
    
    public UUID getEntityId() {
        return entityId;
    }
    
    public float getDensity() {
        return density;
    }
    
    public int getLinearKey() {
        
        return (y << 16) | (z << 8) | x;
    }
    
    @Override
    public int hashCode() {
        
        return CONSTANT_HASH;
    }
    
    @Override
    public boolean equals(Object obj) {
        
        return this == obj;
    }
    
    public boolean strictEquals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IdBlockPos)) return false;
        
        IdBlockPos other = (IdBlockPos) obj;
        return this.x == other.x &&
               this.y == other.y &&
               this.z == other.z &&
               this.entityId.equals(other.entityId);
    }
    
    @Override
    public String toString() {
        return String.format("IdBlockPos[(%d,%d,%d), entity=%s, density=%.3f, linearKey=%d]",
            x, y, z, entityId, density, getLinearKey());
    }
}
