package org.virgil.akiasync.mixin.brain.allay;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class AllayDiff {
    
    private final UUID targetItemId;
    private final UUID followPlayerId;
    private final BlockPos noteBlockPos;
    
    public AllayDiff(UUID targetItemId, UUID followPlayerId, BlockPos noteBlockPos) {
        this.targetItemId = targetItemId;
        this.followPlayerId = followPlayerId;
        this.noteBlockPos = noteBlockPos;
    }
    
    public void applyTo(Allay allay, ServerLevel level) {
        Brain<?> brain = allay.getBrain();
        
        if (noteBlockPos != null) {
            WalkTarget walkTarget = new WalkTarget(noteBlockPos, 0.5F, 2);
            brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
            return;
        }
        
        if (targetItemId != null) {
            Entity entity = level.getEntity(targetItemId);
            if (entity instanceof ItemEntity && entity.isAlive()) {
                brain.setMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, 0);
                
            }
            return;
        }
        
        if (followPlayerId != null) {
            Player player = level.getPlayerByUUID(followPlayerId);
            if (player != null && player.isAlive()) {
                
                WalkTarget walkTarget = new WalkTarget(player, 0.6F, 2);
                brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "AllayDiff{item=%s, player=%s, noteBlock=%s}",
            targetItemId, followPlayerId, noteBlockPos
        );
    }
}
