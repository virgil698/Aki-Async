package org.virgil.akiasync.mixin.mixins.lighting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LightEngine;
import org.virgil.akiasync.mixin.lighting.LightingOptimizationManager;
import org.virgil.akiasync.mixin.lighting.LightUpdatePriority;
import org.virgil.akiasync.mixin.lighting.LightUpdateRequest;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@SuppressWarnings({"unused", "rawtypes"}) 
@Mixin(value = LightEngine.class, priority = 1100)
public abstract class LightEngineAsyncMixin {
    private static volatile boolean enabled;
    private static volatile ExecutorService lightingExecutor;
    private static volatile int batchThreshold = 16;
    private static volatile int baseBatchThreshold = 16;
    private static volatile boolean useLayeredQueue = true;
    private static volatile int maxPropagationDistance = 15;
    private static volatile boolean deduplicationEnabled = true;
    private static volatile boolean dynamicAdjustmentEnabled = true;
    private static volatile boolean advancedStatsEnabled = false;
    private static volatile boolean initialized = false;
    private static volatile boolean isFolia = false;
    
    private static final List<List<Queue<LightUpdateRequest>>> PRIORITY_LAYERED_QUEUES = new ArrayList<>(4);
    private static final List<List<AtomicInteger>> priorityLayerSizes = new ArrayList<>(4);
    
    private static final List<Queue<BlockPos>> LAYERED_QUEUES = new ArrayList<>(16);
    private static final List<AtomicInteger> layerSizes = new ArrayList<>(16);
    private static final Map<BlockPos, Long> UPDATE_METADATA = new ConcurrentHashMap<>();
    private static final Set<BlockPos> PENDING_UPDATES = ConcurrentHashMap.newKeySet();
    private static final Queue<BlockPos> LIGHT_UPDATE_QUEUE = new LinkedBlockingQueue<>(5000);
    private static final AtomicInteger queueSize = new AtomicInteger(0);
    private static volatile boolean processing = false;
    private static int batchCount = 0;
    private static long lastAdjustmentTime = System.currentTimeMillis();
    private static final long ADJUSTMENT_INTERVAL = 5000;
    
    private static volatile boolean usePriorityScheduling = false;
    private static volatile boolean useDebouncing = false;
    private static volatile boolean useMerging = false;
    private static volatile boolean useAdaptive = false;
    
    private static final AtomicInteger totalProcessed = new AtomicInteger(0);
    private static volatile long lastMetricsTime = System.currentTimeMillis();
    
