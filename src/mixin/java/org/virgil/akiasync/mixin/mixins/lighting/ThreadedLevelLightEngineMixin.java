package org.virgil.akiasync.mixin.mixins.lighting;

import ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface;
import ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(value = ThreadedLevelLightEngine.class, priority = 1100)
public abstract class ThreadedLevelLightEngineMixin extends LevelLightEngine implements StarLightLightingProvider {
    
    public ThreadedLevelLightEngineMixin(LightChunkGetter lightChunkGetter, boolean blockLight, boolean skyLight) {
        super(lightChunkGetter, blockLight, skyLight);
    }
    
    // Configuration
    @Unique
    private static volatile boolean enabled = false;
    @Unique
    private static volatile int parallelism = 4;
    @Unique
    private static volatile long updateIntervalNanos = 10_000_000L;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static ExecutorService aki$lightingExecutor;
    @Unique
    private static final AtomicInteger aki$threadCounter = new AtomicInteger(0);
    
    @Unique
    private final Long2ObjectMap<CompletableFuture<Void>> aki$chunkFutures = 
            new Long2ObjectLinkedOpenHashMap<>();
    @Unique
    private final Object aki$futureLock = new Object();
    
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
            
            if (enabled && parallelism > 1) {
                aki$lightingExecutor = Executors.newFixedThreadPool(parallelism, runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    thread.setName("aki-async-lighting-" + aki$threadCounter.getAndIncrement());
                    thread.setPriority(Thread.NORM_PRIORITY - 1);
                    return thread;
                });
                
                if (bridge.isLightingDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-Lighting] ScalableLux-style parallel lighting initialized:");
                    bridge.debugLog("[AkiAsync-Lighting]   - Parallelism: %d threads", parallelism);
                    bridge.debugLog("[AkiAsync-Lighting]   - Update interval: %dms", intervalMs);
                    bridge.debugLog("[AkiAsync-Lighting]   - Engine: Paper/Luminol StarLight");
                }
            } else if (enabled) {
                if (bridge.isLightingDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-Lighting] Lighting throttle enabled (single-threaded):");
                    bridge.debugLog("[AkiAsync-Lighting]   - Update interval: %dms", intervalMs);
                }
            } else {
                if (bridge.isLightingDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-Lighting] ThreadedLevelLightEngineMixin disabled");
                }
            }
        }
        
        initialized = true;
    }
    
    @Inject(
            method = "checkBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    @SuppressWarnings("unused")
    private void parallelCheckBlock(BlockPos pos, CallbackInfo ci) {
        if (!initialized) {
            aki$init();
        }
        
        if (!enabled || parallelism <= 1) {
            return;
        }
        
        try {
            StarLightInterface starlightEngine = this.starlight$getLightEngine();
            if (starlightEngine == null) {
                return;
            }
            
            Object level = starlightEngine.getWorld();
            if (!(level instanceof ServerLevel world)) {
                return;
            }
            
            long currentTime = System.nanoTime();
            long lastSchedule = aki$lastScheduleTime.get();
            if (currentTime - lastSchedule < updateIntervalNanos) {
                return;
            }
            
            if (!aki$lastScheduleTime.compareAndSet(lastSchedule, currentTime)) {
                return;
            }
            
            final BlockPos immutablePos = pos.immutable();
            final ChunkPos chunkPos = new ChunkPos(immutablePos);
            final long chunkKey = chunkPos.toLong();
            
            synchronized (aki$futureLock) {
                CompletableFuture<Void> existingFuture = aki$chunkFutures.get(chunkKey);
                if (existingFuture != null && !existingFuture.isDone()) {
                    return;
                }
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        ChunkAccess chunk = starlightEngine.getAnyChunkNow(chunkPos.x, chunkPos.z);
                        if (chunk == null || !chunk.getPersistedStatus().isOrAfter(ChunkStatus.LIGHT)) {
                            return;
                        }
                        
                        starlightEngine.blockChange(immutablePos);
                        
                    } catch (Exception e) {
                        org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                                "ThreadedLevelLightEngine", "parallelCheckBlock", e);
                    }
                }, aki$lightingExecutor);
                
                aki$chunkFutures.put(chunkKey, future);
                
                future.whenComplete((result, throwable) -> {
                    synchronized (aki$futureLock) {
                        aki$chunkFutures.remove(chunkKey, future);
                    }
                });
            }
            
            ci.cancel();
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "ThreadedLevelLightEngine", "parallelCheckBlock", e);
        }
    }
}
