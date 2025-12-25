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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class MultithreadedEntityTracker {
    
    private enum TrackerStage {
        
        UPDATE_PLAYERS,
        
        SEND_CHANGES
    }
    
    private static final int PARALLELISM = Math.max(4, Runtime.getRuntime().availableProcessors());
    private static final ExecutorService TRACKER_EXECUTOR;
    
    static {
        
        TRACKER_EXECUTOR = Executors.newFixedThreadPool(PARALLELISM, new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "AkiAsync-EntityTracker-" + threadNumber.getAndIncrement());
                t.setPriority(Thread.NORM_PRIORITY - 2); 
                t.setDaemon(true);
                return t;
            }
        });
    }
    
    private final ReferenceList<LevelChunk> entityTickingChunks;
    private final ConcurrentLinkedQueue<Runnable> mainThreadTasks;
    private final AtomicInteger taskIndex = new AtomicInteger();
    private final AtomicInteger finishedTasks = new AtomicInteger();
    
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
        
        this.taskIndex.set(0);
        this.finishedTasks.set(0);
        
        for (int i = 0; i < PARALLELISM; i++) {
            TRACKER_EXECUTOR.execute(this::runUpdatePlayers);
        }
        
        while (this.taskIndex.get() < size) {
            this.runMainThreadTasks();
            this.handleChunkUpdates(5, chunks, size, TrackerStage.UPDATE_PLAYERS); 
        }
        
        while (this.finishedTasks.get() != PARALLELISM) {
            this.runMainThreadTasks();
        }
        
        this.runMainThreadTasks();
        
        for (int i = 0; i < size; i++) {
            LevelChunk chunk = chunks[i];
            if (chunk != null) {
                this.updateChunkEntities(chunk, TrackerStage.SEND_CHANGES);
            }
        }
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
    
    private void runUpdatePlayers() {
        try {
            final LevelChunk[] chunks = this.entityTickingChunks.getRawDataUnchecked();
            final int size = this.entityTickingChunks.size();
            while (handleChunkUpdates(10, chunks, size, TrackerStage.UPDATE_PLAYERS)); 
        } finally {
            this.finishedTasks.incrementAndGet();
        }
    }
    
    private boolean handleChunkUpdates(int tasks, LevelChunk[] chunks, int size, TrackerStage stage) {
        int index;
        while ((index = this.taskIndex.getAndAdd(tasks)) < size) {
            for (int i = index; i < index + tasks && i < size; i++) {
                LevelChunk chunk = chunks[i];
                if (chunk != null) {
                    try {
                        this.updateChunkEntities(chunk, stage);
                    } catch (Throwable throwable) {
                        ExceptionHandler.handleExpected("MultithreadedEntityTracker", "handleChunkUpdates",
                            new RuntimeException("Ticking tracker failed for chunk " + chunk.getPos(), throwable));
                    }
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    private void updateChunkEntities(LevelChunk chunk, TrackerStage trackerStage) {
        
        final ChunkEntitySlices entitySlices = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)chunk.level)
                .moonrise$getEntityLookup().getChunk(chunk.locX, chunk.locZ);
        if (entitySlices == null) {
            return;
        }
        
        final List<Entity> entities = entitySlices.getAllEntities();
        final ChunkMap chunkMap = chunk.level.chunkSource.chunkMap;
        
        for (Entity entity : entities) {
            if (entity != null) {
                ChunkMap.TrackedEntity entityTracker = null;
                try {
                    if (entity instanceof ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity) {
                        entityTracker = ((ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity) entity).moonrise$getTrackedEntity();
                    }
                } catch (Exception ignored) {
                }
                
                if (entityTracker == null && chunkMap.entityMap != null) {
                    entityTracker = chunkMap.entityMap.get(entity.getId());
                }
                
                if (entityTracker != null) {
                    if (trackerStage == TrackerStage.SEND_CHANGES) {
                        
                        entityTracker.serverEntity.sendChanges();
                    } else if (trackerStage == TrackerStage.UPDATE_PLAYERS) {
                        
                        ca.spottedleaf.moonrise.common.misc.NearbyPlayers.TrackedChunk trackedChunk = 
                            ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)chunk.level)
                                .moonrise$getNearbyPlayers().getChunk(chunk.locX, chunk.locZ);
                        
                        if (trackedChunk != null) {
                            ca.spottedleaf.moonrise.common.list.ReferenceList<ServerPlayer> players = 
                                trackedChunk.getPlayers(ca.spottedleaf.moonrise.common.misc.NearbyPlayers.NearbyMapType.VIEW_DISTANCE);
                            
                            if (players != null && players.size() > 0) {
                                
                                List<ServerPlayer> playersList = new ArrayList<>(players.size());
                                for (int i = 0; i < players.size(); i++) {
                                    playersList.add(players.getUnchecked(i));
                                }
                                
                                entityTracker.updatePlayers(playersList);
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static void shutdown() {
        TRACKER_EXECUTOR.shutdown();
    }
}
