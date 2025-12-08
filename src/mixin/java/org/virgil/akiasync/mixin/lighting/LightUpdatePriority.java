package org.virgil.akiasync.mixin.lighting;

public enum LightUpdatePriority {
    CRITICAL(3),   
    HIGH(2),       
    NORMAL(1),     
    LOW(0);        
    
    private final int value;
    
    LightUpdatePriority(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public boolean isHigherThan(LightUpdatePriority other) {
        return this.value > other.value;
    }
}
