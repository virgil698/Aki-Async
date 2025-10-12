package com.example.plugin.mixin.mixins;

import com.example.plugin.mixin.BridgeManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO: remove this. this is only for example
@Mixin(MinecraftServer.class)
public abstract class TemplateBridgeMixin {
    @Inject(
            method = "runServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void afterServerStarted(CallbackInfo ci) {
        BridgeManager.INSTANCE.getBridge().interact("template");
    }
}
