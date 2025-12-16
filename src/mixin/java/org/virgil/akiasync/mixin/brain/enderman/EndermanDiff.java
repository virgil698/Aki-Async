package org.virgil.akiasync.mixin.brain.enderman;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.EnderMan;

public final class EndermanDiff {
    
    private boolean shouldTeleport = false;
    private boolean shouldClearTarget = false;
    private int changeCount = 0;
    
    public EndermanDiff() {}
    
    public void setShouldTeleport(boolean value) {
        if (value != this.shouldTeleport) {
            this.shouldTeleport = value;
            this.changeCount++;
        }
    }
    
    public void setShouldClearTarget(boolean value) {
        if (value != this.shouldClearTarget) {
            this.shouldClearTarget = value;
            this.changeCount++;
        }
    }
    
    public boolean shouldTeleport() { return shouldTeleport; }
    public boolean shouldClearTarget() { return shouldClearTarget; }
    public boolean hasChanges() { return changeCount > 0; }
    
    public void applyTo(EnderMan enderman, ServerLevel level) {
        if (!hasChanges()) {
            return;
        }
        
        if (shouldClearTarget && enderman.getTarget() != null) {
            enderman.setTarget(null);
        }
        
    }
    
    @Override
    public String toString() {
        return String.format("EndermanDiff[teleport=%s, clearTarget=%s, changes=%d]",
                shouldTeleport, shouldClearTarget, changeCount);
    }
}
