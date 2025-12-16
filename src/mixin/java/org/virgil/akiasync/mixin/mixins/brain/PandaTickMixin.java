package org.virgil.akiasync.mixin.mixins.brain;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.panda.PandaCpuCalculator;
import org.virgil.akiasync.mixin.brain.panda.PandaDiff;
import org.virgil.akiasync.mixin.brain.panda.PandaSnapshot;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import net.minecraft.server.level.ServerLevel;

@Mixin(targets = "net.minecraft.world.entity.animal.Panda", priority = 1100)
public abstract class PandaTickMixin {
    
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
    private PandaSnapshot aki$snapshot = null;
    
    @Unique
    private long aki$nextTick = 0;
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"), require = 0)
    private void aki$asyncPandaAi(ServerLevel level, CallbackInfo ci) {
        if (!initialized) {
            aki$initPandaOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        try {
            net.minecraft.world.entity.animal.Animal panda = 
                (net.minecraft.world.entity.animal.Animal) (Object) this;
            
            if (panda.isInLava() || panda.isOnFire() || 
                panda.getHealth() < panda.getMaxHealth() * 0.5) {
                return;
            }
            
            long currentTick = level.getGameTime();
            
            if (currentTick < aki$nextTick) {
                return;
            }
            
            aki$nextTick = currentTick + cached_tickInterval;
            
            aki$snapshot = PandaSnapshot.capture(panda, level, panda.tickCount);
            
            CompletableFuture<PandaDiff> future = AsyncBrainExecutor.runSync(() ->
                PandaCpuCalculator.runCpuOnly(panda, aki$snapshot), 
                cached_timeout, TimeUnit.MICROSECONDS);
            
            PandaDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(
                future, cached_timeout, TimeUnit.MICROSECONDS, 
                PandaDiff::new);
            
            if (diff != null && diff.hasChanges()) {
                diff.applyTo(panda, level);
                
                if (cached_debugEnabled) {
                    BridgeConfigCache.debugLog(
                        "[AkiAsync-Panda] AI decision: %s", diff.toString());
                }
            }
            
        } catch (Exception e) {
            BridgeConfigCache.errorLog(
                "[AkiAsync-Panda] Error in async AI: %s", e.getMessage());
        }
    }
    
    @Unique
    private static synchronized void aki$initPandaOptimization() {
        if (initialized) {
            return;
        }
        
        Bridge bridge = BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = bridge.isPandaOptimizationEnabled();
            cached_tickInterval = bridge.getPandaTickInterval();
            cached_timeout = bridge.getAsyncAITimeoutMicros();
            cached_debugEnabled = bridge.isDebugLoggingEnabled();
            
            BridgeConfigCache.debugLog(
                "[AkiAsync] PandaTickMixin initialized: enabled=%s, interval=%d, timeout=%d",
                cached_enabled, cached_tickInterval, cached_timeout);
        } else {
            cached_enabled = false;
            cached_tickInterval = 3;
            cached_timeout = 100;
        }
        
        initialized = true;
    }
}
