package org.virgil.akiasync.mixin.mixins.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.hoglin.HoglinCpuCalculator;
import org.virgil.akiasync.mixin.brain.hoglin.HoglinDiff;
import org.virgil.akiasync.mixin.brain.hoglin.HoglinSnapshot;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mixin(value = Hoglin.class, priority = 1100)
public class HoglinTickMixin {
    
    @Unique
    private static volatile boolean cached_enabled = false;
    
    @Unique
    private static volatile boolean cached_debugEnabled = false;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private CompletableFuture<HoglinDiff> pendingDiff = null;
    
    @Unique
    private int tickCounter = 0;
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void aki$asyncHoglinAi(CallbackInfo ci) {
        if (!initialized) {
            aki$initHoglinOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        Hoglin hoglin = (Hoglin) (Object) this;
        ServerLevel level = (ServerLevel) hoglin.level();
        
        tickCounter++;
        
        if (tickCounter % 15 != 0) {
            return;
        }
        
        try {
            
            if (pendingDiff != null && pendingDiff.isDone()) {
                try {
                    HoglinDiff diff = pendingDiff.get(1, TimeUnit.MILLISECONDS);
                    diff.applyTo(hoglin, level);
                    
                    if (cached_debugEnabled) {
                        Bridge bridge = BridgeManager.getBridge();
                        if (bridge != null) {
                            bridge.debugLog(
                                "[AkiAsync-Hoglin] Applied diff: %s",
                                diff.toString()
                            );
                        }
                    }
                } catch (Exception e) {
                    
                }
                pendingDiff = null;
            }
            
            if (pendingDiff == null) {
                
                HoglinSnapshot snapshot = HoglinSnapshot.capture(hoglin, level);
                
                pendingDiff = CompletableFuture.supplyAsync(
                    () -> HoglinCpuCalculator.compute(snapshot)
                );
            }
        } catch (Exception e) {
            
            pendingDiff = null;
        }
    }
    
    @Unique
    private static synchronized void aki$initHoglinOptimization() {
        if (initialized) return;
        
        Bridge bridge = BridgeManager.getBridge();
        
        if (bridge != null) {
            
            cached_enabled = bridge.isHoglinOptimizationEnabled() && 
                           bridge.isAiSpatialIndexEnabled();
            cached_debugEnabled = bridge.isDebugLoggingEnabled();
            
            bridge.debugLog(
                "[AkiAsync] HoglinTickMixin initialized: enabled=%s | Using AI Spatial Index",
                cached_enabled
            );
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
    }
}
