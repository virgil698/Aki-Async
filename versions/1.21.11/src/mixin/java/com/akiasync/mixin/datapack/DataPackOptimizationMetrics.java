package com.akiasync.mixin.datapack;

import com.akiasync.mixin.Bridge.DataPackSnapshot;
import com.akiasync.mixin.BridgeManager;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

public final class DataPackOptimizationMetrics {
    private static final AtomicLong RELOAD_GENERATION = new AtomicLong();
    private static final AtomicReference<DataPackSnapshot> LATEST = new AtomicReference<>();
    private static final LongAdder FUNCTION_HITS = new LongAdder();
    private static final LongAdder FUNCTION_MISSES = new LongAdder();
    private static final LongAdder ZIP_INDEX_HITS = new LongAdder();
    private static final LongAdder ZIP_INDEX_MISSES = new LongAdder();
    private static final LongAdder ZIP_CONTENT_HITS = new LongAdder();
    private static final LongAdder ZIP_CONTENT_MISSES = new LongAdder();
    private static final LongAdder ZIP_BYTES_REUSED = new LongAdder();
    private static final AtomicLong SMALL_FUNCTIONS = new AtomicLong();
    private static final AtomicLong SMALL_FUNCTION_ENTRIES = new AtomicLong();

    private DataPackOptimizationMetrics() {
    }

    public static ReloadToken beginReload() {
        return new ReloadToken(System.currentTimeMillis(), System.nanoTime(), counters());
    }

    public static void completeReload(ReloadToken token, Throwable failure, int functionsLoaded) {
        Counters after = counters();
        Counters before = token.before();
        DataPackSnapshot snapshot = new DataPackSnapshot(
                RELOAD_GENERATION.incrementAndGet(),
                token.startedAtMillis(),
                System.nanoTime() - token.startedNanos(),
                failure == null,
                failure == null ? "" : failure.getClass().getName(),
                functionsLoaded,
                after.functionHits() - before.functionHits(),
                after.functionMisses() - before.functionMisses(),
                after.zipIndexHits() - before.zipIndexHits(),
                after.zipIndexMisses() - before.zipIndexMisses(),
                after.zipContentHits() - before.zipContentHits(),
                after.zipContentMisses() - before.zipContentMisses(),
                after.zipBytesReused() - before.zipBytesReused(),
                after.smallFunctions(),
                after.smallFunctionEntries(),
                FunctionCompilationCache.entryCount(),
                ZipDataPackCache.indexEntryCount(),
                ZipDataPackCache.contentEntryCount(),
                ZipDataPackCache.contentBytes()
        );
        LATEST.set(snapshot);
        BridgeManager.INSTANCE.publishDataPack(snapshot);
    }

    public static void publishLatest() {
        DataPackSnapshot snapshot = LATEST.get();
        if (snapshot != null) {
            BridgeManager.INSTANCE.publishDataPack(snapshot);
        }
    }

    public static void clearCaches() {
        FunctionCompilationCache.clear();
        ZipDataPackCache.clear();
        LATEST.set(null);
    }

    static void functionHit() {
        FUNCTION_HITS.increment();
    }

    static void functionMiss() {
        FUNCTION_MISSES.increment();
    }

    static void zipIndexHit() {
        ZIP_INDEX_HITS.increment();
    }

    static void zipIndexMiss() {
        ZIP_INDEX_MISSES.increment();
    }

    static void zipContentHit(int bytes) {
        ZIP_CONTENT_HITS.increment();
        ZIP_BYTES_REUSED.add(bytes);
    }

    static void zipContentMiss() {
        ZIP_CONTENT_MISSES.increment();
    }

    public static void smallFunctionScheduled(int entries) {
        long functions = SMALL_FUNCTIONS.incrementAndGet();
        long scheduledEntries = SMALL_FUNCTION_ENTRIES.addAndGet(entries);
        if (functions <= 8 || (functions & 1_023L) == 0) {
            publishScheduling(functions, scheduledEntries);
        }
    }

    private static Counters counters() {
        return new Counters(
                FUNCTION_HITS.sum(),
                FUNCTION_MISSES.sum(),
                ZIP_INDEX_HITS.sum(),
                ZIP_INDEX_MISSES.sum(),
                ZIP_CONTENT_HITS.sum(),
                ZIP_CONTENT_MISSES.sum(),
                ZIP_BYTES_REUSED.sum(),
                SMALL_FUNCTIONS.get(),
                SMALL_FUNCTION_ENTRIES.get()
        );
    }

    private static void publishScheduling(long functions, long entries) {
        DataPackSnapshot current = LATEST.get();
        if (current == null) {
            return;
        }
        DataPackSnapshot updated = new DataPackSnapshot(
                current.generation(),
                current.startedAtMillis(),
                current.durationNanos(),
                current.successful(),
                current.failure(),
                current.functionsLoaded(),
                current.functionCompileHits(),
                current.functionCompileMisses(),
                current.zipIndexHits(),
                current.zipIndexMisses(),
                current.zipContentHits(),
                current.zipContentMisses(),
                current.zipBytesReused(),
                functions,
                entries,
                current.functionCacheEntries(),
                current.zipIndexEntries(),
                current.zipContentEntries(),
                current.zipContentBytes()
        );
        if (LATEST.compareAndSet(current, updated)) {
            BridgeManager.INSTANCE.publishDataPack(updated);
        }
    }

    public record ReloadToken(long startedAtMillis, long startedNanos, Counters before) {
    }

    public record Counters(
            long functionHits,
            long functionMisses,
            long zipIndexHits,
            long zipIndexMisses,
            long zipContentHits,
            long zipContentMisses,
            long zipBytesReused,
            long smallFunctions,
            long smallFunctionEntries
    ) {
    }
}
