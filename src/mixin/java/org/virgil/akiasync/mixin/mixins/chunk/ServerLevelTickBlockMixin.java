package org.virgil.akiasync.mixin.mixins.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BlockTickCategory;
import org.virgil.akiasync.mixin.util.BlockTickTask;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
@Mixin(value = ServerLevel.class, priority = 1200)
public abstract class ServerLevelTickBlockMixin {
    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean initialized = false;
    @Unique private static volatile int cached_batchSize = 16;
    @Unique private static volatile boolean isFoliaEnvironment = false;
    @Unique private static ExecutorService ASYNC_BLOCK_TICK_EXECUTOR;
    @Unique private static Object smoothingScheduler;
    
    @Unique private static final ConcurrentHashMap<Block, BlockTickCategory> BLOCK_CATEGORY_CACHE = new ConcurrentHashMap<>();
    
    @Unique private static final ThreadLocal<List<BlockTickTask>> asyncTasks = ThreadLocal.withInitial(ArrayList::new);
    
    @Unique private static final java.util.concurrent.atomic.AtomicLong asyncExecutionCount = new java.util.concurrent.atomic.AtomicLong(0);
    @Unique private static final java.util.concurrent.atomic.AtomicLong mainThreadExecutionCount = new java.util.concurrent.atomic.AtomicLong(0);
    @Unique private static final java.util.concurrent.atomic.AtomicLong syncFallbackCount = new java.util.concurrent.atomic.AtomicLong(0);
    @Unique private static final java.util.concurrent.atomic.AtomicLong batchSubmissionCount = new java.util.concurrent.atomic.AtomicLong(0);
    
    @Unique private static final java.util.concurrent.atomic.AtomicLong cropGrowthCount = new java.util.concurrent.atomic.AtomicLong(0);
    @Unique private static final java.util.concurrent.atomic.AtomicLong leafDecayCount = new java.util.concurrent.atomic.AtomicLong(0);
    @Unique private static final java.util.concurrent.atomic.AtomicLong redstoneCount = new java.util.concurrent.atomic.AtomicLong(0);
    @Unique private static final java.util.concurrent.atomic.AtomicLong entityInteractionCount = new java.util.concurrent.atomic.AtomicLong(0);
    
    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void aki$asyncTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        if (pos == null || block == null) {
            return;
        }
        
        if (!initialized) {
            aki$initBlockTickAsync();
        }
        
        if (!cached_enabled) {
            return;
        }

        ServerLevel level = (ServerLevel) (Object) this;
        BlockState state = level.getBlockState(pos);
        
        if (state == null || !state.is(block)) {
            ci.cancel();
            return;
        }

        BlockTickCategory category = aki$classifyBlock(block);
        
        aki$updateCategoryStats(category);
        
        if (!category.canAsync()) {
            mainThreadExecutionCount.incrementAndGet();
            return; 
        }
        
        List<BlockTickTask> tasks = asyncTasks.get();
        tasks.add(new BlockTickTask(pos.immutable(), block, state, category));
        ci.cancel(); 
        
