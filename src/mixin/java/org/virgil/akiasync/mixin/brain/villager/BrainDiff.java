package org.virgil.akiasync.mixin.brain.villager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

/**
 * Brain state differential recorder for async CPU computation results
 * 
 * Minimal implementation using pure primitive types to avoid computation overhead.
 * Captures results from async thread for main thread writeback:
 * - topPoi (BlockPos) -> WALK_TARGET memory
 * - likedPlayer (Object) -> LIKED_PLAYER memory
 * 
 * @author Virgil
 */
public final class BrainDiff {
    
    // Pure primitive type fields
    private BlockPos topPoi;
    private Object likedPlayer;
    
    // Statistics
    private int changeCount;
    
    public BrainDiff() {
        this.changeCount = 0;
    }
    
    /**
     * Set the best POI position from async calculation
     */
    public void setTopPoi(BlockPos pos) {
        this.topPoi = pos;
        this.changeCount++;
    }
    
    /**
     * Set the liked player from async calculation
     */
    public void setLikedPlayer(Object player) {
        this.likedPlayer = player;
        this.changeCount++;
    }
    
    /**
     * Apply diff to target Brain (main thread execution)
     * 
     * Whitelist approach: only writes controlled fields, ignores everything else
     * to prevent ExpirableValue type pollution
     * 
     * @param brain Target Brain object
     */
    @SuppressWarnings("unchecked")
    public <E extends LivingEntity> void applyTo(Brain<E> brain) {
        // Write topPoi as WalkTarget (correct type wrapping)
        if (topPoi != null) {
            WalkTarget walkTarget = new WalkTarget(topPoi, 1.0f, 1);
            brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
        }
        
        // Write likedPlayer
        if (likedPlayer != null) {
            brain.setMemory(MemoryModuleType.LIKED_PLAYER, (java.util.UUID) likedPlayer);
        }
        
        // Legacy memory changes removed to prevent ExpirableValue pollution
        // Only write whitelisted fields above
    }
    
    /**
     * Check if there are any changes
     */
    public boolean hasChanges() {
        return changeCount > 0;
    }
    
    /**
     * Get change count
     */
    public int getChangeCount() {
        return changeCount;
    }
    
    @Override
    public String toString() {
        return String.format("BrainDiff[topPoi=%s, likedPlayer=%s, changes=%d]",
                topPoi != null ? "set" : "null",
                likedPlayer != null ? "set" : "null",
                changeCount);
    }
}

