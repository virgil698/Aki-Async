package org.virgil.akiasync.mixin.brain.witch;
import java.util.Comparator;
import net.minecraft.world.entity.monster.Witch;
public final class WitchCpuCalculator {
    public static WitchDiff runCpuOnly(Witch witch, WitchSnapshot snap) {
        WitchDiff diff = new WitchDiff();
        if (!snap.players().isEmpty()) {
            java.util.UUID target = snap.players().stream()
                .min(Comparator.comparingDouble(p -> p.pos().distSqr(witch.blockPosition())))
                .map(WitchSnapshot.PlayerInfo::id)
                .orElse(null);
            diff.setWitchTarget(target);
        }
        return diff;
    }
}
