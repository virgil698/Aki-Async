package org.virgil.akiasync.config.sections;

import org.bukkit.configuration.file.FileConfiguration;

public class NetworkConfig {

    private boolean advancedNetworkOptimizationEnabled;
    private boolean fastVarIntEnabled;
    private boolean eventLoopAffinityEnabled;
    private boolean byteBufOptimizerEnabled;
    private boolean strictEventLoopChecking;
    private boolean pooledByteBufAllocator;
    private boolean directByteBufPreferred;

    private boolean skipZeroMovementPacketsEnabled;
    private boolean skipZeroMovementPacketsStrictMode;

    private boolean mtuAwareBatchingEnabled;
    private int mtuLimit;
    private int mtuHardCapPackets;

    private boolean flushConsolidationEnabled;
    private int flushConsolidationExplicitFlushAfterFlushes;
    private boolean flushConsolidationConsolidateWhenNoReadInProgress;

    private boolean nativeCompressionEnabled;
    private int nativeCompressionLevel;

    private boolean nativeEncryptionEnabled;

    private boolean explosionBlockUpdateOptimizationEnabled;
    private int explosionBlockChangeThreshold;

    private int highLatencyThreshold;
    private int highLatencyMinViewDistance;
    private long highLatencyDurationMs;

    private boolean afkPacketThrottleEnabled;
    private long afkDurationMs;
    private double afkParticleMaxDistance;
    private double afkSoundMaxDistance;

    private boolean dynamicChunkSendRateEnabled;
    private long dynamicChunkLimitBandwidth;
    private long dynamicChunkGuaranteedBandwidth;

    private boolean packetCompressionOptimizationEnabled;
    private boolean adaptiveCompressionThresholdEnabled;
    private boolean skipSmallPacketsEnabled;
    private int skipSmallPacketsThreshold;

    private boolean chunkBatchOptimizationEnabled;
    private float chunkBatchMinChunks;
    private float chunkBatchMaxChunks;
    private int chunkBatchMaxUnacked;

    private boolean packetPriorityQueueEnabled;
    private boolean prioritizePlayerPacketsEnabled;
    private boolean prioritizeChunkPacketsEnabled;
    private boolean deprioritizeParticlesEnabled;
    private boolean deprioritizeSoundsEnabled;

    private boolean chunkVisibilityFilterEnabled;

    private boolean fastMovementChunkLoadEnabled;
    private double fastMovementSpeedThreshold;
    private int fastMovementPreloadDistance;
    private int fastMovementMaxConcurrentLoads;
    private int fastMovementPredictionTicks;

    private boolean centerOffsetEnabled;
    private double minOffsetSpeed;
    private double maxOffsetSpeed;
    private double maxOffsetRatio;
    private int asyncLoadingBatchSize;
    private long asyncLoadingBatchDelayMs;

    private boolean playerJoinWarmupEnabled;
    private long playerJoinWarmupDurationMs;
    private double playerJoinWarmupInitialRate;

    private boolean mapRenderingOptimizationEnabled;
    private int mapRenderingThreads;

