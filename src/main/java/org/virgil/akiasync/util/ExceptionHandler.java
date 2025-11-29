package org.virgil.akiasync.util;

import org.virgil.akiasync.constants.AkiAsyncConstants;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ExceptionHandler {

    private ExceptionHandler() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void safeExecute(Runnable operation, String operationName) {
        safeExecute(operation, operationName, null);
    }

    public static void safeExecute(Runnable operation, String operationName, Runnable fallback) {
        try {
            operation.run();
        } catch (Exception e) {
            logError(operationName + " failed: " + e.getMessage(), e);
            if (fallback != null) {
                try {
                    fallback.run();
                } catch (Exception fallbackEx) {
                    logError(operationName + " fallback also failed: " + fallbackEx.getMessage(), fallbackEx);
                }
            }
        }
    }

    public static <T> T safeSupply(Supplier<T> supplier, String operationName, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            logError(operationName + " failed: " + e.getMessage(), e);
            return defaultValue;
        }
    }

    public static <T> T safeSupply(Supplier<T> supplier, String operationName, Supplier<T> fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            logError(operationName + " failed: " + e.getMessage(), e);
            if (fallback != null) {
                try {
                    return fallback.get();
                } catch (Exception fallbackEx) {
                    logError(operationName + " fallback also failed: " + fallbackEx.getMessage(), fallbackEx);
                    return null;
                }
            }
            return null;
        }
    }

    public static void safeReflection(ReflectiveRunnable reflectionOperation, String operationName) {
        try {
            reflectionOperation.run();
        } catch (ReflectiveOperationException e) {
            logError("Reflection operation '" + operationName + "' failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logError("Unexpected error in reflection operation '" + operationName + "': " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface ReflectiveRunnable {
        void run() throws ReflectiveOperationException;
    }

    public static <T> T safeReflectionSupply(ReflectiveSupplier<T> reflectionOperation, String operationName, T defaultValue) {
        try {
            return reflectionOperation.get();
        } catch (ReflectiveOperationException e) {
            logError("Reflection operation '" + operationName + "' failed: " + e.getMessage(), e);
            return defaultValue;
        } catch (Exception e) {
            logError("Unexpected error in reflection operation '" + operationName + "': " + e.getMessage(), e);
            return defaultValue;
        }
    }

    @FunctionalInterface
    public interface ReflectiveSupplier<T> {
        T get() throws ReflectiveOperationException;
    }

    public static void logError(String message, Throwable throwable) {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            bridge.errorLog(AkiAsyncConstants.Logging.ERROR_PREFIX + " " + message);
        } else {
            System.err.println(AkiAsyncConstants.Logging.ERROR_PREFIX + " " + message);
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }
    }

    public static void logDebug(String message) {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isDebugLoggingEnabled()) {
            bridge.debugLog(AkiAsyncConstants.Logging.DEBUG_PREFIX + " " + message);
        }
    }

    public static boolean isAsyncCatcherError(Throwable throwable) {
        if (throwable == null) return false;
        StackTraceElement[] stack = throwable.getStackTrace();
        return stack.length > 0 && "org.spigotmc.AsyncCatcher".equals(stack[0].getClassName());
    }

    public static <T> Consumer<T> safeConsumer(Consumer<T> consumer, String operationName) {
        return item -> safeExecute(() -> consumer.accept(item), operationName);
    }
}
