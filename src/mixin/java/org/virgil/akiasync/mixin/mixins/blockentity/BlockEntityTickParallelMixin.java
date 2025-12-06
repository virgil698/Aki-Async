package org.virgil.akiasync.mixin.mixins.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TickingBlockEntity;

@SuppressWarnings("unused")
@Mixin(value = ServerLevel.class, priority = 1100)
public abstract class BlockEntityTickParallelMixin {
    @Unique private static volatile boolean enabled;
    @Unique private static volatile int minBlockEntities;
    @Unique private static volatile int batchSize;
    @Unique private static volatile ExecutorService executor;
    @Unique private static volatile boolean initialized = false;
    @Unique private static volatile boolean isFolia = false;
    @Unique private static volatile boolean protectContainers;
    @Unique private static volatile int timeoutMs;
    @Unique private static int executionCount = 0;
    @Unique private static volatile Object smoothingScheduler;

    @Unique private static java.lang.reflect.Field blockEntityTickersField = null;
    @Unique private static boolean blockEntityTickersFieldChecked = false;

    @Inject(method = "tickBlockEntities", at = @At("HEAD"), cancellable = true, require = 0)
    private void parallelTickBlockEntities(CallbackInfo ci) {
        if (!initialized) { akiasync$initBlockEntityParallel(); }
        if (!enabled) return;
        
        if (smoothingScheduler != null) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.notifySmoothSchedulerTick(smoothingScheduler);
                bridge.updateSmoothSchedulerMetrics(smoothingScheduler, bridge.getCurrentTPS(), bridge.getCurrentMSPT());
            }
        }

        List<TickingBlockEntity> blockEntityTickers = akiasync$getBlockEntityTickers();
        if (blockEntityTickers == null || blockEntityTickers.size() < minBlockEntities) return;

        ci.cancel();
        executionCount++;
        
        if (smoothingScheduler != null && !isFolia) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {

                java.util.Map<Integer, java.util.List<Runnable>> tasksByPriority = new java.util.HashMap<>();
                
                for (TickingBlockEntity blockEntity : blockEntityTickers) {
                    if (blockEntity == null) continue;
                    
                    if (protectContainers && akiasync$isContainerBlockEntity(blockEntity)) {
                        continue;
                    }
                    
                    int priority = akiasync$determineBlockEntityPriority(blockEntity);
                    tasksByPriority.computeIfAbsent(priority, k -> new java.util.ArrayList<>())
                        .add(() -> {
                            try {
                                blockEntity.tick();
                            } catch (Throwable t) {
                                
                                org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                                if (errorBridge != null && errorBridge.isDebugLoggingEnabled()) {
                                    errorBridge.errorLog("[BlockEntity-Smooth] Error ticking %s at %s: %s", 
                                        blockEntity.getType(), blockEntity.getPos(), t.getMessage());
                                }
                            }
                        });
                }
                
                for (java.util.Map.Entry<Integer, java.util.List<Runnable>> entry : tasksByPriority.entrySet()) {
                    bridge.submitSmoothTaskBatch(smoothingScheduler, entry.getValue(), entry.getKey(), "BlockEntity");
                }
                
                if (protectContainers) {
                    for (TickingBlockEntity blockEntity : blockEntityTickers) {
                        if (blockEntity != null && akiasync$isContainerBlockEntity(blockEntity)) {
                            try {
                                blockEntity.tick();
                            } catch (Throwable t) {
                                
                                org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                                if (errorBridge != null && errorBridge.isDebugLoggingEnabled()) {
                                    errorBridge.errorLog("[BlockEntity-Container] Error ticking container %s at %s: %s", 
                                        blockEntity.getType(), blockEntity.getPos(), t.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            return;
        }

        List<List<TickingBlockEntity>> batches = akiasync$partitionBlockEntities(blockEntityTickers, batchSize);

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>(batches.size());

            for (List<TickingBlockEntity> batch : batches) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (TickingBlockEntity blockEntity : batch) {
                        if (blockEntity == null) continue;

                        try {
                            if (protectContainers && akiasync$isContainerBlockEntity(blockEntity)) {
                                continue;
                            }

                            blockEntity.tick();
                        } catch (Throwable t) {
                            if (executionCount <= 3) {
                                org.virgil.akiasync.mixin.bridge.Bridge bridge =
                                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                                if (bridge != null) {
                                    bridge.errorLog("[AkiAsync-BlockEntity] Error ticking block entity: " +
                                        blockEntity.getType() + " at " + blockEntity.getPos());
                                }
                            }
                        }
                    }
                }, executor);

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(timeoutMs, TimeUnit.MILLISECONDS);
            if (protectContainers) {
                for (TickingBlockEntity blockEntity : blockEntityTickers) {
                    if (blockEntity != null && akiasync$isContainerBlockEntity(blockEntity)) {
                        try {
                            blockEntity.tick();
                        } catch (Throwable t2) {
                            
                            org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                            if (errorBridge != null && errorBridge.isDebugLoggingEnabled()) {
                                errorBridge.errorLog("[BlockEntity-Protected] Error ticking protected container %s at %s: %s", 
                                    blockEntity.getType(), blockEntity.getPos(), t2.getMessage());
                            }
                        }
                    }
                }
            }

            if (executionCount % 100 == 0) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge =
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog(
                        "[AkiAsync-BlockEntity] Processed %d block entities in %d batches",
                        blockEntityTickers.size(), batches.size()
                    );
                }
            }

        } catch (Throwable t) {
            if (executionCount <= 3) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge =
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.errorLog("[AkiAsync-BlockEntity] Parallel execution failed, falling back to sync: " +
                        t.getMessage());
                }
            }

            for (TickingBlockEntity blockEntity : blockEntityTickers) {
                if (blockEntity != null) {
                    try {
                        blockEntity.tick();
                    } catch (Throwable t2) {
                        
                        org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                        if (errorBridge != null && errorBridge.isDebugLoggingEnabled()) {
                            errorBridge.errorLog("[BlockEntity-Fallback] Error in fallback tick for %s at %s: %s", 
                                blockEntity.getType(), blockEntity.getPos(), t2.getMessage());
                        }
                    }
                }
            }
        }
    }

    @Unique
    private boolean akiasync$isContainerBlockEntity(TickingBlockEntity blockEntity) {
        if (!protectContainers) return false;

        String type = blockEntity.getType();
        return type.contains("chest") ||
               type.contains("barrel") ||
               type.contains("shulker_box") ||
               type.contains("hopper") ||
               type.contains("dropper") ||
               type.contains("dispenser") ||
               type.contains("furnace") ||
               type.contains("blast_furnace") ||
               type.contains("smoker") ||
               type.contains("brewing_stand") ||
               type.contains("beacon") ||
               type.contains("campfire") ||
               type.contains("lectern") ||
               type.contains("jukebox");
    }

    @Unique
    private boolean akiasync$isLithiumSleepingBlockEntity(TickingBlockEntity blockEntity) {
        try {
            String type = blockEntity.getType();
            return type.contains("lithium") || type.contains("sleeping");
        } catch (Throwable t) {
            return false;
        }
    }

    @Unique
    private List<List<TickingBlockEntity>> akiasync$partitionBlockEntities(
        List<TickingBlockEntity> list, int size) {
        List<List<TickingBlockEntity>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    @Unique
    @SuppressWarnings("unchecked")
    private List<TickingBlockEntity> akiasync$getBlockEntityTickers() {
        
        if (blockEntityTickersFieldChecked && blockEntityTickersField != null) {
            try {
                return (List<TickingBlockEntity>) blockEntityTickersField.get(this);
            } catch (Throwable t) {
                return null;
            }
        }
        
        if (!blockEntityTickersFieldChecked) {
            synchronized (BlockEntityTickParallelMixin.class) {
                if (!blockEntityTickersFieldChecked) {
                    try {
                        blockEntityTickersField = ServerLevel.class.getDeclaredField("blockEntityTickers");
                        blockEntityTickersField.setAccessible(true);
                    } catch (NoSuchFieldException e) {
                        blockEntityTickersField = null;
                    } catch (Throwable t) {
                        blockEntityTickersField = null;
                    }
                    blockEntityTickersFieldChecked = true;
                }
            }
        }

        if (blockEntityTickersField == null) {
            return null;
        }

        try {
            return (List<TickingBlockEntity>) blockEntityTickersField.get(this);
        } catch (Throwable t) {
            return null;
        }
    }

    @Unique
    private static synchronized void akiasync$initBlockEntityParallel() {
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
                bridge.debugLog("[AkiAsync] BlockEntityTickParallel disabled in Folia mode");
            } else {
                enabled = bridge.isBlockEntityParallelTickEnabled();
                minBlockEntities = bridge.getBlockEntityParallelMinBlockEntities();
                batchSize = bridge.getBlockEntityParallelBatchSize();
                protectContainers = bridge.isBlockEntityParallelProtectContainers();
                timeoutMs = bridge.getBlockEntityParallelTimeoutMs();
                executor = bridge.getGeneralExecutor();

                bridge.debugLog("[AkiAsync] BlockEntityTickParallel initialized:");
                bridge.debugLog("  - Enabled: " + enabled);
                bridge.debugLog("  - Min block entities: " + minBlockEntities);
                bridge.debugLog("  - Batch size: " + batchSize);
                bridge.debugLog("  - Protect containers: " + protectContainers);
                bridge.debugLog("  - Timeout: " + timeoutMs + "ms");
            }
        } else {
            enabled = false;
        }

        initialized = true;
        
        if (bridge != null && enabled && !isFolia) {
            smoothingScheduler = bridge.getBlockEntitySmoothingScheduler();
            if (smoothingScheduler != null) {
                bridge.debugLog("[AkiAsync] BlockEntity TaskSmoothingScheduler obtained from Bridge");
            }
        }
    }
    
    @Unique
    private int akiasync$determineBlockEntityPriority(TickingBlockEntity blockEntity) {
        if (blockEntity == null) return 3;
        
        try {
            String type = blockEntity.getType();
            
            if (type.contains("hopper") || type.contains("piston")) {
                return 0;
            }
            
            if (type.contains("furnace") || type.contains("blast_furnace") ||
                type.contains("smoker") || type.contains("brewing_stand") ||
                type.contains("beacon")) {
                return 1;
            }
            
            if (type.contains("chest") || type.contains("barrel") ||
                type.contains("shulker_box") || type.contains("dropper") ||
                type.contains("dispenser")) {
                return 2;
            }
            
            if (type.contains("sign") || type.contains("banner") ||
                type.contains("skull") || type.contains("lectern") ||
                type.contains("jukebox") || type.contains("campfire")) {
                return 3;
            }
            
            return 2;
        } catch (Throwable t) {
            return 3;
        }
    }
}
