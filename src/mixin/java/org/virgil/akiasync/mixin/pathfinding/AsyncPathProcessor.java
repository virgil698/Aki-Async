package org.virgil.akiasync.mixin.pathfinding;

import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncPathProcessor {

    private static volatile ThreadPoolExecutor executor;
    private static volatile boolean initialized = false;
    private static volatile boolean enabled = false;
    private static final Object LOCK = new Object();

    public static void initialize() {
        synchronized (LOCK) {
            if (initialized) return;

            Bridge bridge = BridgeManager.getBridge();
            if (bridge == null) {
                initialized = true;
                return;
            }

            enabled = bridge.isAsyncPathfindingEnabled();
            if (!enabled) {
                bridge.debugLog("[AkiAsync-AsyncPath] Async pathfinding is disabled");
                initialized = true;
                return;
            }

            int maxThreads = bridge.getAsyncPathfindingMaxThreads();
            int keepAliveSeconds = bridge.getAsyncPathfindingKeepAliveSeconds();
            int maxQueueSize = bridge.getAsyncPathfindingMaxQueueSize();

            ThreadFactory threadFactory = new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "AkiAsync-Pathfinding-" + threadNumber.getAndIncrement());
                    thread.setDaemon(true);
                    thread.setPriority(Thread.NORM_PRIORITY - 1);
                    return thread;
                }
            };

            BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(maxQueueSize);

            executor = new ThreadPoolExecutor(
                maxThreads,
                maxThreads,
                keepAliveSeconds, TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy() 
            );

            int prestarted = executor.prestartAllCoreThreads();

            bridge.debugLog("[AkiAsync-AsyncPath] Async pathfinding processor initialized:");
            bridge.debugLog("  - Max threads: " + maxThreads);
            bridge.debugLog("  - Keep-alive: " + keepAliveSeconds + "s");
            bridge.debugLog("  - Max queue size: " + maxQueueSize);
            bridge.debugLog("  - Prestarted threads: " + prestarted);

            initialized = true;
        }
    }

    public static void queue(AsyncPath path) {
        if (!initialized) {
            initialize();
        }

        if (!enabled || executor == null) {
            path.process();
            return;
        }

        try {
            executor.execute(() -> {
                try {
                    path.process();
                } catch (Exception e) {
                }
            });
        } catch (RejectedExecutionException e) {
            path.process();
        }
    }

    public static void shutdown() {
        synchronized (LOCK) {
            if (executor != null && !executor.isShutdown()) {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog("[AkiAsync-AsyncPath] Shutting down async pathfinding processor...");
                }

                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }

                if (bridge != null) {
                    bridge.debugLog("[AkiAsync-AsyncPath] Async pathfinding processor shut down");
                }
            }
            initialized = false;
            enabled = false;
            executor = null;
        }
    }

    public static String getStatistics() {
        if (executor == null) {
            return "Async pathfinding: disabled";
        }

        return String.format(
            "AsyncPath: Pool=%d/%d | Active=%d | Queue=%d | Completed=%d",
            executor.getPoolSize(),
            executor.getCorePoolSize(),
            executor.getActiveCount(),
            executor.getQueue().size(),
            executor.getCompletedTaskCount()
        );
    }

    public static boolean isEnabled() {
        return enabled && executor != null && !executor.isShutdown();
    }
}
