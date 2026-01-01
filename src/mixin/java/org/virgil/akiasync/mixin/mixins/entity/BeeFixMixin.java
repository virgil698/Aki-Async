package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

@SuppressWarnings("unused")
@Mixin(Bee.class)
public abstract class BeeFixMixin {

    @Unique private static volatile boolean initialized = false;
    @Unique private static volatile boolean enabled = true;

    @Inject(method = "wantsToEnterHive", at = @At("HEAD"), cancellable = true, require = 0)
    private void aki$fixHiveLeavingInOtherDimensions(CallbackInfoReturnable<Boolean> cir) {
        if (!aki$isEnabled()) return;

        Bee bee = (Bee) (Object) this;
        Level level = bee.level();

        if (!level.dimension().equals(Level.OVERWORLD)) {
            if (bee.getHivePos() != null && bee.getTarget() == null) {
                BlockPos hivePos = bee.getHivePos();
                if (bee.distanceToSqr(Vec3.atCenterOf(hivePos)) < 64.0) {
                    if (level.getBlockEntity(hivePos) instanceof BeehiveBlockEntity hive) {
                        
                        if (hive.getOccupantCount() >= 3) {
                            cir.setReturnValue(false);
                        }
                        
                    }
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void aki$preventBeesFalling(CallbackInfo ci) {
        if (!aki$isEnabled()) return;

        Bee bee = (Bee) (Object) this;

        if (!bee.onGround() && bee.getDeltaMovement().y < 0) {
            if (bee.isFlying() || bee.getTarget() != null || bee.hasHive()) {
                Vec3 motion = bee.getDeltaMovement();
                bee.setDeltaMovement(motion.x, Math.max(motion.y, -0.1), motion.z);
            }
        }
    }

    @ModifyArg(method = "getRandomPos", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ai/util/RandomPos;getPos(Lnet/minecraft/world/entity/PathfinderMob;IILnet/minecraft/world/phys/Vec3;)Lnet/minecraft/core/BlockPos;"),
        index = 3, require = 0)
    private Vec3 aki$fixWanderingBias(Vec3 direction) {
        if (!aki$isEnabled()) return direction;

        Bee bee = (Bee) (Object) this;
        RandomSource random = bee.getRandom();

        double angle = random.nextDouble() * 2 * Math.PI;
        double x = Math.cos(angle);
        double z = Math.sin(angle);

        return new Vec3(x, direction.y, z);
    }

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true, require = 0)
    private void aki$preventDespawnWithHive(CallbackInfo ci) {
        if (!aki$isEnabled()) return;

        Bee bee = (Bee) (Object) this;

        if (bee.hasHive()) {
            ci.cancel();
            return;
        }

        if (bee.hasNectar()) {
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("TAIL"), require = 0)
    private void aki$fixDeathAnimation(net.minecraft.world.damagesource.DamageSource damageSource, CallbackInfo ci) {
        if (!aki$isEnabled()) return;

        Bee bee = (Bee) (Object) this;

        if (bee.isDeadOrDying()) {
            bee.setYRot(bee.getYRot() + 180.0F);
        }
    }

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true, require = 0)
    private void aki$preventTurtleEggTrampling(BlockPos pos, net.minecraft.world.level.block.state.BlockState blockState, CallbackInfo ci) {
        if (!aki$isEnabled()) return;

        if (blockState.getBlock() instanceof net.minecraft.world.level.block.TurtleEggBlock) {
            ci.cancel();
        }
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"), require = 0)
    private void aki$optimizePathfinding(CallbackInfo ci) {
        if (!aki$isEnabled()) return;

        Bee bee = (Bee) (Object) this;

        if (bee.hasHive() && bee.getTarget() == null && bee.tickCount % 20 == 0) {
            BlockPos hivePos = bee.getHivePos();
            if (hivePos != null && bee.distanceToSqr(Vec3.atCenterOf(hivePos)) < 256.0) {
                return;
            }
        }
    }

    @Unique
    private static boolean aki$isEnabled() {
        if (!initialized) {
            aki$initBeeFix();
        }
        return enabled;
    }

    @Unique
    private static synchronized void aki$initBeeFix() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isBeeFixEnabled();
            bridge.debugLog("[AkiAsync] BeeFixMixin initialized: enabled=" + enabled);
            bridge.debugLog("[AkiAsync]   Fixed bugs: MC-168329, MC-190042, MC-206401, MC-229321, MC-234364, MC-248332, MC-255743");
        
            initialized = true;
        } else {
            enabled = false; 
        }
    }
}
