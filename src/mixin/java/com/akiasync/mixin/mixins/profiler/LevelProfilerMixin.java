package com.akiasync.mixin.mixins.profiler;

import com.akiasync.mixin.profiler.LagProfilerCollector;
import com.akiasync.mixin.Bridge.LagSourceType;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public abstract class LevelProfilerMixin {
    @WrapMethod(method = "tickBlockEntities")
    private void akiAsync$profileBlockEntityPhase(Operation<Void> original) {
        long started = LagProfilerCollector.start(false);
        try {
            original.call();
        } finally {
            String world = (Object) this instanceof ServerLevel level
                    ? level.dimension().location().toString()
                    : "";
            LagProfilerCollector.record(
                    started, LagSourceType.BLOCK_ENTITIES, "", "block entity phase", world, false, 0, 0, 0
            );
        }
    }
}
