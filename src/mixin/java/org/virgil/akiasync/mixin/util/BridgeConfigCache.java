package org.virgil.akiasync.mixin.util;

import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

public class BridgeConfigCache {

    private static volatile boolean debugLoggingEnabled = false;

    private static volatile boolean nbtOptimizationEnabled = true;
    private static volatile boolean bitSetPoolingEnabled = true;
    private static volatile boolean completableFutureOptimizationEnabled = true;
    private static volatile boolean chunkOptimizationEnabled = true;
    private static volatile boolean asyncLightingEnabled = true;

    private static volatile boolean initialized = false;

    private static volatile long lastRefreshTime = 0;

    private static final long REFRESH_INTERVAL_MS = 5000;

    public static boolean isBridgeReady() {
        return BridgeManager.getBridge() != null;
    }

    public static Bridge getBridgeSafe() {
        try {
            return BridgeManager.getBridge();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isDebugLoggingEnabled() {

        return debugLoggingEnabled;
    }

    public static boolean isDebugEnabled() {
        return debugLoggingEnabled;
    }

    public static void refreshCache() {
        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                debugLoggingEnabled = bridge.isDebugLoggingEnabled();

                nbtOptimizationEnabled = bridge.isNbtOptimizationEnabled();
                bitSetPoolingEnabled = bridge.isBitSetPoolingEnabled();
                completableFutureOptimizationEnabled = bridge.isCompletableFutureOptimizationEnabled();
                chunkOptimizationEnabled = bridge.isChunkOptimizationEnabled();
                asyncLightingEnabled = bridge.isAsyncLightingEnabled();

                initialized = true;
                lastRefreshTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            ExceptionHandler.handleUnexpected("BridgeConfigCache", "refreshCache", e);
        }
    }

    public static boolean isNbtOptimizationEnabled() {
        checkAndRefresh();
        return nbtOptimizationEnabled;
    }

    public static boolean isBitSetPoolingEnabled() {
        checkAndRefresh();
        return bitSetPoolingEnabled;
    }

    public static boolean isCompletableFutureOptimizationEnabled() {
        checkAndRefresh();
        return completableFutureOptimizationEnabled;
    }

    public static boolean isChunkOptimizationEnabled() {
        checkAndRefresh();
        return chunkOptimizationEnabled;
    }

    public static boolean isAsyncLightingEnabled() {
        checkAndRefresh();
        return asyncLightingEnabled;
    }

    public static int getLightUpdateIntervalMs() {
        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                return bridge.getLightUpdateIntervalMs();
            }
        } catch (Exception e) {

        }
        return 10;
    }

    public static int getMidTickChunkTasksIntervalMs() {
        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                return bridge.getMidTickChunkTasksIntervalMs();
            }
        } catch (Exception e) {

        }
        return 1;
    }

    public static void checkAndRefresh() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRefreshTime > REFRESH_INTERVAL_MS) {
            refreshCache();
        }
    }

    public static Bridge getBridge() {
        return BridgeManager.getBridge();
    }

    public static void debugLog(String message) {
        if (debugLoggingEnabled) {
            try {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog(message);
                }
            } catch (Exception e) {

                System.err.println("[BridgeConfigCache] Failed to log debug message: " + e.getMessage());
            }
        }
    }

    public static void debugLog(String format, Object... args) {
        if (debugLoggingEnabled) {
            try {
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog(format, args);
                }
            } catch (Exception e) {

                System.err.println("[BridgeConfigCache] Failed to log debug message: " + e.getMessage());
            }
        }
    }

    public static void errorLog(String format, Object... args) {
        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.errorLog(format, args);
            }
        } catch (Exception e) {

            System.err.println("[BridgeConfigCache] CRITICAL: Failed to log error message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static int getGeneralThreadPoolSize() {
        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                return bridge.getGeneralThreadPoolSize();
            }
        } catch (Exception e) {

        }
        int cpuCores = Runtime.getRuntime().availableProcessors();
        return Math.max(2, cpuCores / 4);
    }
}
