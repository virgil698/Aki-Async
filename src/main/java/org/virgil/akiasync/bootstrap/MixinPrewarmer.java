package org.virgil.akiasync.bootstrap;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.parallel.ParallelEntityProcessor;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class MixinPrewarmer {

    private final AkiAsyncPlugin plugin;
    private final Logger logger;
    private final boolean enabled;
    private final boolean async;

    private static final AtomicInteger prewarmCount = new AtomicInteger(0);
    private static final AtomicInteger failCount = new AtomicInteger(0);

    public MixinPrewarmer(AkiAsyncPlugin plugin, boolean enabled, boolean async) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.enabled = enabled;
        this.async = async;
    }

    public void prewarm() {
        if (!enabled) {
            logger.info("[MixinPrewarmer] Prewarming disabled, skipping...");
            return;
        }

        long startTime = System.currentTimeMillis();
        logger.info("[MixinPrewarmer] Starting mixin configuration prewarming...");

        if (async) {
            prewarmAsync();
        } else {
            prewarmSync();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info(String.format("[MixinPrewarmer] Prewarming completed: %d success, %d failed, took %dms",
            prewarmCount.get(), failCount.get(), elapsed));
    }

    private void prewarmSync() {
        prewarmBridgeConfigCache();
        prewarmParallelEntityProcessor();
        prewarmEntityTracker();
        prewarmPathfinding();
        prewarmNetworkComponents();
        prewarmMixinClasses();
    }

    private void prewarmAsync() {
        ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "AkiAsync-Prewarm");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        );

        try {
            CompletableFuture<Void> bridgeFuture = CompletableFuture.runAsync(this::prewarmBridgeConfigCache, executor);
            CompletableFuture<Void> entityFuture = CompletableFuture.runAsync(this::prewarmParallelEntityProcessor, executor);
            CompletableFuture<Void> trackerFuture = CompletableFuture.runAsync(this::prewarmEntityTracker, executor);
            CompletableFuture<Void> pathFuture = CompletableFuture.runAsync(this::prewarmPathfinding, executor);
            CompletableFuture<Void> networkFuture = CompletableFuture.runAsync(this::prewarmNetworkComponents, executor);
            CompletableFuture<Void> mixinFuture = CompletableFuture.runAsync(this::prewarmMixinClasses, executor);

            CompletableFuture.allOf(bridgeFuture, entityFuture, trackerFuture, pathFuture, networkFuture, mixinFuture)
                .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warning("[MixinPrewarmer] Async prewarming timeout or error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private void prewarmBridgeConfigCache() {
        try {
            BridgeConfigCache.refreshCache();
            
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.isEntityTickParallel();
                bridge.isMultithreadedEntityTrackerEnabled();
                bridge.isBrainThrottleEnabled();
                bridge.isAsyncPathfindingEnabled();
                bridge.isCollisionOptimizationEnabled();
                bridge.getGeneralThreadPoolSize();
                bridge.getMinEntitiesForParallel();
            }
            
            prewarmCount.incrementAndGet();
            logger.info("[MixinPrewarmer] BridgeConfigCache prewarmed");
        } catch (Exception e) {
            failCount.incrementAndGet();
            logger.warning("[MixinPrewarmer] Failed to prewarm BridgeConfigCache: " + e.getMessage());
        }
    }

    private void prewarmParallelEntityProcessor() {
        try {
            ParallelEntityProcessor.init();
            prewarmCount.incrementAndGet();
            logger.info("[MixinPrewarmer] ParallelEntityProcessor prewarmed");
        } catch (Exception e) {
            failCount.incrementAndGet();
            logger.warning("[MixinPrewarmer] Failed to prewarm ParallelEntityProcessor: " + e.getMessage());
        }
    }

    private void prewarmEntityTracker() {
        try {
            Class.forName("org.virgil.akiasync.mixin.entitytracker.MultithreadedEntityTracker");
            prewarmCount.incrementAndGet();
            logger.info("[MixinPrewarmer] MultithreadedEntityTracker class loaded");
        } catch (Exception e) {
            failCount.incrementAndGet();
            logger.warning("[MixinPrewarmer] Failed to prewarm EntityTracker: " + e.getMessage());
        }
    }

    private void prewarmPathfinding() {
        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null && bridge.isAsyncPathfindingEnabled()) {
                org.virgil.akiasync.mixin.pathfinding.AsyncPathProcessor.initialize();
                prewarmCount.incrementAndGet();
                logger.info("[MixinPrewarmer] AsyncPathProcessor prewarmed");
            }
        } catch (Exception e) {
            failCount.incrementAndGet();
            logger.warning("[MixinPrewarmer] Failed to prewarm Pathfinding: " + e.getMessage());
        }
    }

    private void prewarmNetworkComponents() {
        try {
            tryLoadClass("com.velocitypowered.natives.util.Natives");
            tryLoadClass("io.netty.handler.flush.FlushConsolidationHandler");
            prewarmCount.incrementAndGet();
            logger.info("[MixinPrewarmer] Network components prewarmed");
        } catch (Exception e) {
            failCount.incrementAndGet();
            logger.warning("[MixinPrewarmer] Failed to prewarm Network: " + e.getMessage());
        }
    }

    private void prewarmMixinClasses() {
        try {
            String[] utilityClasses = {
                "org.virgil.akiasync.mixin.util.EntitySyncChecker",
                "org.virgil.akiasync.mixin.util.DirectEntityQuery",
                "org.virgil.akiasync.mixin.util.BridgeConfigCache",
                "org.virgil.akiasync.mixin.util.ExceptionHandler",
                "org.virgil.akiasync.mixin.bridge.BridgeManager"
            };

            int loaded = 0;
            for (String className : utilityClasses) {
                if (tryLoadClass(className)) {
                    loaded++;
                }
            }

            prewarmCount.incrementAndGet();
            logger.info("[MixinPrewarmer] Loaded " + loaded + "/" + utilityClasses.length + " utility classes");
        } catch (Exception e) {
            failCount.incrementAndGet();
            logger.warning("[MixinPrewarmer] Failed to prewarm utility classes: " + e.getMessage());
        }
    }

    private boolean tryLoadClass(String className) {
        try {
            Class.forName(className, true, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            logger.warning("[MixinPrewarmer] Error loading class " + className + ": " + e.getMessage());
            return false;
        }
    }

    public static int getPrewarmCount() {
        return prewarmCount.get();
    }

    public static int getFailCount() {
        return failCount.get();
    }

    public static void reset() {
        prewarmCount.set(0);
        failCount.set(0);
    }
}
