package org.virgil.akiasync.mixin.brain.witch;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.virgil.akiasync.mixin.util.SafeTargetSetter;
public final class WitchDiff {
    private UUID witchTarget;
    private int changeCount;
    public WitchDiff() {}
    public void setWitchTarget(UUID id) { this.witchTarget = id; changeCount++; }
    public void applyTo(net.minecraft.world.entity.monster.Witch witch, ServerLevel level) {
        if (witchTarget != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(witchTarget);
            if (player != null && !player.isRemoved()) {
                SafeTargetSetter.setClosestPlayer(witch, player);
            }
        }
    }
    public boolean hasChanges() { return changeCount > 0; }
}
