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
            List<PiglinSnapshot.PlayerGoldInfo> holdingGoldPlayers = snapshot.getNearbyPlayers().stream()
                .filter(PiglinSnapshot.PlayerGoldInfo::holdingGold)
                .sorted(Comparator.comparingDouble((PiglinSnapshot.PlayerGoldInfo playerInfo) ->
                    scoreBarterTarget(playerInfo.pos(), snapshot.getInventoryItems(), piglin.blockPosition())
                ).reversed())
                .collect(Collectors.toList());
            Vec3 avoidVec = Vec3.ZERO;
            BlockPos piglinPos = piglin.blockPosition();
            for (BlockPos threat : snapshot.getNearbyThreats()) {
                double dx = piglinPos.getX() - threat.getX();
                double dy = piglinPos.getY() - threat.getY();
                double dz = piglinPos.getZ() - threat.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist < 12.0) {
                    double weight = 1.0 / (dist + 0.1);
                    avoidVec = avoidVec.add(dx * weight, dy * weight, dz * weight);
                }
            }
            int newTimer = !snapshot.getNearbyThreats().isEmpty() ? 1 : 0;
            PiglinDiff diff = new PiglinDiff();
            if (!holdingGoldPlayers.isEmpty()) {
                PiglinSnapshot.PlayerGoldInfo topPlayer = holdingGoldPlayers.get(0);
                diff.setBarterTarget(topPlayer.playerId());
                diff.setLookTarget(topPlayer.playerId(), topPlayer.pos());
            }
            if (avoidVec.length() > 0.1) {
                BlockPos avoidPos = piglinPos.offset(
                    (int) avoidVec.x * 8,
                    (int) avoidVec.y * 8,
                    (int) avoidVec.z * 8
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