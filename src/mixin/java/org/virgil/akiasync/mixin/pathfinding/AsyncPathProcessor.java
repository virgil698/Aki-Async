package org.virgil.akiasync.mixin.pathfinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

public final class AsyncPathProcessor {

  private static volatile ThreadPoolExecutor executor;
  private static final AtomicBoolean initialized = new AtomicBoolean(false);
  private static volatile boolean enabled = false;
  
  private static final ThreadLocal<List<AsyncPath>> pendingPaths = ThreadLocal.withInitial(ArrayList::new);
  private static final AtomicInteger batchCount = new AtomicInteger(0);
  private static final AtomicInteger totalBatchedPaths = new AtomicInteger(0);
  private static final AtomicInteger mergedPaths = new AtomicInteger(0);
  private static final AtomicInteger cacheHits = new AtomicInteger(0);

  private AsyncPathProcessor() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  public static void initialize() {
    
    if (!initialized.compareAndSet(false, true)) {
      return; 
    }

    Bridge bridge = BridgeManager.getBridge();
    if (bridge == null) {
      return;
    }

    enabled = bridge.isAsyncPathfindingEnabled();
    if (!enabled) {
      bridge.debugLog("[AkiAsync-AsyncPath] Async pathfinding is disabled");
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
  }

  public static void queue(AsyncPath path) {
    if (!initialized.get()) {
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
    
    List<AsyncPath> mergedBatch = mergeSimilarPaths(batch);
    
    batchCount.incrementAndGet();
    totalBatchedPaths.addAndGet(batch.size());
    if (mergedBatch.size() < batch.size()) {
      mergedPaths.addAndGet(batch.size() - mergedBatch.size());
    }
    
    try {

      executor.execute(() -> {
        for (AsyncPath path : mergedBatch) {
          try {
            path.process();
          } catch (Exception e) {

          }
        }
      });
    } catch (RejectedExecutionException e) {

      for (AsyncPath path : mergedBatch) {
        try {
          path.process();
        } catch (Exception ex) {

        }
      }
    }
  }

  private static List<AsyncPath> mergeSimilarPaths(List<AsyncPath> paths) {
    if (paths.size() <= 1) {
      return paths;
    }
    
    List<AsyncPath> merged = new ArrayList<>();
    List<AsyncPath> duplicates = new ArrayList<>();
    
    for (int i = 0; i < paths.size(); i++) {
      AsyncPath current = paths.get(i);
      boolean isDuplicate = false;
      
      for (AsyncPath existing : merged) {
        if (current.hasSameProcessingPositions(existing.getPositions())) {
          
          duplicates.add(current);
          
          existing.postProcessing(() -> {
            
            try {
              java.lang.reflect.Field delegatedPathField = AsyncPath.class.getDeclaredField("delegatedPath");
              delegatedPathField.setAccessible(true);
              delegatedPathField.set(current, existing.getPath());
              
              java.lang.reflect.Field processStateField = AsyncPath.class.getDeclaredField("processState");
              processStateField.setAccessible(true);
              processStateField.set(current, existing.getClass().getDeclaredField("processState").get(existing));
            } catch (Exception e) {
              
              current.process();
            }
          });
          
          isDuplicate = true;
          break;
        }
      }
      
      if (!isDuplicate) {
        merged.add(current);
      }
    }
    
    return merged;
  }

  public static void shutdown() {
    
    if (!initialized.compareAndSet(true, false)) {
      return; 
    }

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
    
    enabled = false;
    executor = null;
  }

  public static String getStatistics() {
    if (executor == null) {
      return "Async pathfinding: disabled";
    }

    int batches = batchCount.get();
    int totalPaths = totalBatchedPaths.get();
    int merged = mergedPaths.get();
    int hits = cacheHits.get();
    double avgBatchSize = batches > 0 ? (double) totalPaths / batches : 0;
    double mergeRate = totalPaths > 0 ? (double) merged / totalPaths * 100 : 0;
    
    String cacheStats = SharedPathCache.getStats();
    
    return String.format(
        "AsyncPath: Pool=%d/%d | Active=%d | Queue=%d | Completed=%d | Batches=%d | AvgBatch=%.1f | Merged=%d(%.1f%%) | CacheHits=%d | %s",
        executor.getPoolSize(),
        executor.getCorePoolSize(),
        executor.getActiveCount(),
        executor.getQueue().size(),
        executor.getCompletedTaskCount(),
        batches,
        avgBatchSize,
        merged,
        mergeRate,
        hits,
        cacheStats
    );
  }

  public static void recordCacheHit() {
    cacheHits.incrementAndGet();
  }

  public static boolean isEnabled() {
    return enabled && executor != null && !executor.isShutdown();
  }

  public static ThreadPoolExecutor getExecutor() {
    if (!initialized.get()) {
      initialize();
    }
    return executor;
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
