package org.virgil.akiasync.mixin.mixins.chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.concurrent.atomic.AtomicLong;


@Pseudo
@Mixin(targets = "ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager", remap = false)
public abstract class ChunkUnloadQueueOptimizationMixin {

    @Unique
    private static final AtomicLong akiasync$saveCount = new AtomicLong(0);
    
    @Unique
    private static volatile long akiasync$lastLogTime = 0L;
    
    @Unique
    private static volatile boolean akiasync$initialized = false;

    
    @Inject(
        method = "saveAllChunks",
        at = @At("RETURN"),
        require = 0
    )
    private void akiasync$onSaveAllChunks(boolean flush, boolean shutdown, boolean logProgress, CallbackInfo ci) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }
        
        akiasync$saveCount.incrementAndGet();
        
        if (shutdown) {
            BridgeConfigCache.debugLog(String.format(
                "[AkiAsync-ChunkUnload] Shutdown stats - Total saves: %d",
                akiasync$saveCount.get()
            ));
        } else {
            akiasync$logStatistics();
        }
    }

    @Unique
    private static void akiasync$logStatistics() {
        if (!akiasync$initialized) {
            akiasync$initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-ChunkUnload] ChunkHolderManager statistics enabled");
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastLogTime > 60000) {
            akiasync$lastLogTime = currentTime;
            BridgeConfigCache.debugLog(String.format(
                "[AkiAsync-ChunkUnload] Stats - Save count: %d",
                akiasync$saveCount.get()
            ));
        }
    }
}
