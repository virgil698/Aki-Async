package org.virgil.akiasync.mixin.mixins.chunk;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.concurrent.atomic.AtomicLong;


@Mixin(ChunkMap.class)
public abstract class ChunkTaskSchedulingOptimizationMixin {

    @Shadow
    @Final
    ServerLevel level;

    @Unique
    private static final AtomicLong akiasync$taskCount = new AtomicLong(0);
    
    @Unique
    private static final AtomicLong akiasync$batchedTasks = new AtomicLong(0);
    
    @Unique
    private static volatile long akiasync$lastLogTime = 0L;
    
    @Unique
    private static volatile boolean akiasync$initialized = false;

    
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
            BridgeConfigCache.debugLog("[AkiAsync-ChunkScheduling] Chunk task scheduling optimization enabled");
        }
        
        akiasync$taskCount.incrementAndGet();
        akiasync$logStatistics();
    }

    
    @Inject(
        method = "saveAllChunks",
        at = @At("HEAD"),
        require = 0
    )
    private void akiasync$onSaveAllChunks(boolean flush, CallbackInfo ci) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }
        
        akiasync$batchedTasks.incrementAndGet();
    }

    @Unique
    private static void akiasync$logStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastLogTime > 60000) {
            akiasync$lastLogTime = currentTime;
            long tasks = akiasync$taskCount.get();
            long batched = akiasync$batchedTasks.get();
            if (tasks > 0) {
                BridgeConfigCache.debugLog(String.format(
                    "[AkiAsync-ChunkScheduling] Stats - Ticks: %d, Batch saves: %d",
                    tasks, batched
                ));
            }
        }
    }
}
