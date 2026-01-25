package org.virgil.akiasync.util;

import org.bukkit.entity.Entity;
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

        int entityId = entity.getEntityId();
        Boolean cached = virtualEntityCache.get(entityId);
        if (cached != null) return cached;

        if (isDefinitelyRealEntity(entity)) {
            virtualEntityCache.putIfAbsent(entityId, false);
            return false;
        }

        boolean isVirtual = isVirtualEntitySlow(entity);
        cacheResult(entityId, isVirtual);
        return isVirtual;
    }

    public static boolean isVirtualEntityQuick(Entity entity) {
        if (entity == null) return false;
        Boolean cached = virtualEntityCache.get(entity.getEntityId());
        return cached != null ? cached : isVirtualEntity(entity);
    }

    private static boolean isDefinitelyRealEntity(Entity entity) {

        if (entity instanceof org.bukkit.entity.Item) return true;
        if (entity instanceof org.bukkit.entity.Minecart) return true;
        if (entity instanceof org.bukkit.entity.FallingBlock) return true;
        if (entity instanceof org.bukkit.entity.Projectile) return true;
        if (entity instanceof org.bukkit.entity.TNTPrimed) return true;

        if (entity.getVelocity().lengthSquared() > 0.0001) return true;

        return false;
    }

    private static boolean isVirtualEntitySlow(Entity entity) {
        try {

            UUID uuid = entity.getUniqueId();

            if (detectorRegistry != null) {
                try {
                    if (detectorRegistry.isVirtualEntity(entity)) {
                        String source = detectorRegistry.getEntitySource(entity);
                        debugLog("Virtual entity detected via PluginDetectorRegistry: " + source + ", uuid=" + uuid);
                        return true;
                    }
                } catch (Throwable t) {
                    debugLog("Error using PluginDetectorRegistry: " + t.getMessage());
                }
            }

            if (isZNPCEntity(entity)) {
                debugLog("Virtual entity detected: ZNPCS NPC, uuid=" + uuid);
                return true;
            }

            if (hasVirtualEntityMarkers(entity)) {
                debugLog("Virtual entity detected: has virtual markers, uuid=" + uuid);
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
                    String source = detectorRegistry.getEntitySource(entity);
                    if (source != null) {
                        debugLog("Entity source determined: " + source + ", uuid=" + entity.getUniqueId());
                        return source;
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
            if (!(entity instanceof org.bukkit.entity.Player)) {
                return false;
            }

            String name = entity.getName();
            if (name != null && name.startsWith(ZNPC_PREFIX)) {
                return true;
            }

            net.kyori.adventure.text.Component customNameComponent = entity.customName();
            if (customNameComponent != null) {

                String customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(customNameComponent);

                if (customName != null && customName.startsWith(ZNPC_PREFIX)) {
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
            PersistentDataContainer pdc = entity.getPersistentDataContainer();

            for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                String keyStr = key.toString().toLowerCase(Locale.ROOT);
                if (keyStr.contains("virtual") || keyStr.contains("fake") ||
                    keyStr.contains("packet") || keyStr.contains("npc") ||
                    keyStr.contains("hologram") || keyStr.contains("marker")) {
                    return true;
                }
            }

        } catch (Throwable t) {
            debugLog("Error checking virtual entity markers: " + t.getClass().getSimpleName());
        }

        return false;
    }

    private static void debugLog(String message) {
        if (debugEnabled && logger != null) {
            logger.info("[AkiAsync-VirtualEntity] " + message);
        }
    }
}
