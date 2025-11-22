package org.virgil.akiasync.mixin.mixins.chunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

@Mixin(value = ServerLevel.class)
public abstract class ServerLevelTickBlockMixin {

    @Unique
    private static final boolean ENABLED = true;

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("AkiAsync");

    @Unique
    private static final AtomicInteger pendingTasks = new AtomicInteger(0);

    @Unique
    private static final int MAX_PENDING_TASKS = 50;

    @Unique
    private static final int MAX_QUEUE_SIZE = 4096;

    @Unique
    private static long totalTasksSubmitted = 0;
    @Unique
    private static long totalTasksRejected = 0;

    // 内联黑名单 - 静态黑名单
    @Unique
    private static final Set<String> STATIC_BLACKLIST;

    // 内联黑名单 - 动态黑名单（线程安全）
    @Unique
    private static final Set<String> DYNAMIC_BLACKLIST = Collections.synchronizedSet(new HashSet<>());

    static {
        Set<String> blacklist = new HashSet<>();

        // === 液体和流动方块 ===
        addToBlacklist(blacklist, "water");
        addToBlacklist(blacklist, "lava");
        addToBlacklist(blacklist, "bubble");
        addToBlacklist(blacklist, "flowing");

        // === 重力方块 ===
        addToBlacklist(blacklist, "sand");
        addToBlacklist(blacklist, "gravel");
        addToBlacklist(blacklist, "concrete_powder");
        addToBlacklist(blacklist, "anvil");
        addToBlacklist(blacklist, "scaffolding");
        addToBlacklist(blacklist, "dragon_egg");

        // === 红石相关 ===
        addToBlacklist(blacklist, "redstone");
        addToBlacklist(blacklist, "comparator");
        addToBlacklist(blacklist, "repeater");
        addToBlacklist(blacklist, "observer");
        addToBlacklist(blacklist, "piston");
        addToBlacklist(blacklist, "dispenser");
        addToBlacklist(blacklist, "dropper");
        addToBlacklist(blacklist, "hopper");
        addToBlacklist(blacklist, "lever");
        addToBlacklist(blacklist, "button");
        addToBlacklist(blacklist, "pressure_plate");
        addToBlacklist(blacklist, "tripwire");
        addToBlacklist(blacklist, "target");
        addToBlacklist(blacklist, "daylight_detector");
        addToBlacklist(blacklist, "tnt");
        addToBlacklist(blacklist, "note_block");

        // === 火和光源 ===
        addToBlacklist(blacklist, "fire");
        addToBlacklist(blacklist, "torch");
        addToBlacklist(blacklist, "lantern");
        addToBlacklist(blacklist, "campfire");

        // === 植物和自然方块 ===
        addToBlacklist(blacklist, "leaves");
        addToBlacklist(blacklist, "sapling");
        addToBlacklist(blacklist, "grass");
        addToBlacklist(blacklist, "big_dripleaf");
        addToBlacklist(blacklist, "dripleaf");
        addToBlacklist(blacklist, "fern");
        addToBlacklist(blacklist, "flower");
        addToBlacklist(blacklist, "mushroom");
        addToBlacklist(blacklist, "vine");
        addToBlacklist(blacklist, "lily");
        addToBlacklist(blacklist, "cactus");
        addToBlacklist(blacklist, "sugar_cane");
        addToBlacklist(blacklist, "bamboo");
        addToBlacklist(blacklist, "kelp");
        addToBlacklist(blacklist, "seagrass");
        addToBlacklist(blacklist, "sea_pickle");
        addToBlacklist(blacklist, "coral");
        addToBlacklist(blacklist, "azalea");
        addToBlacklist(blacklist, "mangrove");
        addToBlacklist(blacklist, "cherry");
        addToBlacklist(blacklist, "spore_blossom");
        addToBlacklist(blacklist, "moss");
        addToBlacklist(blacklist, "chorus");
        addToBlacklist(blacklist, "eyeblossom");

        // === 功能方块 ===
        addToBlacklist(blacklist, "command");
        addToBlacklist(blacklist, "structure");
        addToBlacklist(blacklist, "spawner");
        addToBlacklist(blacklist, "bed");
        addToBlacklist(blacklist, "door");
        addToBlacklist(blacklist, "trapdoor");
        addToBlacklist(blacklist, "fence_gate");
        addToBlacklist(blacklist, "chest");
        addToBlacklist(blacklist, "barrel");
        addToBlacklist(blacklist, "furnace");
        addToBlacklist(blacklist, "enchanting_table");
        addToBlacklist(blacklist, "beacon");
        addToBlacklist(blacklist, "conduit");
        addToBlacklist(blacklist, "bell");

        // === 跨维度相关 ===
        addToBlacklist(blacklist, "portal");
        addToBlacklist(blacklist, "end_gateway");

        // === 特殊机制方块 ===
        addToBlacklist(blacklist, "dragon_egg");
        addToBlacklist(blacklist, "sponge");
        addToBlacklist(blacklist, "cake");
        addToBlacklist(blacklist, "sculk");
        addToBlacklist(blacklist, "magma");
        addToBlacklist(blacklist, "soul");
        addToBlacklist(blacklist, "crying_obsidian");
        addToBlacklist(blacklist, "copper");
        addToBlacklist(blacklist, "farmland");
        addToBlacklist(blacklist, "composter");
        addToBlacklist(blacklist, "bee_nest");
        addToBlacklist(blacklist, "candle");
        addToBlacklist(blacklist, "rail");
        addToBlacklist(blacklist, "pointed_dripstone");
        addToBlacklist(blacklist, "lightning_rod");
        addToBlacklist(blacklist, "powder_snow");
        addToBlacklist(blacklist, "amethyst_cluster");
        addToBlacklist(blacklist, "budding_amethyst");
        addToBlacklist(blacklist, "calibrated_sculk_sensor");
        addToBlacklist(blacklist, "reinforced_deepslate");
        addToBlacklist(blacklist, "decorated_pot");
        addToBlacklist(blacklist, "suspicious_sand");
        addToBlacklist(blacklist, "suspicious_gravel");
        addToBlacklist(blacklist, "trial_spawner");
        addToBlacklist(blacklist, "vault");

        STATIC_BLACKLIST = Collections.unmodifiableSet(blacklist);
    }

