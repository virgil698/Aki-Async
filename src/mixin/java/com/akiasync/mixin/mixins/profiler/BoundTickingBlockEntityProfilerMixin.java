package com.akiasync.mixin.mixins.profiler;

import com.akiasync.mixin.profiler.LagProfilerCollector;
import com.akiasync.mixin.Bridge.LagSourceType;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoundTickingBlockEntityProfilerMixin {
    @Shadow
    public abstract BlockPos getPos();

    @Shadow
    public abstract String getType();

    @WrapMethod(method = "tick")
    private void akiAsync$profileBlockEntity(Operation<Void> original) {
        long started = LagProfilerCollector.start(true);
        try {
            original.call();
        } finally {
            BlockPos position = getPos();
            LagProfilerCollector.record(
                    started, LagSourceType.BLOCK_ENTITY, "", getType(), "", true,
                    position.getX(), position.getY(), position.getZ()
            );
        }
    }
}
