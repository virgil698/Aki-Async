package org.virgil.akiasync.mixin.mixins.ai;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.sensing.Sensing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@Mixin(Sensing.class)
public class SensingCacheOptimizationMixin {
    
    @Shadow @Final private Mob mob;
    @Shadow @Final private IntSet seen;
    @Shadow @Final private IntSet unseen;
    
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile int clearInterval = 5;
    
    @Unique
    private int aki$ticksSinceLastClear = 0;
    
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$optimizeCacheClear(CallbackInfo ci) {
        if (!initialized) {
            aki$initConfig();
        }
        
        if (!enabled) {
            return; 
        }
        
        aki$ticksSinceLastClear++;
        
        boolean shouldClear = aki$shouldClearCache();
        
        if (!shouldClear) {
            
            ci.cancel();
        } else {
            
            aki$ticksSinceLastClear = 0;
        }
    }
    
    @Unique
    private boolean aki$shouldClearCache() {
        
        if (aki$ticksSinceLastClear >= clearInterval) {
            return true;
        }
        
        if (mob.isAggressive() && aki$ticksSinceLastClear >= 2) {
            return true;
        }
        
        if (mob.getTarget() != null && aki$ticksSinceLastClear >= 2) {
            return true;
        }
        
        if (mob.getLastHurtByMob() != null && aki$ticksSinceLastClear >= 1) {
            return true;
        }
        
        if (mob instanceof net.minecraft.world.entity.NeutralMob neutralMob) {
            if (neutralMob.getRemainingPersistentAngerTime() > 0 || neutralMob.getPersistentAngerTarget() != null) {
                return true;
            }
        }
        
        if (mob instanceof net.minecraft.world.entity.monster.Monster && aki$ticksSinceLastClear >= 2) {
            return true;
        }
        
        if (mob.getDeltaMovement().lengthSqr() > 0.001 && aki$ticksSinceLastClear >= 3) {
            return true;
        }
        
        if (seen.size() + unseen.size() > 50) {
            return true;
        }
        
        return false;
    }
    
    @Unique
    private static synchronized void aki$initConfig() {
        if (initialized) {
            return;
        }
        
        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                enabled = bridge.isAiSensorOptimizationEnabled();
                clearInterval = bridge.getAiSensorRefreshInterval();
                
                bridge.debugLog("[SensingCacheOptimization] Initialized: enabled=%s, clearInterval=%d ticks",
                    enabled, clearInterval);
            
                    initialized = true;
                }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "SensingCacheOptimizationMixin", "initConfig", e);
        }
    }
}
