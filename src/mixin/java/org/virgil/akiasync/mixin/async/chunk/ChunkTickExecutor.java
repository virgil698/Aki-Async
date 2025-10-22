package org.virgil.akiasync.mixin.async.chunk;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
public final class ChunkTickExecutor {
    private static final ForkJoinPool POOL = new ForkJoinPool(
        4,
        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
        null,
        false
    );
    private static final ThreadPoolExecutor FALLBACK_POOL = new ThreadPoolExecutor(
        4, 4,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(256),
        new ThreadFactory() {
            private int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("AkiChunk-Fallback-" + (count++));
                return t;
            }
        },
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
    public static CompletableFuture<ChunkSnapshot> runAsync(ChunkSnapshot snap) {
        return CompletableFuture.supplyAsync(() -> {
            return snap;
        }, POOL);
    }
    public static java.util.concurrent.ExecutorService getExecutor() {
        return POOL;
    }
    public static void shutdown() {
        POOL.shutdown();
        FALLBACK_POOL.shutdown();
    }
    public static void restartSmooth() {
        try {
            POOL.shutdown();
            if (!POOL.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            FALLBACK_POOL.shutdown();
            if (!FALLBACK_POOL.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                FALLBACK_POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            FALLBACK_POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}