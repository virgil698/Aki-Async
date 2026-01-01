package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Connection.class)
public class MultiNettyEventLoopMixin {

    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;

    @Inject(method = "setupInboundProtocol", at = @At("RETURN"))
    private <T extends PacketListener> void onSetupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initConfig();
        }

        if (!enabled) {
            return;
        }

        try {
            ConnectionProtocol protocol = protocolInfo.id();
            
            
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                bridge.handleConnectionProtocolChange((Connection)(Object)this, protocol.ordinal());
            
                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "MultiNettyEventLoop", "onSetupInboundProtocol", e);
        }
    }

    @Unique
    private static synchronized void akiasync$initConfig() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isMultiNettyEventLoopEnabled();
                bridge.debugLog("[MultiNettyEventLoop] Initialized: enabled=%s", enabled);
            
                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "MultiNettyEventLoop", "initConfig", e);
        }
    }
}
