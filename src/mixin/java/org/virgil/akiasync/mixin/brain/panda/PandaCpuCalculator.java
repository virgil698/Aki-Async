package org.virgil.akiasync.mixin.brain.panda;

import java.util.UUID;


public final class PandaCpuCalculator {
    
    
    public static PandaDiff runCpuOnly(net.minecraft.world.entity.animal.Animal panda, 
                                      PandaSnapshot snap) {
        PandaDiff diff = new PandaDiff();
        
        try {
            
            if (shouldEat(snap)) {
                diff.setShouldEat(true);
            }
            
            
            if (shouldRoll(snap)) {
                diff.setShouldRoll(true);
            }
            
            
            if (shouldSitDown(snap)) {
                diff.setShouldSitDown(true);
            }
            
            
            UUID targetItem = findTargetItem(snap);
            if (targetItem != null) {
                diff.setShouldPickupItem(true, targetItem);
            }
            
        } catch (Exception e) {
            
        }
        
        return diff;
    }
    
    
    private static boolean shouldEat(PandaSnapshot snap) {
        
        if (!snap.hasFood()) {
            return false;
        }
        
        
        if (snap.isEating()) {
            return true;
        }
        
        
        if (snap.health() < snap.maxHealth()) {
            return true;
        }
        
        
        if ("PLAYFUL".equals(snap.personality()) || "LAZY".equals(snap.personality())) {
            return true;
        }
        
        return false;
    }
    
    
    private static boolean shouldRoll(PandaSnapshot snap) {
        
        if (snap.isBaby()) {
            return true;
        }
        
        
        if ("PLAYFUL".equals(snap.personality())) {
            return true;
        }
        
        
        if (!snap.onGround()) {
            return false;
        }
        
        return false;
    }
    
    
    private static boolean shouldSitDown(PandaSnapshot snap) {
        
        if ("LAZY".equals(snap.personality())) {
            return true;
        }
        
        
        if ("WORRIED".equals(snap.personality()) && snap.nearbyEntities().isEmpty()) {
            return true;
        }
        
        
        if (!snap.onGround()) {
            return false;
        }
        
        return false;
    }
    
    
    private static UUID findTargetItem(PandaSnapshot snap) {
        
        if (snap.hasFood()) {
            return null;
        }
        
        
        UUID bestItem = null;
        double bestDistance = Double.MAX_VALUE;
        
        for (PandaSnapshot.ItemInfo item : snap.nearbyItems()) {
            if ("BAMBOO".equals(item.itemType()) && item.distanceSq() < bestDistance) {
                bestItem = item.id();
                bestDistance = item.distanceSq();
            }
        }
        
        return bestItem;
    }
}

