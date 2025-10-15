package org.virgil.akiasync.mixin.brain;

import java.util.Comparator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.AbstractIllager;

/**
 * Pillager family CPU calculator (Evoker/Vindicator/Ravager unified)
 * 
 * Async computation (0.5-1 ms):
 * - Crossbow charge scoring (CrossbowItem.isCharged check)
 * - Raid priority scoring (RaidManager.findRaid)
 * - Attack target filtering (health < 75%)
 * - Patrol POI scoring
 * 
 * @author Virgil
 */
public final class PillagerCpuCalculator {
    
    public static PillagerDiff runCpuOnly(AbstractIllager illager, PillagerSnapshot snap) {
        PillagerDiff diff = new PillagerDiff();
        
        // 1. Crossbow charge score (Pillager only)
        double chargeScore = snap.charging() ? 100.0 : 0.0;
        diff.setChargeScore(chargeScore);
        
        // 2. Raid priority (if raid exists)
        if (snap.raid() != null) {
            diff.setRaidTarget(snap.raid());
        }
        
        // 3. Attack target (health < 75%)
        java.util.UUID attackTarget = snap.players().stream()
            .filter(p -> p.health() < 0.75f)
            .min(Comparator.comparingDouble(p -> 
                p.pos().distSqr(illager.blockPosition())
            ))
            .map(PillagerSnapshot.PlayerHealthInfo::id)
            .orElse(null);
        
        if (attackTarget != null) {
            diff.setAttackTarget(attackTarget);
        }
        
        // 4. Patrol POI (closest POI)
        BlockPos patrolTarget = snap.pois().stream()
            .min(Comparator.comparingDouble(poi -> poi.distSqr(illager.blockPosition())))
            .orElse(null);
        
        if (patrolTarget != null) {
            diff.setPatrolTarget(patrolTarget);
        }
        
        return diff;
    }
}

