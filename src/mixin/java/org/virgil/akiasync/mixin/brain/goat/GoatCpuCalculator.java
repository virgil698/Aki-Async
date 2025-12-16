package org.virgil.akiasync.mixin.brain.goat;

import java.util.UUID;

public final class GoatCpuCalculator {
    
    public static GoatDiff runCpuOnly(net.minecraft.world.entity.animal.Animal goat, 
                                     GoatSnapshot snap) {
        GoatDiff diff = new GoatDiff();
        
        try {
            
            UUID targetEntity = findRamTarget(snap);
            if (targetEntity != null) {
                diff.setShouldRam(true, targetEntity);
            }
            
            if (shouldHighJump(snap)) {
                diff.setShouldHighJump(true);
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "GoatCpuCalculator", "runCpuOnly", e);
        }
        
        return diff;
    }
    
    private static UUID findRamTarget(GoatSnapshot snap) {
        
        if (snap.isBaby()) {
            return null;
        }
        
        if (!snap.canRam()) {
            return null;
        }
        
        double ramDistance = snap.isScreaming() ? 256.0 : 64.0; 
        
        UUID bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        
        for (GoatSnapshot.EntityInfo entity : snap.nearbyEntities()) {
            
            if (entity.distanceSq() < ramDistance && entity.distanceSq() < bestDistance) {
                bestTarget = entity.id();
                bestDistance = entity.distanceSq();
            }
        }
        
        return bestTarget;
    }
    
    private static boolean shouldHighJump(GoatSnapshot snap) {
        
        if (snap.isBaby()) {
            return false;
        }
        
        if (!snap.canRam()) {
            return false;
        }
        
        for (GoatSnapshot.EntityInfo entity : snap.nearbyEntities()) {
            
            if (entity.distanceSq() > 16.0 && entity.distanceSq() < 100.0) {
                return true;
            }
        }
        
        return false;
    }
}
