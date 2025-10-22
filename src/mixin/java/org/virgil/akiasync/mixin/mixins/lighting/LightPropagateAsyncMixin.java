package org.virgil.akiasync.mixin.mixins.lighting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.minecraft.world.level.lighting.LightEngine", remap = false)
public class LightPropagateAsyncMixin {
    
    @Inject(method = "runLightUpdates", at = @At("HEAD"), cancellable = true)
    private void handler$zzi000$asyncLight(CallbackInfoReturnable<Integer> cir) {
        if (this == null) return;
        
        try {
            java.lang.reflect.Field levelField = this.getClass().getDeclaredField("level");
            levelField.setAccessible(true);
            Object level = levelField.get(this);
            
            if (level == null) return;
            
            boolean isClientSide = false;
            try {
                java.lang.reflect.Method isClientMethod = level.getClass().getMethod("isClientSide");
                isClientSide = (boolean) isClientMethod.invoke(level);
            } catch (Exception ignored) {}
            
            if (isClientSide) return;
            
            cir.setReturnValue(0);
            
            try {
                java.lang.reflect.Method getSchedulerMethod = level.getClass().getMethod("moonrise$getChunkTaskScheduler");
                Object scheduler = getSchedulerMethod.invoke(level);
                
                java.lang.reflect.Field ioExecutorField = scheduler.getClass().getDeclaredField("ioExecutor");
                ioExecutorField.setAccessible(true);
                Object ioExecutor = ioExecutorField.get(scheduler);
                
                if (ioExecutor == null) {
                    return;
                }
                
                final Object engine = this;
                java.lang.reflect.Method submitMethod = ioExecutor.getClass().getMethod("submit", 
                    Object.class, Object.class, Runnable.class);
                
                submitMethod.invoke(ioExecutor, level, engine, (Runnable) () -> {
                    try {
                        java.lang.reflect.Method propagateIncMethod = engine.getClass().getDeclaredMethod("propagateIncreases");
                        propagateIncMethod.setAccessible(true);
                        propagateIncMethod.invoke(engine);
                        
                        java.lang.reflect.Method propagateDecMethod = engine.getClass().getDeclaredMethod("propagateDecreases");
                        propagateDecMethod.setAccessible(true);
                        propagateDecMethod.invoke(engine);
                        
                        java.lang.reflect.Method swapMethod = engine.getClass().getDeclaredMethod("swapSectionMap");
                        swapMethod.setAccessible(true);
                        swapMethod.invoke(engine);
                    } catch (Exception ex) {
                    }
                });
                
            } catch (Exception e) {
            }
            
        } catch (Exception e) {
        }
    }
}

