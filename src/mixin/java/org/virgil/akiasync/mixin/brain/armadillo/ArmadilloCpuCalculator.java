package org.virgil.akiasync.mixin.brain.armadillo;

public final class ArmadilloCpuCalculator {
    
    public static ArmadilloDiff runCpuOnly(net.minecraft.world.entity.animal.Animal armadillo, 
                                          ArmadilloSnapshot snap) {
        ArmadilloDiff diff = new ArmadilloDiff();
        
        try {
            
            if (shouldStartRolling(snap)) {
                diff.setShouldStartRolling(true);
            }
            
            if (shouldStopRolling(snap)) {
                diff.setShouldStopRolling(true);
            }
            
            if (shouldFlee(snap)) {
                diff.setShouldFlee(true);
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ArmadilloCpuCalculator", "runCpuOnly", e);
        }
        
        return diff;
    }
    
    private static boolean shouldStartRolling(ArmadilloSnapshot snap) {
        
        if (snap.hasTarget() || snap.health() < snap.maxHealth()) {
            return true;
        }
        
        for (ArmadilloSnapshot.ThreatInfo threat : snap.nearbyThreats()) {
            if (threat.distanceSq() < 25.0) { 
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean shouldStopRolling(ArmadilloSnapshot snap) {
        
        if (snap.nearbyThreats().isEmpty() && 
            snap.health() >= snap.maxHealth() && 
            !snap.hasTarget()) {
            return true;
        }
        
        return false;
    }
    
    private static boolean shouldFlee(ArmadilloSnapshot snap) {
        
        if (snap.isBaby()) {
            for (ArmadilloSnapshot.ThreatInfo threat : snap.nearbyThreats()) {
                if (threat.distanceSq() < 64.0) { 
                    return true;
                }
            }
        }
        
        for (ArmadilloSnapshot.ThreatInfo threat : snap.nearbyThreats()) {
            if (threat.isPlayer() && threat.distanceSq() < 16.0) { 
                return true;
            }
        }
        
        return false;
    }
}
