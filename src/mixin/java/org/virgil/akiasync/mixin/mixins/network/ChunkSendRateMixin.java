package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.UUID;

@Mixin(value = ChunkMap.class, priority = 900)
public abstract class ChunkSendRateMixin {

    @Inject(
        method = "move",
        at = @At("HEAD"),
        cancellable = false
    )
    private void aki$onPlayerMove(ServerPlayer player, CallbackInfo ci) {
        try {

            Bridge bridge = BridgeManager.getBridge();
            if (bridge == null || !bridge.isNetworkOptimizationEnabled()) {
                return;
            }

            if (!bridge.isChunkRateControlEnabled()) {
                return;
            }

            UUID playerId = player.getUUID();
            
            if (bridge.isPlayerUsingViaVersion(playerId)) {
                if (!bridge.isViaConnectionInPlayState(playerId)) {
                    return;
                }
            }

            bridge.updatePlayerChunkLocation(player);

            int currentRate = bridge.calculatePlayerChunkSendRate(playerId);

            net.minecraft.world.level.ChunkPos playerChunkPos = player.chunkPosition();

            double priority = bridge.calculateChunkPriority(playerId, playerChunkPos.x, playerChunkPos.z);
            boolean isInViewDirection = bridge.isChunkInPlayerViewDirection(playerId, playerChunkPos.x, playerChunkPos.z);

            bridge.recordPlayerChunkSent(playerId, isInViewDirection);

            if (bridge.isDebugLoggingEnabled()) {

                int congestionLevel = bridge.detectPlayerCongestion(playerId);
                String congestionName = switch (congestionLevel) {
                    case 0 -> "NONE";
                    case 1 -> "LOW";
                    case 2 -> "MEDIUM";
                    case 3 -> "HIGH";
                    case 4 -> "SEVERE";
                    default -> "UNKNOWN";
                };

                bridge.debugLog(
                    "[ChunkSend] Player: %s, Chunk: (%d, %d), Rate: %d, Priority: %.2f, InView: %s, Congestion: %s",
                    playerId.toString(),
                    playerChunkPos.x, playerChunkPos.z,
                    currentRate,
                    priority,
                    isInViewDirection,
                    congestionName
                );
            }

        } catch (Exception e) {

            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.errorLog("[ChunkSend] Error in chunk send rate control: %s", e.getMessage());
            }
        }
    }
}
