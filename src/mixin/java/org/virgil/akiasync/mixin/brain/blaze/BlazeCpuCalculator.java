package org.virgil.akiasync.mixin.brain.blaze;
import java.util.Comparator;
import net.minecraft.world.entity.monster.Blaze;
public final class BlazeCpuCalculator {
    public static BlazeDiff runCpuOnly(Blaze blaze, BlazeSnapshot snap) {
        BlazeDiff diff = new BlazeDiff();
        if (snap.blazeCd() <= 0 && !snap.players().isEmpty()) {
            java.util.UUID target = snap.players().stream()
                .min(Comparator.comparingDouble(p -> p.pos().distSqr(blaze.blockPosition())))
                .map(BlazeSnapshot.PlayerInfo::id)
                .orElse(null);
            diff.setBlazeTarget(target);
        }
        return diff;
    }
}
