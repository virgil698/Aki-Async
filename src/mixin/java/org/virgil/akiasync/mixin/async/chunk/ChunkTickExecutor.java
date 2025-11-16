package org.virgil.akiasync.mixin.async.chunk;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
public final class ChunkTickExecutor {
    private static volatile ForkJoinPool POOL;
    private static volatile ThreadPoolExecutor FALLBACK_POOL;
    private static volatile int threadCount = 4;
    private static volatile boolean initialized = false;
    
    private static synchronized void initializePools() {
        if (initialized && POOL != null && !POOL.isShutdown()) {
            return;
        }
        
        POOL = new ForkJoinPool(
            threadCount,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            false
        );
        
        FALLBACK_POOL = new ThreadPoolExecutor(
            threadCount, threadCount,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(256),
            new ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("AkiChunk-Fallback-" + (count++));
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        initialized = true;
        System.out.println("[AkiAsync-Debug] ChunkTickExecutor initialized with " + threadCount + " threads");
    }
    public static CompletableFuture<ChunkSnapshot> runAsync(ChunkSnapshot snap) {
        if (POOL == null || POOL.isShutdown()) {
            initializePools();
        }
        return CompletableFuture.supplyAsync(() -> {
            return snap;
        }, POOL);
    }
    
    public static java.util.concurrent.ExecutorService getExecutor() {
        if (POOL == null || POOL.isShutdown()) {
            initializePools();
        }
        return POOL;
    }
    
    public static void setThreadCount(int count) {
        threadCount = Math.max(1, Math.min(count, 16));
    }
    public static void shutdown() {
        if (POOL != null) {
            POOL.shutdown();
        }
        if (FALLBACK_POOL != null) {
            FALLBACK_POOL.shutdown();
        }
        initialized = false;
    }
    
    public static void restartSmooth() {
        System.out.println("[AkiAsync-Debug] Starting ChunkTickExecutor smooth restart...");
        
        if (POOL != null || FALLBACK_POOL != null) {
            ForkJoinPool oldPool = POOL;
            ThreadPoolExecutor oldFallback = FALLBACK_POOL;
            
            initialized = false;
            POOL = null;
            FALLBACK_POOL = null;
            
            if (oldPool != null) {
                oldPool.shutdown();
                try {
                    if (!oldPool.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                        System.out.println("[AkiAsync-Debug] ChunkTickExecutor main pool force shutdown");
                        oldPool.shutdownNow();
                    } else {
                        System.out.println("[AkiAsync-Debug] ChunkTickExecutor main pool gracefully shutdown");
                    }
                } catch (InterruptedException e) {
                    oldPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            if (oldFallback != null) {
                oldFallback.shutdown();
                try {
                    if (!oldFallback.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                        System.out.println("[AkiAsync-Debug] ChunkTickExecutor fallback pool force shutdown");
                        oldFallback.shutdownNow();
                    } else {
                        System.out.println("[AkiAsync-Debug] ChunkTickExecutor fallback pool gracefully shutdown");
                    }
                } catch (InterruptedException e) {
                    oldFallback.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        System.out.println("[AkiAsync-Debug] ChunkTickExecutor restart completed, will reinitialize on next use");
    }
}