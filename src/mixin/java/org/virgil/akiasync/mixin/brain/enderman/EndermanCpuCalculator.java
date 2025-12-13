package org.virgil.akiasync.mixin.brain.enderman;

import net.minecraft.world.entity.monster.EnderMan;


public final class EndermanCpuCalculator {
    
    
    public static EndermanDiff runCpuOnly(EnderMan enderman, EndermanSnapshot snap) {
        EndermanDiff diff = new EndermanDiff();
        
        try {
            
            if (shouldTeleportFromSunlight(snap)) {
                diff.setShouldTeleport(true);
                diff.setShouldClearTarget(true);
            }
            
            
            if (shouldTeleportFromWater(snap)) {
                diff.setShouldTeleport(true);
            }
            
        } catch (Exception e) {
            
        }
        
        return diff;
    }
    
    
    private static boolean shouldTeleportFromSunlight(EndermanSnapshot snap) {
        
        
        if (!snap.isBrightOutside()) {
            return false;
        }
        
        if (snap.tickCount() < snap.targetChangeTime() + 600) {
            return false;
        }
        
        if (snap.lightLevel() <= 0.5F) {
            return false;
        }
        
        if (!snap.canSeeSky()) {
            return false;
        }
        
        
        float threshold = (snap.lightLevel() - 0.4F) * 2.0F;
        
        
        return threshold > 15.0F;
    }
    
    
    private static boolean shouldTeleportFromWater(EndermanSnapshot snap) {
        
        return snap.isInWater();
    }
}
