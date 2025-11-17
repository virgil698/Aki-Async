package org.virgil.akiasync.mixin.async;
import java.util.concurrent.ExecutorService;
import org.virgil.akiasync.mixin.optimization.OptimizationManager;
import org.virgil.akiasync.mixin.optimization.thread.VirtualThreadService;
public class TNTThreadPool {
    private static ExecutorService executor;
    private static int threadCount = Math.max(6, Runtime.getRuntime().availableProcessors() * 3 / 2);
    public static void init(int threads) {
        if (executor != null) {
            shutdown();
        }
        threadCount = threads;
        
        VirtualThreadService virtualService = OptimizationManager.getInstance().getVirtualThreadService();
        if (virtualService != null) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-TNT] Using Virtual Thread executor");
            }
            executor = java.util.concurrent.Executors.newThreadPerTaskExecutor(
                virtualService.createFactory()
            );
        } else {
            executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount, r -> {
                Thread t = new Thread(r, "AkiAsync-TNT-" + System.currentTimeMillis());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });
        }
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
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[AkiAsync-Debug] Starting TNTThreadPool smooth restart...");
        }
        
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
                    org.virgil.akiasync.mixin.bridge.Bridge tntForceShutdownBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    if (tntForceShutdownBridge != null) {
                        tntForceShutdownBridge.debugLog("[AkiAsync-Debug] TNTThreadPool force shutdown");
                    }
                    oldExecutor.shutdownNow();
                } else {
                    org.virgil.akiasync.mixin.bridge.Bridge tntGracefulShutdownBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                    if (tntGracefulShutdownBridge != null) {
                        tntGracefulShutdownBridge.debugLog("[AkiAsync-Debug] TNTThreadPool gracefully shutdown");
                    }
                }
            } catch (InterruptedException e) {
                oldExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            org.virgil.akiasync.mixin.bridge.Bridge tntCompleteBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (tntCompleteBridge != null) {
                tntCompleteBridge.debugLog("[AkiAsync-Debug] TNTThreadPool restart completed with " + threadCount + " threads");
            }
        }
    }
}