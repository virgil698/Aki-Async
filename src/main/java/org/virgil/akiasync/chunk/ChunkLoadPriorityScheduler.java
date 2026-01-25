package org.virgil.akiasync.chunk;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.virgil.akiasync.config.ConfigManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkLoadPriorityScheduler {

    private final ConfigManager config;
    private final ExecutorService loadExecutor;
    private final PriorityBlockingQueue<ChunkLoadTask> taskQueue;
    private final Map<UUID, PlayerLoadStats> playerStats;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    private final AtomicInteger totalLoaded = new AtomicInteger(0);
    private final AtomicInteger totalQueued = new AtomicInteger(0);
    private final AtomicInteger totalSkipped = new AtomicInteger(0);

    public ChunkLoadPriorityScheduler(ConfigManager config) {
        this.config = config;
        this.taskQueue = new PriorityBlockingQueue<>(1000, Comparator.comparingInt(ChunkLoadTask::getPriority).reversed());
        this.playerStats = new ConcurrentHashMap<>();

        int threads = Math.max(2, Runtime.getRuntime().availableProcessors() / 4);
        this.loadExecutor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "AkiAsync-ChunkLoader");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            t.setDaemon(true);
            return t;
        });

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AkiAsync-ChunkScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (running) return;
        running = true;

        for (int i = 0; i < Math.max(2, Runtime.getRuntime().availableProcessors() / 4); i++) {
            loadExecutor.submit(this::processQueue);
        }

        scheduler.scheduleAtFixedRate(this::cleanup, 10, 10, TimeUnit.SECONDS);
    }

    public void shutdown() {
        running = false;
        loadExecutor.shutdown();
        scheduler.shutdown();
        try {
            if (!loadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                loadExecutor.shutdownNow();
            }
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            loadExecutor.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void submitChunkLoad(Player player, World world, int chunkX, int chunkZ, int priority, double speed) {
        if (!running || player == null || world == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        PlayerLoadStats stats = playerStats.computeIfAbsent(playerId, k -> new PlayerLoadStats());

        if (stats.shouldThrottle()) {
            totalSkipped.incrementAndGet();
            return;
        }

        ChunkLoadTask task = new ChunkLoadTask(player, world, chunkX, chunkZ, priority, speed, System.currentTimeMillis());

        if (taskQueue.offer(task)) {
            totalQueued.incrementAndGet();
            stats.recordQueued();
        }
    }

    private void processQueue() {
        int batchCount = 0;
        int batchSize = config.getAsyncLoadingBatchSize();
        long batchDelay = config.getAsyncLoadingBatchDelayMs();

        while (running) {
            try {
                ChunkLoadTask task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                if (task == null) continue;

                if (System.currentTimeMillis() - task.getQueueTime() > 5000) {
                    totalSkipped.incrementAndGet();
                    continue;
                }

                if (task.getPlayer().isOnline()) {
                    loadChunk(task);
                    batchCount++;
                }

                if (batchCount >= batchSize && batchDelay > 0) {
                    Thread.sleep(batchDelay);
                    batchCount = 0;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "ChunkLoadPriorityScheduler", "processQueue", e);
            }
        }
    }

    private void loadChunk(ChunkLoadTask task) {
        try {
            Player player = task.getPlayer();
            World world = task.getWorld();
            int chunkX = task.getChunkX();
            int chunkZ = task.getChunkZ();

            world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
                if (chunk != null) {
                    totalLoaded.incrementAndGet();
                    PlayerLoadStats stats = playerStats.get(player.getUniqueId());
                    if (stats != null) {
                        stats.recordLoaded();
                    }
                }
            });

        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ChunkLoadPriorityScheduler", "loadChunk", e);
        }
    }

    private void cleanup() {
        playerStats.entrySet().removeIf(entry -> {
            PlayerLoadStats stats = entry.getValue();
            return System.currentTimeMillis() - stats.getLastActivity() > 60000;
        });

        taskQueue.removeIf(task ->
            System.currentTimeMillis() - task.getQueueTime() > 10000 ||
            !task.getPlayer().isOnline()
        );
    }

    public int getQueueSize() {
        return taskQueue.size();
    }

    public int getPlayerQueueSize(UUID playerId) {
        return (int) taskQueue.stream()
            .filter(task -> task.getPlayer().getUniqueId().equals(playerId))
            .count();
    }

    public String getStatistics() {
        return String.format(
            "ChunkLoadScheduler: Loaded=%d, Queued=%d, Skipped=%d, QueueSize=%d, Players=%d",
            totalLoaded.get(),
            totalQueued.get(),
            totalSkipped.get(),
            taskQueue.size(),
            playerStats.size()
        );
    }

    private static class ChunkLoadTask {
        private final Player player;
        private final World world;
        private final int chunkX;
        private final int chunkZ;
        private final int priority;
        private final double speed;
        private final long queueTime;

        public ChunkLoadTask(Player player, World world, int chunkX, int chunkZ, int priority, double speed, long queueTime) {
            this.player = player;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.priority = priority;
            this.speed = speed;
            this.queueTime = queueTime;
        }

        public Player getPlayer() { return player; }
        public World getWorld() { return world; }
        public int getChunkX() { return chunkX; }
        public int getChunkZ() { return chunkZ; }
        public int getPriority() { return priority; }
        public double getSpeed() { return speed; }
        public long getQueueTime() { return queueTime; }
    }

    private static class PlayerLoadStats {
        private final AtomicInteger queuedCount = new AtomicInteger(0);
        private final AtomicInteger loadedCount = new AtomicInteger(0);
        private volatile long lastActivity = System.currentTimeMillis();
        private volatile long lastLoadTime = 0;
        private volatile long firstLoadTime = System.currentTimeMillis();
        private static final int MAX_LOADS_PER_SECOND = 20;
        private static final int INITIAL_GRACE_PERIOD_MS = 3000;

        public void recordQueued() {
            queuedCount.incrementAndGet();
            lastActivity = System.currentTimeMillis();
        }

        public void recordLoaded() {
            loadedCount.incrementAndGet();
            lastLoadTime = System.currentTimeMillis();
            lastActivity = System.currentTimeMillis();
        }

        public boolean shouldThrottle() {
            long now = System.currentTimeMillis();
            long timeSinceFirst = now - firstLoadTime;

            if (timeSinceFirst < INITIAL_GRACE_PERIOD_MS) {
                int allowedLoads = (int) ((timeSinceFirst / 1000.0) * MAX_LOADS_PER_SECOND * 0.5);
                if (queuedCount.get() >= Math.max(5, allowedLoads)) {
                    return true;
                }
            }

            if (now - lastLoadTime < 50) {
                return true;
            }

            long recentWindow = 1000;
            if (timeSinceFirst < recentWindow) {
                int loadsInWindow = queuedCount.get();
                return loadsInWindow >= MAX_LOADS_PER_SECOND;
            }

            return false;
        }

        public long getLastActivity() {
            return lastActivity;
        }
    }
}
