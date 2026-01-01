package org.virgil.akiasync.mixin.mixins.brain;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.armadillo.ArmadilloCpuCalculator;
import org.virgil.akiasync.mixin.brain.armadillo.ArmadilloDiff;
import org.virgil.akiasync.mixin.brain.armadillo.ArmadilloSnapshot;
import org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import net.minecraft.server.level.ServerLevel;

@Mixin(targets = "net.minecraft.world.entity.animal.armadillo.Armadillo", priority = 1100)
public abstract class ArmadilloTickMixin {
    
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
    private ArmadilloSnapshot aki$snapshot = null;
    
    @Unique
    private long aki$nextTick = 0;
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"), require = 0)
    private void aki$asyncArmadilloAi(ServerLevel level, CallbackInfo ci) {
        if (!initialized) {
            aki$initArmadilloOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        try {
            net.minecraft.world.entity.animal.Animal armadillo = 
                (net.minecraft.world.entity.animal.Animal) (Object) this;
            
            if (armadillo.isInLava() || armadillo.isOnFire() || 
                armadillo.getHealth() < armadillo.getMaxHealth() * 0.5) {
                return;
            }
            
            long currentTick = level.getGameTime();
            
            if (currentTick < aki$nextTick) {
                return;
            }
            
            aki$nextTick = currentTick + cached_tickInterval;
            
            aki$snapshot = ArmadilloSnapshot.capture(armadillo, level, armadillo.tickCount);
            
            CompletableFuture<ArmadilloDiff> future = AsyncBrainExecutor.runSync(() ->
                ArmadilloCpuCalculator.runCpuOnly(armadillo, aki$snapshot), 
                cached_timeout, TimeUnit.MICROSECONDS);
            
            ArmadilloDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(
                future, cached_timeout, TimeUnit.MICROSECONDS, 
                ArmadilloDiff::new);
            
            if (diff != null && diff.hasChanges()) {
                diff.applyTo(armadillo, level);
                
                if (cached_debugEnabled) {
                    BridgeConfigCache.debugLog(
                        "[AkiAsync-Armadillo] AI decision: %s", diff.toString());
                }
            }
            
        } catch (Exception e) {
            BridgeConfigCache.errorLog(
                "[AkiAsync-Armadillo] Error in async AI: %s", e.getMessage());
        }
    }
    
    @Unique
    private static synchronized void aki$initArmadilloOptimization() {
        if (initialized) {
            return;
        }
        
        Bridge bridge = BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = bridge.isArmadilloOptimizationEnabled();
            cached_tickInterval = bridge.getArmadilloTickInterval();
            cached_timeout = bridge.getAsyncAITimeoutMicros();
            cached_debugEnabled = bridge.isDebugLoggingEnabled();
            
            BridgeConfigCache.debugLog(
                "[AkiAsync] ArmadilloTickMixin initialized: enabled=%s, interval=%d, timeout=%d",
                cached_enabled, cached_tickInterval, cached_timeout);
        
            initialized = true;
        } else {
            cached_enabled = false;
            cached_tickInterval = 3;
            cached_timeout = 100;
        }
    }
}
