package org.virgil.akiasync.mixin.mixins.brain;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Brain.class)
public class BrainConcurrentMixin {

    @Shadow
    private final Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = 
        new ConcurrentHashMap<>();
}
