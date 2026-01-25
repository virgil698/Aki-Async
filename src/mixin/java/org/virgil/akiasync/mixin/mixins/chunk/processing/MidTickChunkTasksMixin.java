package org.virgil.akiasync.mixin.mixins.chunk.processing;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
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

@Mixin(ServerLevel.class)
public abstract class MidTickChunkTasksMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    public abstract ServerChunkCache getChunkSource();

    @Unique
    private static volatile long akiasync$midTickInterval = 1_000_000L;

    @Unique
    private long akiasync$lastMidTickTime = System.nanoTime();

    @Unique
    private static volatile boolean akiasync$initialized = false;

    @Unique
    private static final AtomicLong akiasync$totalMidTickCalls = new AtomicLong(0);

    @Unique
    private static final AtomicLong akiasync$tasksExecuted = new AtomicLong(0);

    @Unique
    private static volatile long akiasync$lastLogTime = 0L;

    @Inject(method = "tickBlock", at = @At("RETURN"), require = 0)
    private void akiasync$onPostTickBlock(CallbackInfo ci) {
        akiasync$executeTasksMidTick();
    }

    @Inject(method = "tickFluid", at = @At("RETURN"), require = 0)
    private void akiasync$onPostTickFluid(CallbackInfo ci) {
        akiasync$executeTasksMidTick();
    }

    @Unique
    private void akiasync$executeTasksMidTick() {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }

        if (Thread.currentThread() != this.server.getRunningThread()) {
            return;
        }

        long currentTime = System.nanoTime();
        if (currentTime - akiasync$lastMidTickTime < akiasync$midTickInterval) {
            return;
        }

        akiasync$initIfNeeded();
        akiasync$lastMidTickTime = currentTime;

        try {
            ServerChunkCache chunkSource = this.getChunkSource();
            if (chunkSource != null) {

                if (chunkSource.pollTask()) {
                    akiasync$tasksExecuted.incrementAndGet();
                }
            }

            akiasync$totalMidTickCalls.incrementAndGet();
            akiasync$logStatistics();

        } catch (Exception e) {

        }
    }

    @Unique
    private static void akiasync$initIfNeeded() {
        if (!akiasync$initialized) {
            akiasync$initialized = true;

            int intervalMs = BridgeConfigCache.getMidTickChunkTasksIntervalMs();
            if (intervalMs > 0) {
                akiasync$midTickInterval = intervalMs * 1_000_000L;
            }

            BridgeConfigCache.debugLog("[AkiAsync-MidTickChunk] C2ME-style mid-tick chunk tasks enabled");
            BridgeConfigCache.debugLog("[AkiAsync-MidTickChunk] Interval: " + intervalMs + "ms");
            BridgeConfigCache.debugLog("[AkiAsync-MidTickChunk] Injection points: tickBlock, tickFluid");
        }
    }

    @Unique
    private static void akiasync$logStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastLogTime > 60000) {
            akiasync$lastLogTime = currentTime;
            BridgeConfigCache.debugLog(String.format(
                "[AkiAsync-MidTickChunk] Stats - Mid-tick calls: %d, Tasks executed: %d",
                akiasync$totalMidTickCalls.get(), akiasync$tasksExecuted.get()
            ));
        }
    }
}
