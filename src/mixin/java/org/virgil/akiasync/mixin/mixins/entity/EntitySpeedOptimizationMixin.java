package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;


@Mixin(Entity.class)
public abstract class EntitySpeedOptimizationMixin {
    
    @Unique
    private static boolean akiasync$initialized = false;
    
    @Unique
    private static boolean akiasync$enabled = true;
    
    @Shadow
    public abstract Vec3 getDeltaMovement();
    
    @Shadow
    protected abstract float getBlockSpeedFactor();
    
    @Shadow
    public abstract BlockPos blockPosition();
    
    @Unique
    private float akiasync$cachedBlockSpeedFactor = 1.0F;
    
    @Unique
    private BlockPos akiasync$cachedSpeedFactorPos = null;
    
    @Unique
    private static void akiasync$initialize() {
        if (akiasync$initialized) {
            return;
        }
        
        try {
            Bridge bridge = BridgeConfigCache.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isEntitySpeedOptimizationEnabled();
            }
        } catch (Exception e) {
            
            akiasync$enabled = true;
        }
        
        akiasync$initialized = true;
    }
    
    
    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getBlockSpeedFactor()F"
        )
    )
    private float cacheBlockSpeedFactor(Entity instance) {
        
        if (!akiasync$initialized) {
            akiasync$initialize();
        }
        
        
        if (!akiasync$enabled) {
            return this.getBlockSpeedFactor();
        }
        
        
        if (instance instanceof net.minecraft.world.entity.player.Player) {
            return this.getBlockSpeedFactor();
        }
        
        
        if (instance instanceof net.minecraft.world.entity.animal.Bee) {
            return this.getBlockSpeedFactor();
        }
        
        
        Vec3 movement = this.getDeltaMovement();
        if (movement.x == 0.0 && movement.z == 0.0) {
            
            BlockPos currentPos = this.blockPosition();
            if (currentPos.equals(akiasync$cachedSpeedFactorPos)) {
                return akiasync$cachedBlockSpeedFactor;
            }
        }
        
        
        BlockPos currentPos = this.blockPosition();
        if (!currentPos.equals(akiasync$cachedSpeedFactorPos)) {
            akiasync$cachedBlockSpeedFactor = this.getBlockSpeedFactor();
            akiasync$cachedSpeedFactorPos = currentPos.immutable();
        }
        
        return akiasync$cachedBlockSpeedFactor;
    }
}
