package com.akiasync.lag.model;

import java.util.List;

public record LagStackRecord(
        LagCauseCategory category,
        String source,
        int samples,
        List<String> frames
) {
    public LagStackRecord {
        frames = List.copyOf(frames);
    }
}
