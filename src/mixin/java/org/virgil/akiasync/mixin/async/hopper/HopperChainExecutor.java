package org.virgil.akiasync.mixin.async.hopper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class HopperChainExecutor {
    private static final int THREAD_POOL_SIZE = 4;
    private static final ExecutorService executor = Executors.newFixedThreadPool(
        THREAD_POOL_SIZE,
        r -> {
            Thread t = new Thread(r, "AkiAsync-Hopper-IO");
            t.setDaemon(true);
            return t;
        }
    );
    
    private static final Map<BlockPos, Long> nbtCache = new ConcurrentHashMap<>();
    private static final int NBT_CACHE_TICKS = 1;
    
    public static void submit(ServerLevel level, BlockPos pos, Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
                nbtCache.put(pos, level.getGameTime());
            } catch (Exception e) {
                System.err.println("[AkiAsync] Hopper async error at " + pos + ": " + e.getMessage());
            }
        });
    }
    
    public static boolean isCacheValid(BlockPos pos, long currentTick) {
        Long lastAccess = nbtCache.get(pos);
        if (lastAccess == null) return false;
        return (currentTick - lastAccess) <= NBT_CACHE_TICKS;
    }
    
    public static void clearOldCache(long currentTick) {
        nbtCache.entrySet().removeIf(entry -> 
            (currentTick - entry.getValue()) > NBT_CACHE_TICKS * 20
        );
    }
    
    public static void shutdown() {
        executor.shutdown();
        nbtCache.clear();
    }
    
    public static ExecutorService getExecutor() {
        return executor;
    }
}

