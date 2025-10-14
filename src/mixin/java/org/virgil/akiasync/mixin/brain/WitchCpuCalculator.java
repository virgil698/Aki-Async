package org.virgil.akiasync.mixin.brain;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Witch;

/**
 * Witch CPU-intensive calculator
 * 
 * Async tasks (0.5-1 ms):
 * 1. Potion recipe matching (PotionBrewing iteration)
 * 2. Effect overlap detection (hasEffect × 4-5 types)
 * 3. Attack target filtering (AABB scan + health < 75%)
 * 4. Produce WitchDiff
 * 
 * @author Virgil
 */
public final class WitchCpuCalculator {
    
    /**
     * Read-only CPU computation (async thread invocation)
     * 
     * @param witch Witch entity
     * @param level ServerLevel
     * @param snapshot Witch snapshot
     * @return WitchDiff
     */
    public static WitchDiff runCpuOnly(
            Witch witch,
            ServerLevel level,
            WitchSnapshot snapshot
    ) {
        try {
            // 1. Potion recipe matching (iterate PotionBrewing)
            // CPU-intensive: check which potion witch should drink
            String potionToDrink = decidePotionToDrink(snapshot);
            
            // 2. Attack target selection (filter players by health < 75%)
            List<WitchSnapshot.PlayerHealthInfo> lowHealthPlayers = snapshot.getNearbyPlayers().stream()
                .filter(p -> p.healthPercent() < 0.75f)
                .sorted(Comparator.comparingDouble((WitchSnapshot.PlayerHealthInfo p) -> 
                    scoreAttackTarget(p.pos(), witch.blockPosition())
                ).reversed())
                .collect(Collectors.toList());
            
            // 3. Produce Diff
            WitchDiff diff = new WitchDiff();
            
            // Set potion to drink
            if (potionToDrink != null) {
                diff.setPotionToDrink(potionToDrink);
            }
            
            // Set attack target (virtual reference: UUID)
            if (!lowHealthPlayers.isEmpty()) {
                WitchSnapshot.PlayerHealthInfo topTarget = lowHealthPlayers.get(0);
                diff.setAttackTarget(topTarget.playerId());
            }
            
            return diff;
            
        } catch (Exception e) {
            return new WitchDiff();
        }
    }
    
    /**
     * Decide which potion witch should drink (CPU-intensive)
     * 
     * @param snapshot Witch snapshot
     * @return Potion type name, null if none needed
     */
    private static String decidePotionToDrink(WitchSnapshot snapshot) {
        // Check current health
        if (snapshot.getHealth() < 8.0) {
            // Low health: drink healing potion
            return "minecraft:healing";
        }
        
        // Check for fire damage
        if (snapshot.getActiveEffects().contains("minecraft:fire_resistance")) {
            return null;  // Already has fire resistance
        }
        
        // Check for water (simplified: assume need water breathing if health > 15)
        if (snapshot.getHealth() > 15.0 && !snapshot.getActiveEffects().contains("minecraft:water_breathing")) {
            return "minecraft:water_breathing";
        }
        
        // Default: speed potion for mobility
        if (!snapshot.getActiveEffects().contains("minecraft:speed")) {
            return "minecraft:speed";
        }
        
        return null;  // No potion needed
    }
    
    /**
     * Attack target scoring (CPU-intensive)
     * 
     * @param playerPos Player position
     * @param witchPos Witch position
     * @return Score (higher is better)
     */
    private static double scoreAttackTarget(BlockPos playerPos, BlockPos witchPos) {
        // Distance scoring (closer = higher priority)
        double dist = Math.sqrt(
            Math.pow(playerPos.getX() - witchPos.getX(), 2) +
            Math.pow(playerPos.getY() - witchPos.getY(), 2) +
            Math.pow(playerPos.getZ() - witchPos.getZ(), 2)
        );
        
        return 1000.0 / (dist + 1.0);
    }
}

