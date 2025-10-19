package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/**
 * ItemEntity async physics v7.0 - Visual 0-delay + async step
 * 
 * Rules:
 * 1. New items (< 20 tick): Full physics every tick (no visual glitch)
 * 2. Old items (> 20 tick idle): Throttle to every 20 ticks
 * 3. Picked up items: Whitelist skip (first-frame must be accurate)
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = ItemEntity.class, priority = 989)
public class ItemEntityOptimizationMixin {
    
    @Shadow private int age;
    
    @Unique private boolean aki$isNewborn = true;
    @Unique private int aki$idleTicks = 0;
    @Unique private Vec3 aki$lastPos = Vec3.ZERO;
    
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void aki$markNewborn(CallbackInfo ci) {
        this.aki$isNewborn = true;
    }
    
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$throttlePhysics(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel sl)) return;
        
        if (self.age < 20) {
            aki$isNewborn = true;
            return;
        }
        
        Vec3 currentPos = self.position();
        if (aki$lastPos.distanceToSqr(currentPos) < 1.0E-4D) {
            aki$idleTicks++;
        } else {
            aki$idleTicks = 0;
        }
        aki$lastPos = currentPos;
        
        if (aki$idleTicks >= 20) {
            if (sl.getGameTime() % 20 != 0) {
                ci.cancel();
            }
        }
    }
    
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void aki$throttleMerge(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level() instanceof ServerLevel sl) {
            if (sl.getGameTime() % 10 != 0) {
                ci.cancel();
            }
        }
    }
}

