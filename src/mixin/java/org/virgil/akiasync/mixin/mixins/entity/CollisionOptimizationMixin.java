package org.virgil.akiasync.mixin.mixins.entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("unused")
@Mixin(Entity.class)
public abstract class CollisionOptimizationMixin {
    @Unique
    private static volatile boolean enabled;
    @Unique
    private static volatile double minMovement = 0.001D;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean aggressiveMode = true;
    
    @Unique
    private Vec3 lastCheckedPosition = Vec3.ZERO;
    @Unique
    private long lastCollisionCheck = 0;
    @Unique
    private static final long COLLISION_CHECK_INTERVAL = 50;
    @Inject(method = "checkInsideBlocks", at = @At("HEAD"), cancellable = true)
    private void optimizeBlockCollision(CallbackInfo ci) {
        if (!initialized) { akiasync$initCollisionOptimization(); }
        if (!enabled) return;
        Entity self = (Entity) (Object) this;

        if (akiasync$isVirtualEntity(self)) {
            return;
        }
        
        if (aggressiveMode) {
            long currentTime = System.currentTimeMillis();
            Vec3 currentPos = self.position();
            
            if (currentTime - lastCollisionCheck < COLLISION_CHECK_INTERVAL) {
                double distSqr = currentPos.distanceToSqr(lastCheckedPosition);
                if (distSqr < minMovement * minMovement) {
                    ci.cancel();
                    return;
                }
            }
            
            lastCollisionCheck = currentTime;
            lastCheckedPosition = currentPos;
        }

        if (self instanceof net.minecraft.world.entity.item.ItemEntity item) {
            if (!item.isInLava() && !item.isOnFire() && item.getRemainingFireTicks() <= 0) {
                if (self.getDeltaMovement().lengthSqr() < minMovement) {
                    ci.cancel();
                }
            }
            return;
        }

        if (self instanceof net.minecraft.world.entity.vehicle.AbstractMinecart) {
            return;
        }

        if (self instanceof net.minecraft.world.entity.item.FallingBlockEntity falling) {
            double verticalSpeed = Math.abs(falling.getDeltaMovement().y);
            if (verticalSpeed < 0.01) {
                return;
            }
            ci.cancel();
            return;
        }

        if (self.isInLava() || self.isOnFire() || self.getRemainingFireTicks() > 0) {
            return;
        }

        if (self instanceof net.minecraft.world.entity.LivingEntity living) {
            if (living.getTicksFrozen() > 0) {
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
        
        double selfMovementSqr = self.getDeltaMovement().lengthSqr();
        double otherMovementSqr = other.getDeltaMovement().lengthSqr();
        
        if (selfMovementSqr < minMovement && otherMovementSqr < minMovement) {
            ci.cancel();
            return;
        }
        
        if (aggressiveMode) {
            double threshold = minMovement * 4;
            if ((selfMovementSqr < minMovement && otherMovementSqr < threshold) ||
                (otherMovementSqr < minMovement && selfMovementSqr < threshold)) {
                ci.cancel();
            }
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
