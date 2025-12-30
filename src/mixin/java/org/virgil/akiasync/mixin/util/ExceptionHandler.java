package org.virgil.akiasync.mixin.util;

import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ExceptionHandler {
    
    private static final Map<String, AtomicInteger> exceptionCounts = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastLogTime = new ConcurrentHashMap<>();
    private static final long LOG_THROTTLE_MS = 60000; 
    
    public static void handleExpected(String component, String operation, Exception e) {
        recordException(component, "expected");
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isDebugLoggingEnabled()) {
            BridgeConfigCache.debugLog("[%s] Expected exception in %s: %s", 
                component, operation, e.getClass().getSimpleName());
        }
    }
    
    public static void handleUnexpected(String component, String operation, Exception e) {
        recordException(component, "unexpected");
        
        String key = component + ":" + operation;
        long now = System.currentTimeMillis();
        Long lastLog = lastLogTime.get(key);
        
        if (lastLog == null || now - lastLog > LOG_THROTTLE_MS) {
            lastLogTime.put(key, now);
            
            BridgeConfigCache.errorLog("[%s] Unexpected exception in %s: %s - %s", 
                component, operation, e.getClass().getSimpleName(), e.getMessage());
            
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                BridgeConfigCache.errorLog("[%s] Stack trace:", component);
                StackTraceElement[] stackTrace = e.getStackTrace();
                for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                    BridgeConfigCache.errorLog("  at %s", stackTrace[i].toString());
                }
                
                if (e.getCause() != null) {
                    BridgeConfigCache.errorLog("[%s] Caused by: %s - %s", 
                        component, e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
                }
            }
        }
    }
    
    public static void handleRecoverable(String component, String operation, 
                                        Exception e, Runnable fallback) {
        recordException(component, "recoverable");
        
        BridgeConfigCache.errorLog("[%s] Recoverable exception in %s: %s - %s", 
            component, operation, e.getClass().getSimpleName(), e.getMessage());
        
        if (fallback != null) {
            try {
                BridgeConfigCache.debugLog("[%s] Executing fallback for %s", component, operation);
                fallback.run();
            } catch (Exception fallbackEx) {
                BridgeConfigCache.errorLog("[%s] Fallback also failed in %s: %s", 
                    component, operation, fallbackEx.getMessage());
                recordException(component, "fallback-failed");
            }
        }
    }
    
    public static boolean handleInitialization(String component, Exception e) {
        recordException(component, "initialization");
        
        BridgeConfigCache.errorLog("[%s] Initialization failed: %s - %s", 
            component, e.getClass().getSimpleName(), e.getMessage());
        BridgeConfigCache.errorLog("[%s] Feature will be disabled", component);
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isDebugLoggingEnabled()) {
            e.printStackTrace();
        }
        
        return true; 
    }
    
    public static void handleCleanup(String component, String resource, Exception e) {
        recordException(component, "cleanup");
        
        BridgeConfigCache.errorLog("[%s] Failed to cleanup %s: %s", 
            component, resource, e.getMessage());
        
    }
    
    private static void recordException(String component, String type) {
        String key = component + ":" + type;
        exceptionCounts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }
    
    public static String getStatistics() {
        if (exceptionCounts.isEmpty()) {
            return "No exceptions recorded";
        }
        
        StringBuilder sb = new StringBuilder("Exception Statistics:\n");
        exceptionCounts.entrySet().stream()
            .filter(entry -> entry != null && entry.getValue() != null)
            .sorted((a, b) -> b.getValue().get() - a.getValue().get())
            .forEach(entry -> {
                sb.append(String.format("  %s: %d\n", entry.getKey(), entry.getValue().get()));
            });
        return sb.toString();
    }
    
    public static void clearStatistics() {
        exceptionCounts.clear();
        lastLogTime.clear();
    }
    
    public static void cleanupStaleData() {
        long now = System.currentTimeMillis();
        long expireTime = now - (LOG_THROTTLE_MS * 10);
        
        lastLogTime.entrySet().removeIf(entry -> entry != null && entry.getValue() != null && entry.getValue() < expireTime);
        
        exceptionCounts.entrySet().removeIf(entry -> {
            if (entry == null || entry.getKey() == null) {
                return true;
            }
            String key = entry.getKey();
            Long lastLog = lastLogTime.get(key);
            return lastLog != null && lastLog < expireTime;
        });
    }
    
    public static int getExceptionCount(String component, String type) {
        String key = component + ":" + type;
        AtomicInteger count = exceptionCounts.get(key);
        return count != null ? count.get() : 0;
    }
    
    public static <T> T safeSupply(java.util.function.Supplier<T> supplier, String operation, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            handleExpected("SafeSupply", operation, e);
            return defaultValue;
        }
    }
    
    public static <T> T safeReflectionSupply(java.util.function.Supplier<T> supplier, String operation, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            handleExpected("Reflection", operation, e);
            return defaultValue;
        }
    }
    
    public static void safeReflection(Runnable runnable, String operation) {
        try {
            runnable.run();
        } catch (Exception e) {
            handleExpected("Reflection", operation, e);
        }
    }
    
    public static void safeExecute(Runnable task, String context) {
        try {
            task.run();
        } catch (Exception e) {
            handleExpected("SafeExecute", context, e);
        }
    }
    
    public static void logDebug(String message) {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isDebugLoggingEnabled()) {
            BridgeConfigCache.debugLog(message);
        }
    }
    
    public static void logError(String message, Exception e) {
        if (e != null) {
            BridgeConfigCache.errorLog("%s: %s", message, e.getMessage());
        } else {
            BridgeConfigCache.errorLog(message);
        }
    }
}
