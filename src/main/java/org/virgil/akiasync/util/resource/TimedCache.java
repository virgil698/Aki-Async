package org.virgil.akiasync.util.resource;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class TimedCache<K, V> implements CacheInvalidator {

    private final Map<K, CacheEntry<V>> cache;
    private final long ttlMillis;

    public TimedCache(long ttlMillis) {
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("TTL must be positive");
        }
        this.cache = new ConcurrentHashMap<>();
        this.ttlMillis = ttlMillis;
    }

    @Nonnull
    public Optional<V> get(@Nonnull K key) {
        if (key == null) {
            return Optional.empty();
        }

        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isExpired()) {
            
            cache.remove(key);
            return Optional.empty();
        }

        return Optional.of(entry.getValue());
    }

    public void put(@Nonnull K key, @Nonnull V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis(), ttlMillis));
    }

    public void invalidate(@Nonnull K key) {
        cache.remove(key);
    }

    @Override
    public void invalidateAll() {
        cache.clear();
    }

    @Override
    public boolean isValid() {
        return !cache.isEmpty();
    }

    public int cleanupExpired() {
        int removed = 0;
        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removed++;
            }
        }
        return removed;
    }

    public int size() {
        return cache.size();
    }

    public boolean containsKey(@Nonnull K key) {
        return cache.containsKey(key);
    }

    private static final class CacheEntry<V> {
        private final V value;
        private final long timestamp;
        private final long ttlMillis;

        CacheEntry(@Nonnull V value, long timestamp, long ttlMillis) {
            this.value = value;
            this.timestamp = timestamp;
            this.ttlMillis = ttlMillis;
        }

        @Nonnull
        V getValue() {
            return value;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > ttlMillis;
        }
    }

    @Override
    public void invalidate() {
        
        invalidateAll();
    }
}
