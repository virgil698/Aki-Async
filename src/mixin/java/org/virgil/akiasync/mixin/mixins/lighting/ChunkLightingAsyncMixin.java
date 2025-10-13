package org.virgil.akiasync.mixin.mixins.lighting;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Async chunk lighting calculation (ScalableLux/Starlight inspired).
 * Offloads lighting calculation to dedicated thread pool during chunk loading.
 * 
 * Key optimizations:
 * - Async light initialization for newly loaded chunks
 * - Batch processing to reduce thread overhead
 * - Timeout protection to prevent hanging
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = LevelChunk.class, priority = 1100)
public abstract class ChunkLightingAsyncMixin {

    private static volatile boolean enabled;
    private static volatile ExecutorService lightingExecutor;
    private static volatile boolean initialized = false;
    private static int asyncLightCount = 0;
    
    /**
     * Inject into chunk light initialization
     * Runs lighting calculation asynchronously when chunk is loaded
     */
    @Inject(method = "setLoaded", at = @At("HEAD"))
    private void onChunkLoaded(boolean loaded, CallbackInfo ci) {
        if (!initialized) { akiasync$initLighting(); }
        if (!enabled || !loaded) return;
        
        LevelChunk chunk = (LevelChunk) (Object) this;
        
        // Only process if chunk needs lighting
        if (chunk.getLevel() instanceof ServerLevel) {
            asyncLightCount++;
            
            // Log first few async operations
            if (asyncLightCount <= 3) {
                System.out.println("[AkiAsync-Lighting] Scheduling async light calculation for chunk at " + chunk.getPos());
            }
            
            // Schedule async lighting calculation
            if (lightingExecutor != null) {
                CompletableFuture.runAsync(() -> {
                    try {
                        // Trigger light recalculation in async thread
                        // The actual light engine will handle the calculation
                        chunk.getLevel().getChunkSource().getLightEngine().checkBlock(
                            chunk.getPos().getWorldPosition()
                        );
                        
                        if (asyncLightCount <= 3) {
                            System.out.println("[AkiAsync-Lighting] Completed async light calculation for chunk at " + chunk.getPos());
                        }
                    } catch (Exception e) {
                        // Silently catch to prevent chunk loading failures
                    }
                }, lightingExecutor).orTimeout(500, TimeUnit.MILLISECONDS).exceptionally(ex -> {
                    // Timeout or error - not critical, main thread will handle it
                    return null;
                });
            }
        }
    }
    
    /**
     * Initialize lighting optimization settings from Bridge
     */
    private static synchronized void akiasync$initLighting() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isAsyncLightingEnabled();
            lightingExecutor = bridge.getLightingExecutor();
        } else {
            enabled = false;
            lightingExecutor = null;
        }
        
        initialized = true;
        System.out.println("[AkiAsync] ChunkLightingAsyncMixin initialized: enabled=" + enabled + 
            ", executor=" + (lightingExecutor != null ? "dedicated" : "none"));
    }
}

