package com.akiasync.scheduler;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

final class PaperOwnerScheduler implements AutoCloseable {
    private final Plugin plugin;
    private final Server server;
    private final int capacity;
    private final LongSupplier generationSupplier;
    private final AtomicBoolean active = new AtomicBoolean();
    private final AtomicInteger pendingCount = new AtomicInteger();
    private final AtomicLong taskIds = new AtomicLong();
    private final ConcurrentHashMap<Long, OwnerTask<?>> pending = new ConcurrentHashMap<>();
    private volatile long generation;

    PaperOwnerScheduler(Plugin plugin, int capacity, LongSupplier generationSupplier) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        server = plugin.getServer();
        this.capacity = capacity;
        this.generationSupplier = Objects.requireNonNull(generationSupplier, "generationSupplier");
    }

    void start(long currentGeneration) {
        generation = currentGeneration;
        if (!active.compareAndSet(false, true)) {
            throw new IllegalStateException("Owner scheduler is already running");
        }
    }

    <T> CompletableFuture<T> global(Supplier<T> commit) {
        return schedule(commit, task -> server.getGlobalRegionScheduler().execute(plugin, task));
    }

    <T> CompletableFuture<T> region(World world, int chunkX, int chunkZ, Supplier<T> commit) {
        Objects.requireNonNull(world, "world");
        return schedule(commit, task -> server.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, task));
    }

    <T> CompletableFuture<T> entity(Entity entity, Supplier<T> commit) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(commit, "commit");
        OwnerTask<T> task = create(commit);
        if (task == null) {
            return failedCapacityFuture();
        }
        try {
            boolean scheduled = entity.getScheduler().execute(
                    plugin,
                    task,
                    () -> task.cancel("Entity retired before the commit could run"),
                    1L
            );
            if (!scheduled) {
                task.cancel("Entity retired before the commit could be scheduled");
            }
        } catch (RuntimeException | Error failure) {
            task.failBeforeRun(failure);
        }
        return task.future;
    }

    int pendingCount() {
        return pendingCount.get();
    }

    @Override
    public void close() {
        if (!active.compareAndSet(true, false)) {
            return;
        }
        generation++;
        for (OwnerTask<?> task : new ArrayList<>(pending.values())) {
            task.cancel("Plugin scheduler stopped before the commit could run");
        }
    }

    private <T> CompletableFuture<T> schedule(Supplier<T> commit, Dispatcher dispatcher) {
        Objects.requireNonNull(commit, "commit");
        OwnerTask<T> task = create(commit);
        if (task == null) {
            return failedCapacityFuture();
        }
        try {
            dispatcher.dispatch(task);
        } catch (RuntimeException | Error failure) {
            task.failBeforeRun(failure);
        }
        return task.future;
    }

    private <T> OwnerTask<T> create(Supplier<T> commit) {
        if (!active.get() || !reserve()) {
            return null;
        }
        long id = taskIds.incrementAndGet();
        OwnerTask<T> task = new OwnerTask<>(id, generation, commit);
        pending.put(id, task);
        if (!active.get() || task.generation != generation) {
            task.cancel("Plugin scheduler stopped while the commit was being scheduled");
        }
        return task;
    }

    private boolean reserve() {
        while (true) {
            int current = pendingCount.get();
            if (current >= capacity) {
                return false;
            }
            if (pendingCount.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    private <T> CompletableFuture<T> failedCapacityFuture() {
        return CompletableFuture.failedFuture(new RejectedExecutionException(
                active.get() ? "Owner-thread commit capacity is exhausted" : "Owner scheduler is not running"
        ));
    }

    private void release(OwnerTask<?> task) {
        if (pending.remove(task.id, task)) {
            pendingCount.decrementAndGet();
        }
    }

    @FunctionalInterface
    private interface Dispatcher {
        void dispatch(Runnable task);
    }

    private final class OwnerTask<T> implements Runnable {
        private static final int PENDING = 0;
        private static final int RUNNING = 1;
        private static final int TERMINAL = 2;

        private final long id;
        private final long generation;
        private final Supplier<T> commit;
        private final CompletableFuture<T> future = new CompletableFuture<>();
        private final AtomicInteger state = new AtomicInteger(PENDING);

        private OwnerTask(long id, long generation, Supplier<T> commit) {
            this.id = id;
            this.generation = generation;
            this.commit = commit;
        }

        @Override
        public void run() {
            if (!state.compareAndSet(PENDING, RUNNING)) {
                return;
            }
            if (!active.get()
                    || generation != PaperOwnerScheduler.this.generation
                    || generation != generationSupplier.getAsLong()) {
                future.cancel(false);
                state.set(TERMINAL);
                release(this);
                return;
            }
            try {
                future.complete(commit.get());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            } finally {
                state.set(TERMINAL);
                release(this);
            }
        }

        private void cancel(String message) {
            if (state.compareAndSet(PENDING, TERMINAL)) {
                future.completeExceptionally(new CancellationException(message));
                release(this);
            }
        }

        private void failBeforeRun(Throwable failure) {
            if (state.compareAndSet(PENDING, TERMINAL)) {
                future.completeExceptionally(failure);
                release(this);
            }
        }
    }
}
