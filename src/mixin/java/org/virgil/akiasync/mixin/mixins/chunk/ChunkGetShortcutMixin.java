package org.virgil.akiasync.mixin.mixins.chunk;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;


@Mixin(ServerChunkCache.class)
public abstract class ChunkGetShortcutMixin {

    @Shadow
    @Final
    Thread mainThread;

    @Shadow
    protected abstract ChunkHolder getVisibleChunkIfPresent(long chunkPos);

    @Unique
    private static final AtomicLong akiasync$shortcutHits = new AtomicLong(0);
    
    @Unique
    private static final AtomicLong akiasync$shortcutMisses = new AtomicLong(0);
    
    @Unique
    private static volatile long akiasync$lastLogTime = 0L;
    
    @Unique
    private static volatile boolean akiasync$initialized = false;

    
    @Inject(
        method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void akiasync$shortcutGetChunk(int x, int z, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<ChunkAccess> cir) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }
        
        
        if (Thread.currentThread() == this.mainThread) {
            return;
        }
        
        akiasync$initIfNeeded();
        
        try {
            long chunkPos = net.minecraft.world.level.ChunkPos.asLong(x, z);
            ChunkHolder holder = this.getVisibleChunkIfPresent(chunkPos);
            
            if (holder != null) {
                
                
                CompletableFuture<ChunkResult<ChunkAccess>> future = null;
                
                
                if (leastStatus.isOrAfter(ChunkStatus.FULL)) {
                    
                    var fullFuture = holder.getFullChunkFuture();
                    if (fullFuture != null && fullFuture.isDone() && !fullFuture.isCompletedExceptionally()) {
                        var levelChunk = fullFuture.getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK);
                        if (levelChunk != null && levelChunk.isSuccess()) {
                            akiasync$shortcutHits.incrementAndGet();
                            akiasync$logStatistics();
                            cir.setReturnValue(levelChunk.orElse(null));
                            return;
                        }
                    }
                }
                
                
                var chunkNow = holder.getChunkToSend();
                if (chunkNow != null && chunkNow.getPersistedStatus().isOrAfter(leastStatus)) {
                    akiasync$shortcutHits.incrementAndGet();
                    akiasync$logStatistics();
                    cir.setReturnValue(chunkNow);
                    return;
                }
            }
            
            akiasync$shortcutMisses.incrementAndGet();
            akiasync$logStatistics();
            
        } catch (Exception e) {
            
            akiasync$shortcutMisses.incrementAndGet();
        }
    }

    @Unique
    private static void akiasync$initIfNeeded() {
        if (!akiasync$initialized) {
            akiasync$initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-ChunkShortcut] C2ME chunk get shortcut optimization enabled");
        }
    }

    @Unique
    private static void akiasync$logStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastLogTime > 60000) {
            akiasync$lastLogTime = currentTime;
            long hits = akiasync$shortcutHits.get();
            long misses = akiasync$shortcutMisses.get();
            long total = hits + misses;
            double hitRate = total > 0 ? (hits * 100.0 / total) : 0;
            
            if (total > 0) {
                BridgeConfigCache.debugLog(String.format(
                    "[AkiAsync-ChunkShortcut] Stats - Hits: %d, Misses: %d, Hit rate: %.1f%%",
                    hits, misses, hitRate
                ));
            }
        }
    }
}
