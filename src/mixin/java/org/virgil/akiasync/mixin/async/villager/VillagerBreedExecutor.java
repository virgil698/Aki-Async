package org.virgil.akiasync.mixin.async.villager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class VillagerBreedExecutor {
    private static ExecutorService executor;
    private static final Map<UUID, Long> movementCache = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> lastPositionCache = new ConcurrentHashMap<>();
    private static final int IDLE_THRESHOLD_TICKS = 20;
    private static volatile boolean initialized = false;
    
    static {
        
        initializeExecutor();
    }
    
    private static synchronized void initializeExecutor() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            executor = bridge.getVillagerBreedExecutor();
            if (bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync] VillagerBreedExecutor using shared VillagerBreed Executor");
            }
        } else {
            
            executor = Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "AkiAsync-Villager-Breed-Fallback");
                t.setDaemon(true);
                return t;
            });
        }
        
        initialized = true;
    }

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
            bridge.debugLog("[AkiAsync-Debug] VillagerBreedExecutor restart - clearing caches");
        }

        movementCache.clear();
        lastPositionCache.clear();
        
        if (bridge != null) {
            executor = bridge.getVillagerBreedExecutor();
            bridge.debugLog("[AkiAsync-Debug] VillagerBreedExecutor restart completed");
        }
    }
}
