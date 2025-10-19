package org.virgil.akiasync.mixin.brain.evoker;

import java.util.Comparator;
import java.util.UUID;

import net.minecraft.world.entity.monster.Evoker;

/**
 * Evoker CPU calculator.
 * @author Virgil
 */
public final class EvokerCpuCalculator {
    public static EvokerDiff runCpuOnly(Evoker evoker, EvokerSnapshot snap) {
        EvokerDiff diff = new EvokerDiff();
        if (snap.spellCd() <= 0 && snap.emptyBlocks() > 3 && !snap.players().isEmpty()) {
            UUID target = snap.players().stream()
                .min(Comparator.comparingDouble(p -> p.pos().distSqr(evoker.blockPosition())))
                .map(EvokerSnapshot.PlayerInfo::id)
                .orElse(null);
            
            diff.setEvokerTarget(target);
        }
        
        return diff;
    }
}

