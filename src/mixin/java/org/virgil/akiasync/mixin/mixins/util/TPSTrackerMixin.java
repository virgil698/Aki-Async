package org.virgil.akiasync.mixin.mixins.util;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.TPSTracker;

@Mixin(MinecraftServer.class)
public class TPSTrackerMixin {
    
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onServerTick(CallbackInfo ci) {
        try {
            TPSTracker.getInstance().onTick();
        } catch (Throwable t) {
            
        }
    }
}
