package org.virgil.akiasync.mixin.brain;

import java.util.UUID;

/**
 * Witch differential for async computation results
 * 
 * Note: Witches use Goal system (not Brain), so writeback is CPU-computation only for monitoring.
 * Fields computed but not written back (vanilla Goal system controls behavior).
 * 
 * @author Virgil
 */
public final class WitchDiff {
    
    // Computed fields (for monitoring/logging only)
    private String potionToDrink;
    private UUID attackTargetId;
    
    // Statistics
    private int changeCount;
    
    public WitchDiff() {
        this.changeCount = 0;
    }
    
    // Setters
    public void setPotionToDrink(String potion) {
        this.potionToDrink = potion;
        this.changeCount++;
    }
    
    public void setAttackTarget(UUID playerId) {
        this.attackTargetId = playerId;
        this.changeCount++;
    }
    
    /**
     * Apply to Brain (main thread < 0.1 ms)
     * Virtual reference pattern: getPlayerByUUID + health validation
     */
    @SuppressWarnings("unchecked")
    public void applyTo(net.minecraft.world.entity.monster.Witch witch, net.minecraft.server.level.ServerLevel level, int scanDist) {
        // Witches use Goal system, not Brain system
        // All computed data can only be logged/used for monitoring
        // Actual behavior controlled by vanilla Goal AI
        
        // Note: Witch optimization is CPU-heavy computation only
        // Writeback would require modifying Goal system (not implemented yet)
    }
    
    public boolean hasChanges() {
        return changeCount > 0;
    }
    
    @Override
    public String toString() {
        return String.format("WitchDiff[potion=%s, attack=%s, changes=%d]",
                potionToDrink != null ? potionToDrink : "null",
                attackTargetId != null ? "UUID" : "null",
                changeCount);
    }
}

