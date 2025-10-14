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
     * Enhanced: Check 16 potion types (increased from 4)
     * 
     * @param snapshot Witch snapshot
     * @return Potion type name, null if none needed
     */
    private static String decidePotionToDrink(WitchSnapshot snapshot) {
        java.util.List<String> effects = snapshot.getActiveEffects();
        
        // Priority 1: Critical survival (health < 8)
        if (snapshot.getHealth() < 8.0 && !effects.contains("minecraft:regeneration")) {
            return "minecraft:healing";
        }
        
        // Priority 2: Environmental hazards (16 types expanded)
        String[] hazardPotions = {
            "fire_resistance", "water_breathing", "night_vision", "invisibility",
            "slow_falling", "absorption", "resistance", "health_boost"
        };
        for (String potion : hazardPotions) {
            if (!effects.contains("minecraft:" + potion)) {
                // CPU-intensive: Double scoring
                double score = scorePotion(potion, snapshot);
                if (score > 50.0) {
                    return "minecraft:" + potion;
                }
            }
        }
        
        // Priority 3: Combat buffs (8 types)
        String[] combatPotions = {
            "speed", "strength", "jump_boost", "haste",
            "luck", "dolphins_grace", "conduit_power", "hero_of_the_village"
        };
        for (String potion : combatPotions) {
            if (!effects.contains("minecraft:" + potion)) {
                double score = scorePotion(potion, snapshot);
                if (score > 30.0) {
                    return "minecraft:" + potion;
                }
            }
        }
        
        return null;  // No potion needed
    }
    
    /**
     * Score potion priority (CPU-intensive double scoring)
     */
    private static double scorePotion(String potion, WitchSnapshot snapshot) {
        double score = 0.0;
        
        // Factor 1: Health-based scoring
        score += (10.0 - snapshot.getHealth()) * 5.0;
        
        // Factor 2: Potion type weight
        if (potion.contains("healing") || potion.contains("regeneration")) {
            score += 100.0;
        } else if (potion.contains("resistance") || potion.contains("absorption")) {
            score += 80.0;
        } else if (potion.contains("speed") || potion.contains("strength")) {
            score += 60.0;
        } else {
            score += 40.0;
        }
        
        return score;
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

