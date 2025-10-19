package org.virgil.akiasync.mixin.brain.witch;

import java.lang.reflect.Field;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

public final class WitchDiff {
    private static final Field TARGET_FIELD;
    
    static {
        Field temp = null;
        try {
            temp = Mob.class.getDeclaredField("target");
            temp.setAccessible(true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        TARGET_FIELD = temp;
    }
    
    private UUID witchTarget;
    private int changeCount;
    
    public WitchDiff() {}
    public void setWitchTarget(UUID id) { this.witchTarget = id; changeCount++; }
    
    public void applyTo(net.minecraft.world.entity.monster.Witch witch, ServerLevel level) {
        if (TARGET_FIELD == null) return;
        
        if (witchTarget != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(witchTarget);
            if (player != null && !player.isRemoved()) {
                try {
                    TARGET_FIELD.set(witch, player);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
    
    public boolean hasChanges() { return changeCount > 0; }
}

