package org.virgil.akiasync.mixin.mixins.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.enderman.EndermanCpuCalculator;
import org.virgil.akiasync.mixin.brain.enderman.EndermanDiff;
import org.virgil.akiasync.mixin.brain.enderman.EndermanSnapshot;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mixin(value = EnderMan.class, priority = 1100)
public abstract class EndermanTickMixin {
    
    @Shadow
    private int targetChangeTime;
    
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
    private EndermanSnapshot aki$snapshot = null;
    
    @Unique
    private long aki$nextTick = 0;
    
    @Unique
    private EndermanDiff aki$pendingDiff = null;
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void aki$asyncEndermanAi(ServerLevel level, CallbackInfo ci) {
        if (!initialized) {
            aki$initEndermanOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        EnderMan enderman = (EnderMan) (Object) this;
        
        if (enderman.isInLava() || enderman.isOnFire() || 
            enderman.getHealth() < enderman.getMaxHealth() * 0.5) {
            return;
        }
        
        long currentTick = level.getGameTime();
        
        if (currentTick < aki$nextTick) {
            
            if (aki$pendingDiff != null && aki$pendingDiff.hasChanges()) {
                if (aki$pendingDiff.shouldTeleport()) {
                    
                    enderman.teleport();
                }
                aki$pendingDiff = null;
            }
            return;
        }
        
        aki$nextTick = currentTick + cached_tickInterval;
        
        try {
            
            aki$snapshot = EndermanSnapshot.capture(enderman, level, 
                                                   enderman.tickCount, 
                                                   this.targetChangeTime);
            
            CompletableFuture<EndermanDiff> future = AsyncBrainExecutor.runSync(() ->
                EndermanCpuCalculator.runCpuOnly(enderman, aki$snapshot), 
                cached_timeout, TimeUnit.MICROSECONDS);
            
            EndermanDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(
                future, cached_timeout, TimeUnit.MICROSECONDS, 
                EndermanDiff::new);
            
            if (diff != null && diff.hasChanges()) {
                diff.applyTo(enderman, level);
                
                if (diff.shouldTeleport()) {
                    aki$pendingDiff = diff;
                }
                
                if (cached_debugEnabled) {
                    BridgeConfigCache.debugLog(
                        "[AkiAsync-Enderman] AI decision: %s", diff.toString());
                }
            }
            
        } catch (Exception e) {
            BridgeConfigCache.errorLog(
                "[AkiAsync-Enderman] Error in async AI: %s", e.getMessage());
        }
    }
    
    @Unique
    private static synchronized void aki$initEndermanOptimization() {
        if (initialized) {
            return;
        }
        
        Bridge bridge = BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = bridge.isEndermanOptimizationEnabled();
            cached_timeout = bridge.getAsyncAITimeoutMicros();
            cached_tickInterval = bridge.getEndermanTickInterval();
            cached_debugEnabled = bridge.isDebugLoggingEnabled();
            
            BridgeConfigCache.debugLog(
                "[AkiAsync] EndermanTickMixin initialized: enabled=%s, interval=%d, timeout=%dus",
                cached_enabled, cached_tickInterval, cached_timeout);
        
            initialized = true;
        } else {
            cached_enabled = false;
            cached_tickInterval = 3;
            cached_timeout = 100;
        }
    }
}
 
