package org.virgil.akiasync.mixin.mixins.chunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;


@Mixin(MinecraftServer.class)
public abstract class MidTickChunkTasksMixin {

    @Shadow
    @Final
    private Thread serverThread;
    
    @Shadow
    public abstract Iterable<ServerLevel> getAllLevels();

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

    
    @Inject(
        method = "tickChildren(Ljava/util/function/BooleanSupplier;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V",
            shift = At.Shift.AFTER
        ),
        require = 0
    )
    private void akiasync$executeMidTickTasks(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }
        
        if (Thread.currentThread() != this.serverThread) {
            return;
        }
        
        long currentTime = System.nanoTime();
        if (currentTime - akiasync$lastMidTickTime < akiasync$midTickInterval) {
            return;
        }
        
        akiasync$initIfNeeded();
        akiasync$lastMidTickTime = currentTime;
        
        try {
            for (ServerLevel world : this.getAllLevels()) {
                akiasync$executeWorldChunkTasks(world);
            }
            
            akiasync$totalMidTickCalls.incrementAndGet();
            akiasync$logStatistics();
            
        } catch (Exception e) {
            
        }
    }

    @Unique
    private void akiasync$executeWorldChunkTasks(ServerLevel world) {
        try {
            var chunkSource = world.getChunkSource();
            if (chunkSource == null) {
                return;
            }
            
            try {
                if (chunkSource.pollTask()) {
                    akiasync$tasksExecuted.incrementAndGet();
                }
            } catch (Exception e) {
                
            }
            
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
            
            BridgeConfigCache.debugLog("[AkiAsync-MidTickChunk] C2ME mid-tick chunk tasks optimization enabled");
            BridgeConfigCache.debugLog("[AkiAsync-MidTickChunk] Interval: " + intervalMs + "ms");
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
