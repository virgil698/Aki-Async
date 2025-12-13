package org.virgil.akiasync.mixin.async.structure;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class StructureLocateTask<T> {

    private final MinecraftServer server;
    private final CompletableFuture<T> completableFuture;
    private final Future<?> taskFuture;
    private final long startTime;

    public StructureLocateTask(MinecraftServer server, CompletableFuture<T> completableFuture, Future<?> taskFuture) {
        this.server = server;
        this.completableFuture = completableFuture;
        this.taskFuture = taskFuture;
        this.startTime = System.nanoTime();
    }

    public StructureLocateTask<T> then(Consumer<T> action) {
        completableFuture.thenAccept(action);
        return this;
    }

    public StructureLocateTask<T> thenOnServerThread(Consumer<T> action) {
        completableFuture.thenAccept(result ->
            server.submit(() -> action.accept(result))
        );
        return this;
    }

    public StructureLocateTask<T> onError(Consumer<Throwable> errorHandler) {
        completableFuture.exceptionally(throwable -> {
            errorHandler.accept(throwable);
            return null;
        });
        return this;
    }

    public StructureLocateTask<T> onErrorOnServerThread(Consumer<Throwable> errorHandler) {
        completableFuture.exceptionally(throwable -> {
            server.submit(() -> errorHandler.accept(throwable));
            return null;
        });
        return this;
    }

    public void cancel() {
        taskFuture.cancel(true);
        completableFuture.cancel(false);
    }

    public boolean isDone() {
        return completableFuture.isDone();
    }

    public boolean isCancelled() {
        return completableFuture.isCancelled();
    }

    public long getElapsedTimeMs() {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    public CompletableFuture<T> getCompletableFuture() {
        return completableFuture;
    }

    public static StructureLocateTask<BlockPos> forBlockPos(
        MinecraftServer server,
        CompletableFuture<BlockPos> future,
        Future<?> taskFuture
    ) {
        return new StructureLocateTask<>(server, future, taskFuture);
    }

    public static StructureLocateTask<Pair<BlockPos, Holder<Structure>>> forStructurePair(
        MinecraftServer server,
        CompletableFuture<Pair<BlockPos, Holder<Structure>>> future,
        Future<?> taskFuture
    ) {
        return new StructureLocateTask<>(server, future, taskFuture);
    }
}
