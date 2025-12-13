package org.virgil.akiasync.mixin.mixins.brain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;


@Mixin(Brain.class)
public class BrainConcurrentMixin {

    @Shadow
    @Final
    @Mutable
    private Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories;

    
    @SuppressWarnings("unused")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void replaceMemoriesWithConcurrent(CallbackInfo ci) {
        
        Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> oldMemories = this.memories;
        this.memories = new ConcurrentHashMap<>(oldMemories);
    }
}
