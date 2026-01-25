package org.virgil.akiasync.mixin.util;

import java.util.ArrayDeque;
import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleObjectPool<T> {

    private final ArrayDeque<T> pool;
    private final Function<Void, T> factory;
    private final Consumer<T> reset;
    private final int maxSize;

    public SimpleObjectPool(Function<Void, T> factory, Consumer<T> reset, int maxSize) {
        this.pool = new ArrayDeque<>(Math.min(maxSize, 64));
        this.factory = factory;
        this.reset = reset;
        this.maxSize = maxSize;
    }

    public T alloc() {
        T obj = pool.pollFirst();
        if (obj == null) {
            obj = factory.apply(null);
        }
        return obj;
    }

    public void release(T obj) {
        if (pool.size() < maxSize) {
            reset.accept(obj);
            pool.addFirst(obj);
        }
    }
}
