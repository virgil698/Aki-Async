package org.virgil.akiasync.mixin.mixins.entity.throttling;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@Mixin(Mob.class)
public class MobSunBurnOptimizationMixin {

    @Unique
    private static boolean akiasync$initialized = false;

    @Unique
    private static boolean akiasync$enabled = true;

    @Unique
    private BlockPos akiasync$cachedEyeBlockPos;

    @Unique
    private int akiasync$cachedPositionHashcode;

    @Unique
    private static void akiasync$initialize() {
        if (akiasync$initialized) {
            return;
        }

        try {
            Bridge bridge = BridgeConfigCache.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isMobSunBurnOptimizationEnabled();

                akiasync$initialized = true;
            }
        } catch (Exception e) {

            akiasync$enabled = true;
        }
    }

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void onIsSunBurnTick(CallbackInfoReturnable<Boolean> cir) {

        if (!akiasync$initialized) {
            akiasync$initialize();
        }

        if (!akiasync$enabled) {
            return;
        }

        Mob self = (Mob) (Object) this;

        if (!self.level().isClientSide) {

            int positionHashCode = self.position().hashCode();
            if (this.akiasync$cachedPositionHashcode != positionHashCode) {
                this.akiasync$cachedEyeBlockPos = new BlockPos(
                    (int) self.getX(),
                    (int) self.getEyeY(),
                    (int) self.getZ()
                );
                this.akiasync$cachedPositionHashcode = positionHashCode;
            }

            float brightness = self.level().hasChunkAt(self.getBlockX(), self.getBlockZ())
                ? self.level().getLightLevelDependentMagicValue(akiasync$cachedEyeBlockPos)
                : 0.0F;

            if (brightness <= 0.5F) {
                cir.setReturnValue(false);
                return;
            }

            if (self.getRandom().nextFloat() * 30.0F >= (brightness - 0.4F) * 2.0F) {
                cir.setReturnValue(false);
                return;
            }

            boolean inWaterOrSnow = self.isInWaterOrRain() || self.isInPowderSnow || self.wasInPowderSnow;

            if (!inWaterOrSnow && self.level().canSeeSky(akiasync$cachedEyeBlockPos)) {
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(false);
            }
        }
    }
}
