package org.virgil.akiasync.mixin.mixins.chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;


@Pseudo
@Mixin(targets = "net.minecraft.world.level.chunk.storage.IOWorker", remap = false)
public abstract class BlendingStatusAsyncMixin {
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static final AtomicLong asyncBlendingChecks = new AtomicLong(0);
    
    @Unique
    private static volatile long lastLogTime = 0L;

    @Unique
    private static void akiasync$logStatistics() {
        if (!initialized) {
            initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-Blending] Async blending status optimization enabled");
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > 60000) {
            lastLogTime = currentTime;
            BridgeConfigCache.debugLog(String.format(
                "[AkiAsync-Blending] Stats - Async blending checks: %d",
                asyncBlendingChecks.get()
            ));
        }
    }
    
    
    @Unique
    private static void akiasync$recordBlendingCheck() {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }
        asyncBlendingChecks.incrementAndGet();
        akiasync$logStatistics();
    }
}
