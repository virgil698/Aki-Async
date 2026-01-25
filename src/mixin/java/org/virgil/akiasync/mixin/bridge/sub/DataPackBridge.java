package org.virgil.akiasync.mixin.bridge.sub;

public interface DataPackBridge {

    boolean isDataPackOptimizationEnabled();
    int getDataPackFileLoadThreads();
    int getDataPackZipProcessThreads();
    int getDataPackBatchSize();
    long getDataPackCacheExpirationMinutes();
    int getDataPackMaxFileCacheSize();
    int getDataPackMaxFileSystemCacheSize();
    boolean isDataPackDebugEnabled();

    boolean isExecuteCommandInactiveSkipEnabled();
    int getExecuteCommandSkipLevel();
    double getExecuteCommandSimulationDistanceMultiplier();
    long getExecuteCommandCacheDurationMs();
    java.util.Set<String> getExecuteCommandWhitelistTypes();
    boolean isExecuteCommandDebugEnabled();

    boolean isCommandDeduplicationEnabled();
    boolean isCommandDeduplicationDebugEnabled();
}
