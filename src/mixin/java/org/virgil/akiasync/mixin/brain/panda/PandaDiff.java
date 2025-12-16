package org.virgil.akiasync.mixin.brain.panda;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;

public final class PandaDiff {
    
    private boolean shouldEat = false;
    private boolean shouldRoll = false;
    private boolean shouldSitDown = false;
    private boolean shouldPickupItem = false;
    private UUID targetItemId = null;
    private int changeCount = 0;
    
    public PandaDiff() {}
    
    public void setShouldEat(boolean value) {
        if (value != this.shouldEat) {
            this.shouldEat = value;
            this.changeCount++;
        }
    }
    
    public void setShouldRoll(boolean value) {
        if (value != this.shouldRoll) {
            this.shouldRoll = value;
            this.changeCount++;
        }
    }
    
    public void setShouldSitDown(boolean value) {
        if (value != this.shouldSitDown) {
            this.shouldSitDown = value;
            this.changeCount++;
        }
    }
    
    public void setShouldPickupItem(boolean value, UUID itemId) {
        if (value != this.shouldPickupItem) {
            this.shouldPickupItem = value;
            this.targetItemId = itemId;
            this.changeCount++;
        }
    }
    
    public boolean shouldEat() { return shouldEat; }
    public boolean shouldRoll() { return shouldRoll; }
    public boolean shouldSitDown() { return shouldSitDown; }
    public boolean shouldPickupItem() { return shouldPickupItem; }
    public UUID targetItemId() { return targetItemId; }
    public boolean hasChanges() { return changeCount > 0; }
    
    public void applyTo(net.minecraft.world.entity.animal.Animal panda, ServerLevel level) {
        if (!hasChanges()) {
            return;
        }
        
    }
    
    @Override
    public String toString() {
        return String.format("PandaDiff[eat=%s, roll=%s, sitDown=%s, pickup=%s, item=%s, changes=%d]",
                shouldEat, shouldRoll, shouldSitDown, shouldPickupItem, targetItemId, changeCount);
    }
}