        if (tasks.size() >= cached_batchSize) {
            aki$submitAsyncBatch(level);
        }
    }
    
    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("RETURN"), require = 0)
    private void aki$flushAsyncTasks(CallbackInfo ci) {
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            bridge.tickEntityPacketThrottler();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        if (smoothingScheduler != null && bridge != null) {
            bridge.notifySmoothSchedulerTick(smoothingScheduler);
            double tps = bridge.getCurrentTPS();
            double mspt = bridge.getCurrentMSPT();
            bridge.updateSmoothSchedulerMetrics(smoothingScheduler, tps, mspt);
        }
        
        if (bridge != null) {
            bridge.tickEntityPacketThrottler();
            bridge.tickEntityDataThrottler();
            bridge.tickChunkVisibilityFilter();
        }
        
        ServerLevel level = (ServerLevel) (Object) this;
        List<BlockTickTask> tasks = asyncTasks.get();
        
        if (!tasks.isEmpty()) {
            aki$submitAsyncBatch(level);
        }
    }

    @Unique
    private static synchronized void aki$initBlockTickAsync() {
        aki$initBlockTickAsync(false);
    }

    @Unique
    private static synchronized void aki$initBlockTickAsync(boolean forceReload) {
        if (initialized && !forceReload) {
            return;
        }

        boolean wasInitialized = initialized;
        var bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        isFoliaEnvironment = bridge != null && bridge.isFoliaEnvironment();
        
        boolean previousEnabled = cached_enabled;
        cached_enabled = bridge != null && bridge.isChunkTickAsyncEnabled();
        cached_batchSize = bridge != null ? bridge.getChunkTickAsyncBatchSize() : 16;
        
        if (isFoliaEnvironment && cached_enabled) {
            cached_enabled = false;
            BridgeConfigCache.errorLog("[AkiAsync-BlockTick] ⚠️ Folia environment detected - disabling chunk tick async");
            BridgeConfigCache.errorLog("[AkiAsync-BlockTick] ⚠️ Folia's region threading already provides parallelism");
        }

        if (bridge != null) {
            ASYNC_BLOCK_TICK_EXECUTOR = bridge.getGeneralExecutor();
            
            if (smoothingScheduler == null && cached_enabled && !isFoliaEnvironment) {
                smoothingScheduler = bridge.getBlockTickSmoothingScheduler();
                if (smoothingScheduler != null) {
                    BridgeConfigCache.debugLog("[AkiAsync-BlockTick] TaskSmoothingScheduler obtained from Bridge");
                }
            }
        }

        initialized = true;
        
        if (wasInitialized) {
            BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Reloading...");
            aki$logMetrics();
            aki$resetMetrics();
            BLOCK_CATEGORY_CACHE.clear(); 
            
            BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Reloaded: enabled=" + cached_enabled + 
                " (was: " + previousEnabled + "), batchSize=" + cached_batchSize + 
                (isFoliaEnvironment ? " [Folia]" : ""));
        } else {
            BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Initialized: enabled=" + cached_enabled);
            if (isFoliaEnvironment) {
                BridgeConfigCache.debugLog("[AkiAsync-BlockTick]   Environment: Folia (async disabled - using region threading)");
            } else {
                BridgeConfigCache.debugLog("[AkiAsync-BlockTick]   Strategy: Smart classification + batch async execution");
                BridgeConfigCache.debugLog("[AkiAsync-BlockTick]   Batch size: " + cached_batchSize);
                BridgeConfigCache.debugLog("[AkiAsync-BlockTick]   Categories: CROP_GROWTH, LEAF_DECAY, SAFE_ASYNC (async)");
                BridgeConfigCache.debugLog("[AkiAsync-BlockTick]   Protected: REDSTONE, ENTITY_INTERACTION (main thread)");
            }
        }
    }
    
    @Unique
    private static BlockTickCategory aki$classifyBlock(Block block) {
        return BLOCK_CATEGORY_CACHE.computeIfAbsent(block, b -> {
            
            if (b instanceof net.minecraft.world.level.block.LiquidBlock) {
                return BlockTickCategory.ENTITY_INTERACTION;
            }
            
            String blockId = aki$getBlockId(b);
            
            if (blockId.contains("water") || 
                blockId.contains("lava") || 
                blockId.contains("flowing")) {
                return BlockTickCategory.ENTITY_INTERACTION;
            }
            
            if (aki$isRedstoneRelatedBlock(b)) {
                return BlockTickCategory.REDSTONE;
            }
            
            if (aki$requiresMainThreadExecution(b)) {
                return BlockTickCategory.ENTITY_INTERACTION;
            }
            
            if (blockId.contains("crop") || 
                blockId.contains("wheat") ||
                blockId.contains("carrot") ||
                blockId.contains("potato") ||
                blockId.contains("beetroot") ||
                blockId.contains("melon_stem") ||
                blockId.contains("pumpkin_stem") ||
                blockId.contains("nether_wart") ||
                blockId.contains("cocoa")) {
                return BlockTickCategory.CROP_GROWTH;
            }
            
            if (blockId.contains("leaves")) {
                return BlockTickCategory.LEAF_DECAY;
            }
            
            if (blockId.contains("kelp") ||
                blockId.contains("seagrass") ||
                blockId.contains("coral") ||
                blockId.contains("fungus") ||
                blockId.contains("roots") ||
                blockId.contains("moss") ||
                blockId.contains("lichen")) {
                return BlockTickCategory.SAFE_ASYNC;
            }
            
            return BlockTickCategory.ENTITY_INTERACTION;
        });
    }
    
    @Unique
    private static void aki$updateCategoryStats(BlockTickCategory category) {
        switch (category) {
            case CROP_GROWTH -> cropGrowthCount.incrementAndGet();
            case LEAF_DECAY -> leafDecayCount.incrementAndGet();
            case REDSTONE -> redstoneCount.incrementAndGet();
            case ENTITY_INTERACTION -> entityInteractionCount.incrementAndGet();
        }
    }
    
    @Unique
    private void aki$submitAsyncBatch(ServerLevel level) {
        List<BlockTickTask> tasks = asyncTasks.get();
        if (tasks.isEmpty()) {
            return;
        }
        
        List<BlockTickTask> batch = new ArrayList<>(tasks);
        tasks.clear();
        
        if (smoothingScheduler != null && !isFoliaEnvironment) {
            var bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {

                java.util.Map<Integer, java.util.List<Runnable>> tasksByPriority = new java.util.HashMap<>();
                
                for (BlockTickTask task : batch) {
                    int priority = aki$determinePriority(task.category);
                    tasksByPriority.computeIfAbsent(priority, k -> new java.util.ArrayList<>())
                        .add(() -> {
                            try {
                                BlockState currentState = level.getBlockState(task.pos);
                                if (currentState.is(task.block)) {
                                    currentState.tick(level, task.pos, level.random);
                                }
                            } catch (Throwable t) {
                                syncFallbackCount.incrementAndGet();
                                level.getServer().execute(() -> {
                                    try {
                                        BlockState state = level.getBlockState(task.pos);
                                        if (state.is(task.block)) {
                                            state.tick(level, task.pos, level.random);
                                        }
                                    } catch (Throwable e) {
                                        BridgeConfigCache.errorLog("[BlockTick-Scheduled] Error in scheduled tick at %s: %s", 
                                            task.pos, e.getMessage());
                                    }
                                });
                            }
                        });
                }
                
                for (java.util.Map.Entry<Integer, java.util.List<Runnable>> entry : tasksByPriority.entrySet()) {
                    bridge.submitSmoothTaskBatch(smoothingScheduler, entry.getValue(), entry.getKey(), "BlockTick");
                }
                
                asyncExecutionCount.addAndGet(batch.size());
                batchSubmissionCount.incrementAndGet();
                return;
            }
        }
        
        if (ASYNC_BLOCK_TICK_EXECUTOR == null || ASYNC_BLOCK_TICK_EXECUTOR.isShutdown()) {
            
            for (BlockTickTask task : batch) {
                try {
                    BlockState state = level.getBlockState(task.pos);
                    if (state.is(task.block)) {
                        state.tick(level, task.pos, level.random);
                    }
                } catch (Throwable e) {
                    BridgeConfigCache.errorLog("[BlockTick-Sync] Error in sync fallback at %s: %s", 
                        task.pos, e.getMessage());
                }
            }
            syncFallbackCount.addAndGet(batch.size());
            return;
        }
        
        batchSubmissionCount.incrementAndGet();
        asyncExecutionCount.addAndGet(batch.size());
        
        ASYNC_BLOCK_TICK_EXECUTOR.execute(() -> {
            for (BlockTickTask task : batch) {
                try {
                    
                    BlockState currentState = level.getBlockState(task.pos);
                    if (!currentState.is(task.block)) {
                        continue;
                    }
                    
                    currentState.tick(level, task.pos, level.random);
                    
                } catch (Throwable t) {
                    
                    syncFallbackCount.incrementAndGet();
                    
                    level.getServer().execute(() -> {
                        try {
                            BlockState state = level.getBlockState(task.pos);
                            if (state.is(task.block)) {
                                state.tick(level, task.pos, level.random);
                            }
                        } catch (Throwable e) {
                            BridgeConfigCache.errorLog("[BlockTick-AsyncFallback] Error in async fallback at %s: %s", 
                                task.pos, e.getMessage());
                        }
                    });
                }
            }
        });
    }

    @Unique
    private static void aki$reloadConfiguration() {
        aki$initBlockTickAsync(true);
    }
    
    @Unique
    private static java.util.Map<String, Long> aki$getMetrics() {
        java.util.Map<String, Long> metrics = new java.util.LinkedHashMap<>();
        metrics.put("async_executions", asyncExecutionCount.get());
        metrics.put("main_thread_executions", mainThreadExecutionCount.get());
        metrics.put("sync_fallbacks", syncFallbackCount.get());
        metrics.put("batch_submissions", batchSubmissionCount.get());
        metrics.put("crop_growth", cropGrowthCount.get());
        metrics.put("leaf_decay", leafDecayCount.get());
        metrics.put("redstone", redstoneCount.get());
        metrics.put("entity_interaction", entityInteractionCount.get());
        return metrics;
    }
    
    @Unique
    private static void aki$resetMetrics() {
        asyncExecutionCount.set(0);
        mainThreadExecutionCount.set(0);
        syncFallbackCount.set(0);
        batchSubmissionCount.set(0);
        cropGrowthCount.set(0);
        leafDecayCount.set(0);
        redstoneCount.set(0);
        entityInteractionCount.set(0);
    }
    
    @Unique
    private static void aki$logMetrics() {
        long total = asyncExecutionCount.get() + mainThreadExecutionCount.get();
        if (total == 0) {
            BridgeConfigCache.debugLog("[AkiAsync-BlockTick] No block ticks processed yet");
            return;
        }
        
        double asyncPercent = (asyncExecutionCount.get() * 100.0) / total;
        double mainThreadPercent = (mainThreadExecutionCount.get() * 100.0) / total;
        double fallbackPercent = syncFallbackCount.get() > 0 ? 
            (syncFallbackCount.get() * 100.0) / asyncExecutionCount.get() : 0.0;
        
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] ========== Execution Metrics ==========");
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Total ticks: " + total);
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Async executions: " + asyncExecutionCount.get() + 
            String.format(" (%.2f%%)", asyncPercent));
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Main thread executions: " + mainThreadExecutionCount.get() + 
            String.format(" (%.2f%%)", mainThreadPercent));
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Sync fallbacks: " + syncFallbackCount.get() + 
            String.format(" (%.2f%% of async)", fallbackPercent));
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Batch submissions: " + batchSubmissionCount.get());
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] --- Category Breakdown ---");
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Crop growth: " + cropGrowthCount.get());
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Leaf decay: " + leafDecayCount.get());
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Redstone: " + redstoneCount.get());
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Entity interaction: " + entityInteractionCount.get());
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] Cached categories: " + BLOCK_CATEGORY_CACHE.size());
        BridgeConfigCache.debugLog("[AkiAsync-BlockTick] ======================================");
    }

    @Unique
    private static boolean aki$isRedstoneRelatedBlock(Block block) {
        String blockId = aki$getBlockId(block);

        return blockId.contains("redstone") ||
               blockId.contains("repeater") ||
               blockId.contains("comparator") ||
               blockId.contains("piston") ||
               blockId.contains("observer") ||
               blockId.contains("dispenser") ||
               blockId.contains("dropper") ||
               blockId.contains("hopper") ||
               blockId.contains("rail") ||
               blockId.contains("door") ||
               blockId.contains("trapdoor") ||
               blockId.contains("fence_gate") ||
               blockId.contains("daylight_detector") ||
               blockId.contains("tripwire") ||
               blockId.contains("pressure_plate") ||
               blockId.contains("button") ||
               blockId.contains("lever") ||
               blockId.contains("torch") ||
               blockId.contains("lamp");
    }

    @Unique
    private static String aki$getBlockId(Block block) {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            return bridge.getBlockId(block);
        }
        return block.getClass().getSimpleName().toLowerCase();
    }

    @Unique
    private static boolean aki$requiresMainThreadExecution(Block block) {
        String blockId = aki$getBlockId(block);
        return false;
    }
    
    @Unique
    private static int aki$determinePriority(BlockTickCategory category) {
        return switch (category) {
            case REDSTONE, ENTITY_INTERACTION -> 0;
            case CROP_GROWTH -> 1;
            case LEAF_DECAY -> 2;
            case SAFE_ASYNC -> 3;
        };
    }
    
    @Unique
    private static void aki$logSmoothingStats() {
        if (smoothingScheduler != null) {
            BridgeConfigCache.debugLog("[AkiAsync-BlockTick] TaskSmoothingScheduler statistics logged");
        }
    }
}
