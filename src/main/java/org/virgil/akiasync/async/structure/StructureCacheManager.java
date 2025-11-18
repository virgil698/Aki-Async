package org.virgil.akiasync.async.structure;

import net.minecraft.core.BlockPos;
import org.virgil.akiasync.AkiAsyncPlugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 结构缓存管理器
 * 
 * 功能：
 * 1. 管理结构位置缓存和负结果缓存
 * 2. 自动过期清理
 * 3. 缓存统计和监控
 * 4. 内存使用优化
 */
public class StructureCacheManager {
    
    private static StructureCacheManager instance;
    private final AkiAsyncPlugin plugin;
    
    // 缓存存储
    private final ConcurrentHashMap<String, CacheEntry> structureCache;
    private final ConcurrentHashMap<String, Long> negativeCache;
    
    // 缓存配置
    private volatile int maxCacheSize;
    private volatile long expirationMinutes;
    private volatile boolean cachingEnabled;
    
    // 清理任务
    private final ScheduledExecutorService cleanupExecutor;
    
    // 统计信息
    private volatile long cacheHits = 0;
    private volatile long cacheMisses = 0;
    private volatile long negativeHits = 0;
    
    private StructureCacheManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        this.structureCache = new ConcurrentHashMap<>();
        this.negativeCache = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AkiAsync-StructureCache-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        updateConfiguration();
        startCleanupTask();
    }
    
    public static synchronized StructureCacheManager getInstance(AkiAsyncPlugin plugin) {
        if (instance == null) {
            instance = new StructureCacheManager(plugin);
        }
        return instance;
    }
    
    public static StructureCacheManager getInstance() {
        return instance;
    }
    
    /**
     * 更新缓存配置
     */
    public void updateConfiguration() {
        if (plugin.getBridge() != null) {
            this.cachingEnabled = plugin.getBridge().isStructureCachingEnabled();
            this.maxCacheSize = plugin.getBridge().getStructureCacheMaxSize();
            this.expirationMinutes = plugin.getBridge().getStructureCacheExpirationMinutes();
        } else {
            this.cachingEnabled = true;
            this.maxCacheSize = 1000;
            this.expirationMinutes = 30;
        }
    }
    
    /**
     * 获取缓存的结构位置
     */
    public BlockPos getCachedStructure(String cacheKey) {
        if (!cachingEnabled) {
            return null;
        }
        
        CacheEntry entry = structureCache.get(cacheKey);
        if (entry != null) {
            if (isExpired(entry)) {
                structureCache.remove(cacheKey);
                return null;
            }
            cacheHits++;
            return entry.position;
        }
        
        cacheMisses++;
        return null;
    }
    
    /**
     * 缓存结构位置
     */
    public void cacheStructure(String cacheKey, BlockPos position) {
        if (!cachingEnabled) {
            return;
        }
        
        // 检查缓存大小限制
        if (structureCache.size() >= maxCacheSize) {
            evictOldestEntries();
        }
        
        CacheEntry entry = new CacheEntry(position, System.currentTimeMillis());
        structureCache.put(cacheKey, entry);
    }
    
    /**
     * 检查是否为负结果（没有找到结构）
     */
    public boolean isNegativeCached(String cacheKey) {
        if (!cachingEnabled) {
            return false;
        }
        
        Long timestamp = negativeCache.get(cacheKey);
        if (timestamp != null) {
            if (isExpired(timestamp)) {
                negativeCache.remove(cacheKey);
                return false;
            }
            negativeHits++;
            return true;
        }
        
        return false;
    }
    
    /**
     * 缓存负结果
     */
    public void cacheNegativeResult(String cacheKey) {
        if (!cachingEnabled) {
            return;
        }
        
        // 检查缓存大小限制
        if (negativeCache.size() >= maxCacheSize) {
            evictOldestNegativeEntries();
        }
        
        negativeCache.put(cacheKey, System.currentTimeMillis());
    }
    
    /**
     * 清理所有缓存
     */
    public void clearCache() {
        structureCache.clear();
        negativeCache.clear();
        resetStatistics();
        
        if (plugin.getBridge() != null && plugin.getBridge().isStructureLocationDebugEnabled()) {
            plugin.getLogger().info("[AkiAsync] Structure cache cleared");
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getStatistics() {
        return new CacheStatistics(
            structureCache.size(),
            negativeCache.size(),
            cacheHits,
            cacheMisses,
            negativeHits,
            calculateHitRate()
        );
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        cacheHits = 0;
        cacheMisses = 0;
        negativeHits = 0;
    }
    
    /**
     * 关闭缓存管理器
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        clearCache();
        instance = null;
    }
    
    /**
     * 检查缓存条目是否过期
     */
    private boolean isExpired(CacheEntry entry) {
        return isExpired(entry.timestamp);
    }
    
    private boolean isExpired(long timestamp) {
        long expirationTime = expirationMinutes * 60 * 1000; // 转换为毫秒
        return System.currentTimeMillis() - timestamp > expirationTime;
    }
    
    /**
     * 驱逐最旧的缓存条目
     */
    private void evictOldestEntries() {
        if (structureCache.size() < maxCacheSize * 0.8) {
            return; // 只有当缓存接近满时才清理
        }
        
        long oldestTime = System.currentTimeMillis();
        String oldestKey = null;
        
        for (var entry : structureCache.entrySet()) {
            if (entry.getValue().timestamp < oldestTime) {
                oldestTime = entry.getValue().timestamp;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            structureCache.remove(oldestKey);
        }
    }
    
    private void evictOldestNegativeEntries() {
        if (negativeCache.size() < maxCacheSize * 0.8) {
            return;
        }
        
        long oldestTime = System.currentTimeMillis();
        String oldestKey = null;
        
        for (var entry : negativeCache.entrySet()) {
            if (entry.getValue() < oldestTime) {
                oldestTime = entry.getValue();
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            negativeCache.remove(oldestKey);
        }
    }
    
    /**
     * 启动定期清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(this::performCleanup, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 执行清理任务
     */
    private void performCleanup() {
        if (!cachingEnabled) {
            return;
        }
        
        int removedStructures = 0;
        int removedNegative = 0;
        
        // 清理过期的结构缓存
        var structureIterator = structureCache.entrySet().iterator();
        while (structureIterator.hasNext()) {
            var entry = structureIterator.next();
            if (isExpired(entry.getValue())) {
                structureIterator.remove();
                removedStructures++;
            }
        }
        
        // 清理过期的负缓存
        var negativeIterator = negativeCache.entrySet().iterator();
        while (negativeIterator.hasNext()) {
            var entry = negativeIterator.next();
            if (isExpired(entry.getValue())) {
                negativeIterator.remove();
                removedNegative++;
            }
        }
        
        if (plugin.getBridge() != null && plugin.getBridge().isStructureLocationDebugEnabled()) {
            if (removedStructures > 0 || removedNegative > 0) {
                plugin.getLogger().info(String.format(
                    "[AkiAsync] Cache cleanup: removed %d structure entries, %d negative entries",
                    removedStructures, removedNegative
                ));
            }
        }
    }
    
    /**
     * 计算缓存命中率
     */
    private double calculateHitRate() {
        long totalRequests = cacheHits + cacheMisses;
        return totalRequests > 0 ? (double) cacheHits / totalRequests * 100.0 : 0.0;
    }
    
    /**
     * 缓存条目类
     */
    private static class CacheEntry {
        final BlockPos position;
        final long timestamp;
        
        CacheEntry(BlockPos position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 缓存统计信息类
     */
    public static class CacheStatistics {
        public final int structureCacheSize;
        public final int negativeCacheSize;
        public final long cacheHits;
        public final long cacheMisses;
        public final long negativeHits;
        public final double hitRate;
        
        CacheStatistics(int structureCacheSize, int negativeCacheSize, 
                       long cacheHits, long cacheMisses, long negativeHits, double hitRate) {
            this.structureCacheSize = structureCacheSize;
            this.negativeCacheSize = negativeCacheSize;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.negativeHits = negativeHits;
            this.hitRate = hitRate;
        }
        
        @Override
        public String toString() {
            return String.format(
                "StructureCache[structures=%d, negative=%d, hits=%d, misses=%d, negativeHits=%d, hitRate=%.2f%%]",
                structureCacheSize, negativeCacheSize, cacheHits, cacheMisses, negativeHits, hitRate
            );
        }
    }
}
