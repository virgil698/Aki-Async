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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
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
        
        if (isFolia) {
            if (!akiasync$canAccessEntitySafely(entity)) {
                return;
            }
        }
        
        if (akiasync$isVirtualEntity(entity)) {
            return;
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
            Runnable trackingTask = () -> {
                try {
                    java.util.List<?> players = entity.level().players();
                    for (Object playerObj : players) {
                        if (!(playerObj instanceof ServerPlayer)) continue;
                        ServerPlayer p = (ServerPlayer) playerObj;
                        
                        if (isFolia) {
                            double dx = p.getX() - entityX;
                            double dz = p.getZ() - entityZ;
                            double distSq2D = dx * dx + dz * dz;
                            if (distSq2D > 256 * 256) {
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
                } catch (Throwable t) {
                }
            };
            
            if (!BATCH_QUEUE.offer(trackingTask)) {
                if (asyncTaskCount <= 5) {
                    org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    if (bridge != null) {
                        bridge.errorLog("[AkiAsync-Warning] EntityTracker queue full (" + BATCH_QUEUE.size() + "/" + cached_queueSize + "), executing synchronously");
                    }
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
                    org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    if (bridge != null) {
                        bridge.debugLog("[AkiAsync-Batch] Submitting batch, queue size: " + queueSize + "/" + cached_queueSize);
                    }
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
                            org.virgil.akiasync.mixin.bridge.Bridge debugBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                            if (debugBridge != null) {
                                debugBridge.debugLog("[AkiAsync-Batch] Processing " + batchSize + " tasks in thread: " + Thread.currentThread().getName());
                            }
                        }
                        batch.parallelStream().forEach(Runnable::run);
                        if (asyncTaskCount <= 3) {
                            org.virgil.akiasync.mixin.bridge.Bridge completeBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                            if (completeBridge != null) {
                                completeBridge.debugLog("[AkiAsync-Batch] Completed batch of " + batchSize + " tasks");
                            }
                        }
                    } catch (Throwable t) {
                        org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                        if (errorBridge != null) {
                            errorBridge.errorLog("[AkiAsync-Error] Batch processing failed: " + t.getMessage());
                        }
                    } finally {
                        batchSubmitted.set(false);
                    }
                });
            }
        }
    }
    private boolean akiasync$canAccessEntitySafely(Entity entity) {
        if (!isFolia) return true;
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeManager.getBridge();
            return bridge != null && bridge.canAccessEntityDirectly(entity);
        } catch (Throwable t) {
            return false;
        }
    }
    
    private boolean akiasync$isVirtualEntity(Entity entity) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
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
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isEntityTrackerEnabled();
            cached_executor = bridge.getGeneralExecutor();
            cached_queueSize = bridge.getEntityTrackerQueueSize();
            
            BATCH_QUEUE = new java.util.concurrent.LinkedBlockingQueue<>(cached_queueSize);
            
            if (isFolia) {
                bridge.debugLog("[AkiAsync] EntityTrackerMixin initialized in Folia mode:");
                bridge.debugLog("  - Enabled: " + cached_enabled + " (with region-aware processing)");
                bridge.debugLog("  - Executor: " + (cached_executor != null ? "available" : "null"));
                bridge.debugLog("  - Queue Size: " + cached_queueSize);
                bridge.debugLog("  - Region safety: Cross-region access prevented");
            } else {
                bridge.debugLog("[AkiAsync] EntityTrackerMixin initialized: enabled=" + cached_enabled + ", executor=" + (cached_executor != null) + ", queueSize=" + cached_queueSize);
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