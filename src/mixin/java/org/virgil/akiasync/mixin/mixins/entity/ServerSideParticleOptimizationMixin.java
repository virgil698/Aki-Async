package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public class ServerSideParticleOptimizationMixin {

    @Redirect(
        method = "baseTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;canSpawnSprintParticle()Z"
        )
    )
    private boolean skipParticlesOnServer(Entity instance) {
        if (instance.level().isClientSide()) {
            return instance.canSpawnSprintParticle();
        }
        return false;
    }
}
