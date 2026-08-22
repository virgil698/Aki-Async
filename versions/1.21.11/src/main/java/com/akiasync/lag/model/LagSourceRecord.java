package com.akiasync.lag;

public record LagSourceRecord(
        LagCauseCategory category,
        String type,
        String owner,
        String detail,
        String world,
        boolean hasLocation,
        int x,
        int y,
        int z,
        int count,
        long totalNanos,
        long maxNanos
) {
}
