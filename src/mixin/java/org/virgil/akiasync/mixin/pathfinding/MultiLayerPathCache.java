package org.virgil.akiasync.mixin.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多层路径缓存系统
 * 
 * 三层缓存架构：
 * 1. 热缓存（Hot Cache）- 最近使用，命中率最高，容量小
 * 2. 温缓存（Warm Cache）- 中等使用频率，容量中等
 * 3. 冷缓存（Cold Cache）- 低频使用，容量大，长期保存
 * 
 * 缓存晋升策略：
 * - 冷缓存命中 → 晋升到温缓存
 * - 温缓存命中 → 晋升到热缓存
 * - 热缓存命中 → 刷新时间戳
 * 
 * 缓存降级策略：
 * - 热缓存过期 → 降级到温缓存
 * - 温缓存过期 → 降级到冷缓存
 * - 冷缓存过期 → 删除
 * 
 * @author AkiAsync
 */
public class MultiLayerPathCache {
    
    private static final int HOT_CACHE_SIZE = 200;      
    private static final int WARM_CACHE_SIZE = 500;     
    private static final int COLD_CACHE_SIZE = 1000;    
    
    private static final long HOT_EXPIRE_MS = 10_000;   
    private static final long WARM_EXPIRE_MS = 30_000;  
    private static final long COLD_EXPIRE_MS = 60_000;  
    
    private static final int SIMILARITY_TOLERANCE = 3;  
    
    private final Map<PathCacheKey, CachedPath> hotCache = new ConcurrentHashMap<>();
    private final Map<PathCacheKey, CachedPath> warmCache = new ConcurrentHashMap<>();
    private final Map<PathCacheKey, CachedPath> coldCache = new ConcurrentHashMap<>();
    
    private final AtomicLong hotHits = new AtomicLong(0);
    private final AtomicLong warmHits = new AtomicLong(0);
    private final AtomicLong coldHits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong promotions = new AtomicLong(0);
    private final AtomicLong demotions = new AtomicLong(0);
    
    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL_MS = 5000;
    
    /**
     * 获取缓存的路径
     */
    public Path get(BlockPos start, BlockPos target) {
        PathCacheKey key = new PathCacheKey(start, target);
        
        CachedPath hotPath = hotCache.get(key);
        if (hotPath != null && !hotPath.isExpired(HOT_EXPIRE_MS)) {
            hotHits.incrementAndGet();
            hotPath.recordHit();
            return hotPath.getPath();
        }
        
        CachedPath warmPath = warmCache.get(key);
        if (warmPath != null && !warmPath.isExpired(WARM_EXPIRE_MS)) {
            warmHits.incrementAndGet();
            warmPath.recordHit();
            
            promoteToHot(key, warmPath);
            return warmPath.getPath();
        }
        
        CachedPath coldPath = coldCache.get(key);
        if (coldPath != null && !coldPath.isExpired(COLD_EXPIRE_MS)) {
            coldHits.incrementAndGet();
            coldPath.recordHit();
            
            promoteToWarm(key, coldPath);
            return coldPath.getPath();
        }
        
        Path similarPath = findSimilarPath(key);
        if (similarPath != null) {
            warmHits.incrementAndGet();
            return similarPath;
        }
        
        misses.incrementAndGet();
        return null;
    }
    
