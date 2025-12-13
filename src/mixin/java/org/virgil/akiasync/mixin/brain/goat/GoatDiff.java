package org.virgil.akiasync.mixin.brain.goat;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;


public final class GoatDiff {
    
    private boolean shouldRam = false;
    private UUID targetEntityId = null;
    private boolean shouldHighJump = false;
    private int changeCount = 0;
    
    public GoatDiff() {}
    
    public void setShouldRam(boolean value, UUID targetId) {
        if (value != this.shouldRam) {
            this.shouldRam = value;
            this.targetEntityId = targetId;
            this.changeCount++;
        }
    }
    
    public void setShouldHighJump(boolean value) {
        if (value != this.shouldHighJump) {
            this.shouldHighJump = value;
            this.changeCount++;
        }
    }
    
    public boolean shouldRam() { return shouldRam; }
    public UUID targetEntityId() { return targetEntityId; }
    public boolean shouldHighJump() { return shouldHighJump; }
    public boolean hasChanges() { return changeCount > 0; }
    
    
    public void applyTo(net.minecraft.world.entity.animal.Animal goat, ServerLevel level) {
        if (!hasChanges()) {
            return;
        }
        
        
    }
    
    @Override
    public String toString() {
        return String.format("GoatDiff[ram=%s, target=%s, highJump=%s, changes=%d]",
                shouldRam, targetEntityId, shouldHighJump, changeCount);
    }
}

