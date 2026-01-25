package org.virgil.akiasync.util.concurrency;

import org.virgil.akiasync.config.ConfigManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ConfigReloader {

    private static final List<ConfigReloadListener> listeners = new CopyOnWriteArrayList<>();

    private ConfigReloader() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void registerListener(@Nonnull ConfigReloadListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static boolean unregisterListener(@Nonnull ConfigReloadListener listener) {
        return listeners.remove(listener);
    }

    public static void notifyReload(@Nonnull ConfigManager newConfig) {
        if (newConfig == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        for (ConfigReloadListener listener : listeners) {
            try {
                listener.onConfigReload(newConfig);
            } catch (Exception e) {

                System.err.println("[AkiAsync] Error notifying config reload listener: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static int getListenerCount() {
        return listeners.size();
    }

    public static void clearListeners() {
        listeners.clear();
    }
}
