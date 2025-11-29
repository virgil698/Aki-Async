package org.virgil.akiasync.util.resource;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourceTracker {

    private static final Map<String, WeakReference<AutoCloseable>> trackedResources = new ConcurrentHashMap<>();

    private ResourceTracker() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    @Nonnull
    public static <T extends AutoCloseable> T track(@Nonnull T resource, @Nonnull String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        trackedResources.put(name, new WeakReference<>(resource));
        return resource;
    }

    @Nonnull
    public static List<String> getUnclosedResources() {
        List<String> unclosed = new ArrayList<>();

        for (Map.Entry<String, WeakReference<AutoCloseable>> entry : trackedResources.entrySet()) {
            AutoCloseable resource = entry.getValue().get();
            if (resource != null) {
                unclosed.add(entry.getKey());
            }
        }

        trackedResources.entrySet().removeIf(entry -> entry.getValue().get() == null);

        return unclosed;
    }

    public static void closeAll() {
        for (Map.Entry<String, WeakReference<AutoCloseable>> entry : trackedResources.entrySet()) {
            AutoCloseable resource = entry.getValue().get();
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error closing resource '" + entry.getKey() + "': " + e.getMessage());
                }
            }
        }

        trackedResources.clear();
    }

    public static boolean untrack(@Nonnull String name) {
        return trackedResources.remove(name) != null;
    }

    public static int getTrackedCount() {
        return trackedResources.size();
    }

    public static void clear() {
        trackedResources.clear();
    }
}
