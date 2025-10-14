package org.virgil.akiasync.mixin.mixins.brain;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.WitchCpuCalculator;
import org.virgil.akiasync.mixin.brain.WitchDiff;
import org.virgil.akiasync.mixin.brain.WitchSnapshot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Witch;

/**
 * Witch async AI optimization (1.21.8 specific)
 * 
 * Workflow:
 * ① Main thread snapshot (< 0.05 ms)
 * ② Async computation (0.5-1 ms): Potion recipe matching + effect overlap detection
 * ③ Main thread writeback (< 0.1 ms): setMemory
 * 
 * Total time: < 0.15 ms (sync) or once per 3 ticks (async throttled)
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = Witch.class, priority = 997)
public abstract class WitchBrainMixin {
    
    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile long cached_timeoutMicros;
    @Unique private static volatile int cached_tickInterval;
    @Unique private static volatile int cached_scanDistance;
    @Unique private static volatile boolean initialized = false;
    
    @Unique private WitchSnapshot aki$snapshot;
    @Unique private long aki$nextAsyncTick = 0;
    
    // Statistics
    @Unique private static int executionCount = 0;
    @Unique private static int successCount = 0;
    @Unique private static int timeoutCount = 0;
    
    /**
     * Take snapshot at customServerAiStep start
     */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void aki$takeSnapshot(CallbackInfo ci) {
        if (!initialized) { aki$initWitchAsync(); }
        if (!cached_enabled) return;
        
        Witch witch = (Witch) (Object) this;
        ServerLevel level = (ServerLevel) witch.level();
        if (level == null) return;
        
        // Throttle: once per 3 ticks (same as piglins)
        if (level.getGameTime() < this.aki$nextAsyncTick) {
            return;
        }
        this.aki$nextAsyncTick = level.getGameTime() + cached_tickInterval;
        
        // Take snapshot (< 0.05 ms)
        try {
            this.aki$snapshot = WitchSnapshot.capture(witch, level);
        } catch (Exception e) {
            this.aki$snapshot = null;
        }
    }
    
    /**
     * Async computation and writeback after customServerAiStep ends
     */
    @Inject(method = "customServerAiStep", at = @At("RETURN"))
    private void aki$offloadBrain(CallbackInfo ci) {
        if (!cached_enabled) return;
        if (this.aki$snapshot == null) return;
        
        executionCount++;
        Witch witch = (Witch) (Object) this;
        ServerLevel level = (ServerLevel) witch.level();
        if (level == null) return;
        
        final WitchSnapshot snapshot = this.aki$snapshot;
        
        try {
            // Async computation (0.5-1 ms)
            CompletableFuture<WitchDiff> future = AsyncBrainExecutor.runSync(() -> {
                return WitchCpuCalculator.runCpuOnly(witch, level, snapshot);
            }, cached_timeoutMicros, TimeUnit.MICROSECONDS);
            
            // Main thread wait (≤100μs)
            WitchDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(
                future,
                cached_timeoutMicros,
                TimeUnit.MICROSECONDS,
                () -> new WitchDiff()
            );
            
            if (diff != null && diff.hasChanges()) {
                diff.applyTo(witch, level, cached_scanDistance);
                successCount++;
                
                // Output statistics every 1000 executions
                if (executionCount % 1000 == 0) {
                    double successRate = (successCount * 100.0) / executionCount;
                    double timeoutRate = (timeoutCount * 100.0) / executionCount;
                    System.out.println(String.format(
                        "[AkiAsync-WitchAI] Stats: %d execs | %.1f%% success | %.1f%% timeout",
                        executionCount, successRate, timeoutRate
                    ));
                }
            } else {
                timeoutCount++;
            }
            
        } catch (Exception e) {
            if (executionCount <= 3) {
                System.err.println("[AkiAsync-WitchAI] Error: " + e.getClass().getSimpleName());
            }
        } finally {
            this.aki$snapshot = null;
        }
    }
    
    @Unique
    private static synchronized void aki$initWitchAsync() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = bridge.isWitchOptimizationEnabled();
            cached_timeoutMicros = bridge.getAsyncAITimeoutMicros();
            cached_tickInterval = 3;
            cached_scanDistance = bridge.getWitchScanDistance();
            
            AsyncBrainExecutor.setExecutor(bridge.getGeneralExecutor());
        } else {
            cached_enabled = false;
            cached_timeoutMicros = 100;
            cached_tickInterval = 3;
            cached_scanDistance = 16;
        }
        
        initialized = true;
        System.out.println(String.format(
            "[AkiAsync] WitchBrainMixin initialized: enabled=%s, timeout=%dμs, interval=%d tick",
            cached_enabled, cached_timeoutMicros, cached_tickInterval
        ));
    }
}

