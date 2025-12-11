package org.virgil.akiasync.mixin.brain.warden;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;

import java.util.UUID;

public final class WardenDiff {
    
    private final UUID targetId;
    private final boolean shouldUseSonicBoom;
    private final boolean shouldDig;
    
    public WardenDiff(UUID targetId, boolean shouldUseSonicBoom, boolean shouldDig) {
        this.targetId = targetId;
        this.shouldUseSonicBoom = shouldUseSonicBoom;
        this.shouldDig = shouldDig;
    }
    
    public void applyTo(Warden warden, ServerLevel level) {
        Brain<?> brain = warden.getBrain();
        
        if (targetId != null) {
            Entity target = level.getEntity(targetId);
            if (target instanceof LivingEntity && target.isAlive()) {
                brain.setMemory(MemoryModuleType.ATTACK_TARGET, (LivingEntity) target);
            }
        }
        
    }
    
    @Override
    public String toString() {
        return String.format(
            "WardenDiff{target=%s, sonicBoom=%s, dig=%s}",
            targetId, shouldUseSonicBoom, shouldDig
        );
    }
}
