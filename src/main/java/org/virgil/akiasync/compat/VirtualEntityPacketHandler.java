package org.virgil.akiasync.compat;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.virgil.akiasync.config.ConfigManager;

import java.lang.reflect.Field;
import java.util.logging.Logger;

public class VirtualEntityPacketHandler {
    
    private final PluginDetectorRegistry registry;
    private final ConfigManager configManager;
    private final Logger logger;
    private final boolean debugEnabled;
    
    private static Field addEntityPacketIdField;
    private static Field setEntityDataPacketIdField;
    private static Field teleportEntityPacketIdField;
    private static Field moveEntityPacketIdField;
    private static Field rotateHeadPacketIdField;
    private static Field setEntityMotionPacketIdField;
    private static Field removeEntitiesPacketIdsField;
    
    static {
        try {
            
            addEntityPacketIdField = ClientboundAddEntityPacket.class.getDeclaredField("id");
            addEntityPacketIdField.setAccessible(true);
            
            setEntityDataPacketIdField = ClientboundSetEntityDataPacket.class.getDeclaredField("id");
            setEntityDataPacketIdField.setAccessible(true);
            
            teleportEntityPacketIdField = ClientboundTeleportEntityPacket.class.getDeclaredField("id");
            teleportEntityPacketIdField.setAccessible(true);
            
            moveEntityPacketIdField = ClientboundMoveEntityPacket.class.getDeclaredField("entityId");
            moveEntityPacketIdField.setAccessible(true);
            
            rotateHeadPacketIdField = ClientboundRotateHeadPacket.class.getDeclaredField("entityId");
            rotateHeadPacketIdField.setAccessible(true);
            
            setEntityMotionPacketIdField = ClientboundSetEntityMotionPacket.class.getDeclaredField("id");
            setEntityMotionPacketIdField.setAccessible(true);
            
            removeEntitiesPacketIdsField = ClientboundRemoveEntitiesPacket.class.getDeclaredField("entityIds");
            removeEntitiesPacketIdsField.setAccessible(true);
        } catch (Exception e) {
            
        }
    }
    
    public VirtualEntityPacketHandler(PluginDetectorRegistry registry, ConfigManager configManager, Logger logger) {
        this.registry = registry;
        this.configManager = configManager;
        this.logger = logger;
        
        boolean debug = false;
        try {
            debug = configManager.isVirtualEntityDebugEnabled();
        } catch (NoSuchMethodError e) {
            
            debug = org.virgil.akiasync.util.DebugLogger.isDebugEnabled();
        }
        this.debugEnabled = debug;
    }
    
    public boolean isVirtualEntityRelatedPacket(Packet<?> packet, ServerPlayer player) {
        if (packet == null || player == null) {
            return false;
        }
        
        try {
            Integer entityId = extractEntityId(packet);
            if (entityId == null) {
                return false;
            }
            
            ServerLevel level = (ServerLevel) player.level();
            if (level == null) {
                return false;
            }
            
            Entity entity = level.getEntity(entityId);
            if (entity == null) {
                
                return false;
            }
            
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            if (bukkitEntity == null) {
                return false;
            }
            
            boolean isVirtual = registry.isVirtualEntity(bukkitEntity);
            
            if (isVirtual && debugEnabled) {
                String source = registry.getEntitySource(bukkitEntity);
                debugLog("Virtual entity packet detected: " + packet.getClass().getSimpleName() + 
                        " for entity " + entityId + " from " + source);
            }
            
            return isVirtual;
            
        } catch (Exception e) {
            if (debugEnabled) {
                logger.warning("[VirtualEntity] Error checking packet for virtual entity: " + e.getMessage());
            }
            return false;
        }
    }
    
    public boolean shouldBypassQueue(Packet<?> packet, ServerPlayer player) {
        
        boolean bypassEnabled = true;
        try {
            bypassEnabled = configManager.isVirtualEntityBypassPacketQueue();
        } catch (NoSuchMethodError e) {
            
        }
        
        if (!bypassEnabled) {
            return false;
        }
        
        boolean shouldBypass = isVirtualEntityRelatedPacket(packet, player);
        
        if (shouldBypass && debugEnabled) {
            Integer entityId = extractEntityId(packet);
            String source = getPacketSource(packet, player);
            debugLog("Virtual entity packet bypassing queue: " + packet.getClass().getSimpleName() + 
                    " for entity " + entityId + " from " + source);
        }
        
        return shouldBypass;
    }
    
    public String getPacketSource(Packet<?> packet, ServerPlayer player) {
        if (packet == null || player == null) {
            return null;
        }
        
        try {
            Integer entityId = extractEntityId(packet);
            if (entityId == null) {
                return null;
            }
            
            ServerLevel level = (ServerLevel) player.level();
            if (level == null) {
                return null;
            }
            
            Entity entity = level.getEntity(entityId);
            if (entity == null) {
                return null;
            }
            
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            if (bukkitEntity == null) {
                return null;
            }
            
            return registry.getEntitySource(bukkitEntity);
            
        } catch (Exception e) {
            if (debugEnabled) {
                logger.warning("[VirtualEntity] Error getting packet source: " + e.getMessage());
            }
            return null;
        }
    }
    
    private Integer extractEntityId(Packet<?> packet) {
        try {
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
            
        } catch (Exception e) {
            if (debugEnabled) {
                logger.fine("[VirtualEntity] Failed to extract entity ID from packet: " + e.getMessage());
            }
        }
        
        return null;
    }
    
    private void debugLog(String message) {
        if (debugEnabled) {
            logger.info("[AkiAsync-VirtualEntity] " + message);
        }
    }
}
