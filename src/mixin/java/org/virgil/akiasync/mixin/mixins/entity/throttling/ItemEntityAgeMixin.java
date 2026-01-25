package org.virgil.akiasync.mixin.mixins.entity.throttling;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

@SuppressWarnings("unused")
@Mixin(ItemEntity.class)
public abstract class ItemEntityAgeMixin {

    @Shadow
    public int age;

    @Unique
    private static volatile boolean enabled;
    @Unique
    private static volatile int ageIncrementInterval;
    @Unique
    private static volatile double playerDetectionRange;
    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private int aki$ageTickCounter = 0;
    @Unique
    private boolean aki$isStatic = false;
    @Unique
    private net.minecraft.world.phys.Vec3 aki$lastPosition = null;
    @Unique
    private int aki$staticTicks = 0;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void optimizeAgeTick(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initAgeOptimization();
        }
        if (!enabled) return;

        ItemEntity self = (ItemEntity) (Object) this;

        if (akiasync$isVirtualEntity(self)) {
            return;
        }

        if (akiasync$isInDangerousEnvironment(self)) {
            return;
        }

        akiasync$updateStaticState(self);

        if (aki$isStatic && !akiasync$hasNearbyPlayer(self) && self.onGround()) {
            net.minecraft.world.phys.Vec3 deltaMovement = self.getDeltaMovement();
            double velocitySq = deltaMovement.lengthSqr();

            if (velocitySq < 0.0001) {
                aki$ageTickCounter++;

                if (aki$ageTickCounter % ageIncrementInterval == 0) {
                    org.virgil.akiasync.mixin.util.BridgeConfigCache.debugLog(
                        "[AkiAsync-ItemAge] Static item detected: age=%d, pos=%s, staticTicks=%d",
                        age, self.blockPosition(), aki$staticTicks
                    );
                    aki$ageTickCounter = 0;
                }
            }
        }

    }

    @Unique
    private void akiasync$updateStaticState(ItemEntity self) {
        net.minecraft.world.phys.Vec3 currentPos = self.position();

        if (aki$lastPosition == null) {
            aki$lastPosition = currentPos;
            aki$staticTicks = 0;
            aki$isStatic = false;
            return;
        }

        double distanceSq = currentPos.distanceToSqr(aki$lastPosition);

        if (distanceSq < 0.0001 && self.onGround()) {
            aki$staticTicks++;

            if (aki$staticTicks >= 20) {
                aki$isStatic = true;
            }
        } else {
            aki$staticTicks = 0;
            aki$isStatic = false;
        }

        aki$lastPosition = currentPos;
    }

    @Unique
    private boolean akiasync$hasNearbyPlayer(ItemEntity self) {
        AABB searchBox = self.getBoundingBox().inflate(playerDetectionRange);

        List<Player> nearbyPlayers = self.level().getEntitiesOfClass(
            Player.class,
            searchBox
        );

        return !nearbyPlayers.isEmpty();
    }

    @Unique
    private boolean akiasync$isInDangerousEnvironment(ItemEntity item) {
        if (item.isInLava() || item.isOnFire() || item.getRemainingFireTicks() > 0) {
            return true;
        }

        if (item.isInWater()) {
            return true;
        }

        net.minecraft.core.BlockPos pos = item.blockPosition();
        net.minecraft.world.level.block.state.BlockState state = item.level().getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.LayeredCauldronBlock ||
            state.getBlock() instanceof net.minecraft.world.level.block.LavaCauldronBlock) {
            return true;
        }

        return false;
    }

    @Unique
    private boolean akiasync$isVirtualEntity(ItemEntity entity) {
        if (entity == null) return false;

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                return bridge.isVirtualEntity(entity);
            }
        } catch (Throwable t) {
            return true;
        }

        return false;
    }

    @Unique
    private static synchronized void akiasync$initAgeOptimization() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (bridge != null) {
            enabled = bridge.isItemEntityAgeOptimizationEnabled();
            ageIncrementInterval = bridge.getItemEntityAgeInterval();
            playerDetectionRange = bridge.getItemEntityPlayerDetectionRange();

            initialized = true;
        } else {
            enabled = false;
            ageIncrementInterval = 10;
            playerDetectionRange = 8.0;
        }

        if (bridge != null) {
            bridge.debugLog("[AkiAsync] ItemEntityAgeMixin initialized: enabled=" + enabled +
                ", ageIncrementInterval=" + ageIncrementInterval +
                ", playerDetectionRange=" + playerDetectionRange);
        }
    }
}