    static {
        
        for (int priority = 0; priority < 4; priority++) {
            List<Queue<LightUpdateRequest>> layerQueues = new ArrayList<>(16);
            List<AtomicInteger> layerSizeList = new ArrayList<>(16);
            for (int level = 0; level < 16; level++) {
                layerQueues.add(new PriorityBlockingQueue<>(100));
                layerSizeList.add(new AtomicInteger(0));
            }
            PRIORITY_LAYERED_QUEUES.add(layerQueues);
            priorityLayerSizes.add(layerSizeList);
        }
        
        for (int i = 0; i < 16; i++) {
            LAYERED_QUEUES.add(new LinkedBlockingQueue<>(5000));
            layerSizes.add(new AtomicInteger(0));
        }
    }
    @Inject(method = "checkBlock", at = @At("HEAD"), cancellable = true)
    private void batchLightUpdate(BlockPos pos, CallbackInfo ci) {
        if (!initialized) { akiasync$initLightEngine(); }
        if (!enabled || processing) return;

        try {
            BlockPos immutablePos = pos.immutable();
            
            if (deduplicationEnabled) {
                if (!PENDING_UPDATES.add(immutablePos)) {
                    ci.cancel();
                    return;
                }
            }
            
            int lightLevel = getLightLevel(pos);
            
            LightUpdatePriority priority = LightUpdatePriority.NORMAL;
            boolean isChunkLoading = false;
            
            if (usePriorityScheduling) {
                try {
                    LightEngine lightEngine = (LightEngine) (Object) this;
                    Level level = akiasync$getLevel(lightEngine);
                    if (level instanceof ServerLevel) {
                        priority = LightingOptimizationManager.calculatePriority(
                            immutablePos, (ServerLevel) level, isChunkLoading
                        );
                        
                        if (priority == LightUpdatePriority.CRITICAL) {
                            isChunkLoading = true;
                        }
                    }
                } catch (Exception e) {
                    
                }
            }
            
            if (isChunkLoading || priority == LightUpdatePriority.CRITICAL) {
                
                LightUpdateRequest request = new LightUpdateRequest(immutablePos, lightLevel, LightUpdatePriority.CRITICAL, true);
                
                if (useLayeredQueue && usePriorityScheduling) {
                    int priorityIndex = LightUpdatePriority.CRITICAL.getValue();
                    PRIORITY_LAYERED_QUEUES.get(priorityIndex).get(lightLevel).offer(request);
                    priorityLayerSizes.get(priorityIndex).get(lightLevel).incrementAndGet();
                } else if (useLayeredQueue) {
                    LAYERED_QUEUES.get(lightLevel).offer(immutablePos);
                    layerSizes.get(lightLevel).incrementAndGet();
                    UPDATE_METADATA.put(immutablePos, ((long)lightLevel << 32) | System.currentTimeMillis());
                } else {
                    LIGHT_UPDATE_QUEUE.offer(immutablePos);
                    queueSize.incrementAndGet();
                }
                
                int fastThreshold = Math.max(4, batchThreshold / 4);
                int totalSize = getTotalQueueSize();
                
                if (totalSize >= fastThreshold && !processing) {
                    processing = true;
                    ci.cancel();
                    
                    akiasync$processBatchImmediate();
                }
                
                ci.cancel();
                return;
            }
            
            if (useDebouncing && LightingOptimizationManager.shouldDebounce(immutablePos)) {
                PENDING_UPDATES.remove(immutablePos);
                ci.cancel();
                return;
            }
            
            LightUpdateRequest request = new LightUpdateRequest(immutablePos, lightLevel, priority, isChunkLoading);
            
            if (useMerging && LightingOptimizationManager.tryMergeUpdate(request)) {
                ci.cancel();
                return;
            }
            
            if (LightingOptimizationManager.tryBatchBorderUpdate(request)) {
                ci.cancel();
                return;
            }
            
            if (useLayeredQueue && usePriorityScheduling) {
                int priorityIndex = priority.getValue();
                PRIORITY_LAYERED_QUEUES.get(priorityIndex).get(lightLevel).offer(request);
                priorityLayerSizes.get(priorityIndex).get(lightLevel).incrementAndGet();
            } else if (useLayeredQueue) {
                
                LAYERED_QUEUES.get(lightLevel).offer(immutablePos);
                layerSizes.get(lightLevel).incrementAndGet();
                UPDATE_METADATA.put(immutablePos, ((long)lightLevel << 32) | System.currentTimeMillis());
            } else {
                
                LIGHT_UPDATE_QUEUE.offer(immutablePos);
                queueSize.incrementAndGet();
            }

            int totalSize = getTotalQueueSize();
            
            if (dynamicAdjustmentEnabled) {
                adjustBatchSize();
            }
            
            if (useAdaptive) {
                applyAdaptiveRecommendations();
            }

            if (totalSize >= batchThreshold && !processing) {
                processing = true;
                ci.cancel();
                batchCount++;
                
                long startTime = System.currentTimeMillis();
                
                if (batchCount <= 3) {
                    BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Processing batch of " + totalSize + " light updates (threshold: " + batchThreshold + ")");
                }
                
                if (lightingExecutor != null) {
                    CompletableFuture.runAsync(() -> {
                        if (usePriorityScheduling) {
                            processPriorityLayeredBatch();
                        } else if (useLayeredQueue) {
                            processLayeredBatch();
                        } else {
                            processBatch();
                        }
                    }, lightingExecutor).orTimeout(1000, TimeUnit.MILLISECONDS).whenComplete((result, ex) -> {
                        long processingTime = System.currentTimeMillis() - startTime;
                        
                        if (useAdaptive) {
                            LightingOptimizationManager.recordProcessing(totalSize, processingTime);
                        }
                        
                        processing = false;
                        if (ex != null && batchCount <= 3) {
                            BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Batch processing timeout/error, fallback to sync");
                        }
                    });
                } else {
                    if (usePriorityScheduling) {
                        processPriorityLayeredBatch();
                    } else if (useLayeredQueue) {
                        processLayeredBatch();
                    } else {
                        processBatch();
                    }
                    
                    long processingTime = System.currentTimeMillis() - startTime;
                    if (useAdaptive) {
                        LightingOptimizationManager.recordProcessing(totalSize, processingTime);
                    }
                    
                    processing = false;
                }
            }
        } catch (Exception e) {
            processing = false;
        }
    }
    
