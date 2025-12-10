package org.virgil.akiasync.mixin.brain.allay;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * 悦灵CPU计算器
 * 
 * 在异步线程中分析悦灵的环境并生成决策
 * 
 * 分析内容：
 * - 选择最近的物品进行收集
 * - 选择跟随的玩家
 * - 判断是否应该前往音符盒
 * 
 * @author AkiAsync
 */
public final class AllayCpuCalculator {
    
    /**
     * 异步计算悦灵的决策
     * 
     * @param snapshot 快照
     * @return 差异对象
     */
    public static AllayDiff compute(AllaySnapshot snapshot) {
        
        if (snapshot.isDancing()) {
            return new AllayDiff(null, null, null);
        }
        
        if (snapshot.getNearestNoteBlock() != null) {
            return new AllayDiff(null, null, snapshot.getNearestNoteBlock());
        }
        
        if (!snapshot.getHeldItem().isEmpty() && !snapshot.getNearbyItems().isEmpty()) {
            UUID nearestItem = findNearestItem(snapshot);
            return new AllayDiff(nearestItem, null, null);
        }
        
        UUID nearestPlayer = findNearestPlayer(snapshot);
        return new AllayDiff(null, nearestPlayer, null);
    }
    
    /**
     * 查找最近的物品
     */
    private static UUID findNearestItem(AllaySnapshot snapshot) {
        List<AllaySnapshot.ItemEntityInfo> items = snapshot.getNearbyItems();
        
        if (items.isEmpty()) {
            return null;
        }
        
        AllaySnapshot.ItemEntityInfo nearest = null;
        double minDist = Double.MAX_VALUE;
        
        for (AllaySnapshot.ItemEntityInfo item : items) {
            if (item.getDistanceSq() < minDist) {
                minDist = item.getDistanceSq();
                nearest = item;
            }
        }
        
        return nearest != null ? nearest.getItemEntityId() : null;
    }
    
    /**
     * 查找最近的玩家
     */
    private static UUID findNearestPlayer(AllaySnapshot snapshot) {
        List<AllaySnapshot.PlayerInfo> players = snapshot.getNearbyPlayers();
        
        if (players.isEmpty()) {
            return null;
        }
        
        AllaySnapshot.PlayerInfo nearest = null;
        double minDist = Double.MAX_VALUE;
        
        for (AllaySnapshot.PlayerInfo player : players) {
            if (player.getDistanceSq() < minDist) {
                minDist = player.getDistanceSq();
                nearest = player;
            }
        }
        
        return nearest != null ? nearest.getPlayerId() : null;
    }
}
