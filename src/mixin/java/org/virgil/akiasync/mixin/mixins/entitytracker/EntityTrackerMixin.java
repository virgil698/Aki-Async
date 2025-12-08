package org.virgil.akiasync.mixin.mixins.entitytracker;
import java.util.Set;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
@SuppressWarnings("unused")
@Mixin(ChunkMap.TrackedEntity.class)
public abstract class EntityTrackerMixin {
    private static volatile boolean cached_enabled;
    private static volatile java.util.concurrent.ExecutorService cached_executor;
    private static volatile boolean initialized = false;
    private static volatile boolean isFolia = false;
    private static int asyncTaskCount = 0;
    private static volatile int cached_queueSize = 10000;
    private static volatile java.util.concurrent.BlockingQueue<Runnable> BATCH_QUEUE = null;
    private static final int BATCH_SIZE = 50;
    private static final java.util.concurrent.atomic.AtomicBoolean batchSubmitted =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    @Shadow @Final ServerEntity serverEntity;
    @Shadow @Final Entity entity;
    @Shadow @Final int range;
    @Shadow @Final Set<ServerPlayer> seenBy;
    private volatile long lastAsyncUpdate = 0;
    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void preUpdatePlayer(ServerPlayer player, CallbackInfo ci) {
        if (!initialized) { akiasync$initEntityTracker(); }

        if (!cached_enabled || cached_executor == null || entity instanceof ServerPlayer) return;

        if (akiasync$isVirtualEntity(entity)) {
            return;
        }
        
        Bridge bridgeForThrottle = BridgeManager.getBridge();
        if (bridgeForThrottle != null && bridgeForThrottle.isEntityPacketThrottleEnabled()) {
            if (!bridgeForThrottle.shouldSendEntityUpdate(player, entity)) {
                
                ci.cancel();
                return;
            }
        }
        if (entity.getDeltaMovement().lengthSqr() < 1.0E-7 &&
            !entity.isPassenger() &&
            !entity.hasPassenger(player)) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAsyncUpdate > 50) {
            lastAsyncUpdate = currentTime;
            final double entityX = entity.getX();
            final double entityY = entity.getY();
            final double entityZ = entity.getZ();
            final int trackingRange = this.range;
            final Entity trackedEntity = this.entity;
            final ServerLevel level = (ServerLevel) entity.level();

            Runnable trackingTask = () -> {
                try {
                    Bridge bridge = null;
                    if (isFolia) {
                        bridge = BridgeManager.getBridge();
                        if (bridge == null) return;

                        if (!bridge.canAccessEntityDirectly(trackedEntity)) {
                            return;
                        }
                    }

                    java.util.List<?> players = level.players();
                    for (Object playerObj : players) {
                        if (!(playerObj instanceof ServerPlayer)) continue;
                        ServerPlayer p = (ServerPlayer) playerObj;

                        if (isFolia && bridge != null) {
                            if (!bridge.canAccessEntityDirectly(p)) {
                                continue;
                            }
                        }

                        double dx = p.getX() - entityX;
                        double dy = p.getY() - entityY;
                        double dz = p.getZ() - entityZ;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        double rangeSq = trackingRange * trackingRange;
                        boolean shouldTrack = distSq <= rangeSq;
                    }
                } catch (IllegalStateException e) {
                    if (isFolia && e.getMessage() != null &&
                        e.getMessage().contains("region")) {
                    }
                } catch (Throwable t) {
                }
            };

            if (!BATCH_QUEUE.offer(trackingTask)) {
                if (asyncTaskCount <= 5) {
                    BridgeConfigCache.errorLog("[AkiAsync-Warning] EntityTracker queue full (" + BATCH_QUEUE.size() + "/" + cached_queueSize + "), executing synchronously");
                }
                try {
                    trackingTask.run();
                } catch (Throwable t) {
                }
                return;
            }
            int queueSize = BATCH_QUEUE.size();
            boolean shouldSubmitBatch = (queueSize >= BATCH_SIZE || queueSize > 100) &&
                                       batchSubmitted.compareAndSet(false, true);

            if (shouldSubmitBatch) {
                asyncTaskCount++;
                if (asyncTaskCount <= 3) {
                    BridgeConfigCache.debugLog("[AkiAsync-Batch] Submitting batch, queue size: " + queueSize + "/" + cached_queueSize);
                }

                int dynamicBatchSize = Math.min(Math.max(BATCH_SIZE, queueSize / 2), 200);
                java.util.List<Runnable> batch = new java.util.ArrayList<>(dynamicBatchSize);
                Runnable task;
                while ((task = BATCH_QUEUE.poll()) != null && batch.size() < dynamicBatchSize) {
                    batch.add(task);
                }
                final int batchSize = batch.size();
                cached_executor.execute(() -> {
                    try {
                        if (asyncTaskCount <= 2) {
                            BridgeConfigCache.debugLog("[AkiAsync-Batch] Processing " + batchSize + " tasks in thread: " + Thread.currentThread().getName());
                        }
                        batch.parallelStream().forEach(Runnable::run);
                        if (asyncTaskCount <= 3) {
                            BridgeConfigCache.debugLog("[AkiAsync-Batch] Completed batch of " + batchSize + " tasks");
                        }
                    } catch (Throwable t) {
                        BridgeConfigCache.errorLog("[AkiAsync-Error] Batch processing failed: " + t.getMessage());
                    } finally {
                        batchSubmitted.set(false);
                    }
                });
            }
        }
    }
    private boolean akiasync$isVirtualEntity(Entity entity) {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            return bridge.isVirtualEntity(entity);
        }
        return false;
    }

    private static synchronized void akiasync$initEntityTracker() {
        if (initialized) return;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isEntityTrackerEnabled();
            cached_executor = bridge.getGeneralExecutor();
            cached_queueSize = bridge.getEntityTrackerQueueSize();

            BATCH_QUEUE = new java.util.concurrent.LinkedBlockingQueue<>(cached_queueSize);

            if (isFolia) {
                BridgeConfigCache.debugLog("[AkiAsync] EntityTrackerMixin initialized in Folia mode:");
                BridgeConfigCache.debugLog("  - Enabled: " + cached_enabled + " (async with region safety checks)");
                BridgeConfigCache.debugLog("  - Executor: " + (cached_executor != null ? "available" : "null"));
                BridgeConfigCache.debugLog("  - Queue Size: " + cached_queueSize);
                BridgeConfigCache.debugLog("  - Safety: canAccessEntityDirectly() checks before all entity access");
                BridgeConfigCache.debugLog("  - Exception handling: IllegalStateException for cross-region access");
            } else {
                BridgeConfigCache.debugLog("[AkiAsync] EntityTrackerMixin initialized: enabled=" + cached_enabled + ", executor=" + (cached_executor != null) + ", queueSize=" + cached_queueSize);
            }
        } else {
            cached_enabled = false;
            cached_executor = null;
            cached_queueSize = 10000;
            BATCH_QUEUE = new java.util.concurrent.LinkedBlockingQueue<>(cached_queueSize);
        }
        initialized = true;
    }

    private static synchronized void aki$resetInitialization() {
        initialized = false;
        cached_enabled = false;
        cached_executor = null;
        cached_queueSize = 10000;
        asyncTaskCount = 0;

        if (BATCH_QUEUE != null) {
            BATCH_QUEUE.clear();
        }
        BATCH_QUEUE = null;

        batchSubmitted.set(false);
    }
}
