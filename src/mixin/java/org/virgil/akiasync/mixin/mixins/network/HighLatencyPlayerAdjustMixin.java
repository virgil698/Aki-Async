package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings("unused")
@Mixin(ServerGamePacketListenerImpl.class)
public class HighLatencyPlayerAdjustMixin {
    
    @Shadow public ServerPlayer player;
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile int latencyThreshold = 150;
    @Unique
    private static volatile int minViewDistance = 5;
    @Unique
    private static volatile long highLatencyDurationMs = 60000;
    
    @Unique
    private static final Map<UUID, Long> highLatencyStartTime = new ConcurrentHashMap<>();
    @Unique
    private static final Map<UUID, Integer> originalViewDistance = new ConcurrentHashMap<>();
    @Unique
    private static final Map<UUID, Boolean> adjustedPlayers = new ConcurrentHashMap<>();
    
    @Unique
    private long lastCheckTime = 0;
    @Unique
    private static final long CHECK_INTERVAL_MS = 15000;
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void checkLatencyAndAdjust(CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled || player == null) {
            return;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckTime = now;
        
        try {
            UUID playerId = player.getUUID();
            int currentPing = player.connection.latency();
            
            if (currentPing >= latencyThreshold) {
                Long startTime = highLatencyStartTime.get(playerId);
                if (startTime == null) {
                    highLatencyStartTime.put(playerId, now);
                    return;
                }
                
                if ((now - startTime) >= highLatencyDurationMs) {
                    highLatencyStartTime.remove(playerId);
                    akiasync$adjustViewDistance(playerId);
                }
            } else {
                highLatencyStartTime.remove(playerId);
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "HighLatencyPlayerAdjust", "checkLatencyAndAdjust", e);
        }
    }
    
    @Unique
    private void akiasync$adjustViewDistance(UUID playerId) {
        if (adjustedPlayers.containsKey(playerId)) {
            int currentSendDistance = akiasync$getSendViewDistance();
            if (currentSendDistance > minViewDistance) {
                akiasync$setSendViewDistance(currentSendDistance - 1);
            }
            return;
        }
        
        int clientViewDistance = akiasync$getClientViewDistance();
        originalViewDistance.put(playerId, clientViewDistance);
        adjustedPlayers.put(playerId, true);
        
        int newDistance = Math.max(minViewDistance, clientViewDistance - 2);
        akiasync$setSendViewDistance(newDistance);
    }
    
    @Unique
    private int akiasync$getClientViewDistance() {
        try {
            return player.requestedViewDistance();
        } catch (Exception e) {
            return 10;
        }
    }
    
    @Unique
    private int akiasync$getSendViewDistance() {
        try {
            var level = (net.minecraft.server.level.ServerLevel) player.level();
            if (level != null) {
                return level.getChunkSource().chunkMap.serverViewDistance;
            }
        } catch (Exception e) {
        }
        return 10;
    }
    
    @Unique
    private void akiasync$setSendViewDistance(int distance) {
        try {
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "HighLatencyPlayerAdjust", "setSendViewDistance", e);
        }
    }
    
    @Unique
    private static void akiasync$onPlayerQuit(UUID playerId) {
        highLatencyStartTime.remove(playerId);
        originalViewDistance.remove(playerId);
        adjustedPlayers.remove(playerId);
    }
    
    @Unique
    private static void akiasync$resetPlayerViewDistance(UUID playerId) {
        Integer original = originalViewDistance.remove(playerId);
        adjustedPlayers.remove(playerId);
        highLatencyStartTime.remove(playerId);
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
                enabled = bridge.isHighLatencyAdjustEnabled();
                latencyThreshold = bridge.getHighLatencyThreshold();
                minViewDistance = bridge.getHighLatencyMinViewDistance();
                highLatencyDurationMs = bridge.getHighLatencyDurationMs();
                
                bridge.debugLog("[HighLatencyPlayerAdjust] Initialized: enabled=%s, threshold=%d, minVD=%d",
                    enabled, latencyThreshold, minViewDistance);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "HighLatencyPlayerAdjust", "init", e);
        }
        
        initialized = true;
    }
}
