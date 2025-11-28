package org.virgil.akiasync.mixin.mixins.chunk;

import java.util.concurrent.ExecutorService;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
@Mixin(value = ServerLevel.class, priority = 1200)
public abstract class ServerLevelTickBlockMixin {
    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean initialized = false;
    @Unique private static ExecutorService ASYNC_BLOCK_TICK_EXECUTOR;
    
    @Unique private static final java.util.concurrent.atomic.AtomicLong asyncExecutionCount = new java.util.concurrent.atomic.AtomicLong(0);
    @Unique private static final java.util.concurrent.atomic.AtomicLong syncFallbackCount = new java.util.concurrent.atomic.AtomicLong(0);
    @Unique private static final java.util.concurrent.atomic.AtomicLong executorUnavailableCount = new java.util.concurrent.atomic.AtomicLong(0);
    @Unique private static final java.util.concurrent.atomic.AtomicLong asyncCatcherFallbackCount = new java.util.concurrent.atomic.AtomicLong(0);
    @Unique private static final java.util.concurrent.atomic.AtomicLong submissionFailureCount = new java.util.concurrent.atomic.AtomicLong(0);

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void aki$asyncTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        
        if (pos == null || block == null) {
            return;
        }
        
        if (!initialized) aki$initBlockTickAsync();
        
        if (!cached_enabled) {
            return;
        }

        ServerLevel level = (ServerLevel) (Object) this;
        BlockState blockState = level.getBlockState(pos);
        
        if (blockState == null) {
            return;
        }

        if (!blockState.is(block)) {
            ci.cancel();
            return;
        }

        if (aki$isRedstoneRelatedBlock(block)) {
            return;
        }

        if (aki$isFoliaEnvironment()) {
            if (aki$requiresMainThreadInFolia(block)) {
                return;
            }
        }

        if (ASYNC_BLOCK_TICK_EXECUTOR == null || ASYNC_BLOCK_TICK_EXECUTOR.isShutdown()) {
            
            executorUnavailableCount.incrementAndGet();
            
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.errorLog("[AkiAsync-BlockTick] Executor unavailable (null or shutdown) for block " + 
                    aki$getBlockId(block) + " at " + pos + ", allowing sync execution");
            }
            
