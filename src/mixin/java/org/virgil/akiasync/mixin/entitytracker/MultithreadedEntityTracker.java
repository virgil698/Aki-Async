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
        
        
        final AtomicInteger taskIndex = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(PARALLELISM);
        
        for (int i = 0; i < PARALLELISM; i++) {
            TRACKER_EXECUTOR.execute(() -> {
                try {
                    int index;
                    
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
        
        
        try {
            while (!latch.await(1, TimeUnit.MILLISECONDS)) {
                runMainThreadTasks();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        
        runMainThreadTasks();
    }
    
    
    private void processChunkEntities(LevelChunk chunk) {
        final ChunkEntitySlices entitySlices = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)chunk.level)
                .moonrise$getEntityLookup().getChunk(chunk.locX, chunk.locZ);
        if (entitySlices == null) {
            return;
        }
        
        final List<Entity> entities = entitySlices.getAllEntities();
        final ChunkMap chunkMap = chunk.level.chunkSource.chunkMap;
        
        
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
            
        }
        
        for (Entity entity : entities) {
            if (entity == null) continue;
            
            ChunkMap.TrackedEntity entityTracker = getEntityTracker(entity, chunkMap);
            if (entityTracker == null) continue;
            
            try {
                
                if (nearbyPlayers != null && !nearbyPlayers.isEmpty()) {
                    entityTracker.updatePlayers(nearbyPlayers);
                }
                
                
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
