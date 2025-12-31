package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Mixin(value = Brain.class, priority = 900)
public class BrainConcurrentMemoryMixin<E extends LivingEntity> {

    @Shadow
    private final Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = new ConcurrentHashMap<>();

    
    @Inject(method = "getMemory", at = @At("RETURN"), cancellable = true)
    private <U> void akiasync$wrapGetMemory(MemoryModuleType<U> type, CallbackInfoReturnable<Optional<U>> cir) {
        Optional<U> result = cir.getReturnValue();
        if (result == null) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
