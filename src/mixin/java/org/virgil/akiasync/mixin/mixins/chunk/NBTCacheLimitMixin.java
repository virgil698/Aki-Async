package org.virgil.akiasync.mixin.mixins.chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@Pseudo
@Mixin(targets = "net.minecraft.world.level.chunk.storage.IOWorker", remap = false)
public class NBTCacheLimitMixin {

    
    @Unique
    private static final int MAX_NBT_CACHE_SIZE = 256;
    
    
    @Unique
    private static final int CLEANUP_THRESHOLD = MAX_NBT_CACHE_SIZE + 64;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static final AtomicLong cleanupCount = new AtomicLong(0);
    
    @Unique
    private static final AtomicLong evictedEntries = new AtomicLong(0);
    
    @Unique
    private static volatile long lastLogTime = 0L;

    
    @Unique
    private static <K, V> void akiasync$limitCacheSize(Map<K, V> cache) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }
        
        if (cache == null) {
            return;
        }
        
        try {
            int size = cache.size();
            
            if (size > CLEANUP_THRESHOLD) {
                int toRemove = size - MAX_NBT_CACHE_SIZE;
                
                if (cache instanceof ConcurrentHashMap) {
                    
                    var iterator = cache.entrySet().iterator();
                    int removed = 0;
                    while (iterator.hasNext() && removed < toRemove) {
                        iterator.next();
                        iterator.remove();
                        removed++;
                    }
                    evictedEntries.addAndGet(removed);
                } else {
                    
                    var keys = cache.keySet().stream()
                        .limit(toRemove)
                        .toList();
                    
                    for (K key : keys) {
                        cache.remove(key);
                    }
                    evictedEntries.addAndGet(keys.size());
                }
                
                cleanupCount.incrementAndGet();
                akiasync$logStatistics();
            }
        } catch (Exception e) {
            
        }
    }

    
    @Unique
    private static <K, V> void akiasync$forceClearCache(Map<K, V> cache) {
        if (cache != null) {
            int size = cache.size();
            cache.clear();
            evictedEntries.addAndGet(size);
            cleanupCount.incrementAndGet();
        }
    }

    @Unique
    private static void akiasync$logStatistics() {
        if (!initialized) {
            initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-NBTCache] NBT cache limit optimization enabled");
            BridgeConfigCache.debugLog("[AkiAsync-NBTCache] Max cache size: " + MAX_NBT_CACHE_SIZE);
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > 60000) {
            lastLogTime = currentTime;
            BridgeConfigCache.debugLog(String.format(
                "[AkiAsync-NBTCache] Stats - Cleanups: %d, Evicted entries: %d",
                cleanupCount.get(), evictedEntries.get()
            ));
        }
    }
}
