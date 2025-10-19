package org.virgil.akiasync.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.virgil.akiasync.AkiAsyncPlugin;

public class CacheManager {
    
    private final AkiAsyncPlugin plugin;
    private final Map<String, Object> globalCache = new ConcurrentHashMap<>();
    
    public CacheManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void invalidateAll() {
        plugin.getLogger().info("[AkiAsync] Invalidating all caches...");
        
        globalCache.clear();
        
        try {
            org.virgil.akiasync.mixin.async.hopper.HopperChainExecutor.clearOldCache(Long.MAX_VALUE);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to clear hopper chain cache: " + e.getMessage());
        }
        
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
        
        plugin.getLogger().info("[AkiAsync] All caches invalidated successfully");
    }
    
    public void put(String key, Object value) {
        globalCache.put(key, value);
    }
    
    public Object get(String key) {
        return globalCache.get(key);
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
        return globalCache.containsKey(key);
    }
}
