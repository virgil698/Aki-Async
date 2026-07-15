package com.akiasync.mixin.datapack;

import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ZipDataPackCache {
    private static final int MAX_INDEX_ENTRIES = 4_096;
    private static final int MAX_CONTENT_ENTRIES = 4_096;
    private static final int MAX_CACHEABLE_RESOURCE_BYTES = 4 * 1024 * 1024;
    private static final long MAX_CONTENT_BYTES = 32L * 1024 * 1024;
    private static final ConcurrentHashMap<QueryKey, List<?>> INDEX = new ConcurrentHashMap<>();
    private static final LinkedHashMap<ResourceKey, byte[]> CONTENT = new LinkedHashMap<>(128, 0.75F, true);
    private static long contentBytes;

    private ZipDataPackCache() {
    }

    public static ArchiveFingerprint fingerprint(File file) {
        return new ArchiveFingerprint(
                file.toPath().toAbsolutePath().normalize().toString(),
                file.length(),
                file.lastModified()
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> findIndex(
            ArchiveFingerprint archive,
            String prefix,
            String packDirectory,
            String namespace,
            String path
    ) {
        List<?> result = INDEX.get(new QueryKey(archive, prefix, packDirectory, namespace, path));
        if (result == null) {
            DataPackOptimizationMetrics.zipIndexMiss();
            return null;
        }
        DataPackOptimizationMetrics.zipIndexHit();
        return (List<T>) result;
    }

    public static void storeIndex(
            ArchiveFingerprint archive,
            String prefix,
            String packDirectory,
            String namespace,
            String path,
            List<?> resources
    ) {
        if (INDEX.size() >= MAX_INDEX_ENTRIES) {
            INDEX.clear();
        }
        INDEX.put(new QueryKey(archive, prefix, packDirectory, namespace, path), List.copyOf(resources));
    }

    public static void invalidateIndex(
            ArchiveFingerprint archive,
            String prefix,
            String packDirectory,
            String namespace,
            String path
    ) {
        INDEX.remove(new QueryKey(archive, prefix, packDirectory, namespace, path));
    }

    public static IoSupplier<InputStream> wrap(
            ArchiveFingerprint archive,
            String prefix,
            String packDirectory,
            String location,
            IoSupplier<InputStream> original
    ) {
        ResourceKey key = new ResourceKey(archive, prefix, packDirectory, location);
        return () -> openCached(key, original);
    }

    static int indexEntryCount() {
        return INDEX.size();
    }

    static int contentEntryCount() {
        synchronized (CONTENT) {
            return CONTENT.size();
        }
    }

    static long contentBytes() {
        synchronized (CONTENT) {
            return contentBytes;
        }
    }

    static void clear() {
        INDEX.clear();
        synchronized (CONTENT) {
            CONTENT.clear();
            contentBytes = 0;
        }
    }

    private static InputStream openCached(ResourceKey key, IoSupplier<InputStream> original) throws IOException {
        byte[] cached;
        synchronized (CONTENT) {
            cached = CONTENT.get(key);
        }
        if (cached != null) {
            DataPackOptimizationMetrics.zipContentHit(cached.length);
            return new ByteArrayInputStream(cached);
        }

        DataPackOptimizationMetrics.zipContentMiss();
        InputStream input = original.get();
        try {
            byte[] prefix = input.readNBytes(MAX_CACHEABLE_RESOURCE_BYTES + 1);
            if (prefix.length > MAX_CACHEABLE_RESOURCE_BYTES) {
                return new SequenceInputStream(new ByteArrayInputStream(prefix), input);
            }

            input.close();
            putContent(key, prefix);
            return new ByteArrayInputStream(prefix);
        } catch (IOException | RuntimeException | Error failure) {
            input.close();
            throw failure;
        }
    }

    private static void putContent(ResourceKey key, byte[] bytes) {
        synchronized (CONTENT) {
            byte[] previous = CONTENT.put(key, bytes);
            if (previous != null) {
                contentBytes -= previous.length;
            }
            contentBytes += bytes.length;
            while (CONTENT.size() > MAX_CONTENT_ENTRIES || contentBytes > MAX_CONTENT_BYTES) {
                Map.Entry<ResourceKey, byte[]> eldest = CONTENT.entrySet().iterator().next();
                contentBytes -= eldest.getValue().length;
                CONTENT.remove(eldest.getKey());
            }
        }
    }

    public record ArchiveFingerprint(String path, long length, long lastModifiedMillis) {
    }

    private record QueryKey(
            ArchiveFingerprint archive,
            String prefix,
            String packDirectory,
            String namespace,
            String path
    ) {
    }

    private record ResourceKey(
            ArchiveFingerprint archive,
            String prefix,
            String packDirectory,
            String location
    ) {
    }
}
