package org.virgil.akiasync.config.sections;

import org.bukkit.configuration.file.FileConfiguration;

public class ChunkConfig {

    private boolean noiseOptimizationEnabled;
    private boolean jigsawOptimizationEnabled;

    private boolean playerChunkLoadingOptimizationEnabled;
    private int maxConcurrentChunkLoadsPerPlayer;

    private boolean chunkTickAsyncEnabled;
    private int chunkTickThreads;
    private long chunkTickTimeoutMicros;
    private int chunkTickAsyncBatchSize;

    private boolean chunkOptimizationEnabled;
    private boolean chunkPosOptimizationEnabled;

    private boolean spawnChunkRemovalEnabled;

    public void load(FileConfiguration config) {
        noiseOptimizationEnabled = config.getBoolean("chunk-system.generation.noise-optimization.enabled", true);
        jigsawOptimizationEnabled = config.getBoolean("chunk-system.generation.jigsaw-optimization.enabled", true);

        playerChunkLoadingOptimizationEnabled = config.getBoolean("vmp-optimizations.chunk-loading.enabled", true);
        maxConcurrentChunkLoadsPerPlayer = config.getInt("vmp-optimizations.chunk-loading.max-concurrent-per-player", 5);

        chunkTickAsyncEnabled = config.getBoolean("chunk-system.block-tick.async.enabled", false);
        chunkTickThreads = config.getInt("chunk-system.block-tick.async.threads", 4);
        chunkTickTimeoutMicros = config.getLong("chunk-system.block-tick.async.timeout-us", 200L);
        chunkTickAsyncBatchSize = config.getInt("chunk-system.block-tick.async.batch-size", 16);

        chunkOptimizationEnabled = config.getBoolean("chunk-system.enabled", true);
        chunkPosOptimizationEnabled = config.getBoolean("chunk-system.generation.chunk-pos-optimization.enabled", true);

        spawnChunkRemovalEnabled = config.getBoolean("spawn-chunk-removal.enabled", true);
    }

    public void validate(java.util.logging.Logger logger) {
        if (maxConcurrentChunkLoadsPerPlayer < 1) maxConcurrentChunkLoadsPerPlayer = 1;
        if (maxConcurrentChunkLoadsPerPlayer > 20) maxConcurrentChunkLoadsPerPlayer = 20;
    }

    public boolean isNoiseOptimizationEnabled() { return noiseOptimizationEnabled; }
    public boolean isJigsawOptimizationEnabled() { return jigsawOptimizationEnabled; }

    public boolean isPlayerChunkLoadingOptimizationEnabled() { return playerChunkLoadingOptimizationEnabled; }
    public int getMaxConcurrentChunkLoadsPerPlayer() { return maxConcurrentChunkLoadsPerPlayer; }

    public boolean isChunkTickAsyncEnabled() { return chunkTickAsyncEnabled; }
    public int getChunkTickThreads() { return chunkTickThreads; }
    public long getChunkTickTimeoutMicros() { return chunkTickTimeoutMicros; }
    public int getChunkTickAsyncBatchSize() { return chunkTickAsyncBatchSize; }

    public boolean isChunkOptimizationEnabled() { return chunkOptimizationEnabled; }
    public boolean isChunkPosOptimizationEnabled() { return chunkPosOptimizationEnabled; }

    public boolean isSpawnChunkRemovalEnabled() { return spawnChunkRemovalEnabled; }
}
