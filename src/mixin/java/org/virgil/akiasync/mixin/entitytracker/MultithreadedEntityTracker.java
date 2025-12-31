package org.virgil.akiasync.mixin.entitytracker;

import ca.spottedleaf.moonrise.common.list.ReferenceList;
import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.virgil.akiasync.mixin.util.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多线程实体追踪器
 * 
 * 重要修复：
 * 1. sendChanges() 必须在 updatePlayers() 之后立即执行，不能分离
 * 2. 使用 CountDownLatch 确保所有任务完成后再返回
 * 3. 减少批量大小，提高响应性
 */
public class MultithreadedEntityTracker {
    
    private static final int PARALLELISM = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
    private static final ExecutorService TRACKER_EXECUTOR;
    
    static {
        TRACKER_EXECUTOR = Executors.newFixedThreadPool(PARALLELISM, new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "AkiAsync-EntityTracker-" + threadNumber.getAndIncrement());
                t.setPriority(Thread.NORM_PRIORITY - 1);
                t.setDaemon(true);
                return t;
            }
        });
    }
    
    private final ReferenceList<LevelChunk> entityTickingChunks;
    private final ConcurrentLinkedQueue<Runnable> mainThreadTasks;
    
    public MultithreadedEntityTracker(
            ReferenceList<LevelChunk> entityTickingChunks,
            ConcurrentLinkedQueue<Runnable> mainThreadTasks) {
        this.entityTickingChunks = entityTickingChunks;
        this.mainThreadTasks = mainThreadTasks;
    }
    
    public void tick() {
        final LevelChunk[] chunks = this.entityTickingChunks.getRawDataUnchecked();
        final int size = this.entityTickingChunks.size();
        
        if (size == 0) {
            return;
        }
        
        // 小规模直接串行处理，避免线程开销
        if (size <= 4) {
            for (int i = 0; i < size; i++) {
                LevelChunk chunk = chunks[i];
                if (chunk != null) {
                    processChunkEntities(chunk);
                }
            }
            runMainThreadTasks();
            return;
        }
        
        // 大规模并行处理
        final AtomicInteger taskIndex = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(PARALLELISM);
        
        for (int i = 0; i < PARALLELISM; i++) {
            TRACKER_EXECUTOR.execute(() -> {
                try {
                    int index;
                    // 每次只处理1个区块，提高响应性
                    while ((index = taskIndex.getAndIncrement()) < size) {
                        LevelChunk chunk = chunks[index];
                        if (chunk != null) {
                            try {
                                processChunkEntities(chunk);
                            } catch (Throwable t) {
                                ExceptionHandler.handleExpected("MultithreadedEntityTracker", "processChunk",
                                    new RuntimeException("Failed to process chunk " + chunk.getPos(), t));
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有任务完成，同时处理主线程任务
        try {
            while (!latch.await(1, TimeUnit.MILLISECONDS)) {
                runMainThreadTasks();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 最后处理剩余的主线程任务
        runMainThreadTasks();
    }
    
    /**
     * 处理单个区块的所有实体
     * 关键：updatePlayers 和 sendChanges 必须连续执行
     */
    private void processChunkEntities(LevelChunk chunk) {
        final ChunkEntitySlices entitySlices = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)chunk.level)
                .moonrise$getEntityLookup().getChunk(chunk.locX, chunk.locZ);
        if (entitySlices == null) {
            return;
        }
        
        final List<Entity> entities = entitySlices.getAllEntities();
        final ChunkMap chunkMap = chunk.level.chunkSource.chunkMap;
        
        // 获取附近玩家列表（只获取一次）
        List<ServerPlayer> nearbyPlayers = null;
        try {
            ca.spottedleaf.moonrise.common.misc.NearbyPlayers.TrackedChunk trackedChunk = 
                ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)chunk.level)
                    .moonrise$getNearbyPlayers().getChunk(chunk.locX, chunk.locZ);
            
            if (trackedChunk != null) {
                ca.spottedleaf.moonrise.common.list.ReferenceList<ServerPlayer> players = 
                    trackedChunk.getPlayers(ca.spottedleaf.moonrise.common.misc.NearbyPlayers.NearbyMapType.VIEW_DISTANCE);
                
                if (players != null && players.size() > 0) {
                    nearbyPlayers = new ArrayList<>(players.size());
                    for (int i = 0; i < players.size(); i++) {
                        nearbyPlayers.add(players.getUnchecked(i));
                    }
                }
            }
        } catch (Exception e) {
            // 忽略，使用 null
        }
        
        for (Entity entity : entities) {
            if (entity == null) continue;
            
            ChunkMap.TrackedEntity entityTracker = getEntityTracker(entity, chunkMap);
            if (entityTracker == null) continue;
            
            try {
                // 1. 先更新玩家列表
                if (nearbyPlayers != null && !nearbyPlayers.isEmpty()) {
                    entityTracker.updatePlayers(nearbyPlayers);
                }
                
                // 2. 立即发送变更（关键：不能分离！）
                entityTracker.serverEntity.sendChanges();
            } catch (Throwable t) {
                ExceptionHandler.handleExpected("MultithreadedEntityTracker", "processEntity",
                    new RuntimeException("Failed to process entity " + entity.getId(), t));
            }
        }
    }
    
    private ChunkMap.TrackedEntity getEntityTracker(Entity entity, ChunkMap chunkMap) {
        try {
            if (entity instanceof ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity) {
                return ((ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity) entity).moonrise$getTrackedEntity();
            }
        } catch (Exception e) {
            // 忽略
        }
        
        if (chunkMap.entityMap != null) {
            return chunkMap.entityMap.get(entity.getId());
        }
        
        return null;
    }
    
    private void runMainThreadTasks() {
        try {
            Runnable task;
            while ((task = this.mainThreadTasks.poll()) != null) {
                task.run();
            }
        } catch (Throwable throwable) {
            ExceptionHandler.handleExpected("MultithreadedEntityTracker", "runMainThreadTasks", 
                new RuntimeException("Tasks failed while ticking track queue", throwable));
        }
    }
    
    public static void shutdown() {
        TRACKER_EXECUTOR.shutdown();
    }
}
