package org.virgil.akiasync.mixin.optimization;

import org.virgil.akiasync.mixin.optimization.cache.BlockPosIterationCache;
import org.virgil.akiasync.mixin.optimization.collections.OptimizedEntityCollection;
import org.virgil.akiasync.mixin.optimization.scheduler.WorkStealingTaskScheduler;
import org.virgil.akiasync.mixin.optimization.thread.VirtualThreadService;

public class OptimizationManager {
    
    private static final OptimizationManager INSTANCE = new OptimizationManager();
    private static boolean initialized = false;
    
    private BlockPosIterationCache blockPosCache;
    private WorkStealingTaskScheduler taskScheduler;
    private VirtualThreadService virtualThreadService;
    private OptimizationStats stats;
    
    private OptimizationManager() {
    }
    
    public static OptimizationManager getInstance() {
        if (!initialized) {
            synchronized (OptimizationManager.class) {
                if (!initialized) {
                    INSTANCE.initialize();
                    initialized = true;
                }
            }
        }
        return INSTANCE;
    }
    
    private void initialize() {
        System.out.println("[AkiAsync-Optimization] Initializing Nitori-style optimizations...");
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        boolean nitoriEnabled = bridge != null && bridge.isNitoriOptimizationsEnabled();
        if (!nitoriEnabled) {
            System.out.println("[AkiAsync-Optimization] Nitori optimizations disabled by configuration");
            return;
        }
        
        if (bridge.isBlockPosCacheEnabled()) {
            blockPosCache = BlockPosIterationCache.INSTANCE;
            System.out.println("[AkiAsync-Optimization] BlockPos iteration cache initialized");
        }
        
        if (bridge.isWorkStealingEnabled()) {
            taskScheduler = WorkStealingTaskScheduler.getInstance();
            System.out.println("[AkiAsync-Optimization] Work-stealing task scheduler initialized with " + 
                taskScheduler.getStats().parallelism + " threads");
        }
        
        if (bridge.isVirtualThreadEnabled()) {
            virtualThreadService = VirtualThreadService.get();
            if (virtualThreadService != null) {
                System.out.println("[AkiAsync-Optimization] Virtual Thread support enabled (Java " + 
                    VirtualThreadService.getJavaMajorVersion() + ")");
            } else {
                System.out.println("[AkiAsync-Optimization] Virtual Thread not supported on this JVM");
            }
        }
        
        stats = new OptimizationStats();
        
        System.out.println("[AkiAsync-Optimization] Nitori-style optimizations initialized successfully");
    }
    
    public BlockPosIterationCache getBlockPosCache() {
        return blockPosCache;
    }
    
    public WorkStealingTaskScheduler getTaskScheduler() {
        return taskScheduler;
    }
    
    public VirtualThreadService getVirtualThreadService() {
        return virtualThreadService;
    }
    
    public <T extends net.minecraft.world.entity.Entity> OptimizedEntityCollection<T> createEntityCollection() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null && bridge.isOptimizedCollectionsEnabled()) {
            stats.entityCollectionsCreated++;
            return new OptimizedEntityCollection<>();
        } else {
            return null;
        }
    }
    
    public OptimizationStats getStats() {
        return stats;
    }
    
    public boolean isOptimizationAvailable(OptimizationType type) {
        switch (type) {
            case BLOCK_POS_CACHE:
                return blockPosCache != null;
            case WORK_STEALING:
                return taskScheduler != null;
            case VIRTUAL_THREADS:
                return virtualThreadService != null;
            case ENTITY_COLLECTIONS:
                return true;
            default:
                return false;
        }
    }
    
    public void shutdown() {
        System.out.println("[AkiAsync-Optimization] Shutting down optimizations...");
        
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
        
        System.out.println("[AkiAsync-Optimization] Shutdown complete");
    }
    
    public enum OptimizationType {
        BLOCK_POS_CACHE,
        WORK_STEALING,
        VIRTUAL_THREADS,
        ENTITY_COLLECTIONS
    }
    
    public static class OptimizationStats {
        public volatile long entityCollectionsCreated = 0;
        public volatile long blockPosCacheHits = 0;
        public volatile long workStealingTasksProcessed = 0;
        public volatile long virtualThreadsUsed = 0;
        
        private final long startTime = System.currentTimeMillis();
        
        public long getUptimeMillis() {
            return System.currentTimeMillis() - startTime;
        }
        
        @Override
        public String toString() {
            return String.format(
                "OptimizationStats{uptime=%dms, entityCollections=%d, cacheHits=%d, tasks=%d, vThreads=%d}",
                getUptimeMillis(), entityCollectionsCreated, blockPosCacheHits, 
                workStealingTasksProcessed, virtualThreadsUsed
            );
        }
    }
    
    public void printPerformanceReport() {
        System.out.println("=== AkiAsync Optimization Performance Report ===");
        System.out.println("Uptime: " + (stats.getUptimeMillis() / 1000) + " seconds");
        System.out.println("Entity Collections Created: " + stats.entityCollectionsCreated);
        System.out.println("BlockPos Cache Hits: " + stats.blockPosCacheHits);
        System.out.println("Work-Stealing Tasks: " + stats.workStealingTasksProcessed);
        System.out.println("Virtual Threads Used: " + stats.virtualThreadsUsed);
        
        if (taskScheduler != null) {
            System.out.println("Task Scheduler: " + taskScheduler.getStats());
        }
        
        System.out.println("Available Optimizations:");
        for (OptimizationType type : OptimizationType.values()) {
            System.out.println("  " + type + ": " + isOptimizationAvailable(type));
        }
        System.out.println("===============================================");
    }
}
