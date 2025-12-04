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

            if (self instanceof net.minecraft.world.entity.Mob) {
                return;
            }

            if (self instanceof net.minecraft.world.entity.player.Player) {
                return;
            }


            if (self instanceof net.minecraft.world.entity.decoration.ArmorStand) {
                return;
            }

            Bridge bridge = BridgeManager.getBridge();
            if (bridge == null) {
                return;
            }

            if (bridge.isVirtualEntity(self)) {
                
                if (bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[EntityThrottling] Excluded virtual entity from throttling: %s, uuid=%s",
                        self.getClass().getSimpleName(),
                        self.getUUID().toString()
                    );
                }
                return;
            }

            if (self instanceof net.minecraft.world.entity.item.ItemEntity) {

                if (self.isInLava() || self.isOnFire() || self.getRemainingFireTicks() > 0) {
                    return;
                }
                

                if (self.isInWater() || self.isInWaterOrRain()) {
                    return;
                }

                net.minecraft.core.BlockPos pos = self.blockPosition();
                if (pos == null) {
                    return;
                }
                
                net.minecraft.world.level.block.state.BlockState state = self.level().getBlockState(pos);
                if (state != null && (state.getBlock() instanceof net.minecraft.world.level.block.LayeredCauldronBlock ||
                    state.getBlock() instanceof net.minecraft.world.level.block.LavaCauldronBlock)) {
                    return;
                }

                if (state != null && state.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock) {
                    return;
                }
            }

            if (self instanceof net.minecraft.world.entity.vehicle.AbstractMinecart) {

                if (self.isInLava() || self.isOnFire() || self.getRemainingFireTicks() > 0) {
                    return;
                }
                

                if (self.isInWater() || self.isInWaterOrRain()) {
                    return;
                }

                net.minecraft.core.BlockPos pos = self.blockPosition();
                if (pos == null) {
                    return;
                }
                
                net.minecraft.world.level.block.state.BlockState state = self.level().getBlockState(pos);
                if (state != null && state.getBlock() instanceof net.minecraft.world.level.block.LavaCauldronBlock) {
                    return;
                }

                if (state != null && state.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock) {
                    return;
                }
            }

            if (self instanceof net.minecraft.world.entity.ExperienceOrb) {
                return; 
            }

            if (bridge.shouldThrottleEntity(self)) {
                ci.cancel();
            }

        } catch (Exception e) {
        }
    }
}
