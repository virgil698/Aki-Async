package org.virgil.akiasync.mixin.mixins.entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.LivingEntity;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@SuppressWarnings("unused")
@Mixin(value = LivingEntity.class, priority = 900)
public abstract class PushEntitiesOptimizationMixin {
    @Unique
    private static volatile boolean enabled;
    @Unique
    private static volatile int interval = 2;
    
    static {
        
        Thread initThread = new Thread(() -> {
            try {
                akiasync$initPushOptimization();
            } catch (Exception e) {
                
                enabled = true;
            }
        }, "PushOptimization-Init");
        initThread.setDaemon(true);
        initThread.start();
    }
    
    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void optimizePush(CallbackInfo ci) {
        
        if (!enabled) {
            return;
        }
        
        LivingEntity self = (LivingEntity) (Object) this;
        
        if (self instanceof net.minecraft.world.entity.monster.Shulker) {
            ci.cancel();
            return;
        }
        
        if (interval > 1 && self.tickCount % interval != 0) {
            ci.cancel();
            return;
        }
        
        if (self.getDeltaMovement().lengthSqr() < 1.0E-7) {
            
            if (self.onGround() && !self.isInWater() && !self.isInLava()) {
                
                if (self.tickCount % 10 != 0) {
                    ci.cancel();
                }
            }
            
        }
    }
    
    @Unique
    private static synchronized void akiasync$initPushOptimization() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeConfigCache.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isPushOptimizationEnabled();
            BridgeConfigCache.debugLog("[AkiAsync] PushOptimizationMixin initialized: enabled=" + enabled);
        } else {
            enabled = true;
        }
    }
}
