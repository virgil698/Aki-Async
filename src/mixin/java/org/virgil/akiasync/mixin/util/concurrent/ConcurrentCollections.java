package org.virgil.akiasync.mixin.util.concurrent;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ConcurrentCollections {

    public static <E> Set<E> newHashSet() {
        return ConcurrentHashMap.newKeySet();
    }

    public static <K, V> Map<K, V> newHashMap() {
        return new ConcurrentHashMap<>();
    }

    public static <E> Set<E> newCopyOnWriteArraySet() {
        return new CopyOnWriteArraySet<>();
    }
}
