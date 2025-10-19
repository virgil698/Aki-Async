package org.virgil.akiasync.mixin.brain.piglin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

/**
 * Piglin Brain differential.
 * @author Virgil
 */
public final class PiglinDiff {
    
    private java.util.UUID lookPlayerId;
    private BlockPos lookPlayerPos;
    private java.util.UUID barterPlayerId;
    private BlockPos walkTarget;
    private Integer huntedTimer;
    
    private int changeCount;
    
    public PiglinDiff() {
        this.changeCount = 0;
    }
    
    public void setLookTarget(java.util.UUID playerId, BlockPos playerPos) {
        this.lookPlayerId = playerId;
        this.lookPlayerPos = playerPos;
        this.changeCount++;
    }
    
    public void setBarterTarget(java.util.UUID playerId) {
        this.barterPlayerId = playerId;
        this.changeCount++;
    }
    
    public void setWalkTarget(BlockPos pos) {
        this.walkTarget = pos;
        this.changeCount++;
    }
    
    public void setHuntedTimer(int timer) {
        this.huntedTimer = timer;
        this.changeCount++;
    }
    
    @SuppressWarnings("unchecked")
    public <E extends LivingEntity> void applyTo(Brain<E> brain, net.minecraft.server.level.ServerLevel level, int lookDist, int barterDist) {
        if (walkTarget != null) {
            WalkTarget wt = new WalkTarget(walkTarget, 1.2f, 1);
            brain.setMemory(MemoryModuleType.WALK_TARGET, wt);
        }
        
        if (huntedTimer != null) {
            if (huntedTimer > 0) {
                brain.setMemory(MemoryModuleType.HUNTED_RECENTLY, true);
            } else {
                brain.eraseMemory(MemoryModuleType.HUNTED_RECENTLY);
            }
        }
        
        if (lookPlayerId != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(lookPlayerId);
            
            if (player == null || player.isRemoved()) {
                brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
                brain.eraseMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
                return;
            }
            
            double lookDistSqr = lookDist * lookDist;
            if (player.distanceToSqr(lookPlayerPos.getX(), lookPlayerPos.getY(), lookPlayerPos.getZ()) < lookDistSqr) {
                net.minecraft.world.entity.ai.behavior.EntityTracker tracker = 
                    new net.minecraft.world.entity.ai.behavior.EntityTracker(player, true);
                brain.setMemory(MemoryModuleType.LOOK_TARGET, tracker);
            } else {
                brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            }
        }
        
        if (barterPlayerId != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(barterPlayerId);
            
            if (player == null || player.isRemoved()) {
                brain.eraseMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
                return;
            }
            
            double barterDistSqr = barterDist * barterDist;
            if (player.distanceToSqr(lookPlayerPos.getX(), lookPlayerPos.getY(), lookPlayerPos.getZ()) < barterDistSqr && hasGold(player)) {
                brain.setMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, player);
            } else {
                brain.eraseMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
            }
        }
    }
    
    private boolean hasGold(net.minecraft.world.entity.player.Player player) {
        net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
        return mainHand.is(net.minecraft.world.item.Items.GOLD_INGOT) ||
               mainHand.is(net.minecraft.world.item.Items.GOLD_BLOCK);
    }
    
    public boolean hasChanges() {
        return changeCount > 0;
    }
    
    @Override
    public String toString() {
        return String.format("PiglinDiff[look=%s, barter=%s, walk=%s, hunted=%s, changes=%d]",
                lookPlayerId != null ? "UUID" : "null",
                barterPlayerId != null ? "UUID" : "null",
                walkTarget != null ? "set" : "null",
                huntedTimer != null ? huntedTimer.toString() : "null",
                changeCount);
    }
}

