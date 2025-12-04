package org.virgil.akiasync.mixin.pathfinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

public final class AsyncPathProcessor {

  private static volatile ThreadPoolExecutor executor;
  private static volatile boolean initialized = false;
  private static volatile boolean enabled = false;
  private static final Object LOCK = new Object();
  

  private static final ThreadLocal<List<AsyncPath>> pendingPaths = ThreadLocal.withInitial(ArrayList::new);
  private static final AtomicInteger batchCount = new AtomicInteger(0);
  private static final AtomicInteger totalBatchedPaths = new AtomicInteger(0);

  private AsyncPathProcessor() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  public static void initialize() {
    synchronized (LOCK) {
      if (initialized) {
        return;
      }

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

      ThreadFactory threadFactory = new PathfindingThreadFactory();
      BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(maxQueueSize);

      executor = new ThreadPoolExecutor(
          maxThreads,
          maxThreads,
          keepAliveSeconds,
          TimeUnit.SECONDS,
          workQueue,
          threadFactory,
          new ThreadPoolExecutor.CallerRunsPolicy()
      );

      int prestartedThreads = executor.prestartAllCoreThreads();

      bridge.debugLog("[AkiAsync-AsyncPath] Async pathfinding processor initialized:");
      bridge.debugLog("  - Max threads: " + maxThreads);
      bridge.debugLog("  - Keep-alive: " + keepAliveSeconds + "s");
      bridge.debugLog("  - Max queue size: " + maxQueueSize);
      bridge.debugLog("  - Prestarted threads: " + prestartedThreads);

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


    List<AsyncPath> paths = pendingPaths.get();
    paths.add(path);
    

    if (paths.size() >= 8) {
      flushPendingPaths();
    }
  }
  
  
  public static void flushPendingPaths() {
    List<AsyncPath> paths = pendingPaths.get();
    if (paths.isEmpty()) {
      return;
    }
    

    List<AsyncPath> batch = new ArrayList<>(paths);
    paths.clear();
    
    if (batch.isEmpty()) {
      return;
    }
    
    batchCount.incrementAndGet();
    totalBatchedPaths.addAndGet(batch.size());
    
    try {

      executor.execute(() -> {
        for (AsyncPath path : batch) {
          try {
            path.process();
          } catch (Exception e) {

          }
        }
      });
    } catch (RejectedExecutionException e) {

      for (AsyncPath path : batch) {
        try {
          path.process();
        } catch (Exception ex) {

        }
      }
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

    int batches = batchCount.get();
    int totalPaths = totalBatchedPaths.get();
    double avgBatchSize = batches > 0 ? (double) totalPaths / batches : 0;
    
    return String.format(
        "AsyncPath: Pool=%d/%d | Active=%d | Queue=%d | Completed=%d | Batches=%d | AvgBatch=%.1f",
        executor.getPoolSize(),
        executor.getCorePoolSize(),
        executor.getActiveCount(),
        executor.getQueue().size(),
        executor.getCompletedTaskCount(),
        batches,
        avgBatchSize
    );
  }

  public static boolean isEnabled() {
    return enabled && executor != null && !executor.isShutdown();
  }

  private static class PathfindingThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(
          runnable,
          "AkiAsync-Pathfinding-" + threadNumber.getAndIncrement()
      );
      thread.setDaemon(true);
      thread.setPriority(Thread.NORM_PRIORITY - 1);
      return thread;
    }
  }
}
