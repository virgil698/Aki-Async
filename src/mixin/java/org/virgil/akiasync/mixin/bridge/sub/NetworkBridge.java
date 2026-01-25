package org.virgil.akiasync.mixin.bridge.sub;

public interface NetworkBridge {

    boolean isAdvancedNetworkOptimizationEnabled();
    boolean isFastVarIntEnabled();
    boolean isEventLoopAffinityEnabled();
    boolean isByteBufOptimizerEnabled();
    boolean isStrictEventLoopChecking();
    boolean isPooledByteBufAllocator();
    boolean isDirectByteBufPreferred();

    boolean isVelocityCompressionEnabled();
    boolean isMultiNettyEventLoopEnabled();

    boolean isSkipZeroMovementPacketsEnabled();
    boolean isSkipZeroMovementPacketsStrictMode();

    int getHighLatencyThreshold();
    int getHighLatencyMinViewDistance();
    long getHighLatencyDurationMs();

    boolean isAfkPacketThrottleEnabled();
    long getAfkDurationMs();
    double getAfkParticleMaxDistance();
    double getAfkSoundMaxDistance();

    boolean isDynamicChunkSendRateEnabled();
    long getDynamicChunkLimitBandwidth();
    long getDynamicChunkGuaranteedBandwidth();

    boolean isPacketCompressionOptimizationEnabled();
    boolean isAdaptiveCompressionThresholdEnabled();
    boolean isSkipSmallPacketsEnabled();
    int getSkipSmallPacketsThreshold();

    boolean isChunkBatchOptimizationEnabled();
    float getChunkBatchMinChunks();
    float getChunkBatchMaxChunks();
    int getChunkBatchMaxUnacked();

    boolean isPacketPriorityQueueEnabled();
    boolean isPrioritizePlayerPacketsEnabled();
    boolean isPrioritizeChunkPacketsEnabled();
    boolean isDeprioritizeParticlesEnabled();
    boolean isDeprioritizeSoundsEnabled();

    long getNetworkTrafficInRate();
    long getNetworkTrafficOutRate();
    long getNetworkTrafficTotalIn();
    long getNetworkTrafficTotalOut();
    void calculateNetworkTrafficRates();

    void setPacketStatisticsEnabled(boolean enabled);
    boolean isPacketStatisticsEnabled();
    void resetPacketStatistics();
    long getPacketStatisticsElapsedSeconds();
    java.util.List<Object[]> getTopOutgoingPackets(int limit);
    java.util.List<Object[]> getTopIncomingPackets(int limit);
    long getTotalOutgoingPacketCount();
    long getTotalIncomingPacketCount();

    void handleConnectionProtocolChange(Object connection, int protocolOrdinal);

    boolean isMtuAwareBatchingEnabled();
    int getMtuLimit();
    int getMtuHardCapPackets();

    boolean isFlushConsolidationEnabled();
    int getFlushConsolidationExplicitFlushAfterFlushes();
    boolean isFlushConsolidationConsolidateWhenNoReadInProgress();

    boolean isNativeCompressionEnabled();
    int getNativeCompressionLevel();
    boolean isNativeEncryptionEnabled();

    long getConnectionPendingBytes(Object connection);
    boolean addFlushConsolidationHandler(Object channel, int explicitFlushAfterFlushes, boolean consolidateWhenNoReadInProgress);

    void sendPacketWithoutFlush(Object connection, Object packet);
    void flushConnection(Object connection);

    Object getConnectionFromListener(Object listener);

    boolean isMapRenderingOptimizationEnabled();
    int getMapRenderingThreads();
}
