package org.virgil.akiasync.mixin.mixins.world;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Level.class, priority = 1500)
public abstract class LevelMixin implements LevelAccessor, AutoCloseable {

    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V")
    private void explode(Entity source, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction explosionInteraction, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(source, x, y, z, radius, fire, explosionInteraction);
        }
    }

    @WrapMethod(method = "explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V")
    private void explode(Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction explosionInteraction, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(source, x, y, z, radius, explosionInteraction);
        }
    }

    @WrapMethod(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V")
    private void explode(Entity source, DamageSource damageSource, ExplosionDamageCalculator damageCalculator, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction explosionInteraction, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(source, damageSource, damageCalculator, x, y, z, radius, fire, explosionInteraction);
        }
    }
}
