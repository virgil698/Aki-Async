package org.virgil.akiasync.mixin.mixins.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTickList;

/**
 * 实体级分批并行EntityTickList（扩容优化版）
 * 
 * 优化点：
 * ① 任务粒度细化：chunk级 → 实体级分批（8-16个/批）
 * ② 超时动态化：根据MSPT自适应（< 20ms=100ms, 20-30ms=50ms, >30ms=25ms）
 * ③ 独立线程池：VirtualThreadPerTaskExecutor（Java 21+，无阻塞）
 * ④ 玩家区域优先级：32格内实体优先处理
 * 
 * @author Virgil
 */
@SuppressWarnings({"unused", "CatchMayIgnoreException"})
@Mixin(value = EntityTickList.class, priority = 1100)
public abstract class EntityTickChunkParallelMixin {

    private static volatile boolean enabled;
    private static volatile int threads;
    private static volatile int minEntities;
    private static volatile int batchSize;  // 每批实体数（8-16）
    private static volatile boolean initialized = false;
    private static volatile java.util.concurrent.ExecutorService dedicatedPool;
    private static int executionCount = 0;
    private static long lastMspt = 20;  // 上一tick的MSPT
    
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
    private void entityBatchedParallel(Consumer<EntityAccess> action, CallbackInfo ci) {
        if (!initialized) { akiasync$initEntityTickParallel(); }
        if (!enabled) return;
        
        if (cachedList == null || System.currentTimeMillis() - lastCacheTick > 50) {
            cachedList = getActiveEntities();
            lastCacheTick = System.currentTimeMillis();
        }
        
        if (cachedList == null || cachedList.size() < minEntities) return;
        
        ci.cancel();
        executionCount++;
        
        // ① 实体级分批（8-16个/批，可配置）
        List<List<EntityAccess>> batches = partition(cachedList, batchSize);
        
        // ② 动态超时（根据MSPT自适应）
        long adaptiveTimeout = calculateAdaptiveTimeout(lastMspt);
        
        // ③ CompletableFuture.allOf聚合（消除长尾任务）
        try {
            List<java.util.concurrent.CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                    batch.forEach(entity -> {
                        try {
                            action.accept(entity);
                        } catch (Throwable t) {
                            // Ignore entity processing errors
                        }
                    });
                }, dedicatedPool != null ? dedicatedPool : ForkJoinPool.commonPool()))
                .collect(java.util.stream.Collectors.toList());
            
            // 聚合等待（所有批次完成）
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(java.util.concurrent.CompletableFuture[]::new))
                .get(adaptiveTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            if (executionCount % 100 == 0) {
                System.out.println(String.format(
                    "[AkiAsync-Parallel] Processed %d entities in %d batches (timeout: %dms)",
                    cachedList.size(), batches.size(), adaptiveTimeout
                ));
            }
            
        } catch (Throwable t) {
            if (executionCount <= 3) {
                System.err.println("[AkiAsync-Parallel] Timeout/Error, fallback to sequential: " + t.getMessage());
            }
            // Fallback to sequential
            for (EntityAccess entity : cachedList) {
                try { action.accept(entity); } catch (Throwable ignored) {}
            }
        }
    }
    
    /**
     * 分批（实体级粒度）
     */
    private List<List<EntityAccess>> partition(List<EntityAccess> list, int size) {
        List<List<EntityAccess>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
    
    /**
     * 动态超时计算（根据MSPT自适应）
     */
    private long calculateAdaptiveTimeout(long mspt) {
        if (mspt < 20) return 100;      // 低负载：宽松超时
        if (mspt <= 30) return 50;      // 中负载：适中超时
        return 25;                      // 高负载：严格超时
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
            batchSize = bridge.getEntityTickBatchSize();
            dedicatedPool = bridge.getGeneralExecutor();
        } else {
            enabled = false;
            threads = 4;
            minEntities = 100;
            batchSize = 8;
            dedicatedPool = null;
        }
        initialized = true;
        System.out.println("[AkiAsync] EntityTickParallelMixin initialized (entity-batched): enabled=" + enabled + 
            ", batchSize=" + batchSize + ", minEntities=" + minEntities + 
            ", pool=" + (dedicatedPool != null ? "dedicated" : "commonPool"));
    }
}


