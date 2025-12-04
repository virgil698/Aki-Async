package org.virgil.akiasync.mixin.mixins.entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.Entity;
@SuppressWarnings("unused")
@Mixin(Entity.class)
public abstract class CollisionOptimizationMixin {
    private static volatile boolean enabled;
    private static volatile double minMovement = 0.001D;
    private static volatile boolean initialized = false;
    @Inject(method = "checkInsideBlocks", at = @At("HEAD"), cancellable = true)
    private void optimizeBlockCollision(CallbackInfo ci) {
        if (!initialized) { akiasync$initCollisionOptimization(); }
        if (!enabled) return;
        Entity self = (Entity) (Object) this;

        if (akiasync$isVirtualEntity(self)) {
            return;
        }

        if (self instanceof net.minecraft.world.entity.item.ItemEntity item) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync-Collision] Preserving checkInsideBlocks for ItemEntity: " +
                    "inLava=" + item.isInLava() + ", onFire=" + item.isOnFire() +
                    ", fireTicks=" + item.getRemainingFireTicks() +
                    ", pos=" + item.blockPosition());
            }
            return;
        }

        if (self instanceof net.minecraft.world.entity.vehicle.AbstractMinecart minecart) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync-Collision] Preserving checkInsideBlocks for Minecart: " +
                    "inLava=" + minecart.isInLava() + ", onFire=" + minecart.isOnFire() +
                    ", fireTicks=" + minecart.getRemainingFireTicks() +
                    ", pos=" + minecart.blockPosition());
            }
            return;
        }

        if (self instanceof net.minecraft.world.entity.item.FallingBlockEntity falling) {
            double verticalSpeed = Math.abs(falling.getDeltaMovement().y);

            if (verticalSpeed < 0.01) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null && bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync-Collision] FallingBlock near ground, preserving full collision check: " +
                        "verticalSpeed=" + verticalSpeed + ", pos=" + falling.blockPosition());
                }
                return;
            }

            return;
        }

        if (self.isInLava() || self.isOnFire() || self.getRemainingFireTicks() > 0) {
            return;
        }

        if (self instanceof net.minecraft.world.entity.LivingEntity living) {
            if (living.getTicksFrozen() > 0) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null && bridge.isDebugLoggingEnabled()) {
                    if (living instanceof net.minecraft.world.entity.player.Player player) {
                        bridge.debugLog("[AkiAsync-Collision] Preserving checkInsideBlocks for frozen entity: " +
                            "inPowderSnow=" + player.isInPowderSnow + ", ticksFrozen=" + living.getTicksFrozen());
                    }
                }
                return;
            }
        }

        if (self instanceof net.minecraft.world.entity.projectile.Projectile) {
            return;
        }


        net.minecraft.core.BlockPos pos = self.blockPosition();
        if (pos != null) {
            net.minecraft.world.level.block.state.BlockState state = self.level().getBlockState(pos);
            if (state != null && state.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null && bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync-Collision] Preserving checkInsideBlocks for entity in portal: " +
                        "type=" + self.getType().getDescriptionId() + ", pos=" + pos);
                }
                return;
            }
        }

        if (self.getDeltaMovement().lengthSqr() < minMovement) {
            ci.cancel();
        }
    }
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void optimizeEntityPush(Entity other, CallbackInfo ci) {
        if (!enabled) return;
        Entity self = (Entity) (Object) this;

        if (akiasync$isVirtualEntity(self) || akiasync$isVirtualEntity(other)) {
            return;
        }
        if (self.getDeltaMovement().lengthSqr() < minMovement &&
            other.getDeltaMovement().lengthSqr() < minMovement) {
            ci.cancel();
        }
    }

    private boolean akiasync$isVirtualEntity(Entity entity) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            return bridge.isVirtualEntity(entity);
        }
        return false;
    }

    private static synchronized void akiasync$initCollisionOptimization() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isCollisionOptimizationEnabled();
        } else {
            enabled = true;
        }
        initialized = true;
        if (bridge != null) {
            bridge.debugLog("[AkiAsync] CollisionOptimizationMixin initialized: enabled=" + enabled);
        }
    }
}
