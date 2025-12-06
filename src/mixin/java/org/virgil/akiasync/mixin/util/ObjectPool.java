package org.virgil.akiasync.mixin.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class ObjectPool<T> {
    
    private final Queue<T> pool;
    private final Supplier<T> factory;
    private final int maxSize;
    private int currentSize;
    
    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.pool = new ConcurrentLinkedQueue<>();
        this.factory = factory;
        this.maxSize = maxSize;
        this.currentSize = 0;
    }
    
    public ObjectPool(Supplier<T> factory) {
        this(factory, 32);
    }
    
    public T acquire() {
        T obj = pool.poll();
        if (obj == null) {
            obj = factory.get();
        }
        return obj;
    }
    
    public void release(T obj) {
        if (obj == null) {
            return;
        }
        
        if (currentSize < maxSize) {
            pool.offer(obj);
            currentSize++;
        }
        
    }
    
    public void clear() {
        pool.clear();
        currentSize = 0;
    }
    
    public int size() {
        return pool.size();
    }
    
    public String getStats() {
        return String.format("Pool size: %d/%d", pool.size(), maxSize);
    }
}
