package org.virgil.akiasync.async.structure;

import net.minecraft.core.BlockPos;
import org.virgil.akiasync.AkiAsyncPlugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class StructureCacheManager {

    private static StructureCacheManager instance;
    private final AkiAsyncPlugin plugin;

    private final ConcurrentHashMap<String, CacheEntry> structureCache;
    private final ConcurrentHashMap<String, Long> negativeCache;

    private volatile int maxCacheSize;
    private volatile long expirationMinutes;
    private volatile boolean cachingEnabled;

    private final ScheduledExecutorService cleanupExecutor;

    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong negativeHits = new AtomicLong(0);

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

    public static synchronized StructureCacheManager getInstance() {
        return instance;
    }

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
            cacheHits.incrementAndGet();
            return entry.position;
        }

        cacheMisses.incrementAndGet();
        return null;
    }

    public void cacheStructure(String cacheKey, BlockPos position) {
        if (!cachingEnabled) {
            return;
        }

        if (structureCache.size() >= maxCacheSize) {
            evictOldestEntries();
        }

        CacheEntry entry = new CacheEntry(position, System.currentTimeMillis());
        structureCache.put(cacheKey, entry);
    }

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
            negativeHits.incrementAndGet();
            return true;
        }

        return false;
    }

    public void cacheNegativeResult(String cacheKey) {
        if (!cachingEnabled) {
            return;
        }

        if (negativeCache.size() >= maxCacheSize) {
            evictOldestNegativeEntries();
        }

        negativeCache.put(cacheKey, System.currentTimeMillis());
    }

    public void clearCache() {
        structureCache.clear();
        negativeCache.clear();
        resetStatistics();

        if (plugin.getBridge() != null && plugin.getBridge().isStructureLocationDebugEnabled()) {
            plugin.getLogger().info("[AkiAsync] Structure cache cleared");
        }
    }

    public CacheStatistics getStatistics() {
        return new CacheStatistics(
            structureCache.size(),
            negativeCache.size(),
            cacheHits.get(),
            cacheMisses.get(),
            negativeHits.get(),
            calculateHitRate()
        );
    }

    public void resetStatistics() {
        cacheHits.set(0);
        cacheMisses.set(0);
        negativeHits.set(0);
    }

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
        synchronized (StructureCacheManager.class) {
            instance = null;
        }
    }

    private boolean isExpired(CacheEntry entry) {
        return isExpired(entry.timestamp);
    }

    private boolean isExpired(long timestamp) {
        long expirationTime = expirationMinutes * 60 * 1000;
        return System.currentTimeMillis() - timestamp > expirationTime;
    }

    private void evictOldestEntries() {
        if (structureCache.size() < maxCacheSize) {
            return;
        }


        int toRemove = Math.max(1, (int)(maxCacheSize * 0.1));
        
        structureCache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
            .limit(toRemove)
            .map(java.util.Map.Entry::getKey)
            .collect(Collectors.toList())
            .forEach(structureCache::remove);
        
        if (plugin.getBridge() != null && plugin.getBridge().isStructureLocationDebugEnabled()) {
            plugin.getLogger().info(String.format(
                "[AkiAsync-StructureCache] Evicted %d old structure entries, current: %d/%d",
                toRemove, structureCache.size(), maxCacheSize
            ));
        }
    }

    private void evictOldestNegativeEntries() {
        if (negativeCache.size() < maxCacheSize) {
            return;
        }


        int toRemove = Math.max(1, (int)(maxCacheSize * 0.1));
        
        negativeCache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()))
            .limit(toRemove)
            .map(java.util.Map.Entry::getKey)
            .collect(Collectors.toList())
            .forEach(negativeCache::remove);
        
        if (plugin.getBridge() != null && plugin.getBridge().isStructureLocationDebugEnabled()) {
            plugin.getLogger().info(String.format(
                "[AkiAsync-StructureCache] Evicted %d old negative entries, current: %d/%d",
                toRemove, negativeCache.size(), maxCacheSize
            ));
        }
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(this::performCleanup, 5, 5, TimeUnit.MINUTES);
    }

    private void performCleanup() {
        if (!cachingEnabled) {
            return;
        }

        int removedStructures = 0;
        int removedNegative = 0;

        var structureIterator = structureCache.entrySet().iterator();
        while (structureIterator.hasNext()) {
            var entry = structureIterator.next();
            if (isExpired(entry.getValue())) {
                structureIterator.remove();
                removedStructures++;
            }
        }

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

    private double calculateHitRate() {
        long totalRequests = cacheHits.get() + cacheMisses.get();
        return totalRequests > 0 ? (double) cacheHits.get() / totalRequests * 100.0 : 0.0;
    }

    private static class CacheEntry {
        final BlockPos position;
        final long timestamp;

        CacheEntry(BlockPos position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }
    }

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
