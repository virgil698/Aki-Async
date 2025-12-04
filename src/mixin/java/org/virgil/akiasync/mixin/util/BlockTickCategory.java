package org.virgil.akiasync.mixin.util;

public enum BlockTickCategory {
    
    ENTITY_INTERACTION(false, 0, "Entity Interaction"),
    
    REDSTONE(false, 1, "Redstone"),
    
    CROP_GROWTH(true, 2, "Crop Growth"),
    
    LEAF_DECAY(false, 3, "Leaf Decay"),
    
    SAFE_ASYNC(true, 4, "Safe Async");
    
    private final boolean canAsync;
    private final int priority;
    private final String displayName;
    
    BlockTickCategory(boolean canAsync, int priority, String displayName) {
        this.canAsync = canAsync;
        this.priority = priority;
        this.displayName = displayName;
    }
    
    public boolean canAsync() {
        return canAsync;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
