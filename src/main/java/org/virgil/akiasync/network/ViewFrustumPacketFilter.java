package org.virgil.akiasync.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViewFrustumPacketFilter {

    private static final double HORIZONTAL_FOV = 110.0; 
    private static final double VERTICAL_FOV = 70.0;
    private static final double FOV_BUFFER = 10.0; 
    
    private static final double MAX_ENTITY_DISTANCE = 64.0; 
    private static final double MAX_BLOCK_UPDATE_DISTANCE = 48.0;
    private static final double BEHIND_PLAYER_THRESHOLD = -0.3; 
    
    private static Field addEntityPacketIdField;
    private static Field addEntityPacketXField;
    private static Field addEntityPacketYField;
    private static Field addEntityPacketZField;
    
    private static Field setEntityDataPacketIdField;
    private static Field teleportEntityPacketIdField;
    private static Field teleportEntityPacketXField;
    private static Field teleportEntityPacketYField;
    private static Field teleportEntityPacketZField;
    
    private static Field moveEntityPacketIdField;
    private static Field rotateHeadPacketIdField;
    private static Field setEntityMotionPacketIdField;
    
    private static Field blockUpdatePacketPosField;
    private static Field particlesPacketXField;
    private static Field particlesPacketYField;
    private static Field particlesPacketZField;
    
    static {
        try {
            
            addEntityPacketIdField = ClientboundAddEntityPacket.class.getDeclaredField("id");
            addEntityPacketIdField.setAccessible(true);
            addEntityPacketXField = ClientboundAddEntityPacket.class.getDeclaredField("x");
            addEntityPacketXField.setAccessible(true);
            addEntityPacketYField = ClientboundAddEntityPacket.class.getDeclaredField("y");
            addEntityPacketYField.setAccessible(true);
            addEntityPacketZField = ClientboundAddEntityPacket.class.getDeclaredField("z");
            addEntityPacketZField.setAccessible(true);
            
            setEntityDataPacketIdField = ClientboundSetEntityDataPacket.class.getDeclaredField("id");
            setEntityDataPacketIdField.setAccessible(true);
            
            teleportEntityPacketIdField = ClientboundTeleportEntityPacket.class.getDeclaredField("id");
            teleportEntityPacketIdField.setAccessible(true);
            teleportEntityPacketXField = ClientboundTeleportEntityPacket.class.getDeclaredField("x");
            teleportEntityPacketXField.setAccessible(true);
            teleportEntityPacketYField = ClientboundTeleportEntityPacket.class.getDeclaredField("y");
            teleportEntityPacketYField.setAccessible(true);
            teleportEntityPacketZField = ClientboundTeleportEntityPacket.class.getDeclaredField("z");
            teleportEntityPacketZField.setAccessible(true);
            
            moveEntityPacketIdField = ClientboundMoveEntityPacket.class.getDeclaredField("entityId");
            moveEntityPacketIdField.setAccessible(true);
            
            rotateHeadPacketIdField = ClientboundRotateHeadPacket.class.getDeclaredField("entityId");
            rotateHeadPacketIdField.setAccessible(true);
            
            setEntityMotionPacketIdField = ClientboundSetEntityMotionPacket.class.getDeclaredField("id");
            setEntityMotionPacketIdField.setAccessible(true);
            
            blockUpdatePacketPosField = ClientboundBlockUpdatePacket.class.getDeclaredField("pos");
            blockUpdatePacketPosField.setAccessible(true);
            
            particlesPacketXField = ClientboundLevelParticlesPacket.class.getDeclaredField("x");
            particlesPacketXField.setAccessible(true);
            particlesPacketYField = ClientboundLevelParticlesPacket.class.getDeclaredField("y");
            particlesPacketYField.setAccessible(true);
            particlesPacketZField = ClientboundLevelParticlesPacket.class.getDeclaredField("z");
            particlesPacketZField.setAccessible(true);
        } catch (Exception e) {
            
        }
    }
    
    private final Map<UUID, PlayerViewData> playerViewCache = new ConcurrentHashMap<>();
    
    private boolean enabled = true;
    private boolean filterEntities = true;
    private boolean filterBlocks = true;
    private boolean filterParticles = true;
    private boolean debugEnabled = false;
    
    private static class PlayerViewData {
        final Location location;
        final Vector viewDirection;
        final long timestamp;
        
        PlayerViewData(Location location, Vector viewDirection) {
            this.location = location;
            this.viewDirection = viewDirection;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 100; 
        }
    }
    
    public void updatePlayerView(ServerPlayer player) {
        if (player == null) return;
        
        try {
            org.bukkit.entity.Player bukkitPlayer = player.getBukkitEntity();
            if (bukkitPlayer == null) return;
            
            Location location = bukkitPlayer.getLocation();
            Vector viewDirection = location.getDirection().normalize();
            
            playerViewCache.put(player.getUUID(), new PlayerViewData(location, viewDirection));
        } catch (Exception e) {
            
        }
    }
    
    public boolean shouldFilterPacket(Packet<?> packet, ServerPlayer player) {
        if (!enabled || packet == null || player == null) {
            return false;
        }
        
        try {
            
            PlayerViewData viewData = playerViewCache.get(player.getUUID());
            if (viewData == null || viewData.isExpired()) {
                updatePlayerView(player);
                viewData = playerViewCache.get(player.getUUID());
                if (viewData == null) return false;
            }
            
            if (filterEntities && isEntityPacket(packet)) {
                return shouldFilterEntityPacket(packet, player, viewData);
            }
            
            if (filterBlocks && packet instanceof ClientboundBlockUpdatePacket) {
                return shouldFilterBlockUpdate((ClientboundBlockUpdatePacket) packet, viewData);
            }
            
            if (filterParticles && packet instanceof ClientboundLevelParticlesPacket) {
                return shouldFilterParticles((ClientboundLevelParticlesPacket) packet, viewData);
            }
            
        } catch (Exception e) {
            
            return false;
        }
        
        return false;
    }
    
    private boolean isEntityPacket(Packet<?> packet) {
        return packet instanceof ClientboundAddEntityPacket ||
               packet instanceof ClientboundSetEntityDataPacket ||
               packet instanceof ClientboundTeleportEntityPacket ||
               packet instanceof ClientboundMoveEntityPacket ||
               packet instanceof ClientboundRotateHeadPacket ||
               packet instanceof ClientboundSetEntityMotionPacket;
    }
    
    private boolean shouldFilterEntityPacket(Packet<?> packet, ServerPlayer player, PlayerViewData viewData) {
        try {
            
            Vec3 entityPos = extractEntityPosition(packet, player);
            if (entityPos == null) {
                
                return false;
            }
            
            return !isPositionInViewFrustum(entityPos, viewData);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private Vec3 extractEntityPosition(Packet<?> packet, ServerPlayer player) throws Exception {
        
        if (packet instanceof ClientboundAddEntityPacket && 
            addEntityPacketXField != null && addEntityPacketYField != null && addEntityPacketZField != null) {
            double x = (double) addEntityPacketXField.get(packet);
            double y = (double) addEntityPacketYField.get(packet);
            double z = (double) addEntityPacketZField.get(packet);
            return new Vec3(x, y, z);
        }
        
        if (packet instanceof ClientboundTeleportEntityPacket &&
            teleportEntityPacketXField != null && teleportEntityPacketYField != null && teleportEntityPacketZField != null) {
            double x = (double) teleportEntityPacketXField.get(packet);
            double y = (double) teleportEntityPacketYField.get(packet);
            double z = (double) teleportEntityPacketZField.get(packet);
            return new Vec3(x, y, z);
        }
        
        Integer entityId = extractEntityId(packet);
        if (entityId != null) {
            Entity entity = player.level().getEntity(entityId);
            if (entity != null) {
                return entity.position();
            }
        }
        
        return null;
    }
    
    private Integer extractEntityId(Packet<?> packet) throws Exception {
        if (packet instanceof ClientboundAddEntityPacket && addEntityPacketIdField != null) {
            return (Integer) addEntityPacketIdField.get(packet);
        }
        if (packet instanceof ClientboundSetEntityDataPacket && setEntityDataPacketIdField != null) {
            return (Integer) setEntityDataPacketIdField.get(packet);
        }
        if (packet instanceof ClientboundTeleportEntityPacket && teleportEntityPacketIdField != null) {
            return (Integer) teleportEntityPacketIdField.get(packet);
        }
        if (packet instanceof ClientboundMoveEntityPacket && moveEntityPacketIdField != null) {
            return (Integer) moveEntityPacketIdField.get(packet);
        }
        if (packet instanceof ClientboundRotateHeadPacket && rotateHeadPacketIdField != null) {
            return (Integer) rotateHeadPacketIdField.get(packet);
        }
        if (packet instanceof ClientboundSetEntityMotionPacket && setEntityMotionPacketIdField != null) {
            return (Integer) setEntityMotionPacketIdField.get(packet);
        }
        return null;
    }
    
    private boolean shouldFilterBlockUpdate(ClientboundBlockUpdatePacket packet, PlayerViewData viewData) {
        try {
            if (blockUpdatePacketPosField == null) return false;
            
            BlockPos pos = (BlockPos) blockUpdatePacketPosField.get(packet);
            if (pos == null) return false;
            
            Vec3 blockPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            return !isPositionInViewFrustum(blockPos, viewData);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean shouldFilterParticles(ClientboundLevelParticlesPacket packet, PlayerViewData viewData) {
        try {
            if (particlesPacketXField == null || particlesPacketYField == null || particlesPacketZField == null) {
                return false;
            }
            
            double x = (double) particlesPacketXField.get(packet);
            double y = (double) particlesPacketYField.get(packet);
            double z = (double) particlesPacketZField.get(packet);
            
            Vec3 particlePos = new Vec3(x, y, z);
            return !isPositionInViewFrustum(particlePos, viewData);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isPositionInViewFrustum(Vec3 pos, PlayerViewData viewData) {
        Location playerLoc = viewData.location;
        Vector viewDir = viewData.viewDirection;
        
        double dx = pos.x - playerLoc.getX();
        double dy = pos.y - playerLoc.getY();
        double dz = pos.z - playerLoc.getZ();
        
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (distance > MAX_ENTITY_DISTANCE) {
            if (debugEnabled) {
                System.out.println("[ViewFilter] Filtered: too far (" + distance + "m)");
            }
            return false;
        }
        
        if (distance < 3.0) {
            return true;
        }
        
        Vector toPos = new Vector(dx / distance, dy / distance, dz / distance);
        
        double dotProduct = viewDir.getX() * toPos.getX() +
                           viewDir.getY() * toPos.getY() +
                           viewDir.getZ() * toPos.getZ();
        
        if (dotProduct < BEHIND_PLAYER_THRESHOLD) {
            if (debugEnabled) {
                System.out.println("[ViewFilter] Filtered: behind player (dot=" + dotProduct + ")");
            }
            return false;
        }
        
        Vector viewDirHorizontal = new Vector(viewDir.getX(), 0, viewDir.getZ()).normalize();
        Vector toPosHorizontal = new Vector(toPos.getX(), 0, toPos.getZ()).normalize();
        
        double horizontalDot = viewDirHorizontal.getX() * toPosHorizontal.getX() +
                               viewDirHorizontal.getZ() * toPosHorizontal.getZ();
        double horizontalAngle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, horizontalDot))));
        
        double effectiveHorizontalFOV = HORIZONTAL_FOV / 2.0 + FOV_BUFFER;
        if (horizontalAngle > effectiveHorizontalFOV) {
            if (debugEnabled) {
                System.out.println("[ViewFilter] Filtered: outside horizontal FOV (" + horizontalAngle + "°)");
            }
            return false;
        }
        
        double verticalAngle = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, toPos.getY()))));
        double playerPitch = playerLoc.getPitch();
        double relativePitch = Math.abs(verticalAngle - playerPitch);
        
        double effectiveVerticalFOV = VERTICAL_FOV / 2.0 + FOV_BUFFER;
        if (relativePitch > effectiveVerticalFOV) {
            if (debugEnabled) {
                System.out.println("[ViewFilter] Filtered: outside vertical FOV (" + relativePitch + "°)");
            }
            return false;
        }
        
        return true;
    }
    
    public void removePlayer(UUID playerId) {
        playerViewCache.remove(playerId);
    }
    
    public void clear() {
        playerViewCache.clear();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setFilterEntities(boolean filter) {
        this.filterEntities = filter;
    }
    
    public void setFilterBlocks(boolean filter) {
        this.filterBlocks = filter;
    }
    
    public void setFilterParticles(boolean filter) {
        this.filterParticles = filter;
    }
    
    public void setDebugEnabled(boolean debug) {
        this.debugEnabled = debug;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isFilterEntities() {
        return filterEntities;
    }
    
    public boolean isFilterBlocks() {
        return filterBlocks;
    }
    
    public boolean isFilterParticles() {
        return filterParticles;
    }
}
