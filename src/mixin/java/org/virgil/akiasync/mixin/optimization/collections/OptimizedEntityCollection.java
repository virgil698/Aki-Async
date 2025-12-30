package org.virgil.akiasync.mixin.optimization.collections;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import net.minecraft.world.entity.Entity;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;

public class OptimizedEntityCollection<T extends Entity> {

    private volatile Int2ObjectLinkedOpenHashMap<T> entities = new Int2ObjectLinkedOpenHashMap<>();
    private final AtomicReference<Int2ObjectLinkedOpenHashMap<T>> iteratorPointer = new AtomicReference<>();

    private void ensureActiveIsNotIterated() {
        Int2ObjectLinkedOpenHashMap<T> currentIterating = iteratorPointer.get();
        if (currentIterating == entities) {
            entities = entities.clone();
        }
    }

    public void add(T entity) {
        ensureActiveIsNotIterated();
        entities.put(entity.getId(), entity);
    }

    public void remove(T entity) {
        ensureActiveIsNotIterated();
        entities.remove(entity.getId());
    }

    public boolean contains(T entity) {
        return entities.containsKey(entity.getId());
    }

    public int size() {
        return entities.size();
    }

    public boolean isEmpty() {
        return entities.isEmpty();
    }

    public void forEach(Consumer<T> action) {
        Int2ObjectLinkedOpenHashMap<T> currentEntities = entities;

        if (iteratorPointer.compareAndSet(null, currentEntities)) {
            try {
                currentEntities.values().forEach(entity -> {
                    if (entity != null) {
                        action.accept(entity);
                    }
                });
            } finally {
                iteratorPointer.set(null);
            }
        } else {
            Int2ObjectLinkedOpenHashMap<T> clonedEntities = currentEntities.clone();
            clonedEntities.values().forEach(entity -> {
                if (entity != null) {
                    action.accept(entity);
                }
            });
        }
    }

    public void forEachBatch(Consumer<T> action, int batchSize) {
        Int2ObjectLinkedOpenHashMap<T> currentEntities = entities;

        if (iteratorPointer.compareAndSet(null, currentEntities)) {
            try {
                T[] entityArray = (T[]) currentEntities.values().toArray(new Entity[0]);

                for (int i = 0; i < entityArray.length; i += batchSize) {
                    int end = Math.min(i + batchSize, entityArray.length);
                    for (int j = i; j < end; j++) {
                        if (entityArray[j] != null) {
                            action.accept(entityArray[j]);
                        }
                    }

                    if (i + batchSize < entityArray.length) {
                        Thread.yield();
                    }
                }
            } finally {
                iteratorPointer.set(null);
            }
        } else {
            forEach(action);
        }
    }

    public void clear() {
        ensureActiveIsNotIterated();
        entities.clear();
    }

    @SuppressWarnings("unchecked")
    public T[] getSnapshot() {
        return (T[]) entities.values().toArray(new Entity[0]);
    }

    public CollectionStats getStats() {
        return new CollectionStats(
            entities.size(),
            iteratorPointer.get() != null,
            entities.hashCode()
        );
    }

    public static class CollectionStats {
        public final int size;
        public final boolean isIterating;
        public final int hashCode;

        public CollectionStats(int size, boolean isIterating, int hashCode) {
            this.size = size;
            this.isIterating = isIterating;
            this.hashCode = hashCode;
        }

        @Override
        public String toString() {
            return String.format("CollectionStats{size=%d, iterating=%s, hash=%d}",
                size, isIterating, hashCode);
        }
    }
}
