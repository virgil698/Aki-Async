package org.virgil.akiasync.mixin.mixins.util;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.TPSTracker;

import java.util.function.BooleanSupplier;


@Mixin(MinecraftServer.class)
public class TPSTrackerMixin {
    
    
    @Inject(
        method = "tickChildren(Ljava/util/function/BooleanSupplier;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void akiasync$onServerTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        try {
            TPSTracker.getInstance().onTick();
        } catch (Throwable t) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "TPSTracker", "onTick",
                t instanceof Exception ? (Exception) t : new RuntimeException(t));
        }
    }
}
