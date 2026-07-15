package com.akiasync.scheduler;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class AkiScheduler implements AutoCloseable {
    private final RedisTaskScheduler taskScheduler;
    private final PaperOwnerScheduler ownerScheduler;

    public AkiScheduler(Plugin plugin, SchedulerConfig config) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(config, "config");
        taskScheduler = new RedisTaskScheduler(
                "Aki-Async",
                config,
                failure -> plugin.getLogger().log(Level.SEVERE, "Internal scheduler failure", failure)
        );
        ownerScheduler = new PaperOwnerScheduler(plugin, config.maxOwnerTasks(), taskScheduler::generation);
    }

    public void start() {
        taskScheduler.start();
        ownerScheduler.start(taskScheduler.generation());
    }

    /**
     * Submits a bounded tree of pure computation. Tasks must not access mutable Bukkit or Minecraft state.
     */
    public <T> TaskTreeHandle<T> submit(TaskTree<T> tree) {
        return taskScheduler.submit(tree);
    }

    public long generation() {
        return taskScheduler.generation();
    }

    public boolean isGenerationCurrent(long expectedGeneration) {
        return taskScheduler.isGenerationCurrent(expectedGeneration);
    }

    public <T> CompletableFuture<T> commitGlobal(Supplier<T> commit) {
        return ownerScheduler.global(commit);
    }

    public CompletableFuture<Void> commitGlobal(Runnable commit) {
        Objects.requireNonNull(commit, "commit");
        return commitGlobal(() -> {
            commit.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> commitRegion(
            World world,
            int chunkX,
            int chunkZ,
            Supplier<T> commit
    ) {
        return ownerScheduler.region(world, chunkX, chunkZ, commit);
    }

    public CompletableFuture<Void> commitRegion(
            World world,
            int chunkX,
            int chunkZ,
            Runnable commit
    ) {
        Objects.requireNonNull(commit, "commit");
        return commitRegion(world, chunkX, chunkZ, () -> {
            commit.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> commitEntity(Entity entity, Supplier<T> commit) {
        return ownerScheduler.entity(entity, commit);
    }

    public CompletableFuture<Void> commitEntity(Entity entity, Runnable commit) {
        Objects.requireNonNull(commit, "commit");
        return commitEntity(entity, () -> {
            commit.run();
            return null;
        });
    }

    public SchedulerSnapshot snapshot() {
        return taskScheduler.snapshot(ownerScheduler.pendingCount());
    }

    @Override
    public void close() {
        ownerScheduler.close();
        taskScheduler.close();
    }
}
