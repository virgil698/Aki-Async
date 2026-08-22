package com.akiasync.mixin.mixins.profiler;

import com.akiasync.mixin.profiler.LagProfilerCollector;
import com.akiasync.mixin.Bridge.LagSourceType;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.bukkit.craftbukkit.scheduler.CraftTask;
import org.bukkit.plugin.Plugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CraftTask.class)
public abstract class CraftTaskProfilerMixin {
    @Shadow
    public abstract Plugin getOwner();

    @Shadow
    public abstract int getTaskId();

    @WrapMethod(method = "run")
    private void akiAsync$profilePluginTask(Operation<Void> original) {
        if (!LagProfilerCollector.isDetailedActive()) {
            original.call();
            return;
        }
        long started = LagProfilerCollector.start(true);
        try {
            original.call();
        } finally {
            long elapsed = LagProfilerCollector.elapsed(started);
            if (LagProfilerCollector.shouldRecord(elapsed)) {
                Plugin owner = getOwner();
                LagProfilerCollector.recordElapsed(
                        elapsed, LagSourceType.PLUGIN_TASK, owner == null ? "unknown" : owner.getName(),
                        "task #" + getTaskId(), "", false, 0, 0, 0
                );
            }
        }
    }
}
