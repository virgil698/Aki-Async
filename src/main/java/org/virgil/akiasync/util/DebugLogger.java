package org.virgil.akiasync.util;

import org.bukkit.plugin.Plugin;
import java.util.logging.Logger;

public class DebugLogger {
    private static Logger logger;
    private static boolean debugEnabled = false;

    public static void setLogger(Plugin plugin) {
        logger = plugin.getLogger();
    }

    public static void updateDebugState(boolean enabled) {
        debugEnabled = enabled;
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void debug(String message) {
        if (debugEnabled && logger != null) {
            logger.info(message);
        }
    }

    public static void debug(String format, Object... args) {
        if (debugEnabled && logger != null) {
            logger.info(String.format(format, args));
        }
    }

    public static void info(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    public static void warning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    public static void error(String message) {
        if (logger != null) {
            logger.severe(message);
        }
    }

    public static void error(String format, Object... args) {
        if (logger != null) {
            logger.severe(String.format(format, args));
        }
    }
}
