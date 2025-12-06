package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        List<FallingBlockEntity> fallingBlocks = new ArrayList<>();
        level.getAllEntities().forEach(entity -> {
            if (entity instanceof FallingBlockEntity falling) {
                if (!akiasync$isVirtualEntity(falling)) {
                    fallingBlocks.add(falling);
                }
            }
        });

        if (fallingBlocks.size() < minFallingBlocks) {
            return;
        }

        executionCount++;

        List<List<FallingBlockEntity>> batches = akiasync$partitionFallingBlocks(fallingBlocks, batchSize);
        long adaptiveTimeout = akiasync$calculateFallingTimeout(lastMspt);

        try {
            
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            java.util.concurrent.ExecutorService executor = dedicatedPool != null ? dedicatedPool : 
                (bridge != null ? bridge.getGeneralExecutor() : null);
            
            if (executor == null) {
                
                batches.forEach(batch -> batch.forEach(falling -> {
                    try {
                        akiasync$preTick(falling);
                    } catch (Throwable t) {
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
                        }
                    });
                }, executor))
                .collect(java.util.stream.Collectors.toList());

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .get(adaptiveTimeout, TimeUnit.MILLISECONDS);

            if (executionCount % 100 == 0) {
                org.virgil.akiasync.mixin.bridge.Bridge logBridge =
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (logBridge != null) {
                    logBridge.debugLog(
                        "[AkiAsync-FallingBlock] Processed %d falling blocks in %d batches (timeout: %dms)",
                        fallingBlocks.size(), batches.size(), adaptiveTimeout
                    );
                }
            }
        } catch (Throwable t) {
            if (executionCount <= 3) {
                org.virgil.akiasync.mixin.bridge.Bridge errorBridge =
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (errorBridge != null) {
                    errorBridge.debugLog("[AkiAsync-FallingBlock] Timeout/Error: " + t.getMessage());
                }
            }
        }
    }

    private void akiasync$preTick(FallingBlockEntity falling) {
    }

    private List<List<FallingBlockEntity>> akiasync$partitionFallingBlocks(List<FallingBlockEntity> list, int size) {
        List<List<FallingBlockEntity>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
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
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
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

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (bridge != null) {
            if (isFolia) {
                enabled = false;
                bridge.debugLog("[AkiAsync] FallingBlockParallelMixin disabled in Folia mode");
            } else {
                enabled = bridge.isFallingBlockParallelEnabled();
            }
            minFallingBlocks = bridge.getMinFallingBlocksForParallel();
            batchSize = bridge.getFallingBlockBatchSize();
            dedicatedPool = bridge.getGeneralExecutor();
        } else {
            enabled = false;
            minFallingBlocks = 20;
            batchSize = 10;
            dedicatedPool = null;
        }

        initialized = true;

        if (bridge != null) {
            bridge.debugLog("[AkiAsync] FallingBlockParallelMixin initialized: enabled=" + enabled +
                ", isFolia=" + isFolia + ", batchSize=" + batchSize +
                ", minFallingBlocks=" + minFallingBlocks +
                ", pool=" + (dedicatedPool != null ? "dedicated" : "commonPool"));
        }
    }
}
