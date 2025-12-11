package org.virgil.akiasync.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.bukkit.persistence.PersistentDataContainer;
import org.virgil.akiasync.compat.PluginDetectorRegistry;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class VirtualEntityDetector {

    private static final String ZNPC_PREFIX = "[ZNPC] ";
    private static Logger logger = null;
    private static boolean debugEnabled = false;
    private static PluginDetectorRegistry detectorRegistry = null;
    
    private static final ConcurrentHashMap<Integer, Boolean> virtualEntityCache = new ConcurrentHashMap<>();
    
    private static final int MAX_CACHE_SIZE = 10000;
    
    private static volatile int cacheAccessCount = 0;
    private static final int CLEANUP_INTERVAL = 1000;

    public static void setLogger(Logger log, boolean debug) {
        logger = log;
        debugEnabled = debug;
    }

    public static void setDetectorRegistry(PluginDetectorRegistry registry) {
        detectorRegistry = registry;
        debugLog("PluginDetectorRegistry registered with VirtualEntityDetector");
    }

    public static boolean isVirtualEntity(Entity entity) {
        if (entity == null) return false;
        
        int entityId = entity.getId();
        Boolean cached = virtualEntityCache.get(entityId);
        if (cached != null) return cached;
        
        if (isDefinitelyRealEntity(entity)) {
            virtualEntityCache.putIfAbsent(entityId, false);
            return false;
        }
        
        boolean isVirtual = isVirtualEntitySlow(entity);
        virtualEntityCache.putIfAbsent(entityId, isVirtual);
        return isVirtual;
    }
    
    public static boolean isVirtualEntityQuick(Entity entity) {
        if (entity == null) return false;
        Boolean cached = virtualEntityCache.get(entity.getId());
        return cached != null ? cached : isVirtualEntity(entity);
    }
    
    private static boolean isDefinitelyRealEntity(Entity entity) {
        
        if (entity instanceof ItemEntity) return true;
        if (entity instanceof net.minecraft.world.entity.vehicle.AbstractMinecart) return true;
        if (entity instanceof net.minecraft.world.entity.item.FallingBlockEntity) return true;
        if (entity instanceof net.minecraft.world.entity.projectile.Projectile) return true;
        if (entity instanceof net.minecraft.world.entity.item.PrimedTnt) return true;
        
        if (entity.getDeltaMovement().lengthSqr() > 0.0001) return true;
        
        return false;
    }
    
    private static boolean isVirtualEntitySlow(Entity entity) {

        try {
            
            UUID uuid = entity.getUUID();
            if (uuid == null) {
                debugLog("Virtual entity detected: null UUID");
                return true;
            }

            Level level = entity.level();
            if (level != null) {
                Entity foundEntity = level.getEntity(entity.getId());
                if (foundEntity == null || foundEntity != entity) {
                    debugLog("Virtual entity detected: not in world entity list, id=" + entity.getId());
                    return true;
                }
            }

            org.bukkit.entity.Entity bukkitEntity = null;
            try {
                bukkitEntity = entity.getBukkitEntity();
            } catch (Throwable t) {
                debugLog("Error getting Bukkit entity: " + t.getMessage());
                
                return true;
            }
            
            if (bukkitEntity == null) {
                debugLog("Virtual entity detected: null Bukkit entity");
                return true;
            }

            if (detectorRegistry != null) {
                try {
                    if (detectorRegistry.isVirtualEntity(bukkitEntity)) {
                        String source = detectorRegistry.getEntitySource(bukkitEntity);
                        debugLog("Virtual entity detected via PluginDetectorRegistry: " + source + ", uuid=" + uuid);
                        return true;
                    }
                } catch (Throwable t) {
                    debugLog("Error using PluginDetectorRegistry: " + t.getMessage());
                }
            }

            if (isZNPCEntityFast(bukkitEntity)) {
                debugLog("Virtual entity detected: ZNPCS NPC (fallback), uuid=" + uuid);
                return true;
            }

            if (hasVirtualEntityMarkersFast(bukkitEntity)) {
                debugLog("Virtual entity detected: has virtual markers (fallback), uuid=" + uuid);
                return true;
            }

        } catch (Throwable t) {
            debugLog("Virtual entity detection error: " + t.getMessage());
            return true;
        }

        return false;
    }
    
    private static void cacheResult(int entityId, boolean isVirtual) {
        
        if (++cacheAccessCount >= CLEANUP_INTERVAL) {
            cacheAccessCount = 0;
            if (virtualEntityCache.size() > MAX_CACHE_SIZE) {
                
                virtualEntityCache.clear();
                debugLog("Virtual entity cache cleared, size was: " + MAX_CACHE_SIZE);
            }
        }
        
        virtualEntityCache.put(entityId, isVirtual);
    }
    
    public static void clearCache() {
        virtualEntityCache.clear();
        debugLog("Virtual entity cache manually cleared");
    }
    
    public static String getCacheStats() {
        return String.format("VirtualEntityCache: size=%d/%d", 
            virtualEntityCache.size(), MAX_CACHE_SIZE);
    }

    public static String getEntitySource(Entity entity) {
        if (entity == null) return null;

        try {
            
            if (detectorRegistry != null) {
                try {
                    org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
                    if (bukkitEntity != null) {
                        String source = detectorRegistry.getEntitySource(bukkitEntity);
                        if (source != null) {
                            debugLog("Entity source determined: " + source + ", uuid=" + entity.getUUID());
                            return source;
                        }
                    }
                } catch (Throwable t) {
                    debugLog("Error getting entity source from registry: " + t.getMessage());
                    
                }
            }

            if (isZNPCEntity(entity)) {
                return "ZNPCsPlus";
            }

            if (hasVirtualEntityMarkers(entity)) {
                return "Unknown";
            }

        } catch (Throwable t) {
            debugLog("Error determining entity source: " + t.getMessage());
        }

        return null;
    }

    private static boolean isZNPCEntity(Entity entity) {
        try {
            if (!(entity instanceof net.minecraft.world.entity.player.Player)) {
                return false;
            }

            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            if (bukkitEntity == null) {
                return false;
            }

            return isZNPCEntityFast(bukkitEntity);

        } catch (Throwable t) {
            debugLog("Error checking ZNPC entity: " + t.getClass().getSimpleName());
        }

        return false;
    }
    
    private static boolean isZNPCEntityFast(org.bukkit.entity.Entity bukkitEntity) {
        try {
            if (bukkitEntity == null) {
                return false;
            }

            String name = bukkitEntity.getName();
            if (name != null && name.startsWith(ZNPC_PREFIX)) {
                return true;
            }

            if (bukkitEntity.customName() != null) {
                String customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(bukkitEntity.customName());

                if (customName.startsWith(ZNPC_PREFIX)) {
                    return true;
                }
            }

        } catch (Throwable t) {
            debugLog("Error checking ZNPC entity: " + t.getClass().getSimpleName());
        }

        return false;
    }

    private static boolean hasVirtualEntityMarkers(Entity entity) {
        try {
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            if (bukkitEntity == null) {
                return false;
            }

            return hasVirtualEntityMarkersFast(bukkitEntity);

        } catch (Throwable t) {
            debugLog("Error checking virtual entity markers: " + t.getClass().getSimpleName());
        }

        return false;
    }
    
    private static boolean hasVirtualEntityMarkersFast(org.bukkit.entity.Entity bukkitEntity) {
        try {
            if (bukkitEntity == null) {
                return false;
            }

            try {
                PersistentDataContainer pdc = bukkitEntity.getPersistentDataContainer();

                for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                    String keyStr = key.toString().toLowerCase(Locale.ROOT);
                    if (keyStr.contains("virtual") || keyStr.contains("fake") ||
                        keyStr.contains("packet") || keyStr.contains("npc") ||
                        keyStr.contains("hologram") || keyStr.contains("marker")) {
                        return true;
                    }
                }
            } catch (Throwable t) {
                debugLog("Error checking entity PDC: " + t.getClass().getSimpleName());
            }

        } catch (Throwable t) {
            debugLog("Error checking PDC markers: " + t.getClass().getSimpleName());
        }

        return false;
    }

    private static void debugLog(String message) {
        if (debugEnabled && logger != null) {
            logger.info("[AkiAsync-VirtualEntity] " + message);
        }
    }
}
