package org.virgil.akiasync.mixin.mixins.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTickList;

/**
 * Chunk-based parallel EntityTickList (Async mod inspired).
 * Groups entities by chunk for better cache locality.
 */
@SuppressWarnings({"unused", "CatchMayIgnoreException"})
@Mixin(value = EntityTickList.class, priority = 1100)
public abstract class EntityTickChunkParallelMixin {

    private static volatile boolean enabled;
    private static volatile int threads;
    private static volatile int minEntities;
    private static volatile boolean useChunkBased;
    private static volatile boolean initialized = false;
    private static volatile java.util.concurrent.ExecutorService dedicatedPool;
    private static int executionCount = 0;
    
    // Nitori/Lithium optimization: Cache reflection Field to avoid repeated lookups
    private static final java.lang.reflect.Field ACTIVE_FIELD_CACHE;
    
    static {
        java.lang.reflect.Field tempField = null;
        try {
            // Try multiple possible field names (Paper may remap)
            for (String fieldName : new String[]{"active", "f", "b", "entries", "activeEntities"}) {
                try {
                    tempField = EntityTickList.class.getDeclaredField(fieldName);
                    tempField.setAccessible(true);
                    System.out.println("[AkiAsync] EntityTickList field cached successfully: " + fieldName);
                    break;
                } catch (NoSuchFieldException ignored) {
                    // Try next name
                }
            }
            if (tempField == null) {
                System.out.println("[AkiAsync] EntityTickList field not found, will use reflection fallback");
            }
        } catch (Exception e) {
            System.err.println("[AkiAsync] Failed to cache field: " + e.getMessage());
        }
        ACTIVE_FIELD_CACHE = tempField;
    }
    
    private java.util.List<EntityAccess> cachedList;
    private long lastCacheTick;

    @Inject(method = "forEach", at = @At("HEAD"), cancellable = true)
    private void chunkBasedParallel(Consumer<EntityAccess> action, CallbackInfo ci) {
        if (!initialized) { akiasync$initEntityTickParallel(); }
        if (!enabled || !useChunkBased) return;
        
        if (cachedList == null || System.currentTimeMillis() - lastCacheTick > 50) {
            cachedList = getActiveEntities();
            lastCacheTick = System.currentTimeMillis();
        }
        
        if (cachedList == null || cachedList.size() < minEntities) return;
        
        ci.cancel();
        
        // Group by chunk (Async mod pattern)
        Map<Long, List<EntityAccess>> chunkGroups = new ConcurrentHashMap<>();
        for (EntityAccess entityAccess : cachedList) {
            try {
                // Get actual entity to access position
                net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) entityAccess;
                long chunkKey = ChunkPos.asLong(
                    entity.getBlockX() >> 4,
                    entity.getBlockZ() >> 4
                );
                chunkGroups.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(entityAccess);
            } catch (Throwable t) {
                // Catch all to prevent server crash - fallback to immediate processing
                try { action.accept(entityAccess); } catch (Throwable ignored) {}
            }
        }
        
        // Log first few executions to confirm parallel processing
        executionCount++;
        if (executionCount <= 5) {
            System.out.println("[AkiAsync-Parallel] Processing " + cachedList.size() + " entities in " + chunkGroups.size() + " chunks using ForkJoinPool");
        }
        
        // Nitori optimization: Use dedicated thread pool instead of ForkJoinPool.commonPool()
        // Provides better control and avoids competition with other tasks
        try {
            if (dedicatedPool != null) {
                // Use configured thread pool from Bridge
                java.util.concurrent.Future<?> future = dedicatedPool.submit(() -> {
                    chunkGroups.values().parallelStream().forEach(chunkEntities -> {
                        if (executionCount <= 3) {
                            System.out.println("[AkiAsync-Parallel] Thread " + Thread.currentThread().getName() + " processing chunk with " + chunkEntities.size() + " entities");
                        }
                        for (EntityAccess entity : chunkEntities) {
                            try {
                                action.accept(entity);
                            } catch (Throwable t) {
                                // Ignore entity processing errors
                            }
                        }
                    });
                });
                future.get(100, java.util.concurrent.TimeUnit.MILLISECONDS);  // Timeout protection
            } else {
                // Fallback to ForkJoinPool if pool not available
                ForkJoinPool.commonPool().submit(() -> {
                    chunkGroups.values().parallelStream().forEach(chunkEntities -> {
                        for (EntityAccess entity : chunkEntities) {
                            try {
                                action.accept(entity);
                            } catch (Throwable t) {}
                        }
                    });
                }).join();
            }
        } catch (Throwable t) {
            System.err.println("[AkiAsync-Error] Parallel execution failed, falling back to sequential: " + t.getMessage());
            // Fallback to sequential
            for (EntityAccess entity : cachedList) {
                try { action.accept(entity); } catch (Throwable ignored) {
                    // Ignore entity processing errors in fallback
                }
            }
        }
    }

    private List<EntityAccess> getActiveEntities() {
        // Nitori/Lithium optimization: Use cached Field instead of reflection lookup
        if (ACTIVE_FIELD_CACHE == null) return null;
        
        try {
            Object map = ACTIVE_FIELD_CACHE.get(this);
            if (map instanceof Map) {
                return new ArrayList<>(((Map<?, EntityAccess>) map).values());
            }
        } catch (IllegalAccessException | ClassCastException e) {
            // Reflection errors - field structure changed or access denied
        } catch (Exception e) {
            // Other errors
        }
        return null;
    }
    
    private static synchronized void akiasync$initEntityTickParallel() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isEntityTickParallel();
            threads = bridge.getEntityTickThreads();
            minEntities = bridge.getMinEntitiesForParallel();
            dedicatedPool = bridge.getGeneralExecutor();  // Nitori: Get dedicated pool
            useChunkBased = true;
        } else {
            enabled = false;
            threads = 4;
            minEntities = 100;
            dedicatedPool = null;
            useChunkBased = false;
        }
        initialized = true;
        System.out.println("[AkiAsync] EntityTickParallelMixin initialized: enabled=" + enabled + 
            ", threads=" + threads + ", minEntities=" + minEntities + 
            ", pool=" + (dedicatedPool != null ? "dedicated" : "commonPool"));
    }
}

