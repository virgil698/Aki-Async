package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

@Mixin(AbstractMinecart.class)
public class MinecartTickOptimizeMixin {
    
    @Unique
    private static volatile boolean cached_enabled = false;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile double cached_staticThreshold = 0.01;
    
    @Unique
    private Vec3 aki$lastPosition = Vec3.ZERO;
    
    @Unique
    private int aki$staticTicks = 0;
    
    @Unique
    private int aki$tickCounter = 0;
    
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void optimizeMinecartTick(CallbackInfo ci) {
        if (!initialized) {
            aki$initMinecartOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        AbstractMinecart minecart = (AbstractMinecart) (Object) this;
        Vec3 currentPos = minecart.position();
        Vec3 deltaMovement = minecart.getDeltaMovement();
        
        boolean isStatic = false;
        if (aki$lastPosition != null) {
            double movement = currentPos.distanceTo(aki$lastPosition);
            double speed = deltaMovement.length();
            
            if (movement < cached_staticThreshold && speed < cached_staticThreshold) {
                aki$staticTicks++;
                isStatic = true;
            } else {
                aki$staticTicks = 0;
            }
        }
        
        aki$lastPosition = currentPos;
        
        if (isStatic && aki$staticTicks > 20) {
            aki$tickCounter++;
            
            if (aki$tickCounter % 5 != 0) {
                ci.cancel();
                return;
            }
        }
        
        if (minecart.getPassengers().isEmpty() && isStatic && aki$staticTicks > 100) {

            if (aki$tickCounter % 10 != 0) {
                ci.cancel();
            }
        }
    }
    
    @Unique
    private static void aki$initMinecartOptimization() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = true;
            cached_staticThreshold = 0.01;
            
            bridge.debugLog("[AkiAsync] MinecartTickOptimizeMixin initialized: enabled=" + 
                cached_enabled + " | threshold=" + cached_staticThreshold);
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
    }
}
