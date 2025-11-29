package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
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
public abstract class ItemEntityParallelMixin {

    private static volatile boolean enabled;
    private static volatile int minItemEntities;
    private static volatile int batchSize;
    private static volatile boolean initialized = false;
    private static volatile java.util.concurrent.ExecutorService dedicatedPool;
    private static volatile boolean isFolia = false;

    private static int executionCount = 0;
    private static long lastMspt = 20;

    @Inject(method = "tick", at = @At("TAIL"))
    private void parallelItemEntityTick(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initItemEntityParallel();
        }
        if (!enabled) return;

        ServerLevel level = (ServerLevel) (Object) this;

        List<ItemEntity> itemEntities = new ArrayList<>();
        level.getAllEntities().forEach(entity -> {
            if (entity instanceof ItemEntity item) {
                if (!akiasync$isVirtualEntity(item)) {
                    if (!akiasync$isInDangerousEnvironment(item)) {
                        itemEntities.add(item);
                    }
                }
            }
        });

        if (itemEntities.size() < minItemEntities) {
            return;
        }

        executionCount++;

        List<List<ItemEntity>> batches = akiasync$partitionItemEntities(itemEntities, batchSize);
        long adaptiveTimeout = akiasync$calculateItemTimeout(lastMspt);

        try {
            List<CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> CompletableFuture.runAsync(() -> {
                    batch.forEach(item -> {
                        try {
                            akiasync$preTickItem(item);
                        } catch (Throwable t) {
                        }
                    });
                }, dedicatedPool != null ? dedicatedPool : ForkJoinPool.commonPool()))
                .collect(java.util.stream.Collectors.toList());

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .get(adaptiveTimeout, TimeUnit.MILLISECONDS);

            if (executionCount % 100 == 0) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge =
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog(
                        "[AkiAsync-ItemEntity] Processed %d item entities in %d batches (timeout: %dms)",
                        itemEntities.size(), batches.size(), adaptiveTimeout
                    );
                }
            }
        } catch (Throwable t) {
            if (executionCount <= 3) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge =
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog("[AkiAsync-ItemEntity] Timeout/Error: " + t.getMessage());
                }
            }
        }
    }

    private void akiasync$preTickItem(ItemEntity item) {
    }

    private boolean akiasync$isInDangerousEnvironment(ItemEntity item) {
        if (item.isInLava() || item.isOnFire() || item.getRemainingFireTicks() > 0) {
            return true;
        }

        net.minecraft.core.BlockPos pos = item.blockPosition();
        net.minecraft.world.level.block.state.BlockState state = item.level().getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.LayeredCauldronBlock ||
            state.getBlock() instanceof net.minecraft.world.level.block.LavaCauldronBlock) {
            return true;
        }

        return false;
    }

    private List<List<ItemEntity>> akiasync$partitionItemEntities(List<ItemEntity> list, int size) {
        List<List<ItemEntity>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    private long akiasync$calculateItemTimeout(long mspt) {
        if (mspt < 20) return 100;
        if (mspt <= 30) return 50;
        return 25;
    }

    private boolean akiasync$isVirtualEntity(ItemEntity entity) {
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

    private static synchronized void akiasync$initItemEntityParallel() {
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
                bridge.debugLog("[AkiAsync] ItemEntityParallelMixin disabled in Folia mode");
            } else {
                enabled = bridge.isItemEntityParallelEnabled();
            }
            minItemEntities = bridge.getMinItemEntitiesForParallel();
            batchSize = bridge.getItemEntityBatchSize();
            dedicatedPool = bridge.getGeneralExecutor();
        } else {
            enabled = false;
            minItemEntities = 50;
            batchSize = 20;
            dedicatedPool = null;
        }

        initialized = true;

        if (bridge != null) {
            bridge.debugLog("[AkiAsync] ItemEntityParallelMixin initialized: enabled=" + enabled +
                ", isFolia=" + isFolia + ", batchSize=" + batchSize +
                ", minItemEntities=" + minItemEntities +
                ", pool=" + (dedicatedPool != null ? "dedicated" : "commonPool"));
        }
    }
}
