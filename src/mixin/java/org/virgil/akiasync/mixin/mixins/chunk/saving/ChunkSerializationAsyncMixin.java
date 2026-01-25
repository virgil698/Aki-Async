package org.virgil.akiasync.mixin.mixins.chunk.saving;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Pseudo
@Mixin(targets = "net.minecraft.world.level.chunk.storage.SerializableChunkData", remap = false)
public class ChunkSerializationAsyncMixin {

    @Unique
    private static volatile Executor SERIALIZATION_EXECUTOR = null;

    @Unique
    private static final ThreadLocal<Queue<Runnable>> MAIN_THREAD_QUEUE = ThreadLocal.withInitial(ArrayDeque::new);

    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile long asyncSerializations = 0L;

    @Unique
    private static volatile long lastLogTime = 0L;

    @Unique
    private static Executor akiasync$getExecutor() {
        if (SERIALIZATION_EXECUTOR == null) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                SERIALIZATION_EXECUTOR = bridge.getGeneralExecutor();
            }
        }
        return SERIALIZATION_EXECUTOR;
    }

    @Unique
    private static void akiasync$executeOnMainThread(Runnable task) {
        Queue<Runnable> queue = MAIN_THREAD_QUEUE.get();
        if (queue != null) {
            queue.add(task);
        } else {
            task.run();
        }
    }

    @Unique
    private static void akiasync$pushMainThreadQueue(Queue<Runnable> queue) {
        MAIN_THREAD_QUEUE.set(queue);
    }

    @Unique
    private static void akiasync$popMainThreadQueue(Queue<Runnable> queue) {
        if (MAIN_THREAD_QUEUE.get() == queue) {
            MAIN_THREAD_QUEUE.remove();
        }
    }

    @Unique
    private static void akiasync$drainMainThreadQueue(Queue<Runnable> queue) {
        Runnable task;
        while ((task = queue.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                BridgeConfigCache.debugLog("[AkiAsync-ChunkSerial] Error executing main thread task: " + e.getMessage());
            }
        }
    }

    @Unique
    private static <T> CompletableFuture<T> akiasync$serializeAsync(java.util.function.Supplier<T> task) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return CompletableFuture.completedFuture(task.get());
        }

        Executor executor = akiasync$getExecutor();
        if (executor == null) {
            return CompletableFuture.completedFuture(task.get());
        }

        asyncSerializations++;
        akiasync$logStatistics();

        return CompletableFuture.supplyAsync(task, executor);
    }

    @Unique
    private static void akiasync$logStatistics() {
        if (!initialized) {
            initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-ChunkSerial] Chunk serialization async optimization enabled");
            BridgeConfigCache.debugLog("[AkiAsync-ChunkSerial] Using unified thread pool");
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > 60000) {
            lastLogTime = currentTime;
            BridgeConfigCache.debugLog(String.format(
                "[AkiAsync-ChunkSerial] Stats - Async serializations: %d",
                asyncSerializations
            ));
        }
    }
}