            return;
        }

        try {
            ASYNC_BLOCK_TICK_EXECUTOR.execute(() -> {
                try {
                    
                    BlockState currentState = level.getBlockState(pos);
                    if (currentState == null || !currentState.is(block)) {
                        return;
                    }

                    if (level.random == null) {
                        return;
                    }

                    currentState.tick(level, pos, level.random);
                } catch (Throwable t) {
                    
                    StackTraceElement[] stack = t.getStackTrace();
                    boolean isAsyncCatcherError = stack.length > 0 &&
                        stack[0].getClassName().equals("org.spigotmc.AsyncCatcher");

                    org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    
                    if (isAsyncCatcherError) {
                        
                        asyncCatcherFallbackCount.incrementAndGet();
                        syncFallbackCount.incrementAndGet();
                        
                        if (errorBridge != null) {
                            errorBridge.debugLog("[AkiAsync-BlockTick] [AsyncCatcher] Block " + 
                                aki$getBlockId(block) + " at " + pos + " requires main thread, falling back to sync");
                        }
                        
                        level.getServer().execute(() -> {
                            try {
                                BlockState state = level.getBlockState(pos);
                                if (state != null && state.is(block) && level.random != null) {
                                    state.tick(level, pos, level.random);
                                }
                            } catch (Throwable ignored) {
                            }
                        });
                    } else {
                        
                        syncFallbackCount.incrementAndGet();
                        
                        if (errorBridge != null) {
                            errorBridge.errorLog("[AkiAsync-BlockTick] Async execution failed for block " + 
                                aki$getBlockId(block) + " at " + pos + ": " + 
                                t.getClass().getSimpleName() + " - " + t.getMessage() + ", falling back to sync");
                        }

                        level.getServer().execute(() -> {
                            try {
                                BlockState state = level.getBlockState(pos);
                                if (state != null && state.is(block) && level.random != null) {
                                    state.tick(level, pos, level.random);
                                }
                            } catch (Throwable ignored) {
                            }
                        });
                    }
                }
            });
            
            asyncExecutionCount.incrementAndGet();
            ci.cancel();
            
        } catch (Exception e) {
            
            submissionFailureCount.incrementAndGet();
            
            org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (errorBridge != null) {
                errorBridge.errorLog("[AkiAsync-BlockTick] Failed to submit async task for block " + 
                    aki$getBlockId(block) + " at " + pos + ": " + e.getMessage() + ", allowing sync execution");
            }
            
            return;
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
        
        boolean previousEnabled = cached_enabled;
        cached_enabled = bridge != null && bridge.isChunkTickAsyncEnabled();

        if (bridge != null) {
            ASYNC_BLOCK_TICK_EXECUTOR = bridge.getGeneralExecutor();
        }

        initialized = true;
        
        if (bridge != null) {
            if (wasInitialized) {
                
                bridge.debugLog("[AkiAsync] ServerLevelTickBlockMixin reloading...");
                aki$logMetrics();
                aki$resetMetrics();
                
                bridge.debugLog("[AkiAsync] ServerLevelTickBlockMixin reloaded: enabled=" + cached_enabled + 
                    " (was: " + previousEnabled + ")");
                if (previousEnabled != cached_enabled) {
                    bridge.debugLog("[AkiAsync]   Configuration changed: " + 
                        (cached_enabled ? "async block ticks ENABLED" : "async block ticks DISABLED"));
                }
            } else {
                
                bridge.debugLog("[AkiAsync] ServerLevelTickBlockMixin initialized: enabled=" + cached_enabled);
                bridge.debugLog("[AkiAsync]   Hooked: ServerLevel#tickBlock()");
                bridge.debugLog("[AkiAsync]   Strategy: Offload blockState.tick() to Bridge executor");
                bridge.debugLog("[AkiAsync]   Risk: Thread safety depends on block implementation");
                bridge.debugLog("[AkiAsync]   Protection: Redstone blocks execute on main thread");
            }
        }
    }

    @Unique
    private static void aki$reloadConfiguration() {
        aki$initBlockTickAsync(true);
    }
    
    @Unique
    private static java.util.Map<String, Long> aki$getMetrics() {
        java.util.Map<String, Long> metrics = new java.util.LinkedHashMap<>();
        metrics.put("async_executions", asyncExecutionCount.get());
        metrics.put("sync_fallbacks", syncFallbackCount.get());
        metrics.put("executor_unavailable", executorUnavailableCount.get());
        metrics.put("async_catcher_fallbacks", asyncCatcherFallbackCount.get());
        metrics.put("submission_failures", submissionFailureCount.get());
        return metrics;
    }
    
    @Unique
    private static void aki$resetMetrics() {
        asyncExecutionCount.set(0);
        syncFallbackCount.set(0);
        executorUnavailableCount.set(0);
        asyncCatcherFallbackCount.set(0);
        submissionFailureCount.set(0);
    }
    
    @Unique
    private static void aki$logMetrics() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            long total = asyncExecutionCount.get() + syncFallbackCount.get() + executorUnavailableCount.get() + submissionFailureCount.get();
            if (total == 0) {
                bridge.debugLog("[AkiAsync-BlockTick] No block ticks processed yet");
                return;
            }
            
            double asyncPercent = (asyncExecutionCount.get() * 100.0) / total;
            double fallbackPercent = (syncFallbackCount.get() * 100.0) / total;
            
            bridge.debugLog("[AkiAsync-BlockTick] ========== Execution Metrics ==========");
            bridge.debugLog("[AkiAsync-BlockTick] Total ticks processed: " + total);
            bridge.debugLog("[AkiAsync-BlockTick] Async executions: " + asyncExecutionCount.get() + 
                String.format(" (%.2f%%)", asyncPercent));
            bridge.debugLog("[AkiAsync-BlockTick] Sync fallbacks: " + syncFallbackCount.get() + 
                String.format(" (%.2f%%)", fallbackPercent));
            bridge.debugLog("[AkiAsync-BlockTick]   - AsyncCatcher: " + asyncCatcherFallbackCount.get());
            bridge.debugLog("[AkiAsync-BlockTick]   - Executor unavailable: " + executorUnavailableCount.get());
            bridge.debugLog("[AkiAsync-BlockTick]   - Submission failures: " + submissionFailureCount.get());
            bridge.debugLog("[AkiAsync-BlockTick] ======================================");
        }
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
    private static boolean aki$isFoliaEnvironment() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        return bridge != null && bridge.isFoliaEnvironment();
    }

    @Unique
    private static boolean aki$requiresMainThreadInFolia(Block block) {
        String blockId = aki$getBlockId(block);

        return blockId.contains("command") ||
               blockId.contains("structure") ||
               blockId.contains("jigsaw") ||
               blockId.contains("barrier") ||
               blockId.contains("bedrock");
    }

    @Unique
    private static String aki$getBlockId(Block block) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            return bridge.getBlockId(block);
        }
        return block.getClass().getSimpleName().toLowerCase();
    }
}
