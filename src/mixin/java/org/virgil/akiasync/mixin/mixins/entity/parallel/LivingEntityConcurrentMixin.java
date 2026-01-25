package org.virgil.akiasync.mixin.mixins.entity.parallel;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = LivingEntity.class, priority = 900)
public abstract class LivingEntityConcurrentMixin extends Entity {

    @Shadow
    private final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = new ConcurrentHashMap<>();

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
        original.call(strength, x, z);
    }

    @WrapMethod(method = "tickEffects")
    private void akiasync$tickEffectsSynchronized(Operation<Void> original) {
        original.call();
    }

    @WrapMethod(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean akiasync$addEffectSynchronized(MobEffectInstance effect, Entity source, Operation<Boolean> original) {
        return original.call(effect, source);
    }

    @WrapMethod(method = "removeEffect")
    private boolean akiasync$removeEffectSynchronized(Holder<MobEffect> effect, Operation<Boolean> original) {
        return original.call(effect);
    }

    @WrapMethod(method = "removeAllEffects")
    private boolean akiasync$removeAllEffectsSynchronized(Operation<Boolean> original) {
        return original.call();
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void akiasync$causeFallDamage(double fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        BlockPos pos = new BlockPos(Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()));
        BlockState currentBlock = this.level().getBlockState(pos);

        if (currentBlock.is(BlockTags.CLIMBABLE)) {
            cir.setReturnValue(false);
        }
    }
}

