package org.virgil.akiasync.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.virgil.akiasync.AkiAsyncPlugin;

public class CacheManager {

    private final AkiAsyncPlugin plugin;
    private final Map<String, CacheEntry> globalCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10000;
    private static final long DEFAULT_EXPIRATION_MS = 30 * 60 * 1000;

    public CacheManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        startPeriodicCleanup();
    }
    
    private void startPeriodicCleanup() {
        
        try {
            java.util.concurrent.ScheduledExecutorService scheduler = 
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "AkiAsync-CacheCleanup");
                    t.setDaemon(true);
                    return t;
                });
            
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    cleanupExpired();
                    org.virgil.akiasync.cache.SakuraCacheStatistics.performPeriodicCleanup();
                    
                    if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                        plugin.getLogger().info("[AkiAsync-Cache] Periodic cleanup completed");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[AkiAsync-Cache] Error during periodic cleanup: " + e.getMessage());
                }
            }, 5, 300, java.util.concurrent.TimeUnit.SECONDS);
            
            plugin.getLogger().info("[AkiAsync-Cache] Periodic cleanup scheduled using thread pool");
        } catch (Exception e) {
            plugin.getLogger().severe("[AkiAsync-Cache] Failed to start periodic cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void invalidateAll() {
        plugin.getLogger().info("[AkiAsync] Invalidating all caches...");

        globalCache.clear();

        plugin.getExecutorManager().getExecutorService().execute(() -> {
            try {
                org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor.clearOldCache(Long.MAX_VALUE);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to clear villager breed cache: " + e.getMessage());
            }

            try {
                org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor.resetStatistics();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to reset brain executor statistics: " + e.getMessage());
            }
            
            try {
                clearSakuraOptimizationCaches();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to clear Sakura optimization caches: " + e.getMessage());
            }
        });

        plugin.getLogger().info("[AkiAsync] Main caches cleared, controlled cleanup in progress");
    }
    
    private void clearSakuraOptimizationCaches() {

        try {
            if (plugin.getBridge() != null) {
                plugin.getBridge().clearSakuraOptimizationCaches();
                plugin.getLogger().info("[AkiAsync] Cleared Sakura optimization caches");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to clear Sakura optimization caches: " + e.getMessage());
        }
    }

    public void put(String key, Object value) {
        put(key, value, DEFAULT_EXPIRATION_MS);
    }

    public void put(String key, Object value, long expirationMs) {

        if (globalCache.size() >= MAX_CACHE_SIZE) {
            evictOldEntries();
        }
        
        globalCache.put(key, new CacheEntry(value, System.currentTimeMillis(), expirationMs));
    }

    public Object get(String key) {
        CacheEntry entry = globalCache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            globalCache.remove(key);
            return null;
        }
        
        return entry.value;
    }

    public Object remove(String key) {
        return globalCache.remove(key);
    }

    public void clear() {
        globalCache.clear();
    }

    public int size() {
        return globalCache.size();
    }

    public boolean containsKey(String key) {
        CacheEntry entry = globalCache.get(key);
        if (entry != null && entry.isExpired()) {
            globalCache.remove(key);
            return false;
        }
        return entry != null;
    }
    
    private void evictOldEntries() {
        int toRemove = MAX_CACHE_SIZE / 10;
        
        globalCache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
            .limit(toRemove)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList())
            .forEach(globalCache::remove);
        
        if (plugin.getConfigManager().isDebugLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                "[AkiAsync-Cache] Evicted %d old entries, current size: %d/%d",
                toRemove, globalCache.size(), MAX_CACHE_SIZE
            ));
        }
    }
    
    public void cleanupExpired() {
        int removed = 0;
        var iterator = globalCache.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0 && plugin.getConfigManager().isDebugLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                "[AkiAsync-Cache] Cleaned up %d expired entries",
                removed
            ));
        }
    }
    
    public String getAllCacheStatistics() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("§6=== AkiAsync Cache Statistics ===§r\n");
        sb.append(String.format("§e[Global Cache]§r\n  §7Size: §f%d/%d§r\n", 
            globalCache.size(), MAX_CACHE_SIZE));
        
        sb.append("\n");
        sb.append(org.virgil.akiasync.cache.SakuraCacheStatistics.formatStatistics());
        
        return sb.toString();
    }
    
    private static class CacheEntry {
        final Object value;
        final long timestamp;
        final long expirationMs;
        
        CacheEntry(Object value, long timestamp, long expirationMs) {
            this.value = value;
            this.timestamp = timestamp;
            this.expirationMs = expirationMs;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > expirationMs;
        }
    }
}
