package com.akiasync.lag;

public record LagSourceRanking(
        LagCauseCategory category,
        String type,
        String owner,
        String detail,
        String world,
        int occurrences,
        int affectedTicks,
        long totalNanos,
        long worstTickNanos,
        long maxOccurrenceNanos,
        boolean hasWorstLocation,
        int x,
        int y,
        int z
) {
}
