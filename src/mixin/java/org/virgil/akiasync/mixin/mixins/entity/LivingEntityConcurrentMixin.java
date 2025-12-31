package org.virgil.akiasync.mixin.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Mixin(value = LivingEntity.class, priority = 900)
public abstract class LivingEntityConcurrentMixin extends Entity {

    @Shadow
    private final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = new ConcurrentHashMap<>();

    @Unique
    private static final Object akiasync$effectLock = new Object();

    public LivingEntityConcurrentMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @WrapMethod(method = "die")
    private synchronized void akiasync$dieSynchronized(DamageSource damageSource, Operation<Void> original) {
        original.call(damageSource);
    }

    @WrapMethod(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V")
    private synchronized void akiasync$dropFromLootTableSynchronized(ServerLevel level, DamageSource damageSource, boolean playerKill, Operation<Void> original) {
        original.call(level, damageSource, playerKill);
    }

    @WrapMethod(method = "knockback")
    private void akiasync$knockbackSynchronized(double strength, double x, double z, Operation<Void> original) {
        synchronized (akiasync$effectLock) {
            original.call(strength, x, z);
        }
    }

    @WrapMethod(method = "tickEffects")
    private void akiasync$tickEffectsSynchronized(Operation<Void> original) {
        synchronized (akiasync$effectLock) {
            original.call();
        }
    }

    @WrapMethod(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean akiasync$addEffectSynchronized(MobEffectInstance effect, Entity source, Operation<Boolean> original) {
        synchronized (akiasync$effectLock) {
            return original.call(effect, source);
        }
    }

    @WrapMethod(method = "removeEffect")
    private boolean akiasync$removeEffectSynchronized(Holder<MobEffect> effect, Operation<Boolean> original) {
        synchronized (akiasync$effectLock) {
            return original.call(effect);
        }
    }

    @WrapMethod(method = "removeAllEffects")
    private boolean akiasync$removeAllEffectsSynchronized(Operation<Boolean> original) {
        synchronized (akiasync$effectLock) {
            return original.call();
        }
    }
}
