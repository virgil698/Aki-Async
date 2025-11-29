package org.virgil.akiasync.util.resource;

import javax.annotation.Nonnull;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExecutorLifecycleManager {

    private ExecutorLifecycleManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    @Nonnull
    public static ExecutorService createExecutor(
            @Nonnull String name, 
            int threads, 
            boolean daemon) {
        
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (threads <= 0) {
            throw new IllegalArgumentException("Thread count must be positive");
        }

        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(@Nonnull Runnable r) {
                Thread thread = new Thread(r, name + "-" + threadNumber.getAndIncrement());
                thread.setDaemon(daemon);
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        };

        return new ThreadPoolExecutor(
            threads,
            threads,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public static boolean shutdownGracefully(
            @Nonnull ExecutorService executor, 
            long timeout, 
            @Nonnull TimeUnit unit) {
        
        if (executor == null) {
            return true; 
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(timeout, unit)) {
                executor.shutdownNow();
                return executor.awaitTermination(timeout, unit);
            }
            return true;
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Nonnull
    public static HealthStatus checkHealth(@Nonnull ExecutorService executor) {
        if (executor == null) {
            return new HealthStatus(false, "Executor is null", 0, 0, 0);
        }

        if (executor.isShutdown()) {
            return new HealthStatus(false, "Executor is shutdown", 0, 0, 0);
        }

        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
            int poolSize = tpe.getPoolSize();
            int activeCount = tpe.getActiveCount();
            int queueSize = tpe.getQueue().size();

            boolean healthy = !tpe.isShutdown() && !tpe.isTerminated();
            String message = healthy ? "Healthy" : "Unhealthy";

            return new HealthStatus(healthy, message, poolSize, activeCount, queueSize);
        }

        return new HealthStatus(true, "Unknown executor type", 0, 0, 0);
    }

    @Nonnull
    public static ExecutorService restart(
            @Nonnull ExecutorService oldExecutor, 
            @Nonnull String name, 
            int threads) {
        
        shutdownGracefully(oldExecutor, 5, TimeUnit.SECONDS);

        return createExecutor(name, threads, true);
    }

    public static final class HealthStatus {
        private final boolean healthy;
        private final String message;
        private final int poolSize;
        private final int activeCount;
        private final int queueSize;

        public HealthStatus(boolean healthy, String message, int poolSize, int activeCount, int queueSize) {
            this.healthy = healthy;
            this.message = message;
            this.poolSize = poolSize;
            this.activeCount = activeCount;
            this.queueSize = queueSize;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public int getActiveCount() {
            return activeCount;
        }

        public int getQueueSize() {
            return queueSize;
        }

        @Override
        public String toString() {
            return String.format(
                "HealthStatus{healthy=%s, message='%s', poolSize=%d, activeCount=%d, queueSize=%d}",
                healthy, message, poolSize, activeCount, queueSize
            );
        }
    }
}
