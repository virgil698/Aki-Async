package org.virgil.akiasync.mixin.mixins.chunk;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.PlayerMovementData;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.*;

@Mixin(value = ServerPlayer.class, priority = 900)
public abstract class ServerPlayerFastMovementMixin {

    @Unique private static volatile boolean enabled = false;
    @Unique private static volatile double speedThreshold = 0.5;
    @Unique private static volatile int preloadDistance = 8;
    @Unique private static volatile int maxConcurrentLoads = 4;
    @Unique private static volatile int predictionTicks = 40;
    @Unique private static volatile boolean debugEnabled = false;
    @Unique private static volatile boolean initialized = false;
    @Unique private static volatile boolean isFolia = false;
    
    @Unique private static volatile boolean centerOffsetEnabled = false;
    @Unique private static volatile double minOffsetSpeed = 3.0;  
    @Unique private static volatile double maxOffsetSpeed = 9.0;  
    @Unique private static volatile double maxOffsetRatio = 0.75; 

    @Unique private PlayerMovementData aki$movementData;
    @Unique private Vec3 aki$lastPosition;
    @Unique private long aki$lastCheckTime = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void aki$checkFastMovement(CallbackInfo ci) {
        try {

            if (!initialized) {
                aki$initConfig();
            }

            if (!enabled) {
                return;
            }

            ServerPlayer self = (ServerPlayer) (Object) this;

            if (self.level().isClientSide) {
                return;
            }

            long currentTime = self.level().getGameTime();

            boolean potentiallyFast = aki$isInFastMovementState(self);
            int checkInterval = potentiallyFast ? 1 : 3;

            if (currentTime - aki$lastCheckTime < checkInterval) {
                return;
            }
            aki$lastCheckTime = currentTime;

            if (!aki$isInFastMovementState(self)) {
                return;
            }

            if (aki$movementData == null) {
                aki$movementData = new PlayerMovementData();
            }

            Vec3 currentPos = self.position();
            aki$movementData.updatePosition(currentPos.x, currentPos.y, currentPos.z, currentTime);

            double speed = aki$movementData.getSpeed();
            
            Bridge bridge = BridgeManager.getBridge();
            if (debugEnabled && bridge != null) {
                bridge.debugLog(
                    "[FastChunk-Speed] Player %s: speed=%.3f b/t, threshold=%.2f, state=%s",
                    self.getScoreboardName(),
                    speed,
                    speedThreshold,
                    speed >= speedThreshold ? "TRIGGERED" : "BELOW"
                );
            }

            if (speed < speedThreshold) {
                return;
            }
            
            if (bridge != null) {
                aki$processChunkLoadingAsync(self, speed, bridge);
            }

        } catch (Exception e) {

            if (debugEnabled) {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.errorLog("[FastChunk] Error in fast movement detection: %s", e.getMessage());
                }
            }
        }
    }

    @Unique
    private void aki$processChunkLoadingAsync(ServerPlayer player, double speed, Bridge bridge) {
        
        bridge.safeExecute(() -> {
            try {
                
                aki$doChunkLoading(player, speed, bridge);
            } catch (Exception e) {
                if (debugEnabled) {
                    bridge.errorLog("[FastChunk] Async processing error: %s", e.getMessage());
                }
            }
        }, "FastChunkLoadingAsync");
    }
    
    @Unique
    private void aki$doChunkLoading(ServerPlayer player, double speed, Bridge bridge) {
        
        int dynamicPredictionTicks = predictionTicks;
        int dynamicPreloadDistance = preloadDistance;
        int dynamicMaxLoads = maxConcurrentLoads;
        String speedLevel = "NORMAL";

        if (speed > 1.5) {
            dynamicPredictionTicks = (int)(predictionTicks * 1.5);
            dynamicPreloadDistance = (int)(preloadDistance * 1.5);
            dynamicMaxLoads = maxConcurrentLoads * 3;
            speedLevel = "ULTRA_FAST";
        } else if (speed > 1.0) {
            dynamicPredictionTicks = (int)(predictionTicks * 1.2);
            dynamicPreloadDistance = (int)(preloadDistance * 1.2);
            dynamicMaxLoads = maxConcurrentLoads * 2;
            speedLevel = "FAST";
        }

        if (debugEnabled) {
            bridge.debugLog(
                "[FastChunk-Adjust] Player %s: level=%s, prediction=%dticks, distance=%dchunks, concurrent=%d",
                player.getScoreboardName(),
                speedLevel,
                dynamicPredictionTicks,
                dynamicPreloadDistance,
                dynamicMaxLoads
            );
        }

        double[] predictedPos = aki$movementData.predictPosition(dynamicPredictionTicks);
        
        ChunkPos loadCenter = aki$calculateLoadCenter(player, speed, aki$movementData, dynamicPreloadDistance);

        Set<ChunkPos> chunksToLoad = aki$calculateChunksToLoad(player, predictedPos, aki$movementData, dynamicPreloadDistance, loadCenter);

        if (chunksToLoad.isEmpty()) {
            return;
        }
        
        java.util.List<ChunkPos> sortedChunks = new java.util.ArrayList<>(chunksToLoad);
        sortedChunks.sort((a, b) -> {
            int distA = (a.x - loadCenter.x) * (a.x - loadCenter.x) + (a.z - loadCenter.z) * (a.z - loadCenter.z);
            int distB = (b.x - loadCenter.x) * (b.x - loadCenter.x) + (b.z - loadCenter.z) * (b.z - loadCenter.z);
            return Integer.compare(distA, distB);
        });
        
        int adjustedMaxLoads = dynamicMaxLoads;

        if (debugEnabled) {
            bridge.debugLog(
                "[FastChunk-Load] Player %s: speed=%.3f (%s), loading %d chunks (async)",
                player.getScoreboardName(),
                speed,
                speedLevel,
                sortedChunks.size()
            );
        }

        int submitted = 0;
        int priority = 100; 
        
        for (int i = 0; i < sortedChunks.size() && submitted < adjustedMaxLoads; i++) {
            ChunkPos chunkPos = sortedChunks.get(i);
            if (aki$shouldLoadChunk(player, chunkPos)) {
                
                int chunkPriority = priority - i; 
                bridge.submitChunkLoad(player, chunkPos, chunkPriority, speed);
                submitted++;
            }
        }

        if (debugEnabled) {
            bridge.debugLog(
                "[FastChunk-Result] Player %s: submitted %d/%d chunks to scheduler",
                player.getScoreboardName(),
                submitted,
                sortedChunks.size()
            );
        }
    }

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void aki$onPlayerDisconnect(CallbackInfo ci) {
        aki$movementData = null;
    }

    @Unique
    private static void aki$initConfig() {
        try {

            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }

            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                enabled = bridge.isFastMovementChunkLoadEnabled();
                speedThreshold = bridge.getFastMovementSpeedThreshold();
                preloadDistance = bridge.getFastMovementPreloadDistance();
                maxConcurrentLoads = bridge.getFastMovementMaxConcurrentLoads();
                predictionTicks = bridge.getFastMovementPredictionTicks();
                debugEnabled = false;
                
                centerOffsetEnabled = bridge.isCenterOffsetEnabled();
                minOffsetSpeed = bridge.getMinOffsetSpeed();
                maxOffsetSpeed = bridge.getMaxOffsetSpeed();
                maxOffsetRatio = bridge.getMaxOffsetRatio();

                if (isFolia) {
                    bridge.debugLog("[FastChunk] Initialized in Folia mode with region safety checks");
                }
                if (centerOffsetEnabled) {
                    bridge.debugLog("[FastChunk] Center offset enabled: speed range %.1f-%.1f b/t, max offset ratio %.1f%%",
                        minOffsetSpeed, maxOffsetSpeed, maxOffsetRatio * 100);
                }
            }
            initialized = true;
        } catch (Exception e) {

        }
    }

    @Unique
    private boolean aki$isInFastMovementState(ServerPlayer player) {

        if (player.isFallFlying()) {
            return true;
        }

        if (player.getAbilities().flying) {
            return true;
        }

        if (player.isSprinting()) {
            return true;
        }

        if (player.isPassenger()) {
            return true;
        }

        return false;
    }

    @Unique
    private ChunkPos aki$calculateLoadCenter(ServerPlayer player, double speed, PlayerMovementData data, int dynamicPreloadDistance) {
        ChunkPos currentChunk = player.chunkPosition();
        
        if (!centerOffsetEnabled || speed < minOffsetSpeed || speed > maxOffsetSpeed) {
            return currentChunk;
        }
        
        double[] velocity = data.getVelocity();
        if (velocity == null) {
            return currentChunk;
        }
        
        double velX = velocity[0];
        double velZ = velocity[2];
        double velLength = Math.sqrt(velX * velX + velZ * velZ);
        
        if (velLength < 0.1) {
            return currentChunk;
        }
        
        double dirX = velX / velLength;
        double dirZ = velZ / velLength;  
        
        int viewDistance = ((net.minecraft.server.level.ServerLevel)player.level()).getServer().getPlayerList().getViewDistance();
        double maxOffsetChunks = viewDistance * maxOffsetRatio;
        
        double speedRatio = Math.min(1.0, (speed - minOffsetSpeed) / (maxOffsetSpeed - minOffsetSpeed));
        double offsetDistance = maxOffsetChunks * speedRatio;
        
        int offsetX = (int) Math.round(dirX * offsetDistance);
        int offsetZ = (int) Math.round(dirZ * offsetDistance);
        
        ChunkPos offsetCenter = new ChunkPos(currentChunk.x + offsetX, currentChunk.z + offsetZ);
        
        if (debugEnabled) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog(
                    "[FastChunk-Offset] Player %s: speed=%.2f b/t, offset=(%d, %d) chunks, ratio=%.1f%%, center=(%d, %d)",
                    player.getScoreboardName(),
                    speed,
                    offsetX, offsetZ,
                    speedRatio * 100,
                    offsetCenter.x, offsetCenter.z
                );
            }
        }
        
        return offsetCenter;
    }
    
    @Unique
    private Set<ChunkPos> aki$calculateChunksToLoad(ServerPlayer player, double[] predictedPos, PlayerMovementData data, int dynamicPreloadDistance, ChunkPos loadCenter) {
        Set<ChunkPos> chunks = new HashSet<>();

        ChunkPos currentChunk = player.chunkPosition();
        
        int centerChunkX = loadCenter.x;
        int centerChunkZ = loadCenter.z;

        double[] velocity = data.getVelocity();
        if (velocity == null) {
            return chunks;
        }
        
        double velX = velocity[0];
        double velZ = velocity[2];
        double velLength = Math.sqrt(velX * velX + velZ * velZ);
        
        if (velLength < 0.1) {
            return chunks;
        }
        
        double dirX = velX / velLength;
        double dirZ = velZ / velLength;

        int nearRadius = 2;
        for (int dx = -nearRadius; dx <= nearRadius; dx++) {
            for (int dz = -nearRadius; dz <= nearRadius; dz++) {
                if (dx * dx + dz * dz <= nearRadius * nearRadius) {
                    chunks.add(new ChunkPos(centerChunkX + dx, centerChunkZ + dz));
                }
            }
        }

        for (int dist = 1; dist <= dynamicPreloadDistance; dist++) {
            int targetX = centerChunkX + (int)(dirX * dist);
            int targetZ = centerChunkZ + (int)(dirZ * dist);

            int width;
            if (dist <= 3) {
                width = 3;  
            } else if (dist <= 6) {
                width = 2;  
            } else {
                width = 1;  
            }

            for (int dx = -width; dx <= width; dx++) {
                for (int dz = -width; dz <= width; dz++) {
                    
                    double pointX = dx;
                    double pointZ = dz;
                    double pointLength = Math.sqrt(pointX * pointX + pointZ * pointZ);
                    
                    if (pointLength <= width) {
                        
                        if (pointLength < 0.1) {
                            
                            chunks.add(new ChunkPos(targetX + dx, targetZ + dz));
                        } else {
                            
                            double dotProduct = (pointX * dirX + pointZ * dirZ) / pointLength;
                            
                            if (dotProduct > 0) {
                                chunks.add(new ChunkPos(targetX + dx, targetZ + dz));
                            }
                        }
                    }
                }
            }
        }

        return chunks;
    }

    @Unique
    private boolean aki$shouldLoadChunk(ServerPlayer player, ChunkPos chunkPos) {

        int viewDistance = ((net.minecraft.server.level.ServerLevel)player.level()).getServer().getPlayerList().getViewDistance();
        ChunkPos playerChunk = player.chunkPosition();

        int dx = Math.abs(chunkPos.x - playerChunk.x);
        int dz = Math.abs(chunkPos.z - playerChunk.z);

        return dx <= viewDistance + preloadDistance && dz <= viewDistance + preloadDistance;
    }

    @Unique
    private void aki$scheduleFoliaChunkLoad(ServerPlayer player, java.util.List<ChunkPos> chunksToLoad,
                                            double speed, int totalChunks, Bridge bridge, int maxLoads) {
        try {

            net.minecraft.core.BlockPos playerPos = player.blockPosition();
            net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();

            bridge.scheduleRegionTask(level, playerPos, () -> {
                try {
                    int loaded = 0;
                    for (ChunkPos chunkPos : chunksToLoad) {
                        if (loaded >= maxLoads) {
                            break;
                        }

                        net.minecraft.core.BlockPos chunkBlockPos = new net.minecraft.core.BlockPos(
                            chunkPos.x << 4, player.getBlockY(), chunkPos.z << 4
                        );

                        if (bridge.canAccessBlockPosDirectly(level, chunkBlockPos)) {
                            if (aki$shouldLoadChunk(player, chunkPos)) {
                                aki$loadChunkSync(player, chunkPos, speed);
                                loaded++;
                            }
                        }
                    }

                    if (debugEnabled) {
                        final int finalLoaded = loaded;
                        bridge.debugLog(
                            "[FastChunk-Folia] Player %s moving at %.2f blocks/tick, loaded %d/%d chunks",
                            player.getScoreboardName(),
                            speed,
                            finalLoaded,
                            totalChunks
                        );
                    }
                } catch (Exception ex) {
                    if (debugEnabled) {
                        bridge.errorLog("[FastChunk-Folia] Region chunk loading error: %s", ex.getMessage());
                    }
                }
            });
        } catch (Exception ex) {
            if (debugEnabled) {
                bridge.errorLog("[FastChunk-Folia] Failed to schedule region task: %s", ex.getMessage());
            }
        }
    }

    @Unique
    private void aki$loadChunkSync(ServerPlayer player, ChunkPos chunkPos, double speed) {
        try {
            net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
            level.getChunk(chunkPos.x, chunkPos.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);

            if (debugEnabled) {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog(
                        "[FastChunk] Sync loaded chunk (%d, %d) for player %s",
                        chunkPos.x, chunkPos.z,
                        player.getScoreboardName()
                    );
                }
            }
        } catch (Exception ex) {
            if (debugEnabled) {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.errorLog(
                        "[FastChunk] Failed to sync load chunk (%d, %d): %s",
                        chunkPos.x, chunkPos.z,
                        ex.getMessage()
                    );
                }
            }
        }
    }
}
