package org.virgil.akiasync.mixin.mixins.network.chunk;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
@Mixin(PlayerChunkSender.class)
public class DynamicChunkSendRateMixin {

    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile long limitUploadBandwidth = 10240;
    @Unique
    private static volatile long guaranteedBandwidth = 512;
    @Unique
    private static volatile float minChunksPerTick = 1.0f;
    @Unique
    private static volatile float maxChunksPerTick = 16.0f;

    @Unique
    private static final Map<UUID, AtomicLong> playerTraffic = new ConcurrentHashMap<>();
    @Unique
    private static volatile long lastResetTime = System.currentTimeMillis();
    @Unique
    private static volatile long totalOutgoingBytes = 0;

    @Unique
    private float akiasync$adjustedChunksPerTick = 9.0f;

    @Inject(method = "sendNextChunks", at = @At("HEAD"))
    private void adjustChunkSendRate(ServerPlayer player, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }

        if (!enabled || player == null) {
            return;
        }

        try {
            long now = System.currentTimeMillis();
            if (now - lastResetTime >= 1000) {
                akiasync$resetTraffic();
                lastResetTime = now;
            }

            UUID playerId = player.getUUID();
            AtomicLong traffic = playerTraffic.computeIfAbsent(playerId, k -> new AtomicLong(0));

            long playerBytes = traffic.get() >> 10;
            long totalBytes = totalOutgoingBytes >> 10;

            if (totalBytes >= limitUploadBandwidth && playerBytes >= guaranteedBandwidth) {
                akiasync$adjustedChunksPerTick = Math.max(minChunksPerTick, akiasync$adjustedChunksPerTick * 0.8f);
            } else if (totalBytes < limitUploadBandwidth * 0.7) {
                akiasync$adjustedChunksPerTick = Math.min(maxChunksPerTick, akiasync$adjustedChunksPerTick * 1.1f);
            }

        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "DynamicChunkSendRate", "adjustChunkSendRate", e);
        }
    }

    @Unique
    private static void akiasync$resetTraffic() {
        totalOutgoingBytes = 0;
        for (AtomicLong traffic : playerTraffic.values()) {
            traffic.set(0);
        }
    }

    @Unique
    private static void akiasync$recordTraffic(UUID playerId, long bytes) {
        AtomicLong traffic = playerTraffic.computeIfAbsent(playerId, k -> new AtomicLong(0));
        traffic.addAndGet(bytes);
        totalOutgoingBytes += bytes;
    }

    @Unique
    private static void akiasync$onPlayerQuit(UUID playerId) {
        playerTraffic.remove(playerId);
    }

    @Unique
    private static void akiasync$init() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isDynamicChunkSendRateEnabled();
                limitUploadBandwidth = bridge.getDynamicChunkLimitBandwidth();
                guaranteedBandwidth = bridge.getDynamicChunkGuaranteedBandwidth();

                bridge.debugLog("[DynamicChunkSendRate] Initialized: enabled=%s, limit=%dKB/s, guaranteed=%dKB/s",
                    enabled, limitUploadBandwidth, guaranteedBandwidth);

                    initialized = true;
                }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "DynamicChunkSendRate", "init", e);
        }
    }
}
