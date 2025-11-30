package org.virgil.akiasync.mixin.mixins.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = ServerLevel.class)
public abstract class ServerLevelTickBlockMixin {

    @Unique
    private static final boolean ENABLED = true;
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("AkiAsync");

    @Unique
    private static final Set<String> WHITELIST;

    static {
        Set<String> whitelist = new HashSet<>();
        addToWhitelist(whitelist, "sculk_sensor");
        addToWhitelist(whitelist, "calibrated_sculk_sensor");
        addToWhitelist(whitelist, "lightning_rod");
        addToWhitelist(whitelist, "trial_spawner");
        addToWhitelist(whitelist, "vault");

        WHITELIST = Collections.unmodifiableSet(whitelist);
    }

    @Unique
    private static void addToWhitelist(Set<String> whitelist, String blockName) {
        whitelist.add(blockName);
    }

    @Shadow @Final private MinecraftServer server;

    @Unique
    private ExecutorService akiAsyncExecutor;

    @Unique
    private final AtomicInteger pendingTasks = new AtomicInteger(0);

    @Unique
    private static final int MAX_PENDING_TASKS = 256;
    @Unique
    private static final int MAX_QUEUE_SIZE = 4096;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors() / 4);
        this.akiAsyncExecutor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                r -> {
                    Thread t = new Thread(r, "AkiAsync-Pool-" + ((ServerLevel) (Object) this).dimension().location());
                    t.setDaemon(true);
                    return t;
                },
                (r, executor) -> r.run()
        );
        LOGGER.info("[AkiAsync] 为世界 {} 初始化线程池: 线程数={}, 队列大小={}", ((ServerLevel)(Object)this).dimension().location(), poolSize, MAX_QUEUE_SIZE);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        if (this.akiAsyncExecutor != null) {
            this.akiAsyncExecutor.shutdown();
            try {
                if (!this.akiAsyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.akiAsyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.akiAsyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOGGER.info("[AkiAsync] 已关闭世界 {} 的线程池", ((ServerLevel)(Object)this).dimension().location());
        }
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
        if (!WHITELIST.contains(blockName)) {
            return;
        }

        if (pendingTasks.get() >= MAX_PENDING_TASKS) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[AkiAsync] 流量控制：世界 {} 的待处理任务已满 ({}), 回退到主线程", level.dimension().location(), MAX_PENDING_TASKS);
            }
            return;
        }

        pendingTasks.incrementAndGet();

        final ServerLevel taskLevel = level;
        final BlockPos taskPos = pos;
        final BlockState taskState = blockState;

        try {
            this.akiAsyncExecutor.execute(() -> {
                try {
                    taskState.tick(taskLevel, taskPos, taskLevel.random);
                } catch (Throwable t) {
                    LOGGER.error("[AkiAsync] 白名单方块 {} 在异步 tick 时发生严重错误！这可能是线程安全问题。位置: {}", blockName, taskPos, t);
                } finally {
                    pendingTasks.decrementAndGet();
                }
            });

            ci.cancel();
        } catch (Exception e) {
            pendingTasks.decrementAndGet();
            LOGGER.warn("[AkiAsync] 任务提交失败，回退到同步执行: {}", e.getMessage());
        }
    }
}
