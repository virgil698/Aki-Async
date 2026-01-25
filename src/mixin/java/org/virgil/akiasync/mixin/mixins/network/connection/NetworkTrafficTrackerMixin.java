package org.virgil.akiasync.mixin.mixins.network.connection;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.network.NetworkTrafficTracker;

@SuppressWarnings("unused")
@Mixin(Connection.class)
public class NetworkTrafficTrackerMixin {

    @Shadow
    private int receivedPackets;

    @Shadow
    private int sentPackets;

    @Inject(method = "tickSecond", at = @At("HEAD"), require = 0)
    private void trackTrafficPerSecond(CallbackInfo ci) {
        NetworkTrafficTracker.recordIncoming(this.receivedPackets * 64L);
        NetworkTrafficTracker.recordOutgoing(this.sentPackets * 64L);
    }
}
