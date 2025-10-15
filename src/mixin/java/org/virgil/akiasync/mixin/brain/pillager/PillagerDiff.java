package org.virgil.akiasync.mixin.brain.pillager;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.AbstractIllager;

/**
 * Pillager family differential (safe reflection via REFLECTIONS util)
 * 
 * Fields (written via REFLECTIONS.setField):
 * - chargeScore → Pillager.crossbow...StartTime
 * - attackTarget (UUID) → Mob.target
 * - raidTarget (BlockPos) → Pillager.raidCenter (if exists)
 * - patrolTarget (BlockPos) → Pillager.patrolTarget
 * 
 * @author Virgil
 */
public final class PillagerDiff {
    
    private double chargeScore;
    private UUID attackTarget;
    private BlockPos raidTarget;
    private BlockPos patrolTarget;
    private int changeCount;
    
    public PillagerDiff() {}
    
    public void setChargeScore(double score) { this.chargeScore = score; changeCount++; }
    public void setAttackTarget(UUID id) { this.attackTarget = id; changeCount++; }
    public void setRaidTarget(BlockPos pos) { this.raidTarget = pos; changeCount++; }
    public void setPatrolTarget(BlockPos pos) { this.patrolTarget = pos; changeCount++; }
    
    /**
     * Apply via safe reflection (REFLECTIONS util, no static block crash)
     */
    public void applyTo(AbstractIllager illager, net.minecraft.server.level.ServerLevel level) {
        // 1. Crossbow charge: Write crossbowChargedStartTime (if charging score > 0)
        if (chargeScore > 0) {
            org.virgil.akiasync.mixin.util.REFLECTIONS.setField(
                illager, "crossbowChargedStartTime", 
                chargeScore > 0 ? level.getGameTime() : 0L
            );
        }
        
        // 2. Attack target: Write Mob.target
        if (attackTarget != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(attackTarget);
            if (player != null && !player.isRemoved()) {
                org.virgil.akiasync.mixin.util.REFLECTIONS.setField(illager, "target", player);
            }
        }
        
        // 3. Raid center: Write Pillager.raidCenter (optional field)
        if (raidTarget != null) {
            org.virgil.akiasync.mixin.util.REFLECTIONS.setField(illager, "raidCenter", raidTarget);
        }
        
        // 4. Patrol target: Write Pillager.patrolTarget
        if (patrolTarget != null) {
            org.virgil.akiasync.mixin.util.REFLECTIONS.setField(illager, "patrolTarget", patrolTarget);
        }
    }
    
    public boolean hasChanges() { return changeCount > 0; }
}

