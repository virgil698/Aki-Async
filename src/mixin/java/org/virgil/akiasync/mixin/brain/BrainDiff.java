package org.virgil.akiasync.mixin.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

/**
 * Brain状态差异记录器（方案2落地用）
 * 捕获异步CPU计算结果，用于主线程写回
 * 
 * 最小实现：纯原始类型，避免任何计算
 * - topPoi（BlockPos）→ WALK_TARGET
 * - likedPlayer（Object）→ LIKED_PLAYER
 * 
 * @author Virgil
 */
public final class BrainDiff {
    
    // 纯原始类型字段
    private BlockPos topPoi;
    private Object likedPlayer;
    
    // 统计信息
    private int changeCount;
    
    public BrainDiff() {
        this.changeCount = 0;
    }
    
    /**
     * 设置最佳POI
     */
    public void setTopPoi(BlockPos pos) {
        this.topPoi = pos;
        this.changeCount++;
    }
    
    /**
     * 设置喜欢的玩家
     */
    public void setLikedPlayer(Object player) {
        this.likedPlayer = player;
        this.changeCount++;
    }
    
    
    /**
     * 将差异应用到目标Brain（主线程执行）
     * 最小实现模板：只写可控的3个字段，其余一律无视
     * 
     * 白名单：topPoi / likedPlayer / walkTarget（已包装成WalkTarget）
     * 
     * @param brain 目标Brain对象
     */
    @SuppressWarnings("unchecked")
    public <E extends LivingEntity> void applyTo(Brain<E> brain) {
        // 1. walkTarget 已包装成 WalkTarget，不含错类型
        if (topPoi != null) {
            WalkTarget walkTarget = new WalkTarget(topPoi, 1.0f, 1);
            brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
        }
        
        // 2. likedPlayer
        if (likedPlayer != null) {
            brain.setMemory(MemoryModuleType.LIKED_PLAYER, (java.util.UUID) likedPlayer);
        }
        
        // ⚠️ 删除第103-119行遗留代码，防止ExpirableValue污染
        // 只写这三项，其余一律无视
    }
    
    /**
     * 检查是否有变更
     */
    public boolean hasChanges() {
        return changeCount > 0;
    }
    
    /**
     * 获取变更数量
     */
    public int getChangeCount() {
        return changeCount;
    }
    
    @Override
    public String toString() {
        return String.format("BrainDiff[topPoi=%s, likedPlayer=%s, changes=%d]",
                topPoi != null ? "set" : "null",
                likedPlayer != null ? "set" : "null",
                changeCount);
    }
}

