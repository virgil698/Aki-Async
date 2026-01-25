package org.virgil.akiasync.mixin.mixins.world;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SectionStorage.class)
public abstract class SectionStorageMixin<R, P> implements AutoCloseable {

    @WrapMethod(method = "unpackChunk(Lnet/minecraft/world/level/ChunkPos;)V")
    private synchronized void release(ChunkPos pos, Operation<Void> original) {
        original.call(pos);
    }
}
