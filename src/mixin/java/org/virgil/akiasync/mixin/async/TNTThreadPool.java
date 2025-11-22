package org.virgil.akiasync.mixin.async;
import java.util.concurrent.ExecutorService;

public class TNTThreadPool {
    private static ExecutorService executor;
    private static int threadCount = Math.max(6, Runtime.getRuntime().availableProcessors() * 3 / 2);
    
    public static void init(int threads) {
        threadCount = threads;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            executor = bridge.getTNTExecutor();
            bridge.debugLog("[AkiAsync-TNT] Using Bridge TNT Executor (Folia-compatible)");
        } else {
            executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount, r -> {
                Thread t = new Thread(r, "AkiAsync-TNT-Fallback-" + System.currentTimeMillis());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });
        }
    }
    
    public static ExecutorService getExecutor() {
        if (executor == null) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                executor = bridge.getTNTExecutor();
            } else {
                init(threadCount);
            }
        }
        
        if (isFoliaEnvironment() && executor != null) {
            try {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    String healthStatus = bridge.checkExecutorHealth(executor, "TNT");
                    if (healthStatus != null && healthStatus.contains("unhealthy")) {
                        bridge.errorLog("[AkiAsync-TNT] Folia executor unhealthy, status: " + healthStatus);
                    }
                }
            } catch (Exception e) {
            }
        }
        
        return executor;
    }
    
    public static void shutdown() {
    }
    
    public static void restartSmooth() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[AkiAsync-Debug] TNTThreadPool restart - using Bridge executor (no-op)");
            executor = bridge.getTNTExecutor();
        }
    }
    
    private static volatile Boolean isFolia = null;
    
    private static boolean isFoliaEnvironment() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }
}