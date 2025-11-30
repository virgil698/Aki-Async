package org.virgil.akiasync.mixin.mixins.network;

import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@Mixin(value = RegionizedPlayerChunkLoader.PlayerChunkLoaderData.class, remap = false)
public class RegionizedPlayerChunkLoaderMixin {

    @Shadow(remap = false)
    private ServerPlayer player;

    private boolean aki$isPlayerFastMoving() {
        if (player == null) return false;

        if (player.isFallFlying()) return true;

        if (player.getAbilities().flying) return true;

        if (player.isSprinting()) return true;

        if (player.isPassenger()) return true;

        return false;
    }

    @Inject(
        method = "getMaxChunkSendRate",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void aki$adjustChunkSendRate(CallbackInfoReturnable<Double> cir) {
        try {

            Bridge bridge = BridgeManager.getBridge();
            if (bridge == null || !bridge.isNetworkOptimizationEnabled()) {
                return;
            }

            if (!bridge.isChunkSendOptimizationEnabled()) {
                return;
            }

            double originalRate = cir.getReturnValue();

            if (this.player == null) {
                return;
            }

            int congestionLevel = bridge.getPlayerCongestionLevel(this.player.getUUID());

            boolean isFastMoving = aki$isPlayerFastMoving();

            double adjustedRate = originalRate;

            if (isFastMoving) {
                adjustedRate = Math.max(originalRate, 10.0);
            }

            switch (congestionLevel) {
                case 0:
                    
                    if (isFastMoving) {
                        adjustedRate = Math.min(adjustedRate * 1.5, 25.0);
                    } else {
                        adjustedRate = Math.min(adjustedRate * 1.2, 20.0);
                    }
                    break;
                case 1:
                    
                    if (isFastMoving) {
                        adjustedRate = Math.min(adjustedRate * 1.2, 20.0);
                    }
                    break;
                case 2:
                    
                    if (isFastMoving) {
                        adjustedRate = adjustedRate * 0.95;  
                    } else {
                        adjustedRate = adjustedRate * 0.85;  
                    }
                    break;
                case 3:
                    
                    if (isFastMoving) {
                        adjustedRate = adjustedRate * 0.85;  
                    } else {
                        adjustedRate = adjustedRate * 0.7;   
                    }
                    break;
                case 4:
                    
                    if (isFastMoving) {
                        adjustedRate = adjustedRate * 0.7;   
                    } else {
                        adjustedRate = adjustedRate * 0.5;
                    }
                    break;
                default:
                    break;
            }

            double maxRate = isFastMoving ? 25.0 : 20.0;
            adjustedRate = Math.max(1.0, Math.min(adjustedRate, maxRate));

            if (adjustedRate != originalRate) {
                cir.setReturnValue(adjustedRate);

                if (bridge.isDebugLoggingEnabled()) {
                    String levelName = switch (congestionLevel) {
                        case 0 -> "NONE";
                        case 1 -> "LOW";
                        case 2 -> "MEDIUM";
                        case 3 -> "HIGH";
                        case 4 -> "SEVERE";
                        default -> "UNKNOWN";
                    };

                    bridge.debugLog(
                        "[ChunkSend] Player %s: Adjusted chunk send rate from %.2f to %.2f (congestion: %s, fastMoving: %s)",
                        this.player.getScoreboardName(),
                        originalRate,
                        adjustedRate,
                        levelName,
                        isFastMoving
                    );
                }
            }

        } catch (Exception e) {

            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.errorLog("[ChunkSend] Error adjusting chunk send rate: %s", e.getMessage());
            }
        }
    }
}
