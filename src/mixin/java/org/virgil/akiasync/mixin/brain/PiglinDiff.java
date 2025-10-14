package org.virgil.akiasync.mixin.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

/**
 * 猪灵Brain Diff（异步计算结果）
 * 
 * 虚拟引用模板（1.21.8）：
 * - lookPlayerId（UUID） + lookPlayerPos（BlockPos）→ LOOK_TARGET
 * - barterPlayerId（UUID）→ BARTER_TARGET
 * - walkTarget（BlockPos）→ WALK_TARGET
 * - huntedTimer（Integer）→ HUNTED_RECENTLY
 * 
 * @author Virgil
 */
public final class PiglinDiff {
    
    // 白名单字段（纯原始类型 + 虚拟引用）
    private java.util.UUID lookPlayerId;
    private BlockPos lookPlayerPos;
    private java.util.UUID barterPlayerId;
    private BlockPos walkTarget;
    private Integer huntedTimer;
    
    // 统计
    private int changeCount;
    
    public PiglinDiff() {
        this.changeCount = 0;
    }
    
    // Setters（虚拟引用）
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
     * 应用到Brain（主线程 < 0.1 ms）
     * 虚拟引用模板：getPlayerByUUID + hasGold校验（O(1)哈希查询）
     */
    @SuppressWarnings("unchecked")
    public <E extends LivingEntity> void applyTo(Brain<E> brain, net.minecraft.server.level.ServerLevel level, int lookDist, int barterDist) {
        // 1. WALK_TARGET（已有）
        if (walkTarget != null) {
            WalkTarget wt = new WalkTarget(walkTarget, 1.2f, 1);
            brain.setMemory(MemoryModuleType.WALK_TARGET, wt);
        }
        
        // 2. HUNTED_RECENTLY（已有）
        if (huntedTimer != null) {
            if (huntedTimer > 0) {
                brain.setMemory(MemoryModuleType.HUNTED_RECENTLY, true);
            } else {
                brain.eraseMemory(MemoryModuleType.HUNTED_RECENTLY);
            }
        }
        
        // 3. LOOK_TARGET（虚拟引用：UUID → Player）
        if (lookPlayerId != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(lookPlayerId);
            
            // 防御性空保护：玩家下线后 UUID 残留
            if (player == null || player.isRemoved()) {
                brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
                brain.eraseMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
                return;
            }
            
            // 距离阈值：可配置（默认16格）
            double lookDistSqr = lookDist * lookDist;
            if (player.distanceToSqr(lookPlayerPos.getX(), lookPlayerPos.getY(), lookPlayerPos.getZ()) < lookDistSqr) {
                net.minecraft.world.entity.ai.behavior.EntityTracker tracker = 
                    new net.minecraft.world.entity.ai.behavior.EntityTracker(player, true);
                brain.setMemory(MemoryModuleType.LOOK_TARGET, tracker);
            } else {
                brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            }
        }
        
        // 4. BARTER_TARGET（虚拟引用：UUID → Player + hasGold校验）
        if (barterPlayerId != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(barterPlayerId);
            
            // 防御性空保护（复用上面的检查）
            if (player == null || player.isRemoved()) {
                brain.eraseMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
                return;
            }
            
            // 距离+hasGold双重校验（可配置，默认16格）
            double barterDistSqr = barterDist * barterDist;
            if (player.distanceToSqr(lookPlayerPos.getX(), lookPlayerPos.getY(), lookPlayerPos.getZ()) < barterDistSqr && hasGold(player)) {
                brain.setMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, player);
            } else {
                brain.eraseMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
            }
        }
    }
    
    /**
     * 检查玩家是否持金（gold 检测）
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

