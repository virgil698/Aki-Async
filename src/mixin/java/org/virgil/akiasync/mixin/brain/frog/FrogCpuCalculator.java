package org.virgil.akiasync.mixin.brain.frog;

import java.util.UUID;


public final class FrogCpuCalculator {
    
    
    public static FrogDiff runCpuOnly(net.minecraft.world.entity.animal.Animal frog, 
                                     FrogSnapshot snap) {
        FrogDiff diff = new FrogDiff();
        
        try {
            
            UUID targetSlime = findTargetSlime(snap);
            if (targetSlime != null) {
                diff.setShouldEatSlime(true, targetSlime);
            }
            
            
            if (shouldLongJump(snap)) {
                diff.setShouldLongJump(true);
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "FrogCpuCalculator", "runCpuOnly", e);
        }
        
        return diff;
    }
    
    
    private static UUID findTargetSlime(FrogSnapshot snap) {
        
        if (snap.isBaby()) {
            return null;
        }
        
        
        UUID bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        
        for (FrogSnapshot.SlimeInfo slime : snap.nearbySlimes()) {
            
            if (slime.size() <= 1.0 && slime.distanceSq() < 16.0) { 
                if (slime.distanceSq() < bestDistance) {
                    bestTarget = slime.id();
                    bestDistance = slime.distanceSq();
                }
            }
        }
        
        return bestTarget;
    }
    
    
    private static boolean shouldLongJump(FrogSnapshot snap) {
        
        if (snap.isBaby()) {
            return false;
        }
        
        
        if (!snap.canJump()) {
            return false;
        }
        
        
        if (snap.isInWater()) {
            return false;
        }
        
        
        if (!snap.nearbySlimes().isEmpty()) {
            for (FrogSnapshot.SlimeInfo slime : snap.nearbySlimes()) {
                
                if (slime.distanceSq() > 16.0 && slime.distanceSq() < 100.0) {
                    return true;
                }
            }
        }
        
        return false;
    }
}

