package org.virgil.akiasync.mixin.brain.enderman;

import java.util.UUID;
import net.minecraft.world.entity.monster.EnderMan;

public final class EndermanCpuCalculator {
    
    public static EndermanDiff runCpuOnly(EnderMan enderman, EndermanSnapshot snap) {
        EndermanDiff diff = new EndermanDiff();
        
        try {
            
            if (shouldTeleportFromSunlight(snap)) {
                diff.setShouldTeleport(true);
                diff.setShouldClearTarget(true);
                return diff; 
            }
            
            if (shouldTeleportFromWater(snap)) {
                diff.setShouldTeleport(true);
                return diff;
            }
            
            
            if (snap.hasTarget()) {
                
                
            } else if (!snap.nearbyPlayers().isEmpty()) {
                UUID staringPlayer = findStaringPlayer(snap);
                if (staringPlayer != null) {
                    diff.setAttackTarget(staringPlayer);
                }
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EndermanCpuCalculator", "runCpuOnly", e);
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
    
    private static UUID findStaringPlayer(EndermanSnapshot snap) {
        if (snap.nearbyPlayers().isEmpty()) {
            return null;
        }
        
        EndermanSnapshot.PlayerInfo closest = null;
        double minDistSq = Double.MAX_VALUE;
        
        for (EndermanSnapshot.PlayerInfo player : snap.nearbyPlayers()) {
            if (!isPlayerStaringAt(player, snap)) {
                continue;
            }
            double distSq = player.distanceSq();
            if (distSq < minDistSq) {
                minDistSq = distSq;
                closest = player;
            }
        }
        
        return closest != null ? closest.id() : null;
    }
    
    private static boolean isPlayerStaringAt(EndermanSnapshot.PlayerInfo player, EndermanSnapshot snap) {
        net.minecraft.world.phys.Vec3 playerView = player.viewVector();
        double playerEyeY = player.eyeY();
        net.minecraft.world.phys.Vec3 endermanPos = snap.position();
        
        double endermanEyeY = endermanPos.y + 2.55;
        
        net.minecraft.world.phys.Vec3 toEnderman = new net.minecraft.world.phys.Vec3(
            endermanPos.x - player.pos().x,
            endermanEyeY - playerEyeY,
            endermanPos.z - player.pos().z
        );
        
        double len = toEnderman.length();
        if (len < 0.01) {
            return false;
        }
        
        net.minecraft.world.phys.Vec3 toEndermanNorm = toEnderman.normalize();
        double dotProduct = playerView.dot(toEndermanNorm);
        
        double tolerance = 0.025;
        double threshold = 1.0 - tolerance / len;
        
        return dotProduct > threshold;
    }
}
