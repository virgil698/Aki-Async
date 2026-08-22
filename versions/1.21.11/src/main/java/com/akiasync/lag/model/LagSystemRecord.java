package com.akiasync.lag;

public record LagSystemRecord(
        long heapUsedBytes,
        long heapMaxBytes,
        long heapDeltaBytes,
        long gcCollections,
        long gcPauseNanos
) {
}
