package org.virgil.akiasync.mixin.brain.universal;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * Universal AI differential (unified target field)
 * 
 * @author Virgil
 */
public final class UniversalAiDiff {
    
    private UUID target;
    private int changeCount;
    
    public UniversalAiDiff() {}
    
    public void setTarget(UUID id) {
        this.target = id;
        changeCount++;
    }
    
    public void applyTo(Mob mob, ServerLevel level) {
        if (target != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(target);
            if (player != null && !player.isRemoved()) {
                org.virgil.akiasync.mixin.util.REFLECTIONS.setField(mob, "target", player);
            }
        }
    }
    
    public boolean hasChanges() { return changeCount > 0; }
}

