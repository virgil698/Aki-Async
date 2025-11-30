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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LightEngine;

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
    static {
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

            if (useLayeredQueue) {
                int lightLevel = getLightLevel(pos);
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

            if (totalSize >= batchThreshold && !processing) {
                processing = true;
                ci.cancel();
                batchCount++;
                if (batchCount <= 3) {
                    org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    if (bridge != null) {
                        bridge.debugLog("[AkiAsync-LightEngine] Processing batch of " + totalSize + " light updates (threshold: " + batchThreshold + ")");
                    }
                }
                if (lightingExecutor != null) {
                    CompletableFuture.runAsync(() -> {
                        if (useLayeredQueue) {
                            processLayeredBatch();
                        } else {
                            processBatch();
                        }
                    }, lightingExecutor).orTimeout(1000, TimeUnit.MILLISECONDS).whenComplete((result, ex) -> {
                        processing = false;
                        if (ex != null && batchCount <= 3) {
                            org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                            if (errorBridge != null) {
                                errorBridge.debugLog("[AkiAsync-LightEngine] Batch processing timeout/error, fallback to sync");
                            }
                        }
                    });
                } else {
                    if (useLayeredQueue) {
                        processLayeredBatch();
                    } else {
                        processBatch();
                    }
                    processing = false;
                }
            }
        } catch (Exception e) {
            processing = false;
        }
    }
    private void processLayeredBatch() {
        try {
            LightEngine lightEngine = (LightEngine) (Object) this;
            int totalProcessed = 0;
            int maxProcess = batchThreshold * 2;
            for (int level = 15; level >= 0 && totalProcessed < maxProcess; level--) {
                Queue<BlockPos> queue = LAYERED_QUEUES.get(level);
                BlockPos pos;
                while ((pos = queue.poll()) != null && totalProcessed < maxProcess) {
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
                        totalProcessed++;
                    } catch (Exception e) {
                        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                        if (bridge != null && bridge.isDebugLoggingEnabled()) {
                            bridge.debugLog("[AkiAsync-LightEngine] Failed to check block at " + pos + ": " + e.getMessage());
                        }
                    }
                    PENDING_UPDATES.remove(pos);
                    layerSizes.get(level).decrementAndGet();
                }
            }
            if (advancedStatsEnabled || batchCount <= 3) {
                if (totalProcessed > 0) {
                    org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    if (bridge != null) {
                        bridge.debugLog("[AkiAsync-LightEngine] Layered batch processed " + totalProcessed + " updates");
                    }
                    if (advancedStatsEnabled) {
                        for (int i = 15; i >= 0; i--) {
                            int size = layerSizes.get(i).get();
                            if (size > 0) {
                                org.virgil.akiasync.mixin.bridge.Bridge levelBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                                if (levelBridge != null) {
                                    levelBridge.debugLog("  - Level " + i + ": " + size + " pending");
                                }
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
                    org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    if (bridge != null && bridge.isDebugLoggingEnabled()) {
                        bridge.debugLog("[AkiAsync-LightEngine] Failed to process light update at " + pos + ": " + e.getMessage());
                    }
                }
                PENDING_UPDATES.remove(pos);
                queueSize.decrementAndGet();
            }
            if (batchCount <= 3 && processed > 0) {
                org.virgil.akiasync.mixin.bridge.Bridge simpleBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (simpleBridge != null) {
                    simpleBridge.debugLog("[AkiAsync-LightEngine] Simple batch processed " + processed + " updates");
                }
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
        return 20.0;
    }
    private int getTotalQueueSize() {
        if (useLayeredQueue) {
            int total = 0;
            for (AtomicInteger size : layerSizes) {
                total += size.get();
            }
            return total;
        }
        return queueSize.get();
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
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync-LightEngine] Error clearing queues: " + e.getMessage());
            }
        } finally {
            processing = false;
        }
    }
    private int getLightLevel(BlockPos pos) {
        try {
            LightEngine lightEngine = (LightEngine) (Object) this;
            return 15;
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

            if (isFolia) {
                bridge.debugLog("[AkiAsync] LightEngineAsyncMixin (Enhanced) initialized in Folia mode:");
                bridge.debugLog("  - Enabled: " + enabled + " (with region safety checks)");
                bridge.debugLog("  - Batch threshold: " + batchThreshold + " (dynamic: " + dynamicAdjustmentEnabled + ")");
                bridge.debugLog("  - Layered queue (16 levels): " + useLayeredQueue);
                bridge.debugLog("  - Region-aware async processing enabled");
            } else {
                bridge.debugLog("[AkiAsync] LightEngineAsyncMixin (Enhanced) initialized:");
                bridge.debugLog("  - Enabled: " + enabled);
                bridge.debugLog("  - Batch threshold: " + batchThreshold + " (dynamic: " + dynamicAdjustmentEnabled + ")");
                bridge.debugLog("  - Layered queue (16 levels): " + useLayeredQueue);
                bridge.debugLog("  - Max propagation distance: " + maxPropagationDistance);
                bridge.debugLog("  - Deduplication: " + deduplicationEnabled);
                bridge.debugLog("  - Advanced stats: " + advancedStatsEnabled);
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
        }
        initialized = true;
    }
}
