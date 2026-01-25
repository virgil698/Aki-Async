package org.virgil.akiasync.mixin.mixins.entity.parallel;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = Brain.class, priority = 900)
public class BrainConcurrentMixin {

    @Shadow
    private final Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = new ConcurrentHashMap<>();

    @WrapMethod(method = "getMemory")
    private <U> Optional<U> aki$safeGetMemory(MemoryModuleType<U> type, Operation<Optional<U>> original) {
        Optional<U> result = original.call(type);
        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }
        return result;
    }
}
