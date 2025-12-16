package org.virgil.akiasync.mixin.mixins.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.warden.WardenCpuCalculator;
import org.virgil.akiasync.mixin.brain.warden.WardenDiff;
import org.virgil.akiasync.mixin.brain.warden.WardenSnapshot;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mixin(value = Warden.class, priority = 1100)
public class WardenTickMixin {
    
    @Unique
    private static volatile boolean cached_enabled = false;
    
    @Unique
    private static volatile boolean cached_debugEnabled = false;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private CompletableFuture<WardenDiff> pendingDiff = null;
    
    @Unique
    private int tickCounter = 0;
    
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void aki$ensureWardenVanillaFeatures(ServerLevel level, CallbackInfo ci) {
        
        if (!initialized) {
            aki$initWardenOptimization();
        }
    }
    
    @Unique
    private static synchronized void aki$initWardenOptimization() {
        if (initialized) return;
        
        Bridge bridge = BridgeManager.getBridge();
        
        if (bridge != null) {
            
            cached_enabled = false; 
            cached_debugEnabled = bridge.isDebugLoggingEnabled();
            
            bridge.debugLog(
                "[AkiAsync] WardenTickMixin initialized: Vanilla behavior preserved (anger, darkness, vibrations)"
            );
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
    }
}
