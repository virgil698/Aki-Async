package org.virgil.akiasync.compat;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.virgil.akiasync.util.DebugLogger;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FancyNpcsDetector implements PluginDetector {

    private static final String PLUGIN_NAME = "FancyNpcs";
    private static final int PRIORITY = 90;
    private static final long CACHE_TTL_MS = 5000;
    private static final int MAX_CACHE_SIZE = 1000;

    private final ConcurrentHashMap<UUID, CacheEntry> detectionCache;
    private volatile boolean pluginAvailable;
    private volatile boolean useAPI;
    private volatile Object fancyNpcsAPI;

    public FancyNpcsDetector() {
        this.detectionCache = new ConcurrentHashMap<>();
        this.pluginAvailable = checkPluginAvailability();
        this.useAPI = this.pluginAvailable;

        if (this.pluginAvailable) {
            try {
                initializeAPI();
            } catch (Exception e) {
                DebugLogger.error("[FancyNpcs Compat] Failed to initialize FancyNpcs API: %s", e.getMessage());
                this.useAPI = false;
            }
        }

        DebugLogger.debug("[FancyNpcs Compat] Initialized - Available: %s, UseAPI: %s",
            pluginAvailable, useAPI);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isAvailable() {
        return pluginAvailable;
    }

    @Override
    public boolean isVirtualEntity(Entity entity) {
        if (entity == null) {
            return false;
        }

        UUID entityId = entity.getUniqueId();
        CacheEntry cached = detectionCache.get(entityId);
        if (cached != null && !cached.isExpired()) {
            DebugLogger.debug("[FancyNpcs Compat] Cache hit for entity %s: %s",
                entityId, cached.isVirtual);
            return cached.isVirtual;
        }

        boolean isVirtual = false;

        if (useAPI) {
            try {
                isVirtual = detectViaAPI(entity);
                if (isVirtual) {
                    DebugLogger.debug("[FancyNpcs Compat] Detected FancyNpcs NPC entity via API: %s",
                        entityId);
                }
            } catch (Exception e) {
                DebugLogger.debug("[FancyNpcs Compat] API detection failed for %s, falling back: %s",
                    entityId, e.getMessage());
                isVirtual = detectViaFallback(entity);
            }
        } else {
            isVirtual = detectViaFallback(entity);
            if (isVirtual) {
                DebugLogger.debug("[FancyNpcs Compat] Detected FancyNpcs NPC entity via fallback: %s",
                    entityId);
            }
        }

        cacheResult(entityId, isVirtual);

        return isVirtual;
    }

    @Override
    public boolean detectViaAPI(Entity entity) {
        if (!useAPI || fancyNpcsAPI == null) {
            return false;
        }

        if (!(entity instanceof Player)) {
            return false;
        }

        try {

            Class<?> apiProviderClass = Class.forName("de.oliver.fancynpcs.api.NpcApiProvider");
            Object apiInstance = apiProviderClass.getMethod("get").invoke(null);

            Class<?> apiClass = apiInstance.getClass();
            Object npcTracker = apiClass.getMethod("getNpcTracker").invoke(apiInstance);

            Class<?> trackerClass = npcTracker.getClass();
            Object npcData = trackerClass.getMethod("getNpc", Entity.class).invoke(npcTracker, entity);

            if (npcData != null) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            DebugLogger.debug("[FancyNpcs Compat] FancyNpcs API class not found, disabling API detection");
            useAPI = false;
        } catch (Exception e) {
            DebugLogger.debug("[FancyNpcs Compat] API detection error: %s", e.getMessage());
        }

        return false;
    }

    @Override
    public boolean detectViaFallback(Entity entity) {

        if (!(entity instanceof Player)) {
            return false;
        }

        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            for (NamespacedKey key : pdc.getKeys()) {
                String keyStr = key.toString().toLowerCase(Locale.ROOT);
                if (keyStr.contains("fancynpcs") || keyStr.contains("fancy")) {
                    DebugLogger.debug("[FancyNpcs Compat] Found FancyNpcs PDC key: %s", keyStr);
                    return true;
                }
            }
        } catch (Exception e) {
            DebugLogger.debug("[FancyNpcs Compat] Error checking entity PDC: %s", e.getMessage());
        }

        try {
            if (entity.hasMetadata("fancynpcs") || entity.hasMetadata("npc")) {
                DebugLogger.debug("[FancyNpcs Compat] Found FancyNpcs metadata marker");
                return true;
            }
        } catch (Exception e) {
            DebugLogger.debug("[FancyNpcs Compat] Error checking entity metadata: %s", e.getMessage());
        }

        return false;
    }

    public void clearCache() {
        detectionCache.clear();
        DebugLogger.debug("[FancyNpcs Compat] Detection cache cleared");
    }

    public void setUseAPI(boolean useAPI) {
        this.useAPI = useAPI && pluginAvailable;
        DebugLogger.debug("[FancyNpcs Compat] API detection set to: %s", this.useAPI);
    }

    private boolean checkPluginAvailability() {
        try {
            Class.forName("de.oliver.fancynpcs.api.NpcApiProvider");
            boolean pluginEnabled = Bukkit.getPluginManager().getPlugin("FancyNpcs") != null;
            return pluginEnabled;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void initializeAPI() throws Exception {
        Class<?> apiProviderClass = Class.forName("de.oliver.fancynpcs.api.NpcApiProvider");
        fancyNpcsAPI = apiProviderClass.getMethod("get").invoke(null);
    }

    private void cacheResult(UUID entityId, boolean isVirtual) {

        if (detectionCache.size() >= MAX_CACHE_SIZE) {
            evictExpiredEntries();
        }

        detectionCache.put(entityId, new CacheEntry(isVirtual, System.currentTimeMillis() + CACHE_TTL_MS));
    }

    private void evictExpiredEntries() {
        long now = System.currentTimeMillis();
        detectionCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private static class CacheEntry {
        final boolean isVirtual;
        final long expiryTime;

        CacheEntry(boolean isVirtual, long expiryTime) {
            this.isVirtual = isVirtual;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        boolean isExpired(long currentTime) {
            return currentTime > expiryTime;
        }
    }
}
