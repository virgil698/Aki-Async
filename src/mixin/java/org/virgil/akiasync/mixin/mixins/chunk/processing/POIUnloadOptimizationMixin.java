package org.virgil.akiasync.mixin.mixins.chunk.processing;

import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(PoiManager.class)
public abstract class POIUnloadOptimizationMixin {

    @Unique
    private static final AtomicLong akiasync$poiUnloadCount = new AtomicLong(0);

    @Unique
    private static final AtomicLong akiasync$poiLoadCount = new AtomicLong(0);

    @Unique
    private static volatile long akiasync$lastLogTime = 0L;

    @Unique
    private static volatile boolean akiasync$initialized = false;

    @Inject(
        method = "checkConsistencyWithBlocks",
        at = @At("HEAD"),
        require = 0
    )
    private void akiasync$onCheckConsistency(CallbackInfo ci) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }

        akiasync$poiLoadCount.incrementAndGet();
        akiasync$logStatistics();
    }

    @Inject(
        method = "tick",
        at = @At("HEAD"),
        require = 0
    )
    private void akiasync$onTick(CallbackInfo ci) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }

        if (!akiasync$initialized) {
            akiasync$initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-POI] POI optimization enabled");
        }
    }

    @Unique
    private static void akiasync$logStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastLogTime > 60000) {
            akiasync$lastLogTime = currentTime;
            long loads = akiasync$poiLoadCount.get();
            long unloads = akiasync$poiUnloadCount.get();
            if (loads > 0 || unloads > 0) {
                BridgeConfigCache.debugLog(String.format(
                    "[AkiAsync-POI] Stats - Loads: %d, Unloads: %d",
                    loads, unloads
                ));
            }
        }
    }
}
