package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;


@Mixin(Entity.class)
public abstract class EntityFallDamageOptimizationMixin {
    
    @Unique
    private static boolean akiasync$initialized = false;
    
    @Unique
    private static boolean akiasync$enabled = true;

    @Shadow
    public abstract boolean onGround();

    @Shadow
    public abstract void resetFallDistance();
    
    @Unique
    private static void akiasync$initialize() {
        if (akiasync$initialized) {
            return;
        }
        
        try {
            Bridge bridge = BridgeConfigCache.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isEntityFallDamageOptimizationEnabled();
            
                akiasync$initialized = true;
            }
        } catch (Exception e) {
            
            akiasync$enabled = true;
        }
    }

    
    @Inject(method = "checkFallDamage", at = @At("HEAD"), cancellable = true)
    private void skipSimpleEntityFallDamage(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition, CallbackInfo ci) {
        
        if (!akiasync$initialized) {
            akiasync$initialize();
        }
        
        
        if (!akiasync$enabled) {
            return;
        }
        
        Entity entity = (Entity) (Object) this;
        
        
        if (!(entity instanceof LivingEntity) && 
            !(entity instanceof Boat) && 
            !(entity instanceof AbstractMinecart)) {
            
            
            if (entity instanceof ItemEntity || entity instanceof Projectile) {
                
                if (this.onGround()) {
                    this.resetFallDistance();
                }
                
                ci.cancel();
            }
        }
    }
}
