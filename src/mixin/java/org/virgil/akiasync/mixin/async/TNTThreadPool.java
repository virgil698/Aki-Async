package org.virgil.akiasync.mixin.async;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
public class TNTThreadPool {
    private static ExecutorService executor;
    private static int threadCount = Math.max(6, Runtime.getRuntime().availableProcessors() * 3 / 2);
    public static void init(int threads) {
        if (executor != null) {
            shutdown();
        }
        threadCount = threads;
        executor = new ForkJoinPool(threadCount);
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
    public static void restartSmooth() {
        if (executor != null) {
            ExecutorService oldExecutor = executor;
            executor = new ForkJoinPool(threadCount);
            oldExecutor.shutdown();
            try {
                if (!oldExecutor.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    oldExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                oldExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}