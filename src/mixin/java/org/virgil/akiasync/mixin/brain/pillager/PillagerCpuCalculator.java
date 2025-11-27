package org.virgil.akiasync.mixin.brain.pillager;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.AbstractIllager;
public final class PillagerCpuCalculator {
    public static PillagerDiff runCpuOnly(AbstractIllager illager, PillagerSnapshot snap) {
        PillagerDiff diff = new PillagerDiff();
        double chargeScore = snap.charging() ? 100.0 : 0.0;
        diff.setChargeScore(chargeScore);
        if (snap.raid() != null) {
            diff.setRaidTarget(snap.raid());
        }
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
        BlockPos patrolTarget = snap.pois().stream()
            .min(Comparator.comparingDouble(poi -> poi.distSqr(illager.blockPosition())))
            .orElse(null);
        if (patrolTarget != null) {
            diff.setPatrolTarget(patrolTarget);
        }
        return diff;
    }
}
