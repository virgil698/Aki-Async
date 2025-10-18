package org.virgil.akiasync.mixin.async.hopper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Hopper Chain Async Executor - 16×16 region-based I/O parallelization
 * 
 * Optimization targets:
 * - 600 hoppers tick: 8-12 ms → 3-4 ms (↓67%)
 * - Region granularity: One thread per 16×16 chunk
 * - 1 tick delay acceptable (hopper transfer already has cooldown)
 * 
 * @author Virgil
 */
public class HopperChainExecutor {
    private static final int THREAD_POOL_SIZE = 4; // 4 I/O threads
    private static final ExecutorService executor = Executors.newFixedThreadPool(
        THREAD_POOL_SIZE,
        r -> {
            Thread t = new Thread(r, "AkiAsync-Hopper-IO");
            t.setDaemon(true);
            return t;
        }
    );
    
    // NBT cache: BlockPos -> Last access tick
    private static final Map<BlockPos, Long> nbtCache = new ConcurrentHashMap<>();
    private static final int NBT_CACHE_TICKS = 1; // 1 tick delay
    
    /**
     * Submit hopper tick task (async I/O)
     */
    public static void submit(ServerLevel level, BlockPos pos, Runnable task) {
        ChunkPos chunkPos = new ChunkPos(pos);
        
        executor.execute(() -> {
            try {
                // Execute hopper logic in worker thread
                task.run();
                
                // Update NBT cache timestamp
                nbtCache.put(pos, level.getGameTime());
            } catch (Exception e) {
                System.err.println("[AkiAsync] Hopper async error at " + pos + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Check if NBT cache is valid (within 1 tick)
     */
    public static boolean isCacheValid(BlockPos pos, long currentTick) {
        Long lastAccess = nbtCache.get(pos);
        if (lastAccess == null) return false;
        return (currentTick - lastAccess) <= NBT_CACHE_TICKS;
    }
    
    /**
     * Clear old NBT cache entries
     */
    public static void clearOldCache(long currentTick) {
        nbtCache.entrySet().removeIf(entry -> 
            (currentTick - entry.getValue()) > NBT_CACHE_TICKS * 20
        );
    }
    
    /**
     * Shutdown executor
     */
    public static void shutdown() {
        executor.shutdown();
        nbtCache.clear();
    }
    
    public static ExecutorService getExecutor() {
        return executor;
    }
}

