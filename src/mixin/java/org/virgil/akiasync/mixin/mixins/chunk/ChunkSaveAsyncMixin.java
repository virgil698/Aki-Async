package org.virgil.akiasync.mixin.mixins.chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.level.ChunkHolder;

@Pseudo
@Mixin(value = ChunkHolder.class, remap = false)
public class ChunkSaveAsyncMixin {
    
    @Redirect(
        method = "moonrise$scheduleSave",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkHolder;moonrise$unsafeSave(Z)V"
        ),
        require = 0
    )
    private void handler$zzi000$delegateSave(ChunkHolder holder, boolean flush) {
        if (flush) {
            callUnsafeSave(holder, flush);
            return;
        }
        
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
            callUnsafeSave(holder, flush);
        }
    }
    
    private void callUnsafeSave(ChunkHolder holder, boolean flush) {
        try {
            java.lang.reflect.Method unsafeSaveMethod = holder.getClass().getDeclaredMethod("moonrise$unsafeSave", boolean.class);
            unsafeSaveMethod.setAccessible(true);
            unsafeSaveMethod.invoke(holder, flush);
        } catch (Exception e) {
        }
    }
}