    /**
     * 存储路径到缓存
     */
    public void put(BlockPos start, BlockPos target, Path path) {
        if (path == null || !path.canReach()) {
            return;
        }
        
        PathCacheKey key = new PathCacheKey(start, target);
        CachedPath cachedPath = new CachedPath(path);
        
        if (hotCache.size() >= HOT_CACHE_SIZE) {
            evictFromHot();
        }
        hotCache.put(key, cachedPath);
        
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            cleanupExpired();
            lastCleanupTime = now;
        }
    }
    
    /**
     * 晋升到热缓存
     */
    private void promoteToHot(PathCacheKey key, CachedPath path) {
        warmCache.remove(key);
        
        if (hotCache.size() >= HOT_CACHE_SIZE) {
            evictFromHot();
        }
        
        hotCache.put(key, path.refresh());
        promotions.incrementAndGet();
    }
    
    /**
     * 晋升到温缓存
     */
    private void promoteToWarm(PathCacheKey key, CachedPath path) {
        coldCache.remove(key);
        
        if (warmCache.size() >= WARM_CACHE_SIZE) {
            evictFromWarm();
        }
        
        warmCache.put(key, path.refresh());
        promotions.incrementAndGet();
    }
    
    /**
     * 从热缓存驱逐（降级到温缓存）
     */
    private void evictFromHot() {
        
        Map.Entry<PathCacheKey, CachedPath> lruEntry = null;
        long minLastAccess = Long.MAX_VALUE;
        
        for (Map.Entry<PathCacheKey, CachedPath> entry : hotCache.entrySet()) {
            if (entry.getValue().getLastAccessTime() < minLastAccess) {
                minLastAccess = entry.getValue().getLastAccessTime();
                lruEntry = entry;
            }
        }
        
        if (lruEntry != null) {
            hotCache.remove(lruEntry.getKey());
            
            if (warmCache.size() < WARM_CACHE_SIZE) {
                warmCache.put(lruEntry.getKey(), lruEntry.getValue());
                demotions.incrementAndGet();
            }
        }
    }
    
    /**
     * 从温缓存驱逐（降级到冷缓存）
     */
    private void evictFromWarm() {
        
        Map.Entry<PathCacheKey, CachedPath> lruEntry = null;
        long minLastAccess = Long.MAX_VALUE;
        
        for (Map.Entry<PathCacheKey, CachedPath> entry : warmCache.entrySet()) {
            if (entry.getValue().getLastAccessTime() < minLastAccess) {
                minLastAccess = entry.getValue().getLastAccessTime();
                lruEntry = entry;
            }
        }
        
        if (lruEntry != null) {
            warmCache.remove(lruEntry.getKey());
            
            if (coldCache.size() < COLD_CACHE_SIZE) {
                coldCache.put(lruEntry.getKey(), lruEntry.getValue());
                demotions.incrementAndGet();
            }
        }
    }
    
    /**
     * 查找相似路径
     */
    private Path findSimilarPath(PathCacheKey targetKey) {
        
        for (Map.Entry<PathCacheKey, CachedPath> entry : hotCache.entrySet()) {
            if (entry.getKey().isSimilar(targetKey, SIMILARITY_TOLERANCE)) {
                if (!entry.getValue().isExpired(HOT_EXPIRE_MS)) {
                    return entry.getValue().getPath();
                }
            }
        }
        
        for (Map.Entry<PathCacheKey, CachedPath> entry : warmCache.entrySet()) {
            if (entry.getKey().isSimilar(targetKey, SIMILARITY_TOLERANCE)) {
                if (!entry.getValue().isExpired(WARM_EXPIRE_MS)) {
                    return entry.getValue().getPath();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 清理过期条目
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        
        hotCache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(HOT_EXPIRE_MS)) {
                if (warmCache.size() < WARM_CACHE_SIZE) {
                    warmCache.put(entry.getKey(), entry.getValue());
                }
                return true;
            }
            return false;
        });
        
        warmCache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(WARM_EXPIRE_MS)) {
                if (coldCache.size() < COLD_CACHE_SIZE) {
                    coldCache.put(entry.getKey(), entry.getValue());
                }
                return true;
            }
            return false;
        });
        
        coldCache.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(COLD_EXPIRE_MS)
        );
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        hotCache.clear();
        warmCache.clear();
        coldCache.clear();
    }
    
    /**
     * 获取统计信息
     */
    public String getStatistics() {
        long totalHits = hotHits.get() + warmHits.get() + coldHits.get();
        long totalRequests = totalHits + misses.get();
        double hitRate = totalRequests > 0 ? (totalHits * 100.0 / totalRequests) : 0.0;
        
        return String.format(
            "Cache[Hot=%d/%d,Warm=%d/%d,Cold=%d/%d] Hits[H=%d,W=%d,C=%d] Rate=%.1f%% P=%d D=%d",
            hotCache.size(), HOT_CACHE_SIZE,
            warmCache.size(), WARM_CACHE_SIZE,
            coldCache.size(), COLD_CACHE_SIZE,
            hotHits.get(), warmHits.get(), coldHits.get(),
            hitRate,
            promotions.get(), demotions.get()
        );
    }
    
    /**
     * 缓存的路径
     */
    private static class CachedPath {
        private final Path path;
        private final long createTime;
        private volatile long lastAccessTime;
        private volatile int hitCount;
        
        CachedPath(Path path) {
            this.path = path;
            this.createTime = System.currentTimeMillis();
            this.lastAccessTime = createTime;
            this.hitCount = 0;
        }
        
        Path getPath() {
            return path;
        }
        
        void recordHit() {
            this.lastAccessTime = System.currentTimeMillis();
            this.hitCount++;
        }
        
        boolean isExpired(long expireMs) {
            return System.currentTimeMillis() - createTime > expireMs;
        }
        
        long getLastAccessTime() {
            return lastAccessTime;
        }
        
        int getHitCount() {
            return hitCount;
        }
        
        CachedPath refresh() {
            this.lastAccessTime = System.currentTimeMillis();
            return this;
        }
    }
}
