package org.virgil.akiasync.mixin.mixins.entitytracker;

import ca.spottedleaf.moonrise.common.list.ReferenceList;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.accessor.ChunkMapTrackerAccess;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.entitytracker.MultithreadedEntityTracker;

import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(ChunkMap.class)
public abstract class ChunkMapTrackerMixin implements ChunkMapTrackerAccess {
    
    @Shadow @Final ServerLevel level;
    
    @Unique
    private MultithreadedEntityTracker aki$multithreadedTracker;
    
    @Unique
    private final ConcurrentLinkedQueue<Runnable> aki$trackerMainThreadTasks = new ConcurrentLinkedQueue<>();
    
    @Unique
    private boolean aki$tracking = false;
    
    @Unique
    private static volatile boolean aki$initialized = false;
    
    @Unique
    private static volatile boolean aki$enabled = true;
    
    @Unique
    public void aki$runOnTrackerMainThread(final Runnable runnable) {
        if (!aki$initialized) {
            aki$initConfig();
        }
        
        if (!aki$enabled) {
            runnable.run();
            return;
        }
        
        if (Thread.currentThread() == this.level.getServer().getRunningThread()) {
            
            runnable.run();
            return;
        }
        
        
        if (this.aki$trackerMainThreadTasks != null && this.aki$tracking) {
            
            this.aki$trackerMainThreadTasks.add(runnable);
        } else {
            try {
                this.level.getServer().execute(runnable);
            } catch (UnsupportedOperationException e) {
                runnable.run();
            }
        }
    }
    
    @Inject(method = "newTrackerTick", at = @At("HEAD"), cancellable = true)
    private void aki$useMultithreadedTracker(CallbackInfo ci) {
        if (!aki$initialized) {
            aki$initConfig();
        }
        
        if (!aki$enabled) {
            return; 
        }
        
        if (this.aki$multithreadedTracker == null) {
            ReferenceList<LevelChunk> entityTickingChunks = aki$getEntityTickingChunks();
            this.aki$multithreadedTracker = new MultithreadedEntityTracker(
                entityTickingChunks, 
                this.aki$trackerMainThreadTasks
            );
        }
        
        this.aki$tracking = true;
        try {
            this.aki$multithreadedTracker.tick();
        } finally {
            this.aki$tracking = false;
        }
        
        ci.cancel(); 
    }
    
    @Unique
    private ReferenceList<LevelChunk> aki$getEntityTickingChunks() {
        try {
            
            Object worldData = this.level.getClass().getMethod("getCurrentWorldData").invoke(this.level);
            return (ReferenceList<LevelChunk>) worldData.getClass()
                .getMethod("getEntityTickingChunks")
                .invoke(worldData);
        } catch (Exception e) {
            
            try {
                return ((ChunkSystemServerLevel) this.level).moonrise$getEntityTickingChunks();
            } catch (Exception ex) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "ChunkMapTrackerMixin", "getEntityTickingChunks", ex);
                throw new RuntimeException("Failed to get entity ticking chunks", ex);
            }
        }
    }
    
    @Unique
    private static synchronized void aki$initConfig() {
        if (aki$initialized) {
            return;
        }
        
        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                aki$enabled = bridge.isMultithreadedEntityTrackerEnabled();
                
                try {
                    Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                    aki$enabled = false;
                    if (bridge != null) {
                        bridge.debugLog("[MultithreadedEntityTracker] Detected Folia/Luminol environment - disabling multithreaded entity tracker");
                    }
                } catch (ClassNotFoundException ignored) {
                }
                
                if (bridge != null) {
                    bridge.debugLog("[MultithreadedEntityTracker] Initialized: enabled=%s, parallelism=%d",
                        aki$enabled, Runtime.getRuntime().availableProcessors());
                }
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ChunkMapTrackerMixin", "initConfig", e);
        }
        
        aki$initialized = true;
    }
}