    private void akiasync$processBatchImmediate() {
        long startTime = System.currentTimeMillis();
        
        if (lightingExecutor != null) {
            CompletableFuture.runAsync(() -> {
                if (usePriorityScheduling) {
                    processPriorityLayeredBatch();
                } else if (useLayeredQueue) {
                    processLayeredBatch();
                } else {
                    processBatch();
                }
            }, lightingExecutor).orTimeout(500, TimeUnit.MILLISECONDS).whenComplete((result, ex) -> {
                long processingTime = System.currentTimeMillis() - startTime;
                
                if (useAdaptive) {
                    LightingOptimizationManager.recordProcessing(getTotalQueueSize(), processingTime);
                }
                
                processing = false;
            });
        } else {
            
            if (usePriorityScheduling) {
                processPriorityLayeredBatch();
            } else if (useLayeredQueue) {
                processLayeredBatch();
            } else {
                processBatch();
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            if (useAdaptive) {
                LightingOptimizationManager.recordProcessing(getTotalQueueSize(), processingTime);
            }
            
            processing = false;
        }
    }
    
    private void processPriorityLayeredBatch() {
        try {
            LightEngine lightEngine = (LightEngine) (Object) this;
            int processedCount = 0;
            int maxProcess = batchThreshold * 2;
            
            for (int priorityIndex = 3; priorityIndex >= 0 && processedCount < maxProcess; priorityIndex--) {
                
                for (int level = 15; level >= 0 && processedCount < maxProcess; level--) {
                    Queue<LightUpdateRequest> queue = PRIORITY_LAYERED_QUEUES.get(priorityIndex).get(level);
                    LightUpdateRequest request;
                    
                    while ((request = queue.poll()) != null && processedCount < maxProcess) {
                        
                        long age = request.getAge();
                        if (age > 5000) {
                            PENDING_UPDATES.remove(request.getPos());
                            priorityLayerSizes.get(priorityIndex).get(level).decrementAndGet();
                            continue;
                        }
                        
                        try {
                            lightEngine.checkBlock(request.getPos());
                            processedCount++;
                        } catch (Exception e) {
                            BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Failed to check block at " + request.getPos() + ": " + e.getMessage());
                        }
                        
                        PENDING_UPDATES.remove(request.getPos());
                        priorityLayerSizes.get(priorityIndex).get(level).decrementAndGet();
                    }
                }
            }
            
            totalProcessed.addAndGet(processedCount);
            
            if (advancedStatsEnabled || batchCount <= 3) {
                if (processedCount > 0) {
                    BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Priority layered batch processed " + processedCount + " updates");
                }
            }
        } catch (Exception e) {
            clearAllQueues();
        }
    }
    
    private void processLayeredBatch() {
        try {
            LightEngine lightEngine = (LightEngine) (Object) this;
            int processedCount = 0;
            int maxProcess = batchThreshold * 2;
            for (int level = 15; level >= 0 && processedCount < maxProcess; level--) {
                Queue<BlockPos> queue = LAYERED_QUEUES.get(level);
                BlockPos pos;
                while ((pos = queue.poll()) != null && processedCount < maxProcess) {
                    Long metadata = UPDATE_METADATA.remove(pos);
                    if (metadata != null) {
                        long timestamp = metadata & 0xFFFFFFFFL;
                        long age = System.currentTimeMillis() - timestamp;
                        if (age > 5000) {
                            PENDING_UPDATES.remove(pos);
                            layerSizes.get(level).decrementAndGet();
                            continue;
                        }
                    }
                    try {
                        lightEngine.checkBlock(pos);
                        processedCount++;
                    } catch (Exception e) {
                        BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Failed to check block at " + pos + ": " + e.getMessage());
                    }
                    PENDING_UPDATES.remove(pos);
                    layerSizes.get(level).decrementAndGet();
                }
            }
            
            totalProcessed.addAndGet(processedCount);
            
            if (advancedStatsEnabled || batchCount <= 3) {
                if (processedCount > 0) {
                    BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Layered batch processed " + processedCount + " updates");
                    if (advancedStatsEnabled) {
                        for (int i = 15; i >= 0; i--) {
                            int size = layerSizes.get(i).get();
                            if (size > 0) {
                                BridgeConfigCache.debugLog("  - Level " + i + ": " + size + " pending");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            clearAllQueues();
        }
    }
    private void processBatch() {
        try {
            LightEngine lightEngine = (LightEngine) (Object) this;
            int processed = 0;
            int maxProcess = batchThreshold * 2;
            BlockPos pos;
            while ((pos = LIGHT_UPDATE_QUEUE.poll()) != null && processed < maxProcess) {
                try {
                    lightEngine.checkBlock(pos);
                    processed++;
                } catch (Exception e) {
                    BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Failed to process light update at " + pos + ": " + e.getMessage());
                }
                PENDING_UPDATES.remove(pos);
                queueSize.decrementAndGet();
            }
            if (batchCount <= 3 && processed > 0) {
                BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Simple batch processed " + processed + " updates");
            }
        } catch (Exception e) {
            clearAllQueues();
        }
    }
    private void adjustBatchSize() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAdjustmentTime < ADJUSTMENT_INTERVAL) {
            return;
        }
        lastAdjustmentTime = currentTime;
        try {
            double tps = getApproximateTPS();
            if (tps < 18.0) {
                batchThreshold = Math.min(baseBatchThreshold * 3, 64);
            } else if (tps < 19.0) {
                batchThreshold = Math.min(baseBatchThreshold * 2, 48);
            } else if (tps > 19.8) {
                batchThreshold = Math.max(baseBatchThreshold / 2, 8);
            } else {
                batchThreshold = baseBatchThreshold;
            }
        } catch (Exception e) {
            batchThreshold = baseBatchThreshold;
        }
    }
    private double getApproximateTPS() {
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                return bridge.getCurrentTPS();
            }
        } catch (Exception e) {
            
        }
        return 20.0;
    }
    private int getTotalQueueSize() {
        if (usePriorityScheduling) {
            int total = 0;
            for (List<AtomicInteger> priorityLayers : priorityLayerSizes) {
                for (AtomicInteger size : priorityLayers) {
                    total += size.get();
                }
            }
            return total;
        } else if (useLayeredQueue) {
            int total = 0;
            for (AtomicInteger size : layerSizes) {
                total += size.get();
            }
            return total;
        }
        return queueSize.get();
    }
    
    private void applyAdaptiveRecommendations() {
        long now = System.currentTimeMillis();
        if (now - lastMetricsTime < 10000) { 
            return;
        }
        lastMetricsTime = now;
        
        try {
            LightingOptimizationManager.AdaptiveRecommendation recommendation = 
                LightingOptimizationManager.getAdaptiveRecommendation();
            
            if (recommendation.batchAdjustment != 0) {
                int newThreshold = batchThreshold + recommendation.batchAdjustment;
                newThreshold = Math.max(8, Math.min(64, newThreshold));
                if (newThreshold != batchThreshold) {
                    batchThreshold = newThreshold;
                    if (advancedStatsEnabled) {
                        BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Adaptive: Adjusted batch threshold to " + batchThreshold);
                    }
                }
            }
            
        } catch (Exception e) {
            
        }
    }
    
    private Level akiasync$getLevel(LightEngine lightEngine) {
        try {
            
            String[] fieldNames = {"level", "lightEngine", "world", "a", "b", "c"};
            for (String fieldName : fieldNames) {
                try {
                    Level level = akiasync$getLevelFromField(lightEngine, lightEngine.getClass().getDeclaredField(fieldName));
                    if (level != null) return level;
                } catch (NoSuchFieldException e) {
                    
                }
            }
            
            Level level = akiasync$searchFieldsForLevel(lightEngine, lightEngine.getClass().getDeclaredFields());
            if (level != null) return level;
            
            Class<?> superClass = lightEngine.getClass().getSuperclass();
            if (superClass != null) {
                return akiasync$searchFieldsForLevel(lightEngine, superClass.getDeclaredFields());
            }
        } catch (Exception e) {
            
        }
        return null;
    }
    
    private Level akiasync$getLevelFromField(Object obj, java.lang.reflect.Field field) {
        try {
            field.setAccessible(true);
            Object value = field.get(obj);
            return value instanceof Level ? (Level) value : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Level akiasync$searchFieldsForLevel(Object obj, java.lang.reflect.Field[] fields) {
        for (java.lang.reflect.Field field : fields) {
            Level level = akiasync$getLevelFromField(obj, field);
            if (level != null) return level;
        }
        return null;
    }
    private synchronized void clearAllQueues() {
        try {
            processing = true;

            for (int i = 0; i < LAYERED_QUEUES.size(); i++) {
                Queue<BlockPos> queue = LAYERED_QUEUES.get(i);
                if (queue != null) {
                    queue.clear();
                }
                AtomicInteger size = layerSizes.get(i);
                if (size != null) {
                    size.set(0);
                }
            }

            LIGHT_UPDATE_QUEUE.clear();
            queueSize.set(0);
            PENDING_UPDATES.clear();
            UPDATE_METADATA.clear();

        } catch (Exception e) {
            BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Error clearing queues: " + e.getMessage());
        } finally {
            processing = false;
        }
    }
    private int getLightLevel(BlockPos pos) {
        try {
            LightEngine lightEngine = (LightEngine) (Object) this;
            
            int level = lightEngine.getLightValue(pos);
            
            return Math.max(0, Math.min(15, level));
        } catch (Exception e) {
            
            return 15;
        }
    }
    private static synchronized void akiasync$initLightEngine() {
        if (initialized) return;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isAsyncLightingEnabled();
            lightingExecutor = bridge.getLightingExecutor();
            batchThreshold = bridge.getLightBatchThreshold();
            useLayeredQueue = bridge.useLayeredPropagationQueue();
            maxPropagationDistance = bridge.getMaxLightPropagationDistance();
            deduplicationEnabled = bridge.isLightDeduplicationEnabled();
            dynamicAdjustmentEnabled = bridge.isDynamicBatchAdjustmentEnabled();
            advancedStatsEnabled = bridge.isAdvancedLightingStatsEnabled();
            baseBatchThreshold = batchThreshold;
            
            usePriorityScheduling = bridge.isLightingPrioritySchedulingEnabled();
            useDebouncing = bridge.isLightingDebouncingEnabled();
            useMerging = bridge.isLightingMergingEnabled();
            useAdaptive = bridge.isLightingAdaptiveEnabled();
            
            if (!LightingOptimizationManager.isInitialized()) {
                LightingOptimizationManager.initialize(
                    bridge.isLightingPrioritySchedulingEnabled(),
                    bridge.getLightingHighPriorityRadius(),
                    bridge.getLightingMediumPriorityRadius(),
                    bridge.getLightingLowPriorityRadius(),
                    bridge.getLightingMaxLowPriorityDelay(),
                    
                    bridge.isLightingDebouncingEnabled(),
                    bridge.getLightingDebounceDelay(),
                    bridge.getLightingMaxUpdatesPerSecond(),
                    bridge.getLightingResetOnStableMs(),
                    
                    bridge.isLightingMergingEnabled(),
                    bridge.getLightingMergeRadius(),
                    bridge.getLightingMergeDelay(),
                    bridge.getLightingMaxMergedUpdates(),
                    
                    bridge.isLightingChunkBorderEnabled(),
                    bridge.isLightingBatchBorderUpdates(),
                    bridge.getLightingBorderUpdateDelay(),
                    bridge.getLightingCrossChunkBatchSize(),
                    
                    bridge.isLightingAdaptiveEnabled(),
                    bridge.getLightingMonitorInterval(),
                    bridge.isLightingAutoAdjustThreads(),
                    bridge.isLightingAutoAdjustBatchSize(),
                    bridge.getLightingTargetQueueSize(),
                    bridge.getLightingTargetLatency(),
                    
                    bridge.isLightingChunkUnloadEnabled(),
                    bridge.isLightingAsyncCleanup(),
                    bridge.getLightingCleanupBatchSize(),
                    bridge.getLightingCleanupDelay()
                );
            }

            if (isFolia) {
                BridgeConfigCache.debugLog("[AkiAsync] LightEngineAsyncMixin (ScalableLux Enhanced) initialized in Folia mode:");
                BridgeConfigCache.debugLog("  - Enabled: " + enabled + " (with region safety checks)");
                BridgeConfigCache.debugLog("  - Batch threshold: " + batchThreshold + " (dynamic: " + dynamicAdjustmentEnabled + ")");
                BridgeConfigCache.debugLog("  - Layered queue (16 levels): " + useLayeredQueue);
                BridgeConfigCache.debugLog("  - Region-aware async processing enabled");
            } else {
                BridgeConfigCache.debugLog("[AkiAsync] LightEngineAsyncMixin (ScalableLux Enhanced) initialized:");
                BridgeConfigCache.debugLog("  - Enabled: " + enabled);
                BridgeConfigCache.debugLog("  - Batch threshold: " + batchThreshold + " (dynamic: " + dynamicAdjustmentEnabled + ")");
                BridgeConfigCache.debugLog("  - Layered queue (16 levels): " + useLayeredQueue);
                BridgeConfigCache.debugLog("  - Max propagation distance: " + maxPropagationDistance);
                BridgeConfigCache.debugLog("  - Deduplication: " + deduplicationEnabled);
                BridgeConfigCache.debugLog("  - Advanced stats: " + advancedStatsEnabled);
                BridgeConfigCache.debugLog("  - ScalableLux Optimizations:");
                BridgeConfigCache.debugLog("    * Priority Scheduling: " + usePriorityScheduling);
                BridgeConfigCache.debugLog("    * Debouncing: " + useDebouncing);
                BridgeConfigCache.debugLog("    * Update Merging: " + useMerging);
                BridgeConfigCache.debugLog("    * Adaptive Tuning: " + useAdaptive);
            }
        } else {
            enabled = false;
            lightingExecutor = null;
            batchThreshold = 16;
            useLayeredQueue = true;
            maxPropagationDistance = 15;
            deduplicationEnabled = true;
            dynamicAdjustmentEnabled = true;
            advancedStatsEnabled = false;
            usePriorityScheduling = false;
            useDebouncing = false;
            useMerging = false;
            useAdaptive = false;
        }
        initialized = true;
    }
}
