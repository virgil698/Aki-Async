package org.virgil.akiasync.mixin.brain.warden;

import java.util.List;
import java.util.UUID;

public final class WardenCpuCalculator {
    
    public static WardenDiff compute(WardenSnapshot snapshot) {
        
        if (snapshot.isDigging() || snapshot.isEmerging()) {
            return new WardenDiff(null, false, false);
        }
        
        UUID bestTarget = selectBestTarget(snapshot);
        
        boolean shouldUseSonicBoom = evaluateSonicBoom(snapshot, bestTarget);
        
        boolean shouldDig = evaluateDigging(snapshot);
        
        return new WardenDiff(bestTarget, shouldUseSonicBoom, shouldDig);
    }
    
    private static UUID selectBestTarget(WardenSnapshot snapshot) {
        List<WardenSnapshot.PlayerInfo> players = snapshot.getNearbyPlayers();
        List<WardenSnapshot.EntityInfo> entities = snapshot.getNearbyEntities();
        
        if (snapshot.getAngerLevel() > 80) {
            
            if (!players.isEmpty()) {
                WardenSnapshot.PlayerInfo closest = null;
                double minDist = Double.MAX_VALUE;
                
                for (WardenSnapshot.PlayerInfo player : players) {
                    
                    double dist = player.isSneaking() ? player.getDistanceSq() * 2.0 : player.getDistanceSq();
                    if (dist < minDist) {
                        minDist = dist;
                        closest = player;
                    }
                }
                
                if (closest != null) {
                    return closest.getPlayerId();
                }
            }
            
            if (!entities.isEmpty()) {
                WardenSnapshot.EntityInfo closest = null;
                double minDist = Double.MAX_VALUE;
                
                for (WardenSnapshot.EntityInfo entity : entities) {
                    if (entity.getDistanceSq() < minDist) {
                        minDist = entity.getDistanceSq();
                        closest = entity;
                    }
                }
                
                if (closest != null) {
                    return closest.getEntityId();
                }
            }
        }
        
        if (snapshot.getAngerLevel() > 40) {
            for (WardenSnapshot.PlayerInfo player : players) {
                if (player.getDistanceSq() < 64 && !player.isSneaking()) { 
                    return player.getPlayerId();
                }
            }
        }
        
        return null;
    }
    
    private static boolean evaluateSonicBoom(WardenSnapshot snapshot, UUID targetId) {
        if (targetId == null) return false;
        if (snapshot.getAttackCooldown() > 0) return false;
        
        for (WardenSnapshot.PlayerInfo player : snapshot.getNearbyPlayers()) {
            if (player.getPlayerId().equals(targetId)) {
                double dist = Math.sqrt(player.getDistanceSq());
                return dist >= 5.0 && dist <= 15.0;
            }
        }
        
        for (WardenSnapshot.EntityInfo entity : snapshot.getNearbyEntities()) {
            if (entity.getEntityId().equals(targetId)) {
                double dist = Math.sqrt(entity.getDistanceSq());
                return dist >= 5.0 && dist <= 15.0;
            }
        }
        
        return false;
    }
    
    private static boolean evaluateDigging(WardenSnapshot snapshot) {
        
        if (snapshot.getHealth() > snapshot.getHealth() * 0.5) {
            return false;
        }
        
        int threatCount = snapshot.getNearbyPlayers().size() + snapshot.getNearbyEntities().size();
        if (threatCount < 3) {
            return false;
        }
        
        if (snapshot.getAngerLevel() > 90) {
            return false; 
        }
        
        return true;
    }
}
