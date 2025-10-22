package org.virgil.akiasync.mixin.brain.universal;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.world.entity.Mob;
public final class UniversalAiCpuCalculator {
    public static UniversalAiDiff runCpuOnly(Mob mob, UniversalAiSnapshot snap) {
        UniversalAiDiff diff = new UniversalAiDiff();
        if (!snap.players().isEmpty()) {
            UUID target = snap.players().stream()
                .min(Comparator.comparingDouble(p -> p.pos().distSqr(mob.blockPosition())))
                .map(UniversalAiSnapshot.PlayerInfo::id)
                .orElse(null);
            diff.setTarget(target);
        }
        return diff;
    }
}