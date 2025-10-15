package org.virgil.akiasync.mixin.brain.piglin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

/**
 * Piglin Brain differential for async computation results
 * 
 * Virtual reference pattern (1.21.8):
 * - lookPlayerId (UUID) + lookPlayerPos (BlockPos) → LOOK_TARGET
 * - barterPlayerId (UUID) → BARTER_TARGET  
 * - walkTarget (BlockPos) → WALK_TARGET
 * - huntedTimer (Integer) → HUNTED_RECENTLY
 * 
 * @author Virgil
 */
public final class PiglinDiff {
    
    // Whitelisted fields (pure primitive types + virtual references)
    private java.util.UUID lookPlayerId;
    private BlockPos lookPlayerPos;
    private java.util.UUID barterPlayerId;
    private BlockPos walkTarget;
    private Integer huntedTimer;
    
    // Statistics
    private int changeCount;
    
    public PiglinDiff() {
        this.changeCount = 0;
    }
    
    // Setters (virtual reference pattern)
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
    
    /**
     * Apply to Brain (main thread < 0.1 ms)
     * Virtual reference pattern: getPlayerByUUID + hasGold validation (O(1) hash lookup)
     */
    @SuppressWarnings("unchecked")
    public <E extends LivingEntity> void applyTo(Brain<E> brain, net.minecraft.server.level.ServerLevel level, int lookDist, int barterDist) {
        // 1. WALK_TARGET
        if (walkTarget != null) {
            WalkTarget wt = new WalkTarget(walkTarget, 1.2f, 1);
            brain.setMemory(MemoryModuleType.WALK_TARGET, wt);
        }
        
        // 2. HUNTED_RECENTLY
        if (huntedTimer != null) {
            if (huntedTimer > 0) {
                brain.setMemory(MemoryModuleType.HUNTED_RECENTLY, true);
            } else {
                brain.eraseMemory(MemoryModuleType.HUNTED_RECENTLY);
            }
        }
        
        // 3. LOOK_TARGET (virtual reference: UUID → Player)
        if (lookPlayerId != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(lookPlayerId);
            
            // Defensive null protection: UUID persists after player logout
            if (player == null || player.isRemoved()) {
                brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
                brain.eraseMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
                return;
            }
            
            // Distance threshold: configurable (default 16 blocks)
            double lookDistSqr = lookDist * lookDist;
            if (player.distanceToSqr(lookPlayerPos.getX(), lookPlayerPos.getY(), lookPlayerPos.getZ()) < lookDistSqr) {
                net.minecraft.world.entity.ai.behavior.EntityTracker tracker = 
                    new net.minecraft.world.entity.ai.behavior.EntityTracker(player, true);
                brain.setMemory(MemoryModuleType.LOOK_TARGET, tracker);
            } else {
                brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            }
        }
        
        // 4. BARTER_TARGET (virtual reference: UUID → Player + hasGold validation)
        if (barterPlayerId != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(barterPlayerId);
            
            // Defensive null protection (reuse check from above)
            if (player == null || player.isRemoved()) {
                brain.eraseMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
                return;
            }
            
            // Distance + hasGold dual validation (configurable, default 16 blocks)
            double barterDistSqr = barterDist * barterDist;
            if (player.distanceToSqr(lookPlayerPos.getX(), lookPlayerPos.getY(), lookPlayerPos.getZ()) < barterDistSqr && hasGold(player)) {
                brain.setMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, player);
            } else {
                brain.eraseMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
            }
        }
    }
    
    /**
     * Check if player is holding gold (gold detection)
     */
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

