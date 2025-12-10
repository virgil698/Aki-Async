package org.virgil.akiasync.mixin.brain.hoglin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 疣猪兽差异对象
 * 
 * 存储异步计算的结果，在主线程应用到疣猪兽
 * 
 * @author AkiAsync
 */
public final class HoglinDiff {
    
    private final UUID attackTargetId;
    private final boolean shouldFlee;
    private final BlockPos fleeTarget;
    
    public HoglinDiff(UUID attackTargetId, boolean shouldFlee, BlockPos fleeTarget) {
        this.attackTargetId = attackTargetId;
        this.shouldFlee = shouldFlee;
        this.fleeTarget = fleeTarget;
    }
    
    /**
     * 应用差异到疣猪兽
     * 
     * @param hoglin 疣猪兽
     * @param level 世界
     */
    public void applyTo(Hoglin hoglin, ServerLevel level) {
        Brain<?> brain = hoglin.getBrain();
        
        if (shouldFlee && fleeTarget != null) {
            
            WalkTarget walkTarget = new WalkTarget(fleeTarget, 1.2F, 0);
            brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
            
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            return;
        }
        
        if (attackTargetId != null) {
            Player player = level.getPlayerByUUID(attackTargetId);
            if (player != null && player.isAlive()) {
                brain.setMemory(MemoryModuleType.ATTACK_TARGET, player);
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "HoglinDiff{attack=%s, flee=%s, fleeTarget=%s}",
            attackTargetId, shouldFlee, fleeTarget
        );
    }
}
