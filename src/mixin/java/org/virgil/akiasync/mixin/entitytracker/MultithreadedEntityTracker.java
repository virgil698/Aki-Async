package org.virgil.akiasync.mixin.entitytracker;

import ca.spottedleaf.moonrise.common.list.ReferenceList;
import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.util.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class MultithreadedEntityTracker {

    private static volatile int parallelism = 0;
    private static volatile ForkJoinPool TRACKER_POOL;
    private static final AtomicInteger threadPoolID = new AtomicInteger();

    private final BlockingQueue<CompletableFuture<?>> taskQueue = new LinkedBlockingQueue<>();

    private static int getParallelism() {
        if (parallelism <= 0) {
            int configuredSize = BridgeConfigCache.getGeneralThreadPoolSize();
            if (configuredSize > 0) {
                parallelism = Math.max(2, Math.min(configuredSize, Runtime.getRuntime().availableProcessors()));
            } else {
                parallelism = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
            }
        }
        return parallelism;
    }

    private static ForkJoinPool getExecutor() {
        if (TRACKER_POOL == null) {
            synchronized (MultithreadedEntityTracker.class) {
                if (TRACKER_POOL == null) {
                    int threads = getParallelism();
                    ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = pool -> {
                        ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                        worker.setName("AkiAsync-EntityTracker-" + threadPoolID.getAndIncrement());
                        worker.setDaemon(true);
                        worker.setPriority(Thread.NORM_PRIORITY - 1);
                        return worker;
                    };
                    TRACKER_POOL = new ForkJoinPool(threads, threadFactory,
                        (t, e) -> ExceptionHandler.handleExpected("MultithreadedEntityTracker", "pool",
                            e instanceof Exception ? (Exception) e : new RuntimeException(e)),
                        true);
                }
            }
        }
        return TRACKER_POOL;
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

        final ForkJoinPool executor = getExecutor();
        if (executor.isShutdown() || executor.isTerminated()) {

            for (int i = 0; i < size; i++) {
                LevelChunk chunk = chunks[i];
                if (chunk != null) {
                    processChunkEntities(chunk);
                }
            }
            runMainThreadTasks();
            return;
        }

        for (int i = 0; i < size; i++) {
            final LevelChunk chunk = chunks[i];
            if (chunk != null) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        processChunkEntities(chunk);
                    } catch (Throwable t) {
                        ExceptionHandler.handleExpected("MultithreadedEntityTracker", "processChunk",
                            new RuntimeException("Failed to process chunk " + chunk.getPos(), t));
                    }
                }, executor);
                taskQueue.add(future);
            }
        }

        postTrackerTick(chunks.length > 0 ? chunks[0] : null);
    }

    private void postTrackerTick(LevelChunk sampleChunk) {
        List<CompletableFuture<?>> futuresList = new ArrayList<>();
        CompletableFuture<?> future;
        while ((future = taskQueue.poll()) != null) {
            futuresList.add(future);
        }

        if (futuresList.isEmpty()) {
            runMainThreadTasks();
            return;
        }

        CompletableFuture<?> allTasks = CompletableFuture.allOf(
            futuresList.toArray(new CompletableFuture[0])
        );

        allTasks.exceptionally(ex -> {
            Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
            Exception exception = cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
            ExceptionHandler.handleExpected("MultithreadedEntityTracker", "postTrackerTick", exception);
            return null;
        });

        ServerLevel level = sampleChunk != null ? (ServerLevel) sampleChunk.level : null;
        while (!allTasks.isDone()) {

            runMainThreadTasks();

            boolean hasTask = false;
            if (level != null) {
                try {
                    hasTask = level.getChunkSource().pollTask();
                } catch (Exception ignored) {
                }
            }

            if (!hasTask) {
                LockSupport.parkNanos(50_000);
            }
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
        } catch (Exception ignored) {
        }

        final List<ServerPlayer> finalNearbyPlayers = nearbyPlayers;

        for (Entity entity : entities) {
            if (entity == null) continue;

            ChunkMap.TrackedEntity entityTracker = getEntityTracker(entity, chunkMap);
            if (entityTracker == null) continue;

            try {
                if (finalNearbyPlayers != null && !finalNearbyPlayers.isEmpty()) {
                    entityTracker.updatePlayers(finalNearbyPlayers);
                }
                entityTracker.serverEntity.sendChanges();
            } catch (Throwable ignored) {

            }
        }
    }

    private ChunkMap.TrackedEntity getEntityTracker(Entity entity, ChunkMap chunkMap) {
        try {
            if (entity instanceof ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity) {
                return ((ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity) entity).moonrise$getTrackedEntity();
            }
        } catch (Exception ignored) {
        }

        if (chunkMap.entityMap != null) {
            return chunkMap.entityMap.get(entity.getId());
        }

        return null;
    }

    private void runMainThreadTasks() {
        try {
            Runnable task;
            int processed = 0;

            while (processed < 100 && (task = this.mainThreadTasks.poll()) != null) {
                task.run();
                processed++;
            }
        } catch (Throwable throwable) {
            ExceptionHandler.handleExpected("MultithreadedEntityTracker", "runMainThreadTasks",
                new RuntimeException("Tasks failed while ticking track queue", throwable));
        }
    }

    public static void shutdown() {
        if (TRACKER_POOL != null) {
            TRACKER_POOL.shutdown();
            try {
                TRACKER_POOL.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
