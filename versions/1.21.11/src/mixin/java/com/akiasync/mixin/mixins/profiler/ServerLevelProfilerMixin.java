package com.akiasync.mixin.mixins.profiler;

import com.akiasync.mixin.profiler.LagProfilerCollector;
import com.akiasync.mixin.Bridge.LagSourceType;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelProfilerMixin {
    @WrapMethod(method = "tick")
    private void akiAsync$profileWorld(BooleanSupplier hasTimeLeft, Operation<Void> original) {
        long started = LagProfilerCollector.start(false);
        try {
            original.call(hasTimeLeft);
        } finally {
            long elapsed = LagProfilerCollector.elapsed(started);
            if (LagProfilerCollector.shouldRecord(elapsed)) {
                LagProfilerCollector.recordElapsed(
                        elapsed, LagSourceType.WORLD, "", "world tick", akiAsync$world(), false, 0, 0, 0
                );
            }
        }
    }

    @WrapMethod(method = "tickChunk")
    private void akiAsync$profileChunk(LevelChunk chunk, int randomTickSpeed, Operation<Void> original) {
        if (!LagProfilerCollector.isDetailedActive()) {
            original.call(chunk, randomTickSpeed);
            return;
        }
        long started = LagProfilerCollector.start(true);
        try {
            original.call(chunk, randomTickSpeed);
        } finally {
            long elapsed = LagProfilerCollector.elapsed(started);
            if (LagProfilerCollector.shouldRecord(elapsed)) {
                LagProfilerCollector.recordElapsed(
                        elapsed, LagSourceType.CHUNK, "", "chunk tick", akiAsync$world(), true,
                        chunk.getPos().x, 0, chunk.getPos().z
                );
            }
        }
    }

    @WrapMethod(method = "tickNonPassenger")
    private void akiAsync$profileEntity(Entity entity, Operation<Void> original) {
        if (!LagProfilerCollector.isDetailedActive()) {
            original.call(entity);
            return;
        }
        long started = LagProfilerCollector.start(true);
        try {
            original.call(entity);
        } finally {
            long elapsed = LagProfilerCollector.elapsed(started);
            if (LagProfilerCollector.shouldRecord(elapsed)) {
                BlockPos position = entity.blockPosition();
                LagProfilerCollector.recordElapsed(
                        elapsed, entity instanceof Mob ? LagSourceType.MOB : LagSourceType.ENTITY,
                        "", entity.getType().toString(), akiAsync$world(), true,
                        position.getX(), position.getY(), position.getZ()
                );
            }
        }
    }

    @Unique
    private String akiAsync$world() {
        return ((ServerLevel) (Object) this).dimension().identifier().toString();
    }
}
