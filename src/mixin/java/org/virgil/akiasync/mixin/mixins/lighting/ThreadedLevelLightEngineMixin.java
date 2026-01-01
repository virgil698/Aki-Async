package org.virgil.akiasync.mixin.mixins.lighting;

import ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.ChunkLightBatch;

import java.util.concurrent.atomic.AtomicLong;


@Mixin(value = ThreadedLevelLightEngine.class, priority = 1100)
public abstract class ThreadedLevelLightEngineMixin extends LevelLightEngine implements StarLightLightingProvider {
    
    public ThreadedLevelLightEngineMixin(LightChunkGetter lightChunkGetter, boolean blockLight, boolean skyLight) {
        super(lightChunkGetter, blockLight, skyLight);
    }
    
    
    @Unique
    private static volatile boolean enabled = false;
    @Unique
    private static volatile int parallelism = 4;
    @Unique
    private static volatile long updateIntervalNanos = 10_000_000L;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean useSmartBatching = true;
    
    
    @Unique
    private static final AtomicLong aki$totalCheckBlocks = new AtomicLong(0);
    @Unique
    private static final AtomicLong aki$throttledCheckBlocks = new AtomicLong(0);
    @Unique
    private static final AtomicLong aki$batchedCheckBlocks = new AtomicLong(0);
    @Unique
    private static volatile long aki$lastStatsLogTime = 0L;
    
    
    @Unique
    private final Long2ObjectMap<ChunkLightBatch> aki$chunkBatches = 
            new Long2ObjectLinkedOpenHashMap<>();
    @Unique
    private final Object aki$batchLock = new Object();
    
    @Unique
    private final AtomicLong aki$lastScheduleTime = new AtomicLong(0);
    
    @Unique
    private static synchronized void aki$init() {
        if (initialized) return;
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isAsyncLightingEnabled();
            parallelism = bridge.getLightingParallelism();
            
            int intervalMs = bridge.getLightUpdateIntervalMs();
            updateIntervalNanos = intervalMs * 1_000_000L;
            
            
            useSmartBatching = parallelism > 1;
            
            if (enabled) {
                if (bridge.isLightingDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-Lighting] Paper/Starlight light engine optimization enabled:");
                    bridge.debugLog("[AkiAsync-Lighting]   - Update interval: %dms", intervalMs);
                    bridge.debugLog("[AkiAsync-Lighting]   - Smart batching: %s", useSmartBatching ? "enabled" : "disabled");
                    bridge.debugLog("[AkiAsync-Lighting]   - Note: Works alongside Paper's native async lighting");
                
                    initialized = true;
                }
            } else {
                if (bridge.isLightingDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-Lighting] ThreadedLevelLightEngineMixin disabled");
                }
            }
        }
    }
    
    
    @Inject(
            method = "checkBlock",
            at = @At("HEAD"),
            cancellable = false
    )
    @SuppressWarnings("unused")
    private void aki$optimizeCheckBlock(BlockPos pos, CallbackInfo ci) {
        if (!initialized) {
            aki$init();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            aki$totalCheckBlocks.incrementAndGet();
            
            long currentTime = System.nanoTime();
            
            if (useSmartBatching) {
                
                final ChunkPos chunkPos = new ChunkPos(pos);
                final long chunkKey = chunkPos.toLong();
                
                synchronized (aki$batchLock) {
                    ChunkLightBatch batch = aki$chunkBatches.get(chunkKey);
                    
                    if (batch == null) {
                        batch = new ChunkLightBatch(currentTime);
                        aki$chunkBatches.put(chunkKey, batch);
                    }
                    
                    
                    batch.addPosition(pos.immutable());
                    
                    
                    if (batch.size() > 64) {
                        batch.markUrgent();
                    }
                }
                
                
                if (currentTime - aki$lastScheduleTime.get() > updateIntervalNanos * 10) {
                    aki$cleanupOldBatches(currentTime);
                    aki$lastScheduleTime.set(currentTime);
                }
            } else {
                
                long lastSchedule = aki$lastScheduleTime.get();
                if (currentTime - lastSchedule < updateIntervalNanos) {
                    aki$throttledCheckBlocks.incrementAndGet();
                    
                    return;
                }
                aki$lastScheduleTime.compareAndSet(lastSchedule, currentTime);
            }
            
            
            aki$logStatistics();
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "ThreadedLevelLightEngine", "optimizeCheckBlock", e);
        }
    }
    
    @Unique
    private void aki$cleanupOldBatches(long currentTime) {
        synchronized (aki$batchLock) {
            long expiryThreshold = currentTime - updateIntervalNanos * 100; 
            
            aki$chunkBatches.entrySet().removeIf(entry -> {
                ChunkLightBatch batch = entry.getValue();
                if (batch.getCreationTime() < expiryThreshold) {
                    aki$batchedCheckBlocks.addAndGet(batch.size());
                    return true;
                }
                return false;
            });
        }
    }
    
    @Unique
    private static void aki$logStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - aki$lastStatsLogTime > 60000) { 
            aki$lastStatsLogTime = currentTime;
            
            long total = aki$totalCheckBlocks.get();
            long throttled = aki$throttledCheckBlocks.get();
            long batched = aki$batchedCheckBlocks.get();
            
            if (total > 0) {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null && bridge.isLightingDebugEnabled()) {
                    bridge.debugLog(
                        "[AkiAsync-Lighting] Stats - Total: %d, Throttled: %d (%.1f%%), Batched: %d",
                        total, throttled, (throttled * 100.0 / total), batched
                    );
                }
            }
        }
    }
}
