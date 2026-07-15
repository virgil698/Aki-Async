package com.akiasync.mixin.mixins.profiler;

import com.akiasync.mixin.profiler.LagProfilerCollector;
import com.akiasync.mixin.Bridge.LagSourceType;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RegisteredListener.class)
public abstract class RegisteredListenerProfilerMixin {
    @Shadow
    public abstract Plugin getPlugin();

    @WrapMethod(method = "callEvent")
    private void akiAsync$profileEvent(Event event, Operation<Void> original) {
        long started = LagProfilerCollector.start(false);
        try {
            original.call(event);
        } finally {
            Plugin plugin = getPlugin();
            LagProfilerCollector.record(
                    started, LagSourceType.PLUGIN_EVENT, plugin == null ? "unknown" : plugin.getName(),
                    event.getEventName(), "", false, 0, 0, 0
            );
        }
    }
}
