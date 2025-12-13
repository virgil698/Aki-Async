package org.virgil.akiasync.mixin.brain.camel;

import net.minecraft.server.level.ServerLevel;


public final class CamelDiff {
    
    private boolean shouldStandUp = false;
    private boolean shouldSitDown = false;
    private boolean shouldDash = false;
    private int changeCount = 0;
    
    public CamelDiff() {}
    
    public void setShouldStandUp(boolean value) {
        if (value != this.shouldStandUp) {
            this.shouldStandUp = value;
            this.changeCount++;
        }
    }
    
    public void setShouldSitDown(boolean value) {
        if (value != this.shouldSitDown) {
            this.shouldSitDown = value;
            this.changeCount++;
        }
    }
    
    public void setShouldDash(boolean value) {
        if (value != this.shouldDash) {
            this.shouldDash = value;
            this.changeCount++;
        }
    }
    
    public boolean shouldStandUp() { return shouldStandUp; }
    public boolean shouldSitDown() { return shouldSitDown; }
    public boolean shouldDash() { return shouldDash; }
    public boolean hasChanges() { return changeCount > 0; }
    
    
    public void applyTo(net.minecraft.world.entity.animal.Animal camel, ServerLevel level) {
        if (!hasChanges()) {
            return;
        }
        
        
    }
    
    @Override
    public String toString() {
        return String.format("CamelDiff[standUp=%s, sitDown=%s, dash=%s, changes=%d]",
                shouldStandUp, shouldSitDown, shouldDash, changeCount);
    }
}

