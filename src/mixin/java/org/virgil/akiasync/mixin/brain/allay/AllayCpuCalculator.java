package org.virgil.akiasync.mixin.brain.allay;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;

public final class AllayCpuCalculator {
    
    public static AllayDiff compute(AllaySnapshot snapshot) {
        
        if (snapshot.isDancing()) {
            return new AllayDiff(null, null, null);
        }
        
        if (snapshot.getNearestNoteBlock() != null) {
            return new AllayDiff(null, null, snapshot.getNearestNoteBlock());
        }
        
        if (!snapshot.getHeldItem().isEmpty() && !snapshot.getNearbyItems().isEmpty()) {
            UUID nearestItem = findNearestItem(snapshot);
            return new AllayDiff(nearestItem, null, null);
        }
        
        UUID nearestPlayer = findNearestPlayer(snapshot);
        return new AllayDiff(null, nearestPlayer, null);
    }
    
    private static UUID findNearestItem(AllaySnapshot snapshot) {
        List<AllaySnapshot.ItemEntityInfo> items = snapshot.getNearbyItems();
        
        if (items.isEmpty()) {
            return null;
        }
        
        AllaySnapshot.ItemEntityInfo nearest = null;
        double minDist = Double.MAX_VALUE;
        
        for (AllaySnapshot.ItemEntityInfo item : items) {
            if (item.getDistanceSq() < minDist) {
                minDist = item.getDistanceSq();
                nearest = item;
            }
        }
        
        return nearest != null ? nearest.getItemEntityId() : null;
    }
    
    private static UUID findNearestPlayer(AllaySnapshot snapshot) {
        List<AllaySnapshot.PlayerInfo> players = snapshot.getNearbyPlayers();
        
        if (players.isEmpty()) {
            return null;
        }
        
        AllaySnapshot.PlayerInfo nearest = null;
        double minDist = Double.MAX_VALUE;
        
        for (AllaySnapshot.PlayerInfo player : players) {
            if (player.getDistanceSq() < minDist) {
                minDist = player.getDistanceSq();
                nearest = player;
            }
        }
        
        return nearest != null ? nearest.getPlayerId() : null;
    }
}
