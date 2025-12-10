package org.virgil.akiasync.mixin.brain.warden;

import java.util.List;
import java.util.UUID;

/**
 * 监守者CPU计算器
 * 
 * 在异步线程中分析监守者的环境并生成决策
 * 
 * 分析内容：
 * - 选择最优攻击目标（基于距离和威胁度）
 * - 评估是否应该使用音波攻击
 * - 判断是否应该挖掘逃跑
 * 
 * @author AkiAsync
 */
public final class WardenCpuCalculator {
    
    /**
     * 异步计算监守者的决策
     * 
     * @param snapshot 快照
     * @return 差异对象
     */
    public static WardenDiff compute(WardenSnapshot snapshot) {
        
        if (snapshot.isDigging() || snapshot.isEmerging()) {
            return new WardenDiff(null, false, false);
        }
        
        UUID bestTarget = selectBestTarget(snapshot);
        
        boolean shouldUseSonicBoom = evaluateSonicBoom(snapshot, bestTarget);
        
        boolean shouldDig = evaluateDigging(snapshot);
        
        return new WardenDiff(bestTarget, shouldUseSonicBoom, shouldDig);
    }
    
    /**
     * 选择最优攻击目标
     * 
     * 优先级：
     * 1. 愤怒值高的目标
     * 2. 距离近的玩家
     * 3. 距离近的其他生物
     */
    private static UUID selectBestTarget(WardenSnapshot snapshot) {
        List<WardenSnapshot.PlayerInfo> players = snapshot.getNearbyPlayers();
        List<WardenSnapshot.EntityInfo> entities = snapshot.getNearbyEntities();
        
        if (snapshot.getAngerLevel() > 80) {
            
            if (!players.isEmpty()) {
                WardenSnapshot.PlayerInfo closest = null;
                double minDist = Double.MAX_VALUE;
                
                for (WardenSnapshot.PlayerInfo player : players) {
                    
                    double dist = player.isSneaking() ? player.getDistanceSq() * 2.0 : player.getDistanceSq();
                    if (dist < minDist) {
                        minDist = dist;
                        closest = player;
                    }
                }
                
                if (closest != null) {
                    return closest.getPlayerId();
                }
            }
            
            if (!entities.isEmpty()) {
                WardenSnapshot.EntityInfo closest = null;
                double minDist = Double.MAX_VALUE;
                
                for (WardenSnapshot.EntityInfo entity : entities) {
                    if (entity.getDistanceSq() < minDist) {
                        minDist = entity.getDistanceSq();
                        closest = entity;
                    }
                }
                
                if (closest != null) {
                    return closest.getEntityId();
                }
            }
        }
        
        if (snapshot.getAngerLevel() > 40) {
            for (WardenSnapshot.PlayerInfo player : players) {
                if (player.getDistanceSq() < 64 && !player.isSneaking()) { 
                    return player.getPlayerId();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 评估是否应该使用音波攻击
     * 
     * 条件：
     * - 有目标
     * - 目标距离适中（5-15格）
     * - 攻击冷却完成
     */
    private static boolean evaluateSonicBoom(WardenSnapshot snapshot, UUID targetId) {
        if (targetId == null) return false;
        if (snapshot.getAttackCooldown() > 0) return false;
        
        for (WardenSnapshot.PlayerInfo player : snapshot.getNearbyPlayers()) {
            if (player.getPlayerId().equals(targetId)) {
                double dist = Math.sqrt(player.getDistanceSq());
                return dist >= 5.0 && dist <= 15.0;
            }
        }
        
        for (WardenSnapshot.EntityInfo entity : snapshot.getNearbyEntities()) {
            if (entity.getEntityId().equals(targetId)) {
                double dist = Math.sqrt(entity.getDistanceSq());
                return dist >= 5.0 && dist <= 15.0;
            }
        }
        
        return false;
    }
    
    /**
     * 评估是否应该挖掘逃跑
     * 
     * 条件：
     * - 生命值低于50%
     * - 附近有多个威胁
     * - 愤怒值不是很高
     */
    private static boolean evaluateDigging(WardenSnapshot snapshot) {
        
        if (snapshot.getHealth() > snapshot.getHealth() * 0.5) {
            return false;
        }
        
        int threatCount = snapshot.getNearbyPlayers().size() + snapshot.getNearbyEntities().size();
        if (threatCount < 3) {
            return false;
        }
        
        if (snapshot.getAngerLevel() > 90) {
            return false; 
        }
        
        return true;
    }
}
