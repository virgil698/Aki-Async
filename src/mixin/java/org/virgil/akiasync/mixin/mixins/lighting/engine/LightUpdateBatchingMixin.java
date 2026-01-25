package org.virgil.akiasync.mixin.mixins.lighting.engine;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.util.ChunkLightTracker;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.LightLayer;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(value = ChunkHolder.class, priority = 1100)
public abstract class LightUpdateBatchingMixin {

    @Shadow @Final private BitSet blockChangedLightSectionFilter;
    @Shadow @Final private BitSet skyChangedLightSectionFilter;

    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile boolean enabled = false;

    @Unique
    private static volatile long throttleIntervalNanos = 10_000_000L;

    @Unique
    private static volatile int burstThreshold = 50;

    @Unique
    private static volatile long burstWindowNanos = 100_000_000L;

    @Unique
    private static final ConcurrentHashMap<Long, ChunkLightTracker> akiasync$chunkTrackers =
        new ConcurrentHashMap<>();

    @Unique
    private static final AtomicLong akiasync$totalUpdates = new AtomicLong(0);

    @Unique
    private static final AtomicLong akiasync$throttledUpdates = new AtomicLong(0);

    @Unique
    private static final AtomicLong akiasync$batchedUpdates = new AtomicLong(0);

    @Unique
    private static volatile long akiasync$lastLogTime = 0L;

    @Unique
    private static volatile long akiasync$lastCleanupTime = 0L;

    @Unique
    private volatile long akiasync$chunkKey = Long.MIN_VALUE;

    @Unique
    private final AtomicLong akiasync$lastBlockLightUpdate = new AtomicLong(0);

    @Unique
    private final AtomicLong akiasync$lastSkyLightUpdate = new AtomicLong(0);

    @Inject(
        method = "sectionLightChanged",
        at = @At("HEAD"),
        cancellable = true
    )
    private void akiasync$smartLightThrottle(LightLayer lightLayer, int y, CallbackInfoReturnable<Boolean> cir) {
        if (!initialized) {
            akiasync$init();
        }

        if (!enabled) {
            return;
        }

        try {
            long currentTime = System.nanoTime();

            ChunkLightTracker tracker = akiasync$getOrCreateTracker();
            if (tracker == null) {
                return;
            }

            tracker.recordUpdate(currentTime);
            akiasync$totalUpdates.incrementAndGet();

            if (tracker.isInBurstMode(currentTime, burstWindowNanos, burstThreshold)) {

                AtomicLong lastUpdate = (lightLayer == LightLayer.SKY)
                    ? akiasync$lastSkyLightUpdate
                    : akiasync$lastBlockLightUpdate;

                long lastUpdateTime = lastUpdate.get();

                long effectiveInterval = throttleIntervalNanos * 2;

                if (currentTime - lastUpdateTime < effectiveInterval) {
                    BitSet filter = (lightLayer == LightLayer.SKY)
                        ? skyChangedLightSectionFilter
                        : blockChangedLightSectionFilter;

                    if (filter != null && !filter.isEmpty()) {
                        akiasync$throttledUpdates.incrementAndGet();
                        akiasync$batchedUpdates.incrementAndGet();
                        return;
                    }
                }

                lastUpdate.set(currentTime);
            } else {

                AtomicLong lastUpdate = (lightLayer == LightLayer.SKY)
                    ? akiasync$lastSkyLightUpdate
                    : akiasync$lastBlockLightUpdate;

                long lastUpdateTime = lastUpdate.get();

                if (currentTime - lastUpdateTime < throttleIntervalNanos) {
                    BitSet filter = (lightLayer == LightLayer.SKY)
                        ? skyChangedLightSectionFilter
                        : blockChangedLightSectionFilter;

                    if (filter != null && !filter.isEmpty()) {
                        akiasync$throttledUpdates.incrementAndGet();
                        return;
                    }
                }

                lastUpdate.set(currentTime);
            }

            akiasync$periodicMaintenance(currentTime);

        } catch (Exception e) {

        }
    }

    @Unique
    private ChunkLightTracker akiasync$getOrCreateTracker() {
        try {

            if (akiasync$chunkKey == Long.MIN_VALUE) {

                akiasync$chunkKey = System.identityHashCode(this);
            }

            return akiasync$chunkTrackers.computeIfAbsent(akiasync$chunkKey,
                k -> new ChunkLightTracker());
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private static void akiasync$init() {
        if (initialized) {
            return;
        }

        try {
            enabled = BridgeConfigCache.isAsyncLightingEnabled();

            if (enabled) {
                int intervalMs = BridgeConfigCache.getLightUpdateIntervalMs();
                throttleIntervalNanos = intervalMs * 1_000_000L;

                burstThreshold = Math.max(20, 100 / Math.max(1, intervalMs));
                burstWindowNanos = Math.max(50_000_000L, intervalMs * 10_000_000L);

                BridgeConfigCache.debugLog(
                    "[AkiAsync-LightBatch] Smart light batching enabled:");
                BridgeConfigCache.debugLog(
                    "[AkiAsync-LightBatch]   - Throttle interval: %dms", intervalMs);
                BridgeConfigCache.debugLog(
                    "[AkiAsync-LightBatch]   - Burst threshold: %d updates", burstThreshold);
                BridgeConfigCache.debugLog(
                    "[AkiAsync-LightBatch]   - Burst window: %dms", burstWindowNanos / 1_000_000);
            }
        } catch (Exception e) {

        }
    }

    @Unique
    private static void akiasync$periodicMaintenance(long currentTimeNanos) {
        long currentTimeMs = System.currentTimeMillis();

        if (currentTimeMs - akiasync$lastCleanupTime > 30000) {
            akiasync$lastCleanupTime = currentTimeMs;

            long expiryThreshold = currentTimeNanos - 60_000_000_000L;
            akiasync$chunkTrackers.entrySet().removeIf(entry ->
                entry.getValue().getLastUpdateTime() < expiryThreshold);
        }

        if (currentTimeMs - akiasync$lastLogTime > 60000) {
            akiasync$lastLogTime = currentTimeMs;

            long total = akiasync$totalUpdates.get();
            long throttled = akiasync$throttledUpdates.get();
            long batched = akiasync$batchedUpdates.get();

            if (total > 0) {
                BridgeConfigCache.debugLog(
                    "[AkiAsync-LightBatch] Stats - Total: %d, Throttled: %d (%.1f%%), Batched: %d, Active chunks: %d",
                    total, throttled, (throttled * 100.0 / total), batched, akiasync$chunkTrackers.size()
                );
            }
        }
    }

    @Unique
    private static void akiasync$resetStats() {
        akiasync$totalUpdates.set(0);
        akiasync$throttledUpdates.set(0);
        akiasync$batchedUpdates.set(0);
        akiasync$chunkTrackers.clear();
        initialized = false;
    }
}
