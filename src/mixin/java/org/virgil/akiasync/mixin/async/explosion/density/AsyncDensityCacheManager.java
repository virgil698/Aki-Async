package org.virgil.akiasync.mixin.async.explosion.density;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncDensityCacheManager {
    private static ExecutorService CACHE_EXECUTOR;
    private static volatile boolean executorInitialized = false;
    
    private static final ConcurrentHashMap<ServerLevel, AsyncDensityCacheManager> INSTANCES = new ConcurrentHashMap<>();
    
    static {
        initializeExecutor();
    }
    
    private static synchronized void initializeExecutor() {
        if (executorInitialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            
            CACHE_EXECUTOR = bridge.getTNTExecutor();
            if (bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync] AsyncDensityCacheManager using shared TNT Executor");
            }
        } else {
            
            CACHE_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "AkiAsync-DensityCache-Fallback");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });
        }
        
        executorInitialized = true;
    }
    
    @SuppressWarnings("unused")
    private final ServerLevel level;
    private final SakuraBlockDensityCache cache;
    private final ConcurrentHashMap<String, CompletableFuture<Float>> pendingCalculations = new ConcurrentHashMap<>();
    
    private AsyncDensityCacheManager(ServerLevel level) {
        this.level = level;
        this.cache = SakuraBlockDensityCache.getOrCreate(level);
    }
    
    public static AsyncDensityCacheManager getInstance(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, AsyncDensityCacheManager::new);
    }
    
    public float getDensityAsync(Vec3 explosionPos, Entity entity) {

        float cached = cache.getBlockDensity(explosionPos, entity);
        if (cached != SakuraBlockDensityCache.UNKNOWN_DENSITY) {
            return cached;
        }
        
        String key = generateKey(explosionPos, entity);
        CompletableFuture<Float> pending = pendingCalculations.get(key);
        if (pending != null && pending.isDone()) {
            try {
                float result = pending.get();
                pendingCalculations.remove(key);
                return result;
            } catch (Exception e) {
                pendingCalculations.remove(key);
            }
        }
        
        return SakuraBlockDensityCache.UNKNOWN_DENSITY;
    }
    
    public void preCalculateDensities(Vec3 explosionPos, java.util.List<Entity> entities) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge == null || !bridge.isTNTUseSakuraDensityCache()) {
            return;
        }
        
        for (Entity entity : entities) {
            String key = generateKey(explosionPos, entity);
            
            if (cache.getBlockDensity(explosionPos, entity) != SakuraBlockDensityCache.UNKNOWN_DENSITY) {
                continue;
            }
            if (pendingCalculations.containsKey(key)) {
                continue;
            }
            
            CompletableFuture<Float> future = CompletableFuture.supplyAsync(() -> {
                return calculateDensitySync(explosionPos, entity);
            }, CACHE_EXECUTOR);
            
            pendingCalculations.put(key, future);
        }
    }
    
    private float calculateDensitySync(Vec3 explosionPos, Entity entity) {

        return 0.5f;
    }
    
    public void putDensity(Vec3 explosionPos, Entity entity, float density) {
        cache.putBlockDensity(explosionPos, entity, density);
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
    
    private String generateKey(Vec3 explosionPos, Entity entity) {
        return String.format("%d_%d_%d_%s", 
            (int)explosionPos.x, (int)explosionPos.y, (int)explosionPos.z, 
            entity.getUUID());
    }
    
    public static void shutdown() {
        
        INSTANCES.clear();
    }
}
