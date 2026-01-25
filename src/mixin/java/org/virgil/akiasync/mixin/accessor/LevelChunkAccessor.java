package org.virgil.akiasync.mixin.accessor;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelChunk.class)
public interface LevelChunkAccessor {

    @Invoker("akiasync$shouldDoLightning")
    boolean invokeShouldDoLightning(RandomSource random);
}
