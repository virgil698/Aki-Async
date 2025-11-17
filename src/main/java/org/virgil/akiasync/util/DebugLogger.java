package org.virgil.akiasync.util;

public class DebugLogger {
    
    private static volatile boolean debugEnabled = false;
    
    public static void updateDebugState(boolean enabled) {
        debugEnabled = enabled;
    }
    
    public static void debug(String message) {
        if (debugEnabled) {
            System.out.println(message);
        }
    }
    
    public static void debug(String format, Object... args) {
        if (debugEnabled) {
            System.out.println(String.format(format, args));
        }
    }
    
    public static void error(String message) {
        System.err.println(message);
    }
    
    public static void error(String format, Object... args) {
        System.err.println(String.format(format, args));
    }
    
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
}
