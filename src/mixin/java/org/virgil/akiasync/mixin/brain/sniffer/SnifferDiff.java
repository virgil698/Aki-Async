package org.virgil.akiasync.mixin.brain.sniffer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class SnifferDiff {
    
    private boolean shouldSniff = false;
    private boolean shouldDig = false;
    private BlockPos targetDigPos = null;
    private int changeCount = 0;
    
    public SnifferDiff() {}
    
    public void setShouldSniff(boolean value) {
        if (value != this.shouldSniff) {
            this.shouldSniff = value;
            this.changeCount++;
        }
    }
    
    public void setShouldDig(boolean value, BlockPos pos) {
        if (value != this.shouldDig) {
            this.shouldDig = value;
            this.targetDigPos = pos;
            this.changeCount++;
        }
    }
    
    public boolean shouldSniff() { return shouldSniff; }
    public boolean shouldDig() { return shouldDig; }
    public BlockPos targetDigPos() { return targetDigPos; }
    public boolean hasChanges() { return changeCount > 0; }
    
    public void applyTo(net.minecraft.world.entity.animal.Animal sniffer, ServerLevel level) {
        if (!hasChanges()) {
            return;
        }
        
    }
    
    @Override
    public String toString() {
        return String.format("SnifferDiff[sniff=%s, dig=%s, pos=%s, changes=%d]",
                shouldSniff, shouldDig, targetDigPos, changeCount);
    }
}
