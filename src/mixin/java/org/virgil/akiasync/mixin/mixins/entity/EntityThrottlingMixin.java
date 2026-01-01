package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@Mixin(value = Entity.class, priority = 900)
public class EntityThrottlingMixin {

    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile boolean enabled = true;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$throttleEntityTick(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initEntityThrottling();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            Entity self = (Entity) (Object) this;

            if (self.level().isClientSide) {
                return;
            }

            
            if (self instanceof net.minecraft.world.entity.Mob ||
                self instanceof net.minecraft.world.entity.player.Player ||
                self instanceof net.minecraft.world.entity.animal.Animal ||
                self instanceof net.minecraft.world.entity.npc.Npc ||
                self instanceof net.minecraft.world.entity.decoration.ArmorStand ||
                self instanceof net.minecraft.world.entity.ExperienceOrb ||
                self instanceof net.minecraft.world.entity.vehicle.Boat ||
                self instanceof net.minecraft.world.entity.vehicle.AbstractMinecart) {
                return;
            }

            Bridge bridge = BridgeConfigCache.getBridge();
            if (bridge == null) {
                return;
            }

            if (bridge.isVirtualEntity(self)) {
                BridgeConfigCache.debugLog("[EntityThrottling] Excluded virtual entity from throttling: %s, uuid=%s",
                    self.getClass().getSimpleName(),
                    self.getUUID().toString()
                );
                return;
            }

            if (!self.onGround()) {
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

            if (bridge.shouldThrottleEntity(self)) {
                ci.cancel();
            }

        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityThrottling", "throttleCheck", e);
        }
    }
    
    @Unique
    private static synchronized void akiasync$initEntityThrottling() {
        if (initialized) return;
        
        Bridge bridge = BridgeConfigCache.getBridge();
        if (bridge != null) {
            enabled = bridge.isEntityThrottlingEnabled();
            bridge.debugLog("[AkiAsync] EntityThrottlingMixin initialized: enabled=" + enabled);
        
            initialized = true;
        } else {
            enabled = false; 
        }
    }
}
