package org.virgil.akiasync.mixin.async.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.concurrent.*;

public class AsyncRedstoneNetworkManager {
    
    private static ExecutorService CACHE_EXECUTOR;
    private static volatile boolean executorInitialized = false;
    
    private static final Map<ServerLevel, AsyncRedstoneNetworkManager> INSTANCES = new ConcurrentHashMap<>();
    
    static {
        initializeExecutor();
    }
    
    private static synchronized void initializeExecutor() {
        if (executorInitialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            
            CACHE_EXECUTOR = bridge.getGeneralExecutor();
            if (bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync] AsyncRedstoneNetworkManager using shared General Executor");
            }
        } else {
            
            CACHE_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "AkiAsync-RedstoneCache-Fallback");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });
        }
        
        executorInitialized = true;
    }
    
    private final ServerLevel level;
    private final RedstoneNetworkCache cache;
    private final Map<String, CompletableFuture<RedstoneNetworkCache.CachedNetwork>> pendingCalculations = 
        new ConcurrentHashMap<>();
    
    private AsyncRedstoneNetworkManager(ServerLevel level) {
        this.level = level;
        this.cache = RedstoneNetworkCache.getOrCreate(level);
    }
    
    public static AsyncRedstoneNetworkManager getInstance(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, AsyncRedstoneNetworkManager::new);
    }
    
    public RedstoneNetworkCache.CachedNetwork getOrCalculateAsync(BlockPos pos) {

        RedstoneNetworkCache.CachedNetwork cached = cache.getNetwork(pos);
        if (cached != null && cached.isApplicable(level, pos)) {
            return cached;
        }
        
        String key = generateKey(pos);
        CompletableFuture<RedstoneNetworkCache.CachedNetwork> pending = pendingCalculations.get(key);
        if (pending != null && pending.isDone()) {
            try {
                RedstoneNetworkCache.CachedNetwork result = pending.get();
                pendingCalculations.remove(key);
                return result;
            } catch (Exception e) {
                pendingCalculations.remove(key);
            }
        }
        
        return null;
    }
    
    public void preCalculateNetwork(BlockPos sourcePos, List<BlockPos> nearbyWires) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge == null || !bridge.isRedstoneNetworkCacheEnabled()) {
            return;
        }
        
        String key = generateKey(sourcePos);
        
        if (cache.getNetwork(sourcePos) != null) {
            return;
        }
        if (pendingCalculations.containsKey(key)) {
            return;
        }
        
        CompletableFuture<RedstoneNetworkCache.CachedNetwork> future = CompletableFuture.supplyAsync(() -> {
            return calculateNetworkSync(sourcePos, nearbyWires);
        }, CACHE_EXECUTOR);
        
        pendingCalculations.put(key, future);
    }
    
    private RedstoneNetworkCache.CachedNetwork calculateNetworkSync(BlockPos sourcePos, 
                                                                     List<BlockPos> nearbyWires) {

        List<BlockPos> affectedWires = new ArrayList<>();
        Map<BlockPos, Integer> powerChanges = new HashMap<>();
        
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(sourcePos);
        visited.add(sourcePos);
        
        while (!queue.isEmpty() && visited.size() < 100) {
            BlockPos current = queue.poll();
            affectedWires.add(current);
            
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        
                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (!visited.contains(neighbor) && nearbyWires.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        
        return new RedstoneNetworkCache.CachedNetwork(sourcePos, affectedWires, powerChanges);
    }
    
    public void cacheNetwork(BlockPos sourcePos, List<BlockPos> affectedWires, 
                            Map<BlockPos, Integer> powerChanges) {
        cache.cacheNetwork(sourcePos, affectedWires, powerChanges);
    }
    
    public void invalidate(BlockPos pos) {
        cache.invalidate(pos);
        pendingCalculations.remove(generateKey(pos));
    }
    
    public void invalidateNearby(BlockPos pos, int radius) {
        cache.invalidateNearby(pos, radius);
    }
    
    public void expire(long currentTime) {
        cache.expire(currentTime);
        
        pendingCalculations.entrySet().removeIf(entry -> entry.getValue().isDone());
    }
    
    public void clear() {
        cache.clear();
        pendingCalculations.clear();
    }
    
    public String getStats() {
        return String.format("%s, Pending: %d", cache.getStats(), pendingCalculations.size());
    }
    
    private String generateKey(BlockPos pos) {
        return String.format("%d_%d_%d", pos.getX(), pos.getY(), pos.getZ());
    }
    
    public static void shutdown() {
        
        INSTANCES.clear();
    }
    
    public static void clearLevelCache(ServerLevel level) {
        AsyncRedstoneNetworkManager manager = INSTANCES.remove(level);
        if (manager != null) {
            manager.clear();
        }
    }
}
