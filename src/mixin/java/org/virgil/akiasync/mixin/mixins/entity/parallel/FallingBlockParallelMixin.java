package org.virgil.akiasync.mixin.mixins.entity.parallel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@Mixin(ServerLevel.class)
public abstract class FallingBlockParallelMixin {

    private static volatile boolean enabled;
    private static volatile int minFallingBlocks;
    private static volatile int batchSize;
    private static volatile boolean initialized = false;
    private static volatile java.util.concurrent.ExecutorService dedicatedPool;
    private static volatile boolean isFolia = false;

    private static int executionCount = 0;
    private static long lastMspt = 20;

    @Inject(method = "tick", at = @At("TAIL"))
    @SuppressWarnings("resource")
    private void parallelFallingBlockTick(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initFallingBlockParallel();
        }
        if (!enabled) return;

        ServerLevel level = (ServerLevel) (Object) this;

        List<FallingBlockEntity> fallingBlocks = new ArrayList<>(64);

        try {

            final int MAX_FALLING_BLOCKS = 1000;
            int count = 0;

            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                count++;
                if (entity instanceof FallingBlockEntity falling) {
                    if (!akiasync$isVirtualEntity(falling)) {
                        fallingBlocks.add(falling);

                        if (fallingBlocks.size() >= MAX_FALLING_BLOCKS) {
                            BridgeConfigCache.debugLog(
                                "[AkiAsync-FallingBlock] WARNING: Too many falling blocks (%d collected, %d+ total entities), limiting to %d",
                                fallingBlocks.size(), count, MAX_FALLING_BLOCKS
                            );
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "FallingBlockParallel", "collectFallingBlocks", e);
            return;
        }

        if (fallingBlocks.size() < minFallingBlocks) {
            return;
        }

        executionCount++;

        List<List<FallingBlockEntity>> batches = akiasync$partitionFallingBlocks(fallingBlocks, batchSize);
        long adaptiveTimeout = akiasync$calculateFallingTimeout(lastMspt);

        try {

            Bridge bridge = BridgeManager.getBridge();
            java.util.concurrent.ExecutorService executor = dedicatedPool != null ? dedicatedPool :
                (bridge != null ? bridge.getGeneralExecutor() : null);

            if (executor == null) {

                batches.forEach(batch -> batch.forEach(falling -> {
                    try {
                        akiasync$preTick(falling);
                    } catch (Throwable t) {
                        org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                            "FallingBlockParallel", "preTickFallback",
                            t instanceof Exception ? (Exception) t : new RuntimeException(t));
                    }
                }));
                return;
            }

            List<CompletableFuture<Void>> futures = batches.stream()
                .<CompletableFuture<Void>>map(batch -> CompletableFuture.runAsync(() -> {
                    batch.forEach(falling -> {
                        try {
                            akiasync$preTick(falling);
                        } catch (Throwable t) {
                            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                                "FallingBlockParallel", "preTickAsync",
                                t instanceof Exception ? (Exception) t : new RuntimeException(t));
                        }
                    });
                }, executor))
                .collect(java.util.stream.Collectors.toList());

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .get(adaptiveTimeout, TimeUnit.MILLISECONDS);

            if (executionCount % 100 == 0) {
                BridgeConfigCache.debugLog(
                    "[AkiAsync-FallingBlock] Processed %d falling blocks in %d batches (timeout: %dms)",
                    fallingBlocks.size(), batches.size(), adaptiveTimeout
                );
            }
        } catch (Throwable t) {
            if (executionCount <= 3) {
                BridgeConfigCache.debugLog("[AkiAsync-FallingBlock] Timeout/Error: " + t.getMessage());
            }
        } finally {

            fallingBlocks.clear();
            batches.clear();
        }
    }

    private void akiasync$preTick(FallingBlockEntity falling) {
    }

    private List<List<FallingBlockEntity>> akiasync$partitionFallingBlocks(List<FallingBlockEntity> list, int size) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        if (size <= 0) {
            BridgeConfigCache.debugLog(
                "[AkiAsync-FallingBlock] WARNING: Invalid batch size (%d), using default 10",
                size
            );
            size = 10;
        }

        int totalSize = list.size();
        int numBatches = (totalSize + size - 1) / size;
        List<List<FallingBlockEntity>> result = new ArrayList<>(numBatches);

        for (int i = 0; i < totalSize; i += size) {
            int end = Math.min(i + size, totalSize);

            List<FallingBlockEntity> batch = new ArrayList<>(end - i);
            for (int j = i; j < end; j++) {
                batch.add(list.get(j));
            }
            result.add(batch);
        }

        return result;
    }

    private long akiasync$calculateFallingTimeout(long mspt) {
        if (mspt < 20) return 100;
        if (mspt <= 30) return 50;
        return 25;
    }

    private boolean akiasync$isVirtualEntity(FallingBlockEntity entity) {
        if (entity == null) return false;

        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                return bridge.isVirtualEntity(entity);
            }
        } catch (Throwable t) {
            return true;
        }

        return false;
    }

    private static synchronized void akiasync$initFallingBlockParallel() {
        if (initialized) return;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        Bridge bridge = BridgeManager.getBridge();

        if (bridge != null) {
            if (isFolia) {
                enabled = false;
                BridgeConfigCache.debugLog("[AkiAsync] FallingBlockParallelMixin disabled in Folia mode");
            } else {
                enabled = bridge.isFallingBlockParallelEnabled();
            }
            minFallingBlocks = bridge.getMinFallingBlocksForParallel();
            batchSize = bridge.getFallingBlockBatchSize();

            if (batchSize <= 0) {
                BridgeConfigCache.debugLog(
                    "[AkiAsync-FallingBlock] WARNING: Invalid batch size from config (%d), using default 10",
                    batchSize
                );
                batchSize = 10;
            }

            dedicatedPool = bridge.getGeneralExecutor();
            initialized = true;
        } else {
            enabled = false;
            minFallingBlocks = 20;
            batchSize = 10;
            dedicatedPool = null;
        }

        BridgeConfigCache.debugLog("[AkiAsync] FallingBlockParallelMixin initialized: enabled=" + enabled +
            ", isFolia=" + isFolia + ", batchSize=" + batchSize +
            ", minFallingBlocks=" + minFallingBlocks +
            ", pool=" + (dedicatedPool != null ? "dedicated" : "commonPool"));
    }
}
