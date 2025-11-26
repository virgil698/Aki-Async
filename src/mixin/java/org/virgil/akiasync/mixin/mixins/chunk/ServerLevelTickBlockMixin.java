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

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void aki$asyncTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        if (!initialized) aki$initBlockTickAsync();
        if (!cached_enabled) return;

        ServerLevel level = (ServerLevel) (Object) this;
        BlockState blockState = level.getBlockState(pos);

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
            return;
        }

        try {
            ASYNC_BLOCK_TICK_EXECUTOR.execute(() -> {
                try {
                    BlockState currentState = level.getBlockState(pos);
                    if (!currentState.is(block)) {
                        return;
                    }

                    currentState.tick(level, pos, level.random);
                } catch (Throwable t) {
                    StackTraceElement[] stack = t.getStackTrace();
                    boolean isAsyncCatcherError = stack.length > 0 &&
                        stack[0].getClassName().equals("org.spigotmc.AsyncCatcher");

                    if (isAsyncCatcherError) {
                        level.getServer().execute(() -> {
                            try {
                                BlockState state = level.getBlockState(pos);
                                if (state.is(block)) {
                                    state.tick(level, pos, level.random);
                                }
                            } catch (Throwable ignored) {
                            }
                        });
                    } else {
                        org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                        if (errorBridge != null) {
                            errorBridge.errorLog("[AkiAsync-BlockTick] Async block tick failed: " + t.getMessage() +
                                " for " + block.getDescriptionId() + " at " + pos + ": " + t.getClass().getSimpleName());
                        }

                        level.getServer().execute(() -> {
                            try {
                                BlockState state = level.getBlockState(pos);
                                if (state.is(block)) {
                                    state.tick(level, pos, level.random);
                                }
                            } catch (Throwable ignored) {
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (errorBridge != null) {
                errorBridge.errorLog("[AkiAsync-BlockTick] Failed to submit async task: " + e.getMessage() +
                    ", falling back to sync execution");
            }
            return;
        }

        ci.cancel();
    }

    @Unique
    private static synchronized void aki$initBlockTickAsync() {
        if (initialized) return;

        var bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        cached_enabled = bridge != null && bridge.isChunkTickAsyncEnabled();

        if (bridge != null) {
            ASYNC_BLOCK_TICK_EXECUTOR = bridge.getGeneralExecutor();
        }

        initialized = true;
        if (bridge != null) {
            bridge.debugLog("[AkiAsync] ServerLevelTickBlockMixin initialized: enabled=" + cached_enabled);
            bridge.debugLog("[AkiAsync]   Hooked: ServerLevel#tickBlock()");
            bridge.debugLog("[AkiAsync]   Strategy: Offload blockState.tick() to Bridge executor");
            bridge.debugLog("[AkiAsync]   Risk: Thread safety depends on block implementation");
            bridge.debugLog("[AkiAsync]   Protection: Redstone blocks execute on main thread");
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