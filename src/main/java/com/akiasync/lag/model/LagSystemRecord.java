package com.akiasync.lag.model;

public record LagSystemRecord(
        long heapUsedBytes,
        long heapMaxBytes,
        long heapDeltaBytes,
        long gcCollections,
        long gcPauseNanos
) {
}
