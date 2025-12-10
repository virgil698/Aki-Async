package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.TPSTracker;

/**
 * 药水效果TT20补偿
 * 
 * 在低TPS时额外tick药水效果，保持持续时间一致
 * 
 * 警告：可能改变游戏平衡，默认关闭
 * 
 * 参考：TT20 LivingEntityMixin
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPotionTT20Mixin {
    
    @Shadow
    protected abstract void tickEffects();
    
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = false;
    @Unique
    private static volatile double tpsThreshold = 18.0;
    
    @Inject(method = "baseTick", 
        at = @At(value = "INVOKE", 
                 target = "Lnet/minecraft/world/entity/LivingEntity;tickEffects()V",
                 shift = At.Shift.AFTER))
    private void compensatePotionEffects(CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) return;
        
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;
        
        try {
            TPSTracker tracker = TPSTracker.getInstance();
            double currentTPS = tracker.getMostAccurateTPS();
            
            if (currentTPS < tpsThreshold) {
                int missedTicks = tracker.getApplicableMissedTicks();
                
                for (int i = 0; i < missedTicks; i++) {
                    tickEffects();
                }
            }
        } catch (Throwable t) {
            
        }
    }
    
    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isSmartLagPotionEffectsEnabled();
            tpsThreshold = bridge.getSmartLagTPSThreshold();
            
            bridge.debugLog("[AkiAsync] LivingEntityPotionTT20Mixin initialized: enabled=%s, threshold=%.1f",
                enabled, tpsThreshold);
        } else {
            enabled = false;
        }
        
        initialized = true;
    }
}
