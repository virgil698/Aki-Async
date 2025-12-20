package org.virgil.akiasync.mixin.util.collision;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.virgil.akiasync.mixin.async.explosion.OptimizedExplosionCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollisionBlockCache {
    
    private static final Map<Level, CollisionBlockCache> CACHES = new ConcurrentHashMap<>();
    
    private final OptimizedExplosionCache cache;
    private final Level level;
    
    private long lastCleanupTime = 0;
    private static final int CLEANUP_INTERVAL = 600; 
    
    private CollisionBlockCache(Level level) {
        this.level = level;
        this.cache = new OptimizedExplosionCache(level);
    }
    
    public static CollisionBlockCache getOrCreate(Level level) {
        return CACHES.computeIfAbsent(level, CollisionBlockCache::new);
    }
    
    public BlockState getBlockState(BlockPos pos) {
        return cache.getBlockState(pos);
    }
    
    public FluidState getFluidState(BlockPos pos) {
        return cache.getFluidState(pos);
    }
    
    public VoxelShape getCollisionShape(BlockPos pos) {
        BlockState state = getBlockState(pos);
        return state.getCollisionShape(level, pos);
    }
    
    public boolean isCollidable(BlockPos pos) {
        BlockState state = getBlockState(pos);
        return !state.isAir() && state.blocksMotion();
    }
    
    public boolean isAir(BlockPos pos) {
        BlockState state = getBlockState(pos);
        return state.isAir();
    }
    
    public void getBlockStatesBatch(BlockPos[] positions, BlockState[] states) {
        for (int i = 0; i < positions.length; i++) {
            states[i] = getBlockState(positions[i]);
        }
    }
    
    public void warmup(BlockPos center, int radius) {
        cache.warmup(center, radius);
    }
    
    public void cleanup(long gameTime) {
        if (gameTime - lastCleanupTime >= CLEANUP_INTERVAL) {
            cache.cleanup();
            lastCleanupTime = gameTime;
        }
    }
    
    public void clear() {
        cache.clear();
    }
    
    public OptimizedExplosionCache getOptimizedCache() {
        return cache;
    }
    
    public String getStats() {
        return cache.getStats();
    }
    
    public OptimizedExplosionCache.CacheStats getCacheStats() {
        return cache.getCacheStats();
    }
    
    public static void clearLevelCache(Level level) {
        CollisionBlockCache cache = CACHES.remove(level);
        if (cache != null) {
            cache.clear();
        }
    }
    
    public static void clearAllCaches() {
        for (CollisionBlockCache cache : CACHES.values()) {
            cache.clear();
        }
        CACHES.clear();
    }
    
    public static void cleanupAllCaches() {
        for (Map.Entry<Level, CollisionBlockCache> entry : CACHES.entrySet()) {
            Level level = entry.getKey();
            CollisionBlockCache cache = entry.getValue();
            
            if (level instanceof ServerLevel serverLevel) {
                cache.cleanup(serverLevel.getGameTime());
            }
        }
    }
    
    public static String getAllCachesStats() {
        StringBuilder sb = new StringBuilder("CollisionBlockCache Statistics:\n");
        
        for (Map.Entry<Level, CollisionBlockCache> entry : CACHES.entrySet()) {
            Level level = entry.getKey();
            CollisionBlockCache cache = entry.getValue();
            
            String worldName = level.dimension().location().toString();
            sb.append("  ").append(worldName).append(": ").append(cache.getStats()).append("\n");
        }
        
        return sb.toString();
    }
    
    public static int getCacheCount() {
        return CACHES.size();
    }
}
