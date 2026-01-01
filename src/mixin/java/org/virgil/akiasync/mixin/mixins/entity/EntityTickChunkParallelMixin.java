package org.virgil.akiasync.mixin.mixins.entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.util.EntitySyncChecker;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTickList;

@SuppressWarnings({"unused", "CatchMayIgnoreException"})
@Mixin(value = EntityTickList.class, priority = 1100)
public abstract class EntityTickChunkParallelMixin {
    private static volatile boolean enabled;
    private static volatile int threads;
    private static volatile int minEntities;
    private static volatile int batchSize;
    private static volatile boolean initialized = false;
    private static volatile java.util.concurrent.ExecutorService dedicatedPool;
    private static volatile boolean isFolia = false;
    private static volatile Object smoothingScheduler;
    private static int executionCount = 0;
    private static long lastMspt = 20;
    private static final java.lang.reflect.Field ACTIVE_FIELD_CACHE;

    private static final java.util.Set<Integer> processingExperienceOrbs =
        java.util.concurrent.ConcurrentHashMap.newKeySet();
    static {
        java.lang.reflect.Field tempField = null;
        try {
            
            String[] fieldNames = {"active", "f", "b", "c", "d", "e", "entries", "activeEntities", "iterable"};
            
            for (String fieldName : fieldNames) {
                try {
                    tempField = EntityTickList.class.getDeclaredField(fieldName);
                    tempField.setAccessible(true);
                    
                    if (Map.class.isAssignableFrom(tempField.getType())) {
                        BridgeConfigCache.debugLog("[AkiAsync] EntityTickList field found: " + fieldName);
                        break;
                    } else {
                        tempField = null; 
                    }
                } catch (NoSuchFieldException e) {
                    
                }
            }
            
            if (tempField == null) {
                BridgeConfigCache.debugLog("[AkiAsync] EntityTickList field not found. Available fields:");
                for (Field f : EntityTickList.class.getDeclaredFields()) {
                    BridgeConfigCache.debugLog("[AkiAsync]   - " + f.getName() + " : " + f.getType().getSimpleName());
                }
                BridgeConfigCache.debugLog("[AkiAsync] Entity tick parallelization will be disabled (feature gracefully degraded)");
            }
        } catch (Exception e) {
            BridgeConfigCache.debugLog("[AkiAsync] EntityTickList field lookup error: " + 
                e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        ACTIVE_FIELD_CACHE = tempField;
    }
    private java.util.List<EntityAccess> cachedList;
    private long lastCacheTick;
    @Inject(method = "forEach", at = @At("HEAD"), cancellable = true)
    private void entityBatchedParallel(Consumer<EntityAccess> action, CallbackInfo ci) {
        if (!initialized) { akiasync$initEntityTickParallel(); }
        if (!enabled) return;
        
        if (smoothingScheduler != null) {
            var bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.notifySmoothSchedulerTick(smoothingScheduler);
                bridge.updateSmoothSchedulerMetrics(smoothingScheduler, bridge.getCurrentTPS(), bridge.getCurrentMSPT());
            
                initialized = true;
            }
        }
        if (cachedList == null || System.currentTimeMillis() - lastCacheTick > 50) {
            cachedList = getActiveEntities();
            lastCacheTick = System.currentTimeMillis();
        }
        if (cachedList == null || cachedList.size() < minEntities) return;
        ci.cancel();
        executionCount++;
        
        
        List<EntityAccess> syncEntities = new ArrayList<>();
        List<EntityAccess> asyncEntities = new ArrayList<>();
        
        for (EntityAccess entityAccess : cachedList) {
            if (org.virgil.akiasync.mixin.util.VirtualEntityCheck.is(entityAccess)) continue;
            
            if (entityAccess instanceof Entity realEntity) {
                if (EntitySyncChecker.shouldTickSynchronously(realEntity)) {
                    syncEntities.add(entityAccess);
                } else {
                    asyncEntities.add(entityAccess);
                }
            } else {
                
                syncEntities.add(entityAccess);
            }
        }
        
        
        for (EntityAccess entity : syncEntities) {
            try {
                action.accept(entity);
            } catch (Throwable t) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "EntityTickParallel", "syncTick", 
                    t instanceof Exception ? (Exception) t : new RuntimeException(t));
            }
        }
        
        
        if (asyncEntities.isEmpty()) return;
        
