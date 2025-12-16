package org.virgil.akiasync.mixin.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityVisibilityManager {
    
    private static final Map<UUID, PlayerViewFrustum> VIEW_FRUSTUM_CACHE = new ConcurrentHashMap<>();
    
    private static final long CACHE_EXPIRE_TIME = 100;
    
    public static void initialize() {
        
    }
    
    public static double getDistanceSquared(ServerPlayer player, Entity entity) {
        return player.distanceToSqr(entity);
    }
    
    public static int getDistanceTier(double distanceSquared) {
        if (distanceSquared < 16 * 16) return 0;
        if (distanceSquared < 32 * 32) return 1;
        if (distanceSquared < 64 * 64) return 2;
        if (distanceSquared < 128 * 128) return 3;
        return 4;
    }
    
    public static int getUpdateInterval(int tier) {
        return switch (tier) {
            case 0 -> 1;   
            case 1 -> 2;   
            case 2 -> 4;   
            case 3 -> 8;   
            case 4 -> 16;  
            default -> 1;
        };
    }
    
    public static boolean isInViewFrustum(ServerPlayer player, Entity entity, double fovAngle) {
        PlayerViewFrustum frustum = getOrCreateViewFrustum(player, fovAngle);
        return frustum.isEntityVisible(entity);
    }
    
    private static PlayerViewFrustum getOrCreateViewFrustum(ServerPlayer player, double fovAngle) {
        UUID playerId = player.getUUID();
        PlayerViewFrustum frustum = VIEW_FRUSTUM_CACHE.get(playerId);
        
        long currentTime = System.currentTimeMillis();
        
        if (frustum == null || currentTime - frustum.timestamp > CACHE_EXPIRE_TIME) {
            frustum = new PlayerViewFrustum(player, fovAngle);
            VIEW_FRUSTUM_CACHE.put(playerId, frustum);
        }
        
        return frustum;
    }
    
    public static void clearPlayerCache(UUID playerId) {
        VIEW_FRUSTUM_CACHE.remove(playerId);
    }
    
    public static void clearAllCaches() {
        VIEW_FRUSTUM_CACHE.clear();
    }
    
    private static class PlayerViewFrustum {
        private final Vec3 position;
        private final Vec3 lookDirection;
        private final double fovCosine;
        private final long timestamp;
        
        public PlayerViewFrustum(ServerPlayer player, double fovAngle) {
            this.position = player.getEyePosition();
            this.lookDirection = player.getLookAngle().normalize();
            
            this.fovCosine = Math.cos(Math.toRadians(fovAngle / 2.0));
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isEntityVisible(Entity entity) {
            Vec3 entityPos = entity.position();
            Vec3 toEntity = entityPos.subtract(position).normalize();
            
            double dotProduct = lookDirection.dot(toEntity);
            
            return dotProduct >= fovCosine;
        }
    }
    
    public static String getStats() {
        return String.format("ViewFrustumCache: %d players", VIEW_FRUSTUM_CACHE.size());
    }
}
