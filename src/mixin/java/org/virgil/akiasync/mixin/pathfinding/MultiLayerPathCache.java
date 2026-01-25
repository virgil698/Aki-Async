package org.virgil.akiasync.mixin.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MultiLayerPathCache {

    private int hotCacheSize = 200;
    private int warmCacheSize = 500;
    private int coldCacheSize = 1000;

    private long hotExpireMs = 10_000;
    private long warmExpireMs = 30_000;
    private long coldExpireMs = 60_000;

    private int similarityTolerance = 3;

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
    private long cleanupIntervalMs = 5000;

    public MultiLayerPathCache() {
        updateConfigFromBridge();
    }

    public void updateConfigFromBridge() {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isAsyncPathfindingCacheEnabled()) {
            int maxSize = bridge.getAsyncPathfindingCacheMaxSize();
            int expireSeconds = bridge.getAsyncPathfindingCacheExpireSeconds();
            int tolerance = bridge.getAsyncPathfindingCacheReuseTolerance();
            int cleanupSeconds = bridge.getAsyncPathfindingCacheCleanupIntervalSeconds();

            this.hotCacheSize = Math.max(50, maxSize / 5);
            this.warmCacheSize = Math.max(100, maxSize / 2);
            this.coldCacheSize = Math.max(200, maxSize);

            this.hotExpireMs = Math.max(5000, expireSeconds * 1000L / 3);
            this.warmExpireMs = Math.max(10000, expireSeconds * 1000L * 2 / 3);
            this.coldExpireMs = Math.max(15000, expireSeconds * 1000L);

            this.similarityTolerance = Math.max(1, tolerance);
            this.cleanupIntervalMs = Math.max(1000, cleanupSeconds * 1000L);
        }
    }

    public Path get(BlockPos start, BlockPos target) {
        PathCacheKey key = new PathCacheKey(start, target);

        CachedPath hotPath = hotCache.get(key);
        if (hotPath != null && !hotPath.isExpired(hotExpireMs)) {
            hotHits.incrementAndGet();
            hotPath.recordHit();
            return hotPath.getPath();
        }

        CachedPath warmPath = warmCache.get(key);
        if (warmPath != null && !warmPath.isExpired(warmExpireMs)) {
            warmHits.incrementAndGet();
            warmPath.recordHit();

            promoteToHot(key, warmPath);
            return warmPath.getPath();
        }

        CachedPath coldPath = coldCache.get(key);
        if (coldPath != null && !coldPath.isExpired(coldExpireMs)) {
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

    public void put(BlockPos start, BlockPos target, Path path) {
        if (path == null || !path.canReach()) {
            return;
        }

        PathCacheKey key = new PathCacheKey(start, target);
        CachedPath cachedPath = new CachedPath(path);

        if (hotCache.size() >= hotCacheSize) {
            evictFromHot();
        }
        hotCache.put(key, cachedPath);

        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > cleanupIntervalMs) {
            cleanupExpired();
            lastCleanupTime = now;
        }
    }

    private void promoteToHot(PathCacheKey key, CachedPath path) {
        warmCache.remove(key);

        if (hotCache.size() >= hotCacheSize) {
            evictFromHot();
        }

        hotCache.put(key, path.refresh());
        promotions.incrementAndGet();
    }

    private void promoteToWarm(PathCacheKey key, CachedPath path) {
        coldCache.remove(key);

        if (warmCache.size() >= warmCacheSize) {
            evictFromWarm();
        }

        warmCache.put(key, path.refresh());
        promotions.incrementAndGet();
    }

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

            if (warmCache.size() < warmCacheSize) {
                warmCache.put(lruEntry.getKey(), lruEntry.getValue());
                demotions.incrementAndGet();
            }
        }
    }

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

            if (coldCache.size() < coldCacheSize) {
                coldCache.put(lruEntry.getKey(), lruEntry.getValue());
                demotions.incrementAndGet();
            }
        }
    }

    private Path findSimilarPath(PathCacheKey targetKey) {

        for (Map.Entry<PathCacheKey, CachedPath> entry : hotCache.entrySet()) {
            if (entry.getKey().isSimilar(targetKey, similarityTolerance)) {
                if (!entry.getValue().isExpired(hotExpireMs)) {
                    return entry.getValue().getPath();
                }
            }
        }

        for (Map.Entry<PathCacheKey, CachedPath> entry : warmCache.entrySet()) {
            if (entry.getKey().isSimilar(targetKey, similarityTolerance)) {
                if (!entry.getValue().isExpired(warmExpireMs)) {
                    return entry.getValue().getPath();
                }
            }
        }

        return null;
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();

        hotCache.entrySet().removeIf(entry -> {
            CachedPath cached = entry.getValue();
            if (cached == null) {
                return true;
            }
            if (cached.isExpired(hotExpireMs)) {
                if (warmCache.size() < warmCacheSize) {
                    warmCache.put(entry.getKey(), cached);
                }
                return true;
            }
            return false;
        });

        warmCache.entrySet().removeIf(entry -> {
            CachedPath cached = entry.getValue();
            if (cached == null) {
                return true;
            }
            if (cached.isExpired(warmExpireMs)) {
                if (coldCache.size() < coldCacheSize) {
                    coldCache.put(entry.getKey(), cached);
                }
                return true;
            }
            return false;
        });

        coldCache.entrySet().removeIf(entry -> {
            CachedPath cached = entry.getValue();
            return cached == null || cached.isExpired(coldExpireMs);
        });
    }

    public void clear() {
        hotCache.clear();
        warmCache.clear();
        coldCache.clear();
    }

    public String getStatistics() {
        long totalHits = hotHits.get() + warmHits.get() + coldHits.get();
        long totalRequests = totalHits + misses.get();
        double hitRate = totalRequests > 0 ? (totalHits * 100.0 / totalRequests) : 0.0;

        return String.format(
            "Cache[Hot=%d/%d,Warm=%d/%d,Cold=%d/%d] Hits[H=%d,W=%d,C=%d] Rate=%.1f%% P=%d D=%d",
            hotCache.size(), hotCacheSize,
            warmCache.size(), warmCacheSize,
            coldCache.size(), coldCacheSize,
            hotHits.get(), warmHits.get(), coldHits.get(),
            hitRate,
            promotions.get(), demotions.get()
        );
    }

    private static class CachedPath {
        private final Path path;
        private final long createTime;
        private volatile long lastAccessTime;
        private final java.util.concurrent.atomic.AtomicInteger hitCount;

        CachedPath(Path path) {
            this.path = path;
            this.createTime = System.currentTimeMillis();
            this.lastAccessTime = createTime;
            this.hitCount = new java.util.concurrent.atomic.AtomicInteger(0);
        }

        Path getPath() {
            return path;
        }

        void recordHit() {
            this.lastAccessTime = System.currentTimeMillis();
            this.hitCount.incrementAndGet();
        }

        boolean isExpired(long expireMs) {
            return System.currentTimeMillis() - createTime > expireMs;
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }

        int getHitCount() {
            return hitCount.get();
        }

        CachedPath refresh() {
            this.lastAccessTime = System.currentTimeMillis();
            return this;
        }
    }
}
