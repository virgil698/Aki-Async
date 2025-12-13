package org.virgil.akiasync.mixin.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class EntityPacketThrottler {
    
    private static boolean initialized = false;
    
    private static final Map<Integer, Long> ENTITY_UPDATE_TIMERS = new ConcurrentHashMap<>();
    
    private static final Map<String, Long> PLAYER_ENTITY_TIMERS = new ConcurrentHashMap<>();
    
    private static long currentTick = 0;
    
    private static long totalChecks = 0;
    private static long throttledPackets = 0;
    private static long allowedPackets = 0;
    
    public static void initialize(Object pluginInstance) {
        if (initialized) {
            return;
        }
        
        initialized = true;
        
        EntityVisibilityManager.initialize();
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[EntityPacketThrottler] Initialized successfully");
        }
    }
    
    public static void shutdown() {
        if (!initialized) {
            return;
        }
        
        clearAll();
        initialized = false;
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[EntityPacketThrottler] Shutdown completed");
        }
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static void tick() {
        currentTick++;
        
        if (currentTick % 1000 == 0) {
            cleanup();
        }
    }
    
    public static boolean shouldSendUpdate(ServerPlayer player, Entity entity, 
                                          double fovAngle, boolean enableFrustumCulling) {
        totalChecks++;
        
        if (entity.getId() == player.getId()) {
            allowedPackets++;
            return true;
        }
        
        double distanceSquared = EntityVisibilityManager.getDistanceSquared(player, entity);
        int distanceTier = EntityVisibilityManager.getDistanceTier(distanceSquared);
        
        int updateInterval = EntityVisibilityManager.getUpdateInterval(distanceTier);
        
        if (enableFrustumCulling && distanceTier > 0) {
            boolean inFrustum = EntityVisibilityManager.isInViewFrustum(player, entity, fovAngle);
            
            if (!inFrustum) {
                
                updateInterval *= 2;
            }
        }
        
        String key = player.getUUID() + "-" + entity.getId();
        Long lastUpdate = PLAYER_ENTITY_TIMERS.get(key);
        
        if (lastUpdate == null || currentTick - lastUpdate >= updateInterval) {
            
            PLAYER_ENTITY_TIMERS.put(key, currentTick);
            allowedPackets++;
            return true;
        }
        
        throttledPackets++;
        return false;
    }
    
    public static boolean shouldSendUpdateSimple(ServerPlayer player, Entity entity) {
        totalChecks++;
        
        if (entity.getId() == player.getId()) {
            allowedPackets++;
            return true;
        }
        
        double distanceSquared = EntityVisibilityManager.getDistanceSquared(player, entity);
        int distanceTier = EntityVisibilityManager.getDistanceTier(distanceSquared);
        
        int updateInterval = EntityVisibilityManager.getUpdateInterval(distanceTier);
        
        String key = player.getUUID() + "-" + entity.getId();
        Long lastUpdate = PLAYER_ENTITY_TIMERS.get(key);
        
        if (lastUpdate == null || currentTick - lastUpdate >= updateInterval) {
            
            PLAYER_ENTITY_TIMERS.put(key, currentTick);
            allowedPackets++;
            return true;
        }
        
        throttledPackets++;
        return false;
    }
    
    public static void forceUpdate(ServerPlayer player, Entity entity) {
        String key = player.getUUID() + "-" + entity.getId();
        PLAYER_ENTITY_TIMERS.put(key, currentTick);
    }
    
    private static void cleanup() {
        long expireTime = currentTick - 200; 
        
        PLAYER_ENTITY_TIMERS.entrySet().removeIf(entry -> entry.getValue() < expireTime);
        ENTITY_UPDATE_TIMERS.entrySet().removeIf(entry -> entry.getValue() < expireTime);
    }
    
    public static void clearPlayer(UUID playerId) {
        String prefix = playerId.toString() + "-";
        PLAYER_ENTITY_TIMERS.keySet().removeIf(key -> key.startsWith(prefix));
    }
    
    public static void clearEntity(int entityId) {
        ENTITY_UPDATE_TIMERS.remove(entityId);
        String suffix = "-" + entityId;
        PLAYER_ENTITY_TIMERS.keySet().removeIf(key -> key.endsWith(suffix));
    }
    
    public static void clearAll() {
        ENTITY_UPDATE_TIMERS.clear();
        PLAYER_ENTITY_TIMERS.clear();
    }
    
    public static String getStats() {
        double throttleRate = totalChecks > 0 ? 
            (double) throttledPackets / totalChecks * 100.0 : 0.0;
        
        return String.format(
            "EntityPacketThrottler: checks=%d, allowed=%d, throttled=%d (%.1f%%), timers=%d",
            totalChecks, allowedPackets, throttledPackets, throttleRate, 
            PLAYER_ENTITY_TIMERS.size()
        );
    }
    
    public static void resetStats() {
        totalChecks = 0;
        throttledPackets = 0;
        allowedPackets = 0;
    }
    
    public static long getCurrentTick() {
        return currentTick;
    }
}
