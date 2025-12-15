package org.virgil.akiasync.mixin.brain.camel;


public final class CamelCpuCalculator {
    
    
    public static CamelDiff runCpuOnly(net.minecraft.world.entity.animal.Animal camel, 
                                      CamelSnapshot snap) {
        CamelDiff diff = new CamelDiff();
        
        try {
            
            if (shouldStandUp(snap)) {
                diff.setShouldStandUp(true);
            }
            
            
            if (shouldSitDown(snap)) {
                diff.setShouldSitDown(true);
            }
            
            
            if (shouldDash(snap)) {
                diff.setShouldDash(true);
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "CamelCpuCalculator", "runCpuOnly", e);
        }
        
        return diff;
    }
    
    
    private static boolean shouldStandUp(CamelSnapshot snap) {
        
        if (snap.hasPassengers()) {
            return true;
        }
        
        
        for (CamelSnapshot.PlayerInfo player : snap.nearbyPlayers()) {
            if (player.distanceSq() < 16.0) { 
                return true;
            }
        }
        
        return false;
    }
    
    
    private static boolean shouldSitDown(CamelSnapshot snap) {
        
        if (!snap.hasPassengers() && snap.nearbyPlayers().isEmpty()) {
            return true;
        }
        
        
        if (snap.isInWater()) {
            return false;
        }
        
        return false;
    }
    
    
    private static boolean shouldDash(CamelSnapshot snap) {
        
        if (snap.isBaby()) {
            return false;
        }
        
        
        if (!snap.hasPassengers()) {
            return false;
        }
        
        
        if (!snap.onGround()) {
            return false;
        }
        
        
        return snap.health() > snap.maxHealth() * 0.5;
    }
}

