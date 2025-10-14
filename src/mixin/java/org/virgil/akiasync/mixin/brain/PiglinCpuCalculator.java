package org.virgil.akiasync.mixin.brain;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Piglin CPU-intensive calculator
 * 
 * Async tasks (0.5-1 ms):
 * 1. Item barter scoring and sorting (BarteringRecipe matching)
 * 2. Fear vector synthesis (AABB scan + scoring)
 * 3. Produce PiglinDiff
 * 
 * @author Virgil
 */
public final class PiglinCpuCalculator {
    
    /**
     * Read-only CPU computation (async thread invocation)
     * 
     * @param piglin Piglin/Brute entity (AbstractPiglin)
     * @param level ServerLevel
     * @param snapshot Piglin snapshot
     * @return PiglinDiff
     */
    public static PiglinDiff runCpuOnly(
            net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin,
            ServerLevel level,
            PiglinSnapshot snapshot
    ) {
        try {
            // 1. Item barter sorting (iterate inventory + BarteringRecipe matching)
            List<PiglinSnapshot.PlayerGoldInfo> holdingGoldPlayers = snapshot.getNearbyPlayers().stream()
                .filter(PiglinSnapshot.PlayerGoldInfo::holdingGold)
                .sorted(Comparator.comparingDouble((PiglinSnapshot.PlayerGoldInfo playerInfo) -> 
                    scoreBarterTarget(playerInfo.pos(), snapshot.getInventoryItems(), piglin.blockPosition())
                ).reversed())
                .collect(Collectors.toList());
            
            // 2. Fear vector synthesis (AABB scan + fear scoring)
            Vec3 avoidVec = Vec3.ZERO;
            BlockPos piglinPos = piglin.blockPosition();
            
            for (BlockPos threat : snapshot.getNearbyThreats()) {
                // Calculate flee vector
                double dx = piglinPos.getX() - threat.getX();
                double dy = piglinPos.getY() - threat.getY();
                double dz = piglinPos.getZ() - threat.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                if (dist < 12.0) {
                    // Closer distance = higher weight
                    double weight = 1.0 / (dist + 0.1);
                    avoidVec = avoidVec.add(dx * weight, dy * weight, dz * weight);
                }
            }
            
            // 3. Hunted flag (simplified: set if threats exist)
            int newTimer = !snapshot.getNearbyThreats().isEmpty() ? 1 : 0;
            
            // 4. Produce Diff
            PiglinDiff diff = new PiglinDiff();
            
            // Set barter target (highest score) + look target (virtual reference: UUID)
            if (!holdingGoldPlayers.isEmpty()) {
                PiglinSnapshot.PlayerGoldInfo topPlayer = holdingGoldPlayers.get(0);
                diff.setBarterTarget(topPlayer.playerId());
                diff.setLookTarget(topPlayer.playerId(), topPlayer.pos());
            }
            
            // Set walk target (flee vector)
            if (avoidVec.length() > 0.1) {
                BlockPos avoidPos = piglinPos.offset(
                    (int) avoidVec.x * 8,
                    (int) avoidVec.y * 8,
                    (int) avoidVec.z * 8
                );
                diff.setWalkTarget(avoidPos);
            }
            
            // Set hunted timer
            diff.setHuntedTimer(newTimer);
            
            return diff;
            
        } catch (Exception e) {
            return new PiglinDiff();
        }
    }
    
    /**
     * Barter target scoring (CPU-intensive)
     * 
     * @param playerPos Player position
     * @param inventory Piglin inventory items
     * @param piglinPos Piglin position
     * @return Score (higher is better)
     */
    private static double scoreBarterTarget(BlockPos playerPos, ItemStack[] inventory, BlockPos piglinPos) {
        // ① Distance scoring
        double dist = Math.sqrt(
            Math.pow(playerPos.getX() - piglinPos.getX(), 2) +
            Math.pow(playerPos.getY() - piglinPos.getY(), 2) +
            Math.pow(playerPos.getZ() - piglinPos.getZ(), 2)
        );
        
        // ② Inventory value scoring (iterate backpack)
        double inventoryValue = 0.0;
        for (ItemStack item : inventory) {
            if (item != null && !item.isEmpty()) {
                inventoryValue += item.getCount();  // Simplified: count as value
            }
        }
        
        // ③ Composite scoring
        return (1000.0 / (dist + 1.0)) + inventoryValue * 0.1;
    }
}

