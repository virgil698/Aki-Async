package org.virgil.akiasync.mixin.mixins.chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.level.ChunkHolder;

@Pseudo
@Mixin(value = ChunkHolder.class, remap = false)
public class ChunkSaveAsyncMixin {
    
    private static volatile Boolean isFolia = null;
    private static volatile boolean initialized = false;
    private static volatile long foliaCallCount = 0;
    private static volatile long asyncCallCount = 0;
    private static volatile long lastLogTime = 0;
    
    private static boolean checkFolia() {
        if (isFolia == null) {
            synchronized (ChunkSaveAsyncMixin.class) {
                if (isFolia == null) {
                    try {
                        Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                        isFolia = true;
                        logFoliaDetection(true);
                    } catch (ClassNotFoundException e) {
                        isFolia = false;
                        logFoliaDetection(false);
                    }
                    initialized = true;
                }
            }
        }
        return isFolia;
    }
    
    private static void logFoliaDetection(boolean detected) {
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                if (detected) {
                    bridge.debugLog("[AkiAsync-ChunkSave] Folia environment detected - async chunk save DISABLED for compatibility");
                    bridge.debugLog("[AkiAsync-ChunkSave] Using native Folia chunk save mechanism to prevent deadlocks");
                } else {
                    bridge.debugLog("[AkiAsync-ChunkSave] Paper environment detected - async chunk save ENABLED");
                    bridge.debugLog("[AkiAsync-ChunkSave] Using optimized async save executor");
                }
            }
        } catch (Exception e) {
        }
    }
    
    private static void logStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > 60000) {
            lastLogTime = currentTime;
            try {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    if (isFolia) {
                        bridge.debugLog(String.format(
                            "[AkiAsync-ChunkSave] Folia mode stats - Native saves: %d",
                            foliaCallCount
                        ));
                    } else {
                        bridge.debugLog(String.format(
                            "[AkiAsync-ChunkSave] Paper mode stats - Async saves: %d",
                            asyncCallCount
                        ));
                    }
                }
            } catch (Exception e) {
            }
        }
    }
    
    @Redirect(
        method = "moonrise$scheduleSave",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkHolder;moonrise$unsafeSave(Z)V"
        ),
        require = 0
    )
    private void handler$zzi000$delegateSave(ChunkHolder holder, boolean flush) {
        if (checkFolia()) {
            foliaCallCount++;
            logStatistics();
            callUnsafeSave(holder, flush);
            return;
        }
        
        if (flush) {
            callUnsafeSave(holder, flush);
            return;
        }
        
        asyncCallCount++;
        logStatistics();
        
        try {
            java.lang.reflect.Field levelField = holder.getClass().getDeclaredField("level");
            levelField.setAccessible(true);
            Object level = levelField.get(holder);
            
            if (level == null) {
                callUnsafeSave(holder, flush);
                return;
            }
            
            java.lang.reflect.Method getSchedulerMethod = level.getClass().getMethod("moonrise$getChunkTaskScheduler");
            Object scheduler = getSchedulerMethod.invoke(level);
            
            java.lang.reflect.Field saveExecutorField = scheduler.getClass().getDeclaredField("saveExecutor");
            saveExecutorField.setAccessible(true);
            Object saveExecutor = saveExecutorField.get(scheduler);
            
            if (saveExecutor == null) {
                callUnsafeSave(holder, flush);
                return;
            }
            
            java.lang.reflect.Method executeMethod = saveExecutor.getClass().getMethod("execute", 
                java.util.concurrent.Executor.class, Runnable.class, long.class);
            
            long pos = 0L;
            try {
                java.lang.reflect.Method getPosMethod = holder.getClass().getMethod("toLong");
                pos = (long) getPosMethod.invoke(holder);
            } catch (Exception ignored) {}
            
            final long finalPos = pos;
            final ChunkHolder finalHolder = holder;
            executeMethod.invoke(saveExecutor, null, (Runnable) () -> {
                try {
                    callUnsafeSave(finalHolder, false);
                } catch (Exception ex) {
                }
            }, finalPos);
            
        } catch (Exception e) {
            try {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null && asyncCallCount <= 5) {
                    bridge.debugLog("[AkiAsync-ChunkSave] Async save failed, fallback to sync: " + e.getMessage());
                }
            } catch (Exception ignored) {}
            callUnsafeSave(holder, flush);
        }
    }
    
    private void callUnsafeSave(ChunkHolder holder, boolean flush) {
        try {
            java.lang.reflect.Method unsafeSaveMethod = holder.getClass()
                .getDeclaredMethod("moonrise$unsafeSave", boolean.class);
            unsafeSaveMethod.setAccessible(true);
            unsafeSaveMethod.invoke(holder, flush);
        } catch (Exception e) {
            try {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.errorLog("[AkiAsync-ChunkSave] CRITICAL: Failed to save chunk - " + e.getMessage());
                }
            } catch (Exception ignored) {}
        }
    }
}
