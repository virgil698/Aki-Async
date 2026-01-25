package org.virgil.akiasync.mixin.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathCache {

    private final Long2ObjectOpenHashMap<CachedPath> pathCache;

    private static final int CACHE_EXPIRE_TICKS = 100;

    private static final int MAX_CACHE_SIZE = 1000;

    public PathCache() {
        this.pathCache = new Long2ObjectOpenHashMap<>(256);
    }

    private long getCacheKey(BlockPos start, BlockPos target) {

        long startHash = ((long) start.getX() & 0xFFFFL) |
                        (((long) start.getY() & 0xFFFFL) << 16) |
                        (((long) start.getZ() & 0xFFFFL) << 32);
        long targetHash = ((long) target.getX() & 0xFFFFL) |
                         (((long) target.getY() & 0xFFFFL) << 16) |
                         (((long) target.getZ() & 0xFFFFL) << 48);
        return startHash ^ targetHash;
    }

    public Path getCachedPath(BlockPos start, BlockPos target, long currentTick) {
        long key = getCacheKey(start, target);
        CachedPath cached = pathCache.get(key);

        if (cached != null) {

            if (currentTick - cached.createdTick < CACHE_EXPIRE_TICKS) {
                cached.hitCount++;
                return cached.path;
            } else {

                pathCache.remove(key);
            }
        }

        return null;
    }

    public void cachePath(BlockPos start, BlockPos target, Path path, long currentTick) {
        if (path == null) {
            return;
        }

        if (pathCache.size() >= MAX_CACHE_SIZE) {
            cleanOldEntries(currentTick);
        }

        long key = getCacheKey(start, target);
        pathCache.put(key, new CachedPath(path, currentTick));
    }

    private void cleanOldEntries(long currentTick) {
        pathCache.long2ObjectEntrySet().removeIf(entry -> {
            if (entry == null || entry.getValue() == null) {
                return true;
            }
            return currentTick - entry.getValue().createdTick >= CACHE_EXPIRE_TICKS;
        });

        if (pathCache.size() >= MAX_CACHE_SIZE) {
            long minHits = Long.MAX_VALUE;
            long keyToRemove = -1;

            for (var entry : pathCache.long2ObjectEntrySet()) {
                if (entry.getValue().hitCount < minHits) {
                    minHits = entry.getValue().hitCount;
                    keyToRemove = entry.getLongKey();
                }
            }

            if (keyToRemove != -1) {
                pathCache.remove(keyToRemove);
            }
        }
    }

    public void clear() {
        pathCache.clear();
    }

    public String getStats() {
        int totalHits = 0;
        for (CachedPath cached : pathCache.values()) {
            totalHits += cached.hitCount;
        }
        return String.format("Cached paths: %d, Total hits: %d", pathCache.size(), totalHits);
    }

    private static class CachedPath {
        final Path path;
        final long createdTick;
        int hitCount;

        CachedPath(Path path, long createdTick) {
            this.path = path;
            this.createdTick = createdTick;
            this.hitCount = 0;
        }
    }
}
