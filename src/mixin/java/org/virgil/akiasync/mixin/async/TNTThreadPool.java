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
        System.out.println("[AkiAsync-Debug] Starting TNTThreadPool smooth restart...");
        
        if (executor != null) {
            ExecutorService oldExecutor = executor;
            
            executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount, r -> {
                Thread t = new Thread(r, "AkiAsync-TNT-Smooth");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });
            
            oldExecutor.shutdown();
            try {
                if (!oldExecutor.awaitTermination(1000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    System.out.println("[AkiAsync-Debug] TNTThreadPool force shutdown");
                    oldExecutor.shutdownNow();
                } else {
                    System.out.println("[AkiAsync-Debug] TNTThreadPool gracefully shutdown");
                }
            } catch (InterruptedException e) {
                oldExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            System.out.println("[AkiAsync-Debug] TNTThreadPool restart completed with " + threadCount + " threads");
        }
    }
}