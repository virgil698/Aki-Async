package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.collision.NativeCollisionPusher;

import java.util.LinkedList;


@Mixin(value = LivingEntity.class, priority = 1050)
public abstract class LivingEntityCollisionAccessMixin implements NativeCollisionPusher.AcceleratedListAccess {
    
    @Unique
    private LinkedList<Entity> akiasync$acceleratedList = null;
    
    @Override
    public void akiasync$setAcceleratedList(LinkedList<Entity> list) {
        this.akiasync$acceleratedList = list;
    }
    
    @Override
    public LinkedList<Entity> akiasync$getAcceleratedList() {
        return this.akiasync$acceleratedList;
    }
    
    
    @Inject(
        method = "pushEntities",
        at = @At("HEAD"),
        cancellable = true
    )
    private void useAcceleratedListForPush(CallbackInfo ci) {
        LinkedList<Entity> list = akiasync$getAcceleratedList();
        
        if (list != null && !list.isEmpty()) {
            LivingEntity self = (LivingEntity) (Object) this;
            
            
            for (Entity entity : list) {
                if (entity != null && !entity.isRemoved()) {
                    self.push(entity);
                }
            }
            
            
            akiasync$acceleratedList = null;
            
            
            ci.cancel();
        }
    }
}
