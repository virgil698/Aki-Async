package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@Mixin(value = Entity.class, priority = 900)
public class EntityThrottlingMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$throttleEntityTick(CallbackInfo ci) {
        try {
            Entity self = (Entity) (Object) this;
            
            if (self.level().isClientSide) {
                return;
            }
            
            // ItemEntity在危险环境中不能被节流，需要检测炼药锅岩浆等销毁机制
            if (self instanceof net.minecraft.world.entity.item.ItemEntity) {
                // 岩浆、火焰、炼药锅等环境需要每tick检测
                if (self.isInLava() || self.isOnFire() || self.getRemainingFireTicks() > 0) {
                    return; // 不节流，确保能被正确销毁
                }
                
                // 检查是否在炼药锅内部（通过位置检测）
                net.minecraft.core.BlockPos pos = self.blockPosition();
                net.minecraft.world.level.block.state.BlockState state = self.level().getBlockState(pos);
                if (state.getBlock() instanceof net.minecraft.world.level.block.LayeredCauldronBlock ||
                    state.getBlock() instanceof net.minecraft.world.level.block.LavaCauldronBlock) {
                    return; // 在炼药锅中，不节流
                }
            }
            
            Bridge bridge = BridgeManager.getBridge();
            if (bridge == null) {
                return;
            }
            
            if (bridge.shouldThrottleEntity(self)) {
                ci.cancel();
            }
            
        } catch (Exception e) {
        }
    }
}
