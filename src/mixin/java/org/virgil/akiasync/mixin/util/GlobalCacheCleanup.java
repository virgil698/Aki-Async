package org.virgil.akiasync.mixin.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.virgil.akiasync.mixin.pathfinding.EnhancedPathfindingSystem;
import org.virgil.akiasync.mixin.pathfinding.SharedPathCache;
import org.virgil.akiasync.mixin.util.collision.CollisionBlockCache;

import java.util.concurrent.atomic.AtomicLong;

public class GlobalCacheCleanup {

    private static final AtomicLong lastCleanupTick = new AtomicLong(0);
    private static final long CLEANUP_INTERVAL_TICKS = 6000;

    private static final AtomicLong lastMemoryLogTick = new AtomicLong(0);
    private static final long MEMORY_LOG_INTERVAL_TICKS = 6000;

    public static void performCleanup(long currentTick) {
        long lastCleanup = lastCleanupTick.get();

        if (currentTick - lastCleanup >= CLEANUP_INTERVAL_TICKS) {
            if (lastCleanupTick.compareAndSet(lastCleanup, currentTick)) {
                doCleanup();
            }
        }
    }

    private static void doCleanup() {
        try {
            EnhancedPathfindingSystem.cleanupStaleData();
        } catch (Exception e) {
            ExceptionHandler.handleExpected("GlobalCacheCleanup", "EnhancedPathfindingSystem", e);
        }

        try {
            SharedPathCache.cleanupStaleData();
        } catch (Exception e) {
            ExceptionHandler.handleExpected("GlobalCacheCleanup", "SharedPathCache", e);
        }

        try {
            CollisionBlockCache.cleanupAllCaches();
        } catch (Exception e) {
            ExceptionHandler.handleExpected("GlobalCacheCleanup", "CollisionBlockCache", e);
        }

        try {
            ExceptionHandler.cleanupStaleData();
        } catch (Exception e) {
        }
    }

    public static void logMemoryStats(long currentTick) {
        long lastLog = lastMemoryLogTick.get();

        if (currentTick - lastLog >= MEMORY_LOG_INTERVAL_TICKS) {
            if (lastMemoryLogTick.compareAndSet(lastLog, currentTick)) {
                doLogMemoryStats();
            }
        }
    }

    private static void doLogMemoryStats() {
        try {
            BridgeConfigCache.debugLog("[MemoryMonitor] === Cache Statistics ===");
            BridgeConfigCache.debugLog("[MemoryMonitor] %s", EnhancedPathfindingSystem.getStatistics());
            BridgeConfigCache.debugLog("[MemoryMonitor] %s", SharedPathCache.getStats());
            BridgeConfigCache.debugLog("[MemoryMonitor] %s", EntitySliceGridManager.getStats());
            BridgeConfigCache.debugLog("[MemoryMonitor] %s", CollisionBlockCache.getAllCachesStats());

            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            BridgeConfigCache.debugLog("[MemoryMonitor] JVM Memory: %d MB / %d MB (%.1f%%)",
                usedMemory, maxMemory, (usedMemory * 100.0 / maxMemory));
        } catch (Exception e) {
            ExceptionHandler.handleExpected("GlobalCacheCleanup", "logMemoryStats", e);
        }
    }

    public static void cleanupLevel(Level level) {
        try {
            EntitySliceGridManager.clearSliceGrid(level);
        } catch (Exception e) {
            ExceptionHandler.handleExpected("GlobalCacheCleanup", "EntitySliceGridManager.clearSliceGrid", e);
        }

        try {
            CollisionBlockCache.clearLevelCache(level);
        } catch (Exception e) {
            ExceptionHandler.handleExpected("GlobalCacheCleanup", "CollisionBlockCache.clearLevelCache", e);
        }
    }

    public static void cleanupServerLevel(ServerLevel level) {
        cleanupLevel(level);
    }

    public static void clearAllCaches() {
        EnhancedPathfindingSystem.clear();
        SharedPathCache.clear();
        EntitySliceGridManager.clearAllSliceGrids();
        CollisionBlockCache.clearAllCaches();
        ExceptionHandler.clearStatistics();
    }
}
