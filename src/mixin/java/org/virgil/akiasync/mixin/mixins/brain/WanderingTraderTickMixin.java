package org.virgil.akiasync.mixin.mixins.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.wanderingtrader.WanderingTraderCpuCalculator;
import org.virgil.akiasync.mixin.brain.wanderingtrader.WanderingTraderDiff;
import org.virgil.akiasync.mixin.brain.wanderingtrader.WanderingTraderSnapshot;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mixin(value = WanderingTrader.class, priority = 1100)
public class WanderingTraderTickMixin {
    
    @Unique
    private static volatile boolean cached_enabled = false;
    
    @Unique
    private static volatile boolean cached_debugEnabled = false;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private CompletableFuture<WanderingTraderDiff> pendingDiff = null;
    
    @Unique
    private int tickCounter = 0;
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"), require = 0)
    private void aki$asyncTraderAi(CallbackInfo ci) {
        if (!initialized) {
            aki$initTraderOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        WanderingTrader trader = (WanderingTrader) (Object) this;
        ServerLevel level = (ServerLevel) trader.level();
        
        tickCounter++;
        
        if (tickCounter % 20 != 0) {
            return;
        }
        
        try {
            
            if (pendingDiff != null && pendingDiff.isDone()) {
                try {
                    WanderingTraderDiff diff = pendingDiff.get(1, TimeUnit.MILLISECONDS);
                    diff.applyTo(trader, level);
                    
                    if (cached_debugEnabled) {
                        Bridge bridge = BridgeManager.getBridge();
                        if (bridge != null) {
                            bridge.debugLog(
                                "[AkiAsync-WanderingTrader] Applied diff: %s",
                                diff.toString()
                            );
                        }
                    }
                } catch (Exception e) {
                    
                }
                pendingDiff = null;
            }
            
            if (pendingDiff == null) {
                
                WanderingTraderSnapshot snapshot = WanderingTraderSnapshot.capture(trader, level);
                
                pendingDiff = CompletableFuture.supplyAsync(
                    () -> WanderingTraderCpuCalculator.compute(snapshot)
                );
            }
        } catch (Exception e) {
            
            pendingDiff = null;
        }
    }
    
    @Unique
    private static synchronized void aki$initTraderOptimization() {
        if (initialized) return;
        
        Bridge bridge = BridgeManager.getBridge();
        
        if (bridge != null) {
            
            cached_enabled = bridge.isWanderingTraderOptimizationEnabled() && 
                           bridge.isAiSpatialIndexEnabled();
            cached_debugEnabled = bridge.isDebugLoggingEnabled();
            
            bridge.debugLog(
                "[AkiAsync] WanderingTraderTickMixin initialized: enabled=%s | Using AI Spatial Index",
                cached_enabled
            );
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
    }
}
