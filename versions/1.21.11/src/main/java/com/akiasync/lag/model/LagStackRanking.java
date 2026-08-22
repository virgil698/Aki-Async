package com.akiasync.lag;

import java.util.List;

public record LagStackRanking(
        LagCauseCategory category,
        String source,
        int samples,
        int affectedTicks,
        List<String> frames
) {
    public LagStackRanking {
        frames = List.copyOf(frames);
    }
}
