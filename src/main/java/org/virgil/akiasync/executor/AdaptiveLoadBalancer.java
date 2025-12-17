package org.virgil.akiasync.executor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class AdaptiveLoadBalancer {
    
    private static final Logger LOGGER = Logger.getLogger("AkiAsync");
    
    private static final double MSPT_WARNING = 40.0;
    private static final double MSPT_CRITICAL = 45.0;
    private static final double MSPT_TARGET = 30.0;
    
    public enum LoadLevel {
        NORMAL,
        MODERATE,
        HIGH,
        CRITICAL
    }
    
    private static volatile LoadLevel currentLoad = LoadLevel.NORMAL;
    private static final AtomicLong lastMsptUpdate = new AtomicLong(0);
    private static final AtomicInteger consecutiveHighLoad = new AtomicInteger(0);
    
    private static volatile double taskSubmitRate = 1.0;
    
    public static void updateMspt(double mspt) {
        lastMsptUpdate.set(System.currentTimeMillis());
        
        LoadLevel oldLoad = currentLoad;
        
        if (mspt >= MSPT_CRITICAL) {
            currentLoad = LoadLevel.CRITICAL;
            consecutiveHighLoad.incrementAndGet();
        } else if (mspt >= MSPT_WARNING) {
            currentLoad = LoadLevel.HIGH;
            consecutiveHighLoad.incrementAndGet();
        } else if (mspt >= MSPT_TARGET) {
            currentLoad = LoadLevel.MODERATE;
            consecutiveHighLoad.set(0);
        } else {
            currentLoad = LoadLevel.NORMAL;
            consecutiveHighLoad.set(0);
        }
        
        adjustTaskSubmitRate(mspt);
        
        if (oldLoad != currentLoad) {
            LOGGER.info(String.format(
                "[LoadBalancer] Load level changed: %s -> %s (MSPT: %.2f, Rate: %.0f%%)",
                oldLoad, currentLoad, mspt, taskSubmitRate * 100
            ));
        }
    }
    
    private static void adjustTaskSubmitRate(double mspt) {
        switch (currentLoad) {
            case CRITICAL:

                taskSubmitRate = 0.3;
                break;
            case HIGH:

                taskSubmitRate = 0.6;
                break;
            case MODERATE:

                taskSubmitRate = 0.85;
                break;
            case NORMAL:

                taskSubmitRate = 1.0;
                break;
        }
    }
    
    public static LoadLevel getCurrentLoad() {
        return currentLoad;
    }
    
    public static double getTaskSubmitRate() {
        return taskSubmitRate;
    }
    
    public static boolean shouldSubmitTask() {
        if (currentLoad == LoadLevel.NORMAL) {
            return true;
        }
        
        return Math.random() < taskSubmitRate;
    }
    
    public static boolean shouldSkipLowPriority() {
        return currentLoad == LoadLevel.CRITICAL || 
               (currentLoad == LoadLevel.HIGH && consecutiveHighLoad.get() > 3);
    }
    
    public static String getStatistics() {
        return String.format(
            "LoadBalancer: Level=%s | Rate=%.0f%% | ConsecutiveHigh=%d",
            currentLoad,
            taskSubmitRate * 100,
            consecutiveHighLoad.get()
        );
    }
    
    public static void reset() {
        currentLoad = LoadLevel.NORMAL;
        taskSubmitRate = 1.0;
        consecutiveHighLoad.set(0);
    }
}
