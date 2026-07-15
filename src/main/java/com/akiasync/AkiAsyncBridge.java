package com.akiasync;

import com.akiasync.lag.LagProfilerService;
import com.akiasync.lag.LagSourceRecord;
import com.akiasync.lag.LagStackRecord;
import com.akiasync.lag.LagSystemRecord;
import com.akiasync.lag.LagTickRecord;
import com.akiasync.mixin.Bridge;
import com.akiasync.mixin.BridgeManager;

public final class AkiAsyncBridge implements Bridge {
    private final LagProfilerService lagProfiler;

    public AkiAsyncBridge(LagProfilerService lagProfiler) {
        this.lagProfiler = lagProfiler;
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
                        com.akiasync.lag.LagCauseCategory.valueOf(source.category().name()),
                        source.type().name(), source.owner(), source.detail(), source.world(),
                        source.hasLocation(), source.x(), source.y(), source.z(),
                        source.count(), source.totalNanos(), source.maxNanos()
                )).toList(),
                snapshot.stackSamples().stream().map(stack -> new LagStackRecord(
                        com.akiasync.lag.LagCauseCategory.valueOf(stack.category().name()),
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
