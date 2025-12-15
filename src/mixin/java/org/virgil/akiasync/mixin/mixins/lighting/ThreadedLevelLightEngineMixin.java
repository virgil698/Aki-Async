package org.virgil.akiasync.mixin.mixins.lighting;

import java.util.concurrent.atomic.AtomicLong;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface;
import ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;


@Mixin(value = ThreadedLevelLightEngine.class, priority = 1100)
public abstract class ThreadedLevelLightEngineMixin extends LevelLightEngine implements StarLightLightingProvider {
    
    public ThreadedLevelLightEngineMixin(LightChunkGetter lightChunkGetter, boolean blockLight, boolean skyLight) {
        super(lightChunkGetter, blockLight, skyLight);
    }
    
    @Unique
    private static volatile boolean enabled = false;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile long updateIntervalNanos = 10_000_000L; 
    
    @Unique
    private final AtomicLong aki$lastLightUpdate = new AtomicLong(0);
    
    
    @Inject(
            method = "tryScheduleUpdate",
            at = @At("HEAD"),
            cancellable = true
    )
    @SuppressWarnings("unused")
    private void optimizeScheduling(CallbackInfo ci) {
        if (!initialized) {
            aki$init();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            
            
            StarLightInterface starlightEngine = this.starlight$getLightEngine();
            if (starlightEngine != null && starlightEngine.hasUpdates()) {
                
                
                return;
            }
            
            
            long lastUpdate = this.aki$lastLightUpdate.get();
            long currentTime = System.nanoTime();
            
            if (currentTime - lastUpdate < updateIntervalNanos) {
                
                
                ci.cancel();
                return;
            }
            
            
            this.aki$lastLightUpdate.compareAndSet(lastUpdate, currentTime);
            
        } catch (RuntimeException e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ThreadedLevelLightEngine", "optimizeScheduling", e);
        }
    }
    
    
    @Inject(
            method = "checkBlock",
            at = @At("RETURN")
    )
    @SuppressWarnings("unused")
    private void onBlockChange(BlockPos blockPos, CallbackInfo ci) {
        if (enabled) {
            
            
            this.aki$lastLightUpdate.set(0);
        }
    }
    
    
    @Inject(
            method = "updateSectionStatus",
            at = @At("RETURN")
    )
    @SuppressWarnings("unused")
    private void onSectionChange(SectionPos pos, boolean notReady, CallbackInfo ci) {
        if (enabled) {
            this.aki$lastLightUpdate.set(0);
        }
    }
    
    @Unique
    private static synchronized void aki$init() {
        if (initialized) return;
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isAsyncLightingEnabled();
            
            
            int intervalMs = bridge.getLightUpdateIntervalMs();
            updateIntervalNanos = intervalMs * 1_000_000L;
            
            if (bridge.isLightingDebugEnabled()) {
                if (enabled) {
                    bridge.debugLog("[AkiAsync-Lighting] ThreadedLevelLightEngineMixin initialized (ScalableLux-style):");
                    bridge.debugLog("[AkiAsync-Lighting]   - Update interval: %dms", intervalMs);
                    bridge.debugLog("[AkiAsync-Lighting]   - Strategy: Rate limiting + queue awareness");
                    bridge.debugLog("[AkiAsync-Lighting]   - Note: Works with Paper/Luminol's StarLight engine");
                } else {
                    bridge.debugLog("[AkiAsync-Lighting] ThreadedLevelLightEngineMixin disabled");
                }
            }
        }
        
        initialized = true;
    }
}

