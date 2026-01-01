package org.virgil.akiasync.mixin.mixins.brain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.server.level.ServerLevel;

@Mixin(BehaviorBuilder.Instance.class)
public class BehaviorThrottleMixin<E extends LivingEntity> {
    
    @Unique
    private static volatile boolean cached_enabled = false;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile int cached_throttleInterval = 3;
    
    @Unique
    private int aki$triggerCounter = 0;
    
    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true, require = 0)
    private void throttleTrigger(ServerLevel level, E entity, long gameTime, CallbackInfoReturnable<Boolean> cir) {
        if (!initialized) {
            aki$initThrottle();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        aki$triggerCounter++;
        if (aki$triggerCounter % cached_throttleInterval != 0) {
            cir.setReturnValue(false);
        }
    }
    
    @Unique
    private static void aki$initThrottle() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {

            cached_enabled = false;
            cached_throttleInterval = 3;
            
            bridge.debugLog("[AkiAsync] BehaviorThrottleMixin initialized: enabled=" + 
                cached_enabled + " | interval=" + cached_throttleInterval);
        
            initialized = true;
        } else {
            cached_enabled = false;
        }
    }
}
