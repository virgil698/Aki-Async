package org.virgil.akiasync.mixin.brain.frog;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;


public final class FrogDiff {
    
    private boolean shouldEatSlime = false;
    private UUID targetSlimeId = null;
    private boolean shouldLongJump = false;
    private int changeCount = 0;
    
    public FrogDiff() {}
    
    public void setShouldEatSlime(boolean value, UUID slimeId) {
        if (value != this.shouldEatSlime) {
            this.shouldEatSlime = value;
            this.targetSlimeId = slimeId;
            this.changeCount++;
        }
    }
    
    public void setShouldLongJump(boolean value) {
        if (value != this.shouldLongJump) {
            this.shouldLongJump = value;
            this.changeCount++;
        }
    }
    
    public boolean shouldEatSlime() { return shouldEatSlime; }
    public UUID targetSlimeId() { return targetSlimeId; }
    public boolean shouldLongJump() { return shouldLongJump; }
    public boolean hasChanges() { return changeCount > 0; }
    
    
    public void applyTo(net.minecraft.world.entity.animal.Animal frog, ServerLevel level) {
        if (!hasChanges()) {
            return;
        }
        
        
    }
    
    @Override
    public String toString() {
        return String.format("FrogDiff[eatSlime=%s, target=%s, longJump=%s, changes=%d]",
                shouldEatSlime, targetSlimeId, shouldLongJump, changeCount);
    }
}

