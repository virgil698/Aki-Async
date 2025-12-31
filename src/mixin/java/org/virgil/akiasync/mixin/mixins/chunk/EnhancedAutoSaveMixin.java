package org.virgil.akiasync.mixin.mixins.chunk;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@Mixin(MinecraftServer.class)
public abstract class EnhancedAutoSaveMixin {

    @Shadow
    public abstract Iterable<ServerLevel> getAllLevels();

    @Unique
    private static final AtomicLong akiasync$totalSaveTime = new AtomicLong(0);
    
    @Unique
    private static final AtomicInteger akiasync$saveCount = new AtomicInteger(0);
    
    @Unique
    private static volatile long akiasync$lastLogTime = 0L;
    
    @Unique
    private static volatile long akiasync$lastSaveStartTime = 0L;
    
    @Unique
    private static volatile boolean akiasync$initialized = false;

    
    @Inject(
        method = "autoSave",
        at = @At("HEAD"),
        require = 0
    )
    private void akiasync$onAutoSaveStart(CallbackInfo ci) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }
        
        akiasync$initIfNeeded();
        akiasync$lastSaveStartTime = System.nanoTime();
    }

    
    @Inject(
        method = "autoSave",
        at = @At("RETURN"),
        require = 0
    )
    private void akiasync$onAutoSaveEnd(CallbackInfo ci) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }
        
        long saveTime = System.nanoTime() - akiasync$lastSaveStartTime;
        akiasync$totalSaveTime.addAndGet(saveTime);
        akiasync$saveCount.incrementAndGet();
        
        akiasync$logStatistics();
    }

    @Unique
    private static void akiasync$initIfNeeded() {
        if (!akiasync$initialized) {
            akiasync$initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-AutoSave] Enhanced auto-save tracking enabled");
        }
    }

    @Unique
    private static void akiasync$logStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastLogTime > 60000) {
            akiasync$lastLogTime = currentTime;
            int saves = akiasync$saveCount.get();
            long totalTime = akiasync$totalSaveTime.get();
            if (saves > 0) {
                double avgTimeMs = (double) totalTime / saves / 1_000_000.0;
                BridgeConfigCache.debugLog(String.format(
                    "[AkiAsync-AutoSave] Stats - Saves: %d, Avg time: %.2fms",
                    saves, avgTimeMs
                ));
            }
        }
    }
}
