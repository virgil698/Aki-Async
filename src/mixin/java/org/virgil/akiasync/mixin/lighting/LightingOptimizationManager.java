package org.virgil.akiasync.mixin.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LightingOptimizationManager {
    
    private static volatile boolean prioritySchedulingEnabled = true;
    private static volatile int highPriorityRadius = 32;
    private static volatile int mediumPriorityRadius = 64;
    private static volatile int lowPriorityRadius = 128;
    private static volatile long maxLowPriorityDelay = 500;
    
    private static volatile boolean debouncingEnabled = true;
    private static volatile long debounceDelay = 50;
    private static volatile int maxUpdatesPerSecond = 20;
    private static volatile long resetOnStableMs = 200;
    private static final Map<BlockPos, DebounceInfo> DEBOUNCE_MAP = new ConcurrentHashMap<>();
    
    private static volatile boolean mergingEnabled = true;
    private static volatile int mergeRadius = 2;
    private static volatile long mergeDelay = 10;
    private static volatile int maxMergedUpdates = 64;
    private static final Map<ChunkPos, List<LightUpdateRequest>> MERGE_BUFFER = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService MERGE_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AkiAsync-LightMerge");
        t.setDaemon(true);
        return t;
    });
    
    private static volatile boolean chunkBorderEnabled = true;
    private static volatile boolean batchBorderUpdates = true;
    private static volatile long borderUpdateDelay = 20;
    private static volatile int crossChunkBatchSize = 32;
    private static final Map<ChunkPos, List<LightUpdateRequest>> BORDER_BUFFER = new ConcurrentHashMap<>();
    
    private static volatile boolean adaptiveEnabled = true;
    private static volatile int monitorInterval = 10;
    private static volatile boolean autoAdjustThreads = true;
    private static volatile boolean autoAdjustBatchSize = true;
    private static volatile int targetQueueSize = 100;
    private static volatile int targetLatency = 50;
    private static final AtomicInteger currentQueueSize = new AtomicInteger(0);
    private static final AtomicLong totalProcessingTime = new AtomicLong(0);
    private static final AtomicInteger processedCount = new AtomicInteger(0);
    
    private static volatile boolean chunkUnloadEnabled = true;
    private static volatile boolean asyncCleanup = true;
    private static volatile int cleanupBatchSize = 16;
    private static volatile long cleanupDelay = 100;
    
    private static volatile boolean initialized = false;
    
    public static void initialize(
            boolean priorityEnabled, int highRadius, int mediumRadius, int lowRadius, long maxDelay,
            boolean debounceEnabled, long debounceDelayMs, int maxUpdates, long resetMs,
            boolean mergeEnabled, int radius, long mergeDelayMs, int maxMerged,
            boolean borderEnabled, boolean batchBorder, long borderDelay, int borderBatch,
            boolean adaptEnabled, int interval, boolean adjustThreads, boolean adjustBatch, int targetQueue, int targetLat,
            boolean unloadEnabled, boolean asyncClean, int cleanBatch, long cleanDelay) {
        
        prioritySchedulingEnabled = priorityEnabled;
        highPriorityRadius = highRadius;
        mediumPriorityRadius = mediumRadius;
        lowPriorityRadius = lowRadius;
        maxLowPriorityDelay = maxDelay;
        
        debouncingEnabled = debounceEnabled;
        debounceDelay = debounceDelayMs;
        maxUpdatesPerSecond = maxUpdates;
        resetOnStableMs = resetMs;
        
        mergingEnabled = mergeEnabled;
        mergeRadius = radius;
        mergeDelay = mergeDelayMs;
        maxMergedUpdates = maxMerged;
        
        chunkBorderEnabled = borderEnabled;
        batchBorderUpdates = batchBorder;
        borderUpdateDelay = borderDelay;
        crossChunkBatchSize = borderBatch;
        
        adaptiveEnabled = adaptEnabled;
        monitorInterval = interval;
        autoAdjustThreads = adjustThreads;
        autoAdjustBatchSize = adjustBatch;
        targetQueueSize = targetQueue;
        targetLatency = targetLat;
        
        chunkUnloadEnabled = unloadEnabled;
        asyncCleanup = asyncClean;
        cleanupBatchSize = cleanBatch;
        cleanupDelay = cleanDelay;
        
        initialized = true;
        
        if (mergingEnabled) {
            MERGE_SCHEDULER.scheduleAtFixedRate(() -> {
                try {
                    processMergeBuffer();
                } catch (Exception e) {
                    
                }
            }, mergeDelay, mergeDelay, TimeUnit.MILLISECONDS);
        }
    }
    
    public static LightUpdatePriority calculatePriority(BlockPos pos, ServerLevel level, boolean isChunkLoading) {
        if (!prioritySchedulingEnabled) {
            return LightUpdatePriority.NORMAL;
        }
        
        if (isChunkLoading) {
            return LightUpdatePriority.CRITICAL;
        }
        
        double minDistSq = Double.MAX_VALUE;
        boolean playerMovingFast = false;
        
        for (ServerPlayer player : level.players()) {
            double distSq = player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            if (distSq < minDistSq) {
                minDistSq = distSq;
                
                double velocitySq = player.getDeltaMovement().lengthSqr();
                if (velocitySq > 0.25) { 
                    playerMovingFast = true;
                }
            }
        }
        
        double minDist = Math.sqrt(minDistSq);
        
        if (playerMovingFast && minDist <= highPriorityRadius * 2) {
            return LightUpdatePriority.CRITICAL;
        }
        
        if (minDist <= highPriorityRadius) {
            return LightUpdatePriority.CRITICAL;
        } else if (minDist <= mediumPriorityRadius) {
            return LightUpdatePriority.HIGH;
        } else if (minDist <= lowPriorityRadius) {
            return LightUpdatePriority.NORMAL;
        } else {
            return LightUpdatePriority.LOW;
        }
    }
    
    public static boolean shouldDebounce(BlockPos pos) {
        if (!debouncingEnabled) {
            return false;
        }
        
        DebounceInfo info = DEBOUNCE_MAP.computeIfAbsent(pos, p -> new DebounceInfo());
        
        long now = System.currentTimeMillis();
        long timeSinceLastUpdate = now - info.lastUpdateTime;
        
        if (timeSinceLastUpdate > resetOnStableMs) {
            info.updateCount = 0;
            info.firstUpdateTime = now;
        }
        
        long timeSinceFirst = now - info.firstUpdateTime;
        if (timeSinceFirst < 1000) {
            if (info.updateCount >= maxUpdatesPerSecond) {
                
                return true;
            }
        } else {
            
            info.updateCount = 0;
            info.firstUpdateTime = now;
        }
        
        info.updateCount++;
        info.lastUpdateTime = now;
        
        return false;
    }
    
    public static boolean tryMergeUpdate(LightUpdateRequest request) {
        if (!mergingEnabled) {
            return false;
        }
        
        ChunkPos chunkPos = new ChunkPos(request.getPos());
        MERGE_BUFFER.computeIfAbsent(chunkPos, k -> new CopyOnWriteArrayList<>()).add(request);
        
        return true;
    }
    
    private static void processMergeBuffer() {
        
    }
    
    public static boolean isOnChunkBorder(BlockPos pos) {
        int x = pos.getX() & 15;
        int z = pos.getZ() & 15;
        return x == 0 || x == 15 || z == 0 || z == 15;
    }
    
    public static boolean tryBatchBorderUpdate(LightUpdateRequest request) {
        if (!chunkBorderEnabled || !batchBorderUpdates) {
            return false;
        }
        
        if (!isOnChunkBorder(request.getPos())) {
            return false;
        }
        
        ChunkPos chunkPos = new ChunkPos(request.getPos());
        BORDER_BUFFER.computeIfAbsent(chunkPos, k -> new CopyOnWriteArrayList<>()).add(request);
        
        return true;
    }
    
    public static void recordProcessing(int queueSize, long processingTimeMs) {
        if (!adaptiveEnabled) {
            return;
        }
        
        currentQueueSize.set(queueSize);
        totalProcessingTime.addAndGet(processingTimeMs);
        processedCount.incrementAndGet();
    }
    
    public static AdaptiveRecommendation getAdaptiveRecommendation() {
        if (!adaptiveEnabled) {
            return new AdaptiveRecommendation(0, 0);
        }
        
        int avgQueueSize = currentQueueSize.get();
        long avgLatency = processedCount.get() > 0 ? 
                totalProcessingTime.get() / processedCount.get() : 0;
        
        int threadAdjustment = 0;
        int batchAdjustment = 0;
        
        if (autoAdjustThreads) {
            if (avgQueueSize > targetQueueSize * 2) {
                threadAdjustment = 1; 
            } else if (avgQueueSize < targetQueueSize / 2) {
                threadAdjustment = -1; 
            }
        }
        
        if (autoAdjustBatchSize) {
            if (avgLatency > targetLatency * 2) {
                batchAdjustment = -4; 
            } else if (avgLatency < targetLatency / 2 && avgQueueSize > targetQueueSize) {
                batchAdjustment = 4; 
            }
        }
        
        return new AdaptiveRecommendation(threadAdjustment, batchAdjustment);
    }
    
    public static void cleanupForChunk(ChunkPos chunkPos) {
        if (!chunkUnloadEnabled) {
            return;
        }
        
        if (asyncCleanup) {
            CompletableFuture.runAsync(() -> {
                cleanupChunkData(chunkPos);
            });
        } else {
            cleanupChunkData(chunkPos);
        }
    }
    
    private static void cleanupChunkData(ChunkPos chunkPos) {
        
        DEBOUNCE_MAP.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            return new ChunkPos(pos).equals(chunkPos);
        });
        
        MERGE_BUFFER.remove(chunkPos);
        
        BORDER_BUFFER.remove(chunkPos);
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static void clearLevelCache(ServerLevel level) {
        
        DEBOUNCE_MAP.entrySet().removeIf(entry -> {
            
            return false;
        });
        
        MERGE_BUFFER.clear();
        
        BORDER_BUFFER.clear();
    }
    
    public static void clearAllCaches() {
        DEBOUNCE_MAP.clear();
        MERGE_BUFFER.clear();
        BORDER_BUFFER.clear();
    }
    
    private static class DebounceInfo {
        long lastUpdateTime = 0;
        long firstUpdateTime = System.currentTimeMillis();
        int updateCount = 0;
    }
    
    public static class AdaptiveRecommendation {
        public final int threadAdjustment;
        public final int batchAdjustment;
        
        public AdaptiveRecommendation(int threadAdjustment, int batchAdjustment) {
            this.threadAdjustment = threadAdjustment;
            this.batchAdjustment = batchAdjustment;
        }
    }
}
