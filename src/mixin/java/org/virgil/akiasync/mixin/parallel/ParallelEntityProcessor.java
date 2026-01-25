package org.virgil.akiasync.mixin.parallel;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.Projectile;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class ParallelEntityProcessor {

    private static volatile MinecraftServer server;
    private static volatile boolean disabled = false;
    private static volatile boolean initialized = false;

    public static final AtomicInteger currentEntities = new AtomicInteger();
    private static final AtomicInteger threadPoolID = new AtomicInteger();

    private static ExecutorService tickPool;
    private static final BlockingQueue<CompletableFuture<?>> taskQueue = new LinkedBlockingQueue<>();
    private static final Set<UUID> blacklistedEntities = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> portalTickSyncMap = new ConcurrentHashMap<>();
    private static final Map<String, Set<WeakReference<Thread>>> threadTracker = new ConcurrentHashMap<>();

    public static final Set<Class<?>> SYNC_ENTITY_CLASSES = initSyncEntityClasses();

    private static Set<Class<?>> initSyncEntityClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(FallingBlockEntity.class);
        classes.add(Shulker.class);

        tryAddClass(classes, "net.minecraft.world.entity.vehicle.boat.Boat");
        tryAddClass(classes, "net.minecraft.world.entity.vehicle.Boat");
        tryAddClass(classes, "net.minecraft.world.entity.vehicle.boat.AbstractBoat");
        tryAddClass(classes, "net.minecraft.world.entity.vehicle.AbstractBoat");

        tryAddClass(classes, "net.minecraft.world.entity.vehicle.minecart.AbstractMinecart");
        tryAddClass(classes, "net.minecraft.world.entity.vehicle.AbstractMinecart");

        return Set.copyOf(classes);
    }

    private static void tryAddClass(Set<Class<?>> classes, String className) {
        try {
            classes.add(Class.forName(className));
        } catch (ClassNotFoundException ignored) {

        }
    }

    private static final Class<?> ABSTRACT_MINECART_CLASS = findMinecartClass();

    private static Class<?> findMinecartClass() {
        String[] possibleNames = {
            "net.minecraft.world.entity.vehicle.minecart.AbstractMinecart",
            "net.minecraft.world.entity.vehicle.AbstractMinecart"
        };
        for (String name : possibleNames) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private static boolean isMinecartEntity(Entity entity) {
        if (ABSTRACT_MINECART_CLASS == null) return false;
        return ABSTRACT_MINECART_CLASS.isInstance(entity);
    }

    private static Set<String> synchronizedEntityIds = Set.of(
        "minecraft:tnt",
        "minecraft:item",
        "minecraft:experience_orb"
    );

    public static void init() {
        if (initialized) return;
        synchronized (ParallelEntityProcessor.class) {
            if (initialized) return;

            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                disabled = !bridge.isEntityTickParallel();

                int parallelism = bridge.getGeneralThreadPoolSize();
                if (parallelism <= 0) {

                    parallelism = Math.max(2, Runtime.getRuntime().availableProcessors() / 4);
                }
                setupThreadPool(parallelism);
                BridgeConfigCache.debugLog("[ParallelEntityProcessor] Initialized: disabled=%b, threads=%d (from general-thread-pool.size)",
                    disabled, parallelism);
            } else {
                disabled = true;
                BridgeConfigCache.debugLog("[ParallelEntityProcessor] Bridge not available, disabled");
            }
            initialized = true;
        }
    }

    public static void setServer(MinecraftServer minecraftServer) {
        server = minecraftServer;
    }

    public static MinecraftServer getServer() {
        return server;
    }

    public static boolean isDisabled() {
        return disabled;
    }

    private static void setupThreadPool(int parallelism) {
        ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = pool -> {
            ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName("AkiAsync-EntityTick-" + threadPoolID.getAndIncrement());
            registerThread("AkiAsync-EntityTick", worker);
            worker.setDaemon(true);
            worker.setPriority(Thread.NORM_PRIORITY);
            return worker;
        };

        tickPool = new ForkJoinPool(parallelism, threadFactory, (t, e) ->
            BridgeConfigCache.errorLog("Uncaught exception in thread %s: %s", t.getName(), e.getMessage()), true);

        BridgeConfigCache.debugLog("[ParallelEntityProcessor] Thread pool initialized with %d threads", parallelism);
    }

    public static void registerThread(String poolName, Thread thread) {
        threadTracker.computeIfAbsent(poolName, k -> ConcurrentHashMap.newKeySet())
            .add(new WeakReference<>(thread));
    }

    public static boolean isAsyncThread() {
        Thread current = Thread.currentThread();
        return threadTracker.getOrDefault("AkiAsync-EntityTick", Set.of()).stream()
            .map(WeakReference::get)
            .anyMatch(current::equals);
    }

    public static void callEntityTick(ServerLevel level, Entity entity) {
        if (!initialized) init();

        if (entity.isRemoved()) {
            return;
        }

        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.isDeadOrDying()) {
                return;
            }
        }

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            int minEntities = bridge.getMinEntitiesForParallel();
            if (currentEntities.get() < minEntities) {

                tickSynchronously(level, entity);
                return;
            }
        }

        if (shouldTickSynchronously(entity)) {
            tickSynchronously(level, entity);
        } else {
            if (tickPool != null && !tickPool.isShutdown() && !tickPool.isTerminated()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                    performAsyncEntityTick(level, entity), tickPool
                ).exceptionally(e -> {
                    BridgeConfigCache.errorLog("[ParallelEntityProcessor] Async tick error for %s, switching to sync: %s",
                        entity.getType().toString(), e.getMessage());
                    tickSynchronously(level, entity);
                    blacklistedEntities.add(entity.getUUID());
                    return null;
                });
                taskQueue.add(future);
            } else {
                tickSynchronously(level, entity);
            }
        }
    }

    public static boolean shouldTickSynchronously(Entity entity) {
        if (entity.level().isClientSide()) {
            return true;
        }

        UUID entityId = entity.getUUID();

        if (disabled || blacklistedEntities.contains(entityId)) {
            return true;
        }

        if (entity instanceof ServerPlayer) {
            return true;
        }

        if (entity instanceof Projectile) {
            return true;
        }

        if (isMinecartEntity(entity)) {
            return true;
        }

        if (SYNC_ENTITY_CLASSES.contains(entity.getClass())) {
            return true;
        }

        String typeId = EntityType.getKey(entity.getType()).toString();
        if (synchronizedEntityIds.contains(typeId)) {
            return true;
        }

        if (portalTickSyncMap.containsKey(entityId)) {
            int ticksLeft = portalTickSyncMap.get(entityId);
            if (ticksLeft > 0) {
                portalTickSyncMap.put(entityId, ticksLeft - 1);
                return true;
            } else {
                portalTickSyncMap.remove(entityId);
            }
        }

        if (isPortalTickRequired(entity)) {
            portalTickSyncMap.put(entityId, 39);
            return true;
        }

        return false;
    }

    private static boolean isPortalTickRequired(Entity entity) {
        return entity.portalProcess != null && entity.portalProcess.isInsidePortalThisTick();
    }

    private static void tickSynchronously(ServerLevel level, Entity entity) {
        try {
            level.tickNonPassenger(entity);
        } catch (Exception e) {
            BridgeConfigCache.errorLog("[ParallelEntityProcessor] Sync tick error for %s: %s",
                entity.getType().toString(), e.getMessage());
        }
    }

    private static void performAsyncEntityTick(ServerLevel level, Entity entity) {
        currentEntities.incrementAndGet();
        try {

            if (entity.isRemoved()) {
                return;
            }

            if (entity instanceof net.minecraft.world.entity.Mob mob) {
                if (mob.isDeadOrDying()) {
                    return;
                }
            }

            level.tickNonPassenger(entity);
        } catch (Exception e) {

            BridgeConfigCache.errorLog("[ParallelEntityProcessor] Error in async tick for %s: %s",
                entity.getType().toString(), e.getMessage());
        } finally {
            currentEntities.decrementAndGet();
        }
    }

    public static void asyncDespawn(Entity entity) {

        if (entity.isRemoved()) {
            return;
        }

        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.isDeadOrDying()) {
                return;
            }
        }

        if (disabled) {
            entity.checkDespawn();
            return;
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {

                if (entity.isRemoved()) {
                    return;
                }

                if (entity instanceof net.minecraft.world.entity.Mob mob) {
                    if (mob.isDeadOrDying()) {
                        return;
                    }
                }

                entity.checkDespawn();
            } catch (Exception e) {
                BridgeConfigCache.errorLog("[ParallelEntityProcessor] Despawn check error: %s", e.getMessage());
            }
        }, tickPool).exceptionally(e -> {

            try {
                entity.checkDespawn();
            } catch (Exception ex) {

            }
            return null;
        });
        taskQueue.add(future);
    }

    public static void postEntityTick() {
        if (disabled || server == null) return;

        List<CompletableFuture<?>> futures = new ArrayList<>();
        CompletableFuture<?> future;
        while ((future = taskQueue.poll()) != null) {
            futures.add(future);
        }

        if (futures.isEmpty()) return;

        CompletableFuture<?> allTasks = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        allTasks.exceptionally(ex -> {
            Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
            BridgeConfigCache.errorLog("[ParallelEntityProcessor] Error during entity tick: %s", cause.getMessage());
            return null;
        });

        long backoffNanos = 10_000;
        final long maxBackoffNanos = 1_000_000;

        while (!allTasks.isDone()) {
            boolean hasTask = false;
            for (ServerLevel level : server.getAllLevels()) {
                hasTask |= level.getChunkSource().pollTask();
            }

            if (hasTask) {

                backoffNanos = 10_000;
            } else {

                LockSupport.parkNanos(backoffNanos);
                backoffNanos = Math.min(backoffNanos * 2, maxBackoffNanos);
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            level.getChunkSource().pollTask();
            level.getChunkSource().mainThreadProcessor.managedBlock(allTasks::isDone);
        }
    }

    public static void shutdown() {
        if (tickPool != null) {
            BridgeConfigCache.debugLog("[ParallelEntityProcessor] Shutting down thread pool...");
            tickPool.shutdown();
            try {
                if (!tickPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    tickPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                tickPool.shutdownNow();
            }
        }
    }

    public static void blacklistEntity(UUID uuid) {
        blacklistedEntities.add(uuid);
    }

    public static int getCurrentAsyncEntityCount() {
        return currentEntities.get();
    }
}
