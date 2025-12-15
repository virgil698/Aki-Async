package org.virgil.akiasync.mixin.util;

import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

public class BridgeConfigCache {
    
    private static volatile boolean debugLoggingEnabled = false;
    
    private static volatile boolean initialized = false;
    
    private static volatile long lastRefreshTime = 0;
    
    private static final long REFRESH_INTERVAL_MS = 5000;
    
    static {
        
        Thread initThread = new Thread(() -> {
            try {
                refreshCache();
            } catch (Exception e) {
                ExceptionHandler.handleInitialization("BridgeConfigCache", e);
            }
        }, "BridgeConfigCache-Init");
        initThread.setDaemon(true);
        initThread.start();
    }
    
    public static boolean isDebugLoggingEnabled() {
        
        return debugLoggingEnabled;
    }
    
    public static void refreshCache() {
        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                debugLoggingEnabled = bridge.isDebugLoggingEnabled();
                initialized = true;
                lastRefreshTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            ExceptionHandler.handleUnexpected("BridgeConfigCache", "refreshCache", e);
        }
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
}
