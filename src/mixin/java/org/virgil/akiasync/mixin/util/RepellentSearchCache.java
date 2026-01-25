package org.virgil.akiasync.mixin.util;

import net.minecraft.core.BlockPos;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RepellentSearchCache {

    private RepellentSearchCache() {}

    public record CachedResult(Optional<BlockPos> result, long tick) {}

    private static final ConcurrentHashMap<Long, CachedResult> PIGLIN_CACHE = new ConcurrentHashMap<>(1024);
    private static long piglinTickCounter = 0;

    private static final ConcurrentHashMap<Long, CachedResult> HOGLIN_CACHE = new ConcurrentHashMap<>(1024);
    private static long hoglinTickCounter = 0;

    private static volatile int cacheTtlTicks = 20;
    private static volatile int maxCacheSize = 2048;

    private static volatile long cacheHits = 0;
    private static volatile long cacheMisses = 0;
    private static volatile long cacheEvictions = 0;

    public static long incrementPiglinTick() {
        return ++piglinTickCounter;
    }

    public static CachedResult getPiglinCached(long key) {
        CachedResult cached = PIGLIN_CACHE.get(key);
        if (cached != null) {
            if ((piglinTickCounter - cached.tick) < cacheTtlTicks) {
                cacheHits++;
                return cached;
            }

            PIGLIN_CACHE.remove(key);
            cacheEvictions++;
        }
        cacheMisses++;
        return null;
    }

    public static void putPiglinCache(long key, Optional<BlockPos> result) {

        if (PIGLIN_CACHE.size() >= maxCacheSize) {

            long oldestKey = -1;
            long oldestTick = Long.MAX_VALUE;
            for (var entry : PIGLIN_CACHE.entrySet()) {
                if (entry.getValue().tick < oldestTick) {
                    oldestTick = entry.getValue().tick;
                    oldestKey = entry.getKey();
                }
            }
            if (oldestKey != -1) {
                PIGLIN_CACHE.remove(oldestKey);
                cacheEvictions++;
            }
        }
        PIGLIN_CACHE.put(key, new CachedResult(result, piglinTickCounter));
    }

    public static long incrementHoglinTick() {
        return ++hoglinTickCounter;
    }

    public static CachedResult getHoglinCached(long key) {
        CachedResult cached = HOGLIN_CACHE.get(key);
        if (cached != null) {
            if ((hoglinTickCounter - cached.tick) < cacheTtlTicks) {
                cacheHits++;
                return cached;
            }

            HOGLIN_CACHE.remove(key);
            cacheEvictions++;
        }
        cacheMisses++;
        return null;
    }

    public static void putHoglinCache(long key, Optional<BlockPos> result) {

        if (HOGLIN_CACHE.size() >= maxCacheSize) {

            long oldestKey = -1;
            long oldestTick = Long.MAX_VALUE;
            for (var entry : HOGLIN_CACHE.entrySet()) {
                if (entry.getValue().tick < oldestTick) {
                    oldestTick = entry.getValue().tick;
                    oldestKey = entry.getKey();
                }
            }
            if (oldestKey != -1) {
                HOGLIN_CACHE.remove(oldestKey);
                cacheEvictions++;
            }
        }
        HOGLIN_CACHE.put(key, new CachedResult(result, hoglinTickCounter));
    }

    public static void setCacheTtlTicks(int ttl) {
        if (ttl > 0) {
            cacheTtlTicks = ttl;
        }
    }

    public static void setMaxCacheSize(int size) {
        if (size > 0) {
            maxCacheSize = size;
        }
    }

    public static int getCacheTtlTicks() {
        return cacheTtlTicks;
    }

    public static int getMaxCacheSize() {
        return maxCacheSize;
    }

    public static long getCacheHits() {
        return cacheHits;
    }

    public static long getCacheMisses() {
        return cacheMisses;
    }

    public static long getCacheEvictions() {
        return cacheEvictions;
    }

    public static double getCacheHitRate() {
        long total = cacheHits + cacheMisses;
        return total > 0 ? (double) cacheHits / total * 100.0 : 0.0;
    }

    public static void resetStats() {
        cacheHits = 0;
        cacheMisses = 0;
        cacheEvictions = 0;
    }

    public static void clearCache() {
        PIGLIN_CACHE.clear();
        HOGLIN_CACHE.clear();
        resetStats();
    }
}
