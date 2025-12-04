package org.virgil.akiasync.mixin.async.villager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class VillagerBreedExecutor {
    private static final int THREAD_POOL_SIZE = 4;
    private static ExecutorService executor = Executors.newFixedThreadPool(
        THREAD_POOL_SIZE,
        r -> {
            Thread t = new Thread(r, "AkiAsync-Villager-Breed");
            t.setDaemon(true);
            return t;
        }
    );
    private static final Map<UUID, Long> movementCache = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> lastPositionCache = new ConcurrentHashMap<>();
    private static final int IDLE_THRESHOLD_TICKS = 20;

    public static void submit(ServerLevel level, UUID villagerUUID, BlockPos pos, Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
                movementCache.put(villagerUUID, level.getGameTime());
            } catch (Exception e) {
            }
        });
    }

    public static boolean isIdle(UUID villagerUUID, BlockPos currentPos, long currentTick) {
        BlockPos lastPos = lastPositionCache.get(villagerUUID);
        Long lastMovement = movementCache.get(villagerUUID);
        lastPositionCache.put(villagerUUID, currentPos);
        if (lastPos == null || !lastPos.equals(currentPos)) {
            movementCache.put(villagerUUID, currentTick);
            return false;
        }
        if (lastMovement == null) {
            movementCache.put(villagerUUID, currentTick);
            return false;
        }
        return (currentTick - lastMovement) >= IDLE_THRESHOLD_TICKS;
    }

    public static void clearOldCache(long currentTick) {

        java.util.Set<UUID> oldVillagers = movementCache.entrySet().stream()
            .filter(entry -> (currentTick - entry.getValue()) > IDLE_THRESHOLD_TICKS * 100)
            .map(java.util.Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toSet());
        

        oldVillagers.forEach(uuid -> {
            movementCache.remove(uuid);
            lastPositionCache.remove(uuid);
        });
    }

    public static void shutdown() {
        executor.shutdown();
        movementCache.clear();
        lastPositionCache.clear();
    }

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void restartSmooth() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[AkiAsync-Debug] Starting VillagerBreedExecutor smooth restart...");
        }

        movementCache.clear();
        lastPositionCache.clear();
        org.virgil.akiasync.mixin.bridge.Bridge bridge2 = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge2 != null) {
            bridge2.debugLog("[AkiAsync-VillagerBreed] Cleared villager caches");
        }

        ExecutorService oldExecutor = executor;
        executor = Executors.newFixedThreadPool(
            THREAD_POOL_SIZE,
            r -> {
                Thread t = new Thread(r, "AkiAsync-Villager-Smooth");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            }
        );

        oldExecutor.shutdown();
        try {
            if (!oldExecutor.awaitTermination(1000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                org.virgil.akiasync.mixin.bridge.Bridge forceShutdownBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (forceShutdownBridge != null) {
                    forceShutdownBridge.debugLog("[AkiAsync-Debug] VillagerBreedExecutor force shutdown");
                }
                oldExecutor.shutdownNow();
            } else {
                org.virgil.akiasync.mixin.bridge.Bridge gracefulShutdownBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (gracefulShutdownBridge != null) {
                    gracefulShutdownBridge.debugLog("[AkiAsync-Debug] VillagerBreedExecutor gracefully shutdown");
                }
            }
        } catch (InterruptedException e) {
            oldExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        org.virgil.akiasync.mixin.bridge.Bridge bridge3 = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge3 != null) {
            bridge3.debugLog("[AkiAsync-Debug] VillagerBreedExecutor restart completed");
        }
    }
}
