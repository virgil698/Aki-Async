package org.virgil.akiasync.mixin.brain.guardian;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Guardian;
import org.virgil.akiasync.mixin.util.SafeTargetSetter;
public final class GuardianDiff {
    private UUID guardianTarget;
    private int changeCount;
    public GuardianDiff() {}
    public void setGuardianTarget(UUID id) { this.guardianTarget = id; changeCount++; }
    public void applyTo(Guardian guardian, ServerLevel level) {
        if (guardianTarget != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(guardianTarget);
            if (player != null && !player.isRemoved() && player.isInWater()) {
                SafeTargetSetter.setClosestPlayer(guardian, player);
            }
        }
    }
    public boolean hasChanges() { return changeCount > 0; }
}
