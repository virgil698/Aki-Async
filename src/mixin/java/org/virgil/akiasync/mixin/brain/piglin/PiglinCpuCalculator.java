package org.virgil.akiasync.mixin.brain.piglin;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
public final class PiglinCpuCalculator {
    public static PiglinDiff runCpuOnly(
            net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin,
            ServerLevel level,
            PiglinSnapshot snapshot
    ) {
        try {
            
            List<PiglinSnapshot.PlayerGoldInfo> holdingGoldPlayers = new java.util.ArrayList<>();
            BlockPos piglinPos = piglin.blockPosition();
            
            for (PiglinSnapshot.PlayerGoldInfo playerInfo : snapshot.getNearbyPlayers()) {
                if (playerInfo.holdingGold()) {
                    holdingGoldPlayers.add(playerInfo);
                }
            }
            
            if (holdingGoldPlayers.size() > 1) {
                holdingGoldPlayers.sort(Comparator.comparingDouble((PiglinSnapshot.PlayerGoldInfo playerInfo) ->
                    scoreBarterTarget(playerInfo.pos(), snapshot.getInventoryItems(), piglinPos)
                ).reversed());
            }
            
            Vec3 avoidVec = Vec3.ZERO;
            List<BlockPos> threats = snapshot.getNearbyThreats();
            
            if (!threats.isEmpty()) {
                for (BlockPos threat : threats) {
                    int dx = piglinPos.getX() - threat.getX();
                    int dy = piglinPos.getY() - threat.getY();
                    int dz = piglinPos.getZ() - threat.getZ();
                    int distSqr = dx * dx + dy * dy + dz * dz;
                    
                    if (distSqr < 144) { 
                        double dist = Math.sqrt(distSqr);
                        double weight = 1.0 / (dist + 0.1);
                        avoidVec = avoidVec.add(dx * weight, dy * weight, dz * weight);
                    }
                }
            }
            
            int newTimer = !threats.isEmpty() ? 1 : 0;
            PiglinDiff diff = new PiglinDiff();
            
            if (!holdingGoldPlayers.isEmpty()) {
                PiglinSnapshot.PlayerGoldInfo topPlayer = holdingGoldPlayers.get(0);
                diff.setBarterTarget(topPlayer.playerId());
                diff.setLookTarget(topPlayer.playerId(), topPlayer.pos());
            }
            
            if (avoidVec.lengthSqr() > 0.01) { 
                BlockPos avoidPos = piglinPos.offset(
                    (int) (avoidVec.x * 8),
                    (int) (avoidVec.y * 8),
                    (int) (avoidVec.z * 8)
                );
                diff.setWalkTarget(avoidPos);
            }
            
            diff.setHuntedTimer(newTimer);
            return diff;
        } catch (Exception e) {
            return new PiglinDiff();
        }
    }
    private static double scoreBarterTarget(BlockPos playerPos, ItemStack[] inventory, BlockPos piglinPos) {
        double dist = Math.sqrt(
            Math.pow(playerPos.getX() - piglinPos.getX(), 2) +
            Math.pow(playerPos.getY() - piglinPos.getY(), 2) +
            Math.pow(playerPos.getZ() - piglinPos.getZ(), 2)
        );
        double inventoryValue = 0.0;
        for (ItemStack item : inventory) {
            if (item != null && !item.isEmpty()) {
                inventoryValue += item.getCount();
            }
        }
        return (1000.0 / (dist + 1.0)) + inventoryValue * 0.1;
    }
}
