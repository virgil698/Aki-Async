package com.akiasync;

import com.akiasync.datapack.DataPackRecord;
import com.akiasync.datapack.DataPackService;
import com.akiasync.lag.model.LagSourceRecord;
import com.akiasync.lag.model.LagStackRecord;
import com.akiasync.lag.model.LagSystemRecord;
import com.akiasync.lag.model.LagTickRecord;
import com.akiasync.lag.service.LagProfilerService;
import com.akiasync.mixin.Bridge;
import com.akiasync.mixin.BridgeManager;

public final class AkiAsyncBridge implements Bridge {
    private final LagProfilerService lagProfiler;
    private final DataPackService dataPackService;

    public AkiAsyncBridge(LagProfilerService lagProfiler, DataPackService dataPackService) {
        this.lagProfiler = lagProfiler;
        this.dataPackService = dataPackService;
    }

    @Override
    public void publishDataPack(DataPackSnapshot snapshot) {
        dataPackService.accept(new DataPackRecord(
                snapshot.generation(),
                snapshot.startedAtMillis(),
                snapshot.durationNanos(),
                snapshot.successful(),
                snapshot.failure(),
                snapshot.functionsLoaded(),
                snapshot.functionCompileHits(),
                snapshot.functionCompileMisses(),
                snapshot.zipIndexHits(),
                snapshot.zipIndexMisses(),
                snapshot.zipContentHits(),
                snapshot.zipContentMisses(),
                snapshot.zipBytesReused(),
                snapshot.smallFunctionsScheduled(),
                snapshot.smallFunctionEntriesScheduled(),
                snapshot.functionCacheEntries(),
                snapshot.zipIndexEntries(),
                snapshot.zipContentEntries(),
                snapshot.zipContentBytes()
        ));
    }

    @Override
    public void publish(LagTickSnapshot snapshot) {
        lagProfiler.accept(new LagTickRecord(
                snapshot.tickId(),
                snapshot.startedAtMillis(),
                snapshot.wallNanos(),
                snapshot.cpuNanos(),
                snapshot.slow(),
                snapshot.detailed(),
                snapshot.sources().stream().map(source -> new LagSourceRecord(
                        com.akiasync.lag.model.LagCauseCategory.valueOf(source.category().name()),
                        source.type().name(), source.owner(), source.detail(), source.world(),
                        source.hasLocation(), source.x(), source.y(), source.z(),
                        source.count(), source.totalNanos(), source.maxNanos()
                )).toList(),
                snapshot.stackSamples().stream().map(stack -> new LagStackRecord(
                        com.akiasync.lag.model.LagCauseCategory.valueOf(stack.category().name()),
                        stack.source(), stack.samples(), stack.frames()
                )).toList(),
                new LagSystemRecord(
                        snapshot.system().heapUsedBytes(),
                        snapshot.system().heapMaxBytes(),
                        snapshot.system().heapDeltaBytes(),
                        snapshot.system().gcCollections(),
                        snapshot.system().gcPauseNanos()
                )
        ));
    }

    public void requestDetailedTicks(int ticks) {
        BridgeManager.INSTANCE.requestDetailedTicks(ticks);
    }

    public int detailedTicksRemaining() {
        return BridgeManager.INSTANCE.detailedTicksRemaining();
    }
}
