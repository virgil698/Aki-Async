package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityDimensionRefreshMixin {
    
    @Shadow @Final
    protected static EntityDataAccessor<Pose> DATA_POSE;
    
    @Inject(
        method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V",
        at = @At("RETURN")
    )
    private void clearCollisionCacheOnDimensionRefresh(EntityDataAccessor<?> key, CallbackInfo ci) {
        
        if (DATA_POSE.equals(key)) {
            Entity self = (Entity) (Object) this;
            
            try {
                
                org.virgil.akiasync.mixin.util.EntityCollisionCache.clearCacheForEntity(self);
                
                org.virgil.akiasync.mixin.util.CollisionableCache.clearCacheForEntity(self);
                
            } catch (Exception e) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "EntityDimensionRefresh", "clearCollisionCache", e);
            }
        }
    }
}
