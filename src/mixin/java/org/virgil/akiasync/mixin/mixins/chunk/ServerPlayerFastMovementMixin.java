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

            if (debugEnabled && bridge != null) {
                bridge.debugLog(
                    "[FastChunk-Adjust] Player %s: level=%s, prediction=%dticks, distance=%dchunks, concurrent=%d",
                    self.getScoreboardName(),
                    speedLevel,
                    dynamicPredictionTicks,
                    dynamicPreloadDistance,
                    dynamicMaxLoads
                );
            }

            double[] predictedPos = aki$movementData.predictPosition(dynamicPredictionTicks);

            Set<ChunkPos> chunksToLoad = aki$calculateChunksToLoad(self, predictedPos, aki$movementData, dynamicPreloadDistance);

            if (chunksToLoad.isEmpty()) {
                return;
            }

            final ServerPlayer finalPlayer = self;
            final double finalSpeed = speed;
            final int totalChunks = chunksToLoad.size();
            final String finalSpeedLevel = speedLevel;

            if (debugEnabled && bridge != null) {
                bridge.debugLog(
                    "[FastChunk-Load] Player %s: speed=%.3f (%s), loading %d chunks",
                    self.getScoreboardName(),
                    speed,
                    speedLevel,
                    totalChunks
                );
            }
            if (bridge != null) {

                if (isFolia) {

                    aki$scheduleFoliaChunkLoad(finalPlayer, chunksToLoad, finalSpeed, totalChunks, bridge, dynamicMaxLoads);
                } else {

                    final int finalMaxLoads = dynamicMaxLoads;
                    bridge.safeExecute(() -> {
                    try {
                        int loaded = 0;
                        for (ChunkPos chunkPos : chunksToLoad) {
                            if (loaded >= finalMaxLoads) {
                                break;
                            }
                            if (aki$shouldLoadChunk(finalPlayer, chunkPos)) {

                                aki$loadChunkAsync(finalPlayer, chunkPos, finalSpeed);
                                loaded++;
                            }
                        }

                        if (debugEnabled) {
                            final int finalLoaded = loaded;
                            bridge.debugLog(
                                "[FastChunk-Result] Player %s: speed=%.3f (%s), loaded %d/%d chunks",
                                finalPlayer.getScoreboardName(),
                                finalSpeed,
                                finalSpeedLevel,
                                finalLoaded,
                                totalChunks
                            );
                        }
                    } catch (Exception ex) {
                        if (debugEnabled) {
                            bridge.errorLog("[FastChunk] Async chunk loading error: %s", ex.getMessage());
                        }
                    }
                    }, "FastMovementChunkLoad");
                }
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

                if (isFolia) {
                    bridge.debugLog("[FastChunk] Initialized in Folia mode with region safety checks");
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
    private Set<ChunkPos> aki$calculateChunksToLoad(ServerPlayer player, double[] predictedPos, PlayerMovementData data, int dynamicPreloadDistance) {
        Set<ChunkPos> chunks = new HashSet<>();

        ChunkPos currentChunk = player.chunkPosition();
        int currentChunkX = currentChunk.x;
        int currentChunkZ = currentChunk.z;

        int predictedChunkX = (int) predictedPos[0] >> 4;
        int predictedChunkZ = (int) predictedPos[2] >> 4;

        double dirX = predictedChunkX - currentChunkX;
        double dirZ = predictedChunkZ - currentChunkZ;
        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);

        if (length > 0) {
            dirX /= length;
            dirZ /= length;
        }

        int nearRadius = Math.min(4, dynamicPreloadDistance / 2);
        for (int dx = -nearRadius; dx <= nearRadius; dx++) {
            for (int dz = -nearRadius; dz <= nearRadius; dz++) {

                if (dx * dx + dz * dz <= nearRadius * nearRadius) {
                    chunks.add(new ChunkPos(currentChunkX + dx, currentChunkZ + dz));
                }
            }
        }

        if (length > 0) {
            for (int dist = 1; dist <= dynamicPreloadDistance; dist++) {
                int centerX = currentChunkX + (int)(dirX * dist);
                int centerZ = currentChunkZ + (int)(dirZ * dist);

                int width = Math.max(2, dist * 2 / 3);

                for (int dx = -width; dx <= width; dx++) {
                    for (int dz = -width; dz <= width; dz++) {

                        double normalizedDist = Math.sqrt(dx * dx + dz * dz);
                        if (normalizedDist <= width) {
                            chunks.add(new ChunkPos(centerX + dx, centerZ + dz));
                        }
                    }
                }
            }
        }

        return chunks;
    }

    @Unique
    private boolean aki$shouldLoadChunk(ServerPlayer player, ChunkPos chunkPos) {

        int viewDistance = player.getServer().getPlayerList().getViewDistance();
        ChunkPos playerChunk = player.chunkPosition();

        int dx = Math.abs(chunkPos.x - playerChunk.x);
        int dz = Math.abs(chunkPos.z - playerChunk.z);

        return dx <= viewDistance + preloadDistance && dz <= viewDistance + preloadDistance;
    }

    @Unique
    private void aki$scheduleFoliaChunkLoad(ServerPlayer player, Set<ChunkPos> chunksToLoad,
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

    @Unique
    private void aki$loadChunkAsync(ServerPlayer player, ChunkPos chunkPos, double speed) {
        try {

            net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();

            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {

                    level.getChunk(chunkPos.x, chunkPos.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);

                    if (debugEnabled) {
                        Bridge bridge = BridgeManager.getBridge();
                        if (bridge != null) {
                            bridge.debugLog(
                                "[FastChunk] Loaded chunk (%d, %d) for player %s (speed: %.2f)",
                                chunkPos.x, chunkPos.z,
                                player.getScoreboardName(),
                                speed
                            );
                        }
                    }
                } catch (Exception ex) {
                    if (debugEnabled) {
                        Bridge bridge = BridgeManager.getBridge();
                        if (bridge != null) {
                            bridge.errorLog(
                                "[FastChunk] Failed to load chunk (%d, %d): %s",
                                chunkPos.x, chunkPos.z,
                                ex.getMessage()
                            );
                        }
                    }
                }
            });

        } catch (Exception ex) {
            if (debugEnabled) {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.errorLog(
                        "[FastChunk] Failed to start async chunk load (%d, %d): %s",
                        chunkPos.x, chunkPos.z,
                        ex.getMessage()
                    );
                }
            }
        }
    }
}
