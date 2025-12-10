package org.virgil.akiasync.mixin.brain.wanderingtrader;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 流浪商人CPU计算器
 * 
 * 在异步线程中分析流浪商人的环境并生成决策
 * 
 * 分析内容：
 * - 寻找最近的玩家（潜在顾客）
 * - 寻找最优POI（村庄聚集点）
 * - 评估是否应该消失
 * 
 * @author AkiAsync
 */
public final class WanderingTraderCpuCalculator {
    
    /**
     * 异步计算流浪商人的决策
     * 
     * @param snapshot 快照
     * @return 差异对象
     */
    public static WanderingTraderDiff compute(WanderingTraderSnapshot snapshot) {
        
        UUID closestPlayer = findClosestPlayer(snapshot);
        
        BlockPos bestPoi = findBestPoi(snapshot);
        
        boolean shouldWander = evaluateShouldWander(snapshot);
        
        return new WanderingTraderDiff(closestPlayer, bestPoi, shouldWander);
    }
    
    /**
     * 寻找最近的玩家
     */
    private static UUID findClosestPlayer(WanderingTraderSnapshot snapshot) {
        List<WanderingTraderSnapshot.PlayerInfo> players = snapshot.getNearbyPlayers();
        
        if (players.isEmpty()) {
            return null;
        }
        
        if (snapshot.hasCustomer()) {
            return null; 
        }
        
        WanderingTraderSnapshot.PlayerInfo closest = null;
        double minDistSq = Double.MAX_VALUE;
        
        for (WanderingTraderSnapshot.PlayerInfo player : players) {
            if (player.getDistanceSq() < minDistSq) {
                minDistSq = player.getDistanceSq();
                closest = player;
            }
        }
        
        return closest != null ? closest.getPlayerId() : null;
    }
    
    /**
     * 寻找最优POI（村庄聚集点）
     */
    private static BlockPos findBestPoi(WanderingTraderSnapshot snapshot) {
        Map<BlockPos, PoiRecord> pois = snapshot.getNearbyPOIs();
        
        if (pois.isEmpty()) {
            return null;
        }
        
        BlockPos traderPos = snapshot.getPosition();
        BlockPos bestPoi = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (Map.Entry<BlockPos, PoiRecord> entry : pois.entrySet()) {
            BlockPos poiPos = entry.getKey();
            PoiRecord record = entry.getValue();
            
            double distSq = traderPos.distSqr(poiPos);
            
            double score = 0;
            
            if (distSq < 100) { 
                score = distSq / 100.0; 
            } else if (distSq < 1600) { 
                score = 1.0; 
            } else { 
                score = 1600.0 / distSq; 
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestPoi = poiPos;
            }
        }
        
        return bestPoi;
    }
    
    /**
     * 评估是否应该游荡
     */
    private static boolean evaluateShouldWander(WanderingTraderSnapshot snapshot) {
        
        if (snapshot.hasCustomer()) {
            return false;
        }
        
        if (!snapshot.getNearbyPlayers().isEmpty()) {
            return false;
        }
        
        if (snapshot.getDespawnDelay() < 1200) { 
            return false;
        }
        
        if (snapshot.getNearbyPOIs().isEmpty()) {
            return true;
        }
        
        return snapshot.getGameTime() % 200 == 0; 
    }
}
