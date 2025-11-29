package org.virgil.akiasync.mixin.brain.evoker;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Evoker;
public final class EvokerDiff {
    private UUID evokerTarget;
    private int changeCount;
    public EvokerDiff() {}
    public void setEvokerTarget(UUID id) {
        this.evokerTarget = id;
        changeCount++;
    }
    public void applyTo(Evoker evoker, ServerLevel level) {
        if (evokerTarget != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(evokerTarget);
            if (player != null && !player.isRemoved()) {
                org.virgil.akiasync.mixin.util.REFLECTIONS.setField(evoker, "target", player);
            }
        }
    }
    public boolean hasChanges() { return changeCount > 0; }
}
