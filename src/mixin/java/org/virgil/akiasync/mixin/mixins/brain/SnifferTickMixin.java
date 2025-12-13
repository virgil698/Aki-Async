package org.virgil.akiasync.mixin.mixins.brain;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.sniffer.SnifferCpuCalculator;
import org.virgil.akiasync.mixin.brain.sniffer.SnifferDiff;
import org.virgil.akiasync.mixin.brain.sniffer.SnifferSnapshot;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import net.minecraft.server.level.ServerLevel;


@Mixin(targets = "net.minecraft.world.entity.animal.sniffer.Sniffer", priority = 1100)
public abstract class SnifferTickMixin {
    
    @Unique
    private static volatile boolean cached_enabled = false;
    
    @Unique
    private static volatile long cached_timeout = 100;
    
    @Unique
    private static volatile int cached_tickInterval = 3;
    
    @Unique
    private static volatile boolean cached_debugEnabled = false;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private SnifferSnapshot aki$snapshot = null;
    
    @Unique
    private long aki$nextTick = 0;
    
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"), require = 0)
    private void aki$asyncSnifferAi(ServerLevel level, CallbackInfo ci) {
        if (!initialized) {
            aki$initSnifferOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        try {
            net.minecraft.world.entity.animal.Animal sniffer = 
                (net.minecraft.world.entity.animal.Animal) (Object) this;
            
            
            if (sniffer.isInLava() || sniffer.isOnFire() || 
                sniffer.getHealth() < sniffer.getMaxHealth() * 0.5) {
                return;
            }
            
            long currentTick = level.getGameTime();
            
            
            if (currentTick < aki$nextTick) {
                return;
            }
            
            aki$nextTick = currentTick + cached_tickInterval;
            
            
            aki$snapshot = SnifferSnapshot.capture(sniffer, level, sniffer.tickCount);
            
            
            CompletableFuture<SnifferDiff> future = AsyncBrainExecutor.runSync(() ->
                SnifferCpuCalculator.runCpuOnly(sniffer, aki$snapshot), 
                cached_timeout, TimeUnit.MICROSECONDS);
            
            
            SnifferDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(
                future, cached_timeout, TimeUnit.MICROSECONDS, 
                SnifferDiff::new);
            
            
            if (diff != null && diff.hasChanges()) {
                diff.applyTo(sniffer, level);
                
                if (cached_debugEnabled) {
                    BridgeConfigCache.debugLog(
                        "[AkiAsync-Sniffer] AI decision: %s", diff.toString());
                }
            }
            
        } catch (Exception e) {
            BridgeConfigCache.errorLog(
                "[AkiAsync-Sniffer] Error in async AI: %s", e.getMessage());
        }
    }
    
    @Unique
    private static synchronized void aki$initSnifferOptimization() {
        if (initialized) {
            return;
        }
        
        Bridge bridge = BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = bridge.isSnifferOptimizationEnabled();
            cached_tickInterval = bridge.getSnifferTickInterval();
            cached_timeout = bridge.getAsyncAITimeoutMicros();
            cached_debugEnabled = bridge.isDebugLoggingEnabled();
            
            BridgeConfigCache.debugLog(
                "[AkiAsync] SnifferTickMixin initialized: enabled=%s, interval=%d, timeout=%d",
                cached_enabled, cached_tickInterval, cached_timeout);
        } else {
            cached_enabled = false;
            cached_tickInterval = 3;
            cached_timeout = 100;
        }
        
        initialized = true;
    }
}
