package org.virgil.akiasync.mixin.brain;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 猪灵CPU密集计算器
 * 
 * 异步任务（0.5-1 ms）：
 * 1. 物品比价排序（BarteringRecipe匹配）
 * 2. 恐惧向量合成（AABB扫描+评分）
 * 3. 产出PiglinDiff
 * 
 * @author Virgil
 */
public final class PiglinCpuCalculator {
    
    /**
     * 只读CPU计算（异步线程调用）
     * 
     * @param piglin 猪灵/蛮兵实体（AbstractPiglin）
     * @param level ServerLevel
     * @param snapshot 猪灵快照
     * @return PiglinDiff
     */
    public static PiglinDiff runCpuOnly(
            net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin,
            ServerLevel level,
            PiglinSnapshot snapshot
    ) {
        try {
            // 1. 物品比价排序（遍历 getInventory() + BarteringRecipe 匹配）
            List<PiglinSnapshot.PlayerGoldInfo> holdingGoldPlayers = snapshot.getNearbyPlayers().stream()
                .filter(PiglinSnapshot.PlayerGoldInfo::holdingGold)
                .sorted(Comparator.comparingDouble((PiglinSnapshot.PlayerGoldInfo playerInfo) -> 
                    scoreBarterTarget(playerInfo.pos(), snapshot.getInventoryItems(), piglin.blockPosition())
                ).reversed())
                .collect(Collectors.toList());
            
            // 2. 恐惧向量合成（AABB扫描 + 恐惧评分）
            Vec3 avoidVec = Vec3.ZERO;
            BlockPos piglinPos = piglin.blockPosition();
            
            for (BlockPos threat : snapshot.getNearbyThreats()) {
                // 计算逃离向量
                double dx = piglinPos.getX() - threat.getX();
                double dy = piglinPos.getY() - threat.getY();
                double dz = piglinPos.getZ() - threat.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                if (dist < 12.0) {
                    // 距离越近，权重越高
                    double weight = 1.0 / (dist + 0.1);
                    avoidVec = avoidVec.add(dx * weight, dy * weight, dz * weight);
                }
            }
            
            // 3. 仇恨标记（简化：有威胁则设置）
            int newTimer = !snapshot.getNearbyThreats().isEmpty() ? 1 : 0;
            
            // 4. 产出 Diff
            PiglinDiff diff = new PiglinDiff();
            
            // 设置交易目标（最高分）+ 注视目标（虚拟引用：UUID）
            if (!holdingGoldPlayers.isEmpty()) {
                PiglinSnapshot.PlayerGoldInfo topPlayer = holdingGoldPlayers.get(0);
                diff.setBarterTarget(topPlayer.playerId());
                diff.setLookTarget(topPlayer.playerId(), topPlayer.pos());
            }
            
            // 设置行走目标（逃离向量）
            if (avoidVec.length() > 0.1) {
                BlockPos avoidPos = piglinPos.offset(
                    (int) avoidVec.x * 8,
                    (int) avoidVec.y * 8,
                    (int) avoidVec.z * 8
                );
                diff.setWalkTarget(avoidPos);
            }
            
            // 设置计时器
            diff.setHuntedTimer(newTimer);
            
            return diff;
            
        } catch (Exception e) {
            return new PiglinDiff();
        }
    }
    
    /**
     * 交易目标评分（CPU密集）
     * 
     * @param playerPos 玩家位置
     * @param inventory 猪灵物品列表
     * @param piglinPos 猪灵位置
     * @return 评分（越高越好）
     */
    private static double scoreBarterTarget(BlockPos playerPos, ItemStack[] inventory, BlockPos piglinPos) {
        // ① 距离评分
        double dist = Math.sqrt(
            Math.pow(playerPos.getX() - piglinPos.getX(), 2) +
            Math.pow(playerPos.getY() - piglinPos.getY(), 2) +
            Math.pow(playerPos.getZ() - piglinPos.getZ(), 2)
        );
        
        // ② 物品价值评分（遍历背包）
        double inventoryValue = 0.0;
        for (ItemStack item : inventory) {
            if (item != null && !item.isEmpty()) {
                inventoryValue += item.getCount();  // 简化：数量即价值
            }
        }
        
        // ③ 综合评分
        return (1000.0 / (dist + 1.0)) + inventoryValue * 0.1;
    }
}