    public void load(FileConfiguration config) {
        advancedNetworkOptimizationEnabled = config.getBoolean("network-optimization.advanced.enabled", true);
        fastVarIntEnabled = config.getBoolean("network-optimization.advanced.fast-varint.enabled", true);
        eventLoopAffinityEnabled = config.getBoolean("network-optimization.advanced.eventloop-affinity.enabled", true);
        byteBufOptimizerEnabled = config.getBoolean("network-optimization.advanced.bytebuf-optimizer.enabled", true);
        strictEventLoopChecking = config.getBoolean("network-optimization.advanced.eventloop-affinity.strict-checking", false);
        pooledByteBufAllocator = config.getBoolean("network-optimization.advanced.bytebuf-optimizer.pooled-allocator", true);
        directByteBufPreferred = config.getBoolean("network-optimization.advanced.bytebuf-optimizer.prefer-direct", true);

        skipZeroMovementPacketsEnabled = config.getBoolean("network-optimization.advanced.skip-zero-movement-packets.enabled", true);
        skipZeroMovementPacketsStrictMode = config.getBoolean("network-optimization.advanced.skip-zero-movement-packets.strict-mode", false);

        mtuAwareBatchingEnabled = config.getBoolean("network-optimization.advanced.mtu-aware-batching.enabled", true);
        mtuLimit = config.getInt("network-optimization.advanced.mtu-aware-batching.mtu-limit", 1396);
        mtuHardCapPackets = config.getInt("network-optimization.advanced.mtu-aware-batching.hard-cap-packets", 4096);

        flushConsolidationEnabled = config.getBoolean("network-optimization.advanced.flush-consolidation.enabled", true);
        flushConsolidationExplicitFlushAfterFlushes = config.getInt("network-optimization.advanced.flush-consolidation.explicit-flush-after", 256);
        flushConsolidationConsolidateWhenNoReadInProgress = config.getBoolean("network-optimization.advanced.flush-consolidation.consolidate-when-no-read", true);

        nativeCompressionEnabled = config.getBoolean("network-optimization.advanced.native-compression.enabled", true);
        nativeCompressionLevel = config.getInt("network-optimization.advanced.native-compression.level", 6);

        nativeEncryptionEnabled = config.getBoolean("network-optimization.advanced.native-encryption.enabled", true);

        explosionBlockUpdateOptimizationEnabled = config.getBoolean("network-optimization.advanced.explosion-optimization.enabled", true);
        explosionBlockChangeThreshold = config.getInt("network-optimization.advanced.explosion-optimization.block-change-threshold", 512);

        highLatencyThreshold = config.getInt("network-optimization.high-latency-adjust.latency-threshold", 150);
        highLatencyMinViewDistance = config.getInt("network-optimization.high-latency-adjust.min-view-distance", 5);
        highLatencyDurationMs = config.getLong("network-optimization.high-latency-adjust.duration-ms", 60000L);

        afkPacketThrottleEnabled = config.getBoolean("network-optimization.afk-packet-throttle.enabled", true);
        afkDurationMs = config.getLong("network-optimization.afk-packet-throttle.afk-duration-ms", 120000L);
        afkParticleMaxDistance = config.getDouble("network-optimization.afk-packet-throttle.particle-max-distance", 0.0);
        afkSoundMaxDistance = config.getDouble("network-optimization.afk-packet-throttle.sound-max-distance", 64.0);

        dynamicChunkSendRateEnabled = config.getBoolean("network-optimization.dynamic-chunk-send-rate.enabled", true);
        dynamicChunkLimitBandwidth = config.getLong("network-optimization.dynamic-chunk-send-rate.limit-upload-bandwidth", 10240L);
        dynamicChunkGuaranteedBandwidth = config.getLong("network-optimization.dynamic-chunk-send-rate.guaranteed-bandwidth", 512L);

        packetCompressionOptimizationEnabled = config.getBoolean("network-optimization.packet-compression.enabled", true);
        adaptiveCompressionThresholdEnabled = config.getBoolean("network-optimization.packet-compression.adaptive-threshold", true);
        skipSmallPacketsEnabled = config.getBoolean("network-optimization.packet-compression.skip-small-packets", true);
        skipSmallPacketsThreshold = config.getInt("network-optimization.packet-compression.skip-threshold", 32);

        chunkBatchOptimizationEnabled = config.getBoolean("network-optimization.chunk-batch.enabled", true);
        chunkBatchMinChunks = (float) config.getDouble("network-optimization.chunk-batch.min-chunks", 2.0);
        chunkBatchMaxChunks = (float) config.getDouble("network-optimization.chunk-batch.max-chunks", 32.0);
        chunkBatchMaxUnacked = config.getInt("network-optimization.chunk-batch.max-unacked-batches", 8);

        packetPriorityQueueEnabled = config.getBoolean("network-optimization.packet-priority.enabled", true);
        prioritizePlayerPacketsEnabled = config.getBoolean("network-optimization.packet-priority.prioritize-player-packets", true);
        prioritizeChunkPacketsEnabled = config.getBoolean("network-optimization.packet-priority.prioritize-chunk-packets", true);
        deprioritizeParticlesEnabled = config.getBoolean("network-optimization.packet-priority.deprioritize-particles", true);
        deprioritizeSoundsEnabled = config.getBoolean("network-optimization.packet-priority.deprioritize-sounds", true);

        chunkVisibilityFilterEnabled = config.getBoolean("network-optimization.chunk-visibility-filter.enabled", true);

        fastMovementChunkLoadEnabled = config.getBoolean("network-optimization.fast-movement-chunk-load.enabled", false);
        fastMovementSpeedThreshold = config.getDouble("network-optimization.fast-movement-chunk-load.speed-threshold", 0.5);
        fastMovementPreloadDistance = config.getInt("network-optimization.fast-movement-chunk-load.preload-distance", 8);
        fastMovementMaxConcurrentLoads = config.getInt("network-optimization.fast-movement-chunk-load.max-concurrent-loads", 4);
        fastMovementPredictionTicks = config.getInt("network-optimization.fast-movement-chunk-load.prediction-ticks", 40);

        centerOffsetEnabled = config.getBoolean("network-optimization.fast-movement-chunk-load.center-offset.enabled", true);
        minOffsetSpeed = config.getDouble("network-optimization.fast-movement-chunk-load.center-offset.min-speed", 3.0);
        maxOffsetSpeed = config.getDouble("network-optimization.fast-movement-chunk-load.center-offset.max-speed", 9.0);
        maxOffsetRatio = config.getDouble("network-optimization.fast-movement-chunk-load.center-offset.max-offset-ratio", 0.75);

        asyncLoadingBatchSize = config.getInt("network-optimization.fast-movement-chunk-load.async-loading.batch-size", 2);
        asyncLoadingBatchDelayMs = config.getLong("network-optimization.fast-movement-chunk-load.async-loading.batch-delay-ms", 20L);

        playerJoinWarmupEnabled = config.getBoolean("network-optimization.fast-movement-chunk-load.player-join-warmup.enabled", true);
        playerJoinWarmupDurationMs = config.getLong("network-optimization.fast-movement-chunk-load.player-join-warmup.warmup-duration-ms", 3000L);
        playerJoinWarmupInitialRate = config.getDouble("network-optimization.fast-movement-chunk-load.player-join-warmup.initial-rate", 0.5);

        mapRenderingOptimizationEnabled = config.getBoolean("network-optimization.fast-movement-chunk-load.map-rendering.enabled", false);
        mapRenderingThreads = config.getInt("network-optimization.fast-movement-chunk-load.map-rendering.threads", 2);
    }

