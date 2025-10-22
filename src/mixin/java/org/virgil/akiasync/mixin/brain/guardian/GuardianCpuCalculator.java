package org.virgil.akiasync.mixin.brain.guardian;
import java.util.Comparator;
import net.minecraft.world.entity.monster.Guardian;
public final class GuardianCpuCalculator {
    public static GuardianDiff runCpuOnly(Guardian guardian, GuardianSnapshot snap) {
        GuardianDiff diff = new GuardianDiff();
        if (!snap.players().isEmpty()) {
            java.util.UUID target = snap.players().stream()
                .min(Comparator.comparingDouble(p -> p.pos().distSqr(guardian.blockPosition())))
                .map(GuardianSnapshot.PlayerInfo::id)
                .orElse(null);
            diff.setGuardianTarget(target);
        }
        return diff;
    }
}