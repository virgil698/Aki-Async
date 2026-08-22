package com.akiasync.lag.model;

import java.util.List;

public record LagTickRecord(
        long tickId,
        long startedAtMillis,
        long wallNanos,
        long cpuNanos,
        boolean slow,
        boolean detailed,
        List<LagSourceRecord> sources,
        List<LagStackRecord> stackSamples,
        LagSystemRecord system
) {
    public LagTickRecord {
        sources = List.copyOf(sources);
        stackSamples = List.copyOf(stackSamples);
    }
}
