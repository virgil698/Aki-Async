package org.virgil.akiasync.mixin.brain.hoglin;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * 疣猪兽CPU计算器
 * 
 * 在异步线程中分析疣猪兽的环境并生成决策
 * 
 * 分析内容：
 * - 选择攻击目标（最近的玩家）
 * - 判断是否应该逃跑（附近有诡异菌）
 * - 评估逃跑方向
 * 
 * @author AkiAsync
 */
public final class HoglinCpuCalculator {
    
    /**
     * 异步计算疣猪兽的决策
     * 
     * @param snapshot 快照
     * @return 差异对象
     */
    public static HoglinDiff compute(HoglinSnapshot snapshot) {
        
        if (snapshot.isBaby()) {
            return new HoglinDiff(null, false, null);
        }
        
        if (snapshot.isHuntingCooldown()) {
            return new HoglinDiff(null, false, null);
        }
        
        if (!snapshot.getNearbyWarpedFungus().isEmpty()) {
            BlockPos fleeTarget = calculateFleeTarget(snapshot);
            return new HoglinDiff(null, true, fleeTarget);
        }
        
        UUID attackTarget = selectAttackTarget(snapshot);
        
        return new HoglinDiff(attackTarget, false, null);
    }
    
    /**
     * 选择攻击目标（最近的玩家）
     */
    private static UUID selectAttackTarget(HoglinSnapshot snapshot) {
        List<HoglinSnapshot.PlayerInfo> players = snapshot.getNearbyPlayers();
        
        if (players.isEmpty()) {
            return null;
        }
        
        HoglinSnapshot.PlayerInfo closest = null;
        double minDist = Double.MAX_VALUE;
        
        for (HoglinSnapshot.PlayerInfo player : players) {
            if (player.getDistanceSq() < minDist) {
                minDist = player.getDistanceSq();
                closest = player;
            }
        }
        
        if (closest != null && closest.getDistanceSq() < 256) {
            return closest.getPlayerId();
        }
        
        return null;
    }
    
    /**
     * 计算逃跑目标（远离诡异菌）
     */
    private static BlockPos calculateFleeTarget(HoglinSnapshot snapshot) {
        List<BlockPos> fungus = snapshot.getNearbyWarpedFungus();
        
        if (fungus.isEmpty()) {
            return null;
        }
        
        BlockPos hoglinPos = snapshot.getPosition();
        
        double avgX = 0, avgY = 0, avgZ = 0;
        for (BlockPos pos : fungus) {
            avgX += pos.getX();
            avgY += pos.getY();
            avgZ += pos.getZ();
        }
        avgX /= fungus.size();
        avgY /= fungus.size();
        avgZ /= fungus.size();
        
        double dx = hoglinPos.getX() - avgX;
        double dy = hoglinPos.getY() - avgY;
        double dz = hoglinPos.getZ() - avgZ;
        
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001) {
            return hoglinPos.offset(8, 0, 0); 
        }
        
        dx = (dx / length) * 16; 
        dy = (dy / length) * 4;  
        dz = (dz / length) * 16;
        
        return hoglinPos.offset((int) dx, (int) dy, (int) dz);
    }
}
