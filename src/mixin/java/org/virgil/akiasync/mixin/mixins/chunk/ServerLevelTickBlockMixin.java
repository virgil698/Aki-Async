package org.virgil.akiasync.mixin.mixins.chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Mixin(value = ServerLevel.class)
public abstract class ServerLevelTickBlockMixin {

    @Unique
    private static final boolean ENABLED = true;

    @Unique
    private static volatile ExecutorService executorService;

    @Unique
    private static synchronized ExecutorService getExecutor() {
        if (executorService == null) {
            int poolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
            executorService = new ThreadPoolExecutor(
                    poolSize,
                    poolSize,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(1000),
                    r -> {
                        Thread t = new Thread(r, "AkiAsync-Pool");
                        t.setDaemon(true);
                        return t;
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
            System.out.println("[AkiAsync] 线程池已初始化，大小: " + poolSize);
        }
        return executorService;
    }

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void aki$optimizedAsyncTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        if (!ENABLED) return;

        ServerLevel level = (ServerLevel) (Object) this;
        BlockState blockState = level.getBlockState(pos);

        if (!blockState.is(block)) {
            ci.cancel();
            return;
        }

        String blockName = block.getDescriptionId().toLowerCase();
        if (isUnsafeBlock(blockName)) {
            return;
        }

        final ServerLevel taskLevel = level;
        final BlockPos taskPos = pos;
        final BlockState taskState = blockState;

        try {
            getExecutor().execute(() -> {
                try {
                    taskState.tick(taskLevel, taskPos, taskLevel.random);
                } catch (Throwable t) {
                    handleAsyncError(taskLevel, taskPos, block, t);
                }
            });

            ci.cancel();
        } catch (Exception e) {
            System.out.println("[AkiAsync] 线程池异常，回退到同步执行: " + e.getMessage());
        }
    }

    @Unique
    private boolean isUnsafeBlock(String blockName) {
        return
                blockName.contains("water") ||
                        blockName.contains("lava") ||
                        blockName.contains("bubble") ||
                        blockName.contains("redstone") ||
                        blockName.contains("comparator") ||
                        blockName.contains("repeater") ||
                        blockName.contains("observer") ||
                        blockName.contains("piston") ||
                        blockName.contains("dispenser") ||
                        blockName.contains("dropper") ||
                        blockName.contains("hopper") ||
                        blockName.contains("lever") ||
                        blockName.contains("button") ||
                        blockName.contains("pressure_plate") ||
                        blockName.contains("tripwire") ||
                        blockName.contains("target") ||
                        blockName.contains("daylight_detector") ||
                        blockName.contains("tnt") ||
                        blockName.contains("note_block") ||
                        blockName.contains("leaves") ||
                        blockName.contains("sapling") ||
                        blockName.contains("grass") ||
                        blockName.contains("fern") ||
                        blockName.contains("flower") ||
                        blockName.contains("mushroom") ||
                        blockName.contains("vine") ||
                        blockName.contains("lily") ||
                        blockName.contains("cactus") ||
                        blockName.contains("sugar_cane") ||
                        blockName.contains("bamboo") ||
                        blockName.contains("kelp") ||
                        blockName.contains("seagrass") ||
                        blockName.contains("sea_pickle") ||
                        blockName.contains("coral") ||
                        blockName.contains("azalea") ||
                        blockName.contains("mangrove") ||
                        blockName.contains("cherry") ||
                        blockName.contains("spore_blossom") ||
                        blockName.contains("moss") ||
                        blockName.contains("chorus") ||
                        blockName.contains("portal") ||
                        blockName.contains("end_gateway") ||
                        blockName.contains("dragon_egg") ||
                        blockName.contains("sponge") ||
                        blockName.contains("cake") ||
                        blockName.contains("scaffolding") ||
                        blockName.contains("pointed_dripstone") ||
                        blockName.contains("lightning_rod") ||
                        blockName.contains("powder_snow") ||
                        blockName.contains("amethyst_cluster") ||
                        blockName.contains("budding_amethyst") ||
                        blockName.contains("calibrated_sculk_sensor") ||
                        blockName.contains("reinforced_deepslate") ||
                        blockName.contains("decorated_pot") ||
                        blockName.contains("suspicious_sand") ||
                        blockName.contains("suspicious_gravel") ||
                        blockName.contains("trial_spawner") ||
                        blockName.contains("vault") ;
    }

    @Unique
    private void handleAsyncError(ServerLevel level, BlockPos pos, Block block, Throwable t) {
        if (isAsyncError(t)) {
            level.getServer().execute(() -> {
                try {
                    BlockState current = level.getBlockState(pos);
                    if (current.is(block)) {
                        current.tick(level, pos, level.random);
                        System.out.println("[AkiAsync] 异步失败，已回退到同步执行: " + block);
                    }
                } catch (Throwable ignored) {}
            });
        }
    }

    @Unique
    private boolean isAsyncError(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        String className = t.getClass().getName();
        return (msg != null && (
                msg.contains("async") ||
                        msg.contains("main thread") ||
                        msg.contains("thread")
        )) || className.contains("AsyncCatcher");
    }
}
