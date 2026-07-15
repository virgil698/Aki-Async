package com.akiasync.datapack;

public record DataPackRecord(
        long generation,
        long startedAtMillis,
        long durationNanos,
        boolean successful,
        String failure,
        int functionsLoaded,
        long functionCompileHits,
        long functionCompileMisses,
        long zipIndexHits,
        long zipIndexMisses,
        long zipContentHits,
        long zipContentMisses,
        long zipBytesReused,
        long smallFunctionsScheduled,
        long smallFunctionEntriesScheduled,
        int functionCacheEntries,
        int zipIndexEntries,
        int zipContentEntries,
        long zipContentBytes
) {
}