        if (smoothingScheduler != null && !isFolia) {
            var bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {

                java.util.Map<Integer, java.util.List<Runnable>> tasksByPriority = new java.util.HashMap<>();
                
                for (EntityAccess entity : asyncEntities) {
                    int priority = akiasync$determineEntityPriority(entity);
                    tasksByPriority.computeIfAbsent(priority, k -> new java.util.ArrayList<>())
                        .add(() -> akiasync$tickEntityAsync(entity, action));
                
                        initialized = true;
                    }
                
                for (java.util.Map.Entry<Integer, java.util.List<Runnable>> entry : tasksByPriority.entrySet()) {
                    bridge.submitSmoothTaskBatch(smoothingScheduler, entry.getValue(), entry.getKey(), "EntityTick");
                }
            }
            return;
        }
        List<List<EntityAccess>> batches = partition(asyncEntities, batchSize);
        long adaptiveTimeout = calculateAdaptiveTimeout(lastMspt);
        try {
            
            Bridge bridge = BridgeManager.getBridge();
            ExecutorService executor = dedicatedPool != null ? dedicatedPool : 
                (bridge != null ? bridge.getGeneralExecutor() : null);
            
            if (executor == null) {
                
                batches.forEach(batch -> batch.forEach(entity -> akiasync$tickEntityAsync(entity, action)));
                return;
            }
            
            List<java.util.concurrent.CompletableFuture<Void>> futures = batches.stream()
                .<java.util.concurrent.CompletableFuture<Void>>map(batch -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                    batch.forEach(entity -> akiasync$tickEntityAsync(entity, action));
                }, executor))
                .collect(java.util.stream.Collectors.toList());
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(java.util.concurrent.CompletableFuture[]::new))
                .get(adaptiveTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (executionCount % 100 == 0) {
                BridgeConfigCache.debugLog(
                    "[AkiAsync-Parallel] Processed %d entities (%d sync, %d async) in %d batches (timeout: %dms)",
                    cachedList.size(), syncEntities.size(), asyncEntities.size(), batches.size(), adaptiveTimeout
                );
            }
        } catch (Throwable t) {
            if (executionCount <= 3) {
                BridgeConfigCache.errorLog("[AkiAsync-Parallel] Timeout/Error, fallback to sequential: " + t.getMessage());
            }
            for (EntityAccess entity : asyncEntities) {
                try { 
                    action.accept(entity); 
                } catch (Throwable e) {
                    BridgeConfigCache.errorLog("[EntityTick] Error ticking entity %s: %s", 
                        entity.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
    }
    
    private void akiasync$tickEntityAsync(EntityAccess entity, Consumer<EntityAccess> action) {
        try {
            if (entity instanceof Entity realEntity) {
                
                if (realEntity instanceof net.minecraft.world.entity.ExperienceOrb orb) {
                    int entityId = realEntity.getId();
                    if (orb.isRemoved()) return;
                    if (!processingExperienceOrbs.add(entityId)) return;
                    try {
                        action.accept(entity);
                    } finally {
                        processingExperienceOrbs.remove(entityId);
                    }
                    return;
                }
            }
            action.accept(entity);
        } catch (Throwable t) {
            
            if (entity instanceof Entity realEntity) {
                EntitySyncChecker.blacklistEntity(realEntity.getUUID());
            }
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityTickParallel", "asyncTick", 
                t instanceof Exception ? (Exception) t : new RuntimeException(t));
        }
    }
    private List<List<EntityAccess>> partition(List<EntityAccess> list, int size) {
        List<List<EntityAccess>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
    private long calculateAdaptiveTimeout(long mspt) {
        if (mspt < 20) return 100;
        if (mspt <= 30) return 50;
        return 25;
    }
    
    @SuppressWarnings("unchecked") 
    private List<EntityAccess> getActiveEntities() {
        if (ACTIVE_FIELD_CACHE == null) return null;
        Object map = null;
        try {
            map = ACTIVE_FIELD_CACHE.get(this);
            if (map instanceof Map) {
                Map<?, ?> rawMap = (Map<?, ?>) map;
                
                boolean allValid = true;
                for (Object value : rawMap.values()) {
                    if (value != null && !(value instanceof EntityAccess)) {
                        allValid = false;
                        break;
                    }
                }
                
                if (allValid) {
                    
                    Map<?, EntityAccess> typedMap = (Map<?, EntityAccess>) rawMap;
                    return new ArrayList<>(typedMap.values());
                }
            }
        } catch (IllegalAccessException e) {
            BridgeConfigCache.errorLog("[EntityTickParallel] Field access denied: " + e.getMessage());
        } catch (ClassCastException e) {
            BridgeConfigCache.errorLog("[EntityTickParallel] ClassCastException: expected Map but got " + 
                (map != null ? map.getClass().getName() : "null"));
        } catch (Exception e) {
            BridgeConfigCache.errorLog("[EntityTickParallel] Unexpected error getting entities: " + 
                e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return null;
    }

    private static synchronized void akiasync$initEntityTickParallel() {
        if (initialized) return;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            if (isFolia) {
                enabled = false;
                BridgeConfigCache.debugLog("[AkiAsync] EntityTickParallelMixin disabled in Folia mode (Region threading already handles parallelism)");
            
                initialized = true;
            } else {
                enabled = bridge.isEntityTickParallel();
            }
            threads = bridge.getEntityTickThreads();
            minEntities = bridge.getMinEntitiesForParallel();
            batchSize = bridge.getEntityTickBatchSize();
            dedicatedPool = bridge.getGeneralExecutor();
        } else {
            enabled = false;
            threads = 4;
            minEntities = 100;
            batchSize = 8;
            dedicatedPool = null;
        }
        
        if (bridge != null && enabled && !isFolia) {
            smoothingScheduler = bridge.getEntityTickSmoothingScheduler();
            if (smoothingScheduler != null) {
                BridgeConfigCache.debugLog("[AkiAsync] EntityTick TaskSmoothingScheduler obtained from Bridge");
            }
        }
        
        BridgeConfigCache.debugLog("[AkiAsync] EntityTickParallelMixin initialized (entity-batched): enabled=" + enabled +
            ", isFolia=" + isFolia + ", batchSize=" + batchSize + ", minEntities=" + minEntities +
            ", pool=" + (dedicatedPool != null ? "dedicated" : "commonPool"));
    }
    
    private static int akiasync$determineEntityPriority(EntityAccess entity) {
        if (entity == null) return 3;
        
        try {
            if (entity instanceof net.minecraft.world.entity.Entity realEntity) {

                if (realEntity instanceof net.minecraft.server.level.ServerPlayer) {
                    return 0;
                }
                
                if (realEntity instanceof net.minecraft.world.entity.Mob mob) {
                    
                    if (mob.getTarget() != null || mob.getLastHurtByMob() != null) {
                        return 0;
                    }
                    
                    return 2;
                }
                
                if (realEntity instanceof net.minecraft.world.entity.ExperienceOrb ||
                    realEntity instanceof net.minecraft.world.entity.item.ItemEntity) {
                    return 3;
                }
                
                return 2;
            }
        } catch (Throwable t) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityTickParallel", "determinePriority", 
                t instanceof Exception ? (Exception) t : new RuntimeException(t));
        }
        
        return 3;
    }
}
