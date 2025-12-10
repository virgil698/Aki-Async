package org.virgil.akiasync.mixin.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 统一的碰撞排除列表缓存
 * 
 * 避免多个Mixin重复加载配置
 * 使用volatile + double-check locking保证线程安全
 * 
 * @author AkiAsync
 */
public class CollisionExclusionCache {
    
    private static volatile Set<String> excludedEntities = null;
    private static volatile long lastLoadTime = 0;
    private static final long CACHE_LIFETIME_MS = 60000; 
    
    /**
     * 获取排除列表（带缓存）
     * 
     * @return 排除的实体ID集合
     */
    public static Set<String> getExcludedEntities() {
        
        Set<String> cached = excludedEntities;
        long currentTime = System.currentTimeMillis();
        
        if (cached != null && (currentTime - lastLoadTime) < CACHE_LIFETIME_MS) {
            return cached;
        }
        
        synchronized (CollisionExclusionCache.class) {
            
            cached = excludedEntities;
            currentTime = System.currentTimeMillis();
            
            if (cached != null && (currentTime - lastLoadTime) < CACHE_LIFETIME_MS) {
                return cached;
            }
            
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                Set<String> loaded = bridge.getCollisionExcludedEntities();
                excludedEntities = loaded != null ? loaded : Collections.emptySet();
                lastLoadTime = currentTime;
                
                BridgeConfigCache.debugLog("[CollisionExclusionCache] Loaded " + 
                    excludedEntities.size() + " excluded entities");
            } else {
                excludedEntities = Collections.emptySet();
                lastLoadTime = currentTime;
            }
            
            return excludedEntities;
        }
    }
    
    /**
     * 检查实体是否在排除列表中
     * 
     * @param entity 要检查的实体
     * @return true = 在排除列表中
     */
    public static boolean isExcluded(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return false;
        }
        
        String entityId = entity.getEncodeId();
        if (entityId == null) {
            return false;
        }
        
        Set<String> excluded = getExcludedEntities();
        return excluded.contains(entityId);
    }
    
    /**
     * 清空缓存（配置重载时调用）
     */
    public static void clearCache() {
        synchronized (CollisionExclusionCache.class) {
            excludedEntities = null;
            lastLoadTime = 0;
            
            BridgeConfigCache.debugLog("[CollisionExclusionCache] Cache cleared");
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public static String getStats() {
        Set<String> cached = excludedEntities;
        long age = System.currentTimeMillis() - lastLoadTime;
        
        if (cached == null) {
            return "Cache: empty";
        }
        
        return String.format("Cache: %d entities, age: %dms", 
            cached.size(), age);
    }
}
