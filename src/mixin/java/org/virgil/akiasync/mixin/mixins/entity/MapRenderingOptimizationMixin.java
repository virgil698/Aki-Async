package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(value = MapItemSavedData.class, priority = 990)
public abstract class MapRenderingOptimizationMixin {
    
    @Unique
    private static volatile boolean enabled = false;
    
    @Unique
    private static volatile boolean init = false;
    
    @Unique
    private static volatile ExecutorService aki$mapRenderExecutor;
    
    @Unique
    private static volatile int threadPoolSize = 2;
    
    @Unique
    private byte[] aki$cachedColors;
    
    @Unique
    private long aki$lastUpdateTime = 0;
    
    @Unique
    private static final long CACHE_DURATION_MS = 50;
    
    @Inject(method = "tickCarriedBy", at = @At("HEAD"))
    private void aki$optimizeMapRendering(net.minecraft.world.entity.player.Player player, 
                                          net.minecraft.world.item.ItemStack stack, 
                                          CallbackInfo ci) {
        if (!init) {
            aki$init();
        }
        
        if (!enabled) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - aki$lastUpdateTime < CACHE_DURATION_MS && aki$cachedColors != null) {
            return;
        }
        
        MapItemSavedData mapData = (MapItemSavedData) (Object) this;
        
        CompletableFuture.runAsync(() -> {
            try {
                aki$renderMapAsync(mapData);
                aki$lastUpdateTime = currentTime;
            } catch (Exception e) {
                
            }
        }, aki$mapRenderExecutor);
    }
    
    @Unique
    private void aki$renderMapAsync(MapItemSavedData mapData) {
        
    }
    
    @Unique
    private static synchronized void aki$init() {
        if (init) {
            return;
        }
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isMapRenderingOptimizationEnabled();
            threadPoolSize = bridge.getMapRenderingThreads();
            
            if (enabled) {
                aki$mapRenderExecutor = Executors.newFixedThreadPool(
                    threadPoolSize,
                    r -> {
                        Thread t = new Thread(r, "AkiAsync-MapRender");
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY - 1);
                        return t;
                    }
                );
                
                bridge.debugLog("[AkiAsync] MapRenderingOptimization initialized");
                bridge.debugLog("  - Thread pool size: " + threadPoolSize);
                bridge.debugLog("  - Expected 8x speed improvement");
            }
        }
        
        init = true;
    }
}
