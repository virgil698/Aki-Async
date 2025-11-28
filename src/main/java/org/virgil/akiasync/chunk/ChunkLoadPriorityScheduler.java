package org.virgil.akiasync.chunk;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
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
    
    public void submitChunkLoad(ServerPlayer player, ChunkPos chunkPos, int priority, double speed) {
        if (!running || player == null || chunkPos == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        PlayerLoadStats stats = playerStats.computeIfAbsent(playerId, k -> new PlayerLoadStats());
        
        if (stats.shouldThrottle()) {
            totalSkipped.incrementAndGet();
            return;
        }
        
        ChunkLoadTask task = new ChunkLoadTask(player, chunkPos, priority, speed, System.currentTimeMillis());
        
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
                
                if (!task.getPlayer().isRemoved()) {
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
                
            }
        }
    }
    
    private void loadChunk(ChunkLoadTask task) {
        try {
            ServerPlayer player = task.getPlayer();
            ChunkPos chunkPos = task.getChunkPos();
            ServerLevel level = (ServerLevel) player.level();
            
            level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);
            
            totalLoaded.incrementAndGet();
            PlayerLoadStats stats = playerStats.get(player.getUUID());
            if (stats != null) {
                stats.recordLoaded();
            }
            
        } catch (Exception e) {
            
        }
    }
    
    private void cleanup() {
        
        playerStats.entrySet().removeIf(entry -> {
            PlayerLoadStats stats = entry.getValue();
            return System.currentTimeMillis() - stats.getLastActivity() > 60000; 
        });
        
        taskQueue.removeIf(task -> 
            System.currentTimeMillis() - task.getQueueTime() > 10000 || 
            task.getPlayer().isRemoved() 
        );
    }
    
    public int getQueueSize() {
        return taskQueue.size();
    }
    
    public int getPlayerQueueSize(UUID playerId) {
        return (int) taskQueue.stream()
            .filter(task -> task.getPlayer().getUUID().equals(playerId))
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
        private final ServerPlayer player;
        private final ChunkPos chunkPos;
        private final int priority;
        private final double speed;
        private final long queueTime;
        
        public ChunkLoadTask(ServerPlayer player, ChunkPos chunkPos, int priority, double speed, long queueTime) {
            this.player = player;
            this.chunkPos = chunkPos;
            this.priority = priority;
            this.speed = speed;
            this.queueTime = queueTime;
        }
        
        public ServerPlayer getPlayer() { return player; }
        public ChunkPos getChunkPos() { return chunkPos; }
        public int getPriority() { return priority; }
        public double getSpeed() { return speed; }
        public long getQueueTime() { return queueTime; }
    }
    
    private static class PlayerLoadStats {
        private final AtomicInteger queuedCount = new AtomicInteger(0);
        private final AtomicInteger loadedCount = new AtomicInteger(0);
        private volatile long lastActivity = System.currentTimeMillis();
        private volatile long lastLoadTime = 0;
        
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
            
            return System.currentTimeMillis() - lastLoadTime < 10;
        }
        
        public long getLastActivity() {
            return lastActivity;
        }
    }
}