    public boolean isAdvancedNetworkOptimizationEnabled() { return advancedNetworkOptimizationEnabled; }
    public boolean isFastVarIntEnabled() { return fastVarIntEnabled; }
    public boolean isEventLoopAffinityEnabled() { return eventLoopAffinityEnabled; }
    public boolean isByteBufOptimizerEnabled() { return byteBufOptimizerEnabled; }
    public boolean isStrictEventLoopChecking() { return strictEventLoopChecking; }
    public boolean isPooledByteBufAllocator() { return pooledByteBufAllocator; }
    public boolean isDirectByteBufPreferred() { return directByteBufPreferred; }
    public boolean isSkipZeroMovementPacketsEnabled() { return skipZeroMovementPacketsEnabled; }
    public boolean isSkipZeroMovementPacketsStrictMode() { return skipZeroMovementPacketsStrictMode; }

    public boolean isMtuAwareBatchingEnabled() { return mtuAwareBatchingEnabled; }
    public int getMtuLimit() { return mtuLimit; }
    public int getMtuHardCapPackets() { return mtuHardCapPackets; }

    public boolean isFlushConsolidationEnabled() { return flushConsolidationEnabled; }
    public int getFlushConsolidationExplicitFlushAfterFlushes() { return flushConsolidationExplicitFlushAfterFlushes; }
    public boolean isFlushConsolidationConsolidateWhenNoReadInProgress() { return flushConsolidationConsolidateWhenNoReadInProgress; }

    public boolean isNativeCompressionEnabled() { return nativeCompressionEnabled; }
    public int getNativeCompressionLevel() { return nativeCompressionLevel; }

