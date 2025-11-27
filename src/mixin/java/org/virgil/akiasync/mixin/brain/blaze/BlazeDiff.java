package org.virgil.akiasync.mixin.brain.blaze;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Blaze;
public final class BlazeDiff {
    private UUID blazeTarget;
    private int changeCount;
    public BlazeDiff() {}
    public void setBlazeTarget(UUID id) { this.blazeTarget = id; changeCount++; }
    public void applyTo(Blaze blaze, ServerLevel level) {
        if (blazeTarget != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(blazeTarget);
            if (player != null && !player.isRemoved()) {
                org.virgil.akiasync.mixin.util.REFLECTIONS.setField(blaze, "target", player);
            }
        }
    }
    public boolean hasChanges() { return changeCount > 0; }
}
