package org.virgil.akiasync.mixin.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * Dedicated thread pool for TNT explosion calculations
 * 
 * Pool size: CPU cores * 1.5 (configurable)
 * Reuses existing thread pool infrastructure if available
 * 
 * @author Virgil
 */
public class TNTThreadPool {
    private static ExecutorService executor;
    private static int threadCount = Math.max(6, Runtime.getRuntime().availableProcessors() * 3 / 2);
    
    public static void init(int threads) {
        if (executor != null) {
            shutdown();
        }
        threadCount = threads;
        executor = new ForkJoinPool(threadCount);
        System.out.println("[AkiAsync] TNT thread pool initialized with " + threadCount + " threads");
    }
    
    public static ExecutorService getExecutor() {
        if (executor == null) {
            init(threadCount);
        }
        return executor;
    }
    
    public static void shutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
}

