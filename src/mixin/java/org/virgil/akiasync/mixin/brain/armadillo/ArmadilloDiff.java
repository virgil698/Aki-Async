package org.virgil.akiasync.mixin.brain.armadillo;

import net.minecraft.server.level.ServerLevel;


public final class ArmadilloDiff {
    
    private boolean shouldStartRolling = false;
    private boolean shouldStopRolling = false;
    private boolean shouldFlee = false;
    private int changeCount = 0;
    
    public ArmadilloDiff() {}
    
    public void setShouldStartRolling(boolean value) {
        if (value != this.shouldStartRolling) {
            this.shouldStartRolling = value;
            this.changeCount++;
        }
    }
    
    public void setShouldStopRolling(boolean value) {
        if (value != this.shouldStopRolling) {
            this.shouldStopRolling = value;
            this.changeCount++;
        }
    }
    
    public void setShouldFlee(boolean value) {
        if (value != this.shouldFlee) {
            this.shouldFlee = value;
            this.changeCount++;
        }
    }
    
    public boolean shouldStartRolling() { return shouldStartRolling; }
    public boolean shouldStopRolling() { return shouldStopRolling; }
    public boolean shouldFlee() { return shouldFlee; }
    public boolean hasChanges() { return changeCount > 0; }
    
    
    public void applyTo(net.minecraft.world.entity.animal.Animal armadillo, ServerLevel level) {
        if (!hasChanges()) {
            return;
        }
        
        
    }
    
    @Override
    public String toString() {
        return String.format("ArmadilloDiff[startRolling=%s, stopRolling=%s, flee=%s, changes=%d]",
                shouldStartRolling, shouldStopRolling, shouldFlee, changeCount);
    }
}

