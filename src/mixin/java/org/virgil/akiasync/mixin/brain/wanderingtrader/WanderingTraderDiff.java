package org.virgil.akiasync.mixin.brain.wanderingtrader;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class WanderingTraderDiff {
    
    private final UUID closestPlayer;
    private final BlockPos bestPoi;
    private final boolean shouldWander;
    
    public WanderingTraderDiff(UUID closestPlayer, BlockPos bestPoi, boolean shouldWander) {
        this.closestPlayer = closestPlayer;
        this.bestPoi = bestPoi;
        this.shouldWander = shouldWander;
    }
    
    public void applyTo(WanderingTrader trader, ServerLevel level) {
        Brain<?> brain = trader.getBrain();
        
        if (closestPlayer != null) {
            Player player = level.getPlayerByUUID(closestPlayer);
            if (player != null && player.isAlive()) {
                brain.setMemory(MemoryModuleType.INTERACTION_TARGET, player);
            }
        }
        
        if (bestPoi != null && shouldWander) {
            WalkTarget walkTarget = new WalkTarget(bestPoi, 0.5F, 2);
            brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
        }
        
        if (shouldWander && bestPoi == null && closestPlayer == null) {
            
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "WanderingTraderDiff{player=%s, poi=%s, wander=%s}",
            closestPlayer, bestPoi, shouldWander
        );
    }
}