    public boolean isNativeEncryptionEnabled() { return nativeEncryptionEnabled; }

    public boolean isExplosionBlockUpdateOptimizationEnabled() { return explosionBlockUpdateOptimizationEnabled; }
    public int getExplosionBlockChangeThreshold() { return explosionBlockChangeThreshold; }

    public int getHighLatencyThreshold() { return highLatencyThreshold; }
    public int getHighLatencyMinViewDistance() { return highLatencyMinViewDistance; }
    public long getHighLatencyDurationMs() { return highLatencyDurationMs; }

    public boolean isAfkPacketThrottleEnabled() { return afkPacketThrottleEnabled; }
    public long getAfkDurationMs() { return afkDurationMs; }
    public double getAfkParticleMaxDistance() { return afkParticleMaxDistance; }
    public double getAfkSoundMaxDistance() { return afkSoundMaxDistance; }

    public boolean isDynamicChunkSendRateEnabled() { return dynamicChunkSendRateEnabled; }
    public long getDynamicChunkLimitBandwidth() { return dynamicChunkLimitBandwidth; }
    public long getDynamicChunkGuaranteedBandwidth() { return dynamicChunkGuaranteedBandwidth; }

    public boolean isPacketCompressionOptimizationEnabled() { return packetCompressionOptimizationEnabled; }
    public boolean isAdaptiveCompressionThresholdEnabled() { return adaptiveCompressionThresholdEnabled; }
    public boolean isSkipSmallPacketsEnabled() { return skipSmallPacketsEnabled; }
    public int getSkipSmallPacketsThreshold() { return skipSmallPacketsThreshold; }

    public boolean isChunkBatchOptimizationEnabled() { return chunkBatchOptimizationEnabled; }
    public float getChunkBatchMinChunks() { return chunkBatchMinChunks; }
    public float getChunkBatchMaxChunks() { return chunkBatchMaxChunks; }
    public int getChunkBatchMaxUnacked() { return chunkBatchMaxUnacked; }

    public boolean isPacketPriorityQueueEnabled() { return packetPriorityQueueEnabled; }
    public boolean isPrioritizePlayerPacketsEnabled() { return prioritizePlayerPacketsEnabled; }
    public boolean isPrioritizeChunkPacketsEnabled() { return prioritizeChunkPacketsEnabled; }
    public boolean isDeprioritizeParticlesEnabled() { return deprioritizeParticlesEnabled; }
    public boolean isDeprioritizeSoundsEnabled() { return deprioritizeSoundsEnabled; }

    public boolean isChunkVisibilityFilterEnabled() { return chunkVisibilityFilterEnabled; }

    public boolean isFastMovementChunkLoadEnabled() { return fastMovementChunkLoadEnabled; }
    public double getFastMovementSpeedThreshold() { return fastMovementSpeedThreshold; }
    public int getFastMovementPreloadDistance() { return fastMovementPreloadDistance; }
    public int getFastMovementMaxConcurrentLoads() { return fastMovementMaxConcurrentLoads; }
    public int getFastMovementPredictionTicks() { return fastMovementPredictionTicks; }

    public boolean isCenterOffsetEnabled() { return centerOffsetEnabled; }
    public double getMinOffsetSpeed() { return minOffsetSpeed; }
    public double getMaxOffsetSpeed() { return maxOffsetSpeed; }
    public double getMaxOffsetRatio() { return maxOffsetRatio; }

    public boolean isPlayerJoinWarmupEnabled() { return playerJoinWarmupEnabled; }
    public long getPlayerJoinWarmupDurationMs() { return playerJoinWarmupDurationMs; }
    public double getPlayerJoinWarmupInitialRate() { return playerJoinWarmupInitialRate; }
    public int getAsyncLoadingBatchSize() { return asyncLoadingBatchSize; }
    public long getAsyncLoadingBatchDelayMs() { return asyncLoadingBatchDelayMs; }

    public boolean isMapRenderingOptimizationEnabled() { return mapRenderingOptimizationEnabled; }
    public int getMapRenderingThreads() { return mapRenderingThreads; }
}
