package org.virgil.akiasync.mixin.brain.enderman;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import org.virgil.akiasync.mixin.util.SafeTargetSetter;

public final class EndermanDiff {
    
    private boolean shouldTeleport = false;
    private boolean shouldClearTarget = false;
    private UUID attackTarget = null;
    private int changeCount = 0;
    
    public EndermanDiff() {}
    
    public void setShouldTeleport(boolean value) {
        if (value != this.shouldTeleport) {
            this.shouldTeleport = value;
            this.changeCount++;
        }
    }
    
    public void setShouldClearTarget(boolean value) {
        if (value != this.shouldClearTarget) {
            this.shouldClearTarget = value;
            this.changeCount++;
        }
    }
    
    public void setAttackTarget(UUID targetId) {
        if (targetId != this.attackTarget) {
            this.attackTarget = targetId;
            this.changeCount++;
        }
    }
    
    public boolean shouldTeleport() { return shouldTeleport; }
    public boolean shouldClearTarget() { return shouldClearTarget; }
    public boolean hasChanges() { return changeCount > 0; }
    
    public void applyTo(EnderMan enderman, ServerLevel level) {
        if (!hasChanges()) {
            return;
        }
        
        
        if (shouldClearTarget && enderman.getTarget() != null) {
            SafeTargetSetter.clearTarget(enderman);
            return; 
        }
        
        
        if (attackTarget != null) {
            LivingEntity target = level.getEntity(attackTarget) instanceof LivingEntity living ? living : null;
            if (target != null && target.isAlive()) {
                SafeTargetSetter.setCustomTarget(enderman, target);
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format("EndermanDiff[teleport=%s, clearTarget=%s, attackTarget=%s, changes=%d]",
                shouldTeleport, shouldClearTarget, attackTarget != null ? "set" : "null", changeCount);
    }
}
