package org.virgil.akiasync.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityDataThrottler {
    
    private static final Map<String, Integer> METADATA_CACHE = new ConcurrentHashMap<>();
    
    private static final Map<String, Long> NBT_UPDATE_TIMERS = new ConcurrentHashMap<>();
    
    private static final Map<String, Long> METADATA_UPDATE_TIMERS = new ConcurrentHashMap<>();
    
    private static long currentTick = 0;
    
    private static long metadataChecks = 0;
    private static long metadataThrottled = 0;
    private static long nbtChecks = 0;
    private static long nbtThrottled = 0;
    
    public static void tick() {
        currentTick++;
        
        if (currentTick % 1000 == 0) {
            cleanup();
        }
    }
    
    public static boolean shouldSendMetadata(ServerPlayer player, Entity entity, int metadataHash) {
        metadataChecks++;
        
        if (entity.getId() == player.getId()) {
            updateMetadataCache(player, entity, metadataHash);
            return true;
        }
        
        String key = player.getUUID() + "-" + entity.getId();
        
        Integer cachedHash = METADATA_CACHE.get(key);
        if (cachedHash == null || cachedHash != metadataHash) {
            
            updateMetadataCache(player, entity, metadataHash);
            return true;
        }
        
        double distanceSquared = EntityVisibilityManager.getDistanceSquared(player, entity);
        int distanceTier = EntityVisibilityManager.getDistanceTier(distanceSquared);
        
        int refreshInterval = getMetadataRefreshInterval(distanceTier);
        
        Long lastUpdate = METADATA_UPDATE_TIMERS.get(key);
        if (lastUpdate == null || currentTick - lastUpdate >= refreshInterval) {
            
            METADATA_UPDATE_TIMERS.put(key, currentTick);
            updateMetadataCache(player, entity, metadataHash);
            return true;
        }
        
        metadataThrottled++;
        return false;
    }
    
    public static boolean shouldSendNBT(ServerPlayer player, Entity entity, boolean forceUpdate) {
        nbtChecks++;
        
        if (entity.getId() == player.getId()) {
            return true;
        }
        
        if (forceUpdate) {
            updateNBTTimer(player, entity);
            return true;
        }
        
        String key = player.getUUID() + "-" + entity.getId();
        
        double distanceSquared = EntityVisibilityManager.getDistanceSquared(player, entity);
        int distanceTier = EntityVisibilityManager.getDistanceTier(distanceSquared);
        
        int nbtInterval = getNBTUpdateInterval(distanceTier);
        
        Long lastUpdate = NBT_UPDATE_TIMERS.get(key);
        if (lastUpdate == null || currentTick - lastUpdate >= nbtInterval) {
            
            updateNBTTimer(player, entity);
            return true;
        }
        
        nbtThrottled++;
        return false;
    }
    
    private static int getMetadataRefreshInterval(int distanceTier) {
        return switch (distanceTier) {
            case 0 -> 100;  
            case 1 -> 200;  
            case 2 -> 400;  
            case 3 -> 600;  
            default -> 1200; 
        };
    }
    
    private static int getNBTUpdateInterval(int distanceTier) {
        return switch (distanceTier) {
            case 0 -> 40;   
            case 1 -> 100;  
            case 2 -> 200;  
            case 3 -> 400;  
            default -> 800; 
        };
    }
    
    private static void updateMetadataCache(ServerPlayer player, Entity entity, int metadataHash) {
        String key = player.getUUID() + "-" + entity.getId();
        METADATA_CACHE.put(key, metadataHash);
        METADATA_UPDATE_TIMERS.put(key, currentTick);
    }
    
    private static void updateNBTTimer(ServerPlayer player, Entity entity) {
        String key = player.getUUID() + "-" + entity.getId();
        NBT_UPDATE_TIMERS.put(key, currentTick);
    }
    
    public static void forceUpdate(ServerPlayer player, Entity entity) {
        String key = player.getUUID() + "-" + entity.getId();
        METADATA_CACHE.remove(key);
        METADATA_UPDATE_TIMERS.put(key, currentTick);
        NBT_UPDATE_TIMERS.put(key, currentTick);
    }
    
    private static void cleanup() {
        long expireTime = currentTick - 1200; 
        
        METADATA_CACHE.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            Long lastUpdate = METADATA_UPDATE_TIMERS.get(key);
            return lastUpdate != null && lastUpdate < expireTime;
        });
        
        METADATA_UPDATE_TIMERS.entrySet().removeIf(entry -> entry.getValue() < expireTime);
        NBT_UPDATE_TIMERS.entrySet().removeIf(entry -> entry.getValue() < expireTime);
    }
    
    public static void clearPlayer(java.util.UUID playerId) {
        String prefix = playerId.toString() + "-";
        METADATA_CACHE.keySet().removeIf(key -> key.startsWith(prefix));
        METADATA_UPDATE_TIMERS.keySet().removeIf(key -> key.startsWith(prefix));
        NBT_UPDATE_TIMERS.keySet().removeIf(key -> key.startsWith(prefix));
    }
    
    public static void clearEntity(int entityId) {
        String suffix = "-" + entityId;
        METADATA_CACHE.keySet().removeIf(key -> key.endsWith(suffix));
        METADATA_UPDATE_TIMERS.keySet().removeIf(key -> key.endsWith(suffix));
        NBT_UPDATE_TIMERS.keySet().removeIf(key -> key.endsWith(suffix));
    }
    
    public static void clearAll() {
        METADATA_CACHE.clear();
        METADATA_UPDATE_TIMERS.clear();
        NBT_UPDATE_TIMERS.clear();
    }
    
    public static String getStats() {
        double metadataThrottleRate = metadataChecks > 0 ? 
            (double) metadataThrottled / metadataChecks * 100.0 : 0.0;
        double nbtThrottleRate = nbtChecks > 0 ? 
            (double) nbtThrottled / nbtChecks * 100.0 : 0.0;
        
        return String.format(
            "EntityDataThrottler: metadata[checks=%d, throttled=%d (%.1f%%)] nbt[checks=%d, throttled=%d (%.1f%%)] caches=%d",
            metadataChecks, metadataThrottled, metadataThrottleRate,
            nbtChecks, nbtThrottled, nbtThrottleRate,
            METADATA_CACHE.size()
        );
    }
    
    public static void resetStats() {
        metadataChecks = 0;
        metadataThrottled = 0;
        nbtChecks = 0;
        nbtThrottled = 0;
    }
    
    public static long getCurrentTick() {
        return currentTick;
    }
}
