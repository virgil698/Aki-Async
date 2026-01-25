package org.virgil.akiasync.mixin.mixins.entity.breed;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(VillagerMakeLove.class)
public class VillagerMakeLoveConcurrentMixin {

    @Unique
    private static final Object aki$lock = new Object();

    @Unique
    private static boolean aki$hasValidBreedTarget(LivingEntity entity) {
        try {
            Optional<AgeableMob> breedTarget = entity.getBrain().getMemory(MemoryModuleType.BREED_TARGET);
            return breedTarget.isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void aki$checkBreedTargetBeforeTick(ServerLevel level, LivingEntity owner, long gameTime, CallbackInfo ci) {
        synchronized (aki$lock) {
            if (!aki$hasValidBreedTarget(owner)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "canStillUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void aki$checkBreedTargetBeforeCanStillUse(ServerLevel level, LivingEntity entity, long gameTime, CallbackInfoReturnable<Boolean> cir) {
        synchronized (aki$lock) {
            if (!aki$hasValidBreedTarget(entity)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void aki$checkBreedTargetBeforeStart(ServerLevel level, LivingEntity entity, long gameTime, CallbackInfo ci) {
        synchronized (aki$lock) {
            if (!aki$hasValidBreedTarget(entity)) {
                ci.cancel();
            }
        }
    }
}
