package org.virgil.akiasync.mixin.pathfinding;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedPathCache {
    
    private static final Map<PathCacheKey, CachedPath> PATH_CACHE = new ConcurrentHashMap<>(512);
    
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_EXPIRE_MS = 30000; 
    private static final int REUSE_TOLERANCE = 3; 
    
    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL_MS = 5000; 
    
    private static class CachedPath {
        final Path path;
        final long createTime;
        final java.util.concurrent.atomic.AtomicInteger useCount;
        
        CachedPath(Path path) {
            this.path = path;
            this.createTime = System.currentTimeMillis();
            this.useCount = new java.util.concurrent.atomic.AtomicInteger(0);
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - createTime > CACHE_EXPIRE_MS;
        }
        
        Path getPath() {
            useCount.incrementAndGet();
            return path;
        }
    }
    
    public static Path getCachedPath(BlockPos start, BlockPos end) {
        PathCacheKey key = new PathCacheKey(start, end);
        
        CachedPath cached = PATH_CACHE.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.getPath();
        }
        
        for (Map.Entry<PathCacheKey, CachedPath> entry : PATH_CACHE.entrySet()) {
            if (entry.getValue().isExpired()) {
                continue;
            }
            
            if (entry.getKey().isSimilar(key, REUSE_TOLERANCE)) {
                return entry.getValue().getPath();
            }
        }
        
        return null;
    }
    
    public static void cachePath(BlockPos start, BlockPos end, Path path) {
        if (path == null || !path.canReach()) {
            return;
        }
        
        if (PATH_CACHE.size() >= MAX_CACHE_SIZE) {
            cleanup(true); 
        }
        
        PathCacheKey key = new PathCacheKey(start, end);
        PATH_CACHE.put(key, new CachedPath(path));
        
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            cleanup(false);
            lastCleanupTime = now;
        }
    }
    
    private static void cleanup(boolean force) {
        if (force) {
            
            PATH_CACHE.entrySet().removeIf(entry -> 
                entry.getValue().isExpired() || entry.getValue().useCount.get() < 2
            );
        } else {
            
            PATH_CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }
    
    public static void clear() {
        PATH_CACHE.clear();
    }
    
    public static String getStats() {
        int totalPaths = PATH_CACHE.size();
        int expiredPaths = (int) PATH_CACHE.values().stream()
            .filter(CachedPath::isExpired)
            .count();
        
        return String.format("PathCache: %d paths (%d expired)", totalPaths, expiredPaths);
    }
}
