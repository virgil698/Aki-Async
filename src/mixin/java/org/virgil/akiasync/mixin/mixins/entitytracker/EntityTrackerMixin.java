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
@SuppressWarnings("unused")
@Mixin(ChunkMap.TrackedEntity.class)
public abstract class EntityTrackerMixin {
    private static volatile boolean cached_enabled;
    private static volatile java.util.concurrent.ExecutorService cached_executor;
    private static volatile boolean initialized = false;
    private static int asyncTaskCount = 0;
    private static final java.util.concurrent.ConcurrentLinkedQueue<Runnable> BATCH_QUEUE = 
        new java.util.concurrent.ConcurrentLinkedQueue<>();
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
            BATCH_QUEUE.add(trackingTask);
            if (BATCH_QUEUE.size() >= BATCH_SIZE && batchSubmitted.compareAndSet(false, true)) {
                asyncTaskCount++;
                if (asyncTaskCount <= 3) {
                    System.out.println("[AkiAsync-Batch] Submitting batch of " + BATCH_QUEUE.size() + " tracking tasks");
                }
                java.util.List<Runnable> batch = new java.util.ArrayList<>(BATCH_SIZE);
                Runnable task;
                while ((task = BATCH_QUEUE.poll()) != null && batch.size() < BATCH_SIZE) {
                    batch.add(task);
                }
                final int batchSize = batch.size();
                cached_executor.execute(() -> {
                    try {
                        if (asyncTaskCount <= 2) {
                            System.out.println("[AkiAsync-Batch] Processing " + batchSize + " tasks in thread: " + Thread.currentThread().getName());
                        }
                        batch.parallelStream().forEach(Runnable::run);
                        if (asyncTaskCount <= 3) {
                            System.out.println("[AkiAsync-Batch] Completed batch of " + batchSize + " tasks");
                        }
                    } catch (Throwable t) {
                        System.err.println("[AkiAsync-Error] Batch processing failed: " + t.getMessage());
                    } finally {
                        batchSubmitted.set(false);
                    }
                });
            }
        }
    }
    private static synchronized void akiasync$initEntityTracker() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isEntityTrackerEnabled();
            cached_executor = bridge.getGeneralExecutor();
        } else {
            cached_enabled = false;
            cached_executor = null;
        }
        initialized = true;
        System.out.println("[AkiAsync] EntityTrackerMixin initialized: enabled=" + cached_enabled + 
            ", executor=" + (cached_executor != null));
    }
}