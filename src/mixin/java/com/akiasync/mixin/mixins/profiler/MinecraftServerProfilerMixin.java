package com.akiasync.mixin.mixins.profiler;

import com.akiasync.mixin.profiler.LagProfilerCollector;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerProfilerMixin {
    @Shadow
    public abstract int getTickCount();

    @WrapMethod(method = "tickServer")
    private void akiAsync$profileTick(BooleanSupplier hasTimeLeft, Operation<Void> original) {
        LagProfilerCollector.beginTick(getTickCount());
        try {
            original.call(hasTimeLeft);
        } finally {
            LagProfilerCollector.endTick();
        }
    }
}