    @Unique
    private static void addToBlacklist(Set<String> blacklist, String blockName) {
        blacklist.add(blockName);
    }

    @Unique
    private static volatile ExecutorService executorService;

    @Unique
    private static synchronized ExecutorService getExecutor() {
        if (executorService == null) {
            int poolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
            executorService = new ThreadPoolExecutor(
                    poolSize,
                    poolSize,
                    30L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                    r -> {
                        Thread t = new Thread(r, "AkiAsync-Pool");
                        t.setDaemon(true);
                        return t;
                    },
                    (r, executor) -> {
                        totalTasksRejected++;
                        if (totalTasksRejected % 100 == 0) {
                            LOGGER.warn("线程池队列已满，已拒绝 {} 个任务，回退到主线程执行", totalTasksRejected);
                        }
                    }
            );
            LOGGER.info("[AkiAsync] 线程池已初始化: 线程数={}, 队列大小={}, 最大待处理任务={}",
                    poolSize, MAX_QUEUE_SIZE, MAX_PENDING_TASKS);
        }
        return executorService;
    }

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void aki$controlledAsyncTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        if (!ENABLED) return;

        ServerLevel level = (ServerLevel) (Object) this;
        BlockState blockState = level.getBlockState(pos);

        if (!blockState.is(block)) {
            ci.cancel();
            return;
        }

        String blockName = block.getDescriptionId().toLowerCase();

        // 使用内联的黑名单检查
        if (isBlockBlacklisted(blockName)) {
            return;
        }

        int currentPending = pendingTasks.get();
        if (currentPending >= MAX_PENDING_TASKS) {
            totalTasksRejected++;
            if (totalTasksRejected % 100 == 0) {
                LOGGER.warn("流量控制：已拒绝 {} 个任务，当前待处理: {}", totalTasksRejected, currentPending);
            }
            return;
        }

        pendingTasks.incrementAndGet();
        totalTasksSubmitted++;

        final ServerLevel taskLevel = level;
        final BlockPos taskPos = pos;
        final BlockState taskState = blockState;

        try {
            getExecutor().execute(() -> {
                try {
                    taskState.tick(taskLevel, taskPos, taskLevel.random);
                } catch (Throwable t) {
                    handleAsyncError(taskLevel, taskPos, block, t);
                } finally {
                    pendingTasks.decrementAndGet();
                }
            });

            ci.cancel();
        } catch (Exception e) {
            pendingTasks.decrementAndGet();
            LOGGER.warn("任务提交失败，回退到同步执行: {}", e.getMessage());
        }
    }

    @Unique
    private boolean isBlockBlacklisted(String blockName) {
        // 检查静态黑名单
        for (String blacklisted : STATIC_BLACKLIST) {
            if (blockName.contains(blacklisted)) {
                return true;
            }
        }
        // 检查动态黑名单
        synchronized (DYNAMIC_BLACKLIST) {
            for (String blacklisted : DYNAMIC_BLACKLIST) {
                if (blockName.contains(blacklisted)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Unique
    private void handleAsyncError(ServerLevel level, BlockPos pos, Block block, Throwable t) {
        if (isAsyncError(t)) {
            // 将导致错误的方块添加到动态黑名单
            String blockName = block.getDescriptionId().toLowerCase();
            synchronized (DYNAMIC_BLACKLIST) {
                DYNAMIC_BLACKLIST.add(blockName);
            }
            LOGGER.warn("检测到不安全的异步方块tick，已加入动态黑名单: {}", blockName);

            level.getServer().execute(() -> {
                try {
                    BlockState current = level.getBlockState(pos);
                    if (current.is(block)) {
                        current.tick(level, pos, level.random);
                        if (Math.random() < 0.01) {
                            LOGGER.warn("异步失败，已回退到同步执行: {} at {}", block, pos);
                        }
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