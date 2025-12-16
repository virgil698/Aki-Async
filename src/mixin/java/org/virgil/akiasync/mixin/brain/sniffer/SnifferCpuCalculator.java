package org.virgil.akiasync.mixin.brain.sniffer;

import net.minecraft.core.BlockPos;

public final class SnifferCpuCalculator {
    
    public static SnifferDiff runCpuOnly(net.minecraft.world.entity.animal.Animal sniffer, 
                                        SnifferSnapshot snap) {
        SnifferDiff diff = new SnifferDiff();
        
        try {
            
            if (shouldSniff(snap)) {
                diff.setShouldSniff(true);
            }
            
            BlockPos digPos = findBestDigPosition(snap);
            if (digPos != null) {
                diff.setShouldDig(true, digPos);
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "SnifferCpuCalculator", "runCpuOnly", e);
        }
        
        return diff;
    }
    
    private static boolean shouldSniff(SnifferSnapshot snap) {
        
        if (snap.isBaby()) {
            return false;
        }
        
        if (snap.isInWater()) {
            return false;
        }
        
        if (!snap.onGround()) {
            return false;
        }
        
        return !snap.nearbyDirtBlocks().isEmpty();
    }
    
    private static BlockPos findBestDigPosition(SnifferSnapshot snap) {
        
        if (snap.isBaby()) {
            return null;
        }
        
        if (!snap.onGround()) {
            return null;
        }
        
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;
        
        for (SnifferSnapshot.BlockInfo block : snap.nearbyDirtBlocks()) {
            double distance = snap.blockPosition().distSqr(block.pos());
            
            if (distance < 16.0 && distance < bestDistance) {
                bestPos = block.pos();
                bestDistance = distance;
            }
        }
        
        return bestPos;
    }
}
