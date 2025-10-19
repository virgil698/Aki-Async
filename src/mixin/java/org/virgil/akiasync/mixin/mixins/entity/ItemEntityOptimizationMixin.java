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
        if (!(self.level() instanceof ServerLevel sl)) return;
        
        net.minecraft.core.BlockPos pos = self.blockPosition();
        net.minecraft.world.level.block.entity.BlockEntity be = sl.getBlockEntity(pos.below());
        
        if (be instanceof net.minecraft.world.Container) {
            System.out.println("[AkiAsync] Skip throttle: Container at " + pos);
            return;
        }
        
        if (sl.getGameTime() % 10 != 0) {
            ci.cancel();
        }
    }
}

