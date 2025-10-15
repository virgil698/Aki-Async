package org.virgil.akiasync.mixin.brain.evoker;

import java.util.Comparator;
import java.util.UUID;

import net.minecraft.world.entity.monster.Evoker;

/**
 * Evoker CPU calculator (spell scoring + empty blocks)
 * 
 * @author Virgil
 */
public final class EvokerCpuCalculator {
    
    public static EvokerDiff runCpuOnly(Evoker evoker, EvokerSnapshot snap) {
        EvokerDiff diff = new EvokerDiff();
        
        // Spell CD check + empty blocks (Vex summon needs 3+ empty)
        if (snap.spellCd() <= 0 && snap.emptyBlocks() > 3 && !snap.players().isEmpty()) {
            // Find nearest player
            UUID target = snap.players().stream()
                .min(Comparator.comparingDouble(p -> p.pos().distSqr(evoker.blockPosition())))
                .map(EvokerSnapshot.PlayerInfo::id)
                .orElse(null);
            
            diff.setEvokerTarget(target);
        }
        
        return diff;
    }
}

