package org.virgil.akiasync.mixin.mixins.network.packet;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
@Mixin(ServerCommonPacketListenerImpl.class)
public class MtuAwareBatchingMixin {

    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile int mtuLimit = 1396;
    @Unique
    private static volatile int hardCapPackets = 4096;

    @Unique
    private final AtomicInteger bufferedPacketCount = new AtomicInteger(0);

    @Unique
    private static final AtomicLong totalPackets = new AtomicLong(0);
    @Unique
    private static final AtomicLong batchedPackets = new AtomicLong(0);
    @Unique
    private static final AtomicLong mtuFlushes = new AtomicLong(0);
    @Unique
    private static final AtomicLong tickFlushes = new AtomicLong(0);

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void akiasync$mtuAwareBatching(Packet<?> packet, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }

        if (!enabled) {
            return;
        }

        if (!((Object) this instanceof ServerGamePacketListenerImpl)) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge == null) {
                return;
            }

            totalPackets.incrementAndGet();

            Object connection = bridge.getConnectionFromListener(this);
            if (connection == null) {
                return;
            }

            if (akiasync$shouldSendImmediately(packet)) {
                akiasync$flushBuffer(bridge, connection, "Instant");
                return;
            }

            long pendingBytes = bridge.getConnectionPendingBytes(connection);
            if (pendingBytes > mtuLimit) {
                akiasync$flushBuffer(bridge, connection, "MTU");
                mtuFlushes.incrementAndGet();
            }

            bridge.sendPacketWithoutFlush(connection, packet);
            batchedPackets.incrementAndGet();

            int count = bufferedPacketCount.incrementAndGet();
            if (count >= hardCapPackets) {
                akiasync$flushBuffer(bridge, connection, "HardCap");
            }

            ci.cancel();

        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "MtuAwareBatching", "mtuAwareBatching", e);
        }
    }

    @Unique
    private void akiasync$flushBuffer(org.virgil.akiasync.mixin.bridge.Bridge bridge, Object connection, String reason) {
        if (bufferedPacketCount.get() == 0) {
            return;
        }

        try {
            bridge.flushConnection(connection);
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "MtuAwareBatching", "flushBuffer", e);
        }

        bufferedPacketCount.set(0);

        if ("Tick".equals(reason)) {
            tickFlushes.incrementAndGet();
        }
    }

    @Unique
    private void akiasync$tickFlush() {
        if (!enabled || bufferedPacketCount.get() == 0) {
            return;
        }
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                Object connection = bridge.getConnectionFromListener(this);
                if (connection != null) {
                    akiasync$flushBuffer(bridge, connection, "Tick");
                }
            }
        } catch (Exception e) {

        }
    }

    @Unique
    private boolean akiasync$shouldSendImmediately(Packet<?> packet) {

        if (packet instanceof ClientboundPlayerChatPacket ||
            packet instanceof ClientboundSystemChatPacket ||
            packet instanceof ClientboundLoginPacket ||
            packet instanceof ClientboundPlayerAbilitiesPacket ||
            packet instanceof ClientboundRespawnPacket ||
            packet instanceof ClientboundSetHealthPacket ||
            packet instanceof ClientboundPlayerCombatKillPacket) {
            return true;
        }

        String simpleName = packet.getClass().getSimpleName();
        return simpleName.contains("KeepAlive") ||
               simpleName.contains("Disconnect") ||
               simpleName.contains("Configuration") ||
               simpleName.contains("Handshake");
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
                enabled = bridge.isMtuAwareBatchingEnabled();
                mtuLimit = bridge.getMtuLimit();
                hardCapPackets = bridge.getMtuHardCapPackets();

                bridge.debugLog("[MtuAwareBatching] Initialized: enabled=%s, mtuLimit=%d, hardCap=%d",
                    enabled, mtuLimit, hardCapPackets);

                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "MtuAwareBatching", "init", e);
        }
    }

    @Unique
    private static boolean akiasync$isEnabled() {
        if (!initialized) {
            akiasync$init();
        }
        return enabled;
    }

    @Unique
    private static String akiasync$getStatistics() {
        long total = totalPackets.get();
        long batched = batchedPackets.get();
        long mtuF = mtuFlushes.get();
        long tickF = tickFlushes.get();

        if (total == 0) {
            return "MtuAwareBatching: No packets processed";
        }

        double batchRate = (double) batched / total * 100;
        long totalFlushes = mtuF + tickF;
        double avgBatchSize = totalFlushes > 0 ? (double) batched / totalFlushes : 0;

        return String.format(
            "MtuAwareBatching: enabled=%s, total=%d, batched=%d (%.1f%%), flushes=%d (mtu=%d, tick=%d), avgBatch=%.1f",
            enabled, total, batched, batchRate, totalFlushes, mtuF, tickF, avgBatchSize
        );
    }

    @Unique
    private static void akiasync$resetStatistics() {
        totalPackets.set(0);
        batchedPackets.set(0);
        mtuFlushes.set(0);
        tickFlushes.set(0);
    }

    @Unique
    private static void akiasync$reload() {
        initialized = false;
    }
}
