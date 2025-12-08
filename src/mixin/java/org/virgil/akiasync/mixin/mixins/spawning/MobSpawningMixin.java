package org.virgil.akiasync.mixin.mixins.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = NaturalSpawner.class, priority = 1100)
public class MobSpawningMixin {
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile boolean optimizationEnabled = false;
    
    @Unique
    private static volatile boolean cacheEnabled = true;
    
    @Unique
    private static final ConcurrentHashMap<Long, Long> lastSpawnTimeCache = new ConcurrentHashMap<>();
    
    @Unique
    private static volatile int cacheAccessCount = 0;
    @Unique
    private static final int CACHE_CLEANUP_INTERVAL = 1000;
    @Unique
    private static final int MAX_CACHE_SIZE = 5000;
    
    @Inject(
        method = "spawnForChunk",
        at = @At("HEAD"),
        cancellable = false
    )
    private static void onSpawnForChunk(
        ServerLevel level,
        LevelChunk chunk,
        NaturalSpawner.SpawnState spawnState,
        List<MobCategory> categories,
        CallbackInfo ci
    ) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!optimizationEnabled) {
            return;
        }
        
        if (cacheEnabled && ++cacheAccessCount >= CACHE_CLEANUP_INTERVAL) {
            cacheAccessCount = 0;
            akiasync$cleanupCache();
        }
        
        if (cacheEnabled) {
            long chunkPos = chunk.getPos().toLong();
            lastSpawnTimeCache.put(chunkPos, System.currentTimeMillis());
        }
    }
    
    @Unique
    private static void akiasync$cleanupCache() {
        if (lastSpawnTimeCache.size() > MAX_CACHE_SIZE) {
            long currentTime = System.currentTimeMillis();
            long expirationTime = 60000; 
            
            lastSpawnTimeCache.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > expirationTime
            );
            
            BridgeConfigCache.debugLog("[MobSpawn] Cache cleaned, size: " + lastSpawnTimeCache.size());
        }
    }
    
    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            optimizationEnabled = bridge.isMobSpawningEnabled();
            
            BridgeConfigCache.debugLog("[AkiAsync-MobSpawn] Synchronous mob spawning optimization initialized");
            BridgeConfigCache.debugLog("[AkiAsync-MobSpawn]   Optimization enabled: " + optimizationEnabled);
            BridgeConfigCache.debugLog("[AkiAsync-MobSpawn]   Cache enabled: " + cacheEnabled);
            BridgeConfigCache.debugLog("[AkiAsync-MobSpawn]   Strategy: Sync with caching and pre-checks");
            BridgeConfigCache.debugLog("[AkiAsync-MobSpawn]   Reference: Luminol (avoid chunk loading)");
        }
        
        initialized = true;
    }
    
    @Unique
    private static String akiasync$getCacheStats() {
        return String.format("MobSpawnCache: size=%d/%d", 
            lastSpawnTimeCache.size(), MAX_CACHE_SIZE);
    }
    
    @Unique
    private static void akiasync$clearCache() {
        lastSpawnTimeCache.clear();
        BridgeConfigCache.debugLog("[MobSpawn] All caches cleared");
    }
}
