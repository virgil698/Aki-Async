package org.virgil.akiasync.mixin.mixins.lighting;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.SkyLightEngine;
@SuppressWarnings("unused")
@Mixin(value = SkyLightEngine.class, priority = 1100)
public abstract class SkylightCacheMixin {
    private static volatile boolean enabled;
    private static volatile int cacheDurationMs = 100;
    private static volatile boolean initialized = false;
    private static final Map<BlockPos, CachedSkylightValue> SKYLIGHT_CACHE = new ConcurrentHashMap<>();
    private static long lastCleanup = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL = 5000;
    @Inject(method = "computeLevelFromNeighbor", at = @At("HEAD"), cancellable = true)
    private void cacheSkylight(long blockPos, long neighborPos, int currentLevel, CallbackInfoReturnable<Integer> cir) {
        if (!initialized) { akiasync$initSkylightCache(); }
        if (!enabled) return;
        BlockPos pos = BlockPos.of(blockPos);
        long currentTime = System.currentTimeMillis();
        CachedSkylightValue cached = SKYLIGHT_CACHE.get(pos);
        if (cached != null && currentTime - cached.timestamp < cacheDurationMs) {
            cir.setReturnValue(cached.value);
            return;
        }
        if (currentTime - lastCleanup > CLEANUP_INTERVAL) {
            cleanupCache(currentTime);
            lastCleanup = currentTime;
        }
    }
    @Inject(method = "computeLevelFromNeighbor", at = @At("RETURN"))
    private void storeCachedValue(long blockPos, long neighborPos, int currentLevel, CallbackInfoReturnable<Integer> cir) {
        if (!initialized || !enabled) return;
        BlockPos pos = BlockPos.of(blockPos);
        int computedValue = cir.getReturnValue();
        SKYLIGHT_CACHE.put(pos.immutable(), new CachedSkylightValue(computedValue, System.currentTimeMillis()));
        if (SKYLIGHT_CACHE.size() > 10000) {
            cleanupCache(System.currentTimeMillis());
        }
    }
    private static void cleanupCache(long currentTime) {
        SKYLIGHT_CACHE.entrySet().removeIf(entry ->
            currentTime - entry.getValue().timestamp > cacheDurationMs * 10
        );
    }
    private static synchronized void akiasync$initSkylightCache() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isSkylightCacheEnabled();
            cacheDurationMs = bridge.getSkylightCacheDurationMs();
        } else {
            enabled = false;
            cacheDurationMs = 100;
        }
        initialized = true;
        if (bridge != null) {
            bridge.debugLog("[AkiAsync] SkylightCacheMixin initialized: enabled=" + enabled + ", duration=" + cacheDurationMs + "ms");
        }
    }
    private static class CachedSkylightValue {
        final int value;
        final long timestamp;
        CachedSkylightValue(int value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
