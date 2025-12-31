package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.PlayerChunkStats;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@SuppressWarnings("unused")
@Mixin(value = PlayerChunkSender.class, priority = 1100) 
public class ChunkBatchOptimizationMixin {
    
    @Shadow
    private float desiredChunksPerTick;
    
    @Shadow
    private int unacknowledgedBatches;
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile float adaptiveMinChunks = 2.0f;
    @Unique
    private static volatile float adaptiveMaxChunks = 32.0f;
    @Unique
    private static volatile int maxUnackedBatches = 8;
    @Unique
    private static volatile boolean prioritizeNearbyChunks = true;
    
    @Unique
    private static final Map<UUID, PlayerChunkStats> playerStats = new ConcurrentHashMap<>();
    
    @Unique
    private static final AtomicLong totalChunksSent = new AtomicLong(0);
    @Unique
    private static final AtomicLong totalBatchesSent = new AtomicLong(0);
    
    
    @Inject(method = "sendNextChunks", at = @At("RETURN"), require = 0)
    private void optimizeChunkBatch(ServerPlayer player, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled || player == null) {
            return;
        }
        
        try {
            UUID playerId = player.getUUID();
            PlayerChunkStats stats = playerStats.computeIfAbsent(playerId, k -> new PlayerChunkStats());
            
            int ping = player.connection.latency();
            
            
            float targetChunks = akiasync$calculateTargetChunks(ping, stats);
            
            
            if (Math.abs(desiredChunksPerTick - targetChunks) > 2.0f) {
                
                desiredChunksPerTick = Math.min(desiredChunksPerTick, targetChunks);
            }
            
            stats.lastPing = ping;
            stats.lastUpdateTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ChunkBatchOptimization", "sendNextChunks", e);
        }
    }
    
    @Inject(method = "onChunkBatchReceivedByClient", at = @At("RETURN"), require = 0)
    private void trackBatchAck(float desiredBatchSize, CallbackInfo ci) {
        if (!enabled) {
            return;
        }
        
        totalBatchesSent.incrementAndGet();
    }
    
    @Unique
    private float akiasync$calculateTargetChunks(int ping, PlayerChunkStats stats) {
        float base = 9.0f;
        
        
        if (ping < 50) {
            base = adaptiveMaxChunks;
        } else if (ping < 100) {
            base = adaptiveMaxChunks * 0.7f;
        } else if (ping < 200) {
            base = adaptiveMaxChunks * 0.4f;
        } else if (ping < 400) {
            base = adaptiveMinChunks * 2;
        } else {
            base = adaptiveMinChunks;
        }
        
        
        if (unacknowledgedBatches > maxUnackedBatches / 2) {
            base *= 0.7f;
        }
        
        return Math.max(adaptiveMinChunks, Math.min(adaptiveMaxChunks, base));
    }
    
    @Unique
    private static void akiasync$onPlayerQuit(UUID playerId) {
        playerStats.remove(playerId);
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
                enabled = bridge.isChunkBatchOptimizationEnabled();
                adaptiveMinChunks = bridge.getChunkBatchMinChunks();
                adaptiveMaxChunks = bridge.getChunkBatchMaxChunks();
                maxUnackedBatches = bridge.getChunkBatchMaxUnacked();
                
                bridge.debugLog("[ChunkBatchOptimization] Initialized: enabled=%s, min=%.1f, max=%.1f",
                    enabled, adaptiveMinChunks, adaptiveMaxChunks);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ChunkBatchOptimization", "init", e);
        }
        
        initialized = true;
    }
}
