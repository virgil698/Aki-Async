package org.virgil.akiasync.mixin.mixins.network.packet;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@Mixin(ClientboundMoveEntityPacket.class)
public abstract class ZeroMovementPacketFilterMixin {

    @Shadow public abstract short getXa();
    @Shadow public abstract short getYa();
    @Shadow public abstract short getZa();
    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();

    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;

    @Unique
    private static volatile int skipInterval = 3;

    @Unique
    private int akiasync$zeroMoveCounter = 0;

    @Unique
    private static volatile long totalPackets = 0;
    @Unique
    private static volatile long filteredPackets = 0;

    @Inject(
        method = "write(Lnet/minecraft/network/FriendlyByteBuf;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void filterZeroMovement(net.minecraft.network.FriendlyByteBuf buf, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }

        if (!enabled) {
            return;
        }

        totalPackets++;

        try {
            short xa = this.getXa();
            short ya = this.getYa();
            short za = this.getZa();

            float yRot = this.getYRot();
            float xRot = this.getXRot();

            boolean isZeroMove = (xa == 0 && ya == 0 && za == 0);

            boolean hasRotation = (Math.abs(yRot) > 0.1f || Math.abs(xRot) > 0.1f);

            if (isZeroMove && !hasRotation) {
                akiasync$zeroMoveCounter++;

                if (akiasync$zeroMoveCounter < skipInterval) {
                    filteredPackets++;
                    ci.cancel();
                    return;
                }

                akiasync$zeroMoveCounter = 0;
            } else {
                akiasync$zeroMoveCounter = 0;
            }

        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ZeroMovementPacketFilter", "write", e);
        }
    }

    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isSkipZeroMovementPacketsEnabled();
                skipInterval = 3;

                bridge.debugLog("[ZeroMovementPacketFilter] Initialized - enabled=%s, skipInterval=%d",
                    enabled, skipInterval);

                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ZeroMovementPacketFilter", "init", e);
        }
    }

    @Unique
    private static String akiasync$getStatistics() {
        if (totalPackets == 0) {
            return "ZeroMovementPacketFilter: No packets processed yet";
        }

        double filterRate = (double) filteredPackets / totalPackets * 100.0;

        return String.format(
            "ZeroMovementPacketFilter: total=%d, filtered=%d (%.2f%%)",
            totalPackets, filteredPackets, filterRate
        );
    }

    @Unique
    private static void akiasync$resetStatistics() {
        totalPackets = 0;
        filteredPackets = 0;
    }

    @Unique
    private static void akiasync$reload() {
        initialized = false;
    }
}
